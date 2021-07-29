package com.example.spenremote.racingcar

import android.content.Context
import android.graphics.RectF
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.*

class RacingCar : View {
    enum class PlayState {
        Ready, Playing, Pause, LevelUp, Collision, Restart
    }

    private var handler1: Handler? = null
    private var state: PlayState? = null
    private var prevState: PlayState? = null
    private var boardWidth = 0
    private var viewHeight = 0
    private var blockSize = 0
    private var speed = 0
    var currentPos = POS_RIGHT
    private var paint: Paint? = null
    private var random: Random? = null
    private val walls: ArrayList<RectF>? = null
    private var maps: ArrayList<RectF>? = null
    private var obstacles: ArrayList<Truck>? = null
    private var myself: Truck? = null
    private var mapBitmap: Bitmap? = null

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val height = bottom - top
        if (bottom - top > 0) {
            val blockSize = (height - SPACING * (VERTICAL_COUNT + 1)) / VERTICAL_COUNT
            val w = blockSize * HORIZONTAL_COUNT + SPACING * (HORIZONTAL_COUNT - 1)
            val h = blockSize * VERTICAL_COUNT + SPACING * (VERTICAL_COUNT + 1)
            val viewWidth = right - left
            initialize(w, h, blockSize)
        }
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(TAG, "onDraw(), state = $state")
        super.onDraw(canvas)
        drawMaps(canvas)
        if (state == PlayState.Playing || state == PlayState.Collision) {
            drawObastacles(canvas)
        }
        if (myself != null && (state == PlayState.Playing || state == PlayState.Collision)) {
            myself!!.draw(canvas, state == PlayState.Collision)
        }
        if (handler1 != null && state == PlayState.Playing) {
            handler1!!.sendEmptyMessage(MSG_SCORE)
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            move()
        }
        return super.onTouchEvent(event)
    }

    var playState: PlayState?
        get() = state
        set(state) {
            prevState = state
            this.state = state
        }

    fun setSpeed(speed: Int) {
        this.speed = speed
    }

    fun play(handler: Handler?) {
        this.handler1 = handler
        prevState = state
        state = PlayState.Playing
        moveLeft()
    }

    fun resume() {
        prevState = state
        state = PlayState.Playing
        if (prevState != PlayState.Pause) {
            moveLeft()
        }
    }

    fun pause() {
        prevState = state
        state = PlayState.Pause
        if (handler1 != null) {
            handler1!!.sendEmptyMessage(MSG_PAUSE)
        }
    }

    fun reset() {
        prevState = state
        state = PlayState.Ready
        createObstacles()
    }

    private fun initialize(width: Int, height: Int, blockSize: Int) {
        if (myself != null) {
            return
        }
        prevState = state
        state = PlayState.Ready
        boardWidth = width
        viewHeight = height
        this.blockSize = blockSize
        setProperties()

        //createWall();
        createMap()
        createObstacles()
        val drawable = resources.getDrawable(R.drawable.car_main, null)
        val bitmap = (drawable as BitmapDrawable).bitmap
        myself = Truck(bitmap)
        myself!!.x = 200
        myself!!.y = viewHeight - myself!!.height
        val drawableMap = resources.getDrawable(R.drawable.background_line, null)
        val tmpBitmap = (drawableMap as BitmapDrawable).bitmap
        mapBitmap = Bitmap.createScaledBitmap(tmpBitmap, 1060, tmpBitmap.height, false)
    }

    private fun createMap() {
        maps = ArrayList()
        for (i in 0..1) {
            val left = RectF(0.0f, 0.0f, 0.0f, 0.0f)
            left.left = 0f
            left.right = boardWidth.toFloat()
            left.top = (-viewHeight + i * viewHeight).toFloat()
            left.bottom = left.top + viewHeight
            maps!!.add(left)
        }
    }

    private fun setProperties() {
        paint = Paint()
        paint!!.isAntiAlias = true
        random = Random()
    }

    private fun drawMaps(canvas: Canvas) {
        if (state == PlayState.Playing) {
            if (maps != null) {
                for (r in maps!!) {
                    r.top = r.top + 50
                    if (r.top > viewHeight) {
                        r.top = -viewHeight.toFloat()
                        r.bottom = r.top + viewHeight
                    }
                    canvas.drawBitmap(mapBitmap!!, 0f, r.top, null)
                }
            }
        } else {
            canvas.drawBitmap(mapBitmap!!, 0f, 0f, null)
        }
    }

    private fun createObstacles() {
        if (obstacles == null) {
            obstacles = ArrayList()
        } else {
            obstacles!!.clear()
        }
        val carHeight = blockSize * 5 + SPACING * 3
        var startOffset = -carHeight
        var count = speed * MAX_COL_COUNT
        if (speed >= 20) {
            count = count * 2
        } else if (speed >= 30) {
            count = count * 3
        } else if (speed >= 40) {
            count = count * 4
        }
        for (i in 0 until count) {
            val r = random!!.nextInt(MAX_COL_COUNT)
            val resourceId = obstaclesResource
            val drawable = resources.getDrawable(resourceId)
            val bitmap = (drawable as BitmapDrawable).bitmap
            val obstacle = Truck(
                    bitmap,
                    getLeftPositionX(r), startOffset)
            obstacles!!.add(obstacle)
            startOffset = startOffset - (carHeight + SPACING) * 2
        }
        obstacles!![obstacles!!.size - 1].isLast = true
    }

    private val obstaclesResource: Int
        private get() {
            val obstaclesSize = 3
            var rand = random!!.nextInt(obstaclesSize)
            when (rand) {
                0 -> rand = R.drawable.car_sub1
                1 -> rand = R.drawable.car_sub2
                2 -> rand = R.drawable.car_sub3
            }
            return rand
        }

    private fun drawObastacles(c: Canvas) {
        if (obstacles != null) {
            var isComplete = false
            val size = obstacles!!.size
            for (i in 0 until size) {
                val obstacle = obstacles!![i]
                if (state == PlayState.Playing) {
                    obstacle.moveDown(speed)
                }
                obstacle.draw(c)
                if (state == PlayState.Playing) {
                    if (isCollision(obstacle)) {
                        prevState = state
                        state = PlayState.Collision
                        if (handler1 != null) {
                            handler1!!.sendEmptyMessage(MSG_COLLISION)
                        }
                    }
                    if (obstacle.isLast && obstacle.y >= viewHeight + obstacle.height + blockSize) {
                        isComplete = true
                    }
                }
            }
            if (isComplete) {
                prevState = state
                state = PlayState.LevelUp
                createObstacles()
                if (handler1 != null) {
                    handler1!!.sendEmptyMessage(MSG_COMPLETE)
                }
            }
        }
    }

    private fun isCollision(obstacle: Truck): Boolean {
        return if (myself == null) {
            false
        } else myself!!.getBoundary()!!.intersect(obstacle.getBoundary()!!)
    }

    private fun getLeftPositionX(r: Int): Int {
        Log.d("aozo", "test Count r :$r")
        var posX = 180
        if (r == 1) {
            posX = 680
        }
        return posX
    }

    fun moveLeft() {
        if (state != PlayState.Playing) {
            return
        }
        if (myself != null) {
            myself!!.setPosition(200)
        }
        currentPos = POS_LEFT
    }

    fun moveRight() {
        if (state != PlayState.Playing) {
            return
        }
        if (myself != null) {
            myself!!.setPosition(700)
        }
        currentPos = POS_RIGHT
    }

    fun moveCarTo(direction: Int) {
        if (direction == POS_LEFT) {
            moveLeft()
        } else {
            moveRight()
        }
    }

    fun move() {
        val state = playState
        if (state == PlayState.Playing) {
            if (currentPos == POS_LEFT) {
                moveRight()
            } else {
                moveLeft()
            }
        }
    }

    companion object {
        const val TAG = "RacingCar"

        //---------------------------------------------------------------------------------------------
        // fields
        //---------------------------------------------------------------------------------------------
        const val SPACING = 2
        const val MAX_COL_COUNT = 2
        const val VERTICAL_COUNT = 20
        const val HORIZONTAL_COUNT = MAX_COL_COUNT * 3 + 2 + 2
        const val MSG_SCORE = 1000
        const val MSG_COLLISION = 2000
        const val MSG_COMPLETE = 3000
        const val MSG_PAUSE = 4000
        const val POS_LEFT = 0
        const val POS_RIGHT = 1
    }
}