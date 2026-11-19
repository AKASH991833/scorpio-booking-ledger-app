package com.scorpio.bookingledger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;

public class CardRenderer {
    static final int W = 1080, H = 1440;
    static final int NAVY = 0xFF0B1B3F, GOLD = 0xFFD4AF37, GRAY = 0xFF5A6B8C,
            GREEN = 0xFF15803D, RED = 0xFFDC2626, LIGHT = 0xFFF4F6FB, INK = 0xFF0B1B3F;

    public static File png(Context c, DataStore.Booking b, String bizName, String bizPhone) throws Exception {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        draw(cv, b, bizName, bizPhone);
        File dir = new File(c.getCacheDir(), "cards");
        dir.mkdirs();
        File f = new File(dir, "booking-" + b.id + ".png");
        FileOutputStream out = new FileOutputStream(f);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        return f;
    }

    public static File pdf(Context c, DataStore.Booking b, String bizName, String bizPhone) throws Exception {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        draw(cv, b, bizName, bizPhone);
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(W, H, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
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

    static Paint text(int color, float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(bold ? Typeface.create("sans-serif", Typeface.BOLD) : Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return p;
    }

    static void draw(Canvas cv, DataStore.Booking b, String bizName, String bizPhone) {
        cv.drawColor(Color.WHITE);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        // header
        fill.setColor(NAVY);
        cv.drawRect(0, 0, W, 330, fill);
        fill.setColor(GOLD);
        cv.drawRect(0, 330, W, 338, fill);

        Paint gold = text(GOLD, 30, true);
        gold.setLetterSpacing(0.28f);
        cv.drawText("BOOKING CONFIRMATION", 70, 96, gold);
        Paint bn = text(Color.WHITE, 62, true);
        cv.drawText(bizName == null || bizName.length() == 0 ? "Scorpio Tours & Travels" : bizName, 70, 175, bn);
        Paint sub = text(0xFFB9C6E4, 34, false);
        cv.drawText("Mahindra Scorpio S11  \u2022  " + b.type, 70, 235, sub);
        Paint date = text(Color.WHITE, 40, true);
        cv.drawText(Dates.range(b.start, b.end), 70, 292, date);

        float y = 430;
        Paint lab = text(GRAY, 30, true);
        lab.setLetterSpacing(0.18f);

        // customer
        cv.drawText("CUSTOMER", 70, y, lab);
        y += 66;
        cv.drawText(b.name, 70, y, text(INK, 52, true));
        y += 52;
        if (b.address.length() > 0) {
            for (String line : wrap(b.address, text(GRAY, 36, false), W - 140)) {
                cv.drawText(line, 70, y, text(GRAY, 36, false));
                y += 48;
            }
        }
        if (b.phone.length() > 0) { cv.drawText("Ph: " + b.phone, 70, y, text(GRAY, 36, false)); y += 48; }

        y += 18;
        if (Screens.route(b).length() > 0) {
            fill.setColor(LIGHT);
            cv.drawRoundRect(new RectF(70, y, W - 70, y + 96), 24, 24, fill);
            Paint rt = text(INK, 38, true);
            cv.drawText(b.pickup + "  \u2192  " + b.drop, 104, y + 62, rt);
            y += 96;
        }
        y += 56;

        // driver
        cv.drawText("YOUR DRIVER", 70, y, lab);
        float photoSize = 168;
        float photoX = W - 70 - photoSize, photoY = y - 44;
        Bitmap photo = null;
        if (b.driverPhoto != null && b.driverPhoto.length() > 0) {
            try {
                Bitmap raw = BitmapFactory.decodeFile(b.driverPhoto);
                if (raw != null) photo = circle(raw, (int) photoSize);
            } catch (Exception ignored) {}
        }
        float dy = y + 44;
        if (b.driverName.length() > 0) { cv.drawText(b.driverName, 70, dy, text(INK, 46, true)); dy += 58; }
        if (b.driverPhone.length() > 0) { cv.drawText("Ph: " + b.driverPhone, 70, dy, text(GRAY, 36, false)); dy += 58; }
        if (b.driverName.length() == 0 && b.driverPhone.length() == 0) { cv.drawText("Driver details will be shared soon", 70, dy, text(GRAY, 34, false)); dy += 58; }

        if (photo != null) {
            cv.drawBitmap(photo, photoX, photoY, null);
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(6); ring.setColor(GOLD);
            cv.drawCircle(photoX + photoSize / 2, photoY + photoSize / 2, photoSize / 2 - 3, ring);
        } else if (b.driverName.length() > 0) {
            fill.setColor(NAVY);
            cv.drawCircle(photoX + photoSize / 2, photoY + photoSize / 2, photoSize / 2, fill);
            Paint init = text(Color.WHITE, 64, true);
            init.setTextAlign(Paint.Align.CENTER);
            String ini = b.driverName.trim().substring(0, 1).toUpperCase();
            Paint.FontMetrics fm = init.getFontMetrics();
            cv.drawText(ini, photoX + photoSize / 2, photoY + photoSize / 2 - (fm.ascent + fm.descent) / 2, init);
        }

        y = Math.max(dy, photoY + photoSize + 50) + 30;

        // amounts box
        float boxH = 250;
        fill.setColor(LIGHT);
        cv.drawRoundRect(new RectF(70, y, W - 70, y + boxH), 28, 28, fill);
        float ry = y + 76;
        amt(cv, "Total booking", Dates.inr(b.total), INK, ry); ry += 76;
        amt(cv, "Advance paid", Dates.inr(b.advance), GREEN, ry); ry += 76;
        amt(cv, "Balance pending", Dates.inr(b.balance()), b.balance() > 0 ? RED : GREEN, ry);
        y += boxH;

        // footer
        fill.setColor(NAVY);
        cv.drawRect(0, H - 130, W, H, fill);
        Paint th = text(Color.WHITE, 34, true);
        th.setTextAlign(Paint.Align.CENTER);
        cv.drawText("Thank you for booking with us!", W / 2f, H - 74, th);
        Paint ft = text(0xFFB9C6E4, 30, false);
        ft.setTextAlign(Paint.Align.CENTER);
        cv.drawText(bizPhone == null || bizPhone.length() == 0 ? "" : ("Call / WhatsApp: " + bizPhone), W / 2f, H - 30, ft);
    }

    static void amt(Canvas cv, String k, String v, int vc, float y) {
        cv.drawText(k, 110, y, text(GRAY, 36, false));
        Paint p = text(vc, 44, true);
        p.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(v, W - 110, y, p);
    }

    static Bitmap circle(Bitmap src, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Path path = new Path();
        path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW);
        c.clipPath(path);
        int s = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - s) / 2, y = (src.getHeight() - s) / 2;
        Rect r = new Rect(0, 0, size, size);
        c.drawBitmap(Bitmap.createBitmap(src, x, y, s, s), null, r, null);
        return out;
    }

    static java.util.List<String> wrap(String s, Paint p, float maxW) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
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
