package org.yitzi.video.core.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.*;

public class DatetimeConverter {

    // Date parsing code from Jackson databind ISO8601Utils.java
    // https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java
    private static final String GMT_ID = "GMT";

    /**
     * Parse a date from ISO-8601 formatted string. It expects a format
     * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh:mm]]
     *
     * @param date ISO string to parse in the appropriate format.
     * @return the parsed date
     * @throws ParseException if the date is not in the appropriate format
     */
    public static Date parse(String date) throws ParseException {
        return parse(date, new ParsePosition(0));
    }

    private static Date parse(String date, ParsePosition pos) throws ParseException {
        Exception fail;
        try {
            int offset = pos.getIndex();

            // extract year
            int year = parseInt(date, offset, offset += 4);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // extract month
            int month = parseInt(date, offset, offset += 2);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // extract day
            int day = parseInt(date, offset, offset += 2);
            // default time value
            int hour = 0;
            int minutes = 0;
            int seconds = 0;
            int milliseconds = 0; // always use 0 otherwise returned date will include millis of current time
            if (checkOffset(date, offset, 'T')) {

                // extract hours, minutes, seconds and milliseconds
                hour = parseInt(date, offset += 1, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }

                minutes = parseInt(date, offset, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }
                // second and milliseconds can be optional
                if (date.length() > offset) {
                    char c = date.charAt(offset);
                    if (c != 'Z' && c != '+' && c != '-') {
                        seconds = parseInt(date, offset, offset += 2);
                        // milliseconds can be optional in the format
                        if (checkOffset(date, offset, '.')) {
                            milliseconds = parseInt(date, offset += 1, offset += 3);
                        }
                    }
                }
            }

            // extract timezone
            String timezoneId;
            if (date.length() <= offset) {
                // Assume UTC when time zone is missing
                date = date + "Z";
            }
            char timezoneIndicator = date.charAt(offset);
            if (timezoneIndicator == '+' || timezoneIndicator == '-') {
                String timezoneOffset = date.substring(offset);
                timezoneId = GMT_ID + timezoneOffset;
                offset += timezoneOffset.length();
            }
            else if (timezoneIndicator == 'Z') {
                timezoneId = GMT_ID;
                offset += 1;
            }
            else {
                throw new IndexOutOfBoundsException("Invalid time zone indicator " + timezoneIndicator);
            }

            TimeZone timezone = TimeZone.getTimeZone(timezoneId);
            if (!timezone.getID().equals(timezoneId)) {
                throw new IndexOutOfBoundsException();
            }

            Calendar calendar = new GregorianCalendar(timezone);
            calendar.setLenient(false);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, seconds);
            calendar.set(Calendar.MILLISECOND, milliseconds);

            pos.setIndex(offset);
            return calendar.getTime();
            // If we get a ParseException it'll already have the right message/offset.
            // Other exception types can convert here.
        }
        catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            fail = e;
        }
        String input = (date == null) ? null : ("'" + date + "'");
        throw new ParseException("Failed to parse date [" + input + "]: " + fail.getMessage(), pos.getIndex());
    }

    /**
     * Parse an integer located between 2 given offsets in a string
     *
     * @param value      the string to parse
     * @param beginIndex the start index for the integer in the string
     * @param endIndex   the end index for the integer in the string
     * @return the int
     * @throws NumberFormatException if the value is not a number
     */
    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        // use same logic as in Integer.parseInt() but less generic we're not supporting negative values
        int i = beginIndex;
        int result = 0;
        int digit;
        if (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value);
            }
            result = -digit;
        }
        while (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value);
            }
            result *= 10;
            result -= digit;
        }
        return -result;
    }

    /**
     * Check if the expected character exist at the given offset in the value.
     *
     * @param value    the string to check at the specified offset
     * @param offset   the offset to look for the expected character
     * @param expected the expected character
     * @return true if the expected character exist at the given offset
     */
    private static boolean checkOffset(String value, int offset, char expected) {
        return (offset < value.length()) && (value.charAt(offset) == expected);
    }

    /**
     * Format date into yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     *
     * @param date   the date to format
     * @param millis true to include millis precision otherwise false
     * @param tz     timezone to use for the formatting (GMT will produce 'Z')
     * @return the date formatted as yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     */
    public static String format(Date date, boolean millis, TimeZone tz) {
        Calendar calendar = new GregorianCalendar(tz, Locale.US);
        calendar.setTime(date);

        // estimate capacity of buffer as close as we can (yeah, that's pedantic ;)
        int capacity = "yyyy-MM-ddThh:mm:ss".length();
        capacity += millis ? ".sss".length() : 0;
        capacity += tz.getRawOffset() == 0 ? "Z".length() : "+hh:mm".length();
        StringBuilder formatted = new StringBuilder(capacity);

        padInt(formatted, calendar.get(Calendar.YEAR), "yyyy".length());
        formatted.append('-');
        padInt(formatted, calendar.get(Calendar.MONTH) + 1, "MM".length());
        formatted.append('-');
        padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), "dd".length());
        formatted.append('T');
        padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), "hh".length());
        formatted.append(':');
        padInt(formatted, calendar.get(Calendar.MINUTE), "mm".length());
        formatted.append(':');
        padInt(formatted, calendar.get(Calendar.SECOND), "ss".length());
        if (millis) {
            formatted.append('.');
            padInt(formatted, calendar.get(Calendar.MILLISECOND), "sss".length());
        }

        int offset = tz.getOffset(calendar.getTimeInMillis());
        if (offset != 0) {
            int hours = Math.abs((offset / (60 * 1000)) / 60);
            int minutes = Math.abs((offset / (60 * 1000)) % 60);
            formatted.append(offset < 0 ? '-' : '+');
            padInt(formatted, hours, "hh".length());
            formatted.append(':');
            padInt(formatted, minutes, "mm".length());
        }
        else {
            formatted.append('Z');
        }

        return formatted.toString();
    }

    /**
     * Zero pad a number to a specified length
     *
     * @param buffer buffer to use for padding
     * @param value  the integer value to pad if necessary.
     * @param length the length of the string we should zero pad
     */
    private static void padInt(StringBuilder buffer, int value, int length) {
        String strValue = Integer.toString(value);
        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);
    }
}
