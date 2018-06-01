package me.sunrisem.steamweb

import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.httpPost
import org.apache.commons.codec.binary.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

class SteamTotp {

    private fun getCurrentTime(_timeOffset: Long?): Long {

        var timeOffset: Long = 0
        if (_timeOffset != null) timeOffset = _timeOffset

        return (System.currentTimeMillis() / 1000L) + timeOffset
    }

    fun getTimeDiff(): Long {

        class TimeData(val server_time: String)
        class QueryTime(val response: TimeData)

        val url = "http://api.steampowered.com/ITwoFactorService/QueryTime/v1/"
        val (request, response, result) = url.httpPost().responseString()

        val json = Klaxon().parse<QueryTime>(result.get())
        if (json == null) throw Error("Couldn't parse JSON")

        val resServerTime = json.response.server_time.toInt()

        return resServerTime - getCurrentTime(null)
    }

    fun getAuthCode(sharedSecret: String, _timeOffset: Long?): String {

        var timeOffset = _timeOffset
        if (_timeOffset == null) timeOffset = getTimeDiff()

        val unixTime = getCurrentTime(timeOffset)

        val keyBytes = Base64.decodeBase64(sharedSecret)
        val signingKey = SecretKeySpec(keyBytes, "HmacSHA1")

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(signingKey)

        val time = (unixTime / 30).toInt()
        val b = ByteBuffer.allocate(8)
        b.putInt(4, time)
        b.order(ByteOrder.BIG_ENDIAN)
        val result2 = b.array()

        val rawHmac = mac.doFinal(result2)
        val start = (rawHmac[19] and 0x0F).toByte()

        var bytes: ByteArray
        bytes = Arrays.copyOfRange(rawHmac, start.toInt(), start + 4)
        val wrapped = ByteBuffer.wrap(bytes)
        val codeInt = wrapped.int
        var fullcode = codeInt and 0x7fffffff and -0x1

        val chars = "23456789BCDFGHJKMNPQRTVWXY"
        var code = ""
        for (i in 0..4) {
            val curChar = chars[fullcode % chars.length].toString()
            code += curChar
            fullcode /= chars.length
        }

        return code
    }

}