package com.scorpio.bookingledger;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        LinearLayout col = Ui.screen(a);
        int p = Ui.dp(a, 20);
        col.setPadding(p, p, p, p);

        col.addView(Ui.tv(a, editing == null ? "New Booking" : "Edit Booking", 22, Ui.text, Typeface.DEFAULT_BOLD));
        TextView s0 = new TextView(a); col.addView(s0, Ui.lp(-1, Ui.dp(a, 12)));

        final EditText name = Ui.field(a, "e.g. Rahul Sharma", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        final EditText phone = Ui.field(a, "10-digit mobile", InputType.TYPE_CLASS_PHONE);
        final EditText address = Ui.field(a, "Customer address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        final EditText pickup = Ui.field(a, "Pickup location", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText drop = Ui.field(a, "Drop location", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText total = Ui.field(a, "Total booking amount (\u20B9)", InputType.TYPE_CLASS_NUMBER);
        final EditText advance = Ui.field(a, "Advance received (\u20B9)", InputType.TYPE_CLASS_NUMBER);
        final EditText notes = Ui.field(a, "Notes (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText driverName = Ui.field(a, "Driver name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        final EditText driverPhone = Ui.field(a, "Driver mobile", InputType.TYPE_CLASS_PHONE);

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

        // date pickers
        final String[] startD = {editing == null ? Dates.today() : editing.start};
        final String[] endD = {editing == null ? Dates.today() : editing.end};
        final TextView startBtn = Ui.dateBtn(a, "From:  " + Dates.fmt(startD[0]));
        final TextView endBtn = Ui.dateBtn(a, "To:      " + Dates.fmt(endD[0]));
        startBtn.setOnClickListener(v -> pickDate(a, startD[0], (iso) -> { startD[0] = iso; if (endD[0].compareTo(iso) < 0) { endD[0] = iso; endBtn.setText("To:      " + Dates.fmt(iso)); } startBtn.setText("From:  " + Dates.fmt(iso)); }));
        endBtn.setOnClickListener(v -> pickDate(a, endD[0], (iso) -> { endD[0] = iso; endBtn.setText("To:      " + Dates.fmt(iso)); }));

        // driver photo
        photoLabel = Ui.ghostBtn(a, pickedPhoto == null ? "Add driver photo (optional)" : "Driver photo added \u2713", Ui.navy);
        photoLabel.setOnClickListener(v -> a.pickPhoto());

        // balance preview
        final TextView balPrev = Ui.tv(a, "Balance: \u20B90", 15, Ui.chipAmber, Typeface.DEFAULT_BOLD);

        android.text.TextWatcher tw = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int x, int y, int z) {}
            public void onTextChanged(CharSequence s, int x, int y, int z) {}
            public void afterTextChanged(android.text.Editable s) {
                long t = num(total), ad = num(advance);
                long bal = Math.max(0, t - ad);
                balPrev.setText("Balance: " + Dates.inr(bal));
                balPrev.setTextColor(bal <= 0 && t > 0 ? Ui.chipGreen : Ui.chipAmber);
            }
        };
        total.addTextChangedListener(tw);
        advance.addTextChangedListener(tw);

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

        // layout
        addLabeled(col, a, "Customer name", name);
        addLabeled(col, a, "Customer mobile", phone);
        addLabeled(col, a, "Customer address", address);
        col.addView(Ui.label(a, "Booking type"));
        col.addView(typeRow); Ui.margins(typeRow, 0, Ui.dp(a, 6), 0, Ui.dp(a, 10));
        LinearLayout dates = Ui.hrow(a);
        dates.addView(startBtn, Ui.lpw(0, -2, 1));
        TextView dgap = new TextView(a); dates.addView(dgap, Ui.lp(Ui.dp(a, 10), -2));
        dates.addView(endBtn, Ui.lpw(0, -2, 1));
        col.addView(Ui.label(a, "Booking dates"));
        col.addView(dates); Ui.margins(dates, 0, Ui.dp(a, 6), 0, Ui.dp(a, 10));
        addLabeled(col, a, "Pickup", pickup);
        addLabeled(col, a, "Drop", drop);
        addLabeled(col, a, "Total amount", total);
        addLabeled(col, a, "Advance paid", advance);
        col.addView(balPrev); Ui.margins(balPrev, Ui.dp(a, 4), 0, 0, Ui.dp(a, 10));
        addLabeled(col, a, "Driver name", driverName);
        addLabeled(col, a, "Driver mobile", driverPhone);
        col.addView(photoLabel); Ui.margins(photoLabel, 0, 0, 0, Ui.dp(a, 10));
        addLabeled(col, a, "Notes", notes);
        col.addView(save); Ui.margins(save, 0, Ui.dp(a, 6), 0, Ui.dp(a, 24));
        return Ui.scrollWrap(a, col);
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

        LinearLayout head = Ui.hrow(a);
        head.addView(Ui.tv(a, b.name, 22, Ui.text, Typeface.DEFAULT_BOLD), Ui.lpw(0, -2, 1));
        head.addView(Ui.chip(a, b.status()), Ui.lp(-2, -2));
        Screens.spaced(col, a, head);

        LinearLayout card = Ui.cardView(a);
        row(card, a, "Dates", Dates.range(b.start, b.end));
        row(card, a, "Type", b.type);
        if (Screens.route(b).length() > 0) row(card, a, "Route", Screens.route(b));
        if (b.phone.length() > 0) row(card, a, "Mobile", b.phone);
        if (b.address.length() > 0) row(card, a, "Address", b.address);
        if (b.driverName.length() > 0) row(card, a, "Driver", b.driverName + (b.driverPhone.length() > 0 ? " (" + b.driverPhone + ")" : ""));
        if (b.notes.length() > 0) row(card, a, "Notes", b.notes);
        Screens.spaced(col, a, card);

        LinearLayout money = Ui.cardView(a);
        mrow(money, a, "Total amount", Dates.inr(b.total), Ui.text);
        mrow(money, a, "Advance paid", Dates.inr(b.advance), Ui.chipGreen);
        mrow(money, a, "Balance pending", Dates.inr(b.balance()), b.balance() > 0 ? Ui.chipRed : Ui.chipGreen);
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
