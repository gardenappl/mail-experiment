package garden.appl.mail.crypt

import android.util.Base64
import android.util.Log
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.pgpainless.PGPainless

data class AutocryptHeader(
    val address: String,
    val keyRing: PGPPublicKeyRing,
    val preferEncrypt: PreferEncrypt
) {
    enum class PreferEncrypt(val headerParameter: String) {
        MUTUAL("mutual"),
        NO_PREFERENCE("nopreference")
    }

    companion object {
        const val LOGGING_TAG = "AutocryptHeader"

        fun parseHeaderValue(headerString: String): AutocryptHeader {
            lateinit var address: String
            lateinit var keyRing: PGPPublicKeyRing
            var preferEncrypt = PreferEncrypt.NO_PREFERENCE

            val split = headerString.splitToSequence(Regex("; *")).map { parameter ->
                parameter.lines().joinToString(separator = "", transform = String::trim)
            }
            for (item in split) {
                Log.d("headeritem", "item: '$item'")
                val (key, value) = item.split(Regex("= *"))
                when (key) {
                    "addr" -> address = value
                    "keydata" -> {
                        val keyData = Base64.decode(value, Base64.DEFAULT)
                        keyRing = PGPainless.readKeyRing().publicKeyRing(keyData)!!
                    }
                    "prefer-encrypt" -> preferEncrypt = when (value) {
                        "mutual" -> PreferEncrypt.MUTUAL
                        "nopreference" -> PreferEncrypt.NO_PREFERENCE
                        else -> throw IllegalArgumentException("Illegal prefer-encrypt value: $value")
                    }
                    else -> {
                        if (!key.startsWith('_'))
                            throw IllegalArgumentException("Unknown Autocrypt header attribute: $key")
                    }
                }
            }
            return AutocryptHeader(address, keyRing, preferEncrypt)
        }
    }

    override fun toString(): String {
        return "addr=$address; prefer-encrypt=${preferEncrypt.headerParameter}; " +
                "keydata=${String(Base64.encode(keyRing.getEncoded(true), Base64.NO_WRAP))}"
    }
}