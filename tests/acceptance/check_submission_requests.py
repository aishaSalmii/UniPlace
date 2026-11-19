"""M01: two simultaneous submissions followed by one sequential retry.

Uses an existing fictional account and creates application records.
Never resets data. Credentials, cookies and CSRF tokens are not saved.
"""

import concurrent.futures
import copy
import getpass
import http.cookiejar
import json
import re
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path

BASE = "http://127.0.0.1:8080"
POSTING = 8
TITLE = "Sustainability Reporting Intern"
USERNAME = "qa.concurrent"

LETTER = (
    "I am interested in this sustainability reporting internship. "
    "I would like to develop my Excel and writing skills while "
    "supporting your team and learning from practical projects."
)


class Parse(HTMLParser):
    def __init__(self):
        super().__init__()
        self.csrf = None
        self.ids = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)

        if tag == "input" and attrs.get("name") == "_csrf":
            self.csrf = attrs.get("value")

        if tag == "a":
            match = re.fullmatch(
                r"/applications/(\d+)", attrs.get("href", "")
            )
            if match:
                self.ids.append(int(match.group(1)))


def parse(html):
    result = Parse()
    result.feed(html)
    return result


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def opener(jar, redirect=True):
    handlers = [
        urllib.request.ProxyHandler({}),
        urllib.request.HTTPCookieProcessor(jar),
    ]
    if not redirect:
        handlers.append(NoRedirect())
    return urllib.request.build_opener(*handlers)


def fetch(op, path, data=None):
    with op.open(BASE + path, data=data, timeout=20) as response:
        return (
            response.read().decode("utf-8"),
            response.geturl(),
            response.status,
        )


def application_ids(op):
    html, url, status = fetch(op, "/applications")

    if status != 200 or urllib.parse.urlsplit(url).path != "/applications":
        raise ValueError("Applications page unavailable or session expired.")

    return html, sorted(set(parse(html).ids))


