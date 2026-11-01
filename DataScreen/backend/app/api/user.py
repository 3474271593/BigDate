from flask import Blueprint
from .common import query_args,respond,service
bp=Blueprint("user",__name__)
def top(field):
    query=query_args(); return respond(service("user").top(query,field),query)
@bp.route("/api/user/paid-amount-top", methods=["GET"])
def paid_amount(): return top("paidAmount")
@bp.route("/api/user/order-top", methods=["GET"])
def orders(): return top("paidOrderCount")
@bp.route("/api/user/value-distribution", methods=["GET"])
def value_distribution():
    query=query_args(); return respond(service("user").user_distribution(query),query)
