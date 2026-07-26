
package com.ensemblu.axiom.api;

import com.ensemblu.axiom.core.foundation.Dop;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;
import com.ensemblu.axiom.core.foundation.DataCast;

import java.math.BigDecimal;
import java.time.temporal.Temporal;

/**
 * <h1>🏛️ TargetNavigator</h1>
 * <p>
 * <b>⚖️ Structural Law:</b><br>
 * Provides an immutable pathway for structural data traversal. All navigation operations
 * are bound by the underlying {@link DataCast} protocol; failure to match the expected
 * schema results in an immediate structural violation.
 * </p>
 * <p>
 * <b>⚡ Operational Promise:</b><br>
 * Zero-copy projection and type-safe casting. Methods suffixed with <code>*Result()</code>
 * provide safe encapsulation, while <code>*Val()</code> methods enforce immediate
 * resolution or termination.
 * </p>
 */
public interface TargetNavigator {

    /**
     * Executes a low-level structural protocol shift.
     *
     * @param protocol The target definition for data projection.
     */
    <T> Result<T> execute(DataCast.Protocol protocol);

    // --- RESULT SAFE PATHS ---

    default Result<Byte> toByteResult() {
        return execute(DataCast.Protocol.BYTE);
    }

    default Result<Short> toShortResult() {
        return execute(DataCast.Protocol.SHORT);
    }

    default Result<Integer> toIntResult() {
        return execute(DataCast.Protocol.INT);
    }

    default Result<Long> toLongResult() {
        return execute(DataCast.Protocol.LONG);
    }

    default Result<Float> toFloatResult() {
        return execute(DataCast.Protocol.FLOAT);
    }

    default Result<Double> toDoubleResult() {
        return execute(DataCast.Protocol.DOUBLE);
    }

    default Result<Boolean> toBooleanResult() {
        return execute(DataCast.Protocol.BOOLEAN);
    }

    default Result<String> toStringResult() {
        return execute(DataCast.Protocol.STRING);
    }

    default Result<Character> toCharResult() {
        return execute(DataCast.Protocol.CHAR);
    }

    default Result<BigDecimal> toBigDecimalResult() {
        return execute(DataCast.Protocol.BIG_DECIMAL);
    }

    default Result<Temporal> toTemporalResult() {
        return execute(DataCast.Protocol.TEMPORAL);
    }

    default Result<Object> toObjectResult() {
        return execute(DataCast.Protocol.OBJECT);
    }

    // --- STRUCTURAL PROJECTION ---

    default Result<Dop.MapProjector<Object, Object>> toMapProjectorResult() {
        return toObjectMapResult().map(Dop::project);
    }

    default Result<Dop.ListProjector<Object>> toListProjectorResult() {
        return toObjectListResult().map(Dop::project);
    }

    private Result<PersistentMap<Object, Object>> toObjectMapResult() {
        return execute(DataCast.Protocol.MAP);
    }

    private Result<PersistentList<Object>> toObjectListResult() {
        return execute(DataCast.Protocol.LIST);
    }

    // --- RAW VALUE PATHS ---

    default Byte toByteVal() {
        return toByteResult().getOrThrow();
    }

    default Short toShortVal() {
        return toShortResult().getOrThrow();
    }

    default Integer toIntVal() {
        return toIntResult().getOrThrow();
    }

    default Long toLongVal() {
        return toLongResult().getOrThrow();
    }

    default Float toFloatVal() {
        return toFloatResult().getOrThrow();
    }

    default Double toDoubleVal() {
        return toDoubleResult().getOrThrow();
    }

    default Boolean toBooleanVal() {
        return toBooleanResult().getOrThrow();
    }

    default String toStringVal() {
        return toStringResult().getOrThrow();
    }

    default Character toCharVal() {
        return toCharResult().getOrThrow();
    }

    default BigDecimal toBigDecimalVal() {
        return toBigDecimalResult().getOrThrow();
    }

    default Temporal toTemporalVal() {
        return toTemporalResult().getOrThrow();
    }

    default Object toObjectVal() {
        return toObjectResult().getOrThrow();
    }

