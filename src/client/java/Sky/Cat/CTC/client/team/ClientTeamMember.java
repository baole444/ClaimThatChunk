package Sky.Cat.CTC.client.team;

import Sky.Cat.CTC.permission.Permission;

import java.util.UUID;

public record ClientTeamMember(UUID uuid, String name, Permission permission) {
}
