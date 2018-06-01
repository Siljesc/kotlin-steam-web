package me.sunrisem.steamweb

import khttp.post
import khttp.responses.Response
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

class Login(private val username: String, private val password: String, private val sharedSecret: String) {

    private fun getRSAObject(): JSONObject {
        val payload = mapOf("username" to username)
        return post("https://steamcommunity.com/login/getrsakey/", data = payload).jsonObject
    }

    private fun encryptPassword(rsaObj: JSONObject): String {

        val authMod = rsaObj.getString("publickey_mod")
        val authExp = rsaObj.getString("publickey_exp")
        val rsaParams = RSAPublicKeySpec(BigInteger(authMod, 16), BigInteger(authExp, 16))

        val bytepass = StringUtils.getBytesUtf8(password)
        val factory = KeyFactory.getInstance("RSA")
        val pub = factory.generatePublic(rsaParams)
        var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")

        cipher.init(Cipher.ENCRYPT_MODE, pub)

        var passEncrypted = cipher.doFinal(bytepass)
        passEncrypted = Base64.encodeBase64(passEncrypted)

        return StringUtils.newStringUtf8(passEncrypted)
    }

    fun doLogin(): Response {

        val rsaObj = getRSAObject()

        var loginData = mapOf(
                "username" to username,
                "password" to encryptPassword(rsaObj),
                "rsatimestamp" to rsaObj.getString("timestamp"),
                "token_gid" to rsaObj.getString("token_gid"),
                "twofactorcode" to SteamTotp().getAuthCode(sharedSecret, null)
        )

        return post("https://steamcommunity.com/login/dologin/", data = loginData)
    }

    private fun parseCookies(cookies: String): MutableMap<String, String> {
        val splitCookies = cookies.split(",")
        val splitFCookies = mutableListOf<String>()

        for (i in splitCookies.indices) {
            val cookie = splitCookies[i]
            var lastCookie = ""

            if (i > 0) {
                lastCookie = splitCookies[i - 1]
            }

            if (!lastCookie.contains("expires")) {
                splitFCookies.add(cookie)
                continue
            }

            val joinCookies = "$lastCookie,$cookie"
            val lastFCookie = splitFCookies.size
            splitFCookies[lastFCookie - 1] = joinCookies
        }

        val cookiesMap = mutableMapOf<String, String>()

        for (cookie in splitFCookies) {
            val splitCookie = cookie.split("=", limit = 2)
            val cookieName = splitCookie[0].trim()
            var cookieValue = ""
            if (splitCookie.size > 1) cookieValue = splitCookie[1].trim()

            cookiesMap[cookieName] = cookieValue
        }

        return cookiesMap
    }

    fun transferLogin(data: JSONObject): MutableList<MutableMap<String, String>> {

        val transferParams = data.getJSONObject("transfer_parameters")
        val transferUrls = data.getJSONArray("transfer_urls")

        val params = mapOf(
                "steamid" to transferParams.getString("steamid"),
                "webcookie" to transferParams.getString("webcookie"),
                "auth" to transferParams.getString("auth"),
                "token_secure" to transferParams.getString("token_secure"),
                "token" to transferParams.getString("token"),
                "remember_login" to true
        )

        val list = mutableListOf<MutableMap<String, String>>()

        for (url in transferUrls) {
            if (url !is String) continue
            val result = post(url, data = params)

            val stringCookies = result.headers["Set-Cookie"]!!

            val cookies = parseCookies(stringCookies)
            list.add(element = cookies)
        }

        return list
    }
}