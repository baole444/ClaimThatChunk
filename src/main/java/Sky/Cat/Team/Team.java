package Sky.Cat.Team;

import Sky.Cat.CTC.permission.Permission;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Team {
    // UUID assigned to the team.
    private UUID teamId;

    // The name of the team
    // TODO: If not specified, use leader's name + team, for example "Wuan_Si's Team"
    private String teamName;

    // The leader of the team, often the team's original creator unless transferred.
    private UUID leaderUUID;

    // The Name of the leader. Can be useful for display methods.
    private String leaderName;

    // Members of the team.
    private Map<UUID, TeamMember> teamMember;

    // Default setup permission for team member.
    private Permission defaultPermission;

    // Limit the number of member a team can have
    // Store in config, not editable by player. l
    // Limit can be updated by server.
    // TODO: if 0, team have no size limit. Team limit can store in config. Add exception to prevent updating new limit from affecting old team.
    private int teamSizeLimit;

    // Time stamp of when the team was created, can be pass into to create a Date object for display purpose.
    private long createTIme;

    //
    public Team(UUID leaderID, String leaderName, String teamName) {
        this.teamId = UUID.randomUUID();
        if (teamName.isBlank()) {
            this.teamName = leaderName +  "'s Team";
        } else {
            this.teamName = teamName;
        }
        this.leaderUUID = leaderID;
        this.leaderName = leaderName;
        this.teamMember = new HashMap<>();
        this.createTIme = System.currentTimeMillis();

    }

    public Team(UUID teamId, String teamName, UUID leaderUUID, Map<UUID, TeamMember> teamMember, Permission defaultPermission, int teamSizeLimit, long createTIme) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.leaderUUID = leaderUUID;
        this.teamMember = teamMember;
        this.defaultPermission = defaultPermission;
        this.teamSizeLimit = teamSizeLimit;
        this.createTIme = createTIme;
    }

    public boolean addMember(UUID uuid, String name) {


        return false;
    }

    public boolean addMember(Player player) {
        UUID uuid = player.getGameProfile().getId();
        String name = player.getName().getString();

        return addMember(uuid, name);
    }
}
