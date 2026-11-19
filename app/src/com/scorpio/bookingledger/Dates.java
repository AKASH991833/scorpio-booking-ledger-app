package com.scorpio.bookingledger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Dates {
    public static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final SimpleDateFormat PRETTY = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    public static String today() { return ISO.format(new Date()); }

    public static String fmt(String iso) {
        if (iso == null || iso.length() < 10) return "-";
        try { return PRETTY.format(ISO.parse(iso.substring(0, 10))); }
        catch (Exception e) { return iso; }
    }

    public static String range(String s, String e) {
        if (e == null || e.length() == 0 || e.equals(s)) return fmt(s);
        return fmt(s) + " - " + fmt(e);
    }

    public static String add(String iso, int days) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(ISO.parse(iso));
            c.add(Calendar.DAY_OF_YEAR, days);
            return ISO.format(c.getTime());
        } catch (Exception e) { return iso; }
    }

    /** true when [s1,e1] overlaps [s2,e2]; ISO strings compare lexicographically */
    public static boolean overlaps(String s1, String e1, String s2, String e2) {
        return s1.compareTo(e2) <= 0 && e1.compareTo(s2) >= 0;
    }

    public static boolean covers(String s, String e, String day) {
        return s.compareTo(day) <= 0 && e.compareTo(day) >= 0;
    }

    /** Indian grouping: 125000 -> 1,25,000 */
    public static String inr(long v) {
        String s = Long.toString(v);
        if (s.length() <= 3) return "\u20B9" + s;
        String last3 = s.substring(s.length() - 3);
        String rest = s.substring(0, s.length() - 3);
        StringBuilder out = new StringBuilder();
        while (rest.length() > 2) {
            out.insert(0, "," + rest.substring(rest.length() - 2));
            rest = rest.substring(0, rest.length() - 2);
        }
        out.insert(0, rest);
        return "\u20B9" + out + "," + last3;
    }
}
