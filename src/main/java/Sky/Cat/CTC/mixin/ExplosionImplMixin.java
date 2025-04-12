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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {

    @Shadow public abstract Entity getEntity();

    @Shadow public abstract LivingEntity getCausingEntity();

    @Shadow public abstract ServerWorld getWorld();

    @Shadow public abstract Vec3d getPosition();

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

    @Inject(method = "getBlocksToDestroy", at = @At("HEAD"), cancellable = true)
    private void onGetBlocksToDestroy(CallbackInfoReturnable<List<BlockPos>> cir) {
        try {
            Entity direct = this.getEntity();
            LivingEntity causingEntity = this.getCausingEntity();
            ServerWorld world = this.getWorld();
            Vec3d position = this.getPosition();

            BlockPos blockPos = BlockPos.ofFloored(position);
            ChunkPosition chunkPosition = new ChunkPosition(blockPos, world.getRegistryKey());

            ChunkManager chunkManager = ChunkManager.getInstance();

            if (chunkManager.isChunkClaimed(chunkPosition)) {
                ClaimedChunk chunk = chunkManager.getClaimedChunk(chunkPosition);

                Team owner = null;

                if (chunk != null) {
                    owner = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());
                }

                boolean allowExplosion = false;
                if (causingEntity instanceof PlayerEntity player) {
                    allowExplosion = chunkManager.hasPermission(player, blockPos, world.getRegistryKey(), PermType.BREAK);

                    if (!allowExplosion) {
                        player.sendMessage(Text.literal("You don't have permission to use explosives on this chunk"), true);
                    }
                }

                if (!allowExplosion) {
                    ChunkManager.LOGGER.info("| Explosion intercepted:");
                    ChunkManager.LOGGER.info("| Position: {}, Team: {}", chunkPosition, owner != null ? owner.getTeamName() : "unknown");
                    ChunkManager.LOGGER.info("| Source: {}", direct != null ? direct.getName().getLiteralString() : "unknown");
                    ChunkManager.LOGGER.info("| Caused by: {}", causingEntity != null ? causingEntity.getName().getLiteralString() : "unknown");

                    cir.setReturnValue(List.of());
                }
            }

        } catch (Exception e) {
            ChunkManager.LOGGER.error("Error in ExplosionImplMixin: ", e);
        }
    }
}
