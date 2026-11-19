package com.scorpio.bookingledger;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Ui {
    public static boolean dark = false;

    public static int bg, card, text, sub, navy, accent, line, fieldBg, navBg;
    public static int chipGreen, chipGreenBg, chipAmber, chipAmberBg, chipRed, chipRedBg;

    public static void load(Context c) {
        dark = c.getSharedPreferences("settings", 0).getBoolean("dark", false);
        apply();
    }

    public static void setDark(Context c, boolean d) {
        dark = d;
        c.getSharedPreferences("settings", 0).edit().putBoolean("dark", d).apply();
        apply();
    }

    static void apply() {
        if (dark) {
            bg = 0xFF0A0F1E; card = 0xFF131C33; text = 0xFFF2F5FA; sub = 0xFF93A1C0;
            navy = 0xFF3B66C4; accent = 0xFFD4AF37; line = 0xFF243154; fieldBg = 0xFF0F1730; navBg = 0xFF10182E;
            chipGreen = 0xFF4ADE80; chipGreenBg = 0x224ADE80;
            chipAmber = 0xFFFBBF24; chipAmberBg = 0x22FBBF24;
            chipRed = 0xFFF87171; chipRedBg = 0x22F87171;
        } else {
            bg = 0xFFF4F6FB; card = 0xFFFFFFFF; text = 0xFF0B1B3F; sub = 0xFF5A6B8C;
            navy = 0xFF123C8C; accent = 0xFFB8860B; line = 0xFFE3E8F2; fieldBg = 0xFFEFF3FA; navBg = 0xFFFFFFFF;
            chipGreen = 0xFF15803D; chipGreenBg = 0x1A15803D;
            chipAmber = 0xFFB45309; chipAmberBg = 0x1AB45309;
            chipRed = 0xFFDC2626; chipRedBg = 0x1ADC2626;
        }
    }

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static Typeface medium() { return Typeface.create("sans-serif-medium", Typeface.NORMAL); }

    public static GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    public static TextView tv(Context c, String s, int sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        return t;
    }

    public static LinearLayout vcol(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hrow(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    public static LinearLayout.LayoutParams lpw(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    public static void margins(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) p).setMargins(l, t, r, b);
        }
    }

    public static LinearLayout cardView(Context c) {
        LinearLayout l = vcol(c);
        GradientDrawable g = round(card, dp(c, 18));
        g.setStroke(dp(c, 1), line);
        l.setBackground(g);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static TextView primaryBtn(Context c, String label) {
        TextView t = tv(c, label, 15, Color.WHITE, medium());
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(navy, dp(c, 14)));
        int pv = dp(c, 14), ph = dp(c, 18);
        t.setPadding(ph, pv, ph, pv);
        return t;
    }

    public static TextView ghostBtn(Context c, String label, int color) {
        TextView t = tv(c, label, 14, color, medium());
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = round(Color.TRANSPARENT, dp(c, 14));
        g.setStroke(dp(c, 1), color);
        t.setBackground(g);
        int pv = dp(c, 12), ph = dp(c, 14);
        t.setPadding(ph, pv, ph, pv);
        return t;
    }

    public static TextView chip(Context c, String status) {
        int fg, bgr;
        if ("PAID".equals(status) || "CLEARED".equals(status)) { fg = chipGreen; bgr = chipGreenBg; }
        else if ("PARTIAL".equals(status)) { fg = chipAmber; bgr = chipAmberBg; }
        else { fg = chipRed; bgr = chipRedBg; }
        TextView t = tv(c, status, 11, fg, Typeface.DEFAULT_BOLD);
        t.setBackground(round(bgr, dp(c, 20)));
        int ph = dp(c, 10), pv = dp(c, 4);
        t.setPadding(ph, pv, ph, pv);
        return t;
    }

    public static EditText field(Context c, String hint, int inputType) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(sub);
        e.setTextColor(text);
        e.setTextSize(15);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setBackground(round(fieldBg, dp(c, 12)));
        int ph = dp(c, 14), pv = dp(c, 12);
        e.setPadding(ph, pv, ph, pv);
        return e;
    }

    public static TextView label(Context c, String s) {
        TextView t = tv(c, s, 12, sub, medium());
        t.setAllCaps(true);
        return t;
    }

    public static TextView dateBtn(Context c, String text) {
        TextView t = tv(c, text, 15, Ui.text, medium());
        t.setBackground(round(fieldBg, dp(c, 12)));
        t.setGravity(Gravity.CENTER_VERTICAL);
        int ph = dp(c, 14), pv = dp(c, 12);
        t.setPadding(ph, pv, ph, pv);
        return t;
    }

    public static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_LONG).show(); }

    public static void confirm(Context c, String title, String msg, String okLabel, final Runnable onOk) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(okLabel, (d, w) -> onOk.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static LinearLayout screen(Context c) {
        LinearLayout root = vcol(c);
        root.setBackgroundColor(bg);
        return root;
    }

    public static ScrollView scrollWrap(Context c, View child) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.addView(child);
        return sv;
    }
}
