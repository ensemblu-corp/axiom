# 🏛️ Axiom Core

The sovereign, zero-dependency foundation for structural governance and persistent, Data-Oriented architecture. `axiom-core` is the base every other Axiom module (`axiom-jdbc`, `axiom-warp-reactive`) builds on: immutable persistent data structures, a tri-state `Result` pipeline, contract-based validation, and zero-reflection type casting.

No annotations. No reflection. No third-party dependencies — `axiom-core` ships on nothing but the JDK.

---

## Requirements

- **Java 26** (compiled with `--enable-preview` for modern pattern matching)
- Nothing else. `pom.xml` declares zero runtime dependencies.

---

## 🏛️ Integration

Summon the Specification engine into your project:

**Maven**

```xml 
<dependency>    
     <groupId>com.ensemblu</groupId>   
     <artifactId>axiom</artifactId>   
     <version>1.0.0</version>  
</dependency>   
```   
**Gradle**

```groovy
 implementation("com.ensemblu:axiom:1.0.0")   
```

---

## Package Structure

```
com.ensemblu.axiom
├── api
│   ├── Axiom.java              // The sealed entry point — Check / Data / Forge / Config / Io
│   └── TargetNavigator.java    // Type-casting surface (toIntResult, toStringVal, toMapProjector, ...)
├── core
│   ├── config
│   │   └── ConfigSource.java   // .properties-based config, file or string sourced
│   ├── data_structure
│   │   ├── list
│   │   │   ├── PersistentList.java          // Bitmapped Vector Trie (BVT)
│   │   │   ├── ListDelta.java               // added / removed / isOverwrite
│   │   │   ├── behavior/PersistentListBehavior.java
│   │   │   ├── command/ (Append, Fetch, Path, Update)
│   │   │   ├── cursor/Path.java
│   │   │   └── data/ (LeanVectorState, ListNode)
│   │   └── map
│   │       ├── PersistentMap.java           // Hash Array Mapped Trie (HAMT)
│   │       ├── MapDelta.java                // added / removed / updated
│   │       ├── behavior/PersistentMapBehavior.java
│   │       ├── command/ (Diff, Insert, Remove, Search, Traverse)
│   │       ├── cursor/Cursor.java
│   │       └── data/ (LeanMap, MapNode)
│   ├── foundation
│   │   ├── Dop.java             // Numerical normalization, structural equality, MapProjector/ListProjector
│   │   ├── DataCast.java        // Result-safe protocol casting (the backbone of TargetNavigator)
│   │   ├── FileUtils.java       // Classpath resource reading, wrapped in Result
│   │   ├── Nothing.java         // First-class "no value" singleton
│   │   └── TraversalBreak.java  // Zero-overhead signal to abort a functional traversal
│   ├── function
│   │   └── (ThrowingFunction, ThrowingPredicate, ThrowingRunnable, ThrowingSupplier, ConsoleOutputEffect)
│   ├── io
│   │   └── Effect.java          // Deferred, explicit side-effect wrapper
│   ├── navigation
│   │   ├── Source.java          // Zero-logic path navigator into Map/List trees
│   │   └── SourceBehavior.java
│   └── validation
│       ├── Result.java          // Tri-state: Success / Failure / Empty
│       ├── If.java              // Hard and soft guard-check DSL
│       ├── Guard.java           // BooleanSupplier-based condition chaining
│       └── behavior/ (CheckFlow, ResultBehavior)
```

---

## Getting Started

### 1. The Result pipeline

Every fallible operation in Axiom returns a `Result<T>` — never `null`, never an unchecked surprise.

```java
Result<Integer> parsed = Axiom.Check.attempt(() -> Integer.parseInt(raw));

parsed
    .validate(n -> n > 0, "must be positive")
    .map(n -> n * 2)
    .peekFailure(e -> log.warn(e.getMessage()))
    .getOrElse(0);
```

`Result` has exactly three states — `Success`, `Failure`, `Empty` — modeled as a sealed `State<T>`. `Empty` is a first-class "successfully nothing" outcome, distinct from a failure.

### 2. Guarding the perimeter with `If`

```java
Axiom.Check.that(factory)
        .is(Objects::nonNull, "RawProvisioner instance can't be null")
        .will()
        .thenApprovedOrElseThrowException();
```

`If` builds a fluent chain of `is`/`isNot`/`andIs`/`andIsNot` predicates, terminated by `.will()`, which exposes `getResult()`, `mapTo()`, `flatMapTo()`, `getValueOrElseThrow()`, or `thenApprovedOrElseThrowException()`.

For **soft validation** — collecting every violation instead of failing on the first — use `Axiom.Check.soft(value)`:

```java
Axiom.Check.soft(order)
        .is(o -> o.qty() > 0, "qty must be positive")
        .andIs(o -> o.price() >= 0, "price must be non-negative")
        .will()
        .generateResultErrorIfExists();   // Result<String> listing every breach
```

### 3. Persistent data structures

```java
PersistentMap<String, Object> user = Axiom.Data.<String, Object>emptyMap()
        .put("id", 1L)
        .put("name", "Ofek");

PersistentMap<String, Object> updated = user.put("name", "Ofek Cohen"); // user is untouched

PersistentList<Integer> nums = Axiom.Data.range(1, 10);
```

