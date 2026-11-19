"""Lossless diagnostics for the existing D-060 road sample, no pass criteria changes."""
import hashlib
from collections import Counter

def read_snapshot(path):
    rows = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        pos, state, tags = line.split("\t")
        coord = tuple(map(int,pos.split(",")))
        if len(coord)!=3 or coord in rows or not state or not tags:
            raise ValueError("Invalid or duplicate road sample: "+line)
        rows[coord] = (state,tuple(tags.split(";")))
    return rows

def snapshot_digest(rows):
    digest = hashlib.sha256()
    for (x,y,z),(state,_) in rows.items():
        # Minecraft BlockPos.asLong(): 26 x bits, 26 z bits, 12 y bits.
        value = ((x & 0x3FFFFFF)<<38) | ((z & 0x3FFFFFF)<<12) | (y & 0xFFF)
        if value >= 1<<63:
            value -= 1<<64
        digest.update(f"{value}:{state}\n".encode("utf-8"))
    return digest.hexdigest()

def compare_snapshots(left,right):
    differences = []
    by_zone, by_station, by_state = Counter(), Counter(), Counter()
    for pos in sorted(left.keys() | right.keys()):
        a,b = left.get(pos),right.get(pos)
        if a==b:
            continue
        tags = sorted(set(a[1] if a else ()) | set(b[1] if b else ()))
        zones, stations = set(),set()
        for tag in tags:
            station,zone,_ = tag.split("/")
            zones.add(zone)
            stations.add(station)
        by_zone.update(zones)
        by_station.update(stations)
        a_state,b_state = a[0] if a else None,b[0] if b else None
        by_state.update([f"{a_state} -> {b_state}"])
        differences.append({"pos":pos,"left":a_state,"right":b_state,"left_tags":a[1] if a else [],"right_tags":b[1] if b else []})
    return {"count":len(differences),"left_count":len(left),"right_count":len(right),
            "by_zone":dict(by_zone),"by_station":dict(by_station),
            "by_state":dict(by_state),"differences":differences}
