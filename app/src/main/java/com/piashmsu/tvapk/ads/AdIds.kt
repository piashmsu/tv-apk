package com.piashmsu.tvapk.ads

/**
 * AdMob ad-unit identifiers used by the app.
 *
 * The app ID itself is declared in the manifest as
 * `com.google.android.gms.ads.APPLICATION_ID` meta-data and must match the
 * value here.
 */
object AdIds {
    /** Live AdMob app ID. */
    const val APP_ID: String = "ca-app-pub-8891768651066045~8943470926"

    /** Live rewarded ad unit ID — unlocks 30 minutes of recording. */
    const val REWARDED_RECORDING: String = "ca-app-pub-8891768651066045/7557184713"

    /** Google's official rewarded test ID — only used during local dev. */
    const val REWARDED_TEST: String = "ca-app-pub-3940256099942544/5224354917"

    /** Length of the premium unlock granted by one rewarded ad. */
    const val UNLOCK_DURATION_MS: Long = 30L * 60L * 1000L
}
