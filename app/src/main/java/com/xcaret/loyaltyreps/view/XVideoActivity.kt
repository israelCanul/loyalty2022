package com.xcaret.loyaltyreps.view

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.Constraints
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import com.xcaret.loyaltyreps.R
import com.xcaret.loyaltyreps.databinding.ActivityXvideoBinding

class XVideoActivity : AppCompatActivity() {

    lateinit var binding: ActivityXvideoBinding
    var xvideoUrl = ""
    private val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
    private var trackSelector: DefaultTrackSelector? = null
    private var player: SimpleExoPlayer? = null
    private var shouldAutoPlay: Boolean = true
    private lateinit var mediaDataSourceFactory: DataSource.Factory

    private lateinit var fullscreenContent: PlayerView
    private val hideHandler = Handler()
    private val hideRunnable = Runnable { hide() }

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
    }
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide(100)
    }
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_xvideo)

        fullscreenContent = binding.xvideoPlayer//for fullscren

        supportActionBar!!.title = resources.getString(R.string.go_back)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.hide()

        xvideoUrl = intent.getStringExtra("xvideo_url")!!
        val video_id = intent.getStringExtra("video_id")!!.toInt()
        if (video_id == 0){
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            //hideActionBar()
            binding.xvideoPlayer.visibility = View.GONE
            binding.tutorial.visibility = View.VISIBLE
            binding.tutorial.setVideoURI(Uri.parse(xvideoUrl))
            binding.tutorial.start()
        } else {
            println("video no se ")
            //ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            //hideActionBar()
            binding.tutorial.visibility = View.GONE
            binding.xvideoPlayer.visibility = View.VISIBLE
            mediaDataSourceFactory = DefaultHttpDataSourceFactory(
                Util.getUserAgent(this, "loyaltyreps"),
                null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true
            )

            playVideo(xvideoUrl)
        }
    }

    private fun hideActionBar(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH
                window.navigationBarColor = getColor(R.color.colorTransparent)
                it.hide(WindowInsets.Type.statusBars())
            }
        }else{
            @Suppress("Deprecation")
            window.decorView.systemUiVisibility = (
                    //View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Hide the nav bar and status bar
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            // Keep the app content behind the bars even if user swipes them up
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
        actionBar?.hide()
        supportActionBar?.hide()
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }


    private fun playVideo(xv_url: String?){
        binding.xvideoPlayer.requestFocus()

        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_SPEECH)
            .build()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        player = ExoPlayerFactory.newSimpleInstance(this@XVideoActivity, trackSelector)

        binding.xvideoPlayer.player = player

        player!!.setAudioAttributes(audioAttributes, true)
        player!!.playWhenReady = shouldAutoPlay
        binding.xvideoPlayer.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT


        val mediaSource = ExtractorMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(Uri.parse(xv_url))
        player!!.prepare(mediaSource)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (player != null) {
                    player!!.playWhenReady = false
                    player!!.release()
                    player = null
                }
                if (binding.tutorial.isPlaying) {
                    binding.tutorial.stopPlayback()
                }
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        if (player != null) {
            player!!.playWhenReady = false
            player!!.release()
            player = null
        }

        if (binding.tutorial.isPlaying) {
            binding.tutorial.stopPlayback()
        }

    }


    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}
