package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.Main;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
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

    @Inject(method = "getBlocksToDestroy", at = @At("HEAD"), cancellable = true)
    private void onGetBlocksToDestroy(CallbackInfoReturnable<List<BlockPos>> cir) {
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

}
