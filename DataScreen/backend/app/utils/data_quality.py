from collections import defaultdict
def deduplicate_rows(rows,natural_keys,metric_fields):
    groups=defaultdict(list)
    for row in rows: groups[tuple(row.get(key) for key in natural_keys)].append(row)
    clean=[]; duplicates=0; conflicts=0
    for group in groups.values():
        signatures={tuple(row.get(field) for field in metric_fields) for row in group}
        if len(signatures)>1: conflicts+=len(group); continue
        clean.append(group[0]); duplicates+=len(group)-1
    warnings=[]
    if duplicates: warnings.append({"code":"DUPLICATE_ROWS_DETECTED","count":duplicates})
    if conflicts: warnings.append({"code":"DATA_CONFLICT","count":conflicts})
    return clean,warnings
