from app.utils.time_adapter import TimeAdapter
SCHEMAS={
 "trade":("dws_trade_summary",["orderCount","paidAmount","paidOrderCount"]),
 "goods":("dws_goods_summary",["goodsId","goodsName","orderAmount","orderCount","orderGoodsNum","paidAmount","paidGoodsNum","paidOrderCount"]),
 "shop":("dws_shop_summary",["shopId","orderAmount","orderCount","orderUserCount","paidAmount","paidOrderCount","paidUserCount"]),
 "user":("dws_user_summary",["userId","orderAmount","orderCount","orderShopCount","paidAmount","paidOrderCount","paidShopCount"])}
class TopicRepository:
    def __init__(self,client,topic): self.client=client; self.topic=topic
    def fetch(self,query):
        datasource,fields=SCHEMAS[self.topic]
        start=TimeAdapter.to_druid(query["start"])[:19].replace("T"," "); end=TimeAdapter.to_druid(query["end"])[:19].replace("T"," ")
        columns=", ".join(["__time",'"windowEnd"']+[f'"{field}"' for field in fields])
        sql=f'SELECT {columns} FROM "{datasource}" WHERE __time >= TIMESTAMP \'{start}\' AND __time < TIMESTAMP \'{end}\''
        return self.client.query(sql)
