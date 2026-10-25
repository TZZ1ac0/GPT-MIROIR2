package com.tzz1ac0.miroir.camera

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender
import com.pedro.encoder.utils.gl.GlUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Applies zoom and pan in RootEncoder's shared OpenGL render path, so preview and encoded
 * frames receive the same crop. UVs are transformed into a valid crop window rather than
 * shifted and clamped at the edges, which caused stretched stripes.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class PanFilterRender : BaseFilterRender() {
    private val vertices = floatArrayOf(
        -1f, -1f, 0f, 0f, 0f,
         1f, -1f, 0f, 1f, 0f,
        -1f,  1f, 0f, 0f, 1f,
         1f,  1f, 0f, 1f, 1f
    ).let {
        ByteBuffer.allocateDirect(it.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply { put(it); position(0) }
    }

    private var program = -1
    private var positionHandle = -1
    private var textureHandle = -1
    private var mvpMatrixHandle = -1
    private var stMatrixHandle = -1
    private var samplerHandle = -1
    private var panHandle = -1
    private var zoomHandle = -1

    @Volatile private var zoom = 1f
    @Volatile private var panX = 0f
    @Volatile private var panY = 0f

    fun setTransform(zoom: Float, x: Float, y: Float) {
        val safeZoom = zoom.coerceIn(1f, 8f)
        val maxPan = 0.5f * (1f - 1f / safeZoom)
        this.zoom = safeZoom
        panX = x.coerceIn(-maxPan, maxPan)
        panY = y.coerceIn(-maxPan, maxPan)
    }

    override fun initGlFilter(context: Context) {
        val vertexShader = """
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            uniform mat4 uMVPMatrix;
            uniform mat4 uSTMatrix;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = (uSTMatrix * aTextureCoord).xy;
            }
        """.trimIndent()
        val fragmentShader = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D uSampler;
            uniform vec2 uPan;
            uniform float uZoom;
            void main() {
                vec2 uv = (vTextureCoord - vec2(0.5)) / uZoom + vec2(0.5) + uPan;
                // Keep numerical round-off inside the crop's valid texture domain.
                uv = clamp(uv, vec2(0.001), vec2(0.999));
                gl_FragColor = texture2D(uSampler, uv);
            }
        """.trimIndent()
        program = GlUtil.createProgram(vertexShader, fragmentShader)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        stMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        samplerHandle = GLES20.glGetUniformLocation(program, "uSampler")
        panHandle = GLES20.glGetUniformLocation(program, "uPan")
        zoomHandle = GLES20.glGetUniformLocation(program, "uZoom")
        Matrix.setIdentityM(MVPMatrix, 0)
        Matrix.setIdentityM(STMatrix, 0)
    }

    override fun drawFilter() {
        GLES20.glUseProgram(program)

        vertices.position(SQUARE_VERTEX_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false, SQUARE_VERTEX_DATA_STRIDE_BYTES, vertices
        )
        GLES20.glEnableVertexAttribArray(positionHandle)

        vertices.position(SQUARE_VERTEX_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(
            textureHandle, 2, GLES20.GL_FLOAT, false, SQUARE_VERTEX_DATA_STRIDE_BYTES, vertices
        )
        GLES20.glEnableVertexAttribArray(textureHandle)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, MVPMatrix, 0)
        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, STMatrix, 0)
        GLES20.glUniform1i(samplerHandle, 0)
        GLES20.glUniform2f(panHandle, panX, panY)
        GLES20.glUniform1f(zoomHandle, zoom)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previousTexId)
    }

    override fun disableResources() {
        GlUtil.disableResources(textureHandle, positionHandle)
    }

    override fun release() {
        GLES20.glDeleteProgram(program)
    }
}
