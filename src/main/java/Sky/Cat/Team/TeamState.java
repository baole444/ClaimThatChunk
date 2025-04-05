package Sky.Cat.Team;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamState extends PersistentState {
    // Permission codec
    public static final Codec<Permission> PERMISSION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("invite").forGetter(p -> p.hasPermission(PermType.INVITE)),
            Codec.BOOL.fieldOf("kick").forGetter(p -> p.hasPermission(PermType.KICK)),
            Codec.BOOL.fieldOf("claim").forGetter(p -> p.hasPermission(PermType.CLAIM)),
            Codec.BOOL.fieldOf("build").forGetter(p -> p.hasPermission(PermType.BUILD)),
            Codec.BOOL.fieldOf("break").forGetter(p -> p.hasPermission(PermType.BREAK)),
            Codec.BOOL.fieldOf("interact").forGetter(p -> p.hasPermission(PermType.INTERACT)),
            Codec.BOOL.fieldOf("modifyPermission").forGetter(p -> p.hasPermission(PermType.MODIFY_PERMISSION)),
            Codec.BOOL.fieldOf("disband").forGetter(p -> p.hasPermission(PermType.DISBAND))
    ).apply(instance, (invite, kick, claim, build, brk, interact, modifyPerm, disband) -> {
        Permission permission = new Permission();

        // Apply values to new permission instance.
        permission.setPermission(PermType.INVITE, invite);
        permission.setPermission(PermType.KICK, kick);
        permission.setPermission(PermType.CLAIM, claim);
        permission.setPermission(PermType.BUILD, build);
        permission.setPermission(PermType.BREAK, brk);
        permission.setPermission(PermType.INTERACT, interact);
        permission.setPermission(PermType.MODIFY_PERMISSION, modifyPerm);
        permission.setPermission(PermType.DISBAND, disband);

        return permission;
    }));

    // Team member codec.
    public static final Codec<TeamMember> TEAM_MEMBER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(TeamMember::getPlayerUUID),
            Codec.STRING.fieldOf("name").forGetter(TeamMember::getPlayerName),
            PERMISSION_CODEC.fieldOf("permission").forGetter(TeamMember::getPermission),
            Codec.LONG.fieldOf("joinTime").forGetter(TeamMember::getTeamJoinTime)
    ).apply(instance, TeamMember::fromPersistence));

    // Team codec.
    public static final Codec<Team> TEAM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("teamId").forGetter(Team::getTeamId),
            Codec.STRING.fieldOf("teamName").forGetter(Team::getTeamName),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("leaderUUID").forGetter(Team::getLeaderUUID),
            Codec.STRING.fieldOf("leaderName").forGetter(Team::getLeaderName),
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    TEAM_MEMBER_CODEC
            ).fieldOf("teamMembers").forGetter(Team::getTeamMember),
            PERMISSION_CODEC.fieldOf("defaultPermission").forGetter(Team::getDefaultPermission),
            Codec.INT.fieldOf("teamSizeLimit").forGetter(Team::getTeamSizeLimit),
            Codec.LONG.fieldOf("createTime").forGetter(Team::getCreateTime)
    ).apply(instance, Team::fromCodec));

    // TeamState codec.
    public static final Codec<TeamState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    TEAM_CODEC
            ).fieldOf("teams").forGetter(state -> state.teams)
    ).apply(instance, TeamState::new));

    // Create PersistentStateType
    private static final PersistentStateType<TeamState> TYPE = new PersistentStateType<>("claim_that_chunk_teams", TeamState::new, CODEC, null);

    private final Map<UUID, Team> teams;

    public TeamState() {
        this.teams = new ConcurrentHashMap<>();
    }

    public TeamState(Map<UUID, Team> teams) {
        this.teams = new ConcurrentHashMap<>(teams);
    }

    // Get teams map.
    public Map<UUID, Team> getTeams() {
        return teams;
    }

    // Add a team to the state.
    public void addTeam(Team team) {
        teams.put(team.getTeamId(), team);
        markDirty();
    }

    // Remove a team from the state.
    public boolean removeTeam(UUID teamId) {
        boolean removed = teams.remove(teamId) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public static TeamState getOrCreate(MinecraftServer server) {
        return Objects.requireNonNull(server.getWorld(World.OVERWORLD)).
                getPersistentStateManager().
                getOrCreate(TYPE);
    }

}
