package com.scorpio.bookingledger;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class FormDetail {

    static String pickedPhoto = null; // set by MainActivity photo picker
    static TextView photoLabel = null;

    // ---------- ADD / EDIT FORM ----------
    static View form(final MainActivity a, final long editId) {
        final DataStore.Booking editing = editId == 0 ? null : DataStore.get(a).find(editId);
        pickedPhoto = editing == null ? null : (editing.driverPhoto.length() == 0 ? null : editing.driverPhoto);

        LinearLayout root = Ui.screen(a);
        LinearLayout col = Ui.vcol(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);

        col.addView(Ui.tv(a, editing == null ? "New Booking" : "Edit Booking", 22, Ui.text, Typeface.DEFAULT_BOLD));
        TextView s0 = new TextView(a); col.addView(s0, Ui.lp(-1, Ui.dp(a, 14)));

        final EditText name = Ui.field(a, "e.g. Rahul Sharma", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        final EditText phone = Ui.field(a, "10-digit mobile", InputType.TYPE_CLASS_PHONE);
        final EditText address = Ui.field(a, "Customer address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        final EditText pickup = Ui.field(a, "Pickup location", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText drop = Ui.field(a, "Drop location", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText notes = Ui.field(a, "Notes (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText driverName = Ui.field(a, "Driver name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        final EditText driverPhone = Ui.field(a, "Driver mobile", InputType.TYPE_CLASS_PHONE);

        // big amount inputs
        final LinearLayout totalBox = moneyBox(a);
        final EditText total = (EditText) totalBox.getChildAt(1);
        final LinearLayout advBox = moneyBox(a);
        final EditText advance = (EditText) advBox.getChildAt(1);

        if (editing != null) {
            name.setText(editing.name); phone.setText(editing.phone); address.setText(editing.address);
            pickup.setText(editing.pickup); drop.setText(editing.drop);
            if (editing.total > 0) total.setText(String.valueOf(editing.total));
            if (editing.advance > 0) advance.setText(String.valueOf(editing.advance));
            notes.setText(editing.notes);
            driverName.setText(editing.driverName); driverPhone.setText(editing.driverPhone);
        }

        // event type chips
        final String[] types = {"Wedding", "Tour", "Travel", "Other"};
        final String[] selType = {editing == null ? "Wedding" : editing.type};
        final LinearLayout typeRow = Ui.hrow(a);
        final TextView[] typeBtns = new TextView[types.length];
        final Runnable paintTypes = () -> {
            for (int i = 0; i < types.length; i++) {
                boolean sel = types[i].equals(selType[0]);
                typeBtns[i].setTextColor(sel ? 0xFFFFFFFF : Ui.text);
                typeBtns[i].setBackground(Ui.round(sel ? Ui.navy : Ui.fieldBg, Ui.dp(a, 20)));
            }
        };
        for (int i = 0; i < types.length; i++) {
            final int idx = i;
            TextView t = Ui.tv(a, types[i], 13, Ui.text, Ui.medium());
            t.setGravity(Gravity.CENTER);
            int ph = Ui.dp(a, 14), pv = Ui.dp(a, 8);
            t.setPadding(ph, pv, ph, pv);
            t.setOnClickListener(v -> { selType[0] = types[idx]; paintTypes.run(); });
            typeBtns[i] = t;
            typeRow.addView(t, Ui.lpw(0, -2, 1));
            Ui.margins(t, Ui.dp(a, 3), 0, Ui.dp(a, 3), 0);
        }
        paintTypes.run();

        // date tiles
        final String[] startD = {editing == null ? Dates.today() : editing.start};
        final String[] endD = {editing == null ? Dates.today() : editing.end};
        final LinearLayout startTile = dateTile(a, "FROM");
        final LinearLayout endTile = dateTile(a, "TO");
        final TextView startTxt = (TextView) startTile.getChildAt(1);
        final TextView endTxt = (TextView) endTile.getChildAt(1);
        startTxt.setText(Dates.fmt(startD[0]));
        endTxt.setText(Dates.fmt(endD[0]));
        startTile.setOnClickListener(v -> pickDate(a, startD[0], (iso) -> { startD[0] = iso; if (endD[0].compareTo(iso) < 0) { endD[0] = iso; endTxt.setText(Dates.fmt(iso)); } startTxt.setText(Dates.fmt(iso)); }));
        endTile.setOnClickListener(v -> pickDate(a, endD[0], (iso) -> { endD[0] = iso; endTxt.setText(Dates.fmt(iso)); }));

        // driver photo
        photoLabel = Ui.ghostBtn(a, pickedPhoto == null ? "Add driver photo (optional)" : "Driver photo added \u2713", Ui.navy);
        photoLabel.setOnClickListener(v -> a.pickPhoto());

        // live balance + paid progress
        final TextView balPrev = Ui.tv(a, "Balance: \u20B90", 16, Ui.chipAmber, Typeface.DEFAULT_BOLD);
        final TextView pctTxt = Ui.tv(a, "0% paid", 11, Ui.sub, Typeface.DEFAULT_BOLD);
        final LinearLayout bar = Ui.progress(a);
        final android.text.TextWatcher tw = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int x, int y, int z) {}
            public void onTextChanged(CharSequence s, int x, int y, int z) {}
            public void afterTextChanged(android.text.Editable s) {
                long t = num(total), ad = num(advance);
                long bal = Math.max(0, t - ad);
                balPrev.setText("Balance: " + Dates.inr(bal));
                balPrev.setTextColor(bal <= 0 && t > 0 ? Ui.chipGreen : Ui.chipAmber);
                int pct = t > 0 ? (int) Math.min(100, ad * 100 / t) : 0;
                Ui.setProgress(a, bar, pct);
                pctTxt.setText(pct + "% paid");
            }
        };
        total.addTextChangedListener(tw);
        advance.addTextChangedListener(tw);
        tw.afterTextChanged(null);

        // save
        TextView save = Ui.primaryBtn(a, editing == null ? "Save Booking" : "Update Booking");
        save.setOnClickListener(v -> {
            String nm = name.getText().toString().trim();
            if (nm.length() == 0) { Ui.toast(a, "Customer name is required"); return; }
            final DataStore.Booking b = editing == null ? new DataStore.Booking() : editing;
            b.name = nm;
            b.phone = phone.getText().toString().trim();
            b.address = address.getText().toString().trim();
            b.type = selType[0];
            b.start = startD[0]; b.end = endD[0];
            if (b.end.compareTo(b.start) < 0) b.end = b.start;
            b.pickup = pickup.getText().toString().trim();
            b.drop = drop.getText().toString().trim();
            b.total = num(total); b.advance = num(advance);
            b.notes = notes.getText().toString().trim();
            b.driverName = driverName.getText().toString().trim();
            b.driverPhone = driverPhone.getText().toString().trim();
            if (pickedPhoto != null) b.driverPhoto = pickedPhoto;
            List<DataStore.Booking> clashes = DataStore.get(a).overlapping(b.start, b.end, b.id);
            if (!clashes.isEmpty()) {
                DataStore.Booking c0 = clashes.get(0);
                Ui.confirm(a, "Double booking!",
                        "Scorpio is already booked for " + c0.name + " (" + Dates.range(c0.start, c0.end) + ") on these dates.\n\nSave anyway?",
                        "Save anyway",
                        () -> { DataStore.get(a).addOrUpdate(b); Ui.toast(a, "Booking saved"); a.goBack(); });
            } else {
                DataStore.get(a).addOrUpdate(b);
                Ui.toast(a, "Booking saved");
                a.goBack();
            }
        });

        // ---- layout: sectioned premium cards ----
        LinearLayout cCust = Ui.section(a, "\uD83D\uDC64", "CUSTOMER");
        addLabeled(cCust, a, "Full name", name);
        addLabeled(cCust, a, "Mobile", phone);
        addLabeled(cCust, a, "Address", address);
        spacedSection(col, a, cCust);

        LinearLayout cTrip = Ui.section(a, "\uD83D\uDE97", "TRIP");
        cTrip.addView(Ui.label(a, "Booking type"));
        cTrip.addView(typeRow); Ui.margins(typeRow, 0, Ui.dp(a, 6), 0, Ui.dp(a, 12));
        cTrip.addView(Ui.label(a, "Booking dates"));
        LinearLayout tiles = Ui.hrow(a);
        tiles.addView(startTile, Ui.lpw(0, -2, 1));
        TextView tgap = new TextView(a); tiles.addView(tgap, Ui.lp(Ui.dp(a, 10), -2));
        tiles.addView(endTile, Ui.lpw(0, -2, 1));
        cTrip.addView(tiles); Ui.margins(tiles, 0, Ui.dp(a, 6), 0, Ui.dp(a, 12));
        addLabeled(cTrip, a, "Pickup", pickup);
        addLabeled(cTrip, a, "Drop", drop);
        spacedSection(col, a, cTrip);

        LinearLayout cPay = Ui.section(a, "\uD83D\uDCB3", "PAYMENT");
        cPay.addView(Ui.label(a, "Total amount"));
        cPay.addView(totalBox); Ui.margins(totalBox, 0, Ui.dp(a, 6), 0, Ui.dp(a, 10));
        cPay.addView(Ui.label(a, "Advance paid"));
        cPay.addView(advBox); Ui.margins(advBox, 0, Ui.dp(a, 6), 0, Ui.dp(a, 4));
        LinearLayout balRow = Ui.hrow(a);
        TextView balLbl = Ui.tv(a, "Balance", 14, Ui.sub, Ui.medium());
        balRow.addView(balLbl, Ui.lpw(0, -2, 1));
        balRow.addView(balPrev, Ui.lp(-2, -2));
        cPay.addView(balRow); Ui.margins(balRow, 0, Ui.dp(a, 6), 0, 0);
        LinearLayout progRow = Ui.hrow(a);
        progRow.addView(bar, Ui.lpw(0, -2, 1));
        progRow.addView(pctTxt, Ui.lp(-2, -2));
        Ui.margins(pctTxt, Ui.dp(a, 8), 0, 0, 0);
        cPay.addView(progRow); Ui.margins(progRow, 0, Ui.dp(a, 10), 0, 0);
        spacedSection(col, a, cPay);

        LinearLayout cDrv = Ui.section(a, "\uD83E\uDDD1", "DRIVER");
        addLabeled(cDrv, a, "Driver name", driverName);
        addLabeled(cDrv, a, "Driver mobile", driverPhone);
        cDrv.addView(photoLabel);
        spacedSection(col, a, cDrv);

        LinearLayout cNotes = Ui.section(a, "\uD83D\uDCDD", "NOTES");
        cNotes.addView(notes);
        spacedSection(col, a, cNotes);

        TextView bottomSpace = new TextView(a);
        col.addView(bottomSpace, Ui.lp(-1, Ui.dp(a, 4)));

        ScrollView sv = new ScrollView(a);
        sv.addView(col);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = Ui.vcol(a);
        bottom.setBackgroundColor(Ui.bg);
        int bp = Ui.dp(a, 20);
        bottom.setPadding(bp, Ui.dp(a, 10), bp, Ui.dp(a, 14));
        bottom.addView(save);
        root.addView(bottom);
        return root;
    }

    static LinearLayout moneyBox(MainActivity a) {
        LinearLayout mf = Ui.hrow(a);
        mf.setBackground(Ui.round(Ui.fieldBg, Ui.dp(a, 14)));
        int p = Ui.dp(a, 14);
        mf.setPadding(p, Ui.dp(a, 12), p, Ui.dp(a, 12));
        TextView rs = Ui.tv(a, "\u20B9", 18, Ui.sub, Typeface.DEFAULT_BOLD);
        mf.addView(rs);
        EditText e = new EditText(a);
        e.setHint("0");
        e.setHintTextColor(Ui.sub);
        e.setTextColor(Ui.text);
        e.setTextSize(22);
        e.setTypeface(Typeface.DEFAULT_BOLD);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        e.setBackground(null);
        mf.addView(e, Ui.lpw(0, -2, 1));
        Ui.margins(e, Ui.dp(a, 8), 0, 0, 0);
        return mf;
    }

    static LinearLayout dateTile(MainActivity a, String lbl) {
        LinearLayout tile = Ui.vcol(a);
        tile.setBackground(Ui.round(Ui.fieldBg, Ui.dp(a, 14)));
        int p = Ui.dp(a, 12);
        tile.setPadding(p, p, p, p);
        TextView l = Ui.tv(a, lbl, 10, Ui.sub, Typeface.DEFAULT_BOLD);
        l.setLetterSpacing(0.08f);
        TextView d = Ui.tv(a, "", 15, Ui.text, Typeface.DEFAULT_BOLD);
        TextView h = Ui.tv(a, "Tap to change", 10, Ui.navy, Ui.medium());
        tile.addView(l); tile.addView(d); tile.addView(h);
        Ui.margins(d, 0, Ui.dp(a, 3), 0, 0);
        Ui.margins(h, 0, Ui.dp(a, 2), 0, 0);
        return tile;
    }

    static void spacedSection(LinearLayout col, android.content.Context c, View v) {
        col.addView(v);
        Ui.margins(v, 0, 0, 0, Ui.dp(c, 12));
    }

    static void onPhotoPicked(String path) {
        pickedPhoto = path;
        if (photoLabel != null && path != null) photoLabel.setText("Driver photo added \u2713");
    }

    interface DateCb { void onPick(String iso); }

    static void pickDate(MainActivity a, String current, final DateCb cb) {
        Calendar c = Calendar.getInstance();
        try { c.setTime(Dates.ISO.parse(current)); } catch (Exception ignored) {}
        new DatePickerDialog(a, (view, y, m, d) -> {
            Calendar x = Calendar.getInstance();
            x.set(y, m, d);
            cb.onPick(Dates.ISO.format(x.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    static long num(EditText e) {
        try { return Long.parseLong(e.getText().toString().replaceAll("[^0-9]", "")); }
        catch (Exception ex) { return 0; }
    }

    static void addLabeled(LinearLayout col, android.content.Context c, String label, View field) {
        col.addView(Ui.label(c, label));
        col.addView(field);
        Ui.margins(field, 0, Ui.dp(c, 6), 0, Ui.dp(c, 10));
    }

    // ---------- DETAIL ----------
    static View detail(final MainActivity a, long id) {
        final DataStore.Booking b = DataStore.get(a).find(id);
        LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);
        if (b == null) { col.addView(Ui.tv(a, "Booking not found", 16, Ui.sub, Ui.medium())); return col; }

        // hero card: name, id, status + calendar tile, route, dates, type
        LinearLayout hero = Ui.cardView(a);
        LinearLayout head = Ui.hrow(a);
        LinearLayout hleft = Ui.vcol(a);
        hleft.addView(Ui.tv(a, b.name, 22, Ui.text, Typeface.DEFAULT_BOLD));
        hleft.addView(Ui.tv(a, CardRenderer.bookingId(b.id), 12, Ui.sub, Typeface.DEFAULT_BOLD));
        head.addView(hleft, Ui.lpw(0, -2, 1));
        head.addView(Ui.chip(a, b.status()), Ui.lp(-2, -2));
        hero.addView(head);
        Ui.margins(head, 0, 0, 0, Ui.dp(a, 12));

        LinearLayout trip = Ui.hrow(a);
        trip.setGravity(Gravity.CENTER_VERTICAL);
        trip.addView(calTile(a, b.start));
        LinearLayout tcol = Ui.vcol(a);
        String route = Screens.route(b);
        if (route.length() > 0) tcol.addView(Ui.tv(a, route, 17, Ui.text, Typeface.DEFAULT_BOLD));
        tcol.addView(Ui.tv(a, Dates.range(b.start, b.end), 14, Ui.text, Typeface.DEFAULT_BOLD));
        tcol.addView(Ui.tv(a, b.type, 12, Ui.sub, Ui.medium()));
        trip.addView(tcol, Ui.lpw(0, -2, 1));
        Ui.margins(tcol, Ui.dp(a, 12), 0, 0, 0);
        hero.addView(trip);
        Screens.spaced(col, a, hero);

        LinearLayout info = Ui.section(a, "\uD83D\uDC64", "DETAILS");
        if (b.phone.length() > 0) row(info, a, "Mobile", b.phone);
        if (b.address.length() > 0) row(info, a, "Address", b.address);
        if (b.driverName.length() > 0) row(info, a, "Driver", b.driverName + (b.driverPhone.length() > 0 ? " (" + b.driverPhone + ")" : ""));
        if (b.notes.length() > 0) row(info, a, "Notes", b.notes);
        Screens.spaced(col, a, info);

        LinearLayout money = Ui.section(a, "\uD83D\uDCB3", "PAYMENT");
        mrow(money, a, "Total amount", Dates.inr(b.total), Ui.text);
        mrow(money, a, "Advance paid", Dates.inr(b.advance), Ui.chipGreen);
        mrow(money, a, "Balance pending", Dates.inr(b.balance()), b.balance() > 0 ? Ui.chipRed : Ui.chipGreen);
        LinearLayout progRow = Ui.hrow(a);
        LinearLayout bar = Ui.progress(a);
        int pct = b.total > 0 ? (int) Math.min(100, Math.min(b.advance, b.total) * 100 / b.total) : 0;
        Ui.setProgress(a, bar, pct);
        TextView pctTxt = Ui.tv(a, pct + "% paid", 11, Ui.sub, Typeface.DEFAULT_BOLD);
        progRow.addView(bar, Ui.lpw(0, -2, 1));
        progRow.addView(pctTxt, Ui.lp(-2, -2));
        Ui.margins(pctTxt, Ui.dp(a, 8), 0, 0, 0);
        money.addView(progRow); Ui.margins(progRow, 0, Ui.dp(a, 10), 0, 0);
        Screens.spaced(col, a, money);

        // contact actions
        LinearLayout c1 = Ui.hrow(a);
        TextView wa = Ui.primaryBtn(a, "WhatsApp");
        wa.setOnClickListener(v -> a.openWhatsApp(b.phone));
        TextView call = Ui.ghostBtn(a, "Call", Ui.navy);
        call.setOnClickListener(v -> a.callNumber(b.phone));
        c1.addView(wa, Ui.lpw(0, -2, 1));
        TextView g1 = new TextView(a); c1.addView(g1, Ui.lp(Ui.dp(a, 10), -2));
        c1.addView(call, Ui.lpw(0, -2, 1));
        Screens.spaced(col, a, c1);

        // share card
        LinearLayout c2 = Ui.hrow(a);
        TextView cardPng = Ui.primaryBtn(a, "Share booking card");
        cardPng.setOnClickListener(v -> a.shareCard(b, false));
        TextView cardPdf = Ui.ghostBtn(a, "Share as PDF", Ui.navy);
        cardPdf.setOnClickListener(v -> a.shareCard(b, true));
        c2.addView(cardPng, Ui.lpw(0, -2, 1));
        TextView g2 = new TextView(a); c2.addView(g2, Ui.lp(Ui.dp(a, 10), -2));
        c2.addView(cardPdf, Ui.lpw(0, -2, 1));
        Screens.spaced(col, a, c2);

        LinearLayout c3 = Ui.hrow(a);
        TextView edit = Ui.ghostBtn(a, "Edit", Ui.text);
        edit.setOnClickListener(v -> a.openForm(b.id));
        TextView del = Ui.ghostBtn(a, "Delete", Ui.chipRed);
        del.setOnClickListener(v -> Ui.confirm(a, "Delete booking?",
                "Delete booking for " + b.name + " (" + Dates.range(b.start, b.end) + ")? This cannot be undone.",
                "Delete",
                () -> { DataStore.get(a).delete(b.id); Ui.toast(a, "Deleted"); a.goBack(); }));
        c3.addView(edit, Ui.lpw(0, -2, 1));
        TextView g3 = new TextView(a); c3.addView(g3, Ui.lp(Ui.dp(a, 10), -2));
        c3.addView(del, Ui.lpw(0, -2, 1));
        Screens.spaced(col, a, c3);

        return Ui.scrollWrap(a, col);
    }

    static LinearLayout calTile(MainActivity a, String iso) {
        String day = "-", mon = "";
        try {
            java.util.Date dt = Dates.ISO.parse(iso.substring(0, 10));
            day = new java.text.SimpleDateFormat("d", java.util.Locale.US).format(dt);
            mon = new java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(dt).toUpperCase(java.util.Locale.US);
        } catch (Exception ignored) {}
        LinearLayout tile = Ui.vcol(a);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(Ui.round(Ui.fieldBg, Ui.dp(a, 12)));
        tile.setMinimumWidth(Ui.dp(a, 56));
        int pv = Ui.dp(a, 8);
        tile.setPadding(0, pv, 0, pv);
        tile.addView(Ui.tv(a, day, 22, Ui.text, Typeface.DEFAULT_BOLD));
        tile.addView(Ui.tv(a, mon, 11, Ui.navy, Typeface.DEFAULT_BOLD));
        for (int i = 0; i < tile.getChildCount(); i++)
            ((TextView) tile.getChildAt(i)).setGravity(Gravity.CENTER);
        return tile;
    }

    static void row(LinearLayout card, android.content.Context c, String k, String v) {
        LinearLayout r = Ui.hrow(c);
        r.setGravity(Gravity.TOP);
        TextView key = Ui.tv(c, k, 13, Ui.sub, Ui.medium());
        TextView val = Ui.tv(c, v, 14, Ui.text, Ui.medium());
        r.addView(key, Ui.lp(Ui.dp(c, 110), -2));
        r.addView(val, Ui.lpw(0, -2, 1));
        card.addView(r);
        Ui.margins(r, 0, 0, 0, Ui.dp(c, 8));
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
