package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.chunk.ClaimedChunk;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExplosionBehavior.class)
public class ExplosionBehaviorMixin {

    @Inject(method = "canDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onCanDestroyBlock(Explosion explosion, BlockView blockView, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
        try {
            ChunkManager.LOGGER.info("Checking possible damage block");
            // Process on server only
            if (!(blockView instanceof World world) || world.isClient()) return;

            ChunkManager chunkManager = ChunkManager.getInstance();
            ChunkPosition chunkPosition = new ChunkPosition(pos, world.getRegistryKey());


            if (!chunkManager.isChunkClaimed(chunkPosition)) return;

            LivingEntity causingEntity = explosion.getCausingEntity();

            boolean hasPermission = false;

            if (causingEntity instanceof PlayerEntity player) {
                hasPermission = chunkManager.hasPermission(player, pos, world.getRegistryKey(), PermType.BREAK);
                ChunkManager.LOGGER.info("Checking permission, value is {}", hasPermission);
            }

            if (!hasPermission) {
                cir.setReturnValue(false);

                Team owner = null;
                ClaimedChunk chunk = chunkManager.getClaimedChunk(chunkPosition);
                if (chunk != null) {
                    owner = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());
                }

                ChunkManager.LOGGER.debug("| Protected block:");
                ChunkManager.LOGGER.debug("| Position: {}", pos);
                ChunkManager.LOGGER.debug("| Team: {}", owner != null ? owner.getTeamName() : "unknown");
            }
        } catch (Exception e) {
            ChunkManager.LOGGER.error("Error in ExplosionBehaviorMixin (canDestroyBlock): ", e);
        }

    }
}
