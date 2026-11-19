package com.scorpio.bookingledger;

import java.net.URLEncoder;

public class Upi {
    /** Offline UPI pay link for the balance amount. No real ID is hardcoded anywhere. */
    public static String uri(String upiId, String payeeName, long amount, String bookingId) {
        try {
            String pn = URLEncoder.encode(payeeName, "UTF-8").replace("+", "%20");
            String tn = URLEncoder.encode("Booking " + bookingId + " balance", "UTF-8").replace("+", "%20");
            return "upi://pay?pa=" + upiId.trim() + "&pn=" + pn + "&am=" + amount + "&cu=INR&tn=" + tn;
        } catch (Exception e) {
            return "upi://pay?pa=" + upiId.trim() + "&cu=INR";
        }
    }

    public static boolean valid(String upiId) {
        return upiId != null && upiId.trim().matches("[A-Za-z0-9._\\-]{2,}@[A-Za-z]{2,}[A-Za-z0-9.]*");
    }
}
