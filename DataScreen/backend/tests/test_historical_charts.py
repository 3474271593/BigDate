from app import create_app


class FakeClient:
    def __init__(self, shop_rows=None, trade_rows=None):
        self.shop_rows = shop_rows or []
        self.trade_rows = trade_rows or []
        self.queries = []

    def query(self, sql, parameters=None):
        self.queries.append(sql)
        if 'dws_shop_summary' in sql:
            return self.shop_rows
        if 'dws_trade_summary' in sql:
            return self.trade_rows
        return []


def shop_row(start, end, shop_id, orders, amount):
    return {'__time': start, 'windowEnd': end, 'shopId': shop_id,
            'paidOrderCount': orders, 'paidAmount': amount}


def trade_row(start, end, orders, amount):
    return {'__time': start, 'windowEnd': end,
            'paidOrderCount': orders, 'paidAmount': amount}


def test_historical_heatmap_keeps_zero_cells_and_leaves_missing_cells_absent():
    first = '2019-06-22T11:11:00.000Z'
    second = '2019-06-22T11:12:00.000Z'
    later = '2019-09-05T20:27:00.000Z'
    rows = [
        shop_row(first, '2019-06-22 11:12:00', 1, 0, 0),
        shop_row(first, '2019-06-22 11:12:00', 2, 2, 20.01),
        shop_row(second, '2019-06-22 11:13:00', 1, 3, 30.02),
        shop_row(second, '2019-06-22 11:13:00', 1, 3, 30.02),
        shop_row(later, '2019-09-05 20:28:00', 2, 1, 9.99),
    ]
    fake = FakeClient(shop_rows=rows)
    app = create_app({'TESTING': True, 'DRUID_CLIENT': fake})
    response = app.test_client().get('/api/shop/history-heatmap?dataset=historical')
    assert response.status_code == 200
    data = response.get_json()['data']
    assert len(data['shops']) == 2
    assert [item['date'] for item in data['dates']] == ['2019-06-22', '2019-09-05']
    assert [item['time'] for item in data['windows']] == [
        '2019-06-22T11:11:00+08:00', '2019-06-22T11:12:00+08:00',
        '2019-09-05T20:27:00+08:00']
    assert len(data['data']) == 4
    assert data['data'][0]['paidOrders'] == 0
    assert data['data'][0]['paidAmount'] == 0
    assert all(not (cell['shopId'] == 2 and cell['time'] == '2019-06-22T11:12:00+08:00') for cell in data['data'])
    assert data['data'][2]['paidOrders'] == 3
    assert data['data'][2]['paidAmount'] == 30.02
    assert data['data'][2]['windowEnd'] == '2019-06-22T11:13:00+08:00'
    assert response.get_json()['meta']['warnings'][0]['code'] == 'DUPLICATE_ROWS_DETECTED'
    assert '2019-01-01' in fake.queries[0] and '2020-01-01' in fake.queries[0]


def test_historical_volume_price_aggregates_same_start_before_average_and_filters_unpaid():
    first = '2019-06-22T11:11:00.000Z'
    rows = [
        trade_row(first, '2019-06-22 11:12:00', 2, 10.01),
        trade_row(first, '2019-06-22 11:13:00', 3, 15.02),
        trade_row('2019-07-29T15:10:00.000Z', '2019-07-29 15:11:00', 0, 0),
        trade_row('2019-09-05T20:27:00.000Z', '2019-09-05 20:28:00', 1, 99.99),
    ]
    fake = FakeClient(trade_rows=rows)
    app = create_app({'TESTING': True, 'DRUID_CLIENT': fake})
    response = app.test_client().get('/api/trade/history-volume-price?dataset=historical')
    assert response.status_code == 200
    data = response.get_json()['data']
    assert data['totalWindows'] == 2
    assert len(data['points']) == 2
    assert [item['date'] for item in data['dates']] == ['2019-06-22', '2019-09-05']
    assert data['points'][0]['paidOrders'] == 5
    assert data['points'][0]['paidAmount'] == 25.03
    assert data['points'][0]['avgOrderPaid'] == 5.01
    assert data['points'][0]['windowEnd'] == '2019-06-22T11:13:00+08:00'
    assert data['points'][1]['paidAmount'] == 99.99
    assert '2019-01-01' in fake.queries[0] and '2020-01-01' in fake.queries[0]


def test_historical_chart_endpoints_reject_realtime_without_querying_druid():
    fake = FakeClient()
    client = create_app({'TESTING': True, 'DRUID_CLIENT': fake}).test_client()
    for path in ('/api/shop/history-heatmap', '/api/trade/history-volume-price'):
        response = client.get(path + '?dataset=test')
        assert response.status_code == 400
        assert response.get_json()['error']['code'] == 'HISTORICAL_ONLY'
    assert fake.queries == []

def test_historical_heatmap_aggregates_same_shop_and_start_into_one_cell():
    start = '2019-09-05T11:35:00.000Z'
    rows = [
        shop_row(start, '2019-09-05 11:36:00', 100050, 2, 12.34),
        shop_row(start, '2019-09-05 11:37:00', 100050, 3, 23.45),
    ]
    fake = FakeClient(shop_rows=rows)
    client = create_app({'TESTING': True, 'DRUID_CLIENT': fake}).test_client()
    result = client.get('/api/shop/history-heatmap?dataset=historical').get_json()
    assert result['success']
    assert len(result['data']['data']) == 1
    assert result['data']['data'][0]['paidOrders'] == 5
    assert result['data']['data'][0]['paidAmount'] == 35.79
    assert result['meta']['warnings'][0]['code'] == 'WINDOW_END_CONFLICT'
