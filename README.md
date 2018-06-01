# Kotlin Steam Web
Steam-Web is a library focused on easing up interactions with steamcommunity.com and other Steam Web domains. It's made on the purpose of adding multiples components (Chat, Trading, Group Handling, etc)

# Usage

First, you will need to create an SteamWeb instance.

```kotlin
val SteamAccount = SteamWeb(username="", password="", sharedSecret="")
```

Then you can pass it on any Component.

```kotlin
val steamChat = SteamChat(SteamAccount, polltime=5000)

steamChat.startPoll()

steamChat.listen(ChatMessageEvent::class.java).subscribe({
    println("Message from ${it.sender.getSteamID64()}. Text: ${it.message}")

    if (it.message == "Ping") {
        chat.sendMessage(it.sender.getSteamID64(), "Pong")
    }
})

steamChat.sendMessage(76561198150836073, "Hello there")
    
```

# SteamWeb


## Properties


### steamid

String containing your account SteamID64.


### cookies

KHttp Cookie Jar with your login request cookies.


### apiKey

Your account API Key from https://steamcommunity.com/dev/apikey.


## Methods


### getNotifications()

Returns JSONObject containing account notifications.


### getTradeURL()

Returns a `SteamTradeURL`. Then you can use `getFull()` to get your whole trade url or `getToken()` to get just your trade token.



# Other Documentation

The rest of the documentation is available in the [wiki](https://github.com/SunriseM/kotlin-steam-web/wiki) 


# Acknowledgement

Special thanks to these projects that made some things easier.

* [node-steamcommunity](https://github.com/DoctorMcKay/node-steamcommunity)
* [java-steam-totp](https://github.com/Denhart/java-steam-totp)





