# Scala and macro maintenance

Scala 3.10.0. Latest stable Next release, checked 2026-10-20. Both libraries use the same compiler.
Magnolia 1.3.24. ScalaTest 3.2.20. JMH 1.37, benchmark module only.

## Compiler contract

Consumer compiler: Scala 3.10.0 or newer. Older compilers cannot read these newer TASTy files.
`-Wunused:all -deprecation -feature -Werror` applies to production and tests.
Reveal tests add `-Xcheck-macros`; external consumer checks also use `-Ycheck:all`.

Renovate tracks stable Scala 3 releases across minor versions. No LTS pin. No automatic merge.
Upgrade both libraries together; run tests, external consumer checks, POM checks, and benchmarks.

## Macro rules

- Typed symbols first. Use `TypeRepr.of`, `memberType`, and instantiated sealed-child types.
- Quotes belong to their scope. Helpers receiving expressions from nested splices need that splice's `Quotes`.
- `Expansion` retains a context for path-dependent reflection types. Do not pass nested expressions through it casually.
- `.asTerm` can hide scope mistakes from macro checks. Keep external compiler-tree checks too.
- Configuration evaluates once, then root input once. Singleton expressions obey the same rule.
- Excluded and redacted fields stay unread. Never evaluate a field just to learn its type or test null.
- Resolution: recursive self, supplied instance, scalar, wrapper, structural shape, opaque fallback.
- Supplied instances win for scalars and singleton branches. Delegation ends expansion at that boundary.
- Wrapper emission spends the layer budget before recursing, including scalar-ending chains.
- Reachability uncertainty means refusal. Known containers expose payloads; sealed families expose applied children.
- Container subtypes retain their own type arguments and sealed children, even when their element type is safe.
- Only the exact standard Vector symbol skips child scanning; its element type is still checked. Recheck this
  closed implementation assumption on Scala upgrades.
- Open container interfaces behind ordinary nested `toString` are refused. Direct structural rendering still works.
- Unresolvable children report the enclosing fallback boundary as the place to provide an instance.
- Reuse completed payload scans when inspecting siblings and sealed children; nested List/Option branches must not
  multiply work before the depth limit can refuse a growing type graph.

Concrete non-final opaque classes remain supported for Java interoperability. Their runtime subclasses can
expose secrets through `toString`. Use an explicit safe instance or `@Excluded` for such fields.
Custom renderers and custom `toString` implementations own their output. Redaction is not a sandbox.

## Verify

Run from repository root:

```bash
./mill --no-server 'libs.commons.{reveal,pretty-printo}.test'
./mill --no-server libs.commons.reveal.checkConsumer
./mill --no-server 'libs.commons.{reveal,pretty-printo}.checkFormat'
./mill --no-server 'libs.commons.{reveal,pretty-printo}.test.checkFormat'
./mill --no-server 'libs.commons.{reveal,pretty-printo}.pom'
```

`checkConsumer` needs Python 3 and the build JDK. It starts fresh Scala compilers at `-Xss1m`.
Checks: 80 wrappers without derivation compile; 20 wrappers derive; 21 and 80 wrappers refuse with the
layer diagnostic. A sealed container with an unrelated annotated child field must fail compilation.
An argument-growing recursive type must reach the scan-depth refusal without exhausting the compiler stack.
Runtime checks cover singleton effects, exception propagation, nested redaction,
and custom sealed-singleton dispatch. Scratch output is temporary; failures fail the command.

Timing and allocation runs: [benchmark instructions](../renderer-benchmarks/README.md).
Compare the same compiler, JDK, heap, fixtures, and JMH settings. No cross-version speed claims from smoke runs.

## Official references

- [Scala releases](https://www.scala-lang.org/download/): current Next and LTS versions.
- [Quoted code](https://docs.scala-lang.org/scala3/guides/macros/quotes.html): staging and expression composition.
- [Reflection](https://docs.scala-lang.org/scala3/guides/macros/reflection.html): symbol APIs, context ownership, compiler checks.
- [Macro reference](https://docs.scala-lang.org/scala3/reference/metaprogramming/macros.html): scope and staging rules.
- [Macro best practices](https://docs.scala-lang.org/scala3/guides/macros/best-practices.html): avoid brittle tree inspection.
- [Magnolia 1.3.24](https://github.com/softwaremill/magnolia/releases/tag/scala3-v1.3.24): Scala 3.10 compatibility changes.
