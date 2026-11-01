from datetime import datetime

import pytest

from app import create_app
from app.clients.druid_client import DruidClient
from app.utils.data_quality import deduplicate_rows
from app.utils.time_adapter import TimeAdapter
from app.utils.validators import ValidationError, validate_query


class FakeClient:
    def __init__(self, rows=None, error=None):
        self.rows = rows or []
        self.error = error

    def query(self, sql, parameters=None):
        if self.error:
            raise self.error
        return self.rows

    def health(self):
        if self.error:
            raise self.error
        return True


def test_druid_client_bypasses_system_proxy_for_lan_endpoint():
    client = DruidClient("http://192.168.13.232:8888", "/druid/v2/sql")
    assert client.session.trust_env is False


def test_druid_client_parses_object_lines():
    assert DruidClient.parse_object_lines('{"a":1}\n{"a":2}\n') == [{"a": 1}, {"a": 2}]


def test_time_adapter_reinterprets_druid_pseudo_utc():
    assert TimeAdapter.from_druid("2026-09-23T17:06:00.000Z") == "2026-09-23T17:06:00+08:00"
    assert TimeAdapter.to_druid("2026-09-23 17:06:00") == "2026-09-23T17:06:00.000Z"


@pytest.mark.parametrize("params,code", [
    ({}, "DATASET_REQUIRED"),
    ({"dataset": "other"}, "INVALID_DATASET"),
    ({"dataset": "test", "start": "bad"}, "INVALID_TIME"),
    ({"dataset": "test", "start": "2026-09-24 10:00:00", "end": "2026-09-24 09:00:00"}, "INVALID_TIME_RANGE"),
    ({"dataset": "test", "granularity": "week"}, "INVALID_GRANULARITY"),
    ({"dataset": "test", "limit": "21"}, "INVALID_LIMIT"),
])
def test_query_validation(params, code):
    with pytest.raises(ValidationError) as exc:
        validate_query(params)
    assert exc.value.code == code


def test_dataset_bounds_are_isolated():
    assert validate_query({"dataset": "historical"})["start"].year == 2019
    assert validate_query({"dataset": "test"})["start"].year == 2026


def test_exact_duplicates_are_removed_and_warned():
    rows = [{"__time": "2026-09-23T17:06:00.000Z", "windowEnd": "x", "orderCount": 1, "paidAmount": 2.0}] * 2
    result, warnings = deduplicate_rows(rows, ["__time", "windowEnd"], ["orderCount", "paidAmount"])
    assert len(result) == 1
    assert warnings == [{"code": "DUPLICATE_ROWS_DETECTED", "count": 1}]


def test_conflicting_natural_key_is_not_auto_selected():
    rows = [
        {"__time": "t", "windowEnd": "w", "orderCount": 1},
        {"__time": "t", "windowEnd": "w", "orderCount": 2},
    ]
    result, warnings = deduplicate_rows(rows, ["__time", "windowEnd"], ["orderCount"])
    assert result == []
    assert warnings[0]["code"] == "DATA_CONFLICT"


def test_health_success_and_failure():
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient()})
    assert app.test_client().get("/api/health").status_code == 200
    failed = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(error=TimeoutError())})
    response = failed.test_client().get("/api/health")
    assert response.status_code == 503
    assert "Traceback" not in response.get_data(as_text=True)


def test_overview_deduplicates_and_handles_zero_orders():
    rows = [{"__time": "2026-09-23T17:06:00.000Z", "windowEnd": "w", "orderCount": 0, "paidOrderCount": 0, "paidAmount": 10.0}] * 2
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    body = app.test_client().get("/api/dashboard/overview?dataset=test").get_json()
    assert body["data"]["paidAmount"] == 10.0
    assert body["data"]["paymentRate"] is None
    assert body["meta"]["warnings"][0]["code"] == "DUPLICATE_ROWS_DETECTED"


def test_empty_result_warning_and_missing_goods_name():
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient([])})
    empty = app.test_client().get("/api/trade/trend?dataset=test").get_json()
    assert empty["meta"]["warnings"][0]["code"] == "EMPTY_RESULT"
    goods_rows = [{"__time": "2026-09-23T17:06:00.000Z", "windowEnd": "w", "goodsId": 100100, "goodsName": None, "paidGoodsNum": 2}]
    goods = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(goods_rows)}).test_client().get("/api/goods/paid-volume-top?dataset=test").get_json()
    assert goods["data"][0]["name"] == "商品 ID：100100"
    assert goods["data"][0]["missingName"] is True


