# Kotlin Steam Web
Steam-Web is a library focused on easing up interactions with steamcommunity.com and other Steam Web domains. It's made on the purpose of adding multiples components (Chat, Trading, Group Handling, etc)

# Usage

First, you will need to create an SteamWeb instance.

```kotlin
val SteamAccount = SteamWeb(username="", password="", sharedSecret="")
```

Then you can pass it on any Component.

```kotlin
val SteamChat = SteamChat(SteamAccount, polltime=5000)

chat.startPoll()

chat.listen(ChatMessageEvent::class.java).subscribe({
    println("Message from ${it.sender.getSteamID64()}. Text: ${it.message}")

    if (it.message == "Ping") {
        chat.sendMessage(it.sender.getSteamID64(), "Pong")
    }
})

val chatResponse = chat.sendMessage(76561198150836073, "Hello there")
    
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




# SteamChat

## Parameters


### steamWeb

An SteamWeb instance.


### pollTime

Poll period time in milliseconds as Long.




## Properties


### state

`ChatState` enum value.


### oAuthApiToken

Your acount oAuth API Token.




## Methods


### startPoll()

Start polling for chat events. 


### logOff()

Log off from Chat. 


### sendMessage(user, text)

* user: Long. SteamID64 from Recipient Account.
* text: String. Message to been sent.

Returns response error message if we got it, if everything is right returs null.


### listen(ChatEvent) 

Listen to a suscribable Chat Event. Example

```kotlin
chat.listen(ChatMessageEvent::class.java).subscribe({
    println("Message from ${it.sender.getSteamID64()}. ${it.message}")
})
```




## chatEvents

### ChatMessageEvent(sender, message)

* sender: `SteamID`.
* message: String.

Fired when we receive a chat message.


### ChatUpdatePersona(sender)

* sender: `SteamID`.

Fired when an user from friend list updates its persona state.





