package Sky.Cat.CTC.chunk;

import Sky.Cat.CTC.permission.PermType;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ChunkEventHandlers {
    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            // Handle event on the server-side only.
            if (world.isClient()) return ActionResult.PASS;

            // Check if the player has enough permission to break block in chunk
            if (!ChunkManager.getInstance().hasPermission(player, pos, world.getRegistryKey(), PermType.BREAK)) {
                player.sendMessage(Text.literal("Missing permission to break blocks in this chunk."), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();

            if (!ChunkManager.getInstance().hasPermission(player, pos, world.getRegistryKey(), PermType.INTERACT)) {
                player.sendMessage(Text.literal("Missing permission to interact with blocks in this chunk."), true);
            }

            return ActionResult.PASS;
        });
    }
}
