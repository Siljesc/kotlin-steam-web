package me.sunrisem.steamweb

import khttp.get
import khttp.structures.cookie.CookieJar
import org.json.JSONObject

class SteamTradeUrl(private val regexValues: List<String>) {

    fun getToken(): String {
        return regexValues[3]
    }

    fun getFull(): String {
        return regexValues[0]
    }
}

class SteamWeb(username: String? = null, password: String? = null, sharedSecret: String? = null) {

    private val _userName: String = username ?: System.getenv("username")
    private val _password: String = password ?: System.getenv("password")
    private val _sharedSecret: String = sharedSecret ?: System.getenv("sharedSecret")

    private val login = Login(_userName, _password, _sharedSecret)
    val loginResponse = login.doLogin()

    var cookies = CookieJar(login.transferLogin(loginResponse.jsonObject)[0])
    var apikey = getApiKey()

    lateinit var steamid: String
    val profileURL: String = "https://steamcommunity.com/id/me"
    val apiURL: String = "https://api.steampowered.com"

    private fun getApiKey(): String {

        val url = "https://steamcommunity.com/dev/apikey?l=english"
        val response = get(url, allowRedirects = false, cookies = cookies)

        val body = response.text
        val regex = Regex("<p>Key: ([0-9 A-F]+)</p>")
        val find = regex.find(body)!!

        return find.groupValues[1]
    }

    fun getNotifications(): JSONObject {
        val url = "https://steamcommunity.com/actions/GetNotificationCounts"
        val response = get(url, cookies = cookies)

        return response.jsonObject
    }

    fun getTradeURL(): SteamTradeUrl {
        val url = "$profileURL/tradeoffers/privacy"
        val response = get(url, cookies = cookies)
        val body = response.text

        val regex = Regex("https?://(www.)?steamcommunity\\.com/tradeoffer/new/\\?partner=\\d+(&|&amp;)token=([a-zA-Z0-9-_]+)")
        val find = regex.find(body)!!

        val findValues = find.groupValues

        return SteamTradeUrl(findValues)
    }

}

fun main(args: Array<String>) {

    val steam = SteamWeb(null, null, null)
    val chat = Chat(steam, 5000)

    chat.startPoll()

    chat.listen(ChatMessageEvent::class.java).subscribe({
        println("Message from ${it.sender.getSteamID64()}. ${it.message}")

        if (it.message == "!ping") {
            chat.sendMessage(it.sender.getSteamID64(), "Pong")
        }
    })

    val chatResponse = chat.sendMessage(76561198150836073, "Hello beep boop")
    println(chatResponse)
}
