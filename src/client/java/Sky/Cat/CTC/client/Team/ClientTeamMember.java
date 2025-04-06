package Sky.Cat.CTC.client.Team;

import Sky.Cat.CTC.permission.Permission;

import java.util.UUID;

public class ClientTeamMember {
    private final UUID uuid;
    private final String name;
    private final Permission permission;

    public ClientTeamMember(UUID uuid, String name, Permission permission) {
        this.uuid = uuid;
        this.name = name;
        this.permission = permission;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Permission getPermission() {
        return permission;
    }
}
