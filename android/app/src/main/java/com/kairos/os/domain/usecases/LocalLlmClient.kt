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
    private var audioEnabled: Boolean = false

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

    private fun tryInitializeEngine(config: EngineConfig, label: String, enableAudio: Boolean): Engine? {
        return try {
            val newEngine = Engine(config)
            newEngine.initialize()
            audioEnabled = enableAudio
            Log.i(TAG, "✅ LiteRT-LM Engine initialized successfully ($label, audio=$enableAudio)")
            newEngine
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ LiteRT-LM initialization failed ($label): ${e.message}")
            null
        }
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
                val cacheDir = context.cacheDir.path

                if (hasOpenCl) {
                    engine = tryInitializeEngine(
                        EngineConfig(
                            modelPath = modelPath,
                            backend = Backend.GPU(),
                            audioBackend = Backend.CPU(),
                            cacheDir = cacheDir
                        ),
                        label = "GPU + audio",
                        enableAudio = true
                    )
                    if (engine != null) return engine
                }

                engine = tryInitializeEngine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        audioBackend = Backend.CPU(),
                        cacheDir = cacheDir
                    ),
                    label = "CPU + audio",
                    enableAudio = true
                )
                if (engine != null) return engine

                engine = tryInitializeEngine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = cacheDir
                    ),
                    label = "CPU text-only",
                    enableAudio = false
                )
            } else {
                Log.w(TAG, "❌ Gemma model file NOT found or unreadable at expected paths. Intent gate will use rule-based intent evaluation.")
            }
        }
        return engine
    }

    @Synchronized
    fun isAudioReady(): Boolean {
        getEngine()
        return audioEnabled
    }

    @Synchronized
    fun resetEngine() {
        try {
            engine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing engine during reset: ${e.message}")
        }
        engine = null
        audioEnabled = false
    }
}
