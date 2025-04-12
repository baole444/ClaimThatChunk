package Sky.Cat.CTC.mixin;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.permission.PermType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkBuildPermission(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
        // Check on server-side only
        if (context.getWorld().isClient()) return;

        PlayerEntity player = context.getPlayer();

        // None-player block placement
        // TODO: Not really related to build permission check but make sure to also prevent
        //  griefing attempt like lava casting, tnt cannon and live stock kill on claimed chunk.
        if (player == null) return;

        BlockPos pos = context.getBlockPos();

        if (!ChunkManager.getInstance().hasPermission(player, pos, context.getWorld().getRegistryKey(), PermType.BUILD)) {
            player.sendMessage(Text.literal("Missing permission to build on this chunk."), true);
            callbackInfoReturnable.setReturnValue(ActionResult.FAIL);
        }
    }
}
