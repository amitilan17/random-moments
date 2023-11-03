package com.example.randommemories.onboarding

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.randommemories.MainActivity
import com.example.randommemories.R
import com.example.randommemories.helpers.LocaleHelper


class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var activeVideo: VideoView? = null
    private var activeImage: ImageView? = null
    private lateinit var pageIndicatorLayout: LinearLayout
    private lateinit var continueButton: Button

    private val onboardingItems = listOf(
        OnboardingItem(
            title = R.string.onboarding1_title,
            text1 = R.string.onboarding1_text1,
            text2 = R.string.onboarding1_text2,
            text3 = R.string.onboarding1_text3,
            video = R.raw.road
        ),
        OnboardingItem(
            title = R.string.onboarding2_title,
            text1 = R.string.onboarding2_text,
            video = R.raw.sea
        ),
        OnboardingItem(
            title = R.string.onboarding3_title,
            text1 = R.string.onboarding3_text1,
            text2 = R.string.onboarding3_text2,
            text3 = R.string.onboarding3_text3,
            text4 = R.string.onboarding3_text4,
            text5 = R.string.onboarding3_text5,
            video = R.raw.leaves,
            buttonText = R.string.onboarding3_button
        ),
    ).reversed()


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState, persistentState)

        supportActionBar?.hide()
        actionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()

        supportActionBar?.hide()
        actionBar?.hide()
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false

        LocaleHelper.onCreate(this, "he")
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        pageIndicatorLayout = findViewById(R.id.pageIndicatorLayout)
        continueButton = findViewById(R.id.continueButton)

        val adapter = OnboardingPagerAdapter(onboardingItems, context = this, activity = this)
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
                updateButtonVisibility(position)
            }
        })

        // HEBREW - Set initial page to the rightmost
        viewPager.setCurrentItem(adapter.itemCount - 1, false)
        setupPageIndicator()

        continueButton.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun setupPageIndicator() {
        val dots = arrayOfNulls<ImageView>(onboardingItems.size)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        layoutParams.setMargins(8, 0, 8, 0)

        // HEBREW - Iterate in reverse order
        for (i in onboardingItems.indices.reversed()) {
            dots[i] = ImageView(this)
            dots[i]?.setImageResource(R.drawable.onboarding_indicator_unselected)
            pageIndicatorLayout.addView(dots[i], layoutParams)
        }

        updatePageIndicator(onboardingItems.size - 1)
    }

    private fun updatePageIndicator(currentPosition: Int) {
        for (i in 0 until pageIndicatorLayout.childCount) {
            val dot = pageIndicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(
                if (i == currentPosition) R.drawable.onboarding_indicator_selected
                else R.drawable.onboarding_indicator_unselected
            )
        }
    }

    private fun updateButtonVisibility(currentPosition: Int) {
        // HEBREW - page indicator ui defined in reversed order to match hebrew
        // therefore 0 position is the last screen
        if (currentPosition == 0) {
            continueButton.setText(onboardingItems[currentPosition].buttonText!!)
            continueButton.visibility = View.VISIBLE
        } else {
            continueButton.visibility = View.INVISIBLE
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    class OnboardingPagerAdapter(
        private val screens: List<OnboardingItem>,
        private val context: Context,
        private val activity: OnboardingActivity
    ) :
        RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(viewType, parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            val screen = screens[position]
            activity.activeImage?.visibility = View.VISIBLE
            activity.activeVideo?.start()
            holder.bind(screen, context)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onViewAttachedToWindow(holder: OnboardingViewHolder) {
            super.onViewAttachedToWindow(holder)
            activity.activeVideo = holder.itemView.findViewById(R.id.video)
            activity.activeImage = holder.itemView.findViewById(R.id.first_frame)
            activity.activeImage?.visibility = View.VISIBLE
            val video = (activity.activeVideo as VideoView)
            video.start()
            video.setOnCompletionListener {
                video.seekTo(0)
                video.start()
            }
            video.setOnPreparedListener { mp ->
                mp.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        activity.activeImage?.visibility = View.GONE
                        return@setOnInfoListener true
                    }
                    return@setOnInfoListener false
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: OnboardingViewHolder) {
            super.onViewDetachedFromWindow(holder)
            activity.activeVideo = null
            activity.activeImage = null
        }


        override fun getItemCount(): Int {
            return screens.size
        }

        override fun getItemViewType(position: Int): Int {
            println("position is: $position")
            return when (position) {
                0 -> R.layout.onboarding_screen_3
                1 -> R.layout.onboarding_screen_2
                2 -> R.layout.onboarding_screen_1
                else -> error("no onboarding screen number $position")
            }
        }

        override fun getItemId(position: Int): Long {
            return screens.size - 1 - position.toLong()
        }

        class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val video: VideoView = itemView.findViewById(R.id.video)
            private val image: ImageView = itemView.findViewById(R.id.first_frame)

            init {
                image.visibility = View.VISIBLE
                video.setOnCompletionListener { video.start() }
                video.setOnPreparedListener { mp ->
                    mp.setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            image.visibility = View.GONE
                            return@setOnInfoListener true
                        }
                        return@setOnInfoListener false
                    }
                }
            }

            fun bind(screen: OnboardingItem, context: Context) {
                val title = itemView.findViewById<TextView>(R.id.title)
                title.setText(screen.title)

                val text1 = itemView.findViewById<TextView>(R.id.text_1)
                text1.setText(screen.text1)

                if (screen.text2 != null) {
                    val text2 = itemView.findViewById<TextView>(R.id.text_2)
                    text2.setText(screen.text2)
                }

                if (screen.text3 != null) {
                    val text3 = itemView.findViewById<TextView>(R.id.text_3)
                    text3.setText(screen.text3)
                }

                if (screen.text4 != null) {
                    val text4 = itemView.findViewById<TextView>(R.id.text_4)
                    text4.setText(screen.text4)
                }

                if (screen.text5 != null) {
                    val text5 = itemView.findViewById<TextView>(R.id.text_5)
                    text5.setText(screen.text5)
                }

                image.visibility = View.VISIBLE

                video.setVideoURI(Uri.parse("android.resource://" + context.packageName + "/" + screen.video))
                video.start()

                video.setOnCompletionListener {
                    video.seekTo(0)
                    video.start()
                }
                video.setOnPreparedListener { mp ->
                    mp.setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            image.visibility = View.GONE
                            return@setOnInfoListener true
                        }
                        return@setOnInfoListener false
                    }
                }
            }
        }
    }
}