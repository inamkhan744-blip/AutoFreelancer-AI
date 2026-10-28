package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface VoiceCommandState {
    object Idle : VoiceCommandState
    object Listening : VoiceCommandState
    data class Processing(val spokenText: String) : VoiceCommandState
    data class ActionExecuted(val commandTitle: String, val feedback: String) : VoiceCommandState
    data class Error(val message: String) : VoiceCommandState
}

class JarvisVoiceManager(
    private val context: Context,
    private val onApproveAllTasks: () -> Unit,
    private val onFindLeads: () -> Unit,
    private val onNavigateTo: (String) -> Unit
) {
    private val tag = "JarvisVoiceManager"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceCommandState>(VoiceCommandState.Idle)
    val voiceState: StateFlow<VoiceCommandState> = _voiceState.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isRecognitionAvailable) {
            _voiceState.value = VoiceCommandState.Error(
                "Speech recognizer service not available on this device. You can type commands directly into Jarvis."
            )
            return
        }

        try {
            stopListening()
            _voiceState.value = VoiceCommandState.Listening
            _lastRecognizedText.value = ""

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.i(tag, "Jarvis onReadyForSpeech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.i(tag, "Jarvis onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.i(tag, "Jarvis onEndOfSpeech")
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client recognition error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection required for speech"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No command heard. Try saying 'Approve all tasks' or 'Find more leads'."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Jarvis recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Try again."
                            else -> "Speech recognition error ($error)"
                        }
                        Log.w(tag, "Jarvis recognition error: $message")
                        _voiceState.value = VoiceCommandState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull().orEmpty()
                        _lastRecognizedText.value = spoken
                        processSpokenCommand(spoken)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            _lastRecognizedText.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognition: ${e.message}", e)
            _voiceState.value = VoiceCommandState.Error("Could not activate microphone: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
            if (_voiceState.value is VoiceCommandState.Listening) {
                _voiceState.value = VoiceCommandState.Idle
            }
        }
    }

    fun executeManualCommand(commandText: String) {
        _lastRecognizedText.value = commandText
        processSpokenCommand(commandText)
    }

    fun resetState() {
        _voiceState.value = VoiceCommandState.Idle
        _lastRecognizedText.value = ""
    }

    private fun processSpokenCommand(spoken: String) {
        _voiceState.value = VoiceCommandState.Processing(spoken)
        val normalized = spoken.lowercase(Locale.getDefault()).trim()

        when {
            // "Approve all tasks" or variations
            normalized.contains("approve all") ||
            normalized.contains("approve tasks") ||
            normalized.contains("execute tasks") ||
            normalized.contains("run all") -> {
                onApproveAllTasks()
                _voiceState.value = VoiceCommandState.ActionExecuted(
                    commandTitle = "Approve All Tasks",
                    feedback = "Executed: Jarvis triggered autonomous approval for all pending agency tasks."
                )
            }

            // "Find more leads" or variations
            normalized.contains("find more leads") ||
            normalized.contains("find leads") ||
            normalized.contains("scout leads") ||
            normalized.contains("search leads") ||
            normalized.contains("get leads") ||
            normalized.contains("lead scout") -> {
                onFindLeads()
                _voiceState.value = VoiceCommandState.ActionExecuted(
                    commandTitle = "Find More Leads",
                    feedback = "Executed: Jarvis launched live search discovery and Gemini pitch synthesizer."
                )
            }

            // Navigation commands
            normalized.contains("inbox") || normalized.contains("replies") -> {
                onNavigateTo("INBOX")
                _voiceState.value = VoiceCommandState.ActionExecuted(
                    commandTitle = "Open Inbox",
                    feedback = "Navigating to AI Closer Inbox..."
                )
            }

            normalized.contains("crm") || normalized.contains("clients") -> {
                onNavigateTo("CLIENTS")
                _voiceState.value = VoiceCommandState.ActionExecuted(
                    commandTitle = "Open Clients CRM",
                    feedback = "Navigating to Client CRM..."
                )
            }

            normalized.contains("deliverable") || normalized.contains("audit") || normalized.contains("report") -> {
                onNavigateTo("DELIVERABLES")
                _voiceState.value = VoiceCommandState.ActionExecuted(
                    commandTitle = "Open Deliverables",
                    feedback = "Navigating to 5-Point SEO Deliverables..."
                )
            }

            normalized.contains("settings") || normalized.contains("api key") -> {
                onNavigateTo("SETTINGS")
                _voiceState.value = VoiceCommandState.ActionExecuted(
                    commandTitle = "Open Settings",
                    feedback = "Navigating to Settings..."
                )
            }

            else -> {
                _voiceState.value = VoiceCommandState.Error(
                    "Command \"$spoken\" not recognized. Try saying \"Approve all tasks\" or \"Find more leads\"."
                )
            }
        }
    }
}
