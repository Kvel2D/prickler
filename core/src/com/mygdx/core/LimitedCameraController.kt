package com.mygdx.core

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController

class LimitedCameraController(camera: PerspectiveCamera) : CameraInputController(camera) {
    override fun zoom(amount: Float): Boolean {
        val distance = camera.position.x * camera.position.x + camera.position.y * camera.position.y + camera.position.z * camera.position.z
        // limit camera zoom to distances between 3 and 15 to origin
        if ((amount > 0f && distance > 9f) || (amount < 0f && distance < 225f)) {
            return super.zoom(amount)
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }
}
