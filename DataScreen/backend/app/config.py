import os
from dotenv import load_dotenv
load_dotenv()
class Config:
    DRUID_BASE_URL = os.getenv("DRUID_BASE_URL", "http://192.168.13.232:8888")
    DRUID_SQL_PATH = os.getenv("DRUID_SQL_PATH", "/druid/v2/sql")
    DRUID_TIMEOUT = float(os.getenv("DRUID_TIMEOUT", "8"))
    FLASK_PORT = int(os.getenv("FLASK_PORT", "5000"))
    REDIS_SHOP_HOST = os.getenv("REDIS_SHOP_HOST", "192.168.13.232")
    REDIS_SHOP_PORT = int(os.getenv("REDIS_SHOP_PORT", "6379"))
    REDIS_SHOP_DB = int(os.getenv("REDIS_SHOP_DB", "0"))
    REDIS_SHOP_PASSWORD = os.getenv("REDIS_SHOP_PASSWORD")
    REDIS_SHOP_TIMEOUT = float(os.getenv("REDIS_SHOP_TIMEOUT", "1.5"))
