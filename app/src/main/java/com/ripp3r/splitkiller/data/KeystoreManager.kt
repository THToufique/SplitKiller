package com.ripp3r.splitkiller.data

import android.content.Context
import android.util.Base64
import com.ripp3r.splitkiller.model.SigningKey
import com.ripp3r.splitkiller.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Security
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class KeystoreManager(private val context: Context) {
    
    private val keystoreDir = File(context.filesDir, "keystores")
    
    init {
        if (!keystoreDir.exists()) {
            keystoreDir.mkdirs()
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    suspend fun getAllKeys(): List<SigningKey> = withContext(Dispatchers.IO) {
        val keys = mutableListOf<SigningKey>()
        keys.add(getDefaultTestKey())
        
        keystoreDir.listFiles()?.forEach { keystoreFile ->
            if (keystoreFile.extension == "jks" || keystoreFile.extension == "keystore") {
                try {
                    val keyName = keystoreFile.nameWithoutExtension
                    keys.add(SigningKey(
                        id = "custom_$keyName",
                        alias = keyName,
                        createdDate = keystoreFile.lastModified(),
                        validityYears = 25,
                        organization = "Custom",
                        isDefaultKey = false
                    ))
                } catch (e: Exception) {
                    // Skip invalid keystores
                }
            }
        }
        keys
    }
    
    suspend fun createKey(
        alias: String,
        password: String,
        validityYears: Int,
        cn: String,
        ou: String,
        o: String,
        l: String,
        st: String,
        c: String
    ): Result<SigningKey> = withContext(Dispatchers.IO) {
        try {
            // Generate RSA key pair
            val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()
            
            // Build DN string with all provided fields
            val dnParts = mutableListOf<String>()
            if (cn.isNotBlank()) dnParts.add("CN=$cn")
            if (ou.isNotBlank()) dnParts.add("OU=$ou")
            if (o.isNotBlank()) dnParts.add("O=$o")
            if (l.isNotBlank()) dnParts.add("L=$l")
            if (st.isNotBlank()) dnParts.add("ST=$st")
            if (c.isNotBlank()) dnParts.add("C=$c")
            val dn = dnParts.joinToString(", ")
            
            // Create self-signed certificate
            val certGen = org.bouncycastle.cert.X509v3CertificateBuilder(
                org.bouncycastle.asn1.x500.X500Name(dn),
                java.math.BigInteger.valueOf(System.currentTimeMillis()),
                java.util.Date(),
                java.util.Date(System.currentTimeMillis() + validityYears * 365L * 24 * 60 * 60 * 1000),
                org.bouncycastle.asn1.x500.X500Name(dn),
                org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
            )
            
            val signer = org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.private)
            val cert = org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                .getCertificate(certGen.build(signer))
            
            // Create BKS keystore
            val keyStore = java.security.KeyStore.getInstance("BKS", "BC")
            keyStore.load(null, password.toCharArray())
            keyStore.setKeyEntry(
                alias,
                keyPair.private,
                password.toCharArray(),
                arrayOf(cert)
            )
            
            // Save keystore file
            val keystoreFile = File(keystoreDir, "$alias.keystore")
            FileOutputStream(keystoreFile).use {
                keyStore.store(it, password.toCharArray())
            }
            
            // Save password to metadata file
            val metadataFile = File(keystoreDir, "$alias.meta")
            metadataFile.writeText(password)
            
            val signingKey = SigningKey(
                id = UUID.randomUUID().toString(),
                alias = alias,
                createdDate = System.currentTimeMillis(),
                validityYears = validityYears,
                organization = o.ifBlank { cn },
                isDefaultKey = false
            )
            
            AppLogger.log("✓ Key created: $alias")
            Result.success(signingKey)
        } catch (e: Exception) {
            AppLogger.log("✗ Key creation failed: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun deleteKey(keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAllKeys(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            keystoreDir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signApk(
        apkFile: File,
        signingKey: SigningKey,
        signatureScheme: com.ripp3r.splitkiller.model.SignatureScheme
    ): File = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("Loading signing keys...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            val (privateKey, certificate) = if (signingKey.isDefaultKey) {
                loadTestKey()
            } else {
                loadCustomKey(signingKey.alias)
            }
            
            AppLogger.log("Signing APK with ${signatureScheme.name}...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            val signedFile = File(apkFile.parent, "${apkFile.nameWithoutExtension}_signed.apk")
            
            // Use Google's official apksig library
            val signerConfig = com.android.apksig.ApkSigner.SignerConfig.Builder(
                "CERT",
                privateKey,
                listOf(certificate)
            ).build()
            
            com.android.apksig.ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(apkFile)
                .setOutputApk(signedFile)
                .setCreatedBy("SplitKiller")
                .setV1SigningEnabled(signatureScheme.useV1)
                .setV2SigningEnabled(signatureScheme.useV2)
                .setV3SigningEnabled(signatureScheme.useV3)
                .build()
                .sign()
            
            if (signatureScheme.useV1) {
                AppLogger.log("✓ v1 (JAR) signature applied", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            }
            if (signatureScheme.useV2) {
                AppLogger.log("✓ v2 (APK Signature Scheme) applied", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            }
            if (signatureScheme.useV3) {
                AppLogger.log("✓ v3 (APK Signature Scheme v3) applied", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            }
            
            AppLogger.log("✓ APK signed successfully", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            signedFile
        } catch (e: Exception) {
            AppLogger.log("✗ Signing failed: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR, com.ripp3r.splitkiller.util.LogCategory.MERGE)
            e.printStackTrace()
            throw Exception("Signing failed: ${e.message}")
        }
    }
    
    private fun loadCustomKey(alias: String): Pair<PrivateKey, X509Certificate> {
        val keystoreFile = File(keystoreDir, "$alias.keystore")
        val metadataFile = File(keystoreDir, "$alias.meta")
        
        if (!keystoreFile.exists()) {
            throw Exception("Keystore file not found for $alias")
        }
        
        val password = if (metadataFile.exists()) {
            metadataFile.readText()
        } else {
            "android" // default password
        }
        
        val keyStore = java.security.KeyStore.getInstance("BKS", "BC")
        FileInputStream(keystoreFile).use {
            keyStore.load(it, password.toCharArray())
        }
        
        val key = keyStore.getKey(alias, password.toCharArray()) as PrivateKey
        val cert = keyStore.getCertificate(alias) as X509Certificate
        
        return Pair(key, cert)
    }
    
    private fun addV2V3SigningBlock(apkFile: File, privateKey: PrivateKey, certificate: X509Certificate, includeV3: Boolean) {
        val tempFile = File(apkFile.parent, "${apkFile.name}.tmp")
        
        RandomAccessFile(apkFile, "r").use { rafIn ->
            RandomAccessFile(tempFile, "rw").use { rafOut ->
                val fileSize = rafIn.length()
                
                // Find EOCD
                val eocdOffset = findEocdOffset(rafIn, fileSize)
                
                // Read CD offset from EOCD
                rafIn.seek(eocdOffset + 16)
                val cdOffsetBytes = ByteArray(4)
                rafIn.read(cdOffsetBytes)
                val cdOffset = ByteBuffer.wrap(cdOffsetBytes).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
                
                // Calculate digests
                val digests = calculateApkDigests(apkFile, cdOffset, eocdOffset, fileSize)
                
                // Create signature blocks
                val v2Block = createV2SignatureBlock(digests, privateKey, certificate)
                val v3Block = if (includeV3) createV3SignatureBlock(digests, privateKey, certificate) else null
                val signingBlock = buildApkSigningBlock(v2Block, v3Block)
                
                val newCdOffset = cdOffset + signingBlock.size
                
                // Copy data before CD
                rafIn.seek(0)
                val buffer = ByteArray(1024) // Tiny 1KB buffer
                var remaining = cdOffset
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = rafIn.read(buffer, 0, toRead)
                    if (read <= 0) break
                    rafOut.write(buffer, 0, read)
                    remaining -= read
                }
                
                // Write signing block
                rafOut.write(signingBlock)
                
                // Copy CD
                rafIn.seek(cdOffset)
                remaining = eocdOffset - cdOffset
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = rafIn.read(buffer, 0, toRead)
                    if (read <= 0) break
                    rafOut.write(buffer, 0, read)
                    remaining -= read
                }
                
                // Copy and update EOCD
                rafIn.seek(eocdOffset)
                val eocdSize = (fileSize - eocdOffset).toInt()
                val eocd = ByteArray(eocdSize)
                rafIn.read(eocd)
                
                // Update CD offset in EOCD
                ByteBuffer.wrap(eocd, 16, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(newCdOffset.toInt())
                
                rafOut.write(eocd)
            }
        }
        
        // Replace original with temp
        apkFile.delete()
        tempFile.renameTo(apkFile)
    }
    
    private fun findEocdOffset(raf: RandomAccessFile, fileSize: Long): Long {
        val searchSize = minOf(65536, fileSize).toInt()
        raf.seek(fileSize - searchSize)
        val buffer = ByteArray(searchSize)
        raf.read(buffer)
        
        for (i in buffer.size - 22 downTo 0) {
            if (buffer[i] == 0x50.toByte() && buffer[i + 1] == 0x4b.toByte() &&
                buffer[i + 2] == 0x05.toByte() && buffer[i + 3] == 0x06.toByte()) {
                return fileSize - searchSize + i
            }
        }
        throw Exception("EOCD not found")
    }
    
    private fun calculateApkDigests(apkFile: File, cdOffset: Long, eocdOffset: Long, fileSize: Long): Map<Int, ByteArray> {
        val digests = mutableMapOf<Int, ByteArray>()
        val sha256 = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024) // Tiny 1KB buffer
        
        RandomAccessFile(apkFile, "r").use { raf ->
            // Digest: Contents before CD
            raf.seek(0)
            var remaining = cdOffset
            var processed = 0L
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = raf.read(buffer, 0, toRead)
                if (read <= 0) break
                sha256.update(buffer, 0, read)
                remaining -= read
                processed += read
                
                // Log progress for large files
                if (processed % (10 * 1024 * 1024) == 0L) {
                    AppLogger.log("Calculating digest: ${processed / 1024 / 1024}MB processed", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                }
            }
            
            // Digest: Central Directory
            raf.seek(cdOffset)
            remaining = eocdOffset - cdOffset
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = raf.read(buffer, 0, toRead)
                if (read <= 0) break
                sha256.update(buffer, 0, read)
                remaining -= read
            }
            
            // Digest: EOCD (excluding CD offset field at bytes 16-19)
            raf.seek(eocdOffset)
            val eocdSize = (fileSize - eocdOffset).toInt()
            if (eocdSize > 65536) throw Exception("EOCD too large")
            val eocd = ByteArray(eocdSize)
            raf.read(eocd)
            sha256.update(eocd, 0, 16) // Before CD offset
            sha256.update(eocd, 20, eocdSize - 20) // After CD offset
        }
        
        digests[0x0103] = sha256.digest() // SHA-256 with RSA
        return digests
    }
    
    private fun createV2SignatureBlock(digests: Map<Int, ByteArray>, privateKey: PrivateKey, certificate: X509Certificate): ByteArray {
        val baos = ByteArrayOutputStream(8192) // Pre-allocate reasonable size
        
        // Digests block
        val digestsBlock = ByteArrayOutputStream(256)
        digests.forEach { (algId, digest) ->
            digestsBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(algId).array())
            digestsBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(digest.size).array())
            digestsBlock.write(digest)
        }
        val digestsBytes = digestsBlock.toByteArray()
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(digestsBytes.size).array())
        baos.write(digestsBytes)
        
        // Certificates
        val certBytes = certificate.encoded
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(certBytes.size).array())
        baos.write(certBytes)
        
        // Additional attributes (empty)
        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())
        
        val signedDataBytes = baos.toByteArray()
        
        // Sign
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(signedDataBytes)
        val signatureBytes = signature.sign()
        
        // Build signer block
        val signerBlock = ByteArrayOutputStream(signedDataBytes.size + signatureBytes.size + 256)
        signerBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signedDataBytes.size).array())
        signerBlock.write(signedDataBytes)
        
        // Signatures
        val signaturesBlock = ByteArrayOutputStream(signatureBytes.size + 16)
        signaturesBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x0103).array())
        signaturesBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signatureBytes.size).array())
        signaturesBlock.write(signatureBytes)
        val signaturesBytes = signaturesBlock.toByteArray()
        
        signerBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signaturesBytes.size).array())
        signerBlock.write(signaturesBytes)
        
        // Public key
        signerBlock.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(certBytes.size).array())
        signerBlock.write(certBytes)
        
        val signerBytes = signerBlock.toByteArray()
        
        // Final v2 block
        val v2Block = ByteArrayOutputStream(signerBytes.size + 8)
        v2Block.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signerBytes.size).array())
        v2Block.write(signerBytes)
        
        return v2Block.toByteArray()
    }
    
    private fun createV3SignatureBlock(digests: Map<Int, ByteArray>, privateKey: PrivateKey, certificate: X509Certificate): ByteArray {
        // V3 is similar to V2 but with additional rotation info
        // For simplicity, use same structure as V2
        return createV2SignatureBlock(digests, privateKey, certificate)
    }
    
    private fun buildApkSigningBlock(v2Block: ByteArray, v3Block: ByteArray?): ByteArray {
        val estimatedSize = v2Block.size + (v3Block?.size ?: 0) + 256
        val pairs = ByteArrayOutputStream(estimatedSize)
        
        // Add v2 block
        val v2Pair = ByteArrayOutputStream(v2Block.size + 8)
        v2Pair.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x7109871a).array())
        v2Pair.write(v2Block)
        val v2PairBytes = v2Pair.toByteArray()
        pairs.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v2PairBytes.size.toLong()).array())
        pairs.write(v2PairBytes)
        
        // Add v3 block if present
        if (v3Block != null) {
            val v3Pair = ByteArrayOutputStream(v3Block.size + 8)
            v3Pair.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0xf05368c0.toInt()).array())
            v3Pair.write(v3Block)
            val v3PairBytes = v3Pair.toByteArray()
            pairs.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v3PairBytes.size.toLong()).array())
            pairs.write(v3PairBytes)
        }
        
        val pairsBytes = pairs.toByteArray()
        val blockSize = pairsBytes.size.toLong() + 24
        
        val block = ByteArrayOutputStream(pairsBytes.size + 32)
        block.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(blockSize).array())
        block.write(pairsBytes)
        block.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(blockSize).array())
        block.write("APK Sig Block 42".toByteArray())
        
        return block.toByteArray()
    }
    
    private fun addV2V3Signature(apkFile: File, privateKey: PrivateKey, certificate: X509Certificate) {
        // Simplified v2/v3 signature by adding signing block
        RandomAccessFile(apkFile, "rw").use { raf ->
            val fileSize = raf.length()
            
            // Find End of Central Directory
            raf.seek(maxOf(0, fileSize - 65536))
            val buffer = ByteArray((fileSize - raf.filePointer).toInt())
            raf.read(buffer)
            
            var eocdOffset = -1L
            for (i in buffer.size - 22 downTo 0) {
                if (buffer[i] == 0x50.toByte() && buffer[i + 1] == 0x4b.toByte() &&
                    buffer[i + 2] == 0x05.toByte() && buffer[i + 3] == 0x06.toByte()) {
                    eocdOffset = raf.filePointer - buffer.size + i
                    break
                }
            }
            
            if (eocdOffset < 0) throw Exception("EOCD not found")
            
            // Read central directory offset
            raf.seek(eocdOffset + 16)
            val cdOffsetBytes = ByteArray(4)
            raf.read(cdOffsetBytes)
            val cdOffset = ByteBuffer.wrap(cdOffsetBytes).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            
            // Create simple v2 signature block
            val signatureBlock = createSimpleV2Block(apkFile, privateKey, certificate, cdOffset)
            
            // Read data before and after CD
            val beforeCD = ByteArray(cdOffset.toInt())
            raf.seek(0)
            raf.read(beforeCD)
            
            val afterCDSize = (fileSize - cdOffset).toInt()
            val afterCD = ByteArray(afterCDSize)
            raf.seek(cdOffset)
            raf.read(afterCD)
            
            // Update CD offset in EOCD
            val newCDOffset = cdOffset + signatureBlock.size
            ByteBuffer.wrap(afterCD, (eocdOffset - cdOffset + 16).toInt(), 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(newCDOffset.toInt())
            
            // Write everything back
            raf.seek(0)
            raf.write(beforeCD)
            raf.write(signatureBlock)
            raf.write(afterCD)
            raf.setLength(raf.filePointer)
        }
    }
    
    private fun createSimpleV2Block(
        apkFile: File,
        privateKey: PrivateKey,
        certificate: X509Certificate,
        cdOffset: Long
    ): ByteArray {
        // Calculate digest of APK contents (before CD)
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(apkFile).use { fis ->
            val buffer = ByteArray(8192)
            var remaining = cdOffset
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = fis.read(buffer, 0, toRead)
                if (read <= 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
        }
        val apkDigest = digest.digest()
        
        // Sign the digest
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(apkDigest)
        val signatureBytes = signature.sign()
        
        // Create APK Signing Block
        val certBytes = certificate.encoded
        val blockData = ByteArrayOutputStream().apply {
            // Signature algorithm (0x0103 = RSA with SHA-256)
            write(byteArrayOf(0x03, 0x01, 0x00, 0x00))
            // Signature
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signatureBytes.size).array())
            write(signatureBytes)
            // Certificate
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(certBytes.size).array())
            write(certBytes)
        }.toByteArray()
        
        return ByteArrayOutputStream().apply {
            val totalSize = blockData.size.toLong() + 24
            // Block size
            write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(totalSize).array())
            // ID-value pair
            write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(blockData.size.toLong() + 4).array())
            write(byteArrayOf(0x42, 0x71, 0x77, 0x74)) // APK Signature Scheme v2 Block ID
            write(blockData)
            // Block size (repeated)
            write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(totalSize).array())
            // Magic
            write("APK Sig Block 42".toByteArray())
        }.toByteArray()
    }
    
    private fun loadTestKey(): Pair<PrivateKey, X509Certificate> {
        val possibleDirs = listOf(
            File(context.filesDir, "signing_keys"),
            File(context.getExternalFilesDir(null), "signing_keys"),
            File("/storage/emulated/0/SplitKiller/signing_keys")
        )
        
        for (dir in possibleDirs) {
            if (dir.exists()) {
                try {
                    return loadKeysFromDirectory(dir)
                } catch (e: Exception) {
                    // Try next location
                }
            }
        }
        
        try {
            return loadKeysFromAssets()
        } catch (e: Exception) {
            throw Exception("Signing keys not found. Please ensure testkey.pk8 and testkey.x509.pem are available.")
        }
    }
    
    private fun loadKeysFromAssets(): Pair<PrivateKey, X509Certificate> {
        val pk8Bytes = context.assets.open("signing_keys/testkey.pk8").readBytes()
        val certBytes = context.assets.open("signing_keys/testkey.x509.pem").readBytes()
        
        val keySpec = PKCS8EncodedKeySpec(pk8Bytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(keySpec)
        
        val certFactory = CertificateFactory.getInstance("X.509")
        val certificate = certFactory.generateCertificate(certBytes.inputStream()) as X509Certificate
        
        return Pair(privateKey, certificate)
    }
    
    private fun loadKeysFromDirectory(dir: File): Pair<PrivateKey, X509Certificate> {
        val pk8File = File(dir, "testkey.pk8")
        val certFile = File(dir, "testkey.x509.pem")
        
        if (!pk8File.exists() || !certFile.exists()) {
            throw Exception("Key files not found in ${dir.absolutePath}")
        }
        
        val keyBytes = pk8File.readBytes()
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(keySpec)
        
        val certFactory = CertificateFactory.getInstance("X.509")
        val certificate = FileInputStream(certFile).use {
            certFactory.generateCertificate(it) as X509Certificate
        }
        
        return Pair(privateKey, certificate)
    }
    
    private fun signApkV1(
        inputApk: File,
        outputApk: File,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ) {
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        manifest.mainAttributes[Attributes.Name("Created-By")] = "1.0 (Android)"
        
        val tempApk = File(outputApk.parent, "${outputApk.nameWithoutExtension}_temp.apk")
        
        ZipInputStream(FileInputStream(inputApk)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                    val data = zipIn.readBytes()
                    val digest = MessageDigest.getInstance("SHA-256").digest(data)
                    val digestBase64 = Base64.encodeToString(digest, Base64.NO_WRAP)
                    
                    val attrs = Attributes()
                    attrs[Attributes.Name("SHA-256-Digest")] = digestBase64
                    manifest.entries[entry.name] = attrs
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        
        val manifestBytes = ByteArrayOutputStream().apply {
            manifest.write(this)
        }.toByteArray()
        
        val sf = createSignatureFile(manifestBytes, manifest)
        val sfBytes = ByteArrayOutputStream().apply {
            sf.write(this)
        }.toByteArray()
        
        val signatureBytes = createPKCS7Signature(sfBytes, privateKey, certificate)
        
        ZipOutputStream(FileOutputStream(tempApk)).use { zipOut ->
            ZipInputStream(FileInputStream(inputApk)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.name.startsWith("META-INF/")) {
                        zipOut.putNextEntry(ZipEntry(entry.name))
                        zipIn.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            zipOut.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zipOut.write(manifestBytes)
            zipOut.closeEntry()
            
            zipOut.putNextEntry(ZipEntry("META-INF/CERT.SF"))
            zipOut.write(sfBytes)
            zipOut.closeEntry()
            
            zipOut.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
            zipOut.write(signatureBytes)
            zipOut.closeEntry()
        }
        
        tempApk.renameTo(outputApk)
    }
    
    private fun createSignatureFile(manifestBytes: ByteArray, manifest: Manifest): Manifest {
        val sf = Manifest()
        sf.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        sf.mainAttributes[Attributes.Name("Created-By")] = "1.0 (Android)"
        
        val mfDigest = MessageDigest.getInstance("SHA-256").digest(manifestBytes)
        sf.mainAttributes[Attributes.Name("SHA-256-Digest-Manifest")] = 
            Base64.encodeToString(mfDigest, Base64.NO_WRAP)
        
        for ((name, attrs) in manifest.entries) {
            val entryText = "Name: $name\r\nSHA-256-Digest: ${attrs[Attributes.Name("SHA-256-Digest")]}\r\n\r\n"
            val entryDigest = MessageDigest.getInstance("SHA-256").digest(entryText.toByteArray())
            
            val sfAttrs = Attributes()
            sfAttrs[Attributes.Name("SHA-256-Digest")] = 
                Base64.encodeToString(entryDigest, Base64.NO_WRAP)
            sf.entries[name] = sfAttrs
        }
        
        return sf
    }
    
    private fun createPKCS7Signature(
        data: ByteArray,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): ByteArray {
        val certList = listOf(certificate)
        val certs = JcaCertStore(certList)
        
        val gen = CMSSignedDataGenerator()
        val sha256Signer = JcaContentSignerBuilder("SHA256withRSA").build(privateKey)
        
        val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
            JcaDigestCalculatorProviderBuilder().build()
        ).build(sha256Signer, certificate)
        
        gen.addSignerInfoGenerator(signerInfoGenerator)
        gen.addCertificates(certs)
        
        val msg = CMSProcessableByteArray(data)
        val signedData = gen.generate(msg, false)
        
        return signedData.encoded
    }
    
    private fun getDefaultTestKey(): SigningKey {
        return SigningKey(
            id = "default_test_key",
            alias = "testkey",
            createdDate = 0L,
            validityYears = 30,
            organization = "Android",
            isDefaultKey = true
        )
    }
}

