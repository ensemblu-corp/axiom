# 🌊 Axiom Core: Philosophy
### *Validate at the Perimeter. Execute in the Core.*

Axiom Core exists because Data-Oriented Programming isn't a style choice bolted onto Object-Oriented habits — it's a different answer to a different question. OOP asks "what is this thing?" Axiom asks "what does this data need to become, and who is allowed to change it?"

---

## 🛡️ The Four Axioms of DOP

Everything in this module is downstream of four commitments:

1. **Separate Code from Data.** Data is a value. Code is a transformation. `PersistentMap` and `PersistentList` carry no behavior of their own beyond structural operations — logic lives in functions that take data in and hand a `Result` back.

2. **Generic Data Structures.** There is no `User` class, no `Order` class. There is a `PersistentMap<String, Object>` shaped like a user, and another shaped like an order. The same `Source` navigator, the same `Dop` equality, the same `TargetNavigator` casting work on both — because the tools were never written against a specific shape.

3. **Data is Immutable.** `put`, `remove`, `append` never mutate — they return a new persistent structure sharing every unaffected branch with the old one. There is no defensive copying, and there is no concurrency bug from two threads racing to mutate the same object, because nothing is ever mutated.

4. **Separate Schema from Representation.** Your Java code stays generic; validation is a separate concern applied at the boundary, not baked into a constructor. `If`, `Guard`, and `Result` are how that boundary is enforced in this module.

---

## 🏰 Validate at the Perimeter, Execute in the Core

- **`Result` is for the bridge.** `mapTry`/`flatMapTry`/`Axiom.Check.attempt` exist for the moment data crosses from an unpredictable outside world (a file, a config string, a parsed number) into the validated zone. This is where exceptions get caught and converted into documented failures.
- **`PersistentMap`/`PersistentList` are for the inside.** Once data is sitting in a `PersistentMap`, there are no "try" variants for `get` or `put`. Inside the core, data is assumed valid — because it was validated on the way in, not because anyone is hoping for the best.

The gateways (`Axiom.java` and anything playing that role) are the **Hard Shell** and must always speak in `Result<T>`. Logic *inside* an already-guarded block is the **Soft Core**, and is explicitly permitted to call `.getOrThrow()` — if that throws, it's a real signal that the business logic disagreed with data that already passed the gate, not a case the framework should paper over.

```java
// Hard Shell: validates, returns Result
Result<PersistentMap<String, Object>> row = Axiom.Check.attempt(() -> parseRow(line));

// Soft Core: once inside a validated block, getOrThrow is fine
row.map(m -> {
    String name = m.targetKey("name").toStringVal(); // throws only if the contract lied
    return process(name);
});
```

---

## 🔢 The Numerical Handshake

A recurring failure mode in dynamically-typed data pipelines is **type drift**: `123`, `123L`, and `123.0` arriving from different sources and silently failing to compare equal. `Dop.normalize` closes that gap at the single point every value passes through before it's hashed, stored, or compared — collapsing whole-number doubles to `Integer`/`Long`, parsing numeric strings, and recognizing `"true"`/`"false"`. `PersistentMap` and `PersistentList` both build their `equals`/`hashCode` on top of `Dop.isEqual`, so this guarantee holds everywhere, not just at the edges.

---


## ⚔️ What This Buys You

| Traditional Pain | Axiom's Answer |
| :--- | :--- |
| `NullPointerException` from a missing field | `Source`/`TargetNavigator` return a documented `Result.failure`, never `null` |
| Hibernate proxies, DTO↔Entity mapping | Everything is already a `PersistentMap`; there is no second representation to keep in sync |
| Mutable state races | Structural sharing — every write is a new value, nothing is ever mutated in place |
| `123` vs `123.0` breaking an equality check | `Dop.normalize` + `Dop.isEqual` make numeric type a non-issue |
| Silent swallowed exceptions in a pipeline | `Result`'s tri-state (`Success`/`Failure`/`Empty`) makes every outcome explicit and chainable |
| Checked exceptions breaking functional composition | `ThrowingFunction`/`ThrowingSupplier`/`ThrowingPredicate` convert them into `Result` failures at the source |

---

## 🛑 When to Walk Away

This isn't the right tool if you're comfortable with — or actively prefer — a `User` class with forty private fields, eighty getters, and a Hibernate proxy underneath. Axiom Core has no interest in hiding data behind objects. If boilerplate feels like safety to you, this will feel like the opposite.

**The data is the star. The code is just the stage.**
