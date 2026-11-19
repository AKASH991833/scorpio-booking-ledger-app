#!/bin/bash
set -e
SDK=$HOME/sdk
BT=$SDK/bt/android-14
AJAR=$SDK/plat/android-34/android.jar
APP=$HOME/scorpio/app
OUT=$HOME/scorpio/out
rm -rf $OUT
mkdir -p $OUT/res $OUT/classes $OUT/dex

echo "== aapt2 compile =="
$BT/aapt2 compile --dir $APP/res -o $OUT/res.zip

echo "== aapt2 link =="
$BT/aapt2 link -o $OUT/base.apk -I $AJAR \
  --manifest $APP/AndroidManifest.xml \
  --java $OUT/gen \
  --min-sdk-version 24 --target-sdk-version 34 \
  $OUT/res.zip

echo "== javac =="
java -jar $SDK/ecj.jar -source 1.8 -target 1.8 -nowarn -classpath $AJAR -d $OUT/classes \
  $OUT/gen/com/scorpio/bookingledger/R.java \
  $APP/src/com/scorpio/bookingledger/*.java

echo "== d8 =="
$BT/d8 --min-api 24 --output $OUT/dex $(find $OUT/classes -name '*.class')

echo "== package dex =="
cd $OUT/dex && zip -q -j $OUT/base.apk classes.dex && cd -

echo "== keytool =="
if [ ! -f $HOME/scorpio/keystore.jks ]; then
  keytool -genkeypair -keystore $HOME/scorpio/keystore.jks -alias scorpio \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass scorpio123 -keypass scorpio123 \
    -dname "CN=Scorpio Booking Ledger" 2>/dev/null
fi

echo "== zipalign + sign =="
$BT/zipalign -f 4 $OUT/base.apk $OUT/aligned.apk
$BT/apksigner sign --ks $HOME/scorpio/keystore.jks --ks-pass pass:scorpio123 \
  --out $HOME/scorpio/scorpio-booking-ledger.apk $OUT/aligned.apk

echo "== verify =="
$BT/apksigner verify --verbose --print-certs $HOME/scorpio/scorpio-booking-ledger.apk | head -8
$BT/aapt2 dump badging $HOME/scorpio/scorpio-booking-ledger.apk | head -12
ls -la $HOME/scorpio/scorpio-booking-ledger.apk
