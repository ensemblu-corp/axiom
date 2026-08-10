
# 🏛️ Axiom Core

![Version](https://img.shields.io/badge/version-2.0.0-blue)
![Java](https://img.shields.io/badge/Java-26-orange)
![Dependencies](https://img.shields.io/badge/dependencies-zero-brightgreen)
![License](https://img.shields.io/badge/license-Limited%20Commercial-red)

**The sovereign, zero-dependency foundation for structural governance and persistent, Data-Oriented architecture.**

`axiom` is the base every other Axiom module builds on: immutable persistent data structures, a tri-state `Result` pipeline, contract-based validation, and zero-reflection type casting.

No annotations. No reflection. No third-party dependencies — `axiom` ships on nothing but the JDK.

---

## Why Axiom Core?

| Traditional pain | Axiom’s answer |
| :--- | :--- |
| `NullPointerException` from a missing field | `Source` / `TargetNavigator` return a documented `Result.failure`, never `null` |
| Hibernate proxies, DTO ↔ Entity mapping | Everything is already a `PersistentMap` — there is no second representation |
| Mutable state races | Structural sharing — every write is a new value; nothing is ever mutated in place |
| `123` vs `123.0` breaking equality | `Dop.normalize` + `Dop.isEqual` make numeric type a non-issue |
| Silent swallowed exceptions | `Result`’s tri-state (`Success` / `Failure` / `Empty`) makes every outcome explicit |
| Checked exceptions breaking composition | `ThrowingFunction` / `ThrowingSupplier` convert them into `Result` failures at the source |

---

## Requirements

- **Java 26** (compiled with `--enable-preview` for modern pattern matching)
- Nothing else. The POM declares **zero runtime dependencies**.

---

## Installation

**Maven**

```xml
<dependency>
    <groupId>com.ensemblu</groupId>
    <artifactId>axiom</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Gradle**

```groovy
implementation("com.ensemblu:axiom:2.0.0")
```

---

## Quick start

```java
import com.ensemblu.axiom.api.Axiom;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;
import com.ensemblu.axiom.core.foundation.Dop;
import com.ensemblu.axiom.core.validation.Result;

// 1. Build persistent data (HAMT — every put returns a new map)
PersistentMap<String, Object> user = Axiom.Data.<String, Object>emptyMap()
        .put("id", 1L)
        .put("name", "Ofek")
        .put("age", 36);

PersistentMap<String, Object> updated = user.put("name", "Ofek Cohen"); // user is untouched

// 2. Type-safe access via TargetNavigator (Result-safe or throwing)
Result<Integer> age  = user.targetKey("age").toIntResult();
String          name = user.targetKey("name").toStringVal();   // throws on missing / mismatch

// 3. Numerical identity (prevents 123 vs 123.0 drift)
Object key = Dop.normalize(123.0);      // → Integer 123
boolean same = Dop.isEqual(123, 123L);  // true

// 4. Result pipeline
Result<Integer> parsed = Axiom.Check.attempt(() -> Integer.parseInt(raw));
parsed
    .validate(n -> n > 0, "must be positive")
    .map(n -> n * 2)
    .getOrElse(0);

// 5. Perimeter guard with If
Axiom.Check.that(factory)
        .is(Objects::nonNull, "RawProvisioner instance can't be null")
        .will()
        .thenApprovedOrElseThrowException();

// 6. Deep navigation with Source
Source root = Axiom.Forge.source(deeplyNestedMap);
Source child = root.follow("users").inIndex(0).follow("address");
```

---

## Package structure

```
com.ensemblu.axiom
├── api
│   ├── Axiom.java                 // Sealed entry point — Check / Data / Forge / Config / Io
│   └── TargetNavigator.java       // Typed casting surface
├── core
│   ├── config
│   │   └── ConfigSource.java      // .properties config (file or string)
│   ├── data_structure
│   │   ├── list/                  // PersistentList (Bitmapped Vector Trie)
│   │   └── map/                   // PersistentMap (Hash Array Mapped Trie)
│   ├── foundation
│   │   ├── Dop.java               // Normalization, equality, projectors (final utility)
│   │   ├── DataCast.java
│   │   ├── TraversalBreak.java    // Zero-cost early-exit signal
│   │   └── …
│   ├── function/                  // ThrowingFunction, ThrowingSupplier, …
│   ├── io/                        // Effect
│   ├── navigation/                // Source navigator
│   └── validation/                // Result, If, Guard
```

---

## Core concepts

### The Axiom entry point

`Axiom` is a **sealed interface** with five permitted “fingers” — each a static factory surface. There is no `Axiom` instance to construct:

| Finger | Role |
| :--- | :--- |
| `Axiom.Check` | `Result` / `If` / `Guard` entry points |
| `Axiom.Data` | Persistent structure factories (`emptyMap`, `emptyList`, `fromJava`, …) |
| `Axiom.Forge` | `source(value)` — lifts raw data into a `Source` navigator |
| `Axiom.Config` | `file(name)` / `source(string)` — configuration |
| `Axiom.Io` | `read(path)` / `log(message)` — side effects wrapped in `Result` |

### Numerical normalization (`Dop`)

`Dop` is a **final utility class** (not an interface). Every value that will be compared, hashed, or stored should pass through `Dop.normalize`:

- Numeric strings are parsed
- Whole-number doubles collapse to `Integer` / `Long`
- `"true"` / `"false"` become `Boolean`
- Leading-zero integers and scientific notation are rejected by the strict scanner

`PersistentMap` and `PersistentList` build `equals` / `hashCode` on `Dop.isEqual`, so the guarantee holds everywhere.

> **Note (2.0.0):** `Dop.toJson` was removed. Use `JsonEmitter.emit(...)` from `axiom-spec` for JSON emission.

### Hard Shell / Soft Core

- **Hard Shell** (gateways like `Axiom.java`): always return `Result<T>`.
- **Soft Core** (already-validated logic): may call `.getOrThrow()`. A crash there signals that business logic is inconsistent with data that already passed the perimeter — not something the framework should silently absorb.

### `TraversalBreak`

A singleton `RuntimeException` constructed with stack-trace generation disabled. Used purely as a fast, allocation-free signal to unwind recursive / functional traversal early.

---

## Philosophy in one page

1. **Separate code from data** — data is a value; code is a transformation.
2. **Generic data structures** — no `User` class; a `PersistentMap` shaped like a user.
3. **Data is immutable** — `put` / `append` return new structures with structural sharing.
4. **Separate schema from representation** — validation lives at the boundary (`If`, `Guard`, `Result`), not in constructors.

> Validate at the Perimeter. Execute in the Core.

Full treatment: see `PHILOSOPHY.md` in this repository.

---

## What this module is *not*

- Not an ORM
- Not a dependency-injection container
- Not a place for mutable domain objects
- Not interested in hiding data behind getters and proxies

If boilerplate feels like safety to you, this will feel like the opposite.  
**The data is the star. The code is just the stage.**

---

## Related modules

| Module | Role |
|--------|------|
| [`axiom-sovereign`](https://github.com/ensemblu-corp/axiom-sovereign) | Byte-oriented DOP parser |
| [`axiom-language`](https://github.com/ensemblu-corp/axiom-language) | Schema / policy enforcement |
| [`axiom-spec`](https://github.com/ensemblu-corp/axiom-spec) | CSV / JSON / SQL parsers + `JsonEmitter` |
| [`axiom-warp-jdbc`](https://github.com/ensemblu-corp/axiom-warp-jdbc) | Blocking JDBC driver |
| [`axiom-warp-reactive`](https://github.com/ensemblu-corp/axiom-warp-reactive) | Reactive (Vert.x) driver |

---

## Legal

Limited Commercial License — free for evaluation, testing, and non-commercial development.  
Commercial or production use requires a paid annual contract from Ensemblu Corp.

See `LICENSE.md`. Contact: **contact@ensemblu.com**
