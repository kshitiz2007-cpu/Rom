# A04s ROM Studio — Emulator Worker (Phase 2 scaffold)

This worker runs Android Emulator on a **dedicated Linux host with KVM**. Do not deploy it to the ordinary static-site service. The worker exposes an authenticated HTTP API and a WebSocket screen stream.

## Requirements
- Linux host with KVM enabled and `/dev/kvm` available
- Docker, CPU virtualization extensions, sufficient RAM (recommended 8+ GB available to the host)
- Network access to Android SDK packages
- A unique secret `WORKER_TOKEN`

## Build/run
```bash
docker build -t romstudio-emulator-worker .
docker run --rm --device=/dev/kvm -p 8787:8787 -e WORKER_TOKEN='REPLACE_WITH_LONG_RANDOM_SECRET' romstudio-emulator-worker
```

## Endpoints
- `GET /health` — basic worker health, no secret
- `GET /api/status` — status (Bearer token)
- `POST /api/start` — launch configured AVD (Bearer token)
- `POST /api/stop` — stop emulator (Bearer token)
- `WS /stream` — authenticated WebSocket; send `{"type":"stream-start"}` to receive binary PNG frames; send JSON `{"type":"input","action":"keyevent","key":"3"}`, `tap`, `swipe`, or `text` messages for ADB input.

## Important current limitations
- This is an emulator-worker scaffold, not a complete session manager.
- WebSocket currently sends PNG screencaps; it is not WebRTC and has no client-side decoder/renderer wired into ROM Studio yet.
- The AVD is a generic Android 35 virtual device. A Samsung Galaxy A04s device ROM/image cannot simply be booted as this AVD. A compatible emulator-target image is required.
- ROM uploads/install, isolated multi-user sessions, queueing, logcat collection, automated tests, and device-specific compatibility checks are not implemented.
- Keep the worker behind HTTPS/WSS and a strong secret. Do not expose unauthenticated ADB or emulator ports to the public internet.
- Emulator success is not a guarantee of safe flashing on real hardware.
