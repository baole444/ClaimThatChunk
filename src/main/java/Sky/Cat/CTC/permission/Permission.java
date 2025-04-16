package Sky.Cat.CTC.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

    // Allow killing friendly entity on claimed chunk.
    private boolean allowKillFriendly;

    // Allow killing hostile entity on claimed chunk.
    private boolean allowKillHostile;

    // Allow killing teammate on claimed chunk.
    private boolean allowKillTeammate;

    // Allow modifying fields in permission.
    private boolean allowModifyPermission;

    // Allow disbandment of the team.
    private boolean allowDisband;

    /**
     * Permission's CODEC for serialization and deserialization.
     */
    public static final Codec<Permission> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("invite").forGetter(p -> p.hasPermission(PermType.INVITE)),
            Codec.BOOL.fieldOf("kick").forGetter(p -> p.hasPermission(PermType.KICK)),
            Codec.BOOL.fieldOf("claim").forGetter(p -> p.hasPermission(PermType.CLAIM)),
            Codec.BOOL.fieldOf("build").forGetter(p -> p.hasPermission(PermType.BUILD)),
            Codec.BOOL.fieldOf("break").forGetter(p -> p.hasPermission(PermType.BREAK)),
            Codec.BOOL.fieldOf("interact").forGetter(p -> p.hasPermission(PermType.INTERACT)),
            Codec.BOOL.fieldOf("killFriendly").forGetter(p -> p.hasPermission(PermType.KILL_FRIENDLY)),
            Codec.BOOL.fieldOf("killHostile").forGetter(p -> p.hasPermission(PermType.KILL_HOSTILE)),
            Codec.BOOL.fieldOf("killTeammate").forGetter(p -> p.hasPermission(PermType.KILL_TEAMMATE)),
            Codec.BOOL.fieldOf("modifyPermission").forGetter(p -> p.hasPermission(PermType.MODIFY_PERMISSION)),
            Codec.BOOL.fieldOf("disband").forGetter(p -> p.hasPermission(PermType.DISBAND))
    ).apply(instance, (invite, kick, claim, build, brk, interact, killFriendly, killHostile, killTeammate, modifyPerm, disband) -> {
        Permission permission = new Permission();

        // Apply values to new permission instance.
        permission.setPermission(PermType.INVITE, invite);
        permission.setPermission(PermType.KICK, kick);
        permission.setPermission(PermType.CLAIM, claim);
        permission.setPermission(PermType.BUILD, build);
        permission.setPermission(PermType.BREAK, brk);
        permission.setPermission(PermType.INTERACT, interact);
        permission.setPermission(PermType.KILL_FRIENDLY, killFriendly);
        permission.setPermission(PermType.KILL_HOSTILE, killHostile);
        permission.setPermission(PermType.KILL_TEAMMATE, killTeammate);
        permission.setPermission(PermType.MODIFY_PERMISSION, modifyPerm);
        permission.setPermission(PermType.DISBAND, disband);

        return permission;
    }));


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
        allowKillFriendly = false;
        allowKillHostile = false;
        allowKillTeammate = false;
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
            case KILL_FRIENDLY -> {return allowKillFriendly;}
            case KILL_HOSTILE -> {return allowKillHostile;}
            case KILL_TEAMMATE -> {return allowKillTeammate;}
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
            case KILL_FRIENDLY -> this.allowKillFriendly = value;
            case KILL_HOSTILE -> this.allowKillHostile = value;
            case KILL_TEAMMATE -> this.allowKillTeammate = value;
            case MODIFY_PERMISSION -> this.allowModifyPermission = value;
            case DISBAND -> this.allowDisband = value;
            default -> {
                Throwable cause = new Throwable("Unhandled permission type: '" + type + "'.");
                throw new IllegalArgumentException("Performed update on unknown permission type.", cause);
            }
        }
    }

    public void grantAllPermission() {
        //Field[] fields = this.getClass().getDeclaredFields();
        //for (Field field : fields) {
        //    field.setAccessible(true);
        //    if (field.getType() == Boolean.class) {
        //        field.setBoolean(this, true);
        //    }
        //    field.setAccessible(false);
        //}
        for (PermType type : PermType.values()) {
            this.setPermission(type, true);
        }
    }

    public void revokeAllPermission() {
        for (PermType type : PermType.values()) {
            this.setPermission(type, false);
        }
    }
}
