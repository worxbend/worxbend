# Local benchmark run — 2026-10-20

Measured code: `950312a4c6af5ef3c4f1f74c56b37e346398f849`.
Scala 3.10.0, JMH 1.37, Temurin JDK 25.0.1+8, Linux x86_64, Ryzen 7 5700X (16 logical CPUs).
512 MiB fixed heap, one worker thread. Two forks; three 500 ms warmups; five 500 ms measurements per fork.
GC profiler enabled. All 24 workload combinations completed; all 48 forks succeeded.

Shared host. Concurrent builds kept observed load averages around 27–32. Treat these numbers as a
reproducible workload sample, not a controlled performance baseline or evidence of improvement over an older release.
Timing error below is JMH's 99.9% confidence half-width. Allocation values are bytes per operation.
Null cases measure the short-circuit boundary and can approach harness overhead.

```bash
./mill --no-server libs.commons.renderer-benchmarks.runJmh \
  -f 2 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -rf json -rff results.json -foe true
```

| Benchmark | Size | Multiline | ns/op ± error | B/op |
| --- | ---: | --- | ---: | ---: |
| `printoCollections` | 0 | false | 460.56 ± 83.40 | 1,240.01 |
| `printoCollections` | 1 | false | 976.40 ± 212.21 | 2,036.01 |
| `printoCollections` | 32 | false | 12,332.43 ± 1,960.52 | 23,544.18 |
| `printoCollections` | 1024 | false | 365,075.49 ± 56,526.98 | 725,441.76 |
| `printoCollections` | 0 | true | 475.76 ± 72.55 | 1,476.01 |
| `printoCollections` | 1 | true | 837.52 ± 299.58 | 2,304.01 |
| `printoCollections` | 32 | true | 9,733.95 ± 2,376.31 | 24,748.15 |
| `printoCollections` | 1024 | true | 258,207.03 ± 67,353.65 | 763,271.88 |
| `revealCollections` | 0 | false | 165.48 ± 73.10 | 600.00 |
| `revealCollections` | 1 | false | 560.91 ± 217.50 | 1,352.01 |
| `revealCollections` | 32 | false | 8,619.62 ± 2,697.86 | 22,440.12 |
| `revealCollections` | 1024 | false | 334,855.17 ± 41,556.08 | 713,215.49 |
| `revealCollections` | 0 | true | 291.18 ± 89.60 | 768.00 |
| `revealCollections` | 1 | true | 783.93 ± 134.24 | 1,680.01 |
| `revealCollections` | 32 | true | 9,329.77 ± 2,097.15 | 23,776.13 |
| `revealCollections` | 1024 | true | 251,953.13 ± 91,160.54 | 751,087.84 |
| `printoDelegated` | — | — | 1,526.31 ± 466.77 | 4,576.02 |
| `printoFlat` | — | — | 799.13 ± 150.20 | 1,716.01 |
| `printoNull` | — | — | 1.15 ± 0.19 | 0.00 |
| `printoRedacted` | — | — | 343.84 ± 107.22 | 1,028.01 |
| `revealDelegated` | — | — | 1,079.12 ± 164.92 | 2,844.01 |
| `revealFlat` | — | — | 535.33 ± 110.51 | 1,272.01 |
| `revealNull` | — | — | 1.76 ± 0.29 | 0.00 |
| `revealRedacted` | — | — | 146.94 ± 55.58 | 500.00 |
