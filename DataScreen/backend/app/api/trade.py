from flask import Blueprint,current_app
from app.repositories.trade_repository import TradeRepository
from app.services.trade_service import TradeService
from app.utils.validators import ValidationError
from .common import query_args,respond,service
bp=Blueprint("trade",__name__)
@bp.route("/api/trade/trend", methods=["GET"])
def trend():
    query=query_args(); return respond(service("trade").trend(query,["paidAmount"]),query)
@bp.route("/api/trade/order-trend", methods=["GET"])
def order_trend():
    query=query_args(); return respond(service("trade").trend(query,["orderCount","paidOrderCount"]),query)
@bp.route("/api/trade/history-volume-price", methods=["GET"])
def history_volume_price():
    query = query_args()
    if query["dataset"] != "historical":
        raise ValidationError("HISTORICAL_ONLY", "仅支持2019历史模式")
    return respond(TradeService(
        TradeRepository(current_app.extensions["druid_client"]), "trade"
    ).historical_volume_price(query), query)

@bp.route("/api/trade/active-days", methods=["GET"])
def active_days():
    query=query_args(); return respond(service("trade").active_days(query),query)
