# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Markwon
-keep class io.noties.markwon.** { *; }
-keep class org.commonmark.** { *; }

# Keep Prism4j
-keep class nu.validator.htmlparser.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep PDFBox
-keep class com.tom_roush.pdfbox.** { *; }

# Keep model classes
-keep class com.thaibanai.multillmchat.data.local.entity.** { *; }
-keep class com.thaibanai.multillmchat.data.remote.model.** { *; }
-keep class com.thaibanai.multillmchat.domain.model.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
