package com.mygdx.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

class GridToModel {
    enum class STATE {
        GRID, MODEL
    }

    var state = STATE.GRID

    val spriteBatch = SpriteBatch()
    val modelBatch = ModelBatch()
    val shapeRenderer = ShapeRenderer()
    val camera: PerspectiveCamera
    val cameraController: LimitedCameraController
    val environment: Environment
    val viewport = IntRect(0, 0, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT)

    val sphereInstance = ModelInstance(assets.getModel("sphere.g3db"))
    val mesh = sphereInstance.nodes[0].parts[0].meshPart.mesh
    val originalVertices = FloatArray(mesh.numVertices * 6)
    val vertices = FloatArray(mesh.numVertices * 6)
    val gridWidth = 24
    val gridHeight = 13
    val grid = Array(gridWidth + 1, { i -> IntArray(gridHeight + 1, { j -> 0 }) })
    val verticesToGrid = Array(mesh.numVertices, { j -> IntVector2() })

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

            spriteBatch.projectionMatrix.setToOrtho2D(0f, 0f, viewport.w.toFloat(), viewport.w.toFloat())
        }

        camera = PerspectiveCamera(67f, viewport.w.toFloat(), viewport.h.toFloat())
        camera.position.set(5f, 0f, 0f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 1f;
        camera.far = 300f;
        camera.update()

        cameraController = LimitedCameraController(camera)
        Gdx.input.inputProcessor = cameraController

        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1.0f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        mesh.getVertices(originalVertices)

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
        while (i < vertices.size) {
            verticesToGrid[i / 6].x = ((originalVertices[i] - xMin) / xDiff * gridWidth).toInt()
            verticesToGrid[i / 6].y = ((originalVertices[i + 1] - yMin) / yDiff * gridHeight).toInt()
            i += 6
        }
    }

    fun dispose() {
        spriteBatch.dispose()
        modelBatch.dispose()
        shapeRenderer.dispose()
    }

    fun updateModel(deltaTime: Float) {
        cameraController.update()

        // Reset
        if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) && !Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = STATE.GRID

            mesh.setVertices(originalVertices)

            for (x in 0..gridWidth) {
                for (y in 0..gridHeight) {
                    grid[x][y] = 0
                }
            }
        }

        Gdx.gl.glViewport(viewport.x.toInt(), viewport.y.toInt(), viewport.w, viewport.h)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        modelBatch.render(sphereInstance, environment)
        modelBatch.end()
    }

    fun updateGrid(deltaTime: Float) {

        val touchX = ((Gdx.input.x - 40f) / 50f).toInt()
        val touchY = ((Constants.VIEWPORT_HEIGHT - Gdx.input.y - 35f) / 50f).toInt()
        if (touchX >= 0 && touchY >= 0 && touchX <= gridWidth && touchY <= gridHeight) {
            if (grid[touchX][touchY] < 20) {
                if (Gdx.input.isTouched) {
                    grid[touchX][touchY] += 6
                } else {
                    grid[touchX][touchY] += 2
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) && !Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = STATE.MODEL

            mesh.getVertices(vertices)

            var i = 0
            while (i < vertices.size) {
                if (grid[verticesToGrid[i / 6].x][verticesToGrid[i / 6].y] > 0) {
                    for (j in 0..2) {
                        vertices[i + j] *= 1f + grid[verticesToGrid[i / 6].x][verticesToGrid[i / 6].y] * 0.05f
                    }
                }
                i += 6
            }

            mesh.setVertices(vertices)

            var sum = 0
            for (x in 0..gridWidth) {
                for (y in 0..gridHeight) {
                    sum += grid[x][y]
                }
            }
            val max = Math.sqrt(sum / (gridWidth * gridHeight * 20).toDouble()).toFloat()
            val color = Color(MathUtils.random(max / 2, max).toFloat(), MathUtils.random(max / 2, max).toFloat(), MathUtils.random(max / 2, max).toFloat(), 1f)
            sphereInstance.nodes[0].parts[0].material.set(ColorAttribute(ColorAttribute.Diffuse, color))
        }

        Gdx.gl.glViewport(viewport.x.toInt(), viewport.y.toInt(), viewport.w, viewport.h)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        shapeRenderer.color = Color.ORANGE
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (x in 0..gridWidth) {
            for (y in 0..gridHeight) {
                if (grid[x][y] > 0) {
                    shapeRenderer.setColor(Color.ORANGE.r, Color.ORANGE.g, Color.ORANGE.b + grid[x][y] * 0.05f, 1f)
                    shapeRenderer.rect(40f + x * 50f, 35f + y * 50f, 10f, 10f)
                    shapeRenderer.color = Color.ORANGE
                } else {
                    shapeRenderer.rect(40f + x * 50f, 35f + y * 50f, 10f, 10f)
                }
            }
        }
        shapeRenderer.end()
    }

    fun update(deltaTime: Float) {
        when (state) {
            STATE.MODEL -> updateModel(deltaTime)
            STATE.GRID -> updateGrid(deltaTime)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }
    }
}
