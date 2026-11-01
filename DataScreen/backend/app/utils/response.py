from datetime import datetime
from flask import jsonify
def success(data,query=None,warnings=None,data_as_of=None):
    query=query or {}; start=query.get("start"); end=query.get("end")
    return jsonify({"success":True,"data":data,"meta":{"dataset":query.get("dataset"),"timezone":"Asia/Shanghai","timeRange":{"start":start.isoformat() if start else None,"end":end.isoformat() if end else None},"dataAsOf":data_as_of or datetime.now().astimezone().isoformat(timespec="seconds"),"warnings":warnings or []}})
def failure(code,message,status=400): return jsonify({"success":False,"error":{"code":code,"message":message}}),status
