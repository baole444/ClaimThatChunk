package Sky.Cat.CTC.command;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.chunk.ClaimedChunk;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import Sky.Cat.CTC.team.TeamState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminCommands {
    private static final Map<ServerCommandSource, PurgeRequest> pendingPurges = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static class PurgeRequest {
        final int pin;
        final long expirationTime;

        PurgeRequest(int pin, long expirationTime) {
            this.pin = pin;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ctc")
                .then(CommandManager.literal("purge")
                        .requires(source -> hasAdminPermission(source))
                        .executes(AdminCommands::executePurge)
                )
                .then(CommandManager.literal("confirm")
                        .then(CommandManager.argument("pin", IntegerArgumentType.integer(0, 999999))
                                .executes(context -> executeConfirm(context, IntegerArgumentType.getInteger(context, "pin")))
                        )
                )
                .then(CommandManager.literal("cancel").
                        executes(AdminCommands::executeCancel)
                )
        );
    }

    private static boolean hasAdminPermission(ServerCommandSource source) {
        // TODO: If luckperm integration is added in the future,
        //  check all these permission check for alternative luckperm permission node.
        return source.hasPermissionLevel(2);
    }

    private static int executePurge(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (pendingPurges.containsKey(source)) {
            source.sendError(Text.literal("You already have a pending purge operation. " +
                    "Use /ctc confirm <pin> or /ctc cancel"));
            return 0;
        }

        Random random = new Random();
        int pin = 100000 + random.nextInt(900000);

        long expirationTime = System.currentTimeMillis() + 15000;
        PurgeRequest request = new PurgeRequest(pin, expirationTime);
        pendingPurges.put(source, request);

        Main.LOGGER.info("Queued {} purge(s) request", pendingPurges.size());

        scheduler.schedule(() -> {
            if (pendingPurges.containsKey(source)) {
                PurgeRequest currentRequest = pendingPurges.get(source);

                if (currentRequest.isExpired()) {
                    pendingPurges.remove(source);
                    source.sendError(Text.literal("Purge confirmation timed out. Operation cancelled."));
                }
            }
        }, 16, TimeUnit.SECONDS);

        source.sendFeedback(() -> Text.literal("/!\\ WARNING! This will purge ALL team and chunk data! /!\\"), false);
        source.sendFeedback(() -> Text.literal("To confirm, enter this 6-digit PIN within 15 seconds: " + pin), false);
        source.sendFeedback(() -> Text.literal("Execute: /ctc confirm " + pin), false);
        source.sendFeedback(() -> Text.literal("To cancel, type: /ctc cancel"), false);

        return 1;
    }

    private static int executeConfirm(CommandContext<ServerCommandSource> context, int enteredPin) {
        ServerCommandSource source = context.getSource();

        if (!pendingPurges.containsKey(source)) {
            source.sendError(Text.literal("You don't have any pending purge operation."));
            return 0;
        }

        PurgeRequest request = pendingPurges.get(source);

        if (request.isExpired()) {
            pendingPurges.remove(source);
            source.sendError(Text.literal("Purge confirmation timed out. Operation cancelled."));
            return 0;
        }

        if (request.pin != enteredPin) {
            source.sendError(Text.literal("Incorrect PIN. Purge operation cancelled."));
            pendingPurges.remove(source);
            return 0;
        }

        pendingPurges.remove(source);
        return purgeAllData(source);
    }

    private static int executeCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!pendingPurges.containsKey(source)) {
            source.sendError(Text.literal("You don't have a pending purge operation"));
            return 0;
        }

        pendingPurges.remove(source);
        source.sendFeedback(() -> Text.literal("Purge operation cancelled."), false);

        return 1;
    }

    private static int purgeAllData(ServerCommandSource source) {
        TeamManager teamManager = TeamManager.getInstance();
        ChunkManager chunkManager = ChunkManager.getInstance();

        try {
            int teamCount = 0;
            int chunkCount = 0;

            Map<UUID, Team> allTeams = new HashMap<>(teamManager.getTeamState().getTeams());
            teamCount = allTeams.size();

            Map<ChunkPosition, ClaimedChunk> allChunks = chunkManager.getAllClaimedChunks();
            chunkCount = allChunks.size();

            for (ChunkPosition position : allChunks.keySet()) {
                chunkManager.forceUnclaimChunk(position);
            }

            for (UUID teamId : allTeams.keySet()) {
                teamManager.disbandTeam(teamId);
            }

            source.sendFeedback(() -> Text.literal("Data purge completed successfully!"), true);

            int finalTeamCount = teamCount;
            int finalChunkCount = chunkCount;
            source.sendFeedback(() -> Text.literal("Purged " + finalTeamCount + " teams and " + finalChunkCount + " claimed chunks."), true);

            Main.LOGGER.warn("Data purge executed by {}: {} teams and {} chunks purged.", getSourceName(source), finalTeamCount, finalChunkCount);

            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error during purge operation: " + e.getMessage()));
            Main.LOGGER.error("Error during purge operation", e);
            return 0;
        }
    }

    private static String getSourceName(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            return player.getName().getString();
        } catch (Exception e) {
            return "Console";
        }
    }


}
