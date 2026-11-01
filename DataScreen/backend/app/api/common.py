from flask import current_app,request
from app.repositories.trade_repository import TradeRepository
from app.repositories.goods_repository import GoodsRepository
from app.repositories.shop_repository import ShopRepository
from app.repositories.user_repository import UserRepository
from app.services.analytics_service import AnalyticsService
from app.utils.response import success
from app.utils.validators import validate_query
REPOSITORIES={"trade":TradeRepository,"goods":GoodsRepository,"shop":ShopRepository,"user":UserRepository}
def service(topic): return AnalyticsService(REPOSITORIES[topic](current_app.extensions["druid_client"]),topic)
def query_args(): return validate_query(request.args)
def respond(result,query):
    data,warnings,as_of=result
    return success(data,query,warnings,as_of)