def test_goods_share_returns_top_items_plus_real_other():
    rows = [
        {"__time": f"2019-06-22T11:0{i}:00.000Z", "windowEnd": str(i), "goodsId": i, "goodsName": f"g{i}", "paidGoodsNum": i}
        for i in range(1, 8)
    ]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    body = app.test_client().get("/api/goods/paid-volume-share?dataset=historical").get_json()
    assert [item["value"] for item in body["data"][:-1]] == [7, 6, 5, 4, 3]
    assert body["data"][-1] == {"goodsId": None, "name": "其他", "value": 3.0, "missingName": False}


def test_goods_paid_volume_all_returns_each_product_without_other():
    rows = [
        {"__time": f"2019-06-22T11:{i:02d}:00.000Z", "windowEnd": str(i), "goodsId": i, "goodsName": f"g{i}", "paidGoodsNum": i}
        for i in range(1, 8)
    ]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    data = app.test_client().get("/api/goods/paid-volume-all?dataset=historical").get_json()["data"]
    assert [(item["id"], item["value"]) for item in data] == [(i, float(i)) for i in range(7, 0, -1)]
    assert all(item["name"] != "其他" for item in data)


def test_shop_contribution_contains_amount_and_orders():
    rows = [
        {"__time": "2019-06-22T11:01:00.000Z", "windowEnd": "1", "shopId": 9, "paidAmount": 12.5, "paidOrderCount": 2},
        {"__time": "2019-06-22T11:02:00.000Z", "windowEnd": "2", "shopId": 9, "paidAmount": 7.5, "paidOrderCount": 1},
    ]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    data = app.test_client().get("/api/shop/contribution?dataset=historical").get_json()["data"]
    assert data == [{"shopId": 9, "name": "店铺 9", "paidAmount": 20.0, "paidOrderCount": 3}]


def test_shop_names_are_resolved_by_id_without_changing_metrics():
    class FakeShopNames:
        def get_many(self, ids):
            assert list(ids) == [9]
            return {"9": "真实店铺名"}

    rows = [{"__time": "2019-06-22T11:01:00.000Z", "windowEnd": "1", "shopId": 9,
             "paidAmount": 12.5, "paidOrderCount": 2}]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows), "SHOP_NAME_LOOKUP": FakeShopNames()})
    client = app.test_client()
    contribution = client.get("/api/shop/contribution?dataset=historical").get_json()["data"]
    ranking = client.get("/api/shop/order-top?dataset=historical").get_json()["data"]
    assert contribution == [{"shopId": 9, "name": "真实店铺名", "paidAmount": 12.5, "paidOrderCount": 2}]
    assert ranking == [{"id": 9, "name": "真实店铺名", "value": 2.0, "missingName": False}]


def test_user_value_distribution_aggregates_real_dimensions():
    rows = [{"__time": "2019-06-22T11:01:00.000Z", "windowEnd": "1", "userId": 3, "paidAmount": 88.0, "paidOrderCount": 2}]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    data = app.test_client().get("/api/user/value-distribution?dataset=historical").get_json()["data"]
    assert data == [{"userId": 3, "name": "用户 3", "paidAmount": 88.0, "paidOrderCount": 2}]


def test_active_days_returns_only_dates_with_real_rows():
    rows = [
        {"__time": "2019-06-22T11:01:00.000Z", "windowEnd": "1", "paidAmount": 10.0, "orderCount": 2, "paidOrderCount": 1},
        {"__time": "2019-09-05T20:28:00.000Z", "windowEnd": "2", "paidAmount": 90.0, "orderCount": 3, "paidOrderCount": 3},
    ]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    data = app.test_client().get("/api/trade/active-days?dataset=historical").get_json()["data"]
    assert [item["date"] for item in data] == ["2019-06-22", "2019-09-05"]


def test_dataset_meta_uses_actual_row_range_not_configured_range():
    rows = [
        {"__time": "2019-06-22T11:01:00.000Z", "windowEnd": "1", "paidAmount": 1, "orderCount": 1, "paidOrderCount": 0},
        {"__time": "2019-09-05T20:28:00.000Z", "windowEnd": "2", "paidAmount": 2, "orderCount": 0, "paidOrderCount": 1},
    ]
    app = create_app({"TESTING": True, "DRUID_CLIENT": FakeClient(rows)})
    data = app.test_client().get("/api/dashboard/dataset-meta?dataset=historical").get_json()["data"]
    assert data == {"actualStart": "2019-06-22T11:01:00+08:00", "actualEnd": "2019-09-05T20:28:00+08:00", "dataAsOf": "2019-09-05T20:28:00+08:00", "activeDays": 2}