    default Dop.MapProjector<Object, Object> toMapProjector() {
        return toMapProjectorResult().getOrThrow();
    }

    default Dop.ListProjector<Object> toListProjector() {
        return toListProjectorResult().getOrThrow();
    }

    // --- CONVENIENCE LAYER: MAPS ---

    default PersistentMap<Byte, Object> toByteKeyMapVal() {
        return toStringMapProjector().mapKeys(Byte::valueOf).deploy();
    }

    default PersistentMap<Short, Object> toShortKeyMapVal() {
        return toStringMapProjector().mapKeys(Short::valueOf).deploy();
    }

    default PersistentMap<Integer, Object> toIntKeyMapVal() {
        return toStringMapProjector().mapKeys(Integer::valueOf).deploy();
    }

    default PersistentMap<Long, Object> toLongKeyMapVal() {
        return toStringMapProjector().mapKeys(Long::valueOf).deploy();
    }

    default PersistentMap<Float, Object> toFloatKeyMapVal() {
        return toStringMapProjector().mapKeys(Float::valueOf).deploy();
    }

    default PersistentMap<Double, Object> toDoubleKeyMapVal() {
        return toStringMapProjector().mapKeys(Double::valueOf).deploy();
    }

    default PersistentMap<Character, Object> toCharKeyMapVal() {
        return toStringMapProjector().mapKeys(s -> s.charAt(0)).deploy();
    }

    default PersistentMap<String, Object> toStringKeyMapVal() {
        return toStringMapProjector().deploy();
    }

    default Dop.MapProjector<String, Object> toStringMapProjector() {
        return toMapProjector().mapKeys(String::valueOf);
    }

    // --- CONVENIENCE LAYER: LISTS ---

    default PersistentList<Byte> toByteListVal() {
        return toStringListProjector().map(Byte::valueOf).deploy();
    }

    default PersistentList<Short> toShortListVal() {
        return toStringListProjector().map(Short::valueOf).deploy();
    }

    default PersistentList<Integer> toIntListVal() {
        return toStringListProjector().map(Integer::valueOf).deploy();
    }

    default PersistentList<Long> toLongListVal() {
        return toStringListProjector().map(Long::valueOf).deploy();
    }

    default PersistentList<Float> toFloatListVal() {
        return toStringListProjector().map(Float::valueOf).deploy();
    }

    default PersistentList<Double> toDoubleListVal() {
        return toStringListProjector().map(Double::valueOf).deploy();
    }

    default PersistentList<Character> toCharListVal() {
        return toStringListProjector().map(s -> s.charAt(0)).deploy();
    }

    default PersistentList<String> toStringListVal() {
        return toStringListProjector().deploy();
    }

    default Dop.ListProjector<String> toStringListProjector() {
        return toListProjector().map(String::valueOf);
    }

    // --- MASS PROJECTION ---

    default <T> PersistentList<PersistentMap<T, Object>> toGenericTypeKeyMapListVal(DataCast.Protocol protocol) {
        return toListProjector()
                .map(obj -> {
                    var map = DataCast.<PersistentMap<Object, Object>>cast(obj, DataCast.Protocol.MAP).getOrThrow();
                    return Dop.project(map)
                            .mapKeys(o -> (T) DataCast.<T>cast(o, protocol).getOrThrow())
                            .deploy();
                })
                .deploy();
    }

    default PersistentList<PersistentMap<Byte, Object>> toByteKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.BYTE);
    }

    default PersistentList<PersistentMap<Short, Object>> toShortKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.SHORT);
    }

    default PersistentList<PersistentMap<Integer, Object>> toIntKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.INT);
    }

    default PersistentList<PersistentMap<Long, Object>> toLongKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.LONG);
    }

    default PersistentList<PersistentMap<Float, Object>> toFloatKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.FLOAT);
    }

    default PersistentList<PersistentMap<Short, Object>> toDoubleKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.SHORT);
    }

    default PersistentList<PersistentMap<Character, Object>> toCharKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.CHAR);
    }

    default PersistentList<PersistentMap<String, Object>> toStringKeyMapListVal() {
        return toGenericTypeKeyMapListVal(DataCast.Protocol.STRING);
    }
}