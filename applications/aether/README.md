# Aether

HTTP proxy for the [AirGradient](https://www.airgradient.com/) local sensor API. Polls the device on a background virtual thread and serves the latest measurements over a REST+JSON API with Swagger UI.

## Stack

| Concern | Library |
|---|---|
| HTTP server | [Tapir](https://tapir.softwaremill.com) + `tapir-netty-server-sync` (Java 21 virtual threads, direct style) |
| Structured concurrency | [Ox](https://github.com/softwaremill/ox) |
| JSON | Circe 0.14 (`derives Codec.AsObject`) |
| HTTP client | sttp client4 `DefaultSyncBackend` |
| Configuration | PureConfig (Scala 3 `derives ConfigReader`) |
| Logging | Logback |

## How it works

```
┌─────────────────────────────────────────────────┐
│  supervised (Ox scope)                           │
│                                                  │
│  forkDiscard ──► AirGradientPoller               │
│                    while true:                   │
│                      GET /measures/current       │
│                      → MeasuresCache             │
│                      Thread.sleep(pollIntervalMs)│
│                                                  │
│  NettySyncServer (blocks until stopped)          │
│    GET /api/v1/measures/current ──► cache.get    │
│    GET /api/v1/health                            │
│    GET /docs (Swagger UI)                        │
└─────────────────────────────────────────────────┘
```

## Configuration

Environment variables override defaults:

| Variable | Default | Description |
|---|---|---|
| `AIRGRADIENT_BASE_URL` | `http://192.168.1.100` | Device local address |
| `AIRGRADIENT_POLL_INTERVAL_MS` | `30000` | Poll frequency in ms |
| `AIRGRADIENT_TIMEOUT_MS` | `5000` | Request timeout |
| `SERVER_HOST` | `0.0.0.0` | Bind address |
| `SERVER_PORT` | `8080` | Listen port |

## Running

```bash
AIRGRADIENT_BASE_URL=http://192.168.1.42 ./mill applications.aether.run
```

Swagger UI will be available at `http://localhost:8080/docs`.
