package com.example.network

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets

/**
 * Result of attempting to send data over TCP socket.
 */
sealed class SendResult {
    abstract val reason: String?

    data object Success : SendResult() {
        override val reason: String? = null
    }

    data class Failure(
        override val reason: String,
        val cause: Throwable? = null
    ) : SendResult()

    val isSuccess: Boolean get() = this is Success
}

/**
 * Low-level TCP Client for Wi-Fi communication with Ludo-app (Host/Server).
 * Provides non-blocking socket management, serialized writes protected by Mutex,
 * message framing (newline-delimited JSON), and disconnect detection.
 */
class WifiControllerClient(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "WifiControllerClient"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val SO_TIMEOUT_MS = 0 // 0 = indefinite for reading stream
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var readerJob: Job? = null
    private val writeMutex = Mutex()

    private val _rawIncomingFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val rawIncomingFlow: SharedFlow<String> = _rawIncomingFlow.asSharedFlow()

    private val _disconnectFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val disconnectFlow: SharedFlow<String> = _disconnectFlow.asSharedFlow()

    val isConnected: Boolean
        get() = socket?.let { !it.isClosed && it.isConnected } ?: false

    var currentRemoteHost: String? = null
        private set
    var currentRemotePort: Int? = null
        private set

    /**
     * Connects to the remote Ludo Host TCP Server.
     */
    suspend fun connect(ipAddress: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            closeExistingSocket()

            Log.d(TAG, "Connecting to TCP server at $ipAddress:$port...")
            val newSocket = Socket()
            newSocket.tcpNoDelay = true
            newSocket.keepAlive = true
            newSocket.soTimeout = SO_TIMEOUT_MS
            newSocket.connect(InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT_MS)

            socket = newSocket
            reader = BufferedReader(InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8))
            currentRemoteHost = ipAddress
            currentRemotePort = port

            startReaderLoop()
            Log.i(TAG, "TCP socket connected to $ipAddress:$port")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to $ipAddress:$port", e)
            closeExistingSocket()
            Result.failure(e)
        }
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            val currentReader = reader ?: return@launch
            try {
                while (isActive) {
                    val line = currentReader.readLine() ?: break // EOF reached
                    if (line.isNotBlank()) {
                        _rawIncomingFlow.emit(line)
                    }
                }
                Log.w(TAG, "TCP stream closed by remote server (EOF)")
                handleDisconnect("Remote host closed connection (EOF)")
            } catch (e: SocketException) {
                if (isActive) {
                    Log.w(TAG, "TCP socket exception: ${e.message}")
                    handleDisconnect("Connection lost: ${e.message}")
                }
            } catch (e: Exception) {
                if (e !is CancellationException && isActive) {
                    Log.e(TAG, "Error in TCP reader loop", e)
                    handleDisconnect("Reader error: ${e.message}")
                }
            }
        }
    }

    /**
     * Requirement (Issue 2):
     * Sends a newline-delimited message over the socket and confirms real write & flush.
     * Protected by [writeMutex] against concurrent stream corruption.
     */
    suspend fun send(message: String): SendResult = withContext(Dispatchers.IO) {
        val currentSocket = socket
        val currentWriter = writer

        if (currentSocket == null || currentSocket.isClosed || !currentSocket.isConnected || currentWriter == null) {
            val err = "Cannot send: TCP socket is closed or not connected"
            Log.w(TAG, err)
            return@withContext SendResult.Failure(err)
        }

        writeMutex.withLock {
            try {
                val toSend = if (message.endsWith("\n")) message else "$message\n"
                currentWriter.write(toSend)
                currentWriter.flush()
                SendResult.Success
            } catch (e: IOException) {
                val err = "Socket write failed (IOException): ${e.message}"
                Log.e(TAG, err, e)
                handleDisconnect(err)
                SendResult.Failure(err, e)
            } catch (e: Exception) {
                val err = "Socket write failed: ${e.message}"
                Log.e(TAG, err, e)
                handleDisconnect(err)
                SendResult.Failure(err, e)
            }
        }
    }

    private fun handleDisconnect(reason: String) {
        closeExistingSocket()
        scope.launch {
            _disconnectFlow.emit(reason)
        }
    }

    fun disconnect(reason: String = "User requested disconnect") {
        Log.i(TAG, "Disconnecting TCP client: $reason")
        closeExistingSocket()
    }

    private fun closeExistingSocket() {
        readerJob?.cancel()
        readerJob = null
        try {
            reader?.close()
        } catch (ignored: Exception) {}
        try {
            writer?.close()
        } catch (ignored: Exception) {}
        try {
            socket?.close()
        } catch (ignored: Exception) {}

        reader = null
        writer = null
        socket = null
        currentRemoteHost = null
        currentRemotePort = null
    }
}
