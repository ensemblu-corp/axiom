package com.ensemblu.axiom.core.foundation;

import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;

import java.math.BigDecimal;
import java.time.temporal.Temporal;

public interface DataCast {

    enum Protocol {
        // Primitives
        BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING, CHAR, BIG_DECIMAL, TEMPORAL,OBJECT,
        // Structures
        MAP, LIST
    }

    @SuppressWarnings("unchecked")
    static <T> Result<T> cast(Object val, Protocol protocol) {
        if (val == null)
            return Result.success(null);

        Object normalized = Dop.normalize(val);

        return (Result<T>) switch (protocol) {
            case BYTE    -> toByte(normalized);
            case SHORT   -> toShort(normalized);
            case INT     -> toInt(normalized);
            case LONG    -> toLong(normalized);
            case FLOAT   -> toFloat(normalized);
            case DOUBLE  -> toDouble(normalized);
            case BOOLEAN -> toBoolean(normalized);
            case STRING  -> Result.success(normalized.toString());
            case CHAR    -> toChar(normalized);
            case BIG_DECIMAL -> toBigDecimal(normalized);
            case TEMPORAL    -> toTemporal(normalized);
            case MAP  -> toMap(normalized);
            case LIST -> toList(normalized);
            case OBJECT -> Result.success(normalized);
        };
    }

    static Object harden(Object val) {
        return switch (Dop.normalize(val)) {
            case null -> null;
            case Boolean b -> b;
            case String s -> s;
            case Integer i -> i;
            case Long l -> l;
            case Double d -> d;
            case PersistentMap<?, ?> m -> m;
            case PersistentList<?> l -> l;
            default -> Dop.toString(val.toString());
        };
    }

    private static Result<Character> toChar(Object val) {
        if (val instanceof Character c) return Result.success(c);
        if (val instanceof String s && s.length() == 1) return Result.success(s.charAt(0));
        return Result.failure("Expected single Character, found [" + val.getClass().getSimpleName() + "]");
    }

    private static Result<BigDecimal> toBigDecimal(Object val) {
        return switch (val) {
            case BigDecimal bd -> Result.success(bd);
            case Number n     -> Result.success(BigDecimal.valueOf(n.doubleValue()));
            case String s     -> Result.of(() -> new BigDecimal(s));
            default -> Result.failure("Expected BigDecimal, found [" + val.getClass().getSimpleName() + "]");
        };
    }

    private static Result<String> toTemporal(Object val) {
        return (val instanceof Temporal || val instanceof String)
                ? Result.success(val.toString())
                : Result.failure("Expected Temporal/String, found [" + val.getClass().getSimpleName() + "]");
    }


    // --- Private Primitive Converters ---

    private static Result<Byte> toByte(Object val) {
        return switch (val) {
            case Integer i -> Result.success(i.byteValue());
            case Long l    -> Result.success(l.byteValue());
            case Double d  -> Result.success(d.byteValue());
            default -> Result.failure("Cannot cast " + val.getClass().getSimpleName() + " to Byte");
        };
    }

    private static Result<Short> toShort(Object val) {
        return switch (val) {
            case Integer i -> Result.success(i.shortValue());
            case Long l    -> Result.success(l.shortValue());
            case Double d  -> Result.success(d.shortValue());
            default -> Result.failure("Cannot cast " + val.getClass().getSimpleName() + " to Short");
        };
    }

    private static Result<Integer> toInt(Object val) {
        return switch (val) {
            case Integer i -> Result.success(i);
            case Long l    -> Result.success(l.intValue());
            case Double d  -> Result.success(d.intValue());
            default -> Result.failure("Cannot cast " + val.getClass().getSimpleName() + " to Integer");
        };
    }

    private static Result<Long> toLong(Object val) {
        return switch (val) {
            case Integer i -> Result.success(i.longValue());
            case Long l    -> Result.success(l);
            case Double d  -> Result.success(d.longValue());
            default -> Result.failure("Cannot cast " + val.getClass().getSimpleName() + " to Long");
        };
    }

    private static Result<Float> toFloat(Object val) {
        return switch (val) {
            case Integer i -> Result.success(i.floatValue());
            case Long l    -> Result.success(l.floatValue());
            case Double d  -> Result.success(d.floatValue());
            default -> Result.failure("Cannot cast " + val.getClass().getSimpleName() + " to Float");
        };
    }

    private static Result<Double> toDouble(Object val) {
        return switch (val) {
            case Integer i -> Result.success(i.doubleValue());
            case Long l    -> Result.success(l.doubleValue());
            case Double d  -> Result.success(d);
            default -> Result.failure("Cannot cast " + val.getClass().getSimpleName() + " to Double");
        };
    }

    private static Result<Boolean> toBoolean(Object val) {
        return (val instanceof Boolean b)
                ? Result.success(b)
                : Result.failure("Expected Boolean, found [" + val.getClass().getSimpleName() + "]");
    }


    // --- The Structural Converters ---
    @SuppressWarnings("unchecked")
    private static Result<PersistentMap<Object, Object>> toMap(Object val) {
        return (val instanceof PersistentMap<?, ?> m)
                ? Result.success((PersistentMap<Object, Object>) m)
                : Result.failure("Expected PersistentMap, found [" + val.getClass().getSimpleName() + "]");
    }

    @SuppressWarnings("unchecked")
    private static  Result<PersistentList<Object>> toList(Object val) {
        return (val instanceof PersistentList<?> l)
                ? Result.success((PersistentList<Object>) l)
                : Result.failure("Expected PersistentList, found [" + val.getClass().getSimpleName() + "]");
    }
}