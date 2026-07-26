package com.ensemblu.axiom.api;

import com.ensemblu.axiom.core.function.ThrowingRunnable;
import com.ensemblu.axiom.core.validation.Guard;
import com.ensemblu.axiom.core.validation.If;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.config.ConfigSource;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;
import com.ensemblu.axiom.core.function.ThrowingSupplier;
import com.ensemblu.axiom.core.io.Effect;
import com.ensemblu.axiom.core.navigation.Source;
import com.ensemblu.axiom.core.foundation.DataCast;
import com.ensemblu.axiom.core.foundation.FileUtils;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.function.BooleanSupplier;

/**
 * <h1>🏛️ Axiom Entry Point</h1>
 * <p>
 * <b>⚖️ Structural Law:</b><br>
 * All incoming substance must be normalized via <code>Dop.normalize()</code> before
 * interacting with the internal Trie state to prevent Type Drift.
 * </p>
 * <p>
 * <b>⚡ Operational Promise:</b><br>
 * Provides a thread-confined, zero-garbage pathway for structural mutation.
 * Ensures O(log n) strike efficiency across the immutable Arsenal.
 * </p>
 */
public sealed interface Axiom permits
        Axiom.Check,
        Axiom.Data,
        Axiom.Forge,
        Axiom.Config,
        Axiom.Io {

    /**
     * 🛡️ THE FIRST FINGER: dev.axiom.core.assertion
     * Guards the perimeter through validation and error-lifting.
     */
    non-sealed interface Check extends Axiom {
        static <T> If<T> that(T value) {
            return If.givenObject(value);
        }

        static <T> If.GetSoft<T> soft(T value) {
            return If.givenNonNullForSoftValidation(value);
        }

        static <T> Result<T> attempt(ThrowingSupplier<T> supplier) {
            return Result.of(supplier);
        }

        static Result<Nothing> attempt(ThrowingRunnable action) {
            return Result.of(action);
        }

        static <T> Result<T> success(T value) {
            return Result.success(value);
        }

        static <T> Result<T> failure(String msg) {
            return Result.failure(msg);
        }

        static <T> Result<T> failure(RuntimeException exception) { return Result.failure(exception);}

        static <T> Result<T> failure(String msg, Throwable e) {
            return Result.failure(msg, e);
        }

        static <T> Result<T> empty() {
            return Result.empty();
        }

        static <T> Result<T> optional(T value) {
            return value == null ? Result.empty() : Result.success(value);
        }

        static Guard.Cond supplyThat(BooleanSupplier condition){return Guard.supplyThat(condition);}
    }

    /**
     * ❄️ THE SECOND FINGER: dev.axiom.core.data_structure
     * The Arsenal of Sovereign Vector Tries and Persistent Maps.
     */
    non-sealed interface Data extends Axiom {
        static PersistentList<Integer> range(int start, int end) {
            return PersistentList.range(start, end);
        }

        /**
         * 🛡️ Ingests a legacy Java List into the Sovereign Trie.
         */
        static <T> PersistentList<T> fromJava(java.util.List<T> legacy) {
            return PersistentList.fromJavaList(legacy);
        }

        /**
         * 🛡️ Ingests a legacy Java Map into the Sovereign HAMT.
         */
        static <K, V> PersistentMap<K, V> fromJava(java.util.Map<K, V> legacy) {
            return PersistentMap.fromJavaMap(legacy);
        }

        static <K, V> PersistentMap<K, V> emptyMap() {
            return PersistentMap.empty();
        }

        static <T> PersistentList<T> emptyList() {
            return PersistentList.empty();
        }

        @SafeVarargs
        static <T> PersistentList<T> list(T... elements) {
            return PersistentList.list(elements);
        }

        static Object harden(Object substance) {
            return DataCast.harden(substance);
        }
    }

    /**
     * ⚒️ THE THIRD FINGER: dev.axiom.core.parser
     * Materializes raw substance into pure information.
     */
    non-sealed interface Forge extends Axiom {
        /**
         * 🛰️ The High-Precision Navigator.
         * Lifts a raw substance (Map/List) into a Source Laser.
         * Allows for deep navigation and type-safe extraction.
         */
        static Source source(Object value) {
            return Source.of(value);
        }
    }

    /**
     * ⚙️ THE FOURTH FINGER: dev.axiom.core.config
     * Accesses the environmental perimeter.
     */
    non-sealed interface Config extends Axiom {
        static ConfigSource file(String fileName) {
            return ConfigSource.fileConfigSource(fileName);
        }

        static ConfigSource source(String source) {
            return ConfigSource.stringConfigSource(source);
        }
    }

    /**
     * 🔊 THE FIFTH FINGER: dev.axiom.core.io & util
     * Encapsulates side effects and physical world interactions.
     */
    non-sealed interface Io extends Axiom {
        static Result<String> read(String path) {
            return FileUtils.readFile2String(path);
        }

        static <A> Effect<Nothing> log(A message) {
            return Effect.printlnToConsole(message);
        }
    }
}