package com.example.spenremote.racingcar

import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import com.example.spenremote.racingcar.RacingCar.PlayState
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    private var contLabel: View? = null
    private var tvNotify: TextView? = null
    private var tvScore: TextView? = null
    private var tvBest: TextView? = null
    private var  ivCenter: ImageView? = null
    private var racingCar: RacingCar? = null
    private var score = 0
    private var level = 0
    private var bestScore = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        contLabel = findViewById(R.id.contNotify)
        tvNotify = findViewById(R.id.notify)
        tvScore = findViewById(R.id.score)
        tvBest = findViewById(R.id.best)
        ivCenter = findViewById(R.id.imgCenter)
        ivCenter?.setOnClickListener { play() }
        racingCar = findViewById(R.id.racingView)
        initialize()
    }

    override fun onResume() {
        super.onResume()
        if (racingCar != null && racingCar!!.playState === PlayState.Pause) {
            racingCar!!.resume()
        }
    }

    override fun onPause() {
        if (racingCar != null && racingCar!!.playState === PlayState.Playing) {
            pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        racingCar!!.reset()
        super.onDestroy()
    }

    private val racingHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                RacingCar.MSG_SCORE -> {
                    score += level
                    tvScore!!.text = score.toString()
                }
                RacingCar.MSG_COLLISION -> {
                    var achieveBest = false
                    if (bestScore < score) {
                        tvBest!!.text = score.toString()
                        bestScore = score
                        saveBestScore(bestScore)
                        achieveBest = true
                    }
                    collision(achieveBest)
                }
                RacingCar.MSG_COMPLETE -> prepare()
                RacingCar.MSG_PAUSE -> {
                    setTvNotify(R.string.pause)
                    prepare()
                }
                else -> {
                }
            }
        }
    }

    private fun initialize() {
        reset()
        setTvNotify(R.string.ready)
        prepare()
    }

    private fun loadBestScore(): Int {
        val preferences = getSharedPreferences("MyFirstGame", MODE_PRIVATE)
        return if (preferences.contains("BestScore")) {
            preferences.getInt("BestScore", 0)
        } else {
            0
        }
    }

    private fun saveBestScore(bestScore: Int) {
        val preferences = getSharedPreferences("MyFirstGame", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt("BestScore", bestScore)
        editor.commit()
    }

    private fun reset() {
        score = 0
        level = 1
        bestScore = loadBestScore()
        racingCar!!.setSpeed(INIT_SPEED)
        racingCar!!.playState = PlayState.Ready
        tvScore!!.text = score.toString()
        tvBest!!.text = bestScore.toString()
    }

    private fun restart() {
        reset()
        racingCar!!.playState = PlayState.Restart
        setTvNotify(R.string.restart)
        prepare()
    }

    private fun prepare() {
        val state = racingCar!!.playState
        var resource = R.drawable.ic_play
        if (state === PlayState.Pause) {
            resource = R.drawable.ic_pause
        } else if (state === PlayState.Restart) {
            resource = R.drawable.ic_retry
        }
        ivCenter!!.setImageResource(resource)
        showLabelContainer()
    }

    private fun play() {
        if (racingCar!!.playState === PlayState.Collision
                || racingCar!!.playState === PlayState.Restart) {
            initialize()
            racingCar!!.reset()
            return
        }

        // Click on playing
        if (racingCar!!.playState === PlayState.Playing) {
            pause()
        } else {
            ivCenter!!.setImageResource(R.drawable.ic_pause)

            // Click on pause
            when {
                racingCar!!.playState === PlayState.Pause -> {
                    ivCenter!!.setImageResource(R.drawable.ic_play)
                    racingCar!!.resume()
                    hideLabelContainer()
                }
                racingCar!!.playState === PlayState.LevelUp -> {
                    racingCar!!.resume()
                    hideLabelContainer()
                }
                else -> {
                    hideLabelContainer()
                    racingCar!!.play(racingHandler)
                }
            }
        }
    }

    private fun pause() {
        Log.d("RacingCar", "pause(): ")
        ivCenter!!.setImageResource(R.drawable.ic_pause)
        racingCar!!.pause()
    }

    private fun canResume(): Boolean {
        val state = racingCar!!.playState
        return if (state === PlayState.Pause || state === PlayState.Ready || state === PlayState.Restart) {
            true
        } else false
    }

    private fun resume() {
        hideLabelContainer()
        play()
    }

    private fun setTvNotify(stringId: Int) {
        tvNotify!!.setText(stringId)
    }

    private fun collision(achieveBest: Boolean) {
        if (achieveBest) {
            setTvNotify(R.string.best_ranking)
        } else {
            setTvNotify(R.string.try_again)
        }
        contLabel!!.visibility = View.VISIBLE
        contLabel!!.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
        ivCenter!!.setImageResource(R.drawable.ic_retry)
    }

    private fun showLabelContainer() {
        Log.d("RacingCar", "showLabelContainer: ")
        contLabel!!.visibility = View.VISIBLE
        if (contLabel!!.animation != null) {
            contLabel!!.animation.cancel()
        }
        contLabel!!.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
    }

    private fun hideLabelContainer() {
        Log.d("RacingCar", "hideLabelContainer: ")
        val anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        anim.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                contLabel!!.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        contLabel!!.startAnimation(anim)
    }

    private fun moveCarTo(direction: Int) {
        racingCar!!.moveCarTo(direction)
    }

    private fun moveCar() {
        racingCar!!.move()
    }

    companion object {
        private const val INIT_SPEED = 8
    }
}