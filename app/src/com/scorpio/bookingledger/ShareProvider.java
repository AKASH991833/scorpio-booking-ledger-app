package com.scorpio.bookingledger;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Serves generated booking cards (PNG/PDF) from the app cache to share targets. */
public class ShareProvider extends ContentProvider {

    @Override
    public boolean onCreate() { return true; }

    private File fileFor(Uri uri) {
        String name = uri.getLastPathSegment();
        return new File(new File(getContext().getCacheDir(), "cards"), name == null ? "x" : name);
    }

    @Override
    public String getType(Uri uri) {
        String n = uri.getLastPathSegment();
        return n != null && n.endsWith(".pdf") ? "application/pdf" : "image/png";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = fileFor(uri);
        if (!f.exists()) throw new FileNotFoundException(uri.toString());
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String s, String[] sa, String s2) {
        File f = fileFor(uri);
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.exists() ? f.length() : 0});
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] sa) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] sa) { return 0; }
}
