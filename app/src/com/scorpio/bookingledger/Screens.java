package com.scorpio.bookingledger;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class Screens {

    // ---------- shared booking row ----------
    static View bookingRow(final MainActivity a, final DataStore.Booking b) {
        LinearLayout card = Ui.cardView(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = Ui.vcol(a);
        left.addView(Ui.tv(a, b.name, 16, Ui.text, Typeface.DEFAULT_BOLD));
        TextView dates = Ui.tv(a, Dates.range(b.start, b.end), 12, Ui.sub, Ui.medium());
        left.addView(dates);
        String route = route(b);
        if (route.length() > 0) left.addView(Ui.tv(a, route, 12, Ui.sub, Ui.medium()));

        LinearLayout right = Ui.vcol(a);
        right.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView amt = Ui.tv(a, Dates.inr(b.total), 16, Ui.text, Typeface.DEFAULT_BOLD);
        right.addView(amt);
        right.addView(Ui.chip(a, b.status()));

        card.addView(left, Ui.lpw(0, -2, 1));
        card.addView(right, Ui.lp(-2, -2));
        card.setOnClickListener(v -> a.openDetail(b.id));
        return card;
    }

    static String route(DataStore.Booking b) {
        if (b.pickup.length() == 0 && b.drop.length() == 0) return "";
        return (b.pickup.length() == 0 ? "?" : b.pickup) + "  \u2192  " + (b.drop.length() == 0 ? "?" : b.drop);
    }

    static void spaced(LinearLayout col, android.content.Context c, View v) {
        col.addView(v);
        int m = Ui.dp(c, 10);
        Ui.margins(v, 0, 0, 0, m);
    }

    // ---------- HOME ----------
    static View home(final MainActivity a) {
        LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);

        col.addView(Ui.tv(a, "Scorpio Booking Ledger", 22, Ui.text, Typeface.DEFAULT_BOLD));
        col.addView(Ui.tv(a, "Wedding \u2022 Tour \u2022 Travel bookings", 13, Ui.sub, Ui.medium()));

        TextView space = new TextView(a); col.addView(space, Ui.lp(-1, Ui.dp(a, 14)));

        DataStore ds = DataStore.get(a);
        long totalValue = 0, totalAdvance = 0;
        for (DataStore.Booking b : ds.bookings) { totalValue += b.total; totalAdvance += Math.min(b.advance, b.total); }
        long pending = Math.max(0, totalValue - totalAdvance);

        LinearLayout g1 = Ui.hrow(a), g2 = Ui.hrow(a);
        g1.addView(statCard(a, "Booked value", Dates.inr(totalValue)), Ui.lpw(0, -2, 1));
        g1.addView(statCard(a, "Advance received", Dates.inr(totalAdvance)), Ui.lpw(0, -2, 1));
        g2.addView(statCard(a, "Balance pending", Dates.inr(pending)), Ui.lpw(0, -2, 1));
        g2.addView(statCard(a, "Total bookings", String.valueOf(ds.bookings.size())), Ui.lpw(0, -2, 1));
        spaced(col, a, g1);
        spaced(col, a, g2);

        TextView add = Ui.primaryBtn(a, "+  New Booking");
        add.setOnClickListener(v -> a.openForm(0));
        spaced(col, a, add);

        TextView up = Ui.tv(a, "UPCOMING", 12, Ui.sub, Typeface.DEFAULT_BOLD);
        spaced(col, a, up);

        List<DataStore.Booking> ups = ds.upcoming(5);
        if (ups.isEmpty()) {
            col.addView(Ui.tv(a, "No upcoming bookings. Add your first one.", 13, Ui.sub, Ui.medium()));
        } else {
            for (DataStore.Booking b : ups) spaced(col, a, bookingRow(a, b));
        }
        return Ui.scrollWrap(a, col);
    }

    static View statCard(android.content.Context c, String label, String value) {
        LinearLayout card = Ui.cardView(c);
        card.addView(Ui.tv(c, value, 20, Ui.text, Typeface.DEFAULT_BOLD));
        card.addView(Ui.tv(c, label, 12, Ui.sub, Ui.medium()));
        int m = Ui.dp(c, 5);
        Ui.margins(card, m, m, m, m);
        return card;
    }

    // ---------- BOOKINGS LIST ----------
    static View list(final MainActivity a) {
        final LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);

        col.addView(Ui.tv(a, "All Bookings", 22, Ui.text, Typeface.DEFAULT_BOLD));
        TextView space = new TextView(a); col.addView(space, Ui.lp(-1, Ui.dp(a, 10)));

        final EditText search = Ui.field(a, "Search name or phone", android.text.InputType.TYPE_CLASS_TEXT);
        spaced(col, a, search);

        final LinearLayout results = Ui.vcol(a);
        col.addView(results);

        final Runnable fill = () -> {
            results.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase();
            boolean any = false;
            for (DataStore.Booking b : DataStore.get(a).sortedAll()) {
                if (q.length() > 0 && !b.name.toLowerCase().contains(q) && !b.phone.contains(q)) continue;
                any = true;
                spaced(results, a, bookingRow(a, b));
            }
            if (!any) results.addView(Ui.tv(a, "No bookings found.", 13, Ui.sub, Ui.medium()));
        };
        fill.run();
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a1, int b1, int c1) {}
            public void onTextChanged(CharSequence s, int a1, int b1, int c1) { fill.run(); }
            public void afterTextChanged(Editable s) {}
        });
        return Ui.scrollWrap(a, col);
    }

    // ---------- CALENDAR ----------
    static int calYear = -1, calMonth = -1; // 0-based month
    static String calSelected = null;

    static View calendar(final MainActivity a) {
        if (calYear < 0) {
            Calendar c = Calendar.getInstance();
            calYear = c.get(Calendar.YEAR);
            calMonth = c.get(Calendar.MONTH);
        }
        if (calSelected == null) calSelected = Dates.today();

        final LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 16);
        col.setPadding(p, p, p, p);

        // header with month nav
        LinearLayout head = Ui.hrow(a);
        TextView prev = Ui.ghostBtn(a, "<", Ui.text);
        TextView next = Ui.ghostBtn(a, ">", Ui.text);
        Calendar mc = Calendar.getInstance();
        mc.set(calYear, calMonth, 1);
        String title = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US).format(mc.getTime());
        TextView ttl = Ui.tv(a, title, 18, Ui.text, Typeface.DEFAULT_BOLD);
        ttl.setGravity(Gravity.CENTER);
        head.addView(prev, Ui.lp(Ui.dp(a, 44), -2));
        head.addView(ttl, Ui.lpw(0, -2, 1));
        head.addView(next, Ui.lp(Ui.dp(a, 44), -2));
        spaced(col, a, head);

        // weekday header
        LinearLayout wk = Ui.hrow(a);
        String[] days = {"S", "M", "T", "W", "T", "F", "S"};
        for (String d : days) {
            TextView t = Ui.tv(a, d, 12, Ui.sub, Typeface.DEFAULT_BOLD);
            t.setGravity(Gravity.CENTER);
            wk.addView(t, Ui.lpw(0, -2, 1));
        }
        spaced(col, a, wk);

        // grid
        DataStore ds = DataStore.get(a);
        Calendar first = Calendar.getInstance();
        first.set(calYear, calMonth, 1);
        int lead = first.get(Calendar.DAY_OF_WEEK) - 1;
        int dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cell = Ui.dp(a, 44);
        int dayNum = 1;
        for (int r = 0; r < 6 && dayNum <= dim; r++) {
            LinearLayout row = Ui.hrow(a);
            for (int cIdx = 0; cIdx < 7; cIdx++) {
                LinearLayout cellV = Ui.vcol(a);
                cellV.setGravity(Gravity.CENTER);
                cellV.setMinimumHeight(cell);
                if (r == 0 && cIdx < lead) {
                    row.addView(cellV, Ui.lpw(0, -2, 1));
                    continue;
                }
                if (dayNum > dim) { row.addView(cellV, Ui.lpw(0, -2, 1)); continue; }
                Calendar dc = Calendar.getInstance();
                dc.set(calYear, calMonth, dayNum);
                final String iso = Dates.ISO.format(dc.getTime());
                int n = ds.countOnDay(iso);
                TextView num = Ui.tv(a, String.valueOf(dayNum), 14,
                        iso.equals(calSelected) ? 0xFFFFFFFF : Ui.text,
                        n > 0 ? Typeface.DEFAULT_BOLD : Ui.medium());
                num.setGravity(Gravity.CENTER);
                int sz = Ui.dp(a, 34);
                if (iso.equals(calSelected)) num.setBackground(Ui.round(Ui.navy, sz / 2f));
                else if (n > 0) {
                    android.graphics.drawable.GradientDrawable g = Ui.round(Ui.chipGreenBg, sz / 2f);
                    g.setStroke(Ui.dp(a, 1), n > 1 ? Ui.chipAmber : Ui.chipGreen);
                    num.setBackground(g);
                }
                cellV.addView(num, Ui.lp(sz, sz));
                if (n > 1) {
                    TextView warn = Ui.tv(a, n + " bookings", 9, Ui.chipAmber, Typeface.DEFAULT_BOLD);
                    cellV.addView(warn);
                }
                cellV.setOnClickListener(v -> { calSelected = iso; a.render(); });
                row.addView(cellV, Ui.lpw(0, -2, 1));
                dayNum++;
            }
            col.addView(row);
        }

        prev.setOnClickListener(v -> { calMonth--; if (calMonth < 0) { calMonth = 11; calYear--; } a.render(); });
        next.setOnClickListener(v -> { calMonth++; if (calMonth > 11) { calMonth = 0; calYear++; } a.render(); });

        // selected day bookings
        TextView sel = Ui.tv(a, "Bookings on " + Dates.fmt(calSelected), 14, Ui.text, Typeface.DEFAULT_BOLD);
        int m = Ui.dp(a, 12);
        col.addView(sel); Ui.margins(sel, 0, m, 0, m);
        List<DataStore.Booking> dayList = ds.onDay(calSelected);
        if (dayList.isEmpty()) col.addView(Ui.tv(a, "No booking on this date.", 13, Ui.sub, Ui.medium()));
        else for (DataStore.Booking b : dayList) spaced(col, a, bookingRow(a, b));

        return Ui.scrollWrap(a, col);
    }

    // ---------- SETTINGS ----------
    static View settings(final MainActivity a) {
        LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);

        col.addView(Ui.tv(a, "More", 22, Ui.text, Typeface.DEFAULT_BOLD));
        TextView s1 = new TextView(a); col.addView(s1, Ui.lp(-1, Ui.dp(a, 12)));

        // theme
        LinearLayout themeCard = Ui.cardView(a);
        LinearLayout trow = Ui.hrow(a);
        LinearLayout tleft = Ui.vcol(a);
        tleft.addView(Ui.tv(a, "Dark mode", 16, Ui.text, Typeface.DEFAULT_BOLD));
        tleft.addView(Ui.tv(a, "Easy on the eyes at night", 12, Ui.sub, Ui.medium()));
        Switch sw = new Switch(a);
        sw.setChecked(Ui.dark);
        sw.setOnCheckedChangeListener((btn, on) -> { Ui.setDark(a, on); a.render(); });
        trow.addView(tleft, Ui.lpw(0, -2, 1));
        trow.addView(sw, Ui.lp(-2, -2));
        themeCard.addView(trow);
        spaced(col, a, themeCard);

        // business details for the booking card
        LinearLayout bizCard = Ui.cardView(a);
        bizCard.addView(Ui.tv(a, "Your business (shown on booking card)", 16, Ui.text, Typeface.DEFAULT_BOLD));
        TextView b1 = new TextView(a); bizCard.addView(b1, Ui.lp(-1, Ui.dp(a, 8)));
        final EditText bizName = Ui.field(a, "Business name", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        bizName.setText(a.getBizName());
        bizCard.addView(Ui.label(a, "Business name"));
        bizCard.addView(bizName);
        TextView b2 = new TextView(a); bizCard.addView(b2, Ui.lp(-1, Ui.dp(a, 8)));
        final EditText bizPhone = Ui.field(a, "Business contact number", android.text.InputType.TYPE_CLASS_PHONE);
        bizPhone.setText(a.getBizPhone());
        bizCard.addView(Ui.label(a, "Contact number"));
        bizCard.addView(bizPhone);
        TextView saveBiz = Ui.primaryBtn(a, "Save business details");
        int m8 = Ui.dp(a, 12);
        bizCard.addView(saveBiz); Ui.margins(saveBiz, 0, m8, 0, 0);
        saveBiz.setOnClickListener(v -> {
            a.setBiz(bizName.getText().toString().trim(), bizPhone.getText().toString().trim());
            Ui.toast(a, "Saved");
        });
        spaced(col, a, bizCard);

        // backup
        LinearLayout backupCard = Ui.cardView(a);
        backupCard.addView(Ui.tv(a, "Backup & restore", 16, Ui.text, Typeface.DEFAULT_BOLD));
        backupCard.addView(Ui.tv(a, "All data stays on this phone. Export a backup file to keep it safe.", 12, Ui.sub, Ui.medium()));
        TextView exp = Ui.primaryBtn(a, "Export backup (JSON)");
        backupCard.addView(exp); Ui.margins(exp, 0, m8, 0, 0);
        exp.setOnClickListener(v -> a.exportJson());
        TextView imp = Ui.ghostBtn(a, "Restore from backup", Ui.navy);
        backupCard.addView(imp); Ui.margins(imp, 0, m8, 0, 0);
        imp.setOnClickListener(v -> a.importJson());
        spaced(col, a, backupCard);

        col.addView(Ui.tv(a, "Scorpio Booking Ledger v1.0 \u2022 offline \u2022 your data never leaves this phone", 11, Ui.sub, Ui.medium()));
        return Ui.scrollWrap(a, col);
    }
}
