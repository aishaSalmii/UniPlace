"""A local stand-in for an OpenAI-compatible AI provider, for black-box tests of the
UniPlace AI Assistant without a real API key.

It listens on http://127.0.0.1:9999/v1/chat/completions, records every request the
Portal sends (so the exact prompt can be inspected), and answers in one of these modes:

  capture  answer at once with a valid letter review            (AI-03, AI-07, AI-10)
  slow     wait 30 seconds, then answer with a valid review      (AI-05: 10-second timeout)
  invalid  answer at once with 6 strengths and a score of 150    (AI-06: validation)

Usage (from the repository root; stop it with Ctrl+C):
    python tests/acceptance/fake_ai_provider.py capture        (Windows: py ...)
Portal Settings: AI provider "Other OpenAI-compatible endpoint",
    AI provider base URL http://127.0.0.1:9999/v1, API key test-key, AI model name fake-model-1.
Every request and answer is appended to tests/evidence/offers/ai-fake-provider-<mode>.jsonl.

These results come from a fake provider, not from a real model.
AI-assisted (Claude); reviewed and executed by Areeg AlGhazali.
"""
import json
import sys
import time
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

MODE = sys.argv[1] if len(sys.argv) > 1 else "capture"
ROOT = Path(__file__).resolve().parents[2]
LOG = ROOT / "tests" / "evidence" / "offers" / f"ai-fake-provider-{MODE}.jsonl"

VALID = {"score": 72, "strengths": ["Describes a concrete Java and Spring project."],
         "gaps": ["No example of Docker use."], "improvements": ["Add one measurable result."]}
INVALID = {"score": 150, "strengths": ["s1", "s2", "s3", "s4", "s5", "s6"],
           "gaps": ["g1"], "improvements": ["i1"]}


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        received = datetime.now()
        body = self._read_body().decode("utf-8")
        try:
            request = json.loads(body)
        except json.JSONDecodeError:
            request = {"raw": body}
        if MODE == "slow":
            time.sleep(30)
        answer = INVALID if MODE == "invalid" else VALID
        reply = {"id": "fake-1", "object": "chat.completion", "model": "fake-model-1",
                 "choices": [{"index": 0, "finish_reason": "stop",
                              "message": {"role": "assistant", "content": json.dumps(answer)}}]}
        try:
            data = json.dumps(reply).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            delivered = True
        except (BrokenPipeError, ConnectionResetError):
            delivered = False
        answered = datetime.now()
        record = {"mode": MODE, "path": self.path, "receivedAt": received.isoformat(timespec="milliseconds"),
                  "answeredAt": answered.isoformat(timespec="milliseconds"),
                  "secondsUntilAnswer": round((answered - received).total_seconds(), 1),
                  "answerDelivered": delivered, "authHeaderPresent": bool(self.headers.get("Authorization")
                                                                          or self.headers.get("api-key")),
                  "request": request, "answer": answer}
        LOG.parent.mkdir(parents=True, exist_ok=True)
        with LOG.open("a", encoding="utf-8") as f:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")
        prompt = " ".join(m.get("content", "") for m in request.get("messages", [])) if isinstance(request, dict) else ""
        print(f"[{received:%H:%M:%S}] {self.path} model={request.get('model')} "
              f"temperature={request.get('temperature')} answered after "
              f"{record['secondsUntilAnswer']} s, delivered={delivered}", flush=True)
        for line in prompt.splitlines():
            if line.startswith("Student:"):
                print("   prompt line:", line, flush=True)

    def _read_body(self):
        if self.headers.get("Content-Length"):
            return self.rfile.read(int(self.headers["Content-Length"]))
        data = b""
        if "chunked" in self.headers.get("Transfer-Encoding", "").lower():
            while True:
                size = int(self.rfile.readline().strip().split(b";")[0] or b"0", 16)
                if size == 0:
                    self.rfile.readline()
                    break
                data += self.rfile.read(size)
                self.rfile.readline()
        return data

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    if MODE not in ("capture", "slow", "invalid"):
        raise SystemExit("mode must be capture, slow or invalid")
    print(f"Fake AI provider ({MODE}) on http://127.0.0.1:9999/v1 - recording to {LOG}", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 9999), Handler).serve_forever()