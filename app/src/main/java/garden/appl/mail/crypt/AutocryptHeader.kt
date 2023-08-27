package garden.appl.mail.crypt

import android.util.Base64
import android.util.Log
import org.bouncycastle.openpgp.PGPPublicKey
import org.pgpainless.PGPainless
import java.util.regex.Pattern

class AutocryptHeader(val headerString: String) {
    companion object {
        const val LOGGING_TAG = "AutocryptHeader"
    }

    lateinit var addr: String
        private set
    lateinit var key: PGPPublicKey
        private set

    init {
        val split = headerString.lines().joinToString()
            .splitToSequence(Regex("; *"))
        for (item in split) {
            Log.d("headeritem", "item: '$item'")
            val (key, value) = item.split(Regex("= *"))
            when (key) {
                "addr" -> addr = value
                "keydata" -> {
                    val keyData = Base64.decode(value, Base64.DEFAULT)
                    val keyRing = PGPainless.readKeyRing().keyRing(keyData)
                    if (keyRing == null) {
                        Log.d(LOGGING_TAG, "failed to parse keydata as ring")
                    } else {
                        this.key = keyRing.publicKey
                    }
                }
            }
        }
    }
}