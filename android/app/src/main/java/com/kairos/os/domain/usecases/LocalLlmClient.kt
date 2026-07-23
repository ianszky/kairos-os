package com.kairos.os.domain.usecases

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "LocalLlmClient"
    
    private var engine: Engine? = null

    private fun preloadOpenClDriver(): Boolean {
        val openClPaths = listOf(
            "/vendor/lib64/libOpenCL.so",
            "/system/vendor/lib64/libOpenCL.so",
            "/vendor/lib64/egl/libGLES_mali.so",
            "/vendor/lib64/libOpenCL.3.0.so",
            "/system/lib64/libOpenCL.so"
        )
        for (path in openClPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    System.load(path)
                    Log.i(TAG, "⚡ Successfully pre-loaded Qualcomm/Vendor OpenCL driver from: $path")
                    return true
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Notice: Could not load OpenCL driver from $path: ${e.message}")
            }
        }
        return false
    }

    @Synchronized
    fun getEngine(): Engine? {
        if (engine == null) {
            val candidatePaths = listOf(
                File(context.filesDir, "gemma.litertlm"),
                File(context.filesDir, "gemma-4-e2b.litertlm"),
                File(context.filesDir, "models/gemma-4-e2b.litertlm"),
                File(context.filesDir, "models/gemma.litertlm"),
                File("/data/local/tmp/llm/gemma.litertlm"),
                File("/data/local/tmp/llm/gemma-4-e2b.litertlm"),
                File("/data/local/tmp/gemma.litertlm")
            )

            val modelFile = candidatePaths.firstOrNull { it.exists() && it.canRead() }

            if (modelFile != null) {
                val modelPath = modelFile.absolutePath
                Log.i(TAG, "🔍 Gemma On-Device model detected at: $modelPath")
                
                val hasOpenCl = preloadOpenClDriver()

                // 1. Attempt GPU acceleration if OpenCL driver preloaded or available
                if (hasOpenCl) {
                    try {
                        Log.i(TAG, "Initializing LiteRT-LM Engine with GPU backend for Snapdragon Adreno...")
                        val gpuConfig = EngineConfig(
                            modelPath = modelPath,
                            backend = Backend.GPU(),
                            cacheDir = context.cacheDir.path
                        )
                        val newEngine = Engine(gpuConfig)
                        newEngine.initialize()
                        Log.i(TAG, "🚀 LiteRT-LM Engine initialized successfully with GPU backend!")
                        engine = newEngine
                        return engine
                    } catch (gpuError: Exception) {
                        Log.w(TAG, "⚠️ LiteRT-LM GPU backend initialization failed (${gpuError.message}). Falling back to CPU backend...")
                    }
                }

                // 2. CPU backend fallback
                try {
                    Log.i(TAG, "Initializing LiteRT-LM Engine with CPU backend...")
                    val cpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.path
                    )
                    val newEngine = Engine(cpuConfig)
                    newEngine.initialize()
                    Log.i(TAG, "✅ LiteRT-LM Engine initialized successfully with CPU backend.")
                    engine = newEngine
                    return engine
                } catch (cpuError: Exception) {
                    Log.e(TAG, "❌ LiteRT-LM CPU backend initialization failed", cpuError)
                }
            } else {
                Log.w(TAG, "❌ Gemma model file NOT found or unreadable at expected paths. Intent gate will use rule-based intent evaluation.")
            }
        }
        return engine
    }

    @Synchronized
    fun resetEngine() {
        try {
            engine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing engine during reset: ${e.message}")
        }
        engine = null
    }
}
