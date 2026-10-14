# Enable R8 code shrinking and obfuscation to strip unused Admin screens from Reception builds
-repackageclasses 'a'
-allowaccessmodification
-dontusemixedcaseclassnames
-verbose

# Keep essential entry points for Reception
-keep class com.example.ReceptionActivity { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.data.** { *; }
-keep class com.example.ui.screens.reception.** { *; }
