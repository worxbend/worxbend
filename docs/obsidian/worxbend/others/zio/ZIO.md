# What is IO?
---
`class IO(thunk: Thunk[A])` , thunk = statement or function without parameters

---

```mermaid
flowchart
	scala{{Scala}} --> twitter{{Twitter Future}}
	twitter --> scala-future{{Scala Future}}
	scala-future --> Akka
	haskell --> scalaz
	scala-future --> scalaz{{ScalaZ: from Haskell to Scala, low-performant and toxic community }}
	scalaz --> cats{{cats: Better than scalaz and non-toxic community }}
	scalaz --> scalaz-stream
	scalaz-stream --> cats
	cats --> fs2
	scalaz-stream --> fs2
```

---
# ZIO = Zero-dependencies IO
---
## High-performance

Build scalable applications with 100x the performance of Scala’s Future

---
### Asynchronous, Type-safe, Concurrent,  Resource-safe, Resilient, Concurrent

Write sequential code that looks the same whether it’s asynchronous or synchronous
Build apps that never leak resources (including threads!), even when they fail
Build apps that never lose errors, and which respond to failure locally and flexibly
Rapidly compose solutions to complex problems from simple building blocks


---
The `ZIO[R, E, A]` data type has three type parameters:

---
-   **`R` - Environment Type**, **Input**. The effect requires an environment of type `R`. If this type parameter is `Any`, it means the effect has no requirements. **ZIO Effect Tracking**
---
-   **`E` - Failure Type**. The effect may fail with a value of type `E`. Some applications will use `Throwable`. If this type parameter is `Nothing`, it means the effect cannot fail, because there are no values of type `Nothing`.
---
-   **`A` - Success Type** **Ouput**. The effect may succeed with a value of type `A`. If this type parameter is `Unit`, it means the effect produces no useful information, while if it is `Nothing`, it means the effect runs forever (or until failure).
---
# Runtime
A `Runtime[R]` is capable of executing tasks (`ZIO`) within an environment `R`.
  -  has two underlying exec contexts: non-blocking IO, blocking IO
  -  spawns concurrent fiber
  -  handles unexpected errors


---
ZIO types:

```
type IO[+E, +A] = ZIO[Any, E, A]
```
- *Succeed with an `A`, may fail with `E` , no requirements.* 
---
```
type Task[+A] = ZIO[Any, Throwable, A] 
```
- *Succeed with an `A`, may fail with `Throwable`, no requirements.*
---
```
type RIO[-R, +A] = ZIO[R, Throwable, A]
```
- *Succeed with an `A`, may fail with `Throwable`, requires an `R`.*
---
```
type URIO[-R, +A] = ZIO[R, Nothing, A]
```
- - *Succeed with an `A`, cannot fail*
---
```
type UIO[+A] = ZIO[Any, Nothing, A]
```
 - Succeed with an `A`, cannot fail , no requirements.  
---
# ZIO: Context propagation 

```scala
for {
  _ <- doSomething().provide(...)
}

def doSomething() = for {
	service <- ZIO.service[Context] // access specific dependency in the environment of the effect
	_ <- service.process(...)
}
```
---
```scala
for {
  _ <- handleRequest().provide(ZLayer.succeed(requestCtx))
}
```
---
https://github.com/lotusflare/lfscala/pull/6217
---
# ZIO Ecosystem

Native Logging
Built-in Metrics
DI: Module Pattern ?

---

# Lets write simple application

---

```scala
"dev.zio" %% "zio" % "2.0.13",  
"dev.zio" %% "zio-streams" % "2.0.13",  
"dev.zio" %% "zio-config" % "4.0.0-RC14",  
"dev.zio" %% "zio-config-magnolia" % "4.0.0-RC14",  
"dev.zio" %% "zio-config-typesafe" % "4.0.0-RC14",  
"dev.zio" %% "zio-config-refined" % "4.0.0-RC14",  
"dev.zio" %% "zio-http" % "3.0.0-RC1", //"0.0.5",
"dev.zio" %% "zio-json" % "0.5.0",  
"dev.zio" %% "zio-logging" % "2.1.12",  
"dev.zio" %% "zio-metrics-connectors" % "2.0.8",
```

---
