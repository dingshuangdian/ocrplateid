# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\DevelopTools\Studio2.0\studioSdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#混淆start
#*******************下面避免混淆配置需要在主工程中配置*******************


# -------------不需要更改-------------------------------------------------
-optimizationpasses 5                    # 指定代码的压缩级别
-dontusemixedcaseclassnames              # 是否使用大小写混合
-dontpreverify                           # 混淆时是否做预校验
-verbose                                 # 混淆时是否记录日志
-dontwarn                                # 去掉警告
-dontskipnonpubliclibraryclassmembers    # 是否混淆第三方jar
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # 混淆时所采用的算法


# ------ 保持哪些类不被混淆    ---------------不需要更改-------------------------------------------------

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService


# ------ 保持一些方法不被混淆    ----------------不需要更改----------------------------------------------
#------保持native方法不被混淆 ------
-keepclasseswithmembernames class * {
    native <methods>;
}
#------保持自定义控件类不被混淆------
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
#------保持自定义控件类不被混淆------
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
#------保持自定义控件类不被混淆------
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
#------保持枚举 enum 类不被混淆 ------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
#------保持 Parcelable 不被混淆------
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ------ 去掉发出的警告   ----------------随情况更改-------------------------------------



-dontwarn com.wintone.Adaptor.**          #去掉该路径下的类所发出的警告
-keep class com.wintone.Adaptor.** { *;}  #如果该路径发出了警告，就不混淆该路径
-dontwarn com.wintone.cipher.**
-keep class com.wintone.cipher.** { *;}
-dontwarn com.kernal.lisence.**
-keep class com.kernal.lisence.** { *;}
-dontwarn kernal.sun.misc.**
-keep class kernal.sun.misc.** { *;}
-dontwarn com.kernal.plateid.**
-keep class com.kernal.plateid.** { *;}
-dontwarn utills.**
-keep class utills.** { *;}

# ------ 序列号授权需要   -----------
#-dontwarn org.**
#-keep class org.** { *;}


#混淆end