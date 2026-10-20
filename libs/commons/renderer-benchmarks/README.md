# Renderer benchmarks

JMH 1.37. Scala 3.10.0, inherited from Reveal. Separate module; neither library publishes benchmark dependencies.

Cases: flat products with escaped strings, redaction, delegated nested products, direct nulls, lists and maps.
Collection sizes: 0, 1, 32, 1024. Single-line and multiline configurations.
Instances and fixtures created outside timed methods. Each worker owns its state. Returned strings keep work observable.

## Run

From repository root:

```bash
./mill --no-server libs.commons.renderer-benchmarks.listJmhBenchmarks
./mill --no-server libs.commons.renderer-benchmarks.runJmh -prof gc -rf json -rff results.json -foe true
```

Defaults: two forked JVMs, five one-second warmups, five one-second measurements, one thread,
512 MiB fixed heap. Average time in ns/op. GC profiler adds bytes/op and collection metrics.
`results.json` lands in `out/libs/commons/renderer-benchmarks/runJmh.dest/`.

Fast wiring check:

```bash
./mill --no-server libs.commons.renderer-benchmarks.runJmh -f 1 -wi 1 -i 1 -w 100ms -r 100ms -p size=1 -p multiline=false -foe true
```

Smoke scores are not performance evidence. Full runs need an idle machine and repeated forks.
Record commit, Scala/JDK versions, CPU, OS, power mode, JMH flags, and raw JSON beside any report.
Compare identical fixtures and configuration. Libraries have distinct semantics; numbers do not establish universal superiority.

Runtime benchmarks exclude compilation and derivation. Reveal's separate `checkConsumer` command verifies
compiler-stack boundaries; it does not measure compiler throughput. No timing threshold in CI: shared runners vary.

[Recorded local run](RESULTS.md): timings, allocations, environment, and limitations.
