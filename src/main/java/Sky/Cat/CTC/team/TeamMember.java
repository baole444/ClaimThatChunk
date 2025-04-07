package Sky.Cat.CTC.team;

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

    // Constructor intended for factory only
    TeamMember(UUID playerUUID, String playerName, Permission permission, long teamJoinTime) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.permission = new Permission();
        this.teamJoinTime = teamJoinTime;
    }

    // Use for Team member codec deserialization.
    public static TeamMember fromPersistence(UUID playerUUID, String playerName, Permission permission, long joinTime) {
        return new TeamMember(playerUUID, playerName, permission, joinTime);
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
