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
    private val MODEL_PATH = "/data/local/tmp/llm/gemma.litertlm"
    
    private var engine: Engine? = null

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
                
                // 1. Try GPU backend first
                try {
                    Log.i(TAG, "Initializing LiteRT-LM Engine with GPU backend...")
                    val gpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.path
                    )
                    val newEngine = Engine(gpuConfig)
                    newEngine.initialize()
                    Log.i(TAG, "✅ LiteRT-LM Engine initialized with GPU backend.")
                    engine = newEngine
                    return engine
                } catch (gpuError: Exception) {
                    Log.w(TAG, "⚠️ LiteRT-LM GPU backend initialization failed (${gpuError.message}). Falling back to CPU backend...")
                }

                // 2. Fallback to CPU backend
                try {
                    val cpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.path
                    )
                    val newEngine = Engine(cpuConfig)
                    newEngine.initialize()
                    Log.i(TAG, "✅ LiteRT-LM Engine initialized with CPU backend.")
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
}
