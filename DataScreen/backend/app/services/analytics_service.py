from collections import defaultdict
from datetime import datetime
from decimal import Decimal, ROUND_HALF_UP
from app.repositories.base_repository import SCHEMAS
from app.utils.data_quality import deduplicate_rows
from app.utils.time_adapter import TimeAdapter

KEYS={"trade":["__time","windowEnd"],"goods":["__time","windowEnd","goodsId"],"shop":["__time","windowEnd","shopId"],"user":["__time","windowEnd","userId"]}
METRICS={topic:[field for field in fields if not field.endswith("Id") and field!="goodsName"] for topic,(_,fields) in SCHEMAS.items()}

class AnalyticsService:
    def __init__(self,repository,topic): self.repository=repository; self.topic=topic
    def _rows(self,query):
        rows=self.repository.fetch(query)
        rows,warnings=deduplicate_rows(rows,KEYS[self.topic],METRICS[self.topic])
        if not rows: warnings.append({"code":"EMPTY_RESULT"})
        data_as_of=max((TimeAdapter.from_druid(r["__time"]) for r in rows),default=None)
        return rows,warnings,data_as_of
    def overview(self,query):
        rows,warnings,as_of=self._rows(query)
        order_count=sum(float(r.get("orderCount") or 0) for r in rows); paid_orders=sum(float(r.get("paidOrderCount") or 0) for r in rows)
        return {"paidAmount":round(sum(float(r.get("paidAmount") or 0) for r in rows),2),"orderCount":int(order_count),"paidOrderCount":int(paid_orders),"paymentRate":round(paid_orders/order_count*100,2) if order_count else None},warnings,as_of
    def trend(self,query,fields):
        rows,warnings,as_of=self._rows(query); buckets=defaultdict(lambda:defaultdict(float)); fmt={"minute":"%Y-%m-%dT%H:%M:00+08:00","hour":"%Y-%m-%dT%H:00:00+08:00","day":"%Y-%m-%dT00:00:00+08:00"}[query["granularity"]]
        for row in rows:
            dt=datetime.fromisoformat(TimeAdapter.from_druid(row["__time"])); key=dt.strftime(fmt)
            for field in fields: buckets[key][field]+=float(row.get(field) or 0)
        data=[{"time":key,**{field:round(values[field],2) for field in fields}} for key,values in sorted(buckets.items())]
        return data,warnings,as_of
    def top(self,query,value_field,all_items=False):
        rows,warnings,as_of=self._rows(query); id_field={"goods":"goodsId","shop":"shopId","user":"userId"}[self.topic]; totals=defaultdict(float); names={}
        for row in rows:
            item_id=row.get(id_field); totals[item_id]+=float(row.get(value_field) or 0)
            if self.topic=="goods": names[item_id]=row.get("goodsName")
        missing=0; data=[]
        ranked=sorted(totals.items(),key=lambda x:x[1],reverse=True)
        for item_id,value in (ranked if all_items else ranked[:query["limit"]]):
            missing_name=self.topic=="goods" and not names.get(item_id); missing+=int(missing_name)
            label=(names.get(item_id) or f"商品 ID：{item_id}") if self.topic=="goods" else f"{'店铺' if self.topic=='shop' else '用户'} {item_id}"
            data.append({"id":item_id,"name":label,"value":round(value,2),"missingName":bool(missing_name)})
        if missing: warnings.append({"code":"MISSING_GOODS_NAME","count":missing})
        if self.topic=="goods": warnings.append({"code":"GOODS_DETAIL_COVERAGE_WARNING","message":"商品主题为商品明细口径，不代表完整平台 GMV 拆解"})
        return data,warnings,as_of
    def goods_share(self,query):
        rows,warnings,as_of=self._rows(query); totals=defaultdict(float); names={}
        for row in rows:
            item_id=row.get("goodsId"); totals[item_id]+=float(row.get("paidGoodsNum") or 0); names[item_id]=row.get("goodsName")
        ranked=sorted(totals.items(),key=lambda x:x[1],reverse=True); top=ranked[:5]; other=sum(value for _,value in ranked[5:])
        data=[]; missing=0
        for item_id,value in top:
            missing_name=not names.get(item_id); missing+=int(missing_name)
            data.append({"goodsId":item_id,"name":names.get(item_id) or f"商品 ID：{item_id}","value":round(value,2),"missingName":missing_name})
        if other>0: data.append({"goodsId":None,"name":"其他","value":round(other,2),"missingName":False})
        if missing: warnings.append({"code":"MISSING_GOODS_NAME","count":missing})
        warnings.append({"code":"GOODS_DETAIL_COVERAGE_WARNING","message":"商品主题为商品明细口径，不代表完整平台 GMV 拆解"})
        return data,warnings,as_of
    def goods_quadrants(self,query):
        rows,warnings,as_of=self._rows(query)
        totals=defaultdict(lambda:{"units":Decimal(0),"amount":Decimal(0),"name":None,"name_time":""})
        for row in rows:
            goods_id=row.get("goodsId")
            if goods_id is None: continue
            item=totals[goods_id]
            item["units"]+=Decimal(str(row.get("paidGoodsNum") or 0))
            item["amount"]+=Decimal(str(row.get("paidAmount") or 0))
            name=row.get("goodsName")
            if name and str(name).strip() and str(row.get("__time") or "")>=item["name_time"]:
                item["name"]=str(name).strip()
                item["name_time"]=str(row.get("__time") or "")
        counts={"highValueNiche":0,"highValueBestSeller":0,"lowSalesLowValue":0,"lowPriceVolume":0}
        threshold_units=Decimal(100)
        threshold_revenue=Decimal(80)
        cents=Decimal("0.01")
        goods=[]
        missing=0
        for goods_id,item in totals.items():
            units=item["units"]
            if units<=0: continue
            amount=item["amount"]
            revenue=amount/units
            high_units=units>=threshold_units
            high_revenue=revenue>=threshold_revenue
            quadrant=("highValueBestSeller" if high_units else "highValueNiche") if high_revenue else ("lowPriceVolume" if high_units else "lowSalesLowValue")
            counts[quadrant]+=1
            name=item["name"] or f"商品 ID：{goods_id}"
            missing+=int(not item["name"])
            goods.append({"goodsId":goods_id,"goodsName":name,"salesUnits":int(units),
                          "salesAmount":float(amount.quantize(cents,rounding=ROUND_HALF_UP)),
                          "avgUnitRevenue":float(revenue.quantize(cents,rounding=ROUND_HALF_UP)),
                          "quadrant":quadrant})
        goods.sort(key=lambda item:(-item["salesUnits"],str(item["goodsId"])))
        if missing: warnings.append({"code":"MISSING_GOODS_NAME","count":missing})
        data={"goods":goods,"totalGoods":len(goods),"counts":counts,
              "thresholds":{"salesUnits":100,"avgUnitRevenue":80,"source":"fixed"},
              "timeRange":{"start":query["start"].isoformat(),"end":query["end"].isoformat()}}
        return data,warnings,as_of
    def shop_contribution(self,query):
        rows,warnings,as_of=self._rows(query); totals=defaultdict(lambda:{"paidAmount":0.0,"paidOrderCount":0})
        for row in rows:
            item=totals[row.get("shopId")]; item["paidAmount"]+=float(row.get("paidAmount") or 0); item["paidOrderCount"]+=int(row.get("paidOrderCount") or 0)
        data=[{"shopId":key,"name":f"店铺 {key}","paidAmount":round(value["paidAmount"],2),"paidOrderCount":value["paidOrderCount"]} for key,value in sorted(totals.items(),key=lambda x:x[1]["paidAmount"],reverse=True)[:query["limit"]]]
        return data,warnings,as_of
    def user_distribution(self,query):
        rows,warnings,as_of=self._rows(query); totals=defaultdict(lambda:{"paidAmount":0.0,"paidOrderCount":0})
        for row in rows:
            item=totals[row.get("userId")]; item["paidAmount"]+=float(row.get("paidAmount") or 0); item["paidOrderCount"]+=int(row.get("paidOrderCount") or 0)
        data=[{"userId":key,"name":f"用户 {key}","paidAmount":round(value["paidAmount"],2),"paidOrderCount":value["paidOrderCount"]} for key,value in totals.items()]
        return data,warnings,as_of
    def active_days(self,query):
        rows,warnings,as_of=self._rows(query); days=defaultdict(lambda:{"paidAmount":0.0,"orderCount":0,"paidOrderCount":0})
        for row in rows:
            day=TimeAdapter.from_druid(row["__time"])[:10]; item=days[day]; item["paidAmount"]+=float(row.get("paidAmount") or 0); item["orderCount"]+=int(row.get("orderCount") or 0); item["paidOrderCount"]+=int(row.get("paidOrderCount") or 0)
        data=[{"date":day,"paidAmount":round(value["paidAmount"],2),"orderCount":value["orderCount"],"paidOrderCount":value["paidOrderCount"]} for day,value in sorted(days.items())]
        total=sum(item["paidAmount"] for item in data)
        if query["dataset"]=="historical" and total and max(item["paidAmount"] for item in data)/total>.95: warnings.append({"code":"DATA_SKEW_WARNING","message":"历史交易高度集中于单个活跃日，图表按真实数据展示"})
        return data,warnings,as_of
    def dataset_meta(self,query):
        rows,warnings,as_of=self._rows(query)
        times=sorted(TimeAdapter.from_druid(row["__time"]) for row in rows); days={value[:10] for value in times}
        data={"actualStart":times[0] if times else None,"actualEnd":times[-1] if times else None,"dataAsOf":times[-1] if times else None,"activeDays":len(days)}
        return data,warnings,as_of
