#!/usr/bin/env bash
set -euo pipefail
echo "ROM Studio Emulator Worker host preflight"
if [[ ! -e /dev/kvm ]]; then echo "ERROR: /dev/kvm not found. Enable virtualization/KVM."; exit 1; fi
if ! command -v docker >/dev/null; then echo "ERROR: Docker is not installed."; exit 1; fi
if ! docker compose version >/dev/null 2>&1; then echo "ERROR: Docker Compose plugin is unavailable."; exit 1; fi
if [[ ! -r /dev/kvm || ! -w /dev/kvm ]]; then echo "WARNING: Current user may not have read/write access to /dev/kvm."; fi
echo "Preflight passed: KVM device and Docker Compose detected."
echo "Next: copy .env.example to .env, set WORKER_TOKEN, then run docker compose up --build."
