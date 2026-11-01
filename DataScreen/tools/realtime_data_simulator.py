# 文件路径：F:\GitShuju\DataScreen\tools\realtime_data_simulator.py
"""按现有 shop 维度生成接近真实购买行为的订单及同店商品明细。"""

import argparse
import atexit
from collections import Counter
import json
import os
import random
import shutil
import signal
import subprocess
import sys
import time
import uuid
from datetime import datetime, timedelta
from decimal import Decimal
from pathlib import Path


AUDIT_PATH = Path(__file__).with_name("simulator_inserted.jsonl")
RNG = random.SystemRandom()
STOP = False


def sql_text(value):
    """将文本编码为 MySQL 十六进制字面量，避免拼接引号和注入。"""
    return "CONVERT(0x" + str(value).encode("utf-8").hex() + " USING utf8)"


def mysql(sql, config):
    env = os.environ.copy()
    env["MYSQL_PWD"] = config["password"]
    command = [config["client"], "--batch", "--raw", "--skip-column-names",
               "--default-character-set=utf8", "--host=" + config["host"],
               "--port=" + str(config["port"]), "--user=" + config["user"],
               "--database=" + config["database"]]
    result = subprocess.run(command, input=sql, text=True, encoding="utf-8",
                            capture_output=True, env=env, timeout=30, check=False)
    if result.returncode:
        raise RuntimeError("MySQL 命令失败：" + result.stderr.strip()[:400])
    return [line for line in result.stdout.splitlines() if line]


def read_config():
    client = os.getenv("MYSQL_CLIENT") or shutil.which("mysql")
    if not client:
        standard = Path(r"C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe")
        client = str(standard) if standard.is_file() else None
    if not client:
        raise RuntimeError("未找到 mysql 客户端；请设置 MYSQL_CLIENT")
    password = os.getenv("MYSQL_PASSWORD")
    if not password:
        raise RuntimeError("缺少 MYSQL_PASSWORD 环境变量")
    return {"client": client, "host": os.getenv("MYSQL_HOST", "192.168.13.232"),
            "port": int(os.getenv("MYSQL_PORT", "3306")),
            "user": os.getenv("MYSQL_USER", "root"),
            "password": password, "database": os.getenv("MYSQL_DATABASE", "shop")}


def select_json(sql, config):
    return [json.loads(line) for line in mysql(sql, config)]


def preflight(config):
    databases = mysql("SHOW DATABASES", config)
    if config["database"] not in databases:
        raise RuntimeError("目标数据库不存在或名称大小写不符")
    expected = {
        "orders": {"orderId", "orderNo", "orderunique", "shopId", "userId",
                   "orderStatus", "goodsMoney", "deliverMoney", "totalMoney",
                   "realTotalMoney", "isPay", "payType", "areaId", "userName",
                   "userAddress", "createTime", "payTime"},
        "order_goods": {"ogId", "orderId", "goodsId", "goodsNum", "goodsPrice",
                        "goodsName", "goodsImg"},
        "goods": {"goodsId", "shopId", "goodsName", "goodsImg", "shopPrice",
                  "goodsStock", "isSale", "goodsStatus", "dataFlag"},
        "shops": {"shopId", "shopStatus", "dataFlag"},
        "users": {"userId", "userStatus", "dataFlag"},
    }
    for table, required in expected.items():
        columns = {line.split("\t")[0]: line.split("\t") for line in
                   mysql("SHOW COLUMNS FROM `" + table + "`", config)}
        missing = required - columns.keys()
        if missing:
            raise RuntimeError(table + " 缺少字段：" + ", ".join(sorted(missing)))
        if table in ("orders", "order_goods"):
            primary = "orderId" if table == "orders" else "ogId"
            if "auto_increment" not in columns[primary][-1]:
                raise RuntimeError(table + "." + primary + " 不是 AUTO_INCREMENT")
    server_time = mysql("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')", config)[0]
    if abs((datetime.now() - datetime.strptime(server_time, "%Y-%m-%d %H:%M:%S")).total_seconds()) > 300:
        raise RuntimeError("MySQL 与本机时间相差超过 5 分钟")


