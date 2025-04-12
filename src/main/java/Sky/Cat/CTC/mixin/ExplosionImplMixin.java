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

import java.util.List;

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

    @Inject(method = "getBlocksToDestroy", at = @At("HEAD"), cancellable = true)
    private void onGetBlocksToDestroyV2(CallbackInfoReturnable<List<BlockPos>> cir) {
        try {
            Entity direct = this.getEntity();
            LivingEntity causingEntity = this.getCausingEntity();
            ServerWorld world = this.getWorld();
            Vec3d position = this.getPosition();

            BlockPos blockPos = BlockPos.ofFloored(position);
            ChunkPosition chunkPosition = new ChunkPosition(blockPos, world.getRegistryKey());

            ChunkManager chunkManager = ChunkManager.getInstance();

            boolean isChunkClaimed = chunkManager.isChunkClaimed(chunkPosition);

            if (!isChunkClaimed) {
                float radius = this.getPower() * 2.0f; // explosion radius

                int checkRadius = (int) Math.ceil(radius);

                for (int x = -checkRadius; x <= checkRadius && !isChunkClaimed; x++) {
                    for (int z = - checkRadius; z <= checkRadius && !isChunkClaimed; z++) {
                        ChunkPosition nearbyPos = new ChunkPosition(
                                blockPos.add(x * 16, 0, z * 16),
                                world.getRegistryKey()
                        );

                        if (chunkManager.isChunkClaimed(nearbyPos)) {
                            isChunkClaimed = true;
                            break;
                        }
                    }
                }
            }

            if (isChunkClaimed) {
                ClaimedChunk chunk = chunkManager.getClaimedChunk(chunkPosition);
                Team owner = chunk != null ? TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId()) : null;

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
                    ChunkManager.LOGGER.info("| Source: {}, Power: {}", direct != null ? direct.getClass().getSimpleName() : "unknown", this.getPower());
                    ChunkManager.LOGGER.info("| Caused by: {}", causingEntity != null ? causingEntity.getName().getLiteralString() : "unknown");

                    cir.setReturnValue(List.of());
                }
            }
        } catch (Exception e) {
            ChunkManager.LOGGER.error("Error in ExplosionImplMixinV2: ", e);
        }
    }
}
