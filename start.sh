#!/usr/bin/env bash
# Starts the UniPlace Engine (127.0.0.1:8081) and the UniPlace Portal (127.0.0.1:8080).
#   ./start.sh          start with the existing data, Portal built from source with the Maven wrapper
#   ./start.sh --jar    start the prebuilt Portal (bin/uniplace-portal.jar): nothing is downloaded
#   ./start.sh --reset  delete the Portal data and load the demonstration data again (combine with --jar if needed)
set -u
cd "$(dirname "$0")"
ROOT="$(pwd)"

USE_JAR=0
for arg in "$@"; do
  case "$arg" in
    --reset) rm -rf portal/data; echo "Portal data deleted; the demonstration data will be loaded again." ;;
    --jar) USE_JAR=1 ;;
    *) echo "Unknown option: $arg" >&2; exit 2 ;;
  esac
done

if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 or later is required (java was not found on the PATH)." >&2
  exit 1
fi

engine_up() {
  if command -v curl >/dev/null 2>&1; then
    curl -sf -o /dev/null http://127.0.0.1:8081/api/v1/health
  else
    grep -q "listening" logs/engine.log 2>/dev/null
  fi
}

mkdir -p logs
ENGINE_PID=""
if engine_up; then
  echo "An Engine is already answering on 127.0.0.1:8081; using it."
else
  java -jar engine/uniplace-engine.jar > logs/engine.log 2>&1 &
  ENGINE_PID=$!
  for _ in $(seq 1 30); do
    if engine_up || ! kill -0 "$ENGINE_PID" 2>/dev/null; then break; fi
    sleep 1
  done
  if ! engine_up; then
    echo "The Engine did not start; see logs/engine.log." >&2
    exit 1
  fi
  echo "Engine running on http://127.0.0.1:8081/api/v1 (log: logs/engine.log)"
fi
trap '[ -n "$ENGINE_PID" ] && kill "$ENGINE_PID" 2>/dev/null; exit 0' EXIT INT TERM

cd portal
if [ "$USE_JAR" = 1 ]; then
  if [ ! -f "$ROOT/bin/uniplace-portal.jar" ]; then
    echo "bin/uniplace-portal.jar not found: it is not kept in Git; copy it from the UniPlace package, or start without --jar."
    exit 1
  fi
  echo "Starting the prebuilt Portal on http://127.0.0.1:8080 ..."
  java -jar "$ROOT/bin/uniplace-portal.jar"
else
  echo "Starting the Portal on http://127.0.0.1:8080 ... the first start downloads dependencies and takes a few minutes."
  ./mvnw -q spring-boot:run
fi
