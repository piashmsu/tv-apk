package com.piashmsu.tvapk.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loads and presents Google AdMob rewarded ads. When the user finishes
 * watching the ad and earns the reward, the supplied `onReward` callback
 * fires — the app uses that to extend the premium-unlock window in
 * [com.piashmsu.tvapk.data.AppPrefs].
 *
 * The class keeps exactly one preloaded ad in memory at a time and
 * automatically reloads after a successful presentation.
 */
class RewardedAdManager(private val appContext: Context) {

    private val initialised = AtomicBoolean(false)

    private val _state = MutableStateFlow<AdState>(AdState.Idle)
    val state: StateFlow<AdState> = _state.asStateFlow()

    private var loaded: RewardedAd? = null

    /**
     * Initialise the Mobile Ads SDK. Safe to call multiple times — only
     * runs once. Should be invoked from `Application.onCreate`.
     */
    fun initialiseOnce() {
        if (!initialised.compareAndSet(false, true)) return
        try {
            MobileAds.initialize(appContext) { /* status unused */ }
        } catch (t: Throwable) {
            Log.w(TAG, "MobileAds.initialize failed: ${t.message}")
        }
    }

    /**
     * Begin loading a rewarded ad in the background. Idempotent — returns
     * immediately if an ad is already loaded or loading.
     */
    fun preload() {
        initialiseOnce()
        if (loaded != null) {
            _state.value = AdState.Ready
            return
        }
        if (_state.value is AdState.Loading) return
        _state.value = AdState.Loading
        try {
            RewardedAd.load(
                appContext,
                AdIds.REWARDED_RECORDING,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        loaded = ad
                        _state.value = AdState.Ready
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loaded = null
                        _state.value = AdState.Error(
                            error.message.ifBlank { "Failed to load ad (code ${error.code})" }
                        )
                    }
                },
            )
        } catch (t: Throwable) {
            _state.value = AdState.Error(t.message ?: "Failed to load ad")
        }
    }

    /**
     * Show the preloaded rewarded ad and call [onReward] when (and only
     * when) the user fully completes the ad and earns the reward.
     *
     * Returns `false` if no ad is loaded yet — the caller should preload
     * first or fall back to a manual unlock path.
     */
    fun show(activity: Activity, onReward: () -> Unit, onClosed: (rewarded: Boolean) -> Unit = {}): Boolean {
        val ad = loaded ?: return false
        var rewarded = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                _state.value = AdState.Showing
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loaded = null
                _state.value = AdState.Error(error.message.ifBlank { "Ad failed to show" })
                preload()
                onClosed(false)
            }

            override fun onAdDismissedFullScreenContent() {
                loaded = null
                _state.value = AdState.Idle
                preload()
                onClosed(rewarded)
            }
        }

        ad.show(activity, OnUserEarnedRewardListener { _: RewardItem ->
            rewarded = true
            onReward()
        })
        return true
    }

    sealed interface AdState {
        data object Idle : AdState
        data object Loading : AdState
        data object Ready : AdState
        data object Showing : AdState
        data class Error(val message: String) : AdState
    }

    companion object {
        private const val TAG = "RewardedAdManager"
    }
}