`PersistentMap` is a **HAMT**; `PersistentList` is a **Bitmapped Vector Trie**. Both are structurally shared — every `put`/`append`/`remove` returns a new version without copying the whole structure, and both implement the same numerical equality rules (`123` and `123.0` hash and compare equal).

### 4. Diffing state: `MapDelta` / `ListDelta`

```java
MapDelta<String, Object> delta = oldState.diff(newState);
delta.added();    // keys present only in newState
delta.removed();  // keys present only in oldState
delta.updated();  // keys with a changed value
delta.invert();   // flips added <-> removed, for undo/rollback logic
```

This is the same `MapDelta` consumed by `SyncStrike` in `axiom-jdbc` and `axiom-warp-reactive` to mirror application state into a database.

### 5. Navigating nested structures with `Source`

```java
Source root = Axiom.Forge.source(deeplyNestedMap);

Source name = root.follow("user").follow("profile").follow("name");
if (name.exists()) {
    String value = (String) name.getValue();
}

// Immutable, path-copying updates:
Source updatedRoot = name.update("New Name");
```

`Source` is a zero-logic navigator: `follow(key)` walks into a `PersistentMap`, `inIndex(i)` walks into a `PersistentList`, and `update(value)` rebuilds every ancestor on the path back to the root — the original tree is never mutated.

### 6. Type-safe extraction with `TargetNavigator`

Both `PersistentMap.targetKey(K)` and `ConfigSource.targetField(String)` return a `TargetNavigator`, giving you a full family of `Result`-safe and throwing accessors:

```java
Result<Integer> age  = user.targetKey("age").toIntResult();
String          name = user.targetKey("name").toStringVal();          // throws on failure/missing
PersistentList<String> tags = user.targetKey("tags").toStringListVal();
```

Every numeric/string/boolean/BigDecimal/Temporal type has a `to*Result()` (safe) and `to*Val()` (throwing) variant, plus bulk projections for `PersistentMap`/`PersistentList` of a given key or element type.

### 7. Configuration

```java
ConfigSource config = Axiom.Config.file("app.properties");
// or: Axiom.Config.source("engine.pool.max=10\nengine.pool.min=2")

int poolMax = config.targetField("engine.pool.max").toIntVal();

Result<PersistentList<PersistentMap<String,Object>>> users = config
        .asMappedList("users", sub -> Axiom.Data.<String,Object>empty()
                .put("name", sub.targetField("name").toStringVal())
                .put("role", sub.targetField("role").toStringVal())
        );
```

`asMappedList` splits a `;`-separated property value into sub-`ConfigSource` instances (comma-separated `key:value` pairs internally rewritten as newline-separated properties), letting you map each element with its own reader.

### 8. Side effects, explicitly

```java
Effect<Nothing> log = Axiom.Io.log("Warp initialized");
log.run(); // nothing happens until you call run()
```

`Effect<A>` wraps a `Supplier<A>` so that IO/println/logging calls are values you can pass around, not statements that fire immediately.

### 9. Reading files

```java
Result<String> contents = Axiom.Io.read("data/seed.properties");
```

Wraps classpath resource loading in a `Result` — missing resource or empty content becomes a documented `Failure`, never a `NullPointerException` or unchecked `IOException`.

---

## Core Concepts

### The Axiom Entry Point

`Axiom` is a **sealed interface** with exactly five permitted "fingers," each a `non-sealed` sub-interface holding only static factory methods — there is no `Axiom` instance to construct:

| Finger | Role |
| :--- | :--- |
| `Axiom.Check` | `Result`/`If`/`Guard` entry points: `that()`, `soft()`, `attempt()`, `success()`, `failure()`, `optional()`, `supplyThat()` |
| `Axiom.Data` | Persistent structure factories: `range()`, `fromJava()`, `emptyMap()`, `emptyList()`, `list()`, `harden()` |
| `Axiom.Forge` | `source(value)` — lifts a raw `Map`/`List` into a `Source` navigator |
| `Axiom.Config` | `file(name)` / `source(string)` — `ConfigSource` factories |
| `Axiom.Io` | `read(path)` / `log(message)` — file and console side effects |

### Numerical Normalization (`Dop`)

`Dop.normalize` is the single canonicalization point every value passes through before it's compared, hashed, or stored: numeric strings are parsed, floating values that represent whole numbers collapse to `Integer`/`Long`, and `"true"`/`"false"` strings become `Boolean`. This is what lets `123`, `123L`, and `123.0` occupy the same trie slot and compare equal via `Dop.isEqual`.

### The Hard Shell / Soft Core Boundary

Gateways like `Axiom.java` are the "Hard Shell" and must always return `Result<T>`; logic *inside* an already-validated block is the "Soft Core" and is allowed to call `.getOrThrow()` freely, on the understanding that a crash there is a signal the business logic is inconsistent with data that already passed the perimeter check — not something the framework should silently absorb.

### `TraversalBreak`: the zero-cost escape hatch

`TraversalBreak` is a `RuntimeException` constructed once, with stack-trace generation disabled (`super(null, null, false, false)`), and reused as a singleton (`TraversalBreak.INSTANCE`). It exists purely as a fast, allocation-free signal to unwind a recursive/functional traversal early — not to carry a message or a trace.

---


## 📜 Legal

This project is governed by the principles of immutable software architecture. See `LICENSE.md` for the specific terms of use.