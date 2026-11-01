from datetime import datetime
import pytz
class TimeAdapter:
    SHANGHAI=pytz.timezone("Asia/Shanghai")
    @classmethod
    def parse_api(cls,value):
        parsed=value if isinstance(value,datetime) else datetime.fromisoformat(str(value).replace("Z","+00:00"))
        return cls.SHANGHAI.localize(parsed) if parsed.tzinfo is None else parsed.astimezone(cls.SHANGHAI)
    @classmethod
    def to_druid(cls,value):
        # Druid 保留上海业务钟面数字但错误标记为 UTC，因此禁止实际减 8 小时。
        return cls.parse_api(value).strftime("%Y-%m-%dT%H:%M:%S.000Z")
    @classmethod
    def from_druid(cls,value):
        wall=datetime.fromisoformat(str(value).replace("Z","+00:00")).replace(tzinfo=None)
        return cls.SHANGHAI.localize(wall).isoformat(timespec="seconds")
