package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
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
    private void onCanDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof World realWorld)) return;

        if (realWorld.isClient()) {
            Main.LOGGER.info("If this log occurred, I failed to get the actual world the explosion happened.");
            return;
        }

        ChunkManager chunkManager = ChunkManager.getInstance();
        ChunkPosition chunkPosition = new ChunkPosition(pos, realWorld.getRegistryKey());

        if (chunkManager.isChunkClaimed(chunkPosition)) {
            Entity directEntity = explosion.getEntity();

            LivingEntity causingEntity = explosion.getCausingEntity();

            Team ownerTeam = null;
            if (chunkManager.getClaimedChunk(chunkPosition) != null) {
                ownerTeam = TeamManager.getInstance().getTeamById(chunkManager.getClaimedChunk(chunkPosition).getOwnerTeamId());
            }

            // Duck all explosion on all claimed chunk for testing purpose.
            cir.setReturnValue(false);

            ChunkManager.LOGGER.info("| Explosion debug information:");
            ChunkManager.LOGGER.info("| - Position: {}", chunkPosition);
            ChunkManager.LOGGER.info("| - Team: {}", ownerTeam != null ? ownerTeam.getTeamName() : "Unknown Team");
            ChunkManager.LOGGER.info("| - Direct cause: {}", directEntity != null ? directEntity.getClass().getSimpleName() : "Unknown");
            ChunkManager.LOGGER.info("| - Blame: {}", causingEntity != null ? causingEntity.getClass().getSimpleName() : "Unknown");

            if (causingEntity instanceof PlayerEntity player) {
                boolean hasPermission = chunkManager.hasPermission(player, pos, realWorld.getRegistryKey(), PermType.BREAK);
                Main.LOGGER.info("| Player: {} ({})", player.getName().getLiteralString(), hasPermission ? "Allowed" : "Blocked");

                player.sendMessage(Text.literal("Explosion in claimed chunk blocked on purpose."), true);
            }
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "shouldDamage", at = @At("HEAD"), cancellable = true)
    private void onShouldDamage(Explosion explosion, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        World world = entity.getWorld();

        if (world.isClient()) return;

        BlockPos pos = entity.getBlockPos();

        LivingEntity causingEntity = explosion.getCausingEntity();

        ChunkManager chunkManager = ChunkManager.getInstance();
        ChunkPosition chunkPosition = new ChunkPosition(pos, world.getRegistryKey());

        if (chunkManager.isChunkClaimed(chunkPosition)) {
            if (causingEntity instanceof PlayerEntity player) {
                if (!chunkManager.hasPermission(player, pos, world.getRegistryKey(), PermType.BREAK)) {
                    ChunkManager.LOGGER.info("Damage prevention on {} caused by {}", chunkPosition, player.getName().getLiteralString());
                }

                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(false);
                ChunkManager.LOGGER.info("Damage prevention on {}", chunkPosition);
            }
        }
    }
}
