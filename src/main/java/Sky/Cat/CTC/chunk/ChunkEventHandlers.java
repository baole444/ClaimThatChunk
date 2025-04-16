package Sky.Cat.CTC.chunk;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import Sky.Cat.CTC.team.TeamMember;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;

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
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;

            TeamManager teamManager = TeamManager.getInstance();
            ChunkManager chunkManager = ChunkManager.getInstance();

            Team attackerTeam = teamManager.getTeamByPlayer(player.getUuid());

            ClaimedChunk targetCurrentChunk = chunkManager.getClaimedChunkAt(entity.getBlockPos(), world.getRegistryKey());

            // PvP
            if (entity instanceof PlayerEntity target) {

                Team targetTeam = teamManager.getTeamByPlayer(target.getUuid());

                // both attacker and target have no team.
                if (targetTeam == null && attackerTeam == null) {
                    return ActionResult.PASS;
                }

                // Attacker is in a team
                if (attackerTeam != null) {
                    TeamMember attacked = attackerTeam.getTeamMember().get(player.getUuid());

                    // target has no team
                    if (targetTeam == null) return ActionResult.PASS;

                    TeamMember targeted = targetTeam.getTeamMember().get(target.getUuid());

                    // Attacker and target are on the same team.
                    if (attackerTeam.getTeamId().equals(targetTeam.getTeamId())) {
                        // They both have team kill turn on;
                        if (attacked.getPermission().hasPermission(PermType.KILL_TEAMMATE) && targeted.getPermission().hasPermission(PermType.KILL_TEAMMATE)) {
                            return ActionResult.PASS;
                        }

                        // Team kill is turned off.
                        return ActionResult.FAIL;
                    } else {
                        // target is not on a claimed chunk
                        if (targetCurrentChunk == null) return ActionResult.PASS;

                            // target is on attacker's claimed chunk.
                        if (targetCurrentChunk.getOwnerTeamId().equals(attackerTeam.getTeamId())) {
                            return ActionResult.PASS;
                        }

                        // target is on their claimed chunk.
                        if (targetCurrentChunk.getOwnerTeamId().equals(targetTeam.getTeamId())) {
                            return ActionResult.FAIL;
                        }
                    }
                } else {
                    if (targetCurrentChunk == null) return ActionResult.PASS;

                    if (targetCurrentChunk.getOwnerTeamId().equals(targetTeam.getTeamId())) {
                        return ActionResult.FAIL;
                    }
                }
            }

            // Against passive entities.
            else if (entity instanceof PassiveEntity passiveEntity) {
                if (targetCurrentChunk == null) return ActionResult.PASS;

                if (attackerTeam == null) return ActionResult.FAIL;

                if (attackerTeam.getTeamId().equals(targetCurrentChunk.getOwnerTeamId())) {
                    TeamMember attacked = attackerTeam.getTeamMember().get(player.getUuid());

                    if (attacked.getPermission().hasPermission(PermType.KILL_FRIENDLY)) return ActionResult.PASS;
                }

                return ActionResult.FAIL;
            }

            // Against hostile entities.
            else if (entity instanceof HostileEntity hostileEntity) {
                if (targetCurrentChunk == null) return ActionResult.PASS;

                if (attackerTeam == null) return ActionResult.FAIL;

                if (attackerTeam.getTeamId().equals(targetCurrentChunk.getOwnerTeamId())) {
                    TeamMember attacked = attackerTeam.getTeamMember().get(player.getUuid());

                    if (attacked.getPermission().hasPermission(PermType.KILL_FRIENDLY)) return ActionResult.PASS;
                }

                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}
