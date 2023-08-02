package net.pbforge.crypto

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

private val bouncyCastleProvider = BouncyCastleProvider()
private val certificateConverter = JcaX509CertificateConverter().setProvider(bouncyCastleProvider)

fun KeyPair.toSubjectPublicKeyInfo(): SubjectPublicKeyInfo =
    SubjectPublicKeyInfo.getInstance(ASN1InputStream(public.encoded).readObject())

fun X509CertificateHolder.toCertificate(): X509Certificate = certificateConverter.getCertificate(this)

val X509Certificate.subject
    get() = subjectDN.name

val X509Certificate.issuer
    get() = issuerDN.name

fun KeyStore.toKeyManagerFactory(passphrase: CharArray): KeyManagerFactory {
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(this, passphrase)
    return kmf
}

fun KeyStore.toTrustManagerFactory(): TrustManagerFactory {
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(this)
    return tmf
}

class Identity(
    val privateKey: PrivateKey,
    val certificate: X509Certificate,
    val chain: List<X509Certificate> = emptyList()
) {
    companion object {
        fun loadIdentity(
            inputStream: InputStream,
            storePassword: String,
            keyStoreFormat: String = "PKCS12",
            keyPassword: String = storePassword,
        ): Identity {
            val keyStore = KeyStore.getInstance(keyStoreFormat)
            keyStore.load(inputStream, storePassword.toCharArray())
            val entries = keyStore.aliases().asSequence().map {
                keyStore.getEntry(it, KeyStore.PasswordProtection(keyPassword.toCharArray()))
            }.toList()
            entries.forEach { entry ->
                when (entry) {
                    is KeyStore.PrivateKeyEntry -> {
                        val chain = entry.certificateChain
                        return Identity(
                            privateKey = entry.privateKey!!,
                            certificate = entry.certificate as X509Certificate,
                            chain = chain.map { it as X509Certificate }.subList(1, chain.size).toList(),
                        )
                    }
                }
            }
            throw IllegalStateException("No suitable entry found in keystore")
        }

        fun makeRootIdentity(
            subject: String,
            cryptoSettings: CryptoSettings = DEFAULT_CRYPTO_SETTINGS,
            serialNumber: BigInteger = cryptoSettings.generateSerialNumber(),
            validityTimespan: ValidityTimespan = DEFAULT_VALIDITY_TIMESPAN,
        ): Identity {
            val keyPair = cryptoSettings.generateKeyPair()
            return Identity(
                keyPair.private, cryptoSettings.sign(
                    X509v3CertificateBuilder(
                        X500Name(subject),
                        serialNumber,
                        Date.from(validityTimespan.notBefore),
                        Date.from(validityTimespan.notAfter),
                        validityTimespan.timezoneLocale,
                        X500Name(subject),
                        keyPair.toSubjectPublicKeyInfo(),
                    ),
                    keyPair.private,
                ).toCertificate()
            )
        }
    }

    val subject: String
        get() = certificate.subjectDN.name

    val issuer: String
        get() = certificate.issuerDN.name

    fun makeSubIdentity(
        subject: String,
        cryptoSettings: CryptoSettings = DEFAULT_CRYPTO_SETTINGS,
        serialNumber: BigInteger = cryptoSettings.generateSerialNumber(),
        validityTimespan: ValidityTimespan = DEFAULT_VALIDITY_TIMESPAN,
    ): Identity {
        val keyPair = cryptoSettings.generateKeyPair()
        val chain = ArrayList<X509Certificate>(chain.size + 1)
        chain.add(certificate)
        chain.addAll(this.chain)
        return Identity(
            keyPair.private,
            cryptoSettings.sign(
                X509v3CertificateBuilder(
                    X500Name(this.subject),
                    serialNumber,
                    Date.from(validityTimespan.notBefore),
                    Date.from(validityTimespan.notAfter),
                    validityTimespan.timezoneLocale,
                    X500Name(subject),
                    keyPair.toSubjectPublicKeyInfo()
                ),
                privateKey,
            ).toCertificate(),
            Collections.unmodifiableList(chain),
        )
    }

    fun toKeyStore(
        keyPassword: String,
        entryName: String? = null,
        keyStoreFormat: String = "PKCS12",
    ): KeyStore {
        val keyStore = KeyStore.getInstance(keyStoreFormat)
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            entryName ?: subject,
            privateKey,
            keyPassword.toCharArray(),
            arrayOf(certificate).plus(chain),
        )
        return keyStore
    }

    fun toKeyManagers() = toKeyStore("").toKeyManagerFactory("".toCharArray()).keyManagers

    fun toTrustManagers() = toKeyStore("").toTrustManagerFactory().trustManagers

    fun save(
        output: OutputStream,
        storePassword: String,
        entryName: String? = null,
        keyStoreFormat: String = "PKCS12",
        keyPassword: String = storePassword
    ) {
        val keyStore = toKeyStore(keyPassword, entryName, keyStoreFormat)
        output.use {
            keyStore.store(output, storePassword.toCharArray())
        }
    }
}
