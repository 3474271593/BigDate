from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP

from app.utils.time_adapter import TimeAdapter
from .analytics_service import AnalyticsService


class TradeService(AnalyticsService):
    def historical_volume_price(self, query):
        rows, warnings, as_of = self._rows(query)
        windows = defaultdict(lambda: {"orders": Decimal(0), "amount": Decimal(0), "ends": set()})
        for row in rows:
            start = TimeAdapter.from_druid(row["__time"])
            window = windows[start]
            window["orders"] += Decimal(str(row.get("paidOrderCount") or 0))
            window["amount"] += Decimal(str(row.get("paidAmount") or 0))
            if row.get("windowEnd"):
                window["ends"].add(TimeAdapter.parse_api(row["windowEnd"]).isoformat(timespec="seconds"))
        conflicts = sum(len(window["ends"]) > 1 for window in windows.values())
        if conflicts:
            warnings.append({"code": "WINDOW_END_CONFLICT", "count": conflicts})
        cents = Decimal("0.01")
        points = []
        for start, window in sorted(windows.items()):
            if window["orders"] <= 0:
                continue
            points.append({"time": start, "windowEnd": max(window["ends"], default=None),
                           "date": start[:10], "paidOrders": int(window["orders"]),
                           "paidAmount": float(window["amount"].quantize(cents, rounding=ROUND_HALF_UP)),
                           "avgOrderPaid": float((window["amount"] / window["orders"]).quantize(cents, rounding=ROUND_HALF_UP))})
        dates = sorted({point["date"] for point in points})
        return {"points": points, "dates": [{"date": date, "windows": sum(point["date"] == date for point in points)} for date in dates],
                "totalWindows": len(points), "excludedNoPay": len(windows) - len(points)}, warnings, as_of
