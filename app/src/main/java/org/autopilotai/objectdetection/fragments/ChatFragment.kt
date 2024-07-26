package org.autopilotai.objectdetection.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Pair
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
//import com.mrntlu.websocketguide.Constants
import org.autopilotai.objectdetection.R
import org.autopilotai.objectdetection.service.WebSocketListener
import org.autopilotai.objectdetection.MainViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

//https://itnext.io/websockets-in-android-with-okhttp-and-viewmodel-776a9eed67b5
class ChatFragment : Fragment() {

    companion object {
        fun newInstance() = ChatFragment()
    }

    private lateinit var viewModel: MainViewModel

    private lateinit var webSocketListener: WebSocketListener
    private val okHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        webSocketListener = WebSocketListener(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageET = view.findViewById<EditText>(R.id.messageET)
        val sendMessageButton  = view.findViewById<ImageButton>(R.id.sendButton)
        val connectButton = view.findViewById<Button>(R.id.connectButton)
        val disconnectButton = view.findViewById<Button>(R.id.disconnectButton)
        val statusTV = view.findViewById<TextView>(R.id.statusTV)
        val messageTV = view.findViewById<TextView>(R.id.messageTV)

        viewModel.socketStatus.observe(viewLifecycleOwner) {
            statusTV.text = if (it) "Connected" else "Disconnected"
        }

        var text = ""
        viewModel.messages.observe(viewLifecycleOwner) {
            text += "${if (it.first) "You: " else "Other: "} ${it.second}\n"

            messageTV.text = text
        }

        connectButton.setOnClickListener {
            webSocket = okHttpClient.newWebSocket(createRequest(), webSocketListener)
        }

        disconnectButton.setOnClickListener {
            /**
             * Cancel vs Close (immidiate vs gracefully)
             * cancel -> Immediately and violently release resources held by this web socket, discarding any enqueued messages. This does nothing if the web socket has already been closed or canceled.
             * close -> Attempts to initiate a graceful shutdown of this web socket. Any already-enqueued messages will be transmitted before the close message is sent but subsequent calls to send will
             *      return false and their messages will not be enqueued.
             */
            webSocket?.close(1000, "Canceled manually.")
        }

        sendMessageButton.setOnClickListener {
            webSocket?.send(messageET.text.toString())
            viewModel.addMessage(Pair(true, messageET.text.toString()))
        }
    }

    private fun createRequest(): Request {
        //val websocketURL = "wss://${Constants.CLUSTER_ID}.piesocket.com/v3/1?api_key=${Constants.API_KEY}"
        val websocketURL = "wss://webapp-autopilotai-api.azurewebsites.net/message"

        return Request.Builder()
            .url(websocketURL)
            .build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        okHttpClient.dispatcher.executorService.shutdown()
    }
}