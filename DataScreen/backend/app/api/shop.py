from flask import Blueprint,current_app
from app.repositories.shop_repository import ShopRepository
from app.services.shop_service import ShopService
from app.utils.validators import ValidationError
from .common import query_args,respond,service
bp=Blueprint("shop",__name__)
def with_shop_names(result):
    data,warnings,as_of=result
    lookup=current_app.extensions.get("shop_name_lookup")
    if lookup and data:
        try:
            names=lookup.get_many(row.get("shopId",row.get("id")) for row in data)
            for row in data:
                shop_id=row.get("shopId",row.get("id"))
                row["name"]=names.get(str(shop_id)) or row["name"]
            missing=sum(str(row.get("shopId",row.get("id"))) not in names for row in data)
            if missing: warnings.append({"code":"SHOP_NAME_MISSING","count":missing})
        except (OSError,ValueError):
            current_app.logger.warning("Redis shop-name lookup unavailable")
            warnings.append({"code":"SHOP_DIMENSION_UNAVAILABLE"})
    return data,warnings,as_of
def top(field):
    query=query_args(); return respond(with_shop_names(service("shop").top(query,field)),query)
@bp.route("/api/shop/paid-amount-top", methods=["GET"])
def paid_amount(): return top("paidAmount")
@bp.route("/api/shop/order-top", methods=["GET"])
def orders(): return top("paidOrderCount")
@bp.route("/api/shop/user-top", methods=["GET"])
def users(): return top("paidUserCount")
@bp.route("/api/shop/contribution", methods=["GET"])
def contribution():
    query=query_args(); return respond(with_shop_names(service("shop").shop_contribution(query)),query)

@bp.route("/api/shop/history-heatmap", methods=["GET"])
def history_heatmap():
    query = query_args()
    if query["dataset"] != "historical":
        raise ValidationError("HISTORICAL_ONLY", "仅支持2019历史模式")
    result, warnings, as_of = ShopService(
        ShopRepository(current_app.extensions["druid_client"]), "shop"
    ).historical_heatmap(query)
    with_shop_names((result["shops"], warnings, as_of))
    names = {shop["shopId"]: shop["name"] for shop in result["shops"]}
    for row in result["data"]:
        row["shopName"] = names[row["shopId"]]
    return respond((result, warnings, as_of), query)

@bp.route("/api/shop/time-paid-amount", methods=["GET"])
def time_paid_amount():
    query = query_args()
    result, warnings, as_of = ShopService(
        ShopRepository(current_app.extensions["druid_client"]), "shop"
    ).time_paid_amount(query)
    lookup = current_app.extensions.get("shop_name_lookup")
    if lookup and result["shops"]:
        try:
            names = lookup.get_many(shop["shopId"] for shop in result["shops"])
            for shop in result["shops"]:
                shop["shopName"] = names.get(str(shop["shopId"])) or shop["shopName"]
            missing = sum(str(shop["shopId"]) not in names for shop in result["shops"])
            if missing:
                warnings.append({"code": "SHOP_NAME_MISSING", "count": missing})
        except (OSError, ValueError):
            warnings.append({"code": "SHOP_DIMENSION_UNAVAILABLE"})
    names_by_id = {shop["shopId"]: shop["shopName"] for shop in result["shops"]}
    for row in result["data"]:
        row["shopName"] = names_by_id[row["shopId"]]
    return respond((result, warnings, as_of), query)
