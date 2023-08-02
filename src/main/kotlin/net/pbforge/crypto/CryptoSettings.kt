package net.pbforge.crypto

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import kotlin.math.absoluteValue

val DEFAULT_CRYPTO_SETTINGS = CryptoSettings()

data class CryptoSettings(
    val keySize: Int = 1024,
    val keyPairAlgorithm: String = "RSA",
    val secureRandom: SecureRandom = SecureRandom.getInstanceStrong(),
    val signatureAlgorithm: String = "SHA256WithRSAEncryption",
) {
    private val signerBuilder = JcaContentSignerBuilder(signatureAlgorithm)

    fun sign(certificateBuilder: X509v3CertificateBuilder, privateKey: PrivateKey)
            : X509CertificateHolder =
        certificateBuilder.build(signerBuilder.build(privateKey))

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance(keyPairAlgorithm)
        gen.initialize(keySize, secureRandom)
        return gen.genKeyPair()
    }

    fun generateSerialNumber(): BigInteger =
        BigInteger.valueOf(secureRandom.nextLong().absoluteValue)
}
