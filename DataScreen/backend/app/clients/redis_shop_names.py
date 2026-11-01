import socket


class RedisShopNames:
    """Read only the shopName field from the Redis shop dimension."""

    def __init__(self, host, port=6379, password=None, db=0, timeout=1.5):
        self.host = host
        self.port = port
        self.password = password
        self.db = db
        self.timeout = timeout

    @staticmethod
    def _command(stream, *parts):
        payload = [f"*{len(parts)}\r\n".encode()]
        for part in parts:
            value = str(part).encode("utf-8")
            payload.extend((f"${len(value)}\r\n".encode(), value, b"\r\n"))
        stream.write(b"".join(payload))
        stream.flush()
        kind = stream.read(1)
        if kind in (b"+", b"-", b":"):
            answer = stream.readline().rstrip(b"\r\n").decode("utf-8")
            if kind == b"-":
                raise OSError(f"Redis returned an error: {answer}")
            return answer
        if kind == b"$":
            length = int(stream.readline())
            if length < 0:
                return None
            value = stream.read(length).decode("utf-8")
            stream.read(2)
            return value
        raise OSError("Unexpected Redis response")

    def get_many(self, shop_ids):
        ids = list(dict.fromkeys(str(shop_id) for shop_id in shop_ids if shop_id is not None))
        if not ids:
            return {}
        with socket.create_connection((self.host, self.port), self.timeout) as connection:
            connection.settimeout(self.timeout)
            with connection.makefile("rwb") as stream:
                if self.password:
                    self._command(stream, "AUTH", self.password)
                if self.db:
                    self._command(stream, "SELECT", self.db)
                return {
                    shop_id: name
                    for shop_id in ids
                    if (name := self._command(stream, "HGET", f"dim:shops:{shop_id}", "shopName"))
                }
