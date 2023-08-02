package net.pbforge.crypto

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.Security

class IdentityTest : StringSpec({
    "base test" {
        Security.addProvider(BouncyCastleProvider())
        val identity = Identity.makeRootIdentity(
            subject = "CN=cn, O=o, L=L, ST=il, C=c"
        )
        val rootFile = File(".", "root.p12")
        val subFile1 = File(".", "sub1.p12")
        val subFile2 = File(".", "sub2.p12")

        identity.save(
            output = FileOutputStream(rootFile),
            storePassword = "changeit"
        )
        val loadedIdentity = FileInputStream(rootFile).use {
            Identity.loadIdentity(
                inputStream = it,
                storePassword = "changeit",
            )
        }
        identity.subject shouldBe "CN=cn,O=o,L=L,ST=il,C=c"
        identity.chain.size shouldBe 0
        loadedIdentity.subject shouldBe "C=c, ST=il, L=L, O=o, CN=cn"
        loadedIdentity.chain.size shouldBe 0

        val identity2 = identity.makeSubIdentity(
            subject = "CN=cn2,O=o,L=L,ST=il,C=c"
        )
        identity2.save(
            output = FileOutputStream(subFile1),
            storePassword = "changeit"
        )
        val loadedIdentity2 = FileInputStream(subFile1).use {
            Identity.loadIdentity(
                inputStream = it,
                storePassword = "changeit",
            )
        }
        identity2.subject shouldBe "CN=cn2,O=o,L=L,ST=il,C=c"
        identity2.chain.size shouldBe 1
        loadedIdentity2.subject shouldBe "C=c, ST=il, L=L, O=o, CN=cn2"
        loadedIdentity2.chain.size shouldBe 1

        val identity3 = identity2.makeSubIdentity(
            subject = "CN=cn3,O=o,L=L,ST=il,C=c"
        )
        identity3.save(
            output = FileOutputStream(subFile2),
            storePassword = "changeit"
        )
        val loadedIdentity3 = FileInputStream(subFile2).use {
            Identity.loadIdentity(
                inputStream = it,
                storePassword = "changeit",
            )
        }
        identity3.subject shouldBe "CN=cn3,O=o,L=L,ST=il,C=c"
        identity3.chain.size shouldBe 2
        loadedIdentity3.subject shouldBe "C=c, ST=il, L=L, O=o, CN=cn3"
        loadedIdentity3.chain.size shouldBe 2
    }
})
