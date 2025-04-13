package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.permission.PermType;
import net.minecraft.block.ButtonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Shadow public abstract Entity getOwner();

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void stopInteractionsOnCollision(HitResult hitResult, CallbackInfo ci) {
        try {
            ProjectileEntity projectile = (ProjectileEntity) (Object) this;

            // Processed by server only.
            if (projectile.getWorld().isClient()) return;

            HitResult.Type type = hitResult.getType();

            if (type != HitResult.Type.BLOCK && type != HitResult.Type.ENTITY) return;

            int x = (int) Math.floor(hitResult.getPos().getX());

            int z = (int) Math.floor(hitResult.getPos().getZ());

            BlockPos blockPos = new BlockPos(x, 0, z);

            ChunkManager chunkManager = ChunkManager.getInstance();
            ChunkPosition chunkPosition = new ChunkPosition(blockPos, projectile.getWorld().getRegistryKey());

            // Is the block in claimed chunk?
            if (chunkManager.isChunkClaimed(chunkPosition)) {
                Entity owner = this.getOwner();

                boolean hasPermission = false;

                if (owner instanceof PlayerEntity player) {
                    hasPermission = chunkManager.hasPermission(player, blockPos, projectile.getWorld().getRegistryKey(), PermType.INTERACT);

                    if (!hasPermission) {
                        player.sendMessage(Text.literal("Missing permission to interact with blocks in claimed chunk."),true);
                    }
                }

                // Cancel event and remove projectile
                if (!hasPermission) {
                    ChunkManager.LOGGER.info("| Projectile intercepted at: ");
                    ChunkManager.LOGGER.info("| Position: {}", chunkPosition);
                    ChunkManager.LOGGER.info("| Projectile: {}", projectile.getClass().getSimpleName());

                    projectile.discard();
                    ci.cancel();
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("Error in ProjectileEntityMixin.stopInteractionsOnCollision: ", e);
        }
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void stopBlockLogicAfterCollision(BlockHitResult blockHitResult, CallbackInfo ci) {
        try {
            ProjectileEntity projectile = (ProjectileEntity) (Object) this;

            if (projectile == null) {
                Main.LOGGER.error("Projectile was cleared on this check!");
            }

            // Processed by server only.
            if (projectile.getWorld().isClient()) return;

            BlockPos blockPos = blockHitResult.getBlockPos();

            ChunkManager chunkManager = ChunkManager.getInstance();
            ChunkPosition chunkPosition = new ChunkPosition(blockPos, projectile.getWorld().getRegistryKey());

            if (chunkManager.isChunkClaimed(chunkPosition)) {
                Entity owner = this.getOwner();

                boolean hasPermission = false;

                if (owner instanceof PlayerEntity player) {

                    hasPermission = chunkManager.hasPermission(player, blockPos, projectile. getWorld().getRegistryKey(), PermType.INTERACT);
                    ChunkManager.LOGGER.info("Permission is {}", hasPermission);
                }

                if (!hasPermission) {
                    ChunkManager.LOGGER.info("| Block logic for projectile intercepted:");
                    ChunkManager.LOGGER.info("| Position: {}", chunkPosition);

                    ci.cancel();
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("Error in ProjectEntityMixin.stopBlockLogicAfterCollision: ", e);
        }
    }
}
