package me.sunrisem.steamweb

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import khttp.get
import khttp.post
import me.sunrisem.steamweb.enums.ChatState
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule

data class ChatMessageEvent(val sender: SteamID, val message: String)
data class ChatUpdatePersona(val sender: SteamID)
data class ChatTypingEvent(val sender: SteamID)

class ChatLoginException(override val message: String) : Exception(message)

class Chat(private val steamWeb: SteamWeb, private val pollTime: Long) {

    private var state: Int = ChatState.Offline
    private val oAuthApiToken: String = getOAuthToken()
    private var logonResponse: JSONObject? = logon()
    lateinit var friends: MutableMap<String, SteamID>

    private val publisher = PublishSubject.create<Any>()

    private fun emit(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)

    private fun getOAuthToken(): String {
        val url = get("https://steamcommunity.com/chat", cookies = steamWeb.cookies)
        val body = url.text

        val regex = Regex("([0-9 a-f]{32})")
        val find = regex.find(body)!!
        val findValues = find.groupValues

        return findValues[0]
    }

    private fun logon(): JSONObject? {

        state = ChatState.LoggingOn

        val url = "https://api.steampowered.com/ISteamWebUserPresenceOAuth/Logon/v1"
        val payload = mapOf("access_token" to oAuthApiToken, "ui_mode" to "web")

        val response = post(url, data = payload)

        if (response.statusCode != 200) throw ChatLoginException("HTTP Status Code: ${response.statusCode}")

        if (response.jsonObject.getString("error") != "OK") {
            throw ChatLoginException(response.jsonObject.getString("error"))
        }

        state = ChatState.LoggedOn
        return response.jsonObject
    }

    fun logOff() {

        if (state != ChatState.LoggedOn) return

        val url = "${steamWeb.apiURL}/ISteamWebUserPresenceOAuth/Logoff/v1"

        val payload = mapOf(
                "access_token" to oAuthApiToken,
                "umqid" to logonResponse!!.getString("umqid")
        )

        val response = post(url, data = payload)

        if (response.statusCode != 200) throw ChatLoginException("HTTP Status Code: ${response.statusCode}")

        state = ChatState.Offline
        friends = mutableMapOf()
        logonResponse = null
    }

    private fun poll() {

        if (state != ChatState.LoggedOn) return

        val url = steamWeb.apiURL + "/ISteamWebUserPresenceOAuth/Poll/v1"

        val payload = mapOf(
                "access_token" to oAuthApiToken,
                "umqid" to logonResponse!!.getString("umqid"),
                "message" to logonResponse!!.getInt("message"),
                "pollid" to 1,
                "sectimeout" to 20,
                "secidletime" to 0,
                "use_accountids" to 1
        )

        val response = post(url, data = payload).jsonObject
        var messages: JSONArray

        try {
            messages = response.getJSONArray("messages")!!
        } catch (err: Throwable) {
            return
        }

        logonResponse!!.put("message", response.getInt("messagelast"))

        for (message in messages) {
            if (message !is JSONObject) continue

            val sender = SteamID(_accountid = message.getInt("accountid_from"))

            when (message.getString("type")) {
                "personastate" -> emit(ChatUpdatePersona(sender))
                "saytext" -> emit(ChatMessageEvent(sender, message.getString("text")))
                "typing" -> emit(ChatTypingEvent(sender))
            }
        }
    }

    fun sendMessage(user: Long, text: String): String? {

        if (state != ChatState.LoggedOn) return null

        val url = "${steamWeb.apiURL}/ISteamWebUserPresenceOAuth/Message/v1"

        val payload = mapOf(
                "access_token" to oAuthApiToken,
                "steamid_dst" to user,
                "text" to text,
                "type" to "saytext",
                "umqid" to logonResponse!!.getString("umqid")
        )

        val response = post(url, data = payload).jsonObject.getString("error")

        return when (response) {
            "OK" -> null
            else -> response
        }

    }

    fun startPoll() {
        Timer().schedule(delay = 0, period = pollTime) {
            poll()
        }
    }
}


//chat.listen(ChatMessageEvent::class.java).subscribe({
//    println("Message from ${it.sender}. ${it.message}")
//})
//
//chat.listen(ChatTypingEvent::class.java).subscribe({
//    println("${it.sender} is Typing")
//})
//
//chat.listen(ChatUpdatePersona::class.java).subscribe({
//    println("Updating Persona Sate ${it.sender}")
//})