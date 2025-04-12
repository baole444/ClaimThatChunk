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
                                .executes(ChunkCommands::executeClaim)
                        )
                        // Unclaim command
                        .then(CommandManager.literal("unclaim")
                                .executes(ChunkCommands::executeUnclaim)
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
            source.sendError(Text.literal("Only players can execute this command."));
            return 0;
        }
    }

    private static int executeClaim(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            ChunkManager chunkManager = ChunkManager.getInstance();

            ServerPlayerEntity player = source.getPlayerOrThrow();

            Team team = TeamManager.getInstance().getTeamByPlayer(player.getUuid());

            if (team == null) {
                source.sendError(Text.literal("You are not in a team! Create or join a team first."));
                return 0;
            }

            TeamMember member = team.getTeamMember().get(player.getUuid());

            if (member == null || !member.getPermission().hasPermission(PermType.CLAIM)) {
                source.sendError(Text.literal("You don't have permission to claim this chunk."));
                return 0;
            }

            BlockPos instancePosition = player.getBlockPos();

            RegistryKey<World> dimension = player.getWorld().getRegistryKey();

            ChunkPosition chunkPos = new ChunkPosition(instancePosition, dimension);

            if (chunkManager.isChunkClaimed(chunkPos)) {
                ClaimedChunk existingClaim = chunkManager.getClaimedChunk(chunkPos);

                if (existingClaim.getOwnerTeamId().equals(team.getTeamId())) {
                    source.sendFeedback(() -> Text.literal("Your team already claimed this chunk!"), false);
                } else {
                    source.sendError(Text.literal("This chunk is already claimed by another team."));
                }

                return 0;
            }

            int countTeamClaimedChunks = chunkManager.getTeamChunkCount(team.getTeamId());

            int maxChunks = chunkManager.getMaxChunksPerTeam();

            if (maxChunks > 0 && countTeamClaimedChunks >= maxChunks) {
                source.sendError(Text.literal("Your team has reached chunk claim limit of "  + maxChunks + " chunks."));
                return 0;
            }

            boolean success = chunkManager.claimChunk(chunkPos, team.getTeamId(), player);

            if (success) {
                source.sendFeedback(() -> Text.literal("Chunk claimed successfully for team " + team.getTeamName() + "!"), true);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to claim chunk. Please try again"));
                return 0;
            }

        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Only players can execute this command"));
            return 0;
        }
    }

    public static int executeUnclaim(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            ChunkManager chunkManager = ChunkManager.getInstance();

            ServerPlayerEntity player = source.getPlayerOrThrow();

            BlockPos instancePosition = player.getBlockPos();

            RegistryKey<World> dimension = player.getWorld().getRegistryKey();

            ClaimedChunk chunk = chunkManager.getClaimedChunkAt(instancePosition, dimension);

            if (chunk == null) {
                source.sendError(Text.literal("This chunk is not claimed."));
                return 0;
            }

            Team team = TeamManager.getInstance().getTeamByPlayer(player.getUuid());

            if (team == null || !chunk.getOwnerTeamId().equals(team.getTeamId())) {
                source.sendError(Text.literal("You can only unclaim chunks owned by your team."));
                return 0;
            }

            UUID playerUUID = player.getUuid();

            if (!playerUUID.equals(team.getLeaderUUID())) {
                TeamMember member = team.getTeamMember().get(playerUUID);

                if (member == null || !member.getPermission().hasPermission(PermType.CLAIM)) {
                    source.sendError(Text.literal("You don't have permission to unclaim chunks."));
                    return 0;
                }
            }

            ChunkPosition chunkPos = new ChunkPosition(instancePosition, dimension);

            boolean success = chunkManager.unclaimChunk(chunkPos, player);

            if (success) {
                source.sendFeedback(() -> Text.literal("Chunk unclaimed successfully!"), true);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to unclaim chunk. Please try again."));
                return 0;
            }

        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Only players can execute this command"));
            return 0;
        }
    }
}

