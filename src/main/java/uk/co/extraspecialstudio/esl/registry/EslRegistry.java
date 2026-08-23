package uk.co.extraspecialstudio.esl.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe named registry primitive for ES stack consumers.
 *
 * @param <T> value type
 */
public final class EslRegistry<T> {
    private final ConcurrentHashMap<String, T> entries = new ConcurrentHashMap<>();

    public void register(String id, T value) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must be non-blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must be non-null");
        }
        T previous = entries.putIfAbsent(id, value);
        if (previous != null) {
            throw new IllegalStateException("Already registered: " + id);
        }
    }

    public void registerOrReplace(String id, T value) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must be non-blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must be non-null");
        }
        entries.put(id, value);
    }

    public Optional<T> get(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public boolean contains(String id) {
        return entries.containsKey(id);
    }

    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Collection<String> keys() {
        return Collections.unmodifiableCollection(entries.keySet());
    }

    public void clear() {
        entries.clear();
    }
}
