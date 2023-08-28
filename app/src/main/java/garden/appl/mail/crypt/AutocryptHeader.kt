package garden.appl.mail.crypt

import android.util.Base64
import android.util.Log
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.pgpainless.PGPainless

class AutocryptHeader(val headerString: String) {
    companion object {
        const val LOGGING_TAG = "AutocryptHeader"
    }

    lateinit var addr: String
        private set
    lateinit var keyRing: PGPPublicKeyRing
        private set

    init {
        val split = headerString.splitToSequence(Regex("; *"))
            .map { parameter -> parameter.lines().joinToString(separator = "", transform = String::trim)}
        for (item in split) {
            Log.d("headeritem", "item: '$item'")
            val (key, value) = item.split(Regex("= *"))
            when (key) {
                "addr" -> addr = value
                "keydata" -> {
                    val keyData = Base64.decode(value, Base64.DEFAULT)
                    val keyRing = PGPainless.readKeyRing().publicKeyRing(keyData)
                    if (keyRing == null) {
                        Log.d(LOGGING_TAG, "failed to parse keydata as ring")
                    } else {
                        this.keyRing = keyRing
                    }
                }
            }
        }
    }
}