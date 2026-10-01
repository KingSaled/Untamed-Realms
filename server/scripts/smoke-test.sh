#!/usr/bin/env bash
# Boots a freshly installed server, waits for "Done (", then stops it. Fails on crash or timeout.
#   bash server/scripts/smoke-test.sh <server-dir> [timeout-seconds]
set -uo pipefail
DIR="$1"; TIMEOUT="${2:-900}"
cd "$DIR"
sed -i 's/^-Xm[sx].*$//' user_jvm_args.txt
printf -- "-Xms4G\n-Xmx6G\n" >> user_jvm_args.txt
rm -f smoke.in; mkfifo smoke.in
( ./run.sh --nogui < smoke.in > smoke.out 2>&1; echo "exit=$?" >> smoke.out ) &
exec 3> smoke.in
start=$(date +%s)
while true; do
  if grep -q 'Done (' smoke.out 2>/dev/null; then
    echo ">> Server started in $(( $(date +%s) - start ))s - stopping"; echo "stop" >&3; break
  fi
  if grep -q '^exit=' smoke.out 2>/dev/null; then
    echo ">> Server exited before finishing startup"; tail -n 120 smoke.out; exit 1
  fi
  if (( $(date +%s) - start > TIMEOUT )); then
    echo ">> Timed out after ${TIMEOUT}s"; tail -n 120 smoke.out; exit 1
  fi
  sleep 5
done
for _ in $(seq 1 60); do grep -q '^exit=' smoke.out && break; sleep 2; done
exec 3>&-
grep -E 'ERROR|Exception' smoke.out | grep -v 'DEBUG' | head -n 60 || true
if ls crash-reports/*.txt > /dev/null 2>&1; then echo ">> Crash report produced"; exit 1; fi
grep -q '^exit=0' smoke.out || { echo ">> Non-zero exit"; tail -n 60 smoke.out; exit 1; }
echo ">> Smoke test passed"