def load_dimensions(config):
    users = select_json("SELECT JSON_OBJECT('userId',userId) FROM users "
                        "WHERE userStatus=1 AND dataFlag=1 ORDER BY userId LIMIT 100", config)
    goods = select_json("SELECT JSON_OBJECT('goodsId',g.goodsId,'shopId',g.shopId,"
                        "'goodsName',g.goodsName,'goodsImg',g.goodsImg,"
                        "'goodsType',g.goodsType,'shopPrice',CAST(g.shopPrice AS CHAR)) "
                        "FROM goods g JOIN shops s ON g.shopId=s.shopId "
                        "WHERE g.isSale=1 AND g.goodsStatus=1 AND g.dataFlag=1 "
                        "AND g.goodsStock>0 AND s.shopStatus=1 AND s.dataFlag=1 "
                        "AND g.shopPrice BETWEEN 1 AND 200 ORDER BY g.goodsId LIMIT 100", config)
    if len(users) < 2 or not goods:
        raise RuntimeError("可用用户不足 2 个或无在售商品")
    return users, goods


def tier_weights(items, fractions, shares):
    """随机分层；权重按层内人数归一，表示各层期望订单份额。"""
    count = len(items)
    first = max(1, round(count * fractions[0]))
    second = max(1, round(count * fractions[1]))
    first = min(first, count)
    second = min(second, count - first)
    sizes = (first, second, count - first - second)
    return [shares[tier] / sizes[tier] if sizes[tier] else 0
            for tier, size in enumerate(sizes) for _ in range(size)]


def build_user_weights(users):
    shuffled = list(users)
    RNG.shuffle(shuffled)
    weights = tier_weights(shuffled, (0.20, 0.50), (0.45, 0.40, 0.15))
    return {item["userId"]: weight for item, weight in zip(shuffled, weights)}


