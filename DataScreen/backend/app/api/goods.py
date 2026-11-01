from flask import Blueprint
from .common import query_args,respond,service
from app.utils.validators import ValidationError
bp=Blueprint("goods",__name__)
def top(field):
    query=query_args(); return respond(service("goods").top(query,field),query)
@bp.route("/api/goods/paid-volume-top", methods=["GET"])
def paid_volume(): return top("paidGoodsNum")
@bp.route("/api/goods/paid-volume-all", methods=["GET"])
def paid_volume_all():
    query=query_args(); return respond(service("goods").top(query,"paidGoodsNum",all_items=True),query)
@bp.route("/api/goods/sales-value-quadrants", methods=["GET"])
def sales_value_quadrants():
    query=query_args()
    if query["dataset"]!="test":
        raise ValidationError("REALTIME_ONLY","商品四象限仅支持实时模式")
    return respond(service("goods").goods_quadrants(query),query)
@bp.route("/api/goods/order-volume-top", methods=["GET"])
def order_volume(): return top("orderGoodsNum")
@bp.route("/api/goods/paid-amount-top", methods=["GET"])
def paid_amount(): return top("paidAmount")
@bp.route("/api/goods/paid-volume-share", methods=["GET"])
def paid_volume_share():
    query=query_args(); return respond(service("goods").goods_share(query),query)