def main():
    jar = http.cookiejar.CookieJar()
    op = opener(jar)

    try:
        html, _, _ = fetch(op, "/login")
        token = parse(html).csrf
        if not token:
            raise ValueError("Missing login token.")

        password = getpass.getpass("qa.concurrent password (hidden): ")
        login_data = urllib.parse.urlencode({
            "username": USERNAME,
            "password": password,
            "_csrf": token,
        }).encode()

        fetch(op, "/login", login_data)
        del password, login_data

        html, before = application_ids(op)
        if before or "No applications" not in html:
            raise ValueError(
                "Account is not empty. Do not repeat this test "
                "on existing applications."
            )

        html, url, status = fetch(op, f"/postings/{POSTING}")
        token = parse(html).csrf

        if (
            status != 200
            or urllib.parse.urlsplit(url).path != f"/postings/{POSTING}"
            or not token
            or "apply-form" not in html
            or TITLE not in html
        ):
            raise ValueError("Target application form unavailable.")

    except Exception as error:
        raise SystemExit(f"Preflight stopped: {error}")

    out = Path("evidence") / (
        "m01-http-"
        + datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    )
    out.mkdir(parents=True)

    def save(name, data):
        (out / name).write_text(
            json.dumps(data, indent=2),
            encoding="utf-8",
        )

    save("plan.json", {
        "time_utc": datetime.now(timezone.utc).isoformat(),
        "account": USERNAME,
        "posting_id": POSTING,
        "posting_title": TITLE,
        "before_application_ids": before,
        "method": (
            "Two HTTP POSTs released together using copies of one "
            "authenticated session cookie and the same form CSRF token, "
            "followed by one sequential retry."
        ),
        "limitations": (
            "No forced server-side synchronization. Not a browser "
            "double-click. No actual network timeout or packet loss. "
            "One request pair cannot prove absence of concurrency bugs. "
            "HTTP 405 without CSRF is inconclusive for CSRF protection."
        ),
    })

    payload = urllib.parse.urlencode({
        "letter": LETTER,
        "_csrf": token,
    }).encode()

    def post(label, barrier=None, include_token=True):
        local_jar = http.cookiejar.CookieJar()
        for cookie in jar:
            local_jar.set_cookie(copy.copy(cookie))

        local_op = opener(local_jar, redirect=False)
        body = payload if include_token else urllib.parse.urlencode({
            "letter": LETTER,
        }).encode()

        if barrier:
            barrier.wait(timeout=10)

        start = time.perf_counter()
        record = {
            "label": label,
            "start_utc": datetime.now(timezone.utc).isoformat(),
            "status": None,
            "location": None,
            "error": None,
        }

        try:
            with local_op.open(
                BASE + f"/postings/{POSTING}/apply",
                data=body,
                timeout=20,
            ) as response:
                record["status"] = response.status
                record["location"] = response.headers.get("Location")
                response.read()

        except urllib.error.HTTPError as error:
            record["status"] = error.code
            record["location"] = error.headers.get("Location")
            error.read()

        except Exception as error:
            record["error"] = type(error).__name__

        record["elapsed_ms"] = (time.perf_counter() - start) * 1000
        return record

    # Missing-CSRF precheck: confirm it did not create an application.
    csrf = post("missing-csrf", include_token=False)
    save("csrf-check.json", csrf)

    _, precheck_ids = application_ids(op)

    if csrf["status"] not in (403, 405) or precheck_ids:
        raise SystemExit(
            "Unexpected precheck response or application created. "
            f"Stopped. Evidence: {out}"
        )

    if csrf["status"] == 405:
        csrf_interpretation = (
            "INCONCLUSIVE: missing-CSRF request returned 405 "
            "and created no application; cause unverified."
        )
        print(csrf_interpretation)
    else:
        csrf_interpretation = (
            "Missing-CSRF request returned 403 and created no application."
        )

    # Release two requests together.
    gate = threading.Barrier(2)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        jobs = [
            pool.submit(post, f"concurrent-{number}", gate)
            for number in (1, 2)
        ]
        responses = [job.result(timeout=30) for job in jobs]

    save("concurrent-responses.json", responses)

    _, after = application_ids(op)
    save("after-concurrent.json", {
        "application_ids": after,
        "count": len(after),
    })

    if not after:
        raise SystemExit(
            "No application saved. Eligibility or another refusal "
            f"may block this test. Evidence: {out}"
        )

    # Repeat the same logical request after the original responses.
    retry = post("sequential-retry")
    save("retry-response.json", retry)
    _, final = application_ids(op)

    details = []
    for ident in sorted(set(after + final)):
        html, url, status = fetch(op, f"/applications/{ident}")
        details.append({
            "application_id": ident,
            "detail_page_valid": (
                status == 200
                and urllib.parse.urlsplit(url).path
                == f"/applications/{ident}"
            ),
            "expected_title_visible": TITLE in html,
            "test_name_visible": "QA Concurrency Student" in html,
        })

    identities_valid = all(
        item["detail_page_valid"]
        and item["expected_title_visible"]
        and item["test_name_visible"]
        for item in details
    )

    requests_valid = all(
        response["error"] is None
        and response["status"] in (302, 303)
        for response in responses
    )
    retry_valid = (
        retry["error"] is None
        and retry["status"] in (302, 303)
    )

    if not identities_valid:
        concurrency_verdict = "INCONCLUSIVE: record identity check failed"
    elif len(after) > 1:
        concurrency_verdict = "FAIL: duplicate applications persisted"
    elif not requests_valid:
        concurrency_verdict = "INCONCLUSIVE: unexpected request response"
    else:
        concurrency_verdict = "PASS for this observed request pair only"

    if not identities_valid or not retry_valid:
        retry_verdict = "INCONCLUSIVE: inspect responses and record checks"
    elif final == after:
        retry_verdict = "PASS: retry created no new application"
    else:
        retry_verdict = "FAIL: retry changed the application record set"

    result = {
        "missing_csrf_status": csrf["status"],
        "missing_csrf_interpretation": csrf_interpretation,
        "after_concurrent_ids": after,
        "after_concurrent_count": len(after),
        "concurrent_http_statuses": [
            response["status"] for response in responses
        ],
        "concurrency_observation": concurrency_verdict,
        "after_retry_ids": final,
        "retry_added_record": bool(set(final) - set(after)),
        "retry_record_count_verdict": retry_verdict,
        "record_identity_checks": details,
        "real_timeout_tested": False,
        "browser_double_click_tested": False,
        "evidence_directory": str(out),
    }

    save("summary.json", result)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()