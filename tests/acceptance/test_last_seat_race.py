"""OFF-04: two students accept offers for the last seat at the same moment.

Black-box concurrency test against the running Portal (fresh demonstration data).
Posting 8 "Sustainability Reporting Intern" has 1 seat and two pending offers:
application 7 (student.ibrahim) and application 8 (student.mubarak).
Both students are logged in first; then both Accept requests are released together
with a barrier. Expected: exactly one acceptance succeeds, the other is refused with
an explanation, and the posting never has a negative number of free seats.

Usage (Portal running on 127.0.0.1:8080, started with --reset, posting 8 untouched):
    python tests/acceptance/test_last_seat_race.py          (Windows: py ...)
Each run needs fresh data, because the first run uses the seat.

AI-assisted (Claude); reviewed and executed by Areeg AlGhazali.
"""
import html
import http.cookiejar
import json
import re
import sys
import threading
import time
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

BASE = "http://127.0.0.1:8080"
PASSWORD = "Uniplace!2026"
ROOT = Path(__file__).resolve().parents[2]
CANDIDATES = [("student.ibrahim", 7), ("student.mubarak", 8)]


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Session:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.follow = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.nofollow = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar), NoRedirect)

    def get(self, path):
        with self.follow.open(BASE + path, timeout=30) as r:
            return r.read().decode("utf-8")

    def post(self, path, data):
        req = urllib.request.Request(BASE + path, data=urllib.parse.urlencode(data).encode())
        try:
            with self.nofollow.open(req, timeout=30) as r:
                return r.status, r.headers.get("Location", "")
        except urllib.error.HTTPError as e:
            return e.code, e.headers.get("Location", "")


def csrf(page):
    m = re.search(r'name="_csrf" value="([^"]+)"', page)
    if not m:
        raise SystemExit("No CSRF token found: is the Portal running?")
    return m.group(1)


def login(user):
    s = Session()
    status, loc = s.post("/login", {"username": user, "password": PASSWORD, "_csrf": csrf(s.get("/login"))})
    if "error" in loc or "/login" in loc:
        raise SystemExit(f"Login failed for {user}")
    return s


def flash(page):
    m = re.search(r'<div class="flash (ok|bad)">(.*?)</div>', page, re.S)
    if not m:
        return (None, None)
    text = html.unescape(re.sub(r"<[^>]+>", " ", m.group(2)))
    return (m.group(1), " ".join(text.split()))


def badge(page):
    m = re.search(r'class="badge [a-z]*">([A-Za-z]+)<', page)
    return m.group(1) if m else None


def posting_seats(s):
    page = s.get("/postings/8")
    text = re.sub(r"<[^>]+>", " ", page)
    m = re.search(r"Seats\s+(\d+)\s*\((-?\d+) left\)", text)
    return {"status": badge(page), "seats": int(m.group(1)) if m else None,
            "left": int(m.group(2)) if m else None}


def main():
    coordinator = login("coordinator")
    before = posting_seats(coordinator)
    sessions, prepared = [], []
    for user, app_id in CANDIDATES:
        s = login(user)
        page = s.get(f"/applications/{app_id}")
        m = re.search(r'action="(/offers/\d+/accept)"', page)
        if not m:
            raise SystemExit(f"No pending offer to accept on application {app_id}: restart with --reset.")
        sessions.append(s)
        prepared.append((user, app_id, m.group(1), csrf(page)))

    barrier = threading.Barrier(len(prepared))
    results = [None] * len(prepared)

    def accept(i):
        user, app_id, url, token = prepared[i]
        barrier.wait()
        sent = datetime.now().isoformat(timespec="milliseconds")
        status, loc = sessions[i].post(url, {"_csrf": token})
        results[i] = {"user": user, "application": app_id, "sentAt": sent, "httpStatus": status, "redirect": loc}

    threads = [threading.Thread(target=accept, args=(i,)) for i in range(len(prepared))]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    for i, (user, app_id, _, _) in enumerate(prepared):
        page = sessions[i].get(f"/applications/{app_id}")
        kind, message = flash(page)
        results[i].update({"message": message, "messageKind": kind, "stateAfter": badge(page)})
    time.sleep(0.5)
    after = posting_seats(coordinator)

    accepted = [r for r in results if r["stateAfter"] == "Accepted"]
    refused = [r for r in results if r["stateAfter"] != "Accepted"]
    checks = {
        "exactlyOneAccepted": len(accepted) == 1,
        "otherRefusedWithExplanation": len(refused) == 1 and refused[0]["messageKind"] == "bad"
                                       and bool(refused[0]["message"]),
        "seatsNeverNegative": after["left"] is not None and after["left"] >= 0,
        "postingFilled": after["status"] == "Filled",
    }
    verdict = "PASS" if all(checks.values()) else "FAIL"
    record = {"test": "OFF-04 last-seat race", "runAt": datetime.now().isoformat(timespec="seconds"),
              "postingBefore": before, "postingAfter": after, "requests": results,
              "checks": checks, "verdict": verdict}

    out_dir = ROOT / "tests" / "evidence" / "offers"
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / f"last-seat-race-{datetime.now():%Y%m%d-%H%M%S}.json"
    out.write_text(json.dumps(record, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(record, indent=2, ensure_ascii=False))
    print(f"\nVerdict: {verdict}   (saved to {out})")
    sys.exit(0 if verdict == "PASS" else 1)


if __name__ == "__main__":
    main()