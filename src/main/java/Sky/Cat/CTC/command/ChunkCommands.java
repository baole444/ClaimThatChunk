package Sky.Cat.CTC.command;

import Sky.Cat.CTC.Utilities;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.chunk.ClaimedChunk;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import Sky.Cat.CTC.team.TeamMember;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

import static Sky.Cat.CTC.Utilities.parseDate;

public class ChunkCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ctc")
                .then(CommandManager.literal("chunk")
                        // Information command
                        .then(CommandManager.literal("info")
                                .executes(ChunkCommands::executeInfo)
                        )
                        // Claim command
                        .then(CommandManager.literal("claim")
                                .requires(ChunkCommands::hasClaimPermission)
                                .executes(ChunkCommands::executeClaim)
                        )
                )
        );
    }

    private static boolean hasClaimPermission(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();

            Team team = TeamManager.getInstance().getTeamByPlayer(player.getUuid());

            if (team == null) return false;

            TeamMember member = team.getTeamMember().get(player.getUuid());

            if (member != null) {
                return member.getPermission().hasPermission(PermType.CLAIM);
            }

            return false;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();

            BlockPos instancePosition = player.getBlockPos();

            RegistryKey<World> dimension = player.getWorld().getRegistryKey();

            ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunkAt(instancePosition, dimension);

            if (chunk == null) {
                source.sendFeedback(() -> Text.literal("This chunk is not yet claimed!"), false);
                source.sendFeedback(() -> Text.literal("You can claim it for your team with /ctc chunk claim"), false);
            } else {
                Team team = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());

                String teamName;

                if (team == null) {
                    teamName = "Error: No team found.";
                    ChunkManager.LOGGER.error("A claimed-chunk at {} was not cleared properly when its team was disbanded or purged.", chunk.getPosition());
                } else {
                    teamName = team.getTeamName();
                }

                source.sendFeedback(() -> Text.literal("| Claimed chunk information"), false);
                source.sendFeedback(() -> Text.literal("| Team: " + teamName), false);
                source.sendFeedback(() -> Text.literal("| Claimed time: " + parseDate(chunk.getClaimedTime(), "/", Utilities.TimeFormat.DD_MM_YYYY, true)), false);
            }

            return 1;
        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Only player can execute this command"));
            return 0;
        }
    }

    private static int executeClaim(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            ChunkManager instance = ChunkManager.getInstance();

            ServerPlayerEntity player = source.getPlayerOrThrow();

            BlockPos instancePosition = player.getBlockPos();

            RegistryKey<World> dimension = player.getWorld().getRegistryKey();

            ChunkPosition chunkPos = new ChunkPosition(instancePosition, dimension);

            Team team = TeamManager.getInstance().getTeamByPlayer(player.getUuid());

            if (team == null) {
                source.sendError(Text.literal("You are currently not in a team to claim this chunk."));
                source.sendFeedback(() -> Text.literal("Consider joining a team or create one with /ctc team create <team_name>"), false);
                return 0;
            }

            boolean success = ChunkManager.getInstance().claimChunk(chunkPos, team.getTeamId(), player);

            if (!success) {
                if (instance.isChunkClaimed(chunkPos)) {
                    source.sendFeedback(() -> Text.literal("This chunk is already claimed!"), false);
                }
                else if (!team.getTeamMember().get(player.getUuid()).getPermission().hasPermission(PermType.CLAIM)) {
                    source.sendError(Text.literal("You don't have permission to claim this chunk."));
                }

                return 0;
            }

            source.sendFeedback(() -> Text.literal("Chunk claimed successfully!"), false);

            return 1;

        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Only player can execute this command"));
            return 0;
        }
    }
}

