package Sky.Cat.CTC;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;

/**
 * Common class to store often used methods
 * that doesn't belong to specific feature of the mod.
 */
public class Utilities {
    /**
     * Convert a permission object to an integer representation.
     */
    public static int permissionToInt(Permission permission) {
        int flags = 0;

        // Could replace this with bit shift operation in a for loop on a perm type.
        // However, it will be sensitive to the enum order.

        //int bit = 1;
        //for (PermType type : PermType.values()) {
        //    if (permission.hasPermission(type)) {
        //        flag |= bit;
        //    }
        //    bit <<= 1; // shifting bit to the left to move to the next value
        //}

        if (permission.hasPermission(PermType.INVITE)) flags |= 1;
        if (permission.hasPermission(PermType.KICK)) flags |= 2;
        if (permission.hasPermission(PermType.CLAIM)) flags |= 4;
        if (permission.hasPermission(PermType.BUILD)) flags |= 8;
        if (permission.hasPermission(PermType.BREAK)) flags |= 16;
        if (permission.hasPermission(PermType.INTERACT)) flags |= 32;
        if (permission.hasPermission(PermType.MODIFY_PERMISSION)) flags |= 64;
        if (permission.hasPermission(PermType.DISBAND)) flags |= 128;

        return flags;
    }

    /**
     * Convert the integer permission flags back to Permission Object.
     */
    public static Permission intToPermission(int flags) {
        Permission permission = new Permission();

        permission.setPermission(PermType.INVITE, (flags & 1) != 0);
        permission.setPermission(PermType.KICK, (flags & 2) != 0);
        permission.setPermission(PermType.CLAIM, (flags & 4) != 0);
        permission.setPermission(PermType.BUILD, (flags & 8) != 0);
        permission.setPermission(PermType.BREAK, (flags & 16) != 0);
        permission.setPermission(PermType.INTERACT, (flags & 32) != 0);
        permission.setPermission(PermType.MODIFY_PERMISSION, (flags & 64) != 0);
        permission.setPermission(PermType.DISBAND, (flags & 128) != 0);

        return permission;
    }

}
