# Firebase
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.Exclude *;
}
-keep class com.zexo.app.data.model.** { *; }
