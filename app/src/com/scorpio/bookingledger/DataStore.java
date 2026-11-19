package com.scorpio.bookingledger;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DataStore {

    public static class Booking {
        public long id;
        public String name = "", phone = "", type = "Wedding";
        public String start = "", end = "";
        public String pickup = "", drop = "", notes = "";
        public String address = "";
        public String driverName = "", driverPhone = "", driverPhoto = "";
        public long total = 0, advance = 0;

        public long balance() { return Math.max(0, total - advance); }

        public String status() {
            if (total > 0 && total - advance <= 0) return "PAID";
            if (advance > 0) return "PARTIAL";
            return "PENDING";
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id); o.put("name", name); o.put("phone", phone); o.put("type", type);
                o.put("start", start); o.put("end", end);
                o.put("pickup", pickup); o.put("drop", drop); o.put("notes", notes);
                o.put("address", address);
                o.put("driverName", driverName); o.put("driverPhone", driverPhone); o.put("driverPhoto", driverPhoto);
                o.put("total", total); o.put("advance", advance);
            } catch (Exception ignored) {}
            return o;
        }

        static Booking fromJson(JSONObject o) {
            Booking b = new Booking();
            b.id = o.optLong("id");
            b.name = o.optString("name"); b.phone = o.optString("phone"); b.type = o.optString("type", "Wedding");
            b.start = o.optString("start"); b.end = o.optString("end");
            if (b.end.length() == 0) b.end = b.start;
            b.pickup = o.optString("pickup"); b.drop = o.optString("drop"); b.notes = o.optString("notes");
            b.address = o.optString("address");
            b.driverName = o.optString("driverName"); b.driverPhone = o.optString("driverPhone"); b.driverPhoto = o.optString("driverPhoto");
            b.total = o.optLong("total"); b.advance = o.optLong("advance");
            return b;
        }
    }

    private static DataStore inst;
    private final File file;
    public final ArrayList<Booking> bookings = new ArrayList<>();

    public static synchronized DataStore get(Context c) {
        if (inst == null) {
            inst = new DataStore(c.getApplicationContext());
            inst.load();
        }
        return inst;
    }

    private DataStore(Context c) { file = new File(c.getFilesDir(), "bookings.json"); }

    public synchronized void load() {
        bookings.clear();
        if (!file.exists()) return;
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buf = new byte[(int) file.length()];
            int r = in.read(buf); in.close();
            if (r <= 0) return;
            fromJson(new String(buf, "UTF-8"));
        } catch (Exception ignored) {}
    }

    public synchronized void save() {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(toJson().getBytes("UTF-8"));
            out.close();
        } catch (Exception ignored) {}
    }

    public synchronized long addOrUpdate(Booking b) {
        if (b.id == 0) {
            long m = 0;
            for (Booking x : bookings) if (x.id > m) m = x.id;
            b.id = m + 1;
            bookings.add(b);
        } else {
            for (int i = 0; i < bookings.size(); i++) {
                if (bookings.get(i).id == b.id) { bookings.set(i, b); break; }
            }
        }
        save();
        return b.id;
    }

    public synchronized void delete(long id) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).id == id) { bookings.remove(i); break; }
        }
        save();
    }

    public synchronized Booking find(long id) {
        for (Booking b : bookings) if (b.id == id) return b;
        return null;
    }

    public synchronized String toJson() {
        JSONArray arr = new JSONArray();
        for (Booking b : bookings) arr.put(b.toJson());
        JSONObject root = new JSONObject();
        try { root.put("version", 1); root.put("app", "scorpio-booking-ledger"); root.put("bookings", arr); }
        catch (Exception ignored) {}
        return root.toString();
    }

    public synchronized void fromJson(String s) {
        bookings.clear();
        try {
            JSONObject root = new JSONObject(s);
            JSONArray arr = root.optJSONArray("bookings");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) bookings.add(Booking.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
    }

    public synchronized List<Booking> overlapping(String s, String e, long excludeId) {
        ArrayList<Booking> out = new ArrayList<>();
        for (Booking b : bookings)
            if (b.id != excludeId && Dates.overlaps(s, e, b.start, b.end)) out.add(b);
        return out;
    }

    public synchronized List<Booking> onDay(String day) {
        ArrayList<Booking> out = new ArrayList<>();
        for (Booking b : bookings) if (Dates.covers(b.start, b.end, day)) out.add(b);
        return out;
    }

    public synchronized int countOnDay(String day) {
        int n = 0;
        for (Booking b : bookings) if (Dates.covers(b.start, b.end, day)) n++;
        return n;
    }

    /** upcoming bookings: end date today or later, sorted by start */
    public synchronized List<Booking> upcoming(int limit) {
        String t = Dates.today();
        ArrayList<Booking> out = new ArrayList<>();
        for (Booking b : bookings) if (b.end.compareTo(t) >= 0) out.add(b);
        sortByStart(out);
        if (out.size() > limit) return out.subList(0, limit);
        return out;
    }

    public synchronized List<Booking> sortedAll() {
        ArrayList<Booking> out = new ArrayList<>(bookings);
        sortByStart(out);
        return out;
    }

    private static void sortByStart(List<Booking> l) {
        java.util.Collections.sort(l, new java.util.Comparator<Booking>() {
            public int compare(Booking a, Booking b) { return a.start.compareTo(b.start); }
        });
    }
}
