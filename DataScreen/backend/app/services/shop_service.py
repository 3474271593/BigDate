from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP

from app.utils.data_quality import deduplicate_rows
from app.utils.time_adapter import TimeAdapter
from .analytics_service import AnalyticsService


class ShopService(AnalyticsService):
    def historical_heatmap(self, query):
        rows, warnings, as_of = self._rows(query)
        cells = defaultdict(lambda: {"orders": Decimal(0), "amount": Decimal(0), "ends": set()})
        windows = defaultdict(set)
        shop_ids = set()
        for row in rows:
            shop_id = row.get("shopId")
            if shop_id is None:
                continue
            start = TimeAdapter.from_druid(row["__time"])
            end = TimeAdapter.parse_api(row["windowEnd"]).isoformat(timespec="seconds") if row.get("windowEnd") else None
            cell = cells[(start, shop_id)]
            cell["orders"] += Decimal(str(row.get("paidOrderCount") or 0))
            cell["amount"] += Decimal(str(row.get("paidAmount") or 0))
            if end:
                cell["ends"].add(end)
                windows[start].add(end)
            else:
                windows[start]
            shop_ids.add(shop_id)
        conflicts = sum(len(cell["ends"]) > 1 for cell in cells.values())
        if conflicts:
            warnings.append({"code": "WINDOW_END_CONFLICT", "count": conflicts})
        data = []
        cents = Decimal("0.01")
        for (start, shop_id), cell in sorted(cells.items()):
            data.append({"time": start, "windowEnd": max(cell["ends"], default=None),
                         "date": start[:10], "shopId": shop_id,
                         "paidOrders": int(cell["orders"]),
                         "paidAmount": float(cell["amount"].quantize(cents, rounding=ROUND_HALF_UP))})
        dates = defaultdict(int)
        for start in windows:
            dates[start[:10]] += 1
        result = {"dates": [{"date": date, "windows": count} for date, count in sorted(dates.items())],
                  "windows": [{"time": start, "windowEnd": max(ends, default=None)} for start, ends in sorted(windows.items())],
                  "shops": [{"shopId": shop_id, "name": f"店铺 {shop_id}"} for shop_id in sorted(shop_ids)],
                  "data": data, "zeroCells": sum(cell["paidOrders"] == 0 for cell in data),
                  "missingCells": len(shop_ids) * len(windows) - len(data)}
        return result, warnings, as_of

    def time_paid_amount(self, query):
        windows, rows = self.repository.fetch_latest_paid_windows(query)
        rows, warnings = deduplicate_rows(
            rows, ["__time", "windowEnd", "shopId"], ["paidAmount", "paidOrderCount"]
        )
        if not rows:
            warnings.append({"code": "EMPTY_RESULT"})
        totals = defaultdict(float)
        for row in rows:
            totals[row["shopId"]] += float(row.get("paidAmount") or 0)
        ranked = sorted(totals, key=lambda shop_id: (-totals[shop_id], shop_id))[:10]
        chosen = set(ranked)
        times = [TimeAdapter.from_druid(value) for value in windows]
        data = [
            {"time": TimeAdapter.from_druid(row["__time"]), "shopId": row["shopId"],
             "paidAmount": round(float(row.get("paidAmount") or 0), 2),
             "paidOrderCount": int(row.get("paidOrderCount") or 0)}
            for row in rows if row["shopId"] in chosen
        ]
        data.sort(key=lambda row: (row["time"], ranked.index(row["shopId"])))
        shops = [{"shopId": shop_id, "shopName": f"店铺 {shop_id}"} for shop_id in ranked]
        as_of = times[-1] if times else None
        return {"times": times, "shops": shops, "data": data}, warnings, as_of
