package Sky.Cat.CTC;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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

    // Time display conversion from DBConnects Utility
    // Visit https://github.com/baole444/DBConnector/blob/main/src/main/java/dbConnect/Utility.java for more info

    /**
     * Enums for time formatting, support digit time display with separator of length 1.
     * <div>
     * Supported time string formats:
     * <li>{@link #DD_MM_YYYY}</li>
     * <li>{@link #MM_DD_YYYY}</li>
     * <li>{@link #YYYY_MM_DD}</li>
     * </div>
     */
    public enum TimeFormat {
        DD_MM_YYYY("dd'%s'MM'%s'yyyy"),
        MM_DD_YYYY("MM'%s'dd'%s'yyyy"),
        YYYY_MM_DD("yyyy'%s'MM'%s'dd");

        private final String pattern;

        TimeFormat(String pattern) {
            this.pattern = pattern;
        }

        public String getFormattedPattern(String separator) {
            return String.format(pattern, separator, separator);
        }
    }

    /**
     * Convert date String that matched {@link TimeFormat} to {@code Date} Object.
     * @param dateString A string of date consists of day, month and year.
     * @param timeFormat The date order of the string following {@link TimeFormat} supported enums.
     * @param separator the separator used in the date string.
     * @return {@link Date} Object from the given date string.
     */
    public static Date parseDate(String dateString, TimeFormat timeFormat, String separator) {
        String pattern = timeFormat.getFormattedPattern(separator);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.of("UTC"));

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);

        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * Convert {@code Date} object to String presentation.
     * @param date the date object to convert.
     * @param separator the string use for separation between date field.
     * @param timeFormat the date order of the string following {@link TimeFormat} supported enums.
     * @param includeTime true to add HH:MM in front of the date string.
     * @return A string presentation of the date object.
     * @throws IllegalArgumentException Invalid enum of {@link TimeFormat}.
     */
    public static String parseDate(Date date, String separator, TimeFormat timeFormat, boolean includeTime) throws IllegalArgumentException {
        Instant instant = date.toInstant();

        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));

        String pattern = timeFormat.getFormattedPattern(separator);

        if (includeTime) {
            pattern = "HH:mm " + pattern;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        String parsedDate = zonedDateTime.format(formatter);

        return parsedDate + " UTC";
    }

    /**
     * Convert millisecond since EPOCH to String presentation.
     * @param millis the date in milliseconds to convert.
     * @param separator the string use for separation between date field.
     * @param timeFormat the date order of the string following {@link TimeFormat} supported enums.
     * @param includeTime true to add HH:MM in front of the date string.
     * @return A string presentation of the date object.
     * @throws IllegalArgumentException Invalid enum of {@link TimeFormat}.
     */
    public static String parseDate(long millis, String separator, TimeFormat timeFormat, boolean includeTime) throws IllegalArgumentException {
        Instant instant = Instant.ofEpochMilli(millis);

        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));

        String pattern = timeFormat.getFormattedPattern(separator);

        if (includeTime) {
            pattern = "HH:mm " + pattern;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        String parsedDate = zonedDateTime.format(formatter);

        return parsedDate + " UTC";
    }
}
