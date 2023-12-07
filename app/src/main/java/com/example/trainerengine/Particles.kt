package com.example.trainerengine

import android.app.Activity
import com.plattysoft.leonids.ParticleSystem

const val PARTICLES_AMOUNT = 100
const val TIME_TO_LIVE = 10000L
const val SPEED_MIN = 0.2f
const val SPEED_MAX = 0.4f
const val ROTATION_SPEED_MIN = -144f
const val ROTATION_SPEED_MAX = 144f
const val GRAVITY_ACCELERATION = 0.00015f
val CONFETTI_DRAWABLES = listOf(
    R.drawable.confetti1,
    R.drawable.confetti2,
    R.drawable.confetti3
)
const val BOTTOM_RIGHT = R.id.session_viewer_confetti_bottom_right
const val BOTTOM_LEFT = R.id.session_viewer_confetti_bottom_left
const val BOTTOM_LEFT_ANGLE_MIN = 270
const val BOTTOM_LEFT_ANGLE_MAX = 310

fun confettiOnFinishSession(activity: Activity){
    for (confetti in CONFETTI_DRAWABLES) {
        ParticleSystem(activity, PARTICLES_AMOUNT, confetti, TIME_TO_LIVE)
            .setSpeedModuleAndAngleRange(SPEED_MIN, SPEED_MAX, BOTTOM_LEFT_ANGLE_MIN, BOTTOM_LEFT_ANGLE_MAX)
            .setRotationSpeedRange(ROTATION_SPEED_MIN, ROTATION_SPEED_MAX)
            .setAcceleration(GRAVITY_ACCELERATION, 90)
            .oneShot(activity.findViewById(BOTTOM_LEFT), PARTICLES_AMOUNT)
        ParticleSystem(activity, PARTICLES_AMOUNT, confetti, TIME_TO_LIVE)
            .setSpeedModuleAndAngleRange(SPEED_MIN, SPEED_MAX, 180- BOTTOM_LEFT_ANGLE_MIN, 180- BOTTOM_LEFT_ANGLE_MAX)
            .setRotationSpeedRange(ROTATION_SPEED_MIN, ROTATION_SPEED_MAX)
            .setAcceleration(GRAVITY_ACCELERATION, 90)
            .oneShot(activity.findViewById(BOTTOM_RIGHT), PARTICLES_AMOUNT)
    }
}