package garden.gardenapple.mail.mail

import android.icu.text.IDNA
import java.lang.StringBuilder

data class MailAccount(private val originalAddress: String, val password: String) {
    val canonAddress: String

    init {
        // Canonicalize according to Autocrypt Level 1
        val (addressLocal, addressHost) = originalAddress.split('@')

        val stringBuilder = StringBuilder()
        val info = IDNA.Info()
        IDNA.getUTS46Instance(IDNA.DEFAULT).nameToASCII(addressHost, stringBuilder, info)
        if (info.hasErrors())
            throw IllegalArgumentException("Bad address $originalAddress, errors: ${info.errors.joinToString()}")

        canonAddress = addressLocal.lowercase() + '@' + stringBuilder
    }
}