"""OFF-05: allowance boundaries checked against an independent calculation.

Black-box tests of the Engine POST /api/v1/allowance. Each expected value is computed
in this file from the rules in OFF-05 and engine/openapi.yaml (not from the Engine):
  months = whole calendar months (start and end inclusive) + remaining days / 30;
           a month runs from a day to the day before the same day of the next month,
           or to the end of that next month when it has no such day
  base = monthly x months; transport = 300 x months when weeklyHours >= 30
  cap adjustment = -(monthly + monthly transport - 5000) x months when positive
  penalty = -subtotal x min(20%, 5% x started weeks of lateDays)
  only the total is rounded, to 2 decimals, halves up

Usage (Engine running on 127.0.0.1:8081, e.g. after start.cmd / start.sh):
    python tests/acceptance/test_allowance_boundaries.py      (Windows: py ...)
Writes tests/evidence/offers/allowance-boundaries-<time>.json with every request,
response and expected calculation.

AI-assisted (Claude); reviewed and executed by Areeg AlGhazali.
"""
import calendar
import json
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from fractions import Fraction
from pathlib import Path

URL = "http://127.0.0.1:8081/api/v1/allowance"
ROOT = Path(__file__).resolve().parents[2]
SETTINGS = dict(transportSupplement=300, monthlyCap=5000, penaltyRate=0.05, penaltyCap=0.20)


def month_end(start, k):
    """Last day of the k-th month that begins on start (OpenAPI rule)."""
    y, m = divmod(start.month - 1 + k, 12)
    y, m = start.year + y, m + 1
    last = calendar.monthrange(y, m)[1]
    if start.day <= last:
        return date(y, m, start.day) - timedelta(days=1)
    return date(y, m, last)


def months(start, end):
    whole, covered = 0, start - timedelta(days=1)
    while month_end(start, whole + 1) <= end:
        whole += 1
        covered = month_end(start, whole)
    remaining = (end - covered).days
    return Fraction(whole) + Fraction(remaining, 30), whole, remaining


def expected(p):
    m, whole, rest = months(date.fromisoformat(p["startDate"]), date.fromisoformat(p["endDate"]))
    monthly = Fraction(Decimal(str(p["monthlyAllowance"])))
    t = Fraction(SETTINGS["transportSupplement"]) if p["weeklyHours"] >= 30 else Fraction(0)
    base, transport = monthly * m, t * m
    over = monthly + t - SETTINGS["monthlyCap"]
    cap = -over * m if over > 0 else Fraction(0)
    subtotal = base + transport + cap
    weeks = -(-p["lateDays"] // 7)
    rate = min(Fraction(Decimal(str(SETTINGS["penaltyCap"]))), Fraction(Decimal(str(SETTINGS["penaltyRate"]))) * weeks)
    penalty = -subtotal * rate
    exact = subtotal + penalty
    total = (Decimal(exact.numerator) / Decimal(exact.denominator)).quantize(Decimal("0.01"), ROUND_HALF_UP)
    show = lambda f: float(round(Decimal(f.numerator) / Decimal(f.denominator), 4))
    return {"months": f"{whole} + {rest}/30", "base": show(base), "transport": show(transport),
            "capAdjustment": show(cap), "startedWeeks": weeks, "penaltyRate": float(rate),
            "penalty": show(penalty), "total": str(total),
            "expectTransportLine": transport != 0, "expectCapLine": cap != 0, "expectPenaltyLine": penalty != 0}


def req(monthly=3000, start="2027-01-01", end="2027-01-31", hours=29, late=0):
    return dict(monthlyAllowance=monthly, startDate=start, endDate=end, weeklyHours=hours, lateDays=late, **SETTINGS)


CASES = [
    # id, boundary, request
    ("A01", "transport: 29 weekly hours (below threshold)", req(hours=29)),
    ("A02", "transport: 30 weekly hours (on threshold)", req(hours=30)),
    ("A03", "transport: 31 weekly hours (above threshold)", req(hours=31)),
    ("A04", "cap: 4600 + 300 = 4900 per month (below cap)", req(monthly=4600, hours=31)),
    ("A05", "cap: 4700 + 300 = 5000 per month (equal to cap)", req(monthly=4700, hours=31)),
    ("A06", "cap: 4900 + 300 = 5200 per month (above cap)", req(monthly=4900, hours=31)),
    ("A07", "cap: 5000 without transport (equal to cap)", req(monthly=5000, hours=29)),
    ("A08", "cap: 5000 + 300 = 5300 per month (above cap, maximum input)", req(monthly=5000, hours=40)),
    ("A09", "duration: 1 Jan to 31 Jan = 1 whole month", req(start="2027-01-01", end="2027-01-31")),
    ("A10", "duration: 1 Jan to 15 Feb = 1 month + 15 days", req(start="2027-01-01", end="2027-02-15")),
    ("A11", "duration: 31 Jan to 28 Feb (short next month) = 1 month", req(start="2027-01-31", end="2027-02-28")),
    ("A12", "duration: 15 Jan to 14 Apr = 3 whole months", req(start="2027-01-15", end="2027-04-14")),
    ("A13", "duration: 1 Jan to 10 Jan = 10 days only", req(start="2027-01-01", end="2027-01-10")),
    ("A14", "duration: 1 Feb to 28 Feb 2027 (non-leap) = 1 month", req(start="2027-02-01", end="2027-02-28")),
    ("A15", "duration: 1 Feb to 29 Feb 2028 (leap) = 1 month", req(start="2028-02-01", end="2028-02-29")),
    ("A16", "duration: 30 Jan to 1 Mar = 1 month + 1 day", req(start="2027-01-30", end="2027-03-01")),
    ("A17", "penalty: 0 late days", req(late=0)),
    ("A18", "penalty: 1 late day = 1 started week (5%)", req(late=1)),
    ("A19", "penalty: 7 late days = 1 started week (5%)", req(late=7)),
    ("A20", "penalty: 8 late days = 2 started weeks (10%)", req(late=8)),
    ("A21", "penalty: 28 late days = 4 weeks (20%, at cap)", req(late=28)),
    ("A22", "penalty: 29 late days = 5 weeks (capped at 20%)", req(late=29)),
    ("A23", "penalty: 100 late days (capped at 20%)", req(late=100)),
    ("A24", "rounding: 1000.05 x 3/30 = 100.005 -> 100.01 (half up)", req(monthly=1000.05, start="2027-01-01", end="2027-01-03")),
    ("A25", "rounding: 1000 x 1/30 = 33.333... -> 33.33", req(monthly=1000, start="2027-01-01", end="2027-01-01")),
    ("A26", "rounding: 3333.33 with 5% penalty = 3166.6635 -> 3166.66", req(monthly=3333.33, late=1)),
    ("A27", "rounding: 1000.25 x 3/30 = 100.025 -> 100.03 (half up)", req(monthly=1000.25, start="2027-01-01", end="2027-01-03")),
    ("A28", "duration: 31 Mar to 30 Apr (short next month) = 1 month", req(start="2027-03-31", end="2027-04-30")),
]


def call(body):
    r = urllib.request.Request(URL, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(r, timeout=10) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"{}")


def has_line(lines, *words):
    return any(any(w in l["label"].lower() for w in words) and Decimal(str(l["amount"])) != 0 for l in lines)


def main():
    results, failed = [], 0
    for cid, boundary, body in CASES:
        exp = expected(body)
        status, resp = call(body)
        problems = []
        if status != 200:
            problems.append(f"HTTP {status}")
        else:
            actual = Decimal(str(resp["total"])).quantize(Decimal("0.01"))
            if actual != Decimal(exp["total"]):
                problems.append(f"total {actual} != expected {exp['total']}")
            lines = resp.get("lines", [])
            if exp["expectTransportLine"] != has_line(lines, "transport"):
                problems.append("transport line " + ("missing" if exp["expectTransportLine"] else "unexpected"))
            if exp["expectCapLine"] != has_line(lines, "cap"):
                problems.append("cap adjustment line " + ("missing" if exp["expectCapLine"] else "unexpected"))
            if exp["expectPenaltyLine"] != has_line(lines, "penalty", "late"):
                problems.append("penalty line " + ("missing" if exp["expectPenaltyLine"] else "unexpected"))
        verdict = "PASS" if not problems else "FAIL"
        failed += verdict == "FAIL"
        results.append({"id": cid, "boundary": boundary, "request": body, "expected": exp,
                        "httpStatus": status, "response": resp, "verdict": verdict, "problems": problems})
        got = resp.get("total") if isinstance(resp, dict) else None
        print(f"{cid}  {verdict}  expected {exp['total']:>9}  actual {str(got):>9}  {boundary}"
              + (f"  <-- {'; '.join(problems)}" if problems else ""))

    out_dir = ROOT / "tests" / "evidence" / "offers"
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / f"allowance-boundaries-{datetime.now():%Y%m%d-%H%M%S}.json"
    out.write_text(json.dumps({"test": "OFF-05 allowance boundaries", "url": URL,
                               "runAt": datetime.now().isoformat(timespec="seconds"),
                               "passed": len(CASES) - failed, "failed": failed, "cases": results},
                              indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"\n{len(CASES)} cases: {len(CASES) - failed} passed, {failed} failed   (saved to {out})")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()