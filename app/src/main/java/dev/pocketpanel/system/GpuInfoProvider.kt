package dev.pocketpanel.system

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.opengl.GLES20

/**
 * Reads GPU renderer, vendor and OpenGL ES version through the documented
 * EGL/OpenGL ES APIs.
 *
 * A tiny off-screen context is created once, the strings are read, and the
 * context is released immediately. Failures are cached so an unsupported
 * device does not retry on every monitoring tick.
 */
class GpuInfoProvider {

    private var attempted = false
    private var cached: GpuInfo? = null

    fun info(): GpuInfo? {
        if (attempted) return cached
        attempted = true
        cached = read()
        return cached
    }

    private fun read(): GpuInfo? {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return null
        if (!EGL14.eglInitialize(display, intArrayOf(0, 0), 0, intArrayOf(0, 0), 0)) {
            return null
        }

        var context: EGLContext = EGL14.EGL_NO_CONTEXT
        var surface: EGLSurface = EGL14.EGL_NO_SURFACE
        try {
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            val configAttributes = intArrayOf(
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE,
            )
            if (!EGL14.eglChooseConfig(
                    display,
                    configAttributes,
                    0,
                    configs,
                    0,
                    1,
                    configCount,
                    0,
                ) || configCount[0] == 0
            ) {
                return null
            }
            val config = configs[0] ?: return null

            context = EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
            if (context == EGL14.EGL_NO_CONTEXT) return null

            surface = EGL14.eglCreatePbufferSurface(
                display,
                config,
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
                0,
            )
            if (surface == EGL14.EGL_NO_SURFACE) return null
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return null

            val renderer = GLES20.glGetString(GLES20.GL_RENDERER)?.clean()
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR)?.clean()
            val version = GLES20.glGetString(GLES20.GL_VERSION)?.clean()
            return if (renderer == null && vendor == null && version == null) {
                null
            } else {
                GpuInfo(renderer, vendor, version)
            }
        } catch (_: Exception) {
            return null
        } finally {
            runCatching {
                EGL14.eglMakeCurrent(
                    display,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
            }
            if (surface != EGL14.EGL_NO_SURFACE) {
                runCatching { EGL14.eglDestroySurface(display, surface) }
            }
            if (context != EGL14.EGL_NO_CONTEXT) {
                runCatching { EGL14.eglDestroyContext(display, context) }
            }
            runCatching { EGL14.eglTerminate(display) }
        }
    }

    private fun String.clean(): String? = trim().ifEmpty { null }
}
