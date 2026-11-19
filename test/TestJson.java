package com.scorpio.bookingledger;
public class TestJson {
    static int pass=0, fail=0;
    static void eq(Object g, Object w, String n){ if(w.equals(g))pass++; else {fail++; System.out.println("FAIL "+n+" got="+g+" want="+w);} }
    public static void main(String[] a) throws Exception {
        DataStore.Booking b = new DataStore.Booking();
        b.id=7; b.name="Rahul Sharma"; b.phone="9876543210"; b.type="Wedding";
        b.start="2026-11-20"; b.end="2026-11-22"; b.pickup="Thane"; b.drop="Lonavala";
        b.total=25000; b.advance=10000; b.address="12 MG Road, Thane West";
        b.driverName="Ravi Kumar"; b.driverPhone="9123456780"; b.driverPhoto="/data/x.jpg"; b.notes="night halt";
        String j = b.toJson().toString();
        DataStore.Booking c = DataStore.Booking.fromJson(new org.json.JSONObject(j));
        eq(c.id,7L,"id"); eq(c.name,"Rahul Sharma","name"); eq(c.phone,"9876543210","phone");
        eq(c.type,"Wedding","type"); eq(c.start,"2026-11-20","start"); eq(c.end,"2026-11-22","end");
        eq(c.pickup,"Thane","pickup"); eq(c.drop,"Lonavala","drop");
        eq(c.total,25000L,"total"); eq(c.advance,10000L,"advance");
        eq(c.address,"12 MG Road, Thane West","address");
        eq(c.driverName,"Ravi Kumar","driverName"); eq(c.driverPhone,"9123456780","driverPhone");
        eq(c.driverPhoto,"/data/x.jpg","driverPhoto"); eq(c.notes,"night halt","notes");
        eq(c.balance(),15000L,"balance"); eq(c.status(),"PARTIAL","status partial");
        DataStore.Booking p = new DataStore.Booking(); p.total=8000; p.advance=8000;
        eq(p.status(),"PAID","status paid"); eq(p.balance(),0L,"balance paid");
        DataStore.Booking q = new DataStore.Booking(); q.total=8000; q.advance=0;
        eq(q.status(),"PENDING","status pending");
        DataStore.Booking e = DataStore.Booking.fromJson(new org.json.JSONObject("{\"start\":\"2026-11-20\"}"));
        eq(e.end,"2026-11-20","end defaults to start");
        System.out.println("PASS="+pass+" FAIL="+fail);
        if(fail>0) System.exit(1);
    }
}
