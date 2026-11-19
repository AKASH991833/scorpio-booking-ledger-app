public class TestDates {
    static int pass = 0, fail = 0;

    static void eq(Object got, Object want, String name) {
        boolean ok = want.equals(got);
        if (ok) pass++; else { fail++; System.out.println("FAIL " + name + ": got=" + got + " want=" + want); }
    }
    static void t(boolean cond, String name) {
        if (cond) pass++; else { fail++; System.out.println("FAIL " + name); }
    }

    public static void main(String[] a) {
        // inr Indian grouping
        eq(com.scorpio.bookingledger.Dates.inr(0), "\u20B90", "inr0");
        eq(com.scorpio.bookingledger.Dates.inr(500), "\u20B9500", "inr500");
        eq(com.scorpio.bookingledger.Dates.inr(1500), "\u20B91,500", "inr1500");
        eq(com.scorpio.bookingledger.Dates.inr(25000), "\u20B925,000", "inr25k");
        eq(com.scorpio.bookingledger.Dates.inr(125000), "\u20B91,25,000", "inr125k");
        eq(com.scorpio.bookingledger.Dates.inr(12345678), "\u20B91,23,45,678", "inrBig");

        // fmt
        eq(com.scorpio.bookingledger.Dates.fmt("2026-11-19"), "19 Nov 2026", "fmt");
        eq(com.scorpio.bookingledger.Dates.range("2026-11-19", "2026-11-19"), "19 Nov 2026", "rangeSame");
        eq(com.scorpio.bookingledger.Dates.range("2026-11-19", "2026-11-21"), "19 Nov 2026 - 21 Nov 2026", "rangeDiff");

        // overlaps
        t(com.scorpio.bookingledger.Dates.overlaps("2026-11-10", "2026-11-12", "2026-11-12", "2026-11-14"), "overlap edge-touch");
        t(com.scorpio.bookingledger.Dates.overlaps("2026-11-10", "2026-11-12", "2026-11-11", "2026-11-11"), "overlap inside");
        t(!com.scorpio.bookingledger.Dates.overlaps("2026-11-10", "2026-11-12", "2026-11-13", "2026-11-14"), "no overlap");
        t(!com.scorpio.bookingledger.Dates.overlaps("2026-11-13", "2026-11-14", "2026-11-10", "2026-11-12"), "no overlap rev");

        // covers
        t(com.scorpio.bookingledger.Dates.covers("2026-11-10", "2026-11-12", "2026-11-11"), "covers mid");
        t(com.scorpio.bookingledger.Dates.covers("2026-11-10", "2026-11-12", "2026-11-10"), "covers start");
        t(!com.scorpio.bookingledger.Dates.covers("2026-11-10", "2026-11-12", "2026-11-13"), "covers outside");

        // add across month boundary
        eq(com.scorpio.bookingledger.Dates.add("2026-01-31", 1), "2026-02-01", "add month");
        eq(com.scorpio.bookingledger.Dates.add("2026-12-31", 1), "2027-01-01", "add year");

        System.out.println("PASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
