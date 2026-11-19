package com.scorpio.bookingledger;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Customers {

    // one row per customer, grouped by phone (falls back to name when no phone)
    public static class Agg {
        public String key, name, phone;
        public int count = 0;
        public long billed = 0, paid = 0, pending = 0;
        public boolean cleared() { return pending <= 0; }
    }

    public static String keyOf(DataStore.Booking b) {
        String d = b.phone.replaceAll("[^0-9]", "");
        return d.length() > 0 ? "p:" + d : "n:" + b.name.trim().toLowerCase();
    }

    public static LinkedHashMap<String, Agg> all(MainActivity a) {
        LinkedHashMap<String, Agg> map = new LinkedHashMap<>();
        for (DataStore.Booking b : DataStore.get(a).sortedAll()) {
            String k = keyOf(b);
            Agg g = map.get(k);
            if (g == null) { g = new Agg(); g.key = k; map.put(k, g); }
            if (b.name.trim().length() > 0) g.name = b.name.trim();
            if (b.phone.trim().length() > 0) g.phone = b.phone.trim();
            g.count++;
            g.billed += b.total;
            g.paid += Math.min(b.advance, b.total);
        }
        for (Agg g : map.values()) g.pending = Math.max(0, g.billed - g.paid);
        return map;
    }

    public static List<DataStore.Booking> bookingsOf(MainActivity a, String key) {
        List<DataStore.Booking> out = new ArrayList<>();
        for (DataStore.Booking b : DataStore.get(a).sortedAll())
            if (keyOf(b).equals(key)) out.add(b);
        return out;
    }

    static String custStatus(Agg g) { return g.cleared() ? "CLEARED" : "PENDING"; }

    // ---------- CUSTOMERS LIST ----------
    static View customers(final MainActivity a) {
        final LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);

        col.addView(Ui.tv(a, "Customers", 22, Ui.text, Typeface.DEFAULT_BOLD));
        TextView space = new TextView(a); col.addView(space, Ui.lp(-1, Ui.dp(a, 10)));

        final EditText search = Ui.field(a, "Search name or phone", android.text.InputType.TYPE_CLASS_TEXT);
        Screens.spaced(col, a, search);

        final LinearLayout results = Ui.vcol(a);
        col.addView(results);

        final Runnable fill = () -> {
            results.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase();
            boolean any = false;
            for (Agg g : all(a).values()) {
                if (q.length() > 0 && !g.name.toLowerCase().contains(q) && !g.phone.contains(q)) continue;
                any = true;
                Screens.spaced(results, a, customerRow(a, g));
            }
            if (!any) results.addView(Ui.tv(a, "No customers yet. Add a booking first.", 13, Ui.sub, Ui.medium()));
        };
        fill.run();
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a1, int b1, int c1) {}
            public void onTextChanged(CharSequence s, int a1, int b1, int c1) { fill.run(); }
            public void afterTextChanged(Editable s) {}
        });
        return Ui.scrollWrap(a, col);
    }

    static View customerRow(final MainActivity a, final Agg g) {
        LinearLayout card = Ui.cardView(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = Ui.vcol(a);
        left.addView(Ui.tv(a, g.name.length() > 0 ? g.name : "Unknown", 16, Ui.text, Typeface.DEFAULT_BOLD));
        if (g.phone.length() > 0) left.addView(Ui.tv(a, g.phone, 12, Ui.sub, Ui.medium()));
        left.addView(Ui.tv(a, g.count + (g.count == 1 ? " booking" : " bookings") + "  •  Billed " + Dates.inr(g.billed), 12, Ui.sub, Ui.medium()));

        LinearLayout right = Ui.vcol(a);
        right.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView pend = Ui.tv(a, g.cleared() ? "No dues" : "Due " + Dates.inr(g.pending), 14,
                g.cleared() ? Ui.chipGreen : Ui.chipRed, Typeface.DEFAULT_BOLD);
        right.addView(pend);
        right.addView(Ui.chip(a, custStatus(g)));

        card.addView(left, Ui.lpw(0, -2, 1));
        card.addView(right, Ui.lp(-2, -2));
        card.setOnClickListener(v -> a.openCustomer(g.key));
        return card;
    }

    // ---------- CUSTOMER DETAIL ----------
    static View detail(final MainActivity a, final String key) {
        final Agg g = all(a).get(key);
        LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);
        if (g == null) { col.addView(Ui.tv(a, "Customer not found", 16, Ui.sub, Ui.medium())); return col; }

        LinearLayout head = Ui.hrow(a);
        LinearLayout hleft = Ui.vcol(a);
        hleft.addView(Ui.tv(a, g.name.length() > 0 ? g.name : "Unknown", 22, Ui.text, Typeface.DEFAULT_BOLD));
        if (g.phone.length() > 0) hleft.addView(Ui.tv(a, g.phone, 13, Ui.sub, Ui.medium()));
        head.addView(hleft, Ui.lpw(0, -2, 1));
        head.addView(Ui.chip(a, custStatus(g)), Ui.lp(-2, -2));
        Screens.spaced(col, a, head);

        // money summary
        LinearLayout money = Ui.cardView(a);
        mrow(money, a, "Total billed", Dates.inr(g.billed), Ui.text);
        mrow(money, a, "Total paid", Dates.inr(g.paid), Ui.chipGreen);
        mrow(money, a, "Balance pending", Dates.inr(g.pending), g.pending > 0 ? Ui.chipRed : Ui.chipGreen);
        Screens.spaced(col, a, money);

        // contact actions
        LinearLayout c1 = Ui.hrow(a);
        TextView wa = Ui.primaryBtn(a, "WhatsApp");
        wa.setOnClickListener(v -> a.openWhatsApp(g.phone));
        TextView call = Ui.ghostBtn(a, "Call", Ui.navy);
        call.setOnClickListener(v -> a.callNumber(g.phone));
        c1.addView(wa, Ui.lpw(0, -2, 1));
        TextView g1 = new TextView(a); c1.addView(g1, Ui.lp(Ui.dp(a, 10), -2));
        c1.addView(call, Ui.lpw(0, -2, 1));
        Screens.spaced(col, a, c1);

        // payment reminder
        if (g.pending > 0 && g.phone.length() > 0) {
            TextView rem = Ui.primaryBtn(a, "Send payment reminder (" + Dates.inr(g.pending) + ")");
            rem.setOnClickListener(v -> a.sendPaymentReminder(g));
            Screens.spaced(col, a, rem);
        }

        TextView h2 = Ui.tv(a, "BOOKINGS", 12, Ui.sub, Typeface.DEFAULT_BOLD);
        Screens.spaced(col, a, h2);
        for (DataStore.Booking b : bookingsOf(a, key)) Screens.spaced(col, a, Screens.bookingRow(a, b));
        return Ui.scrollWrap(a, col);
    }

    static void mrow(LinearLayout card, android.content.Context c, String k, String v, int color) {
        LinearLayout r = Ui.hrow(c);
        TextView key = Ui.tv(c, k, 14, Ui.sub, Ui.medium());
        TextView val = Ui.tv(c, v, 16, color, Typeface.DEFAULT_BOLD);
        r.addView(key, Ui.lpw(0, -2, 1));
        r.addView(val, Ui.lp(-2, -2));
        card.addView(r);
        Ui.margins(r, 0, 0, 0, Ui.dp(c, 8));
    }
}
