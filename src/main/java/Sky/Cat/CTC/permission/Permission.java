package Sky.Cat.CTC.permission;

import java.lang.reflect.Field;
import java.util.List;

public class Permission {
    // Allow adding new member to the team.
    private boolean allowInvite;

    // Allow kicking member out of the team.
    private boolean allowKick;

    // Allow claiming chunks in the world.
    private boolean allowClaim;

    // Allow building on claimed chunk.
    private boolean allowBuild;

    // Allow breaking block on claimed chunk.
    private boolean allowBreak;

    // Allow interacting with mechanisms on claimed chunk.
    private boolean allowInteract;

    // Allow modifying fields in permission.
    private boolean allowModifyPermission;

    // Allow disbandment of the team.
    private boolean allowDisband;

    /**
     * Initiate permission with all nodes to false;
     */
    public Permission() {
        allowInvite = false;
        allowKick = false;
        allowClaim = false;
        allowBuild = false;
        allowBreak = false;
        allowInteract = false;
        allowModifyPermission = false;
        allowDisband = false;
    }

    /**
     * Check for permission.
     * @param type Enum of {@link PermType} represent a field of permission.
     * @return boolean value of the respective field.
     */
    public boolean hasPermission(PermType type) {
        switch (type) {
            case INVITE -> {return allowInvite;}
            case KICK -> {return allowKick;}
            case CLAIM -> {return allowClaim;}
            case BUILD -> {return allowBuild;}
            case BREAK -> {return allowBreak;}
            case INTERACT -> {return allowInteract;}
            case MODIFY_PERMISSION -> {return allowModifyPermission;}
            case DISBAND -> {return allowDisband;}
            default -> {
                Throwable cause = new Throwable("Unhandled permission type: '" + type + "'.");
                throw new IllegalArgumentException("Performed check on unknown permission type.", cause);
            }
        }
    }

    /**
     * Update permission value.
     * @param type Enum of {@link PermType} represent a field of permission.
     * @param value value to switch the respective field of permission to.
     */
    public void setPermission(PermType type, boolean value) {
        switch (type) {
            case INVITE -> this.allowInvite = value;
            case KICK -> this.allowKick = value;
            case CLAIM -> this.allowClaim = value;
            case BUILD -> this.allowBuild = value;
            case BREAK -> this.allowBreak = value;
            case INTERACT -> this.allowInteract = value;
            case MODIFY_PERMISSION -> this.allowModifyPermission = value;
            case DISBAND -> this.allowDisband = value;
            default -> {
                Throwable cause = new Throwable("Unhandled permission type: '" + type + "'.");
                throw new IllegalArgumentException("Performed update on unknown permission type.", cause);
            }
        }
    }

    public void grantAllPermission() throws IllegalAccessException {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType() == Boolean.class) {
                field.setBoolean(this, true);
            }
            field.setAccessible(false);
        }
    }
}
