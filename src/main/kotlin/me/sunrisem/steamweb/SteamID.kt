package me.sunrisem.steamweb

object SteamIDConsts {

    const val AccountIDMask = 0xFFFFFFFF
    const val AccountInstanceMask = 0x000FFFFF

    object Universe {
        val INVALID = 0
        val PUBLIC = 1
        val BETA = 2
        val INTERNAL = 3
        val DEV = 4
    }

    object Type {
        val INVALID = 0
        val INDIVIDUAL = 1
        val MULTISEAT = 2
        val GAMESERVER = 3
        val ANON_GAMESERVER = 4
        val PENDING = 5
        val CONTENT_SERVER = 6
        val CLAN = 7
        val CHAT = 8
        val P2P_SUPER_SEEDER = 9
        val ANON_USER = 10
    }

    object Instance {
        val ALL = 0
        val DESKTOP = 1
        val CONSOLE = 2
        val WEB = 4
    }

    object ChatInstanceFlags {
        val Clan = (AccountInstanceMask + 1) shr 1
        val Lobby = (AccountInstanceMask + 1) shr 2
        val MMSLobby = (AccountInstanceMask + 1) shr 3
    }

    val TypeToChar = mapOf(
            SteamIDConsts.Type.INVALID to "I",
            SteamIDConsts.Type.INDIVIDUAL to "U",
            SteamIDConsts.Type.MULTISEAT to "M",
            SteamIDConsts.Type.GAMESERVER to "G",
            SteamIDConsts.Type.ANON_GAMESERVER to "A",
            SteamIDConsts.Type.PENDING to "P",
            SteamIDConsts.Type.CONTENT_SERVER to "C",
            SteamIDConsts.Type.CLAN to "g",
            SteamIDConsts.Type.CHAT to "T",
            SteamIDConsts.Type.ANON_USER to "a"
    )

    val CharToType = mutableMapOf<String, Int>().also {
        TypeToChar.forEach { (k, v) -> it[v] = k }
    }.toMap()

}

class SteamID(private val input: String? = null, private val _accountid: Int? = null) {

    var universe = SteamIDConsts.Universe.INVALID
    var type = SteamIDConsts.Type.INVALID
    var instance = SteamIDConsts.Instance.ALL
    var accountid = 0
    val updateData = getData()

    private fun getData() {

        if (input == null) {
            if (_accountid == null) return

            universe = SteamIDConsts.Universe.PUBLIC
            type = SteamIDConsts.Type.INDIVIDUAL
            instance = SteamIDConsts.Instance.DESKTOP
            accountid = _accountid

            return
        }

        val steamID2Regex = Regex("^STEAM_([0-5]):([0-1]):([0-9]+)$")
        val steamID3Regex = Regex("^\\[([a-zA-Z]):([0-5]):([0-9]+)(:[0-9]+)?]$")

        val matchSteamID2 = steamID2Regex.matchEntire(input)

        if (matchSteamID2 != null) {

            val groupValues = matchSteamID2.groupValues

            universe = try {
                groupValues[1].toInt()
            } catch (err: NumberFormatException) {
                SteamIDConsts.Universe.PUBLIC
            }

            if (universe == 0) universe = SteamIDConsts.Universe.PUBLIC

            type = SteamIDConsts.Type.INDIVIDUAL
            instance = SteamIDConsts.Instance.DESKTOP
            accountid = (groupValues[3].toInt() * 2) + groupValues[2].toInt()

            return
        }

        val matchSteamID3 = steamID3Regex.matchEntire(input)

        if (matchSteamID3 != null) {

            val groupValues = matchSteamID3.groupValues

            universe = groupValues[2].toInt()
            accountid = groupValues[3].toInt()

            var typeChar = groupValues[1]

            if (groupValues[4] != null) instance = groupValues[4].substring(1).toInt()
            else if (typeChar == "U") instance = SteamIDConsts.Instance.DESKTOP

            when (typeChar) {
                "c" -> {
                    instance = instance or SteamIDConsts.ChatInstanceFlags.Clan
                    type = SteamIDConsts.Type.CHAT
                }
                "L" -> {
                    instance = instance or SteamIDConsts.ChatInstanceFlags.Lobby
                    type = SteamIDConsts.Type.CHAT
                }

                else -> this.type = SteamIDConsts.CharToType[typeChar]!!
            }

            return
        }

        var steamID64: Long

        try {
            steamID64 = input.toLong()
        } catch (err: NumberFormatException) {
            throw Error("Invalid SteamID Format")
        }

        accountid = steamID64.toInt() shl 32
        instance = ((steamID64 shr 32) and ((1 shl 20) - 1)).toInt()
        type = ((steamID64 shr 52) and ((1 shl 4) - 1)).toInt()
        universe = ((steamID64 shr 56) and ((1 shl 8) - 1)).toInt()
    }

    fun isGroupChat(): Boolean {
        return (type == SteamIDConsts.Type.CHAT && (instance and SteamIDConsts.ChatInstanceFlags.Clan) != 0)
    }

    fun isLobby(): Boolean {
        return (
                type == SteamIDConsts.Type.CHAT && (
                        (instance and SteamIDConsts.ChatInstanceFlags.Lobby) != 0 ||
                                (instance and SteamIDConsts.ChatInstanceFlags.MMSLobby) != 0
                        )
                )
    }

    fun getSteam2R(): String {

        var universe2R = universe
        if (universe == 1) universe2R = 0

        return "STEAM_$universe2R:${(accountid and 1)}:${accountid / 2}"
    }

    fun getSteam3R(): String {

        var typeChar = if (type == 0) "i" else SteamIDConsts.TypeToChar[type]!!

        typeChar = when {
            instance and SteamIDConsts.ChatInstanceFlags.Clan != 0 -> "c"
            instance and SteamIDConsts.ChatInstanceFlags.Lobby != 0 -> "L"
            else -> typeChar
        }

        val renderInstance = (
                type == SteamIDConsts.Type.ANON_GAMESERVER ||
                        type == SteamIDConsts.Type.MULTISEAT ||
                        (type == SteamIDConsts.Type.INDIVIDUAL && instance != SteamIDConsts.Instance.DESKTOP)
                )

        val renderString = if (renderInstance) ":$instance" else ""

        return "[$typeChar:$universe:$accountid$renderString]"
    }

    fun getSteamID64(): Long {
        return ((universe.toLong() shl 56) or (type.toLong() shl 52) or (instance.toLong() shl 32) or accountid.toLong())
    }

}
