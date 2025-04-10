package Sky.Cat.CTC.command;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TeamCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ctc")
                // Team commands
                .then(CommandManager.literal("team")
                        // Create team command
                        .then(CommandManager.literal("create")
                                .executes(context -> executeCreateTeam(context, ""))
                                .then(CommandManager.argument("team_name", StringArgumentType.greedyString())
                                        .executes(context -> executeCreateTeam(context, StringArgumentType.getString(context, "team_name")))
                                )
                        )
                        // Disband team command
                        .then(CommandManager.literal("disband").
                                executes(TeamCommands::executeDisbandTeam)
                        )
                )
        );
    }

    private static int executeCreateTeam(CommandContext<ServerCommandSource> context, String teamName) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            TeamManager instance = TeamManager.getInstance();

            if (instance.getTeamByPlayer(player.getUuid()) != null) {
                source.sendError(Text.literal("You are already in a team."));
                return 0;
            }

            Team team = instance.createTeam(player.getUuid(), player.getName().getString(), teamName);

            if (team != null) {
                source.sendFeedback(() -> Text.literal("Team created: " + team.getTeamName() + " by " + player.getName().getString()), true);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to create team."));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("Only players can create teams."));
            return 0;
        }
    }

    private static int executeDisbandTeam(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            TeamManager instance = TeamManager.getInstance();
            Team team = instance.getTeamByPlayer(player.getUuid());


            if (!player.getUuid().equals(team.getLeaderUUID()) &&
                    !team.getTeamMember().get(player.getUuid())
                            .getPermission().hasPermission(PermType.DISBAND)) {
                source.sendError(Text.literal("You don't have permission to disband the team."));
                return 0;
            }

            boolean success = instance.disbandTeam(team.getTeamId());

            if (success) {
                source.sendFeedback(() -> Text.literal("Team '" + team.getTeamName() + "' has been disbanded."), true);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to disband the team."));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("Only players can disband teams."));
            return 0;
        }
    }
}
