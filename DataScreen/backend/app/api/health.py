from flask import Blueprint,current_app
from app.utils.response import failure,success
bp=Blueprint("health",__name__)
@bp.route("/api/health", methods=["GET"])
def health():
    try: current_app.extensions["druid_client"].health(); return success({"status":"ok","druid":"reachable"})
    except Exception: return failure("DRUID_UNAVAILABLE","Druid 当前不可用",503)
