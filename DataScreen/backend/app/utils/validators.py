from datetime import datetime
from .time_adapter import TimeAdapter
DATASETS={"historical":(datetime(2019,1,1),datetime(2020,1,1)),"test":(datetime(2026,1,1),datetime(2027,1,1))}
class ValidationError(ValueError):
    def __init__(self,code,message): super().__init__(message); self.code=code; self.message=message
def validate_query(args):
    dataset=args.get("dataset")
    if not dataset: raise ValidationError("DATASET_REQUIRED","dataset 参数必填")
    if dataset not in DATASETS: raise ValidationError("INVALID_DATASET","dataset 仅支持 historical 或 test")
    granularity=args.get("granularity","minute")
    if granularity not in {"minute","hour","day"}: raise ValidationError("INVALID_GRANULARITY","granularity 仅支持 minute/hour/day")
    try: limit=int(args.get("limit",10))
    except (TypeError,ValueError): raise ValidationError("INVALID_LIMIT","limit 必须为 1 到 20 的整数")
    if not 1<=limit<=20: raise ValidationError("INVALID_LIMIT","limit 必须为 1 到 20 的整数")
    ds_start,ds_end=DATASETS[dataset]
    try:
        start=TimeAdapter.parse_api(args["start"]) if args.get("start") else TimeAdapter.parse_api(ds_start)
        end=TimeAdapter.parse_api(args["end"]) if args.get("end") else TimeAdapter.parse_api(ds_end)
    except (ValueError,TypeError): raise ValidationError("INVALID_TIME","start/end 必须是有效日期时间")
    if start>=end: raise ValidationError("INVALID_TIME_RANGE","start 必须早于 end")
    if start<TimeAdapter.parse_api(ds_start) or end>TimeAdapter.parse_api(ds_end): raise ValidationError("DATASET_TIME_MISMATCH","时间范围超出所选数据集")
    return {"dataset":dataset,"start":start,"end":end,"granularity":granularity,"limit":limit}
