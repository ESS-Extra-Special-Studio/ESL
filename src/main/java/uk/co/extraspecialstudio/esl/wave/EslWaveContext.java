package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;

/**
 * Immutable context snapshot passed to wave callbacks and events.
 */
public final class EslWaveContext {

    private final EslWaveController controller;
    private final ServerWorld level;
    private final BlockPos center;
    private final @Nullable ServerPlayerEntity triggeringPlayer;

    public EslWaveContext(EslWaveController controller, ServerWorld level, BlockPos center,
                          @Nullable ServerPlayerEntity triggeringPlayer) {
        this.controller = controller;
        this.level = level;
        this.center = center;
        this.triggeringPlayer = triggeringPlayer;
    }

    public EslWaveController controller() { return controller; }
    public ServerWorld level() { return level; }
    public BlockPos center() { return center; }
    public @Nullable ServerPlayerEntity triggeringPlayer() { return triggeringPlayer; }
}
