from app import create_app


class WindowClient:
    def __init__(self, rows):
        self.rows = rows
        self.sql = []

    def query(self, sql, parameters=None):
        self.sql.append(sql)
        if 'SELECT DISTINCT' in sql:
            return [{'__time': '2026-10-07T21:00:00.000Z'}, {'__time': '2026-10-07T20:59:00.000Z'}]
        return self.rows


class ShopNames:
    def get_many(self, ids):
        return {str(shop_id): f'真实店铺{shop_id}' for shop_id in ids}


def test_shop_time_amount_uses_real_windows_and_top_ten_names():
    rows = []
    for shop_id in range(1, 13):
        for minute in (59, 0):
            hour = 20 if minute == 59 else 21
            rows.append({'__time': f'2026-10-07T{hour:02d}:{minute:02d}:00.000Z',
                         'windowEnd': f'2026-10-07 {hour:02d}:{minute + 1:02d}:00',
                         'shopId': shop_id, 'paidAmount': float(shop_id), 'paidOrderCount': 1})
    druid = WindowClient(rows)
    app = create_app({'TESTING': True, 'DRUID_CLIENT': druid, 'SHOP_NAME_LOOKUP': ShopNames()})
    response = app.test_client().get('/api/shop/time-paid-amount?dataset=test')
    assert response.status_code == 200
    data = response.get_json()['data']
    assert len(data['times']) == 2
    assert len(data['shops']) == 10
    assert data['shops'][0] == {'shopId': 12, 'shopName': '真实店铺12'}
    assert len(data['data']) == 20
    assert data['data'][0]['shopName'].startswith('真实店铺')
    assert all(isinstance(row['paidAmount'], (int, float)) and row['paidAmount'] >= 0 for row in data['data'])
    assert all(row['paidOrderCount'] == 1 and row['time'] in data['times'] for row in data['data'])
    assert 'dws_shop_summary' in druid.sql[0]
    assert all('TIME_FLOOR' not in sql for sql in druid.sql)
    assert 'ORDER BY "__time", "shopId"' not in druid.sql[1]


def test_shop_time_amount_empty_windows_returns_empty_business_data():
    class EmptyClient:
        def query(self, sql, parameters=None):
            return []
    app = create_app({'TESTING': True, 'DRUID_CLIENT': EmptyClient()})
    response = app.test_client().get('/api/shop/time-paid-amount?dataset=test')
    assert response.status_code == 200
    assert response.get_json()['data'] == {'times': [], 'shops': [], 'data': []}
