import hashlib
import tempfile
import unittest
from pathlib import Path
from road_samples import read_snapshot, snapshot_digest, compare_snapshots

class RoadSamplesTest(unittest.TestCase):
    def test_digest_matches_signed_blockpos(self):
        # x=-1 -> -2^38, z=0, y=64; second row has negative Y.
        rows = {(-1,64,0):("A",("0:900/lane/0",)),(0,-1,0):("B",("0:900/lane/-1",))}
        expected = hashlib.sha256(f"{-(1<<38)+64}:A\n4095:B\n".encode()).hexdigest()
        self.assertEqual(snapshot_digest(rows),expected)

    def test_exact_difference_and_overlapping_tags(self):
        a = {(1,2,3):("A",("0:900/shoulder/0","0:900/lantern/0"))}
        b = {(1,2,3):("B",a[(1,2,3)][1]),(2,2,3):("C",("0:900/lane/0",))}
        d = compare_snapshots(a,b)
        self.assertEqual(d["count"],2)
        self.assertEqual(d["by_zone"],{"shoulder":1,"lantern":1,"lane":1})
        self.assertEqual(d["by_station"],{"0:900":2})
        self.assertEqual(compare_snapshots(a,a)["count"],0)

    def test_snapshot_rejects_duplicates(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)/"snapshot.tsv"
            row = "1,2,3\tA\t0:900/lane/0\n"
            path.write_text(row)
            self.assertEqual(len(read_snapshot(path)),1)
            path.write_text(row+row)
            with self.assertRaises(ValueError):
                read_snapshot(path)
