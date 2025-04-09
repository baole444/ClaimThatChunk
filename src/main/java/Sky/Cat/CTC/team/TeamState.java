package Sky.Cat.CTC.team;

import Sky.Cat.CTC.Main;
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
    /**
     * TeamState's CODEC for serialization and deserialization.
     */
    public static final Codec<TeamState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Team.CODEC
            ).fieldOf("teams").forGetter(state -> state.teams)
    ).apply(instance, TeamState::new));

    // Create PersistentStateType
    private static final PersistentStateType<TeamState> TYPE = new PersistentStateType<>(Main.MOD_ID + "_teams", TeamState::new, CODEC, null);

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

    /**
     * Add a team to the state.
     */
    public void addTeam(Team team) {
        teams.put(team.getTeamId(), team);
        markDirty();
    }

    /**
     * Remove a team from the state.
     */
    public boolean removeTeam(UUID teamId) {
        boolean removed = teams.remove(teamId) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    // If this caused crash because the world is null, check the Event that registers the Team Manager in Main.
    public static TeamState getOrCreate(MinecraftServer server) {
        return Objects.requireNonNull(server.getWorld(World.OVERWORLD)).
                getPersistentStateManager().
                getOrCreate(TYPE);
    }

}
