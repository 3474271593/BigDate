from flask import Blueprint
from .common import query_args,respond,service
bp=Blueprint("dashboard",__name__)
@bp.route("/api/dashboard/overview", methods=["GET"])
def overview():
    query=query_args(); return respond(service("trade").overview(query),query)
@bp.route("/api/dashboard/dataset-meta", methods=["GET"])
def dataset_meta():
    query=query_args(); return respond(service("trade").dataset_meta(query),query)
