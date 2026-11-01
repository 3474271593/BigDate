from flask import Flask
from .config import Config
from .clients.druid_client import DruidClient
from .clients.redis_shop_names import RedisShopNames
from .utils.response import failure
from .utils.validators import ValidationError
def create_app(overrides=None):
    app=Flask(__name__); app.config.from_object(Config); app.config.update(overrides or {})
    app.extensions["druid_client"]=app.config.get("DRUID_CLIENT") or DruidClient(app.config["DRUID_BASE_URL"],app.config["DRUID_SQL_PATH"],app.config["DRUID_TIMEOUT"])
    app.extensions["shop_name_lookup"]=app.config.get("SHOP_NAME_LOOKUP") or (RedisShopNames(app.config["REDIS_SHOP_HOST"],app.config["REDIS_SHOP_PORT"],app.config["REDIS_SHOP_PASSWORD"],app.config["REDIS_SHOP_DB"],app.config["REDIS_SHOP_TIMEOUT"]) if app.config["REDIS_SHOP_PASSWORD"] and not app.config.get("TESTING") else None)
    from .api import health,dashboard,trade,goods,shop,user
    for module in (health,dashboard,trade,goods,shop,user): app.register_blueprint(module.bp)
    @app.errorhandler(ValidationError)
    def invalid(exc): return failure(exc.code,exc.message,400)
    @app.errorhandler(Exception)
    def unexpected(exc):
        app.logger.exception("API request failed")
        return failure("INTERNAL_ERROR","服务暂时不可用",500)
    return app
