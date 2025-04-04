package Sky.Cat.Team;

import Sky.Cat.CTC.permission.Permission;

import java.util.UUID;

public class TeamMember {
    private UUID playerUUID;

    private String playerName;

    private Permission permission;

    private final long teamJoinTime;

    public TeamMember(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.permission = new Permission();
        this.teamJoinTime = System.currentTimeMillis();
    }

    public TeamMember(UUID playerUUID, String playerName, Permission permission) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.permission = permission;
        this.teamJoinTime = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public long getTeamJoinTime() {
        return this.teamJoinTime;
    }

    public boolean hasJoinedTeamAtLeast(long durationMillis) {
        return System.currentTimeMillis() - teamJoinTime >= durationMillis;
    }
}
