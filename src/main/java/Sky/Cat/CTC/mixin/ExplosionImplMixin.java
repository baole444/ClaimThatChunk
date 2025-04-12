package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.chunk.ClaimedChunk;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionImpl;
import org.joml.Math;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {

    @Shadow public abstract Entity getEntity();

    @Shadow public abstract LivingEntity getCausingEntity();

    @Shadow public abstract ServerWorld getWorld();

    @Shadow public abstract Vec3d getPosition();

    @Shadow public abstract float getPower();

    @Shadow @Final private ServerWorld world;

    // Debug version
    //@Inject(method = "getBlocksToDestroy", at = @At("HEAD"), cancellable = true)
    private void onGetBlocksToDestroyDebug(CallbackInfoReturnable<List<BlockPos>> cir) {
        try {
            Explosion explosion = (Explosion)(Object)this;

            Main.LOGGER.info("Explosion detected: Entity {}, CausingEntity={}",
                    this.getEntity() != null ? this.getEntity().getClass().getSimpleName() : "null",
                    this.getCausingEntity() != null ? this.getCausingEntity().getClass().getSimpleName() : "null");

            cir.setReturnValue(List.of());
            Main.LOGGER.info("Explosion blocked.");
        } catch (Exception e) {
            Main.LOGGER.error("Error in ExplosionImplMixin: ", e);
        }
    }

    // Version 2
    //@Inject(method = "getBlocksToDestroy", at = @At("HEAD"), cancellable = true)
    private void onGetBlocksToDestroy(CallbackInfoReturnable<List<BlockPos>> cir) {
        try {
            Entity direct = this.getEntity();
            LivingEntity causingEntity = this.getCausingEntity();
            ServerWorld world = this.getWorld();
            Vec3d position = this.getPosition();

            BlockPos blockPos = BlockPos.ofFloored(position);
            ChunkPosition chunkPosition = new ChunkPosition(blockPos, world.getRegistryKey());

            ChunkManager chunkManager = ChunkManager.getInstance();

            // Check on claim chunk only
            if (chunkManager.isChunkClaimed(chunkPosition)) {
                ClaimedChunk chunk = chunkManager.getClaimedChunk(chunkPosition);

                Team owner = null;

                if (chunk != null) {
                    owner = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());
                }

                // Check permission on the player that caused the explosion
                boolean allowExplosion = false;
                if (causingEntity instanceof PlayerEntity player) {
                    allowExplosion = chunkManager.hasPermission(player, blockPos, world.getRegistryKey(), PermType.BREAK);

                    if (!allowExplosion) {
                        player.sendMessage(Text.literal("Missing permission to use explosives on this chunk."), true);
                    }
                }

                if (!allowExplosion) {
                    ChunkManager.LOGGER.info("| Explosion intercepted:");
                    ChunkManager.LOGGER.info("| Position: {}, Team: {}", chunkPosition, owner != null ? owner.getTeamName() : "unknown");
                    ChunkManager.LOGGER.info("| Source: {}", direct != null ? direct.getClass().getSimpleName() : "unknown");
                    ChunkManager.LOGGER.info("| Caused by: {}", causingEntity != null ? causingEntity.getName().getLiteralString() : "unknown");

                    cir.setReturnValue(List.of());
                }
            }
        } catch (Exception e) {
            ChunkManager.LOGGER.error("Error in ExplosionImplMixin: ", e);
        }
    }

    @Inject(method = "getBlocksToDestroy", at = @At("RETURN"), cancellable = true)
    private void filterBlockToDestroy(CallbackInfoReturnable<List<BlockPos>> cir) {
        try {
            // Get the vanilla list of block that would be destroyed.
            List<BlockPos> blocksToDestroy = cir.getReturnValue();

            // Empty destroy list
            if (blocksToDestroy == null || blocksToDestroy.isEmpty()) return;

            Entity direct = this.getEntity();
            LivingEntity causingEntity = this.getCausingEntity();
            ServerWorld world = this.getWorld();
            ChunkManager chunkManager = ChunkManager.getInstance();

            // The player that triggers the explosion
            PlayerEntity player = null;

            if (causingEntity instanceof PlayerEntity playerEntity) {
                player = playerEntity;
            }

            // Let admin skip protection on BYPASS enabled.
            if (player != null && Main.ADMIN_BYPASS && player.hasPermissionLevel(2)) return;

            // List of block that can be destroyed.
            List<BlockPos> safeToDestroy = new ArrayList<>();

            // Prevent duplicate logs and notifications.
            Set<ChunkPosition> loggedChunks = new HashSet<>();
            Set<ChunkPosition> notifiedChunks = new HashSet<>();

            // Check each block in the original list
            for (BlockPos pos : blocksToDestroy) {
                ChunkPosition chunkPosition = new ChunkPosition(pos, world.getRegistryKey());

                // Is the chunk claimed?
                if (chunkManager.isChunkClaimed(chunkPosition)) {
                    boolean hasPermission = false;

                    if (player != null) {
                        hasPermission = chunkManager.hasPermission(player, pos, world.getRegistryKey(), PermType.BREAK);
                    }

                    if (!hasPermission) {
                        // Log once per chunk
                        if (!loggedChunks.contains(chunkPosition)) {
                            ClaimedChunk chunk = chunkManager.getClaimedChunk(chunkPosition);
                            Team owner = chunk != null ? TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId()) : null;

                            ChunkManager.LOGGER.info("| Explosion intercepted at:");
                            ChunkManager.LOGGER.info("| Position: {}, Team: {}", chunkPosition, owner != null ? owner.getTeamName() : "unknown");
                            ChunkManager.LOGGER.info("| Source: {}, Power {}", direct != null ? direct.getClass().getSimpleName() : "unknown", this.getPower());
                            ChunkManager.LOGGER.info("| Caused by: {}", causingEntity != null ? causingEntity.getName().getLiteralString() : "unknown");

                            loggedChunks.add(chunkPosition);
                        }

                        if (player != null && !notifiedChunks.contains(chunkPosition)) {
                            player.sendMessage(Text.literal("Missing permission to use explosives on this chunk."), true);
                            notifiedChunks.add(chunkPosition);
                        }

                        continue;
                    }
                }

                // If block is in unclaimed chunk or the player has permission.
                safeToDestroy.add(pos);
            }

            // Return the filtered list.
            cir.setReturnValue(safeToDestroy);

        } catch (Exception e) {
            ChunkManager.LOGGER.error("Error in filterBlockToDestroy: ", e);
        }
    }

}
