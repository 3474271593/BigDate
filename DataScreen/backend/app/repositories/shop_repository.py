from .base_repository import TopicRepository
from app.utils.time_adapter import TimeAdapter

class ShopRepository(TopicRepository):
    def __init__(self,client): super().__init__(client,"shop")

    def fetch_latest_paid_windows(self, query, count=12):
        start = TimeAdapter.to_druid(query["start"])[:19].replace("T", " ")
        end = TimeAdapter.to_druid(query["end"])[:19].replace("T", " ")
        bounds = f'"__time" >= TIMESTAMP \'{start}\' AND "__time" < TIMESTAMP \'{end}\''
        windows = self.client.query(
            f'SELECT DISTINCT "__time" FROM "dws_shop_summary" '
            f'WHERE {bounds} ORDER BY "__time" DESC LIMIT {count}'
        )
        if not windows:
            return [], []
        selected = {row["__time"] for row in windows}
        earliest = min(selected)[:19].replace("T", " ")
        rows = self.client.query(
            f'SELECT "__time", "windowEnd", "shopId", "paidAmount", "paidOrderCount" '
            f'FROM "dws_shop_summary" WHERE {bounds} '
            f'AND "__time" >= TIMESTAMP \'{earliest}\' ORDER BY "__time"'
        )
        return sorted(selected), [row for row in rows if row.get("__time") in selected]
