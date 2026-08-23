package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Describes one group of entities to spawn within a wave.
 * Either an {@link EntityType} or a custom factory {@link Supplier} is required.
 */
public final class EslSpawnEntry {

    private final @Nullable EntityType<? extends Mob> entityType;
    private final @Nullable Supplier<Mob> factory;
    private final int count;
    private final double radiusMin;
    private final double radiusMax;
    private final int spawnDelayTicks;
    private final @Nullable Consumer<Mob> postSpawnModifier;

    private EslSpawnEntry(Builder b) {
        this.entityType = b.entityType;
        this.factory = b.factory;
        this.count = b.count;
        this.radiusMin = b.radiusMin;
        this.radiusMax = b.radiusMax;
        this.spawnDelayTicks = b.spawnDelayTicks;
        this.postSpawnModifier = b.postSpawnModifier;
    }

    public @Nullable EntityType<? extends Mob> entityType() { return entityType; }
    public @Nullable Supplier<Mob> factory() { return factory; }
    public int count() { return count; }
    public double radiusMin() { return radiusMin; }
    public double radiusMax() { return radiusMax; }
    public int spawnDelayTicks() { return spawnDelayTicks; }
    public @Nullable Consumer<Mob> postSpawnModifier() { return postSpawnModifier; }

    public static Builder of(EntityType<? extends Mob> type, int count) {
        Builder b = new Builder();
        b.entityType = type;
        b.count = count;
        return b;
    }

    public static Builder ofFactory(Supplier<Mob> factory, int count) {
        Builder b = new Builder();
        b.factory = factory;
        b.count = count;
        return b;
    }

    public static final class Builder {
        private @Nullable EntityType<? extends Mob> entityType;
        private @Nullable Supplier<Mob> factory;
        private int count = 1;
        private double radiusMin = 8;
        private double radiusMax = 24;
        private int spawnDelayTicks = 0;
        private @Nullable Consumer<Mob> postSpawnModifier;

        private Builder() {}

        public Builder radius(double min, double max) { this.radiusMin = min; this.radiusMax = max; return this; }
        public Builder spawnDelay(int ticks) { this.spawnDelayTicks = ticks; return this; }
        public Builder onSpawn(Consumer<Mob> modifier) { this.postSpawnModifier = modifier; return this; }

        public EslSpawnEntry build() {
            if (entityType == null && factory == null) throw new IllegalStateException("EslSpawnEntry requires entityType or factory");
            if (count <= 0) throw new IllegalStateException("EslSpawnEntry count must be > 0");
            return new EslSpawnEntry(this);
        }
    }
}
