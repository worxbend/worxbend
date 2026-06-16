# airgradient-proxy

An HTTP caching proxy for the [AirGradient](https://www.airgradient.com/) local server API.

## Purpose

AirGradient IoT sensors expose a local HTTP API at `/measures/current`. The device can handle only a limited number of concurrent connections. This proxy polls the upstream device once every 5 seconds and serves all client requests from its in-memory cache — the device is never called directly from client traffic.

## Architecture

```
AirGradient device
        |
        | (poll every 5s)
        v
  airgradient-proxy   <---  many clients
        |
        | (serve from cache, O(1) per request)
        v
     cached snapshot
```

**Stacked topology** (proxy-to-proxy):

```
AirGradient device
        |
        v
  proxy-A (polls device, cache TTL 10s)
        |
        v
  proxy-B (polls proxy-A, cache TTL 30s)
        |
        v
      clients
```

The JSON body from `/measures/current` is passed through unchanged at every hop.

## Configuration

Edit `config/application.conf`:

```hocon
airgradient-proxy {
  http {
    host = "0.0.0.0"
    port = 8080
  }

  upstream {
    base-url = "http://airgradient_ecda3b1eaaaf.local"
    connect-timeout = 500ms
    request-timeout = 1s
  }

  polling {
    interval = 5s
    initial-fetch = true
    jitter = 250ms
    max-backoff = 60s
  }

  cache {
    fresh-ttl = 10s
    stale-ttl = 5m
    expired-after = 30m
    serve-expired = false
  }

  endpoints {
    config-get-mode = "cached"
    config-put-mode = "disabled"
  }

  logging {
    json = true
    level = "info"
  }
}
```

## Running locally with Mill

```bash
# Compile
./mill applications.airgradient-proxy.compile

# Run tests
./mill applications.airgradient-proxy.test

# Package and run (requires assembled jar)
./mill applications.airgradient-proxy.assembly
java -Dconfig.file=applications/airgradient-proxy/config/application.conf \
     -jar out/applications/airgradient-proxy/assembly.dest/out.jar
```

## Running with Docker

```bash
# Build image (after assembling the jar)
./mill applications.airgradient-proxy.assembly
cp out/applications/airgradient-proxy/assembly.dest/out.jar \
   applications/airgradient-proxy/airgradient-proxy.jar

docker compose -f applications/airgradient-proxy/docker/compose.yaml up
```

## Public endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/measures/current` | GET | Returns cached AirGradient sensor data as JSON |

### Response headers for `/measures/current`

| Header | Values | Description |
|---|---|---|
| `X-AirGradient-Proxy` | `true` | Marks the response as served by this proxy |
| `X-AirGradient-Proxy-Cache-Status` | `fresh`, `stale`, `expired`, `empty` | Freshness of the cached data |
| `X-AirGradient-Proxy-Cache-Age-Millis` | number | Age of the cached snapshot in milliseconds |

## Internal endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/_proxy/health` | GET | Returns 200 if process is alive |
| `/_proxy/ready` | GET | Returns 200 if at least one snapshot has been cached; 503 otherwise |
| `/_proxy/status` | GET | Returns JSON with internal proxy state |
| `/_proxy/metrics` | GET | Prometheus metrics in text exposition format |
| `/_proxy/schema` | GET | JSON array of documented type names |
| `/_proxy/schema/{typeName}` | GET | JSON Schema (Draft-04) for the named type, or 404 |

### `/_proxy/status` response shape

```json
{
  "upstreamUrl": "http://airgradient_ecda3b1eaaaf.local",
  "lastAttemptAt": "2025-01-01T00:00:00Z",
  "lastSuccessAt": "2025-01-01T00:00:00Z",
  "lastFailureAt": null,
  "consecutiveFailures": 0,
  "lastError": null,
  "cacheAgeMillis": 2341,
  "cacheStatus": "fresh",
  "pollingIntervalMillis": 5000,
  "requestTimeoutMillis": 1000
}
```

## Cache behavior

| Cache status | Age | Action |
|---|---|---|
| `fresh` | ≤ fresh-ttl (10s) | Serve; header `X-AirGradient-Proxy-Cache-Status: fresh` |
| `stale` | ≤ stale-ttl (5m) | Serve; header `stale` |
| `expired` | > expired-after (30m) | Serve if `serve-expired=true`; otherwise 503 |
| `empty` | No snapshot yet | Return 503 |

## Failure behavior

- **Upstream unreachable**: keep serving last known snapshot; increment failure counter.
- **Bad upstream JSON**: reject response; keep previous snapshot; increment failure counter.
- **Repeated failures**: apply exponential backoff (max 60 seconds) between retries.
- **Cache empty on first start**: return 503 until first successful fetch.

## Example curl commands

```bash
# Get current sensor readings
curl http://localhost:8080/measures/current

# Check proxy health
curl http://localhost:8080/_proxy/health

# Check if proxy is ready (has data)
curl http://localhost:8080/_proxy/ready

# Inspect internal proxy state
curl http://localhost:8080/_proxy/status
```

## Stacked proxy example

```bash
# Instance A: polls the real AirGradient device
AIRGRADIENT_BASE_URL=http://airgradient_ecda3b1eaaaf.local \
  java -Dairgradient-proxy.upstream.base-url=http://airgradient_ecda3b1eaaaf.local \
       -Dairgradient-proxy.http.port=8080 \
       -jar airgradient-proxy.jar

# Instance B: polls Instance A
java -Dairgradient-proxy.upstream.base-url=http://localhost:8080 \
     -Dairgradient-proxy.http.port=8081 \
     -jar airgradient-proxy.jar

# Clients call Instance B; device sees only Instance A's polling traffic
curl http://localhost:8081/measures/current
```

## JSON Schema (`/_proxy/schema`)

Schema is generated at startup via `TapirSchemaToJsonSchema` (Draft-04) from Tapir's auto-derived `Schema[A]` for each documented type:

| Type name | Endpoint |
|---|---|
| `AirGradientMeasures` | upstream sensor payload shape (documentation-only; raw bytes are passed through) |
| `ProxyStatus` | `/_proxy/status` response body |

```bash
# list available types
curl http://localhost:8080/_proxy/schema
# ["AirGradientMeasures","ProxyStatus"]

# fetch schema for a specific type
curl http://localhost:8080/_proxy/schema/ProxyStatus
```

All `Option` fields appear as `type: ["T", "null"]` (via `markOptionsAsNullable = true`). Nested types are inlined under `$defs` and referenced with `$ref`.

## Observability

### Prometheus metrics (`/_proxy/metrics`)

Standard Tapir HTTP metrics (prefixed `airgradient_proxy_`):

| Metric | Type | Description |
|---|---|---|
| `airgradient_proxy_request_active` | Gauge | In-flight HTTP requests |
| `airgradient_proxy_request_total` | Counter | Requests by path/method/status |
| `airgradient_proxy_request_duration_seconds` | Histogram | Request latency |

Custom proxy metrics:

| Metric | Type | Labels | Description |
|---|---|---|---|
| `airgradient_proxy_cache_responses_total` | Counter | `status` (fresh/stale/expired/empty/none) | Responses by cache status |
| `airgradient_proxy_upstream_polls_total` | Counter | `result` (success/failure) | Upstream poll outcomes |
| `airgradient_proxy_consecutive_failures` | Gauge | — | Current consecutive failure count |

### OpenTelemetry tracing

Set `OTEL_SERVICE_NAME` (and any additional `OTEL_*` env vars) to enable tracing via the Java OTel SDK autoconfigure mechanism. When `OTEL_SERVICE_NAME` is absent, a noop tracer is used and no spans are emitted.

Context propagation is ThreadLocal-based, which is correct for direct-style / virtual-thread environments (Netty sync + ox).

```bash
OTEL_SERVICE_NAME=airgradient-proxy \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
  java -jar airgradient-proxy.jar
```

## Testing

```bash
./mill applications.airgradient-proxy.test
```

Tests cover:
- Config loading and validation
- Cache state transitions (empty → fresh → stale → expired)
- Freshness classification
- Backoff policy
- JSON validation (valid, invalid, unknown fields preserved)
- Integration tests with fake upstream (WireMock)
- Client fan-out does not increase upstream request count
- Malformed JSON does not evict the previous snapshot
