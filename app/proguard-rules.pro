# Proguard rules for ScanPay Kotlin App
-keep class com.scanpay.app.data.model.** { *; }
-keep class com.scanpay.app.data.request.** { *; }
-keep class com.scanpay.app.data.response.** { *; }
-keep class com.scanpay.app.api.** { *; }
-dontwarn com.google.zxing.**

