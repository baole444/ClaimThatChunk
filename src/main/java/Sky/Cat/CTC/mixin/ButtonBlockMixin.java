package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.permission.PermType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonBlock.class)
public class ButtonBlockMixin {
    @Inject(method = "tryPowerWithProjectiles", at = @At("HEAD"), cancellable = true)
    private void canPowerWithProjectiles(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        try {
            // Process by server only.
            if (world.isClient()) return;

            ChunkManager chunkManager = ChunkManager.getInstance();
            ChunkPosition chunkPosition = new ChunkPosition(pos, world.getRegistryKey());

            if (chunkManager.isChunkClaimed(chunkPosition)) {

                PersistentProjectileEntity projectile = world.getNonSpectatingEntities(PersistentProjectileEntity.class,
                                state.getOutlineShape(world, pos).getBoundingBox().offset(pos))
                        .stream().findFirst().orElse(null);

                if (projectile == null) return;

                Entity owner = projectile.getOwner();

                boolean hasPermission = false;

                if (owner instanceof PlayerEntity player) {
                    hasPermission = chunkManager.hasPermission(player, pos, world.getRegistryKey(), PermType.INTERACT);
                }

                if (!hasPermission) {
                    ChunkManager.LOGGER.debug("| Prevented a button from activation at {}", chunkPosition);
                    ci.cancel();
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("Error in ButtonBlockMixin: ", e);
        }
    }
}
