package ca.asmat.buzzkill.oem

import android.os.Build

enum class Oem { OnePlus, Pixel, Samsung, Xiaomi, Oppo, Vivo, Realme, Huawei, Generic }

object OemDetector {
    /** Cached on first read to avoid repeated string ops. */
    val current: Oem by lazy { detect() }

    /** True on the Android emulator. Used to refuse the live-shutdown path while testing. */
    val isEmulator: Boolean by lazy {
        (
            Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                Build.PRODUCT == "google_sdk" ||
                Build.HARDWARE.contains("ranchu") ||
                Build.HARDWARE.contains("goldfish")
            )
    }

    private fun detect(): Oem {
        val mfr = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return when {
            mfr.contains("oneplus") || brand.contains("oneplus") -> Oem.OnePlus
            mfr.contains("google") -> Oem.Pixel
            mfr.contains("samsung") -> Oem.Samsung
            mfr.contains("xiaomi") || brand.contains("redmi") -> Oem.Xiaomi
            mfr.contains("oppo") -> Oem.Oppo
            mfr.contains("vivo") -> Oem.Vivo
            mfr.contains("realme") -> Oem.Realme
            mfr.contains("huawei") || mfr.contains("honor") -> Oem.Huawei
            else -> Oem.Generic
        }
    }
}
