# ZeusReader R8 / ProGuard keep rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.solerunner26.zeusreader.**$$serializer { *; }
-keepclassmembers class com.solerunner26.zeusreader.** {
    *** Companion;
}
-keepclasseswithmembers class com.solerunner26.zeusreader.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# MuPDF native JNI
-keep class com.artifex.mupdf.fitz.** { *; }
-dontwarn com.artifex.mupdf.fitz.**

# Readium Kotlin Toolkit
-keep class org.readium.** { *; }
-dontwarn org.readium.**

# ML Kit
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }
