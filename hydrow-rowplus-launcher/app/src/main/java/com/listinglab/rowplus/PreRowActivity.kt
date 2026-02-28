package com.listinglab.rowplus

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PreRowActivity : AppCompatActivity() {

    private enum class SessionMode(val label: String, val targetLabel: String?) {
        FREE_ROW(SessionActivity.MODE_FREE_ROW, null),
        TIME_TARGET(SessionActivity.MODE_TIME_TARGET, "20:00 target"),
        DISTANCE_TARGET(SessionActivity.MODE_DISTANCE_TARGET, "5,000 m target"),
    }

    private lateinit var sessionStore: SessionStore
    private lateinit var setupContent: View
    private lateinit var countdownNumber: TextView
    private lateinit var countdownGo: TextView
    private lateinit var countdownRing: View
    private lateinit var targetValue: TextView

    private var selectedMode = SessionMode.FREE_ROW
    private var activeProfileKey: String = UserProfile.SLOT_ONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_row)

        sessionStore = SessionStore(this)
        val profileKey = intent.getStringExtra(EXTRA_PROFILE)
        val profile = profileKey?.let(sessionStore::getProfile)
        if (profile == null) {
            Toast.makeText(this, R.string.profile_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        activeProfileKey = profile.slotKey

        setupContent = findViewById(R.id.setupContent)
        countdownNumber = findViewById(R.id.countdownNumber)
        countdownGo = findViewById(R.id.countdownGo)
        countdownRing = findViewById(R.id.countdownRing)
        targetValue = findViewById(R.id.targetValue)

        findViewById<TextView>(R.id.preRowProfile).text = profile.displayName

        bindModeButton(R.id.modeFreeRow, SessionMode.FREE_ROW)
        bindModeButton(R.id.modeTime, SessionMode.TIME_TARGET)
        bindModeButton(R.id.modeDistance, SessionMode.DISTANCE_TARGET)
        applyModeUi()

        findViewById<View>(R.id.goButton).setOnClickListener {
            startCountdown()
        }
        findViewById<View>(R.id.cancelButton).setOnClickListener {
            finish()
        }
    }

    private fun bindModeButton(viewId: Int, mode: SessionMode) {
        findViewById<TextView>(viewId).setOnClickListener {
            selectedMode = mode
            applyModeUi()
        }
    }

    private fun applyModeUi() {
        val modeButtons = listOf(
            R.id.modeFreeRow to SessionMode.FREE_ROW,
            R.id.modeTime to SessionMode.TIME_TARGET,
            R.id.modeDistance to SessionMode.DISTANCE_TARGET,
        )

        modeButtons.forEach { (viewId, mode) ->
            val view = findViewById<TextView>(viewId)
            if (mode == selectedMode) {
                view.setBackgroundResource(R.drawable.bg_mode_pill_selected)
            } else {
                view.setBackgroundResource(R.drawable.bg_quick_pill)
            }
        }

        val label = selectedMode.targetLabel
        if (label == null) {
            targetValue.visibility = View.GONE
        } else {
            targetValue.text = label
            targetValue.visibility = View.VISIBLE
        }
    }

    private fun startCountdown() {
        setupContent.animate()
            .alpha(0f)
            .setDuration(200L)
            .withEndAction {
                setupContent.visibility = View.GONE
                runCountdownSequence()
            }
            .start()
    }

    private fun runCountdownSequence() {
        countdownRing.visibility = View.VISIBLE
        countdownRing.alpha = 0.3f

        val steps = listOf("3", "2", "1")
        var delay = 0L
        for (step in steps) {
            countdownNumber.postDelayed({ showCountdownStep(step) }, delay)
            delay += 900L
        }

        countdownNumber.postDelayed({
            countdownNumber.visibility = View.GONE
            countdownRing.visibility = View.GONE
            showGoAndLaunch()
        }, delay)
    }

    private fun showCountdownStep(text: String) {
        countdownNumber.text = text
        countdownNumber.visibility = View.VISIBLE
        countdownNumber.scaleX = 0.3f
        countdownNumber.scaleY = 0.3f
        countdownNumber.alpha = 0f

        val growSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(countdownNumber, "scaleX", 0.3f, 1f),
                ObjectAnimator.ofFloat(countdownNumber, "scaleY", 0.3f, 1f),
                ObjectAnimator.ofFloat(countdownNumber, "alpha", 0f, 1f),
            )
            duration = 350L
            interpolator = OvershootInterpolator(1.5f)
        }

        val fadeOut = ObjectAnimator.ofFloat(countdownNumber, "alpha", 1f, 0f).apply {
            startDelay = 500L
            duration = 200L
        }

        val ringPulse = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(countdownRing, "scaleX", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(countdownRing, "scaleY", 1f, 1.15f, 1f),
            )
            duration = 700L
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(growSet, fadeOut, ringPulse)
            start()
        }
    }

    private fun showGoAndLaunch() {
        countdownGo.visibility = View.VISIBLE
        countdownGo.scaleX = 0.5f
        countdownGo.scaleY = 0.5f
        countdownGo.alpha = 0f

        val growSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(countdownGo, "scaleX", 0.5f, 1.1f, 1f),
                ObjectAnimator.ofFloat(countdownGo, "scaleY", 0.5f, 1.1f, 1f),
                ObjectAnimator.ofFloat(countdownGo, "alpha", 0f, 1f),
            )
            duration = 400L
            interpolator = OvershootInterpolator(2f)
        }

        growSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                countdownGo.postDelayed({ launchSession() }, 400L)
            }
        })

        growSet.start()
    }

    private fun launchSession() {
        val intent = Intent(this, SessionActivity::class.java)
            .putExtra(SessionActivity.EXTRA_PROFILE, activeProfileKey)
            .putExtra(SessionActivity.EXTRA_MODE, selectedMode.label)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
    }
}
