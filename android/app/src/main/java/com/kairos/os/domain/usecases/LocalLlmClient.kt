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
            try {
                // Check sandbox directory first, then fallback to public /data/local/tmp/
                var modelFile = File(context.filesDir, "gemma.litertlm")
                if (!modelFile.exists() || !modelFile.canRead()) {
                    modelFile = File("/data/local/tmp/llm/gemma.litertlm")
                }

                if (modelFile.exists() && modelFile.canRead()) {
                    val modelPath = modelFile.absolutePath
                    Log.i(TAG, "🔍 Gemma On-Device model detected at: $modelPath")
                    Log.i(TAG, "Initializing LiteRT-LM Engine...")
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.path
                    )
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    Log.i(TAG, "✅ LiteRT-LM Engine successfully initialized with Gemma model.")
                    engine = newEngine
                } else {
                    Log.w(TAG, "❌ Gemma model file NOT found or unreadable at expected paths (/data/data/com.kairos.os/files/gemma.litertlm or /data/local/tmp/llm/gemma.litertlm). Local AI features will run in fallback rule-based mode.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LiteRT-LM Engine", e)
            }
        }
        return engine
    }
}
