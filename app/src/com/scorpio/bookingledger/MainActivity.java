package com.scorpio.bookingledger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    static final int REQ_EXPORT = 11, REQ_IMPORT = 12, REQ_PHOTO = 13, REQ_PAYQR = 14;

    FrameLayout content;
    LinearLayout navBar;
    int tab = 0;          // 0 home, 1 bookings, 2 customers, 3 calendar, 4 more
    int view = 0;         // 0 tab, 1 form, 2 detail, 3 customer detail
    long editId = 0, detailId = 0;
    String customerKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.load(this);
        DataStore.get(this);

        LinearLayout root = Ui.vcol(this);
        content = new FrameLayout(this);
        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(navBar, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        render();
    }

    void render() {
        Window w = getWindow();
        w.setStatusBarColor(Ui.bg);
        w.setNavigationBarColor(Ui.navBg);
        int flags = w.getDecorView().getSystemUiVisibility();
        if (Ui.dark) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(flags);

        content.removeAllViews();
        if (view == 1) content.addView(FormDetail.form(this, editId));
        else if (view == 2) content.addView(FormDetail.detail(this, detailId));
        else if (view == 3) content.addView(Customers.detail(this, customerKey));
        else {
            if (tab == 0) content.addView(Screens.home(this));
            else if (tab == 1) content.addView(Screens.list(this));
            else if (tab == 2) content.addView(Customers.customers(this));
            else if (tab == 3) content.addView(Screens.calendar(this));
            else content.addView(Screens.settings(this));
        }
        buildNav();
    }

    void buildNav() {
        navBar.removeAllViews();
        navBar.setBackgroundColor(Ui.navBg);
        navBar.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));
        String[] icons = {"\uD83C\uDFE0", "\uD83D\uDCCB", "\uD83D\uDC65", "\uD83D\uDCC5", "\u2630"};
        String[] labels = {"Home", "Bookings", "Customers", "Calendar", "More"};
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout cell = Ui.vcol(this);
            cell.setGravity(Gravity.CENTER);
            boolean sel = view == 0 && tab == i;
            TextView ic = Ui.tv(this, icons[i], 20, sel ? Ui.navy : Ui.sub, Ui.medium());
            ic.setGravity(Gravity.CENTER);
            TextView lb = Ui.tv(this, labels[i], 11, sel ? Ui.navy : Ui.sub, sel ? Typeface.DEFAULT_BOLD : Ui.medium());
            lb.setGravity(Gravity.CENTER);
            cell.addView(ic);
            cell.addView(lb);
            cell.setOnClickListener(v -> { view = 0; tab = idx; render(); });
            navBar.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
        }
        View divider = new View(this);
        divider.setBackgroundColor(Ui.line);
    }

    void openForm(long id) { editId = id; view = 1; render(); }
    void openDetail(long id) { detailId = id; view = 2; render(); }
    void openCustomer(String key) { customerKey = key; view = 3; render(); }
    void goBack() { view = 0; render(); }

    @Override
    public void onBackPressed() {
        if (view != 0) goBack();
        else if (tab != 0) { tab = 0; render(); }
        else super.onBackPressed();
    }

    // ----- business identity (shown on booking card) -----
    SharedPreferences prefs() { return getSharedPreferences("settings", 0); }
    String getBizName() { return prefs().getString("biz_name", "Scorpio Tours & Travels"); }
    String getBizPhone() { return prefs().getString("biz_phone", "919918331262"); }
    String getVehicle() { return prefs().getString("vehicle", "Mahindra Scorpio S11"); }
    String getPlate() { return prefs().getString("plate", ""); }
    String getUpi() { return prefs().getString("upi", ""); }
    String getPayQrPath() { return prefs().getString("payqr_path", ""); }
    void setBiz(String n, String p, String v, String pl, String upi) {
        prefs().edit().putString("biz_name", n).putString("biz_phone", p)
                .putString("vehicle", v).putString("plate", pl)
                .putString("upi", upi).apply();
    }
    void setPayQrPath(String path) { prefs().edit().putString("payqr_path", path).apply(); }

    CardRenderer.Settings cardSettings() {
        CardRenderer.Settings s = new CardRenderer.Settings();
        s.bizName = getBizName(); s.bizPhone = getBizPhone();
        s.vehicle = getVehicle(); s.plate = getPlate();
        s.upiId = getUpi(); s.payQrPath = getPayQrPath();
        return s;
    }

    // ----- customer / driver contact -----
    void openWhatsApp(String phone) { openWhatsApp(phone, null); }

    void openWhatsApp(String phone, String text) {
        String d = phone.replaceAll("[^0-9]", "");
        if (d.length() == 10) d = "91" + d;
        if (d.length() < 10) { Ui.toast(this, "No valid mobile number"); return; }
        String url = "https://wa.me/" + d;
        if (text != null) {
            try { url += "?text=" + java.net.URLEncoder.encode(text, "UTF-8"); }
            catch (java.io.UnsupportedEncodingException ignored) {}
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException e) { Ui.toast(this, "WhatsApp not installed"); }
    }

    void sendPaymentReminder(Customers.Agg g) {
        String msg = "Namaste " + (g.name.length() > 0 ? g.name : "") + "! Gentle reminder from "
                + getBizName() + ": your pending balance of " + Dates.inr(g.pending)
                + " across " + g.count + (g.count == 1 ? " booking" : " bookings")
                + " is due. Please pay at your convenience. Thank you!";
        openWhatsApp(g.phone, msg);
    }

    void callNumber(String phone) {
        if (phone.trim().length() == 0) { Ui.toast(this, "No mobile number"); return; }
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()))); }
        catch (ActivityNotFoundException e) { Ui.toast(this, "No dialer found"); }
    }

    // ----- booking card share -----
    void shareCard(DataStore.Booking b, boolean pdf) {
        try {
            File f = pdf ? CardRenderer.pdf(this, b, cardSettings())
                         : CardRenderer.png(this, b, cardSettings());
            Uri uri = Uri.parse("content://com.scorpio.bookingledger.share/" + f.getName());
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(pdf ? "application/pdf" : "image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Share booking card"));
        } catch (Exception e) {
            Ui.toast(this, "Could not create card: " + e.getMessage());
        }
    }

    // ----- driver photo -----
    void pickPhoto() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_PHOTO);
    }

    // ----- payment QR image -----
    void pickPayQr() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_PAYQR);
    }

    // ----- backup -----
    void exportJson() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "scorpio-bookings-backup.json");
        startActivityForResult(i, REQ_EXPORT);
    }

    void importJson() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (req == REQ_EXPORT) {
                java.io.OutputStream out = getContentResolver().openOutputStream(uri);
                out.write(DataStore.get(this).toJson().getBytes("UTF-8"));
                out.close();
                Ui.toast(this, "Backup saved");
            } else if (req == REQ_IMPORT) {
                InputStream in = getContentResolver().openInputStream(uri);
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) bos.write(buf, 0, r);
                in.close();
                final String json = bos.toString("UTF-8");
                Ui.confirm(this, "Restore backup?",
                        "This replaces ALL current bookings with the backup file. Continue?",
                        "Replace",
                        () -> { DataStore.get(this).fromJson(json); DataStore.get(this).save(); Ui.toast(this, "Backup restored"); render(); });
            } else if (req == REQ_PHOTO || req == REQ_PAYQR) {
                InputStream in = getContentResolver().openInputStream(uri);
                File dir = new File(getFilesDir(), "drivers");
                dir.mkdirs();
                File f = new File(dir, (req == REQ_PAYQR ? "payqr" : "driver-" + System.currentTimeMillis()) + ".jpg");
                FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                in.close(); out.close();
                if (req == REQ_PAYQR) { setPayQrPath(f.getAbsolutePath()); Ui.toast(this, "Payment QR saved"); }
                else { FormDetail.onPhotoPicked(f.getAbsolutePath()); Ui.toast(this, "Photo added"); }
            }
        } catch (Exception e) {
            Ui.toast(this, "Failed: " + e.getMessage());
        }
    }
}
