import json
import requests
class DruidError(RuntimeError): pass
class DruidClient:
    def __init__(self, base_url, sql_path, timeout=8):
        self.url=f"{base_url.rstrip('/')}/{sql_path.lstrip('/')}"; self.timeout=timeout
        self.session=requests.Session()
        self.session.trust_env=False
    @staticmethod
    def parse_object_lines(text):
        return [json.loads(line) for line in text.splitlines() if line.strip()]
    def query(self, sql, parameters=None):
        try:
            response=self.session.post(self.url,json={"query":sql,"resultFormat":"objectLines","parameters":parameters or []},timeout=self.timeout)
            response.raise_for_status(); payload=self.parse_object_lines(response.text)
            if not isinstance(payload,list): raise DruidError("Druid 返回格式异常")
            return payload
        except requests.Timeout as exc: raise TimeoutError("Druid 查询超时") from exc
        except (requests.RequestException,ValueError) as exc: raise DruidError("Druid 查询失败") from exc
    def health(self): return bool(self.query("SELECT 1 AS ok"))
