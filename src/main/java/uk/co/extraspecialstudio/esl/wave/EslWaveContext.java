package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Immutable context snapshot passed to wave callbacks and events.
 */
public final class EslWaveContext {

    private final EslWaveController controller;
    private final ServerLevel level;
    private final BlockPos center;
    private final @Nullable ServerPlayer triggeringPlayer;

    public EslWaveContext(EslWaveController controller, ServerLevel level, BlockPos center,
                          @Nullable ServerPlayer triggeringPlayer) {
        this.controller = controller;
        this.level = level;
        this.center = center;
        this.triggeringPlayer = triggeringPlayer;
    }

    public EslWaveController controller() { return controller; }
    public ServerLevel level() { return level; }
    public BlockPos center() { return center; }
    public @Nullable ServerPlayer triggeringPlayer() { return triggeringPlayer; }
}
