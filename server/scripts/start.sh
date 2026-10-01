#!/usr/bin/env bash
# Runs the server with automatic restart after crashes (not after a clean /stop).
cd "$(dirname "$0")"
while true; do
  ./run.sh --nogui
  code=$?
  if [[ $code -eq 0 ]]; then echo "Server stopped cleanly."; break; fi
  echo "Server exited with code $code - restarting in 10s (Ctrl+C to abort)"
  sleep 10
done
