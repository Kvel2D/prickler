package com.mygdx.core

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import java.util.*

val tempVec2 = Vector2()

fun <T> MutableList<T>.shuffle() {
    var currentIndex = this.size

    while (0 !== currentIndex) {
        val randomIndex = (Math.random() * currentIndex).toInt()
        currentIndex -= 1

        val tmp = this[currentIndex]!!
        this[currentIndex] = this[randomIndex]!!
        this[randomIndex] = tmp
    }
}

fun FloatArray.scale(scale: Float) {
    for (i in this.indices)
        this[i] *= scale
}

fun Vector2.rotateAround(xOrigin: Float, yOrigin: Float, angle: Float) {
    val angleConverted = angle * Constants.DEGTORAD

    val cos = Math.cos(angleConverted.toDouble()).toFloat()
    val sin = Math.sin(angleConverted.toDouble()).toFloat()

    this.add(-xOrigin, -yOrigin)
    tempVec2.set(this)
    this.x = tempVec2.x * cos - tempVec2.y * sin
    this.y = tempVec2.x * sin + tempVec2.y * cos
    this.add(xOrigin, yOrigin)
}

fun Vector3.rotateAroundZ(xOrigin: Float, yOrigin: Float, angle: Float) {
    val angleConverted = angle * Constants.DEGTORAD

    val cos = Math.cos(angleConverted.toDouble()).toFloat()
    val sin = Math.sin(angleConverted.toDouble()).toFloat()

    this.add(-xOrigin, -yOrigin, 0f)
    tempVec2.set(this.x, this.y)
    this.x = tempVec2.x * cos - tempVec2.y * sin
    this.y = tempVec2.x * sin + tempVec2.y * cos
    this.add(xOrigin, yOrigin, 0f)
}

fun FloatArray.rotateAround(xOrigin: Float, yOrigin: Float, angle: Float) {
    var i = 0
    while (i < this.size) {
        tempVec2.set(this[i], this[i + 1])
        tempVec2.rotateAround(xOrigin, yOrigin, angle)
        this[i] = tempVec2.x
        this[i + 1] = tempVec2.y
        i += 2
    }
}

fun FloatArray.translate(xOrigin: Float, yOrigin: Float, angle: Float) {
    var i = 0
    while (i < this.size) {
        tempVec2.set(this[i], this[i + 1])
        tempVec2.rotateAround(0f, 0f, angle)
        this[i] = tempVec2.x + xOrigin
        this[i + 1] = tempVec2.y + yOrigin
        i += 2
    }
}

fun FloatArray.getRadiusSimple(): Float {
    val p1 = Vector2(this[0], this[1])
    val p2 = Vector2(this[2], this[3])
    val middlePoint = Vector2((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
    val radius = middlePoint.dst(0f, 0f)
    return radius
}

// Computes area for polygons that don't self-intersect
fun FloatArray.getArea(): Float {
    val triangulator = EarClippingTriangulator()

    var area = 0f
    val indices = triangulator.computeTriangles(this)

    var i = 0
    while (i < indices.size) {
        val i1 = indices.get(i) * 2
        val i2 = indices.get(i + 1) * 2
        val i3 = indices.get(i + 2) * 2

        area += GeometryUtils.triangleArea(
                this[i1 + 0], this[i1 + 1],
                this[i2 + 0], this[i2 + 1],
                this[i3 + 0], this[i3 + 1])
        i += 3
    }
    return area
}

fun FloatArray.getCentroid(): Vector2 {
    val off = Vector2(this[0], this[1])
    var twicearea = 0f
    var x = 0f
    var y = 0f
    var p1: Vector2
    var p2: Vector2
    var f: Float
    var i = 0
    var j = this.size / 2 - 1
    while (i < this.size / 2) {
        p1 = Vector2(this[i * 2], this[i * 2 + 1])
        p2 = Vector2(this[j * 2], this[j * 2 + 1])
        f = (p1.x - off.x) * (p2.y - off.y) - (p2.x - off.x) * (p1.y - off.y)
        twicearea += f
        x += (p1.x + p2.x - 2 * off.x) * f
        y += (p1.y + p2.y - 2 * off.y) * f
        j = i++
    }

    f = twicearea * 3

    return Vector2(x / f + off.x, y / f + off.y)
}

fun FileHandle.toStringList(): ArrayList<String> {
    val lines = arrayListOf<String>()
    val text = this.readString()

    val scanner = Scanner(text)
    while (scanner.hasNextLine()) {
        val line = scanner.nextLine()
        lines.add(line)
    }
    scanner.close()

    return lines
}

fun FileHandle.toVertices(): FloatArray {
    val linesBuffer = arrayListOf<Vector2>()
    val text = this.readString()

    val scanner = Scanner(text)
    while (scanner.hasNextLine()) {
        val line = scanner.nextLine()
        val spaceSeparator = line.indexOf(" ")

        val x = java.lang.Float.parseFloat(line.substring(0, spaceSeparator))
        val y = java.lang.Float.parseFloat(line.substring(spaceSeparator + 1))

        linesBuffer.add(Vector2(x, y))
    }
    scanner.close()

    val lines = FloatArray(linesBuffer.size * 2)

    for (i in 0..linesBuffer.size - 1) {
        lines[i * 2] = linesBuffer.get(i).x
        lines[i * 2 + 1] = linesBuffer.get(i).y
    }

    return lines
}

class IntRect(var x: Int, var y: Int, var w: Int, var h: Int)
class IntVector2(var x: Int = 0, var y: Int = 0)