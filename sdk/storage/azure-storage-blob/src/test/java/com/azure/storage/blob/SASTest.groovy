// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob

import com.azure.storage.blob.models.AccessPolicy
import com.azure.storage.blob.models.BlobRange
import com.azure.storage.blob.models.SignedIdentifier
import com.azure.storage.blob.models.StorageException
import com.azure.storage.blob.models.UserDelegationKey
import com.azure.storage.common.AccountSASPermission
import com.azure.storage.common.AccountSASResourceType
import com.azure.storage.common.AccountSASService
import com.azure.storage.common.AccountSASSignatureValues
import com.azure.storage.common.Constants
import com.azure.storage.common.IPRange
import com.azure.storage.common.SASProtocol
import com.azure.storage.common.Utility
import com.azure.storage.common.credentials.SASTokenCredential
import com.azure.storage.common.credentials.SharedKeyCredential
import spock.lang.Ignore
import spock.lang.Unroll

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SASTest extends APISpec {

    @Unroll
    def "Blob range"() {
        expect:
        if (count == null) {
            assert new BlobRange(offset).toHeaderValue() == result
        } else {
            assert new BlobRange(offset, count).toHeaderValue() == result
        }


        where:
        offset | count || result
        0      | null  || null
        0      | 5     || "bytes=0-4"
        5      | 10    || "bytes=5-14"
    }

    @Unroll
    def "Blob range IA"() {
        when:
        new BlobRange(offset, count)

        then:
        thrown(IllegalArgumentException)

        where:
        offset | count
        -1     | 5
        0      | -1
    }

    def "BlobClient getSnapshot"() {
        setup:
        def data = "test".getBytes()
        def blobName = generateBlobName()
        def bu = cc.getBlockBlobClient(blobName)
        bu.upload(new ByteArrayInputStream(data), data.length)
        def snapshotId = bu.createSnapshot().getSnapshotId()

        when:
        def snapshotBlob = cc.getBlockBlobClient(blobName, snapshotId)

        then:
        snapshotBlob.getSnapshotId() == snapshotId
        bu.getSnapshotId() == null
    }

    def "BlobClient isSnapshot"() {
        setup:
        def data = "test".getBytes()
        def blobName = generateBlobName()
        def bu = cc.getBlockBlobClient(blobName)
        bu.upload(new ByteArrayInputStream(data), data.length)
        def snapshotId = bu.createSnapshot().getSnapshotId()

        when:
        def snapshotBlob = cc.getBlockBlobClient(blobName, snapshotId)

        then:
        snapshotBlob.isSnapshot()
        !bu.isSnapshot()

    }

    def "serviceSASSignatureValues network test blob"() {
        setup:
        def data = "test".getBytes()
        def blobName = generateBlobName()
        def bu = getBlobClient(primaryCredential, cc.getContainerUrl().toString(), blobName).asBlockBlobClient()
        bu.upload(new ByteArrayInputStream(data), data.length)

        def permissions = new BlobSASPermission()
            .setReadPermission(true)
            .setWritePermission(true)
            .setCreatePermission(true)
            .getDeletePermission(true)
            .setAddPermission(true)
        def startTime = getUTCNow().minusDays(1)
        def expiryTime = getUTCNow().plusDays(1)
        def ipRange = new IPRange()
            .setIpMin("0.0.0.0")
            .setIpMax("255.255.255.255")
        def sasProtocol = SASProtocol.HTTPS_HTTP
        def cacheControl = "cache"
        def contentDisposition = "disposition"
        def contentEncoding = "encoding"
        def contentLanguage = "language"
        def contentType = "type"

        when:
        def sas = bu.generateSAS(null, permissions, expiryTime, startTime, null, sasProtocol, ipRange, cacheControl, contentDisposition, contentEncoding, contentLanguage, contentType)

        def client = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName).asBlockBlobClient()

        def os = new ByteArrayOutputStream()
        client.download(os)
        def properties = client.getProperties()

        then:
        os.toString() == new String(data)
        properties.getCacheControl() == "cache"
        properties.getContentDisposition() == "disposition"
        properties.getContentEncoding() == "encoding"
        properties.getContentLanguage() == "language"
        notThrown(StorageException)
    }

    def "serviceSASSignatureValues network test blob snapshot"() {
        setup:

        def data = "test".getBytes()
        def blobName = generateBlobName()
        def bu = getBlobClient(primaryCredential, cc.getContainerUrl().toString(), blobName).asBlockBlobClient()
        bu.upload(new ByteArrayInputStream(data), data.length)
        String snapshotId = bu.createSnapshot().getSnapshotId()

        def snapshotBlob = cc.getBlockBlobClient(blobName, snapshotId)

        def permissions = new BlobSASPermission()
            .setReadPermission(true)
            .setWritePermission(true)
            .setCreatePermission(true)
            .getDeletePermission(true)
            .setAddPermission(true)
        def startTime = getUTCNow().minusDays(1)
        def expiryTime = getUTCNow().plusDays(1)
        def ipRange = new IPRange()
            .setIpMin("0.0.0.0")
            .setIpMax("255.255.255.255")
        def sasProtocol = SASProtocol.HTTPS_HTTP
        def cacheControl = "cache"
        def contentDisposition = "disposition"
        def contentEncoding = "encoding"
        def contentLanguage = "language"
        def contentType = "type"

        when:
        def sas = snapshotBlob.generateSAS(null, permissions, expiryTime, startTime, null, sasProtocol, ipRange, cacheControl, contentDisposition, contentEncoding, contentLanguage, contentType)

        def client = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName, snapshotId).asBlockBlobClient()

        def os = new ByteArrayOutputStream()
        client.download(os)
        def properties = client.getProperties()

        then:
        os.toString() == new String(data)
        properties.getCacheControl() == "cache"
        properties.getContentDisposition() == "disposition"
        properties.getContentEncoding() == "encoding"
        properties.getContentLanguage() == "language"
    }

    def "serviceSASSignatureValues network test container"() {
        setup:
        def identifier = new SignedIdentifier()
            .setId("0000")
            .setAccessPolicy(new AccessPolicy().setPermission("racwdl")
                .setExpiry(getUTCNow().plusDays(1)))
        cc.setAccessPolicy(null, Arrays.asList(identifier))

        // Check containerSASPermissions
        ContainerSASPermission permissions = new ContainerSASPermission()
            .setRead(true)
            .setWrite(true)
            .setList(true)
            .setCreate(true)
            .setDelete(true)
            .setAdd(true)
            .setList(true)

        OffsetDateTime expiryTime = getUTCNow().plusDays(1)

        when:
        String sasWithId = cc.generateSAS(identifier.getId())

        ContainerClient client1 = getContainerClient(SASTokenCredential.fromSASTokenString(sasWithId), cc.getContainerUrl().toString())

        client1.listBlobsFlat().iterator().hasNext()

        String sasWithPermissions = cc.generateSAS(permissions, expiryTime)

        ContainerClient client2 = getContainerClient(SASTokenCredential.fromSASTokenString(sasWithPermissions), cc.getContainerUrl().toString())

        client2.listBlobsFlat().iterator().hasNext()

        then:
        notThrown(StorageException)
    }

    /* TODO: Fix user delegation tests to run in CI */
    @Ignore
    def "serviceSASSignatureValues network test blob user delegation"() {
        setup:
        byte[] data = "test".getBytes()
        String blobName = generateBlobName()
        BlockBlobClient bu = cc.getBlockBlobClient(blobName)
        bu.upload(new ByteArrayInputStream(data), data.length)

        BlobSASPermission permissions = new BlobSASPermission()
            .setReadPermission(true)
            .setWritePermission(true)
            .setCreatePermission(true)
            .getDeletePermission(true)
            .setAddPermission(true)

        OffsetDateTime startTime = getUTCNow().minusDays(1)
        OffsetDateTime expiryTime = getUTCNow().plusDays(1)

        IPRange ipRange = new IPRange()
            .setIpMin("0.0.0.0")
            .setIpMax("255.255.255.255")

        SASProtocol sasProtocol = SASProtocol.HTTPS_HTTP
        String cacheControl = "cache"
        String contentDisposition = "disposition"
        String contentEncoding = "encoding"
        String contentLanguage = "language"
        String contentType = "type"

        UserDelegationKey key = getOAuthServiceClient().getUserDelegationKey(null, expiryTime)

        when:
        String sas = bu.generateUserDelegationSAS(key, primaryCredential.getAccountName(), permissions, expiryTime, startTime, key.getSignedVersion(), sasProtocol, ipRange, cacheControl, contentDisposition, contentEncoding, contentLanguage, contentType)

        then:
        sas != null

        when:
        BlockBlobClient client = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName).asBlockBlobClient()

        OutputStream os = new ByteArrayOutputStream()
        client.download(os)
        BlobProperties properties = client.getProperties()

        then:
        os.toString() == new String(data)
        properties.getCacheControl() == "cache"
        properties.getContentDisposition() == "disposition"
        properties.getContentEncoding() == "encoding"
        properties.getContentLanguage() == "language"
        notThrown(StorageException)
    }

    def "BlobServiceSAS network test blob snapshot"() {
        setup:
        String containerName = generateContainerName()
        String blobName = generateBlobName()
        ContainerClient containerClient = primaryBlobServiceClient.createContainer(containerName)
        BlockBlobClient blobClient = containerClient.getBlockBlobClient(blobName)
        blobClient.upload(defaultInputStream.get(), defaultDataSize) // need something to snapshot
        BlockBlobClient snapshotBlob = blobClient.createSnapshot().asBlockBlobClient()
        String snapshotId = snapshotBlob.getSnapshotId()

        BlobSASPermission permissions = new BlobSASPermission()
            .setReadPermission(true)
            .setWritePermission(true)
            .setCreatePermission(true)
            .getDeletePermission(true)
            .setAddPermission(true)
        OffsetDateTime startTime = getUTCNow().minusDays(1)
        OffsetDateTime expiryTime = getUTCNow().plusDays(1)
        IPRange ipRange = new IPRange()
            .setIpMin("0.0.0.0")
            .setIpMax("255.255.255.255")
        SASProtocol sasProtocol = SASProtocol.HTTPS_HTTP
        String cacheControl = "cache"
        String contentDisposition = "disposition"
        String contentEncoding = "encoding"
        String contentLanguage = "language"
        String contentType = "type"

        when:
        String sas = snapshotBlob.generateSAS(null, permissions, expiryTime, startTime, null, sasProtocol, ipRange, cacheControl, contentDisposition, contentEncoding, contentLanguage, contentType)

        and:
        AppendBlobClient client = getBlobClient(SASTokenCredential.fromSASTokenString(sas), containerClient.getContainerUrl().toString(), blobName).asAppendBlobClient()

        client.download(new ByteArrayOutputStream())

        then:
        thrown(StorageException)

        when:
        AppendBlobClient snapClient = getBlobClient(SASTokenCredential.fromSASTokenString(sas), containerClient.getContainerUrl().toString(), blobName, snapshotId).asAppendBlobClient()

        ByteArrayOutputStream data = new ByteArrayOutputStream()
        snapClient.download(data)

        then:
        notThrown(StorageException)
        data.toByteArray() == defaultData.array()

        and:
        BlobProperties properties = snapClient.getProperties()

        then:
        properties.getCacheControl() == "cache"
        properties.getContentDisposition() == "disposition"
        properties.getContentEncoding() == "encoding"
        properties.getContentLanguage() == "language"

    }

    @Ignore
    def "serviceSASSignatureValues network test blob snapshot user delegation"() {
        setup:
        byte[] data = "test".getBytes()
        String blobName = generateBlobName()
        BlockBlobClient bu = cc.getBlockBlobClient(blobName)
        bu.upload(new ByteArrayInputStream(data), data.length)
        BlockBlobClient snapshotBlob = bu.createSnapshot().asBlockBlobClient()
        String snapshotId = snapshotBlob.getSnapshotId()

        BlobSASPermission permissions = new BlobSASPermission()
            .setReadPermission(true)
            .setWritePermission(true)
            .setCreatePermission(true)
            .getDeletePermission(true)
            .setAddPermission(true)

        OffsetDateTime startTime = getUTCNow().minusDays(1)
        OffsetDateTime expiryTime = getUTCNow().plusDays(1)

        IPRange ipRange = new IPRange()
            .setIpMin("0.0.0.0")
            .setIpMax("255.255.255.255")

        SASProtocol sasProtocol = SASProtocol.HTTPS_HTTP
        String cacheControl = "cache"
        String contentDisposition = "disposition"
        String contentEncoding = "encoding"
        String contentLanguage = "language"
        String contentType = "type"

        UserDelegationKey key = getOAuthServiceClient().getUserDelegationKey(startTime, expiryTime)

        when:
        String sas = snapshotBlob.generateUserDelegationSAS(key, primaryCredential.getAccountName(), permissions, expiryTime, startTime, key.getSignedVersion(), sasProtocol, ipRange, cacheControl, contentDisposition, contentEncoding, contentLanguage, contentType)

        // base blob with snapshot SAS
        BlockBlobClient client1 = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName).asBlockBlobClient()
        client1.download(new ByteArrayOutputStream())

        then:
        // snapshot-level SAS shouldn't be able to access base blob
        thrown(StorageException)

        when:
        // blob snapshot with snapshot SAS
        BlockBlobClient client2 = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName, snapshotId).asBlockBlobClient()
        OutputStream os = new ByteArrayOutputStream()
        client2.download(os)

        then:
        notThrown(StorageException)
        os.toString() == new String(data)

        and:
        def properties = client2.getProperties()

        then:
        properties.getCacheControl() == "cache"
        properties.getContentDisposition() == "disposition"
        properties.getContentEncoding() == "encoding"
        properties.getContentLanguage() == "language"
    }

    @Ignore
    def "serviceSASSignatureValues network test container user delegation"() {
        setup:
        ContainerSASPermission permissions = new ContainerSASPermission()
            .setRead(true)
            .setWrite(true)
            .setCreate(true)
            .setDelete(true)
            .setAdd(true)
            .setList(true)

        OffsetDateTime expiryTime = getUTCNow().plusDays(1)

        UserDelegationKey key = getOAuthServiceClient().getUserDelegationKey(null, expiryTime)

        when:
        String sasWithPermissions = cc.generateUserDelegationSAS(key, primaryCredential.getAccountName(), permissions, expiryTime)

        ContainerClient client = getContainerClient(SASTokenCredential.fromSASTokenString(sasWithPermissions), cc.getContainerUrl().toString())
        client.listBlobsFlat().iterator().hasNext()

        then:
        notThrown(StorageException)
    }

    def "accountSAS network test blob read"() {
        setup:
        def data = "test".getBytes()
        def blobName = generateBlobName()
        def bu = cc.getBlockBlobClient(blobName)
        bu.upload(new ByteArrayInputStream(data), data.length)

        def service = new AccountSASService()
            .setBlob(true)
        def resourceType = new AccountSASResourceType()
            .setContainer(true)
            .setService(true)
            .setObject(true)
        def permissions = new AccountSASPermission()
            .setReadPermission(true)
        def expiryTime = getUTCNow().plusDays(1)

        when:
        def sas = primaryBlobServiceClient.generateAccountSAS(service, resourceType, permissions, expiryTime, null, null, null, null)

        def client = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName).asBlockBlobClient()
        def os = new ByteArrayOutputStream()
        client.download(os)

        then:
        os.toString() == new String(data)
    }

    def "accountSAS network test blob delete fails"() {
        setup:
        def data = "test".getBytes()
        def blobName = generateBlobName()
        def bu = cc.getBlockBlobClient(blobName)
        bu.upload(new ByteArrayInputStream(data), data.length)

        def service = new AccountSASService()
            .setBlob(true)
        def resourceType = new AccountSASResourceType()
            .setContainer(true)
            .setService(true)
            .setObject(true)
        def permissions = new AccountSASPermission()
            .setReadPermission(true)
        def expiryTime = getUTCNow().plusDays(1)

        when:
        def sas = primaryBlobServiceClient.generateAccountSAS(service, resourceType, permissions, expiryTime, null, null, null, null)

        def client = getBlobClient(SASTokenCredential.fromSASTokenString(sas), cc.getContainerUrl().toString(), blobName).asBlockBlobClient()
        client.delete()

        then:
        thrown(StorageException)
    }

    def "accountSAS network create container fails"() {
        setup:
        def service = new AccountSASService()
            .setBlob(true)
        def resourceType = new AccountSASResourceType()
            .setContainer(true)
            .setService(true)
            .setObject(true)
        def permissions = new AccountSASPermission()
            .setReadPermission(true)
            .setCreatePermission(false)
        def expiryTime = getUTCNow().plusDays(1)

        when:
        def sas = primaryBlobServiceClient.generateAccountSAS(service, resourceType, permissions, expiryTime, null, null, null, null)

        def sc = getServiceClient(SASTokenCredential.fromSASTokenString(sas), primaryBlobServiceClient.getAccountUrl().toString())
        sc.createContainer(generateContainerName())

        then:
        thrown(StorageException)
    }

    def "accountSAS network create container succeeds"() {
        setup:
        def service = new AccountSASService()
            .setBlob(true)
        def resourceType = new AccountSASResourceType()
            .setContainer(true)
            .setService(true)
            .setObject(true)
        def permissions = new AccountSASPermission()
            .setReadPermission(true)
            .setCreatePermission(true)
        def expiryTime = getUTCNow().plusDays(1)

        when:
        def sas = primaryBlobServiceClient.generateAccountSAS(service, resourceType, permissions, expiryTime, null, null, null, null)

        def sc = getServiceClient(SASTokenCredential.fromSASTokenString(sas), primaryBlobServiceClient.getAccountUrl().toString())
        sc.createContainer(generateContainerName())

        then:
        notThrown(StorageException)
    }

    /*
     This test will ensure that each field gets placed into the proper location within the string to sign and that null
     values are handled correctly. We will validate the whole SAS with service calls as well as correct serialization of
     individual parts later.
     */

    @Unroll
    def "serviceSasSignatures string to sign"() {
        when:
        BlobServiceSASSignatureValues v = new BlobServiceSASSignatureValues()
        def p = new BlobSASPermission()
        p.setReadPermission(true)
        v.setPermissions(p.toString())

        v.setStartTime(startTime)
        def e = OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        v.setExpiryTime(e)

        v.setCanonicalName("containerName/blobName")
            .setSnapshotId(snapId)
        if (ipRange != null) {
            def ipR = new IPRange()
            ipR.setIpMin("ip")
            v.setIpRange(ipR)
        }
        v.setIdentifier(identifier)
            .setProtocol(protocol)
            .setCacheControl(cacheControl)
            .setContentDisposition(disposition)
            .setContentEncoding(encoding)
            .setContentLanguage(language)
            .setContentType(type)
        v.setResource("bs")

        def token = v.generateSASQueryParameters(primaryCredential)
        then:
        token.getSignature() == primaryCredential.computeHmac256(expectedStringToSign)

        /*
        We don't test the blob or containerName properties because canonicalized resource is always added as at least
        /blob/accountName. We test canonicalization of resources later. Again, this is not to test a fully functional
        sas but the construction of the string to sign.
        Signed resource is tested elsewhere, as we work some minor magic in choosing which value to use.
         */
        where:
        startTime                                                 | identifier | ipRange       | protocol               | snapId   | cacheControl | disposition   | encoding   | language   | type   || expectedStringToSign
        OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | null       | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | "id"       | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\nid\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null       | new IPRange() | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\nip\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null       | null          | SASProtocol.HTTPS_ONLY | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n" + SASProtocol.HTTPS_ONLY + "\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null       | null          | null                   | "snapId" | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\nsnapId\n\n\n\n\n"
        null                                                      | null       | null          | null                   | null     | "control"    | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\ncontrol\n\n\n\n"
        null                                                      | null       | null          | null                   | null     | null         | "disposition" | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\ndisposition\n\n\n"
        null                                                      | null       | null          | null                   | null     | null         | null          | "encoding" | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\nencoding\n\n"
        null                                                      | null       | null          | null                   | null     | null         | null          | null       | "language" | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\nlanguage\n"
        null                                                      | null       | null          | null                   | null     | null         | null          | null       | null       | "type" || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\ntype"
    }

    @Unroll
    def "serviceSasSignatures string to sign user delegation key"() {
        when:
        def v = new BlobServiceSASSignatureValues()

        def p = new BlobSASPermission()
        p.setReadPermission(true)
        v.setPermissions(p.toString())

        v.setStartTime(startTime)
        def e = OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        v.setExpiryTime(e)

        v.setCanonicalName("containerName/blobName")
            .setSnapshotId(snapId)
        if (ipRange != null) {
            def ipR = new IPRange()
            ipR.setIpMin("ip")
            v.setIpRange(ipR)
        }
        v.setProtocol(protocol)
            .setCacheControl(cacheControl)
            .setContentDisposition(disposition)
            .setContentEncoding(encoding)
            .setContentLanguage(language)
            .setContentType(type)
        v.setResource("bs")
        def key = new UserDelegationKey()
            .setSignedOid(keyOid)
            .setSignedTid(keyTid)
            .setSignedStart(keyStart)
            .setSignedExpiry(keyExpiry)
            .setSignedService(keyService)
            .setSignedVersion(keyVersion)
            .setValue(keyValue)
        def token = v.generateSASQueryParameters(key)

        then:
        token.getSignature() == Utility.computeHMac256(key.getValue(), expectedStringToSign)

        /*
        We test string to sign functionality directly related to user delegation sas specific parameters
         */
        where:
        startTime                                                 | keyOid                                 | keyTid                                 | keyStart                                                              | keyExpiry                                                             | keyService | keyVersion   | keyValue                                       | ipRange       | protocol               | snapId   | cacheControl | disposition   | encoding   | language   | type   || expectedStringToSign
        OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | "11111111-1111-1111-1111-111111111111" | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n11111111-1111-1111-1111-111111111111\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | "22222222-2222-2222-2222-222222222222" | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n22222222-2222-2222-2222-222222222222\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | OffsetDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0), ZoneOffset.UTC) | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n2018-01-01T00:00:00Z\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | OffsetDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0), ZoneOffset.UTC) | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n2018-01-01T00:00:00Z\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | "b"        | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\nb\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | "2018-06-17" | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n2018-06-17\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | new IPRange() | null                   | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\nip\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | SASProtocol.HTTPS_ONLY | null     | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n" + SASProtocol.HTTPS_ONLY + "\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | "snapId" | null         | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\nsnapId\n\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | "control"    | null          | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\ncontrol\n\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | "disposition" | null       | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\ndisposition\n\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | "encoding" | null       | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\nencoding\n\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | "language" | null   || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\nlanguage\n"
        null                                                      | null                                   | null                                   | null                                                                  | null                                                                  | null       | null         | "3hd4LRwrARVGbeMRQRfTLIsGMkCPuZJnvxZDU7Gak8c=" | null          | null                   | null     | null         | null          | null       | null       | "type" || "r\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\ncontainerName/blobName\n\n\n\n\n\n\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\nbs\n\n\n\n\n\ntype"
    }

    def "serviceSASSignatureValues canonicalizedResource"() {
        setup:
        def blobName = generateBlobName()
        def accountName = "account"
        def bu = cc.getBlockBlobClient(blobName)

        when:
        def serviceSASSignatureValues = bu.blockBlobAsyncClient.configureServiceSASSignatureValues(new BlobServiceSASSignatureValues(), accountName)

        then:
        serviceSASSignatureValues.getCanonicalName() == "/blob/" + accountName + cc.containerUrl.path + "/" + blobName
    }

    @Unroll
    def "serviceSasSignatureValues IA"() {
        setup:
        def v = new BlobServiceSASSignatureValues()
            .setSnapshotId("2018-01-01T00:00:00.0000000Z")
            .setVersion(version)

        when:
        v.generateSASQueryParameters((SharedKeyCredential) creds)

        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage().contains(parameter)

        where:
        version | creds             || parameter
        null    | primaryCredential || "version"
        "v"     | null              || "sharedKeyCredentials"
    }

    @Unroll
    def "BlobSASPermissions toString"() {
        setup:
        def perms = new BlobSASPermission()
            .setReadPermission(read)
            .setWritePermission(write)
            .getDeletePermission(delete)
            .setCreatePermission(create)
            .setAddPermission(add)

        expect:
        perms.toString() == expectedString

        where:
        read  | write | delete | create | add   || expectedString
        true  | false | false  | false  | false || "r"
        false | true  | false  | false  | false || "w"
        false | false | true   | false  | false || "d"
        false | false | false  | true   | false || "c"
        false | false | false  | false  | true  || "a"
        true  | true  | true   | true   | true  || "racwd"
    }

    @Unroll
    def "BlobSASPermissions parse"() {
        when:
        def perms = BlobSASPermission.parse(permString)

        then:
        perms.getReadPermission() == read
        perms.getWritePermission() == write
        perms.getDeletePermission() == delete
        perms.getCreatePermission() == create
        perms.getAddPermission() == add

        where:
        permString || read  | write | delete | create | add
        "r"        || true  | false | false  | false  | false
        "w"        || false | true  | false  | false  | false
        "d"        || false | false | true   | false  | false
        "c"        || false | false | false  | true   | false
        "a"        || false | false | false  | false  | true
        "racwd"    || true  | true  | true   | true   | true
        "dcwra"    || true  | true  | true   | true   | true
    }

    def "BlobSASPermissions parse IA"() {
        when:
        BlobSASPermission.parse("rwaq")

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "ContainerSASPermissions toString"() {
        setup:
        def perms = new ContainerSASPermission()
            .setRead(read)
            .setWrite(write)
            .setDelete(delete)
            .setCreate(create)
            .setAdd(add)
            .setList(list)

        expect:
        perms.toString() == expectedString

        where:
        read  | write | delete | create | add   | list  || expectedString
        true  | false | false  | false  | false | false || "r"
        false | true  | false  | false  | false | false || "w"
        false | false | true   | false  | false | false || "d"
        false | false | false  | true   | false | false || "c"
        false | false | false  | false  | true  | false || "a"
        false | false | false  | false  | false | true  || "l"
        true  | true  | true   | true   | true  | true  || "racwdl"
    }

    @Unroll
    def "ContainerSASPermissions parse"() {
        when:
        def perms = ContainerSASPermission.parse(permString)

        then:
        perms.getRead() == read
        perms.getWrite() == write
        perms.getDelete() == delete
        perms.getCreate() == create
        perms.getAdd() == add
        perms.getList() == list

        where:
        permString || read  | write | delete | create | add   | list
        "r"        || true  | false | false  | false  | false | false
        "w"        || false | true  | false  | false  | false | false
        "d"        || false | false | true   | false  | false | false
        "c"        || false | false | false  | true   | false | false
        "a"        || false | false | false  | false  | true  | false
        "l"        || false | false | false  | false  | false | true
        "racwdl"   || true  | true  | true   | true   | true  | true
        "dcwrla"   || true  | true  | true   | true   | true  | true
    }

    def "ContainerSASPermissions parse IA"() {
        when:
        ContainerSASPermission.parse("rwaq")

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "IPRange toString"() {
        setup:
        def ip = new IPRange()
            .setIpMin(min)
            .setIpMax(max)

        expect:
        ip.toString() == expectedString

        where:
        min  | max  || expectedString
        "a"  | "b"  || "a-b"
        "a"  | null || "a"
        null | "b"  || ""
    }

    @Unroll
    def "IPRange parse"() {
        when:
        def ip = IPRange.parse(rangeStr)

        then:
        ip.getIpMin() == min
        ip.getIpMax() == max

        where:
        rangeStr || min | max
        "a-b"    || "a" | "b"
        "a"      || "a" | null
        ""       || ""  | null
    }

    @Unroll
    def "SASProtocol parse"() {
        expect:
        SASProtocol.parse(protocolStr) == protocol

        where:
        protocolStr  || protocol
        "https"      || SASProtocol.HTTPS_ONLY
        "https,http" || SASProtocol.HTTPS_HTTP
    }

    @Unroll
    def "ServiceSASSignatureValues assertGenerateOk"() {
        when:
        BlobServiceSASSignatureValues serviceSASSignatureValues = new BlobServiceSASSignatureValues()
        serviceSASSignatureValues.setVersion(version)
        serviceSASSignatureValues.setCanonicalName(canonicalName)
        serviceSASSignatureValues.setExpiryTime(expiryTime)
        serviceSASSignatureValues.setPermissions(permissions)
        serviceSASSignatureValues.setIdentifier(identifier)
        serviceSASSignatureValues.setResource(resource)
        serviceSASSignatureValues.setSnapshotId(snapshotId)

        if (usingUserDelegation) {
            serviceSASSignatureValues.generateSASQueryParameters(new UserDelegationKey())
        } else {
            serviceSASSignatureValues.generateSASQueryParameters(new SharedKeyCredential("", ""))
        }

        then:

        thrown(IllegalArgumentException)

        where:
        usingUserDelegation | version                                          | canonicalName            | expiryTime                                                | permissions                                   | identifier | resource | snapshotId
        false               | null                                             | null                     | null                                                      | null                                          | null       | null     | null
        false               | Constants.HeaderConstants.TARGET_STORAGE_VERSION | null                     | null                                                      | null                                          | null       | null     | null
        false               | Constants.HeaderConstants.TARGET_STORAGE_VERSION | "containerName/blobName" | null                                                      | null                                                       | null   | null | null
        false               | Constants.HeaderConstants.TARGET_STORAGE_VERSION | "containerName/blobName" | OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | null                                                       | null   | null | null
        false               | Constants.HeaderConstants.TARGET_STORAGE_VERSION | "containerName/blobName" | null                                                      | new BlobSASPermission().setReadPermission(true).toString() | null   | null | null
        false               | null                                             | null                     | null                                                      | null                                                       | "0000" | "c"  | "id"
    }

    // TODO : Account SAS should go into the common package
    /*
     This test will ensure that each field gets placed into the proper location within the string to sign and that null
     values are handled correctly. We will validate the whole SAS with service calls as well as correct serialization of
     individual parts later.
     */

    @Unroll
    def "accountSasSignatures string to sign"() {
        when:
        def v = new AccountSASSignatureValues()
        def p = new AccountSASPermission()
            .setReadPermission(true)
        v.setPermissions(p.toString())
            .setServices("b")
            .setResourceTypes("o")
            .setStartTime(startTime)
            .setExpiryTime(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
        if (ipRange != null) {
            def ipR = new IPRange()
            ipR.setIpMin("ip")
            v.setIpRange(ipR)
        }
        v.setProtocol(protocol)

        def token = v.generateSASQueryParameters(primaryCredential)

        then:
        token.getSignature() == primaryCredential.computeHmac256(String.format(expectedStringToSign, primaryCredential.getAccountName()))

        where:
        startTime                                                 | ipRange       | protocol               || expectedStringToSign
        OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | null          | null                   || "%s" + "\nr\nb\no\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\n\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\n"
        null                                                      | new IPRange() | null                   || "%s" + "\nr\nb\no\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\nip\n\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\n"
        null                                                      | null          | SASProtocol.HTTPS_ONLY || "%s" + "\nr\nb\no\n\n" + Utility.ISO_8601_UTC_DATE_FORMATTER.format(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) + "\n\n" + SASProtocol.HTTPS_ONLY + "\n" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "\n"
    }

    @Unroll
    def "accountSasSignatureValues IA"() {
        setup:
        def v = new AccountSASSignatureValues()
            .setPermissions(permissions)
            .setServices(service)
            .setResourceTypes(resourceType)
            .setExpiryTime(expiryTime)
            .setVersion(version)

        when:
        v.generateSASQueryParameters(creds)

        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage().contains(parameter)

        where:
        permissions | service | resourceType | expiryTime                                                | version | creds             || parameter
        null        | "b"     | "c"          | OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | "v"     | primaryCredential || "permissions"
        "c"         | null    | "c"          | OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | "v"     | primaryCredential || "services"
        "c"         | "b"     | null         | OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | "v"     | primaryCredential || "resourceTypes"
        "c"         | "b"     | "c"          | null                                                      | "v"     | primaryCredential || "expiryTime"
        "c"         | "b"     | "c"          | OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | null    | primaryCredential || "version"
        "c"         | "b"     | "c"          | OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC) | "v"     | null              || "SharedKeyCredential"
    }

    @Unroll
    def "AccountSASPermissions toString"() {
        setup:
        def perms = new AccountSASPermission()
        perms.setReadPermission(read)
            .setWritePermission(write)
            .setDeletePermission(delete)
            .setListPermission(list)
            .setAddPermission(add)
            .setCreatePermission(create)
            .setUpdatePermission(update)
            .setProcessMessages(process)

        expect:
        perms.toString() == expectedString

        where:
        read  | write | delete | list  | add   | create | update | process || expectedString
        true  | false | false  | false | false | false  | false  | false   || "r"
        false | true  | false  | false | false | false  | false  | false   || "w"
        false | false | true   | false | false | false  | false  | false   || "d"
        false | false | false  | true  | false | false  | false  | false   || "l"
        false | false | false  | false | true  | false  | false  | false   || "a"
        false | false | false  | false | false | true   | false  | false   || "c"
        false | false | false  | false | false | false  | true   | false   || "u"
        false | false | false  | false | false | false  | false  | true    || "p"
        true  | true  | true   | true  | true  | true   | true   | true    || "rwdlacup"
    }

    @Unroll
    def "AccountSASPermissions parse"() {
        when:
        def perms = AccountSASPermission.parse(permString)

        then:
        perms.getReadPermission() == read
        perms.getWritePermission() == write
        perms.getDeletePermission() == delete
        perms.getListPermission() == list
        perms.getAddPermission() == add
        perms.getCreatePermission() == create
        perms.getUpdatePermission() == update
        perms.getProcessMessages() == process

        where:
        permString || read  | write | delete | list  | add   | create | update | process
        "r"        || true  | false | false  | false | false | false  | false  | false
        "w"        || false | true  | false  | false | false | false  | false  | false
        "d"        || false | false | true   | false | false | false  | false  | false
        "l"        || false | false | false  | true  | false | false  | false  | false
        "a"        || false | false | false  | false | true  | false  | false  | false
        "c"        || false | false | false  | false | false | true   | false  | false
        "u"        || false | false | false  | false | false | false  | true   | false
        "p"        || false | false | false  | false | false | false  | false  | true
        "rwdlacup" || true  | true  | true   | true  | true  | true   | true   | true
        "lwrupcad" || true  | true  | true   | true  | true  | true   | true   | true
    }

    def "AccountSASPermissions parse IA"() {
        when:
        AccountSASPermission.parse("rwaq")

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "AccountSASResourceType toString"() {
        setup:
        def resourceTypes = new AccountSASResourceType()
            .setService(service)
            .setContainer(container)
            .setObject(object)

        expect:
        resourceTypes.toString() == expectedString

        where:
        service | container | object || expectedString
        true    | false     | false  || "s"
        false   | true      | false  || "c"
        false   | false     | true   || "o"
        true    | true      | true   || "sco"
    }

    @Unroll
    def "AccountSASResourceType parse"() {
        when:
        def resourceTypes = AccountSASResourceType.parse(resourceTypeString)

        then:
        resourceTypes.isService() == service
        resourceTypes.isContainer() == container
        resourceTypes.getObject() == object

        where:
        resourceTypeString || service | container | object
        "s"                || true    | false     | false
        "c"                || false   | true      | false
        "o"                || false   | false     | true
        "sco"              || true    | true      | true
    }

    @Unroll
    def "AccountSASResourceType IA"() {
        when:
        AccountSASResourceType.parse("scq")

        then:
        thrown(IllegalArgumentException)
    }

    def "BlobURLParts"() {
        setup:
        def parts = new BlobURLParts()
        parts.setScheme("http")
            .setHost("host")
            .setContainerName("container")
            .setBlobName("blob")
            .setSnapshot("snapshot")
        def sasValues = new BlobServiceSASSignatureValues()
            .setPermissions("r")
            .setCanonicalName("/containerName/blobName")
            .setExpiryTime(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
            .setResource("bs")
        parts.setSasQueryParameters(sasValues.generateSASQueryParameters(primaryCredential))

        when:
        def splitParts = parts.toURL().toString().split("\\?")

        then:
        splitParts.size() == 2 // Ensure that there is only one question mark even when sas and snapshot are present
        splitParts[0] == "http://host/container/blob"
        splitParts[1].contains("snapshot=snapshot")
        splitParts[1].contains("sp=r")
        splitParts[1].contains("sig=")
        splitParts[1].split("&").size() == 6 // snapshot & sv & sr & sp & sig
    }

    def "URLParser"() {
        when:
        def parts = URLParser.parse(new URL("http://host/container/blob?snapshot=snapshot&sv=" + Constants.HeaderConstants.TARGET_STORAGE_VERSION + "&sr=c&sp=r&sig=Ee%2BSodSXamKSzivSdRTqYGh7AeMVEk3wEoRZ1yzkpSc%3D"))

        then:
        parts.getScheme() == "http"
        parts.getHost() == "host"
        parts.getContainerName() == "container"
        parts.getBlobName() == "blob"
        parts.getSnapshot() == "snapshot"
        parts.getSasQueryParameters().getPermissions() == "r"
        parts.getSasQueryParameters().getVersion() == Constants.HeaderConstants.TARGET_STORAGE_VERSION
        parts.getSasQueryParameters().getResource() == "c"
        parts.getSasQueryParameters().getSignature() == Utility.urlDecode("Ee%2BSodSXamKSzivSdRTqYGh7AeMVEk3wEoRZ1yzkpSc%3D")
    }
}
