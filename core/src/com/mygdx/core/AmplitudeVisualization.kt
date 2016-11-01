package com.mygdx.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.sun.media.sound.FFT
import java.io.File
import javax.sound.sampled.AudioSystem

class AmplitudeVisualization {
    val modelBatch = ModelBatch()
    val camera: PerspectiveCamera
    val cameraController: CameraInputController
    val environment: Environment
    val viewport = IntRect(0, 0, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT)

    val sphere = ModelInstance(assets.getModel("sphere.g3db"))
    val mesh = sphere.nodes[0].parts[0].meshPart.mesh
    val originalVertices = FloatArray(mesh.numVertices * 6)

    val music = assets.getMusic("music.wav")
    val amplitude: Array<IntArray>
    val amplitudeLimit = 19000
    val totalLength: Float
    val gridWidth = 24
    val gridHeight = 13
    val grid = Array(gridWidth + 1, { i -> IntArray(gridHeight + 1, { j -> 0 }) })
    val verticesToGrid = Array(mesh.numVertices, { j -> IntVector2() })

    val lerp = 0.4f

    var color = Color.WHITE.cpy()

    init {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Texture.setAssetManager(assets)

        if (Gdx.graphics.isFullscreen) {
            val width = Gdx.graphics.width
            val height = Gdx.graphics.height
            val aspectRatio = width.toFloat() / height.toFloat()
            val scale: Float
            val crop = Vector2()

            if (aspectRatio > Constants.ASPECT_RATIO) {
                scale = height.toFloat() / Constants.VIEWPORT_HEIGHT.toFloat()
                crop.x = (width - Constants.VIEWPORT_WIDTH * scale) / 2f
            } else if (aspectRatio < Constants.ASPECT_RATIO) {
                scale = width.toFloat() / Constants.VIEWPORT_WIDTH.toFloat()
                crop.y = (height - Constants.VIEWPORT_HEIGHT * scale) / 2f;
            } else {
                scale = width.toFloat() / Constants.VIEWPORT_WIDTH.toFloat()
            }

            viewport.x = crop.x.toInt()
            viewport.y = crop.y.toInt()
            viewport.w = (Constants.VIEWPORT_WIDTH * scale).toInt()
            viewport.h = (Constants.VIEWPORT_WIDTH * scale).toInt()
        }

        camera = PerspectiveCamera(67f, Constants.VIEWPORT_WIDTH.toFloat(), Constants.VIEWPORT_HEIGHT.toFloat())
        camera.position.set(30f, 0f, 0f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 1f;
        camera.far = 300f;
        camera.update()

        cameraController = CameraInputController(camera)
        Gdx.input.inputProcessor = cameraController

        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1.0f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        mesh.getVertices(originalVertices)

        val musicFile = File("music.wav")
        val audioStream = AudioSystem.getAudioInputStream(musicFile)
        val bytes = ByteArray((audioStream.frameLength * audioStream.format.frameSize).toInt())
        audioStream.read(bytes)
        amplitude = getUnscaledAmplitude(bytes, 1)

        totalLength = audioStream.frameLength / audioStream.format.frameRate;

        var xMax = 0f
        var xMin = 0f
        var yMax = 0f
        var yMin = 0f
        var i = 0
        while (i < originalVertices.size) {
            if (originalVertices[i] > xMax) {
                xMax = originalVertices[i]
            }
            if (originalVertices[i] < xMin) {
                xMin = originalVertices[i]
            }
            if (originalVertices[i + 1] > yMax) {
                yMax = originalVertices[i + 1]
            }
            if (originalVertices[i + 1] < yMin) {
                yMin = originalVertices[i + 1]
            }
            i += 6
        }

        val xDiff = xMax - xMin
        val yDiff = yMax - yMin
        i = 0
        while (i < originalVertices.size) {
            verticesToGrid[i / 6].x = ((originalVertices[i] - xMin) / xDiff * gridWidth).toInt()
            verticesToGrid[i / 6].y = ((originalVertices[i + 1] - yMin) / yDiff * gridHeight).toInt()
            i += 6
        }

        randomizeGrid()
    }

    fun dispose() {
        modelBatch.dispose()
    }

    fun getUnscaledAmplitude(array: ByteArray, channels: Int): Array<IntArray> {
        val amplitude = Array(channels, { i -> IntArray(array.size / (2 * channels)) })

        var index = 0
        var audioByte = 0
        while (audioByte < array.size) {
            for (channel in 0..channels - 1) {
                val low = array[audioByte].toInt()
                audioByte++
                val high = array[audioByte].toInt()
                audioByte++
                val sample = (high shl 8) + (low and 0x00ff)

                amplitude[channel][index] = sample
            }
            index++
        }

        return amplitude
    }

    fun randomizeGrid() {
        for (x in 0..gridWidth) {
            for (y in 0..gridHeight) {
                val k = MathUtils.random(1, 5)
                grid[x][y] = MathUtils.random(k)
            }
        }
    }

    var playing = false
    val vertices = FloatArray(mesh.numVertices * 6)
    var xMax = 0f
    var yMax = 0f
    var zMax = 0f
    fun update(deltaTime: Float) {
        // Start playing in first update to stay in sync
        if (!playing) {
            music.play()
            playing = true
        }

        randomizeGrid()

        var maxAmplitude = 0
        val low = (amplitude[0].size * music.position / totalLength).toInt()
        val high = (low + amplitude[0].size * deltaTime / totalLength).toInt()
        for (i in low..high) {
            if (amplitude[0][i] > maxAmplitude) {
                maxAmplitude = amplitude[0][i]
            }
        }

        val modifier = maxAmplitude * 20f / amplitudeLimit + 1f

        mesh.getVertices(vertices)

        var xSum = 0f
        var ySum = 0f
        var zSum = 0f
        var i = 0
        while (i < vertices.size) {
            if (grid[verticesToGrid[i / 6].x][verticesToGrid[i / 6].y] == 0) {
                for (m in 0..2) {
                    vertices[i + m] = MathUtils.lerp(vertices[i + m], originalVertices[i + m] * modifier, lerp)
                }
                xSum += Math.abs(vertices[i])
                ySum += Math.abs(vertices[i + 1])
                zSum += Math.abs(vertices[i + 2])
            }
            i += 6
        }
        if (xSum > xMax) {
            xMax = xSum
        }
        if (ySum > yMax) {
            yMax = ySum
        }
        if (zSum > zMax) {
            zMax = zSum
        }

        color.r += Math.signum(xSum - xMax / 3f) * 0.01f
        color.g += Math.signum(ySum - yMax / 3f) * 0.01f
        color.b += Math.signum(zSum - zMax / 3f) * 0.01f
        if (color.r < 0f) {
            color.r = 0f
        } else if (color.r > 1f) {
            color.r = 1f
        }
        if (color.g < 0f) {
            color.g = 0f
        } else if (color.g > 1f) {
            color.g = 1f
        }
        if (color.b < 0f) {
            color.b = 0f
        } else if (color.b > 1f) {
            color.b = 1f
        }
        sphere.nodes[0].parts[0].material.set(ColorAttribute(ColorAttribute.Diffuse, color))

        mesh.setVertices(vertices)

        cameraController.update()

        Gdx.gl.glViewport(viewport.x.toInt(), viewport.y.toInt(), viewport.w, viewport.h)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        modelBatch.render(sphere, environment)
        modelBatch.end()
    }
}