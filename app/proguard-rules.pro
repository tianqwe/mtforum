# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep debugging info for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ==================== Model Classes ====================
# Keep all model classes for Gson serialization
-keep class com.forum.mt.model.** { *; }
-keep class com.forum.mt.viewmodel.** { *; }

# Keep all inner classes in model package (including nested ones)
-keep class com.forum.mt.model.**$* { *; }
-keep class com.forum.mt.viewmodel.**$* { *; }

# ==================== Adapter Classes ====================
# Keep all adapter classes and their ViewHolders (including inner classes)
-keep class com.forum.mt.ui.adapter.** { *; }
-keep class com.forum.mt.ui.adapter.**$* { *; }
-keep class com.forum.mt.adapter.** { *; }
-keep class com.forum.mt.adapter.**$* { *; }

# ==================== UI Classes ====================
# Keep all UI classes and their inner classes
-keep class com.forum.mt.ui.** { *; }
-keep class com.forum.mt.ui.**$* { *; }

# Keep inner classes (like ContentBlock$Attachment)
-keepclassmembers class com.forum.mt.model.** {
    public static ** valueOf(java.lang.String);
    public *;
}

# ==================== Gson ====================
# Gson uses generic type information stored in a class file
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== OkHttp ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Okio
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ==================== Retrofit ====================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Retrofit does not support the following options
# -keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

# ==================== Glide ====================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}
-keep class com.bumptech.glide.load.model.GlideUrl { *; }
-keep class com.bumptech.glide.Glide { *; }
-dontwarn com.bumptech.glide.load.model.GlideUrl

# ==================== Jsoup ====================
-keeppackagenames org.jsoup.nodes
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ==================== PhotoView ====================
-keep class com.github.chrisbanes.photoview.** { *; }

# ==================== AndroidX & Material ====================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ==================== WebView JavaScript Interface ====================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ==================== General Android ====================
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BuildConfig
-keep class com.forum.mt.BuildConfig { *; }
