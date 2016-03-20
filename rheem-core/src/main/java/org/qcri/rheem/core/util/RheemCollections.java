package org.qcri.rheem.core.util;

import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utilities to operate {@link java.util.Collection}s.
 */
public class RheemCollections {

    private RheemCollections() {
    }

    public static <K, V> void put(Map<K, Collection<V>> map, K key, V value) {
        map.compute(key, (k, values) -> {
            if (values == null) {
                values = new LinkedList<>();
            }
            values.add(value);
            return values;
        });
    }

    /**
     * Provides the given {@code collection} as {@link Set}, thereby checking if it is already a {@link Set}.
     */
    public static <T> Set<T> asSet(Collection<T> collection) {
        if (collection instanceof Set<?>) {
            return (Set<T>) collection;
        }
        return new HashSet<>(collection);
    }

    /**
     * Validate that there is only a single element in the {@code collection} and return it.
     */
    public static <T> T getSingle(Collection<T> collection) {
        Validate.isTrue(collection.size() == 1);
        return getAny(collection);
    }

    /**
     * Validate that there is at most one element in the {@code collection} and return it (or {@code null} otherwise).
     */
    public static <T> T getSingleOrNull(Collection<T> collection) {
        Validate.isTrue(collection.size() <= 1, "Expected 0 or 1 elements, found %d.", collection.size());
        return collection.isEmpty() ? null : getAny(collection);
    }

    /**
     * Return any element from the {@code collection}.
     */
    public static <T> T getAny(Collection<T> collection) {
        return collection.iterator().next();
    }

    /**
     * Return a new {@link List} with mapped values.
     */
    public static <S, T> List<T> map(List<S> list, Function<S, T> mapFunction) {
        List<T> result = new ArrayList<>(list.size());
        for (S element : list) {
            result.add(mapFunction.apply(element));
        }
        return result;
    }

    /**
     * Return a new {@link List} with mapped values.
     */
    public static <S, T> List<T> map(List<S> list, BiFunction<Integer, S, T> mapFunction) {
        List<T> result = new ArrayList<>(list.size());
        int i = 0;
        for (S element : list) {
            result.add(mapFunction.apply(i++, element));
        }
        return result;
    }

    /**
     * Returns an {@link Iterable} that iterates the cross product of the given {@code iterables}.
     *
     * @param iterables should be iterable multiple times
     */
    public static <T> Iterable<List<T>> streamedCrossProduct(List<? extends Iterable<T>> iterables) {
        return new CrossProductIterable<>(iterables);
    }

    /**
     * Creates an {@link ArrayList} that is filled with {@code k} {@code null}s.
     */
    public static <T> ArrayList<T> createNullFilledArrayList(int k) {
        ArrayList<T> list = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            list.add(null);
        }
        return list;
    }
}
