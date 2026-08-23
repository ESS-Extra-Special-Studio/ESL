package uk.co.extraspecialstudio.esl.wave;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ordered list of {@link EslWaveDefinition} with global settings for the full sequence.
 */
public final class EslWaveSequence {

    private final List<EslWaveDefinition> waves;
    private final int delayBetweenWavesTicks;
    private final @Nullable Consumer<EslWaveContext> onComplete;
    private final @Nullable Consumer<EslWaveContext> onFailed;
    private final @Nullable Consumer<EslWaveContext> onWaveCleared;

    private EslWaveSequence(Builder b) {
        this.waves = Collections.unmodifiableList(new ArrayList<>(b.waves));
        this.delayBetweenWavesTicks = b.delayBetweenWavesTicks;
        this.onComplete = b.onComplete;
        this.onFailed = b.onFailed;
        this.onWaveCleared = b.onWaveCleared;
    }

    public List<EslWaveDefinition> waves() { return waves; }
    public int totalWaves() { return waves.size(); }
    public int delayBetweenWavesTicks() { return delayBetweenWavesTicks; }
    public @Nullable Consumer<EslWaveContext> onComplete() { return onComplete; }
    public @Nullable Consumer<EslWaveContext> onFailed() { return onFailed; }
    public @Nullable Consumer<EslWaveContext> onWaveCleared() { return onWaveCleared; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<EslWaveDefinition> waves = new ArrayList<>();
        private int delayBetweenWavesTicks = 100; // 5 seconds default
        private @Nullable Consumer<EslWaveContext> onComplete;
        private @Nullable Consumer<EslWaveContext> onFailed;
        private @Nullable Consumer<EslWaveContext> onWaveCleared;

        private Builder() {}

        public Builder wave(EslWaveDefinition wave) { waves.add(wave); return this; }
        public Builder wave(EslWaveDefinition.Builder wave) { waves.add(wave.build()); return this; }
        public Builder delayBetween(int ticks) { this.delayBetweenWavesTicks = ticks; return this; }
        public Builder onComplete(Consumer<EslWaveContext> cb) { this.onComplete = cb; return this; }
        public Builder onFailed(Consumer<EslWaveContext> cb) { this.onFailed = cb; return this; }
        public Builder onWaveCleared(Consumer<EslWaveContext> cb) { this.onWaveCleared = cb; return this; }

        public EslWaveSequence build() {
            if (waves.isEmpty()) throw new IllegalStateException("EslWaveSequence requires at least one wave");
            return new EslWaveSequence(this);
        }
    }
}