def build_goods_weights(goods):
    """热门商品集中在部分店铺，每店设上限，兼顾热度与分散。"""
    shops = {}
    for item in goods:
        shops.setdefault(item["shopId"], []).append(item)
    for pool in shops.values():
        RNG.shuffle(pool)
    shop_ids = list(shops)
    hot_count = max(1, round(len(goods) * 0.12))
    hot_shop_count = min(hot_count, len(shop_ids), max(2, round(len(shop_ids) * 0.67)))
    hot_shops = RNG.sample(shop_ids, hot_shop_count)
    ordered = [shops[shop_id].pop() for shop_id in hot_shops]
    hot_per_shop = Counter({shop_id: 1 for shop_id in hot_shops})
    shop_cap = max(1, (hot_count + hot_shop_count - 1) // hot_shop_count)
    while len(ordered) < hot_count:
        candidates = [shop_id for shop_id in hot_shops
                      if shops[shop_id] and hot_per_shop[shop_id] < shop_cap]
        if not candidates:
            candidates = [shop_id for shop_id in shop_ids if shops[shop_id]]
        chosen = RNG.choice(candidates)
        ordered.append(shops[chosen].pop())
        hot_per_shop[chosen] += 1
    remaining = [item for pool in shops.values() for item in pool]
    RNG.shuffle(remaining)
    ordered.extend(remaining)
    weights = tier_weights(ordered, (0.12, 0.40), (0.50, 0.35, 0.15))
    return {item["goodsId"]: weight for item, weight in zip(ordered, weights)}


def choose_user(users, weights, previous=None, streak=0):
    choices = [item for item in users if streak < 3 or item["userId"] != previous]
    return RNG.choices(choices, weights=[weights[item["userId"]] for item in choices])[0]["userId"]


def choose_goods(goods, weights):
    first = RNG.choices(goods, weights=[weights[item["goodsId"]] for item in goods])[0]
    count = RNG.choices((1, 2, 3), weights=(70, 20, 10))[0]
    selected = [first]
    same_shop = [item for item in goods if item["shopId"] == first["shopId"]
                 and item["goodsId"] != first["goodsId"]]
    while len(selected) < count and same_shop:
        item = RNG.choices(same_shop, weights=[weights[g["goodsId"]] for g in same_shop])[0]
        selected.append(item)
        same_shop.remove(item)
    return selected


def make_lines(goods):
    lines = []
    for item in goods:
        price = Decimal(item["shopPrice"]).quantize(Decimal("0.01"))
        if not Decimal("1") <= price <= Decimal("200"):
            raise ValueError("商品单价超出安全范围")
        lines.append({"goodsId": item["goodsId"], "goodsName": item["goodsName"],
                      "goodsImg": item["goodsImg"], "goodsType": item["goodsType"],
                      "goodsNum": RNG.choices((1, 2, 3), weights=(85, 12, 3))[0],
                      "goodsPrice": str(price)})
    return lines


def calculate_goods_money(lines):
    return sum((Decimal(line["goodsPrice"]) * line["goodsNum"] for line in lines),
               Decimal("0.00"))


def make_order(user_id, goods):
    if not goods or len({item["shopId"] for item in goods}) != 1:
        raise ValueError("一笔订单的商品必须来自同一家店铺")
    now = datetime.now().replace(microsecond=0)
    lines = make_lines(goods)
    goods_money = calculate_goods_money(lines)
    delivery = Decimal("0.00")
    total = goods_money + delivery
    paid = RNG.random() < 0.90
    stamp = now.strftime("%Y-%m-%d %H:%M:%S")
    order_no = str(int(time.time() * 1000)) + f"{RNG.randrange(1000):03d}"
    return {"orderNo": order_no, "orderunique": str(uuid.uuid4()),
            "userId": user_id, "shopId": goods[0]["shopId"], "goods": lines,
            "goodsMoney": str(goods_money), "deliverMoney": str(delivery),
            "totalMoney": str(total), "realTotalMoney": str(total),
            "isPay": int(paid), "payType": 1 if paid else 0,
            "orderStatus": 0 if paid else -2, "areaId": 23,
            "userName": "模拟用户" + str(user_id),
            "userAddress": "模拟订单地址", "createTime": stamp,
            "payTime": (now + timedelta(seconds=RNG.randrange(1, 6))).strftime("%Y-%m-%d %H:%M:%S") if paid else None}


def insert_order(config, order):
    # 全部 INSERT 共用一个 mysql 会话；任一失败即退出，未提交事务由服务端回滚。
    pay_time = "NULL" if order["payTime"] is None else sql_text(order["payTime"])
    detail_sql = "".join(
        "INSERT INTO order_goods (orderId,goodsId,goodsNum,goodsPrice,goodsName,goodsImg,goodsType) VALUES ("
        f"@sim_order_id,{line['goodsId']},{line['goodsNum']},{line['goodsPrice']},"
        f"{sql_text(line['goodsName'])},{sql_text(line['goodsImg'])},{line['goodsType']});\n"
        for line in order["goods"])
    sql = (
        "START TRANSACTION;\n"
        "INSERT INTO orders (orderNo,orderunique,shopId,userId,orderStatus,goodsMoney,"
        "deliverMoney,totalMoney,realTotalMoney,payType,isPay,areaId,userName,userAddress,"
        "createTime,payTime,needPay,dataFlag) VALUES ("
        f"{sql_text(order['orderNo'])},{sql_text(order['orderunique'])},"
        f"{order['shopId']},{order['userId']},{order['orderStatus']},"
        f"{order['goodsMoney']},{order['deliverMoney']},{order['totalMoney']},"
        f"{order['realTotalMoney']},{order['payType']},{order['isPay']},"
        f"{order['areaId']},{sql_text(order['userName'])},{sql_text(order['userAddress'])},"
        f"{sql_text(order['createTime'])},{pay_time},0.00,1);\n"
        "SET @sim_order_id = LAST_INSERT_ID();\n"
        + detail_sql + "COMMIT;\nSELECT @sim_order_id;"
    )
    rows = mysql(sql, config)
    if len(rows) != 1 or not rows[0].isdigit():
        raise RuntimeError("事务结果中缺少 orderId")
    return int(rows[0])


def audit(order_id, order):
    entry = {key: order[key] for key in ("orderNo", "userId", "shopId", "isPay", "createTime", "realTotalMoney")}
    entry["orderId"] = order_id
    entry["goods"] = [{key: line[key] for key in ("goodsId", "goodsNum", "goodsPrice")}
                      for line in order["goods"]]
    with AUDIT_PATH.open("a", encoding="utf-8") as output:
        output.write(json.dumps(entry, ensure_ascii=False) + "\n")


def generate_order(users, goods, user_weights, goods_weights, previous=None, streak=0):
    user_id = choose_user(users, user_weights, previous, streak)
    order = make_order(user_id, choose_goods(goods, goods_weights))
    return order, (streak + 1 if user_id == previous else 1)


def preview_orders(users, goods, user_weights, goods_weights, count):
    """只在内存生成并统计订单；不调用 insert_order 或 audit。"""
    orders = []
    previous = None
    streak = 0
    for _ in range(count):
        order, streak = generate_order(users, goods, user_weights, goods_weights, previous, streak)
        previous = order["userId"]
        orders.append(order)
    users_seen = Counter(order["userId"] for order in orders)
    goods_seen = Counter(line["goodsId"] for order in orders for line in order["goods"])
    shops_seen = Counter(order["shopId"] for order in orders)
    line_counts = Counter(len(order["goods"]) for order in orders)
    amounts = [Decimal(order["realTotalMoney"]) for order in orders]
    return {"orders": count, "distinctUsers": len(users_seen),
            "topUsers": users_seen.most_common(5),
            "distinctGoods": len(goods_seen), "topGoods": goods_seen.most_common(5),
            "distinctShops": len(shops_seen), "topShops": shops_seen.most_common(5),
            "goodsKindsPerOrder": {str(n): line_counts[n] for n in (1, 2, 3)},
            "averageOrderAmount": str((sum(amounts) / count).quantize(Decimal("0.01"))),
            "maxOrderAmount": str(max(amounts)),
            "paidCount": sum(order["isPay"] for order in orders),
            "paidRatio": round(sum(order["isPay"] for order in orders) / count, 4)}


def handle_stop(_signum, _frame):
    global STOP
    STOP = True


class WindowsCtrlCInput:
    """兼容不向子进程转发 SIGINT、却能写入 Ctrl+C 按键的 Windows 终端。"""

    def __init__(self):
        self.handle = None
        self.original_mode = None

    def enable(self):
        if os.name != "nt":
            return
        import ctypes

        kernel = ctypes.windll.kernel32
        handle = kernel.GetStdHandle(-10)  # STD_INPUT_HANDLE
        mode = ctypes.c_uint()
        if not kernel.GetConsoleMode(handle, ctypes.byref(mode)):
            return  # 重定向输入时继续使用常规 SIGINT 处理
        if kernel.SetConsoleMode(handle, mode.value & ~1):  # 关闭 ENABLE_PROCESSED_INPUT
            self.handle = handle
            self.original_mode = mode.value
            atexit.register(self.restore)

    def poll(self):
        if self.handle is not None:
            import msvcrt

            while msvcrt.kbhit():
                if msvcrt.getwch() == "\x03":
                    handle_stop(None, None)
        return STOP

    def restore(self):
        if self.handle is not None:
            import ctypes

            ctypes.windll.kernel32.SetConsoleMode(self.handle, self.original_mode)
            self.handle = None


def main():
    parser = argparse.ArgumentParser(description="MySQL shop 实时订单模拟器")
    parser.add_argument("--interval", type=float, default=10.0, help="订单间隔秒数，默认 10，最小 2")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--once", action="store_true", help="写入一笔完整订单后退出")
    mode.add_argument("--dry-run", action="store_true", help="只读生成并打印一笔订单")
    mode.add_argument("--preview", type=int, metavar="N", help="只在内存生成 N 笔订单并统计，绝不写入")
    args = parser.parse_args()
    if args.interval < 2:
        parser.error("--interval 不得低于 2 秒")
    if args.preview is not None and args.preview < 1:
        parser.error("--preview 必须为正整数")
    signal.signal(signal.SIGINT, handle_stop)
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, handle_stop)
    stop_input = WindowsCtrlCInput()
    stop_input.enable()
    config = read_config()
    preflight(config)
    users, goods = load_dimensions(config)
    user_weights = build_user_weights(users)
    goods_weights = build_goods_weights(goods)
    print(f"database={config['database']} tables=orders,order_goods users={len(users)} "
          f"goods={len(goods)} shops={len({item['shopId'] for item in goods})} "
          f"mode={'DRY RUN / NO DATABASE WRITE' if args.dry_run or args.preview is not None else 'REAL WRITE'}", flush=True)
    if args.preview is not None:
        print(json.dumps(preview_orders(users, goods, user_weights, goods_weights, args.preview),
                         ensure_ascii=False, indent=2))
        return 0
    previous = None
    streak = 0
    try:
        while not stop_input.poll():
            order, streak = generate_order(users, goods, user_weights, goods_weights, previous, streak)
            previous = order["userId"]
            if args.dry_run:
                print(json.dumps(order, ensure_ascii=False, indent=2))
                return 0
            if stop_input.poll():
                break
            order_id = None
            try:
                order_id = insert_order(config, order)
            except Exception as exc:
                print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] stage=transaction "
                      f"orderId={order_id} error={exc} rollback=server-on-disconnect", file=sys.stderr)
                return 1
            try:
                audit(order_id, order)
            except OSError as exc:
                print(f"已提交 orderId={order_id}，审计日志写入失败：{exc}", file=sys.stderr)
                return 1
            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] inserted orderId={order_id} "
                  f"userId={order['userId']} shopId={order['shopId']} "
                  f"goodsIds={[line['goodsId'] for line in order['goods']]} "
                  f"amount=¥{order['realTotalMoney']} paid={'yes' if order['isPay'] else 'no'}", flush=True)
            if args.once:
                return 0
            deadline = time.monotonic() + args.interval
            while not stop_input.poll() and time.monotonic() < deadline:
                time.sleep(min(0.2, deadline - time.monotonic()))
    except KeyboardInterrupt:
        pass
    finally:
        stop_input.restore()
        if not args.dry_run and args.preview is None:
            print("模拟器已停止")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] stage=preflight error={exc}", file=sys.stderr)
        sys.exit(1)
