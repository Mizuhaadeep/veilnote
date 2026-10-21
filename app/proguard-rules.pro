# Add project specific ProGuard rules here.
# The app currently does not enable minification. Keep this file for release builds.

# Room generated implementations are referenced through reflection by Room.
-keep class * extends androidx.room.RoomDatabase
