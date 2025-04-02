package Sky.Cat.Team;

import Sky.Cat.CTC.permission.Permission;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class TeamMember {
    private UUID playerUUID;

    private String playerName;

    private Permission permission;

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
}
