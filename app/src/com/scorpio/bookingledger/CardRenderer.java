package com.scorpio.bookingledger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.nayuki.qrcodegen.QrCode;

/** "Midnight Glass" landscape booking card, 1600x900. Navy/white, single blue accent. */
public class CardRenderer {
    static final int W = 1600, H = 900;
    static final int NAVY = 0xFF0B1B3F, ACCENT = 0xFF2E5BFF, ACCENT2 = 0xFF6D4CFF,
            STEEL = 0xFF93A1C0, LBLUE = 0xFF7FA6FF, GRAYT = 0xFFDCE6FF,
            GREEN = 0xFF4ADE80, RED = 0xFFF87171, AMBER = 0xFFFBBF24;

    public static File png(Context c, DataStore.Booking b, Settings s) throws Exception {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        draw(new Canvas(bmp), b, s);
        File dir = new File(c.getCacheDir(), "cards");
        dir.mkdirs();
        File f = new File(dir, "booking-" + b.id + ".png");
        FileOutputStream out = new FileOutputStream(f);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        return f;
    }

    public static File pdf(Context c, DataStore.Booking b, Settings s) throws Exception {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        draw(new Canvas(bmp), b, s);
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, 1).create());
        page.getCanvas().drawBitmap(bmp, 0, 0, null);
        doc.finishPage(page);
        File dir = new File(c.getCacheDir(), "cards");
        dir.mkdirs();
        File f = new File(dir, "booking-" + b.id + ".pdf");
        FileOutputStream out = new FileOutputStream(f);
        doc.writeTo(out);
        out.close();
        doc.close();
        return f;
    }

    public static String bookingId(long id) {
        return "BK-" + (id < 10 ? "00" : id < 100 ? "0" : "") + id;
    }

    /** business identity shown on the card */
    public static class Settings {
        public String bizName = "Scorpio Tours & Travels";
        public String bizPhone = "";
        public String vehicle = "Mahindra Scorpio S11";
        public String plate = "";
        public String upiId = "";
        public String payQrPath = "";
    }

    static Paint text(int color, float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(bold ? Typeface.create("sans-serif", Typeface.BOLD) : Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return p;
    }

    static Paint label() {
        Paint p = text(LBLUE, 20, true);
        p.setLetterSpacing(0.22f);
        return p;
    }

    static void glow(Canvas cv, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(cx, cy, r, color, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, p);
    }

    static void glass(Canvas cv, RectF r, float radius) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(0x10FFFFFF);
        cv.drawRoundRect(r, radius, radius, fill);
        Paint st = new Paint(Paint.ANTI_ALIAS_FLAG);
        st.setStyle(Paint.Style.STROKE);
        st.setStrokeWidth(2);
        st.setColor(0x1FFFFFFF);
        cv.drawRoundRect(r, radius, radius, st);
    }

    static String dow(String iso) {
        try {
            Date d = Dates.ISO.parse(iso);
            return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(d);
        } catch (Exception e) { return iso; }
    }

    static void draw(Canvas cv, DataStore.Booking b, Settings s) {
        // ---------- background ----------
        Paint bg = new Paint();
        bg.setShader(new LinearGradient(0, 0, W, H, new int[]{0xFF070B18, 0xFF0C1226, 0xFF111B3D}, null, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, bg);
        glow(cv, 80, -60, 640, 0x552E5BFF);
        glow(cv, W + 40, H + 60, 660, 0x446D4CFF);

        // ---------- main glass panel ----------
        glass(cv, new RectF(36, 36, W - 36, H - 36), 48);

        // ---------- header ----------
        Paint lg = new Paint(Paint.ANTI_ALIAS_FLAG);
        lg.setShader(new LinearGradient(64, 56, 148, 140, ACCENT, ACCENT2, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(64, 56, 148, 140), 26, 26, lg);
        Paint lp = text(Color.WHITE, 42, true);
        lp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics lfm = lp.getFontMetrics();
        cv.drawText(s.bizName.trim().isEmpty() ? "S" : s.bizName.trim().substring(0, 1).toUpperCase(), 106, 98 - (lfm.ascent + lfm.descent) / 2, lp);

        cv.drawText(s.bizName, 172, 100, text(Color.WHITE, 40, true));
        Paint bc = text(STEEL, 21, true);
        bc.setLetterSpacing(0.3f);
        cv.drawText("BOOKING CONFIRMATION", 172, 136, bc);

        Paint bid = text(STEEL, 20, true);
        bid.setTextAlign(Paint.Align.RIGHT);
        cv.drawText("BOOKING ID", W - 64, 92, bid);
        Paint bidv = text(LBLUE, 36, true);
        bidv.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(bookingId(b.id), W - 64, 134, bidv);

        // ---------- route + big dates panel ----------
        glass(cv, new RectF(64, 172, 1020, 400), 32);
        // calendar tile
        Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
        tile.setColor(NAVY);
        cv.drawRoundRect(new RectF(96, 212, 204, 360), 20, 20, tile);
        Paint tileB = new Paint(Paint.ANTI_ALIAS_FLAG);
        tileB.setStyle(Paint.Style.STROKE); tileB.setStrokeWidth(2); tileB.setColor(0x33FFFFFF);
        cv.drawRoundRect(new RectF(96, 212, 204, 360), 20, 20, tileB);
        String day = b.start.length() >= 10 ? b.start.substring(8, 10) : "--";
        String mon = b.start.length() >= 7 ? monthName(b.start.substring(5, 7)) : "";
        Paint dp = text(Color.WHITE, 62, true); dp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics dfm = dp.getFontMetrics();
        cv.drawText(day, 150, 280 - (dfm.ascent + dfm.descent) / 2, dp);
        Paint mp = text(LBLUE, 26, true); mp.setTextAlign(Paint.Align.CENTER);
        cv.drawText(mon, 150, 330, mp);

        // route
        Paint rt = text(Color.WHITE, 46, true);
        float rx = 244;
        cv.drawText(b.pickup.length() == 0 ? "Pickup" : b.pickup.toUpperCase(), rx, 262, rt);
        float rw = rt.measureText(b.pickup.length() == 0 ? "Pickup" : b.pickup.toUpperCase());
        Paint ar = text(ACCENT, 46, true);
        cv.drawText("\u27F6", rx + rw + 26, 262, ar);
        cv.drawText(b.drop.length() == 0 ? "Drop" : b.drop.toUpperCase(), rx + rw + 26 + ar.measureText("\u27F6") + 26, 262, rt);

        // BIG dates
        String dateLine = dow(b.start) + (b.end != null && !b.end.equals(b.start) ? "  \u2192  " + dow(b.end) : "");
        cv.drawText(dateLine, rx, 330, text(Color.WHITE, 38, true));

        // type chip
        Paint chipT = text(LBLUE, 24, true);
        float cw = chipT.measureText(b.type);
        Paint chipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipBg.setColor(0x332E5BFF);
        cv.drawRoundRect(new RectF(rx, 352, rx + cw + 48, 388), 18, 18, chipBg);
        cv.drawText(b.type, rx + 24, 380, chipT);

        // ---------- three info panels ----------
        float py = 424, ph = 168, pw = 302, gap = 25;
        float x1 = 64, x2 = x1 + pw + gap, x3 = x2 + pw + gap;

        // customer
        glass(cv, new RectF(x1, py, x1 + pw, py + ph), 24);
        cv.drawText("CUSTOMER", x1 + 22, py + 40, label());
        cv.drawText(b.name, x1 + 22, py + 82, text(Color.WHITE, 29, true));
        float cy = py + 116;
        for (String line : wrap(b.address.length() == 0 ? "-" : b.address, text(GRAYT, 22, false), pw - 44)) {
            cv.drawText(line, x1 + 22, cy, text(GRAYT, 22, false)); cy += 30;
        }
        if (b.phone.length() > 0) cv.drawText(b.phone, x1 + 22, Math.min(cy, py + ph - 16), text(GRAYT, 22, false));

        // vehicle
        glass(cv, new RectF(x2, py, x2 + pw, py + ph), 24);
        cv.drawText("VEHICLE", x2 + 22, py + 40, label());
        float vy = py + 82;
        for (String line : wrap(s.vehicle, text(Color.WHITE, 27, true), pw - 44)) {
            cv.drawText(line, x2 + 22, vy, text(Color.WHITE, 27, true)); vy += 34;
        }
        if (s.plate.length() > 0) {
            Paint pp = text(LBLUE, 24, true);
            float pwid = pp.measureText(s.plate.toUpperCase());
            Paint pb = new Paint(Paint.ANTI_ALIAS_FLAG);
            pb.setStyle(Paint.Style.STROKE); pb.setStrokeWidth(3); pb.setColor(LBLUE);
            cv.drawRoundRect(new RectF(x2 + 22, py + ph - 56, x2 + 22 + pwid + 36, py + ph - 16), 8, 8, pb);
            cv.drawText(s.plate.toUpperCase(), x2 + 40, py + ph - 28, pp);
        }

        // driver
        glass(cv, new RectF(x3, py, x3 + pw, py + ph), 24);
        float ps = 84, px = x3 + pw - 22 - ps, pyf = py + 34;
        Bitmap photo = null;
        if (b.driverPhoto != null && b.driverPhoto.length() > 0) {
            try {
                Bitmap raw = BitmapFactory.decodeFile(b.driverPhoto);
                if (raw != null) photo = circle(raw, (int) ps);
            } catch (Exception ignored) {}
        }
        if (photo != null) {
            cv.drawBitmap(photo, px, pyf, null);
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(4); ring.setColor(ACCENT);
            cv.drawCircle(px + ps / 2, pyf + ps / 2, ps / 2 - 2, ring);
        } else {
            Paint dg = new Paint(Paint.ANTI_ALIAS_FLAG);
            dg.setShader(new LinearGradient(px, pyf, px + ps, pyf + ps, ACCENT, ACCENT2, Shader.TileMode.CLAMP));
            cv.drawCircle(px + ps / 2, pyf + ps / 2, ps / 2, dg);
            Paint init = text(Color.WHITE, 34, true);
            init.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics ifm = init.getFontMetrics();
            String ini = b.driverName.trim().isEmpty() ? "?" : b.driverName.trim().substring(0, 1).toUpperCase();
            cv.drawText(ini, px + ps / 2, pyf + ps / 2 - (ifm.ascent + ifm.descent) / 2, init);
        }
        cv.drawText("DRIVER", x3 + 22, py + 40, label());
        for (String line : wrap(b.driverName.length() == 0 ? "To be assigned" : b.driverName, text(Color.WHITE, 26, true), pw - 44 - ps)) {
            cv.drawText(line, x3 + 22, py + 84, text(Color.WHITE, 26, true)); break;
        }
        if (b.driverPhone.length() > 0) cv.drawText(b.driverPhone, x3 + 22, py + 120, text(GRAYT, 22, false));

        // ---------- terms + signature + whatsapp ----------
        Paint tp = text(STEEL, 20, false);
        float ty = 632;
        for (String line : wrap("Terms: Confirmed on receipt of advance \u2022 Balance to be cleared before the trip starts \u2022 Carry this card during travel", tp, 940)) {
            cv.drawText(line, 64, ty, tp); ty += 28;
        }
        cv.drawText("For " + s.bizName, 64, ty + 34, text(Color.WHITE, 26, true));
        Paint ln = new Paint(); ln.setColor(STEEL);
        cv.drawRect(64, ty + 82, 380, ty + 84, ln);
        cv.drawText("Authorised Signatory", 64, ty + 112, text(STEEL, 20, false));

        Paint wap = text(GRAYT, 24, false);
        wap.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(s.bizPhone.length() == 0 ? "" : ("Call / WhatsApp: " + s.bizPhone), 1020, ty + 112, wap);

        // ---------- payment panel ----------
        RectF pay = new RectF(1044, 172, W - 64, 760);
        glass(cv, pay, 32);

        // stamp
        String st = b.status();
        int stc = "PAID".equals(st) ? GREEN : "PARTIAL".equals(st) ? LBLUE : RED;
        Paint stamp = text(stc, 26, true);
        stamp.setTextAlign(Paint.Align.CENTER);
        Paint sb = new Paint(Paint.ANTI_ALIAS_FLAG);
        sb.setStyle(Paint.Style.STROKE); sb.setStrokeWidth(4); sb.setColor(stc);
        cv.save();
        cv.rotate(8, W - 180, 232);
        float stw = stamp.measureText(st);
        cv.drawRoundRect(new RectF(W - 180 - stw / 2 - 22, 200, W - 180 + stw / 2 + 22, 258), 10, 10, sb);
        Paint.FontMetrics sfm = stamp.getFontMetrics();
        cv.drawText(st, W - 180, 232 - (sfm.ascent + sfm.descent) / 2, stamp);
        cv.restore();

        cv.drawText("PAYMENT", 1076, 226, label());
        prow(cv, "Total booking", Dates.inr(b.total), Color.WHITE, 286);
        prow(cv, "Advance paid", Dates.inr(b.advance), GREEN, 340);
        prow(cv, "Balance pending", Dates.inr(b.balance()), b.balance() > 0 ? RED : GREEN, 394);

        // progress bar
        float barY = 432;
        Paint tr = new Paint(Paint.ANTI_ALIAS_FLAG); tr.setColor(0x20FFFFFF);
        cv.drawRoundRect(new RectF(1076, barY, 1396, barY + 14), 7, 7, tr);
        long frac = b.total > 0 ? Math.min(b.advance, b.total) * 100 / b.total : 0;
        if (frac > 0) {
            Paint f2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            f2.setShader(new LinearGradient(1076, 0, 1396, 0, ACCENT, ACCENT2, Shader.TileMode.CLAMP));
            cv.drawRoundRect(new RectF(1076, barY, 1076 + Math.max((1396 - 1076) * frac / 100f, 14), barY + 14), 7, 7, f2);
        }
        Paint pct = text(STEEL, 20, true); pct.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(frac + "% paid", 1504, barY + 14, pct);

        // ---------- pay QR ----------
        float qbox = 232, qx = (pay.left + pay.right) / 2 - qbox / 2, qy = 486;
        Paint qb = new Paint(Paint.ANTI_ALIAS_FLAG); qb.setColor(Color.WHITE);
        cv.drawRoundRect(new RectF(qx, qy, qx + qbox, qy + qbox), 18, 18, qb);
        boolean drawn = false;
        if (s.payQrPath != null && s.payQrPath.length() > 0) {
            try {
                Bitmap qr = BitmapFactory.decodeFile(s.payQrPath);
                if (qr != null) {
                    cv.drawBitmap(qr, null, new Rect((int) qx + 16, (int) qy + 16, (int) (qx + qbox) - 16, (int) (qy + qbox) - 16), null);
                    drawn = true;
                }
            } catch (Exception ignored) {}
        }
        if (!drawn && Upi.valid(s.upiId) && b.balance() > 0) {
            drawQr(cv, Upi.uri(s.upiId, s.bizName, b.balance(), bookingId(b.id)), qx + 16, qy + 16, (int) qbox - 32, NAVY);
            drawn = true;
        }
        if (!drawn) {
            Paint ph2 = text(0xFF5A6B8C, 20, false);
            ph2.setTextAlign(Paint.Align.CENTER);
            cv.drawText("Add UPI ID in", qx + qbox / 2, qy + qbox / 2 - 12, ph2);
            cv.drawText("More > Your business", qx + qbox / 2, qy + qbox / 2 + 16, ph2);
        }
        Paint cap = text(Color.WHITE, 24, true);
        cap.setTextAlign(Paint.Align.CENTER);
        cv.drawText(b.balance() > 0 ? ("Scan to pay balance " + Dates.inr(b.balance())) : "Payment complete \u2713",
                (pay.left + pay.right) / 2, qy + qbox + 40, cap);
    }

    static String monthName(String mm) {
        String[] m = {"", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        try { return m[Integer.parseInt(mm)]; } catch (Exception e) { return ""; }
    }

    static void prow(Canvas cv, String k, String v, int vc, float y) {
        cv.drawText(k, 1076, y, text(STEEL, 24, false));
        Paint p = text(vc, 30, true);
        p.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(v, 1504, y, p);
    }

    static void drawQr(Canvas cv, String content, float x, float y, int size, int color) {
        try {
            QrCode qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
            int n = qr.size;
            float scale = (float) size / n;
            Paint p = new Paint();
            p.setColor(color);
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++)
                    if (qr.getModule(c, r))
                        cv.drawRect(x + c * scale, y + r * scale, x + (c + 1) * scale + 0.5f, y + (r + 1) * scale + 0.5f, p);
        } catch (Exception ignored) {}
    }

    static Bitmap circle(Bitmap src, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Path path = new Path();
        path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW);
        c.clipPath(path);
        int s = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - s) / 2, y = (src.getHeight() - s) / 2;
        c.drawBitmap(Bitmap.createBitmap(src, x, y, s, s), null, new Rect(0, 0, size, size), null);
        return out;
    }

    static List<String> wrap(String s, Paint p, float maxW) {
        ArrayList<String> lines = new ArrayList<>();
        if (s == null) return lines;
        String cur = "";
        for (String word : s.split(" ")) {
            String t = cur.length() == 0 ? word : cur + " " + word;
            if (p.measureText(t) > maxW && cur.length() > 0) { lines.add(cur); cur = word; }
            else cur = t;
        }
        if (cur.length() > 0) lines.add(cur);
        return lines;
    }
}
