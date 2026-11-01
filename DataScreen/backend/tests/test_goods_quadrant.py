from app import create_app


class FakeClient:
    def __init__(self, rows):
        self.rows = rows
        self.sql = None

    def query(self, sql, parameters=None):
        self.sql = sql
        return self.rows


def row(time, window, goods_id, name, units, amount):
    return {"__time": time, "windowEnd": window, "goodsId": goods_id,
            "goodsName": name, "paidGoodsNum": units, "paidAmount": amount}


def test_quadrant_aggregates_by_goods_id_before_unit_revenue_and_excludes_zero():
    rows = [
        row("2026-01-02T00:00:00.000Z", "a", 1, "old", 40, 3600.02),
        row("2026-01-03T00:00:00.000Z", "b", 1, "new", 60, 4399.98),
        row("2026-01-02T00:00:00.000Z", "a", 2, None, 99, 7920),
        row("2026-01-02T00:00:00.000Z", "a", 3, "zero", 0, 500),
        row("2026-01-02T00:00:00.000Z", "a", 4, "volume", 100, 7999),
        row("2026-01-02T00:00:00.000Z", "a", 5, "small", 1, 1),
    ]
    fake = FakeClient(rows)
    app = create_app({"TESTING": True, "DRUID_CLIENT": fake})
    response = app.test_client().get("/api/goods/sales-value-quadrants?dataset=test")
    assert response.status_code == 200
    data = response.get_json()["data"]
    assert "dws_goods_summary" in fake.sql
    assert "2026-01-01" in fake.sql and "2027-01-01" in fake.sql
    assert data["thresholds"] == {"salesUnits": 100, "avgUnitRevenue": 80, "source": "fixed"}
    assert data["counts"] == {"highValueNiche": 1, "highValueBestSeller": 1,
                               "lowSalesLowValue": 1, "lowPriceVolume": 1}
    assert data["totalGoods"] == 4
    assert sum(data["counts"].values()) == len(data["goods"])
    goods = {item["goodsId"]: item for item in data["goods"]}
    assert goods[1]["goodsName"] == "new"
    assert goods[1]["salesUnits"] == 100
    assert goods[1]["salesAmount"] == 8000.0
    assert goods[1]["avgUnitRevenue"] == 80.0
    assert goods[1]["quadrant"] == "highValueBestSeller"
    assert goods[2]["goodsName"].endswith("2")
    assert goods[2]["quadrant"] == "highValueNiche"
    assert goods[4]["quadrant"] == "lowPriceVolume"
    assert 3 not in goods
    assert data["timeRange"]["start"].startswith("2026-01-01")


def test_quadrant_rejects_historical_mode_without_querying_druid():
    fake = FakeClient([])
    app = create_app({"TESTING": True, "DRUID_CLIENT": fake})
    response = app.test_client().get("/api/goods/sales-value-quadrants?dataset=historical")
    assert response.status_code == 400
    assert response.get_json()["error"]["code"] == "REALTIME_ONLY"
    assert fake.sql is None


def test_quadrant_empty_result_and_deduplication():
    empty = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient([])})
    data = empty.test_client().get("/api/goods/sales-value-quadrants?dataset=test").get_json()["data"]
    assert data["goods"] == [] and data["totalGoods"] == 0
    item = row("2026-01-02T00:00:00.000Z", "a", 1, "A", 10, 100)
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient([item, item])})
    payload = app.test_client().get("/api/goods/sales-value-quadrants?dataset=test").get_json()
    assert payload["data"]["goods"][0]["salesUnits"] == 10
    assert payload["meta"]["warnings"][0]["code"] == "DUPLICATE_ROWS_DETECTED"
