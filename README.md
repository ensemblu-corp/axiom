![Version](https://img.shields.io/badge/version-3.0.0-blue)
![Java](https://img.shields.io/badge/Java-26-orange)
![Depends](https://img.shields.io/badge/depends%20on-axiom-informational)
![License](https://img.shields.io/badge/license-Limited%20Commercial-red)

# Axiom Sovereign Core (`axiom`)

> Private, zero-GC, Panama-driven native engine. The foundation every other Axiom module is built on.

`axiom` is the root of the Axiom ecosystem. It has **zero third-party dependencies** and provides the immutable data
structures, the `Result` error-lifting model, guarded validation, typed navigation over untyped substance,
configuration loading, and side-effect encapsulation used by `axiom-sovereign`, `axiom-language`, `axiom-spec`,
`axiom-telemetry` and every `axiom-warp-*-driver`.

---

## Table of contents

1. [Architecture overview](#architecture-overview)
2. [Installation](#installation)
3. [Runtime requirements](#runtime-requirements)
4. [The five fingers of `Axiom`](#the-five-fingers-of-axiom)
5. [Usage examples](#usage-examples)
6. [Configuration guide](#configuration-guide)
7. [Package map](#package-map)
8. [License](#license)

---

## Architecture overview

```
                      ┌──────────────────────────────────────────┐
                      │           com.ensemblu.axiom.api.Axiom    │   single sealed entry point
                      └──────────────────────────────────────────┘
        Check            Data              Forge            Config            Io
   (validation)    (data structures)   (navigation)     (environment)    (side effects)
        │                  │                 │                 │                │
  Result / If /     PersistentList     Source +          ConfigSource      Effect
  Guard             PersistentMap      TargetNavigator                     FileUtils
                    (Trie / HAMT,
                    transient+freeze
                    via Arena)
```

Design principles:

| Principle | What it means in practice |
|---|---|
| **Immutability first** | `PersistentList` (vector trie) and `PersistentMap` (HAMT) return new versions on every write. `asTransient()` gives a thread-confined mutable builder; `freeze(Arena)` seals it again. |
| **Failures are values** | Every fallible operation returns `Result<T>` (success / failure / empty) instead of throwing. You decide where to `getOrThrow()`. |
| **Typed edges, untyped core** | Data flows as `PersistentMap<String, Object>`; you extract types at the edge with `targetKey(..).toStringVal()`, `toIntResult()`, etc. |
| **Effects are explicit** | Console/IO work is wrapped in `Effect<A>` and only happens on `.run()`. |
| **Off-heap aware** | Built on the Java Foreign Function & Memory API (Panama). Config and file reads are memory-mapped through `Arena` segments. |

---

## Installation

**Maven**

```xml
<dependency>
    <groupId>com.ensemblu</groupId>
    <artifactId>axiom</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL)**

```kotlin
implementation("com.ensemblu:axiom:3.0.0")
```

JPMS automatic module name: `com.ensemblu.axiom`

---

## Runtime requirements

| Requirement | Value |
|---|---|
| JDK | **26** |
| Preview features | Compiled with `--enable-preview`; run your application with `--enable-preview` |
| Native access | Recommended: `--enable-native-access=ALL-UNNAMED` (Panama restricted methods) |

Compiler setup for a consuming project:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.15.0</version>
    <configuration>
        <release>26</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

---

## The five fingers of `Axiom`

`com.ensemblu.axiom.api.Axiom` is a sealed interface with five nested entry points:

| Finger | Purpose | Key methods |
|---|---|---|
| `Axiom.Check` | Validation and error lifting | `that(v)`, `soft(v)`, `attempt(..)`, `success(v)`, `failure(msg)`, `empty()`, `optional(v)`, `supplyThat(cond)` |
| `Axiom.Data` | Persistent collections | `emptyList()`, `emptyMap()`, `list(..)`, `range(a, b)`, `fromJava(list/map)`, `harden(obj)` |
| `Axiom.Forge` | Deep navigation over raw substance | `source(obj)` → `Source` |
| `Axiom.Config` | Environment / properties | `file(name)`, `source(string)` → `ConfigSource` |
| `Axiom.Io` | Side effects | `read(path)` → `Result<String>`, `log(msg)` → `Effect<Nothing>` |

---

## Usage examples

### 1. Results instead of exceptions

```java
import com.ensemblu.axiom.api.Axiom;
import com.ensemblu.axiom.core.validation.Result;

Result<Integer> port = Axiom.Check
        .attempt(() -> Integer.parseInt(System.getenv("APP_PORT")))
        .validate(p -> p > 0 && p < 65536, "port out of range")
        .prependFailureMessage("Boot failure: ");

int resolved = port.getOrElse(8080);

port.peekSuccess(p -> Axiom.Io.log("Listening on " + p).run())
    .peekFailure(e -> Axiom.Io.log(e.getMessage()).run());
```

Useful `Result` operations: `map`, `flatMap`, `validate`, `reject`, `recover`, `recoverWith`, `mapFailure`,
`prependFailureMessage`, `mapEmpty`, `peekSuccess`, `peekFailure`, `getOrElse`, `getOrThrow`, `isSuccess`,
`isFailure`, `isEmpty`, plus the combinators `Result.lift`, `lift2`, `lift3`.

### 2. Guarded validation with `If`

```java
import com.ensemblu.axiom.core.validation.If;

If.givenObject(path)
  .isNonNull("path")
  .andIsNot(String::isBlank, "path is blank")
  .andOtherObjectIsNotNull(handler, "handler")
  .will()
  .getResult()                 // Result<String>
  .getOrThrow();
```

### 3. Persistent collections

```java
import com.ensemblu.axiom.api.Axiom;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;

PersistentMap<String, Object> account = Axiom.Data.<String, Object>emptyMap()
        .put("owner", "Ofek")
        .put("balance", 50_000.0);

PersistentMap<String, Object> updated = account.put("status", "ACTIVE"); // `account` is unchanged

PersistentList<Integer> squares = Axiom.Data.range(1, 6).map(i -> i * i);

String owner  = updated.targetKey("owner").toStringVal();          // throws on mismatch
Result<Double> bal = updated.targetKey("balance").toDoubleResult(); // safe path
```

Bulk building without per-step copying:

```java
import java.lang.foreign.Arena;

var builder = Axiom.Data.<String>emptyList().asTransient();
for (var name : names) builder = builder.append(name);
PersistentList<String> frozen = builder.freeze(Arena.ofShared());
```

### 4. Deep navigation with `Source`

```java
var input   = Axiom.Forge.source(requestMap);
var status  = input.follow("filters").follow("status").navigate().toStringVal();
var firstId = input.follow("items").inIndex(0).follow("id").navigate().toLongResult();
boolean hasLimit = input.follow("limit").exists();
```

### 5. Effects

```java
Axiom.Io.log("🚀 Gateway active").run();   // nothing prints until run()
Result<String> body = Axiom.Io.read("templates/welcome.txt");
```

---

## Configuration guide

`Axiom.Config.file(name)` loads a `.properties` resource. Lookup order:

1. The **classpath** (context class loader, then the `ConfigSource` class loader).
2. If not on the classpath, a **filesystem path** relative to the working directory.

Files found on disk are memory-mapped; classpath entries inside JARs are streamed.

```properties
# src/main/resources/app.properties
engine.host=localhost
engine.port=5432
feature.audit=true
```

```java
var config = Axiom.Config.file("app.properties");

String host  = config.targetField("engine.host").toStringVal();
int port     = config.targetField("engine.port").toIntResult()
                     .validate(p -> p > 0, "port must be positive")
                     .getOrThrow();
boolean audit = config.targetField("feature.audit").toBooleanResult().getOrElse(false);

// Inline configuration (tests, overrides)
var inline = Axiom.Config.source("engine.host=127.0.0.1\nengine.port=6543");
```

All database drivers (`axiom-warp-*-driver`) consume a `ConfigSource` produced this way.

---

## Package map

| Package | Contents |
|---|---|
| `com.ensemblu.axiom.api` | `Axiom` entry point, `TargetNavigator` typed extraction |
| `com.ensemblu.axiom.core.validation` | `Result`, `If`, `Guard` |
| `com.ensemblu.axiom.core.data_structure.list` | `PersistentList`, `ListDelta` + trie internals |
| `com.ensemblu.axiom.core.data_structure.map` | `PersistentMap`, `MapDelta` + HAMT internals |
| `com.ensemblu.axiom.core.navigation` | `Source` deep navigator |
| `com.ensemblu.axiom.core.config` | `ConfigSource` |
| `com.ensemblu.axiom.core.foundation` | `Dop` (projection/normalisation), `DataCast`, `Nothing`, `FileUtils`, `TraversalBreak` |
| `com.ensemblu.axiom.core.function` | `ThrowingSupplier`, `ThrowingFunction`, `ThrowingRunnable`, `ThrowingPredicate` |
| `com.ensemblu.axiom.core.io` | `Effect` |

### Downstream modules

| Module | Builds on `axiom` to provide |
|---|---|
| `axiom-sovereign` | Native `.axiom` DOP grammar parser |
| `axiom-language` | Schema validation (`SchemaGuard`) |
| `axiom-spec` | JSON / CSV parsers and emitters |
| `axiom-telemetry` | Temporal black-box flight recorder |
| `axiom-warp-*-driver` | Native PostgreSQL, MySQL, Oracle, SQLite and SQL Server drivers |

---

## License

**Limited Commercial License** — free for evaluation, testing, and non-commercial development.
Commercial or production use requires a paid annual contract.
See [LICENSE](https://raw.githubusercontent.com/ensemblu-corp/axiom/main/LICENSE) · Contact: contact@ensemblu.com

© Ensemblu Corp
