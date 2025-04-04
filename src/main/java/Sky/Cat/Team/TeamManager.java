package Sky.Cat.Team;

import java.util.Map;
import java.util.UUID;

public class TeamManager{
    private static TeamManager ManagerInstance;
    private Map<UUID, Team> teams;

    public static TeamManager getInstance() {
        if (ManagerInstance == null) {
            ManagerInstance = new TeamManager();
        }
        return ManagerInstance;
    }

    public Team createTeam(UUID creatorId, String creatorName, String teamName) {

    }

    public boolean disbandTeam(UUID teamId) {

    }

    public Team getTeamById(UUID teamId) {

    }

    public Team getTeamByPlayer(UUID playerUUID) {

    }
}
