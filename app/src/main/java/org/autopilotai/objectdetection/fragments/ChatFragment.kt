package org.autopilotai.objectdetection.fragments

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.autopilotai.objectdetection.LoginViewModel
import org.autopilotai.objectdetection.MainViewModel
import org.autopilotai.objectdetection.R
import org.autopilotai.objectdetection.service.WebSocketListener


//https://itnext.io/websockets-in-android-with-okhttp-and-viewmodel-776a9eed67b5
class ChatFragment : Fragment() {

    companion object {
        fun newInstance() = ChatFragment()
    }

    private lateinit var viewModel: MainViewModel
    private val viewLoginModel: LoginViewModel by activityViewModels()

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
        val imageButton = view.findViewById<ImageButton>(R.id.imageButton)

        // If request comes from Gallery Fragment
        val imageResId = arguments?.getInt("imageResId") ?: 0
        val message = arguments?.getString("message") ?: ""
        if (message != "") {
            messageTV.text = message
            sendImgText(message)
        }

        messageTV.movementMethod = ScrollingMovementMethod()

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

        imageButton.setOnClickListener {
            findNavController().navigate(ChatFragmentDirections.actionChatToCamera2())
        }
    }

    private fun createRequest(): Request {
        val accessToken = viewLoginModel.getCredentials()?.accessToken
        val websocketURL = "wss://webapp-autopilotai-api.azurewebsites.net/message?access_token=${accessToken}"

        return Request.Builder()
            .url(websocketURL)
            .build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        okHttpClient.dispatcher.executorService.shutdown()
    }

    private fun sendImgText(message: String) {
        webSocket = okHttpClient.newWebSocket(createRequest(), webSocketListener)
        webSocket!!.send(message)
        viewModel.addMessage(Pair(true, message))
    }
}