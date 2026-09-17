# ProGuard rules for Ashu PayBox
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
