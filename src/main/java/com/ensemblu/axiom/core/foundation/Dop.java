package com.ensemblu.axiom.core.foundation;

import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.MapDelta;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Dop {

    static boolean isEqual(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        // 1. Normalize both sides to Canonical Form
        final var normA = normalize(a);
        final var normB = normalize(b);

        return normA.equals(normB);
    }

    static int hashCode(Object val) {
        if (val == null) return 0;

        if (val instanceof Number n) {
            return Double.hashCode(n.doubleValue());
        }

        return val.hashCode();
    }

    static Object resolve(Object val) {
        return (val instanceof LazyConstant<?> lazy) ? lazy.get() : val;
    }

    static Object normalize(Object o) {
        return switch (o) {
            case Boolean b -> b;
            case Byte b    -> (int) (byte) b;
            case Short s   -> (int) (short) s;
            case Integer i -> i;
            case Long l    -> normalizeLong(l); // Add this helper
            case Float f   -> normalize((double) f);
            case Double d  -> normalizePrimitive(d);
            case String s -> switch (s.toLowerCase()) {
                case "true"  -> true;
                case "false" -> false;
                default      -> tryParseNumeric(s);
            };

            default -> o;
        };
    }

    private static Object normalizeLong(long l) {
        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return (int) l;
        }
        return l;
    }

    private static Object normalizePrimitive(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) return (int) d;
            return (long) d;
        }
        return d;
    }

    private static Object tryParseNumeric(String s) {
        if (s.isBlank()) return s;

        char first = s.charAt(0);
        if (!((first >= '0' && first <= '9') || first == '-')) return s;

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException _) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException _) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException _) {
                    return s;
                }
            }
        }
    }


    static <K, V> String toJson(PersistentMap<K, V> map) {
        final var sb = new StringBuilder("{");

        map.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(", ");

            // Use Dop-style logic for consistent formatting
            sb.append("\"").append(k).append("\": ");

            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else {
                sb.append(v);
            }
        });

        return sb.append("}").toString();
    }

    static String toString(Object data) {
        return (data == null) ? "null" : data.toString();
    }

    static <K, V> MapProjector<K, V> project(PersistentMap<K, V> map) {
        return new MapProjector<>(map);
    }

    static <E> ListProjector<E> project(PersistentList<E> list) {
        return new ListProjector<>(list);
    }

    static <K, V> MapProjector<K, V> projectMap() {
        return new MapProjector<>(PersistentMap.empty());
    }

    static <E> ListProjector<E> projectList() {
        return new ListProjector<>(PersistentList.empty());
    }

    static <K, V> ListProjector<K> keys(PersistentMap<K, V> map) {
        return new MapProjector<>(map).keys();
    }

    static <K, V> ListProjector<V> values(PersistentMap<K, V> map) {
        return new MapProjector<>(map).values();
    }


    class MapProjector<K, V> {
        private final PersistentMap<K, V> map;

        private MapProjector(PersistentMap<K, V> map) {
            this.map = map;
        }


        @SuppressWarnings("unchecked")
        public PersistentMap<K, V> evolve(MapDelta<K, V> delta) {
            final var holder = new AtomicReference<>(this.map.asTransient());

            delta.removed().forEach((k, v) -> holder.set(holder.get().remove(k)));

            holder.set(holder.get().merge(delta.added()));

            delta.updated().forEach((k, change) -> {
                if (change instanceof MapDelta nestedDelta) {
                    PersistentMap<K, V> subMap = (PersistentMap<K, V>) holder.get().get(k);
                    if (subMap != null) {
                        final var evolvedSubMap = new MapProjector<>(subMap).evolve(nestedDelta);
                        holder.set(holder.get().put(k, (V) evolvedSubMap));
                    }
                } else {
                    holder.set(holder.get().put(k, change));
                }
            });

            return holder.get().freeze();
        }

        @SuppressWarnings("unchecked")
        public <S> IngestCondition<S, K, V> ingest(PersistentList<S> list) {
            return keyExtractor -> valExtractor -> {
                final var holder = new AtomicReference<>(this.map.asTransient());

                list.forEach(item -> {
                    K k = keyExtractor.apply(item);
                    V v = valExtractor.apply(item);
                    holder.set(holder.get().put((K) Dop.normalize(k), v));
                });

                return new MapProjector<>(holder.get().freeze());
            };
        }

        @SuppressWarnings("unchecked")
        public <S> IngestCondition<S, K, V> ingest(Iterable<S> sequence) {
            return keyExtractor -> valExtractor -> {
                final var holder = new AtomicReference<>(this.map.asTransient());

                for (S item : sequence) {
                    K k = keyExtractor.apply(item);
                    V v = valExtractor.apply(item);
                    holder.set(holder.get().put((K) Dop.normalize(k), v));
                }

                return new MapProjector<>(holder.get().freeze());
            };
        }

        @SuppressWarnings("unchecked")
        public <S> MapPour<S, K, V> pour(S source) {
            return condition -> (keyExtractor, valExtractor) -> {
                final var holder = new AtomicReference<>(this.map.asTransient());
                while (condition.test(source)) {
                    final K k = keyExtractor.apply(source);
                    final V v = valExtractor.apply(source);
                    holder.set(holder.get().put((K) Dop.normalize(k), v));
                }
                return new MapProjector<>(holder.get().freeze());
            };
        }

        private <K2, V2> PersistentMap<K2, V2> morph(BiFunction<K, V, K2> keyGen,
                                                     BiFunction<K, V, V2> valGen) {
            final var holder = new AtomicReference<>(PersistentMap.<K2, V2>empty().asTransient());

            map.forEach((k, v) -> {
                holder.set(holder.get().put(keyGen.apply(k, v), valGen.apply(k, v)));
            });

            return holder.get().freeze();
        }

        @SuppressWarnings("unchecked")
        public MapProjector<K, V> updateFrom(PersistentMap<K, V> other) {
            final var holder = new AtomicReference<>(this.map.asTransient());
            other.forEach((k, v) -> {
                holder.set(holder.get().put((K) Dop.normalize(k), v));
            });
            return new MapProjector<>(holder.get().freeze());
        }

        public <T> ListProjector<T> intoList(BiFunction<K, V, T> transformer) {
            final var holder = new AtomicReference<>(PersistentList.<T>empty().asTransient());
            map.forEach((k, v) -> holder.set(holder.get().append(transformer.apply(k, v))));
            return new ListProjector<>(holder.get().freeze());
        }

        public <V2> MapProjector<K, V2> mapValues(Function<V, V2> valMapper) {
            return new MapProjector<>(morph((k, v) -> k, (k, v) -> valMapper.apply(v)));
        }

        @SuppressWarnings("unchecked")
        public MapProjector<K, V> put(K key, V value) {
            final var holder = new AtomicReference<>(this.map.asTransient());
            holder.set(holder.get().put((K) Dop.normalize(key), value));
            return new MapProjector<>(holder.get().freeze());
        }

        public <K2> MapProjector<K2, V> mapKeys(Function<K, K2> keyMapper) {
            return new MapProjector<>(morph((k, v) -> keyMapper.apply(k), (k, v) -> v));
        }

        public MapProjector<K, V> filter(BiPredicate<K, V> predicate) {
            final var holder = new AtomicReference<>(PersistentMap.<K, V>empty().asTransient());
            map.forEach((k, v) -> {
                if (predicate.test(k, v)) holder.set(holder.get().put(k, v));
            });
            return new MapProjector<>(holder.get().freeze());
        }

        public MapProjector<K, V> asLongAs(BiPredicate<K, V> predicate) {
            final var holder = new AtomicReference<>(PersistentMap.<K, V>empty().asTransient());

            try {
                map.forEach((k, v) -> {
                    if (predicate.test(k, v)) {
                        holder.set(holder.get().put(k, v));
                    } else {

                        throw MapBreak.INSTANCE;
                    }
                });
            } catch (MapBreak e) {
                // Exit safely; the holder contains the "LongAs" data
            }

            return new MapProjector<>(holder.get().freeze());
        }

        private static final class MapBreak extends RuntimeException {
            static final MapBreak INSTANCE = new MapBreak();
            private MapBreak() { super(null, null, false, false); }
        }

        public ListProjector<K> keys() { return intoList((k, v) -> k); }
        public ListProjector<V> values() { return intoList((k, v) -> v); }

        public PersistentMap<K, V> deploy() { return map; }
    }

    class ListProjector<E> {
        private final PersistentList<E> list;
        private ListProjector(PersistentList<E> list) { this.list = list; }

        @SuppressWarnings("unchecked")
        public <K> MapProjector<K, E> indexBy(Function<E, K> keyMapper) {
            final var holder = new AtomicReference<>(PersistentMap.<Object, E>empty().asTransient());
            list.forEach(item -> holder.set(holder.get().put(Dop.normalize(keyMapper.apply(item)), item)));
            return new MapProjector<>((PersistentMap<K, E>) holder.get().freeze());
        }

        @SuppressWarnings("unchecked")
        public <S> StreamPour<S, E> pour(S source) {
            return condition -> extractor -> {
                final var holder = new AtomicReference<>(this.list.asTransient());
                while (condition.test(source)) {
                    E item = extractor.apply(source);
                    holder.set(holder.get().append((E) Dop.normalize(item)));
                }
                return new ListProjector<>(holder.get().freeze());
            };
        }

        public <R> ListProjector<R> map(Function<E, R> mapper) {
            final var holder = new AtomicReference<>(PersistentList.<R>empty().asTransient());
            list.forEach(item -> holder.set(holder.get().append(mapper.apply(item))));
            return new ListProjector<>(holder.get().freeze());
        }

        public ListProjector<E> filter(Predicate<E> predicate) {
            final var holder = new AtomicReference<>(PersistentList.<E>empty().asTransient());
            list.forEach(item -> {
                if (predicate.test(item)) holder.set(holder.get().append(item));
            });
            return new ListProjector<>(holder.get().freeze());
        }

        public ListProjector<E> asLongAs(Predicate<E> predicate) {
            final var result = list.foldUntil(
                    PersistentList.<E>empty().asTransient(), //
                    (acc, item) -> predicate.test(item)//
                            ? PersistentList.Accumulator.cont(acc.append(item))//
                            : PersistentList.Accumulator.stop(acc)//
            );

            return new ListProjector<>(result.freeze());
        }
        @SuppressWarnings("unchecked")
        public ListProjector<E> append(E item) {
            final var holder = new AtomicReference<>(this.list.asTransient());

            holder.set(holder.get().append((E) Dop.normalize(item)));

            return new ListProjector<>(holder.get().freeze());
        }

        @SuppressWarnings("unchecked")
        public <S> IterateCondition<S, E> iterate(S initial) {
            return condition -> next -> extractor -> {
                final var holder = new AtomicReference<>(this.list.asTransient());

                for (S state = initial; condition.test(state); state = next.apply(state)) {
                    E item = extractor.apply(state);
                    holder.set(holder.get().append((E) Dop.normalize(item)));
                }

                return new ListProjector<>(holder.get().freeze());
            };
        }

        public PersistentList<E> deploy() { return list; }
    }

    interface IterateCondition<S, E> {
        IterateNext<S, E> whileTrue(Predicate<S> condition);
    }

    interface IterateNext<S, E> {
        IterateExtraction<S, E> step(Function<S, S> next);
    }

    interface IterateExtraction<S, E> {
        ListProjector<E> extract(Function<S, E> extractor);
    }

    interface StreamPour<S, E> {
        StreamExtraction<S, E> whileTrue(Predicate<S> condition);
    }
    interface StreamExtraction<S, E> {
        ListProjector<E> extract(Function<S, E> extractor);
    }
    interface MapPour<S, K, V> {
        MapExtraction<S, K, V> whileTrue(Predicate<S> condition);
    }
    interface MapExtraction<S, K, V> {
        MapProjector<K, V> extract(Function<S, K> keyExtractor, Function<S, V> valExtractor);
    }

    interface IngestCondition<S, K, V> {
        IngestExtraction<S, K, V> extractKey(Function<S, K> keyExtractor);
    }

    interface IngestExtraction<S, K, V> {
        MapProjector<K, V> extractValue(Function<S, V> valExtractor);
    }
}