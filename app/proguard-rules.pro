# Firebase
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.Exclude *;
}
-keep class com.zixo.app.data.model.** { *; }
