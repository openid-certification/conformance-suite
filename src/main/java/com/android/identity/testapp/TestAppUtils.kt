package org.multipaz.testapp

import com.nimbusds.jose.jwk.JWK
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.multipaz.cbor.*
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.*
import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.mdoc.devicesigned.DeviceNamespaces
import org.multipaz.documenttype.DocumentCannedRequest
import org.multipaz.documenttype.DocumentType
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.EUPersonalID
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.documenttype.knowntypes.UtopiaMovieTicket
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.issuersigned.IssuerNamespaces
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObject
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.response.DeviceResponse
import org.multipaz.mdoc.response.buildDeviceResponse
import org.multipaz.sdjwt.SdJwt
import org.multipaz.sdjwt.credential.KeyBoundSdJwtVcCredential
import org.multipaz.sdjwt.credential.KeylessSdJwtVcCredential
import org.multipaz.prompt.Reason
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.PassphraseConstraints
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.multipaz.util.Logger
import org.multipaz.util.truncateToWholeSeconds
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object TestAppUtils {
    private const val TAG = "TestAppUtils"

    // This domain is for MdocCredential using mdoc ECDSA/EdDSA authentication and requiring user authentication.
    const val CREDENTIAL_DOMAIN_MDOC_USER_AUTH = "mdoc_user_auth"

    // This domain is for MdocCredential using mdoc ECDSA/EdDSA authentication and not requiring user authentication.
    const val CREDENTIAL_DOMAIN_MDOC_NO_USER_AUTH = "mdoc_no_user_auth"

    // This domain is for MdocCredential using mdoc MAC authentication and requiring user authentication.
    const val CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH = "mdoc_mac_user_auth"

    // This domain is for MdocCredential using mdoc MAC authentication and not requiring user authentication.
    const val CREDENTIAL_DOMAIN_MDOC_MAC_NO_USER_AUTH = "mdoc_mac_no_user_auth"

    // This domain is for KeyBoundSdJwtVcCredential and requiring user authentication.
    const val CREDENTIAL_DOMAIN_SDJWT_USER_AUTH = "sdjwt_user_auth"

    // This domain is for KeyBoundSdJwtVcCredential and not requiring user authentication.
    const val CREDENTIAL_DOMAIN_SDJWT_NO_USER_AUTH = "sdjwt_no_user_auth"

    // This domain is for KeylessSdJwtVcCredential
    const val CREDENTIAL_DOMAIN_SDJWT_KEYLESS = "sdjwt_keyless"

	fun generateDeviceResponse(sessionTranscript: ByteArray): ByteArray {
		return runBlocking {
			generateEncodedDeviceResponse(sessionTranscript)
		}
	}

	suspend fun generateEncodedDeviceResponse(
		sessionTranscript: ByteArray
	): ByteArray {
		val document = documentStore!!.lookupDocument(mdlDocumentId!!)
		val credential = document!!.findCredential(
			CREDENTIAL_DOMAIN_MDOC_NO_USER_AUTH, Clock.System.now()
		) as MdocCredential

		val issuerSigned = Cbor.decode(credential.issuerProvidedData)
		val issuerNamespaces = IssuerNamespaces.fromDataItem(issuerSigned["nameSpaces"])
		val issuerAuthCoseSign1 = issuerSigned["issuerAuth"].asCoseSign1

		val deviceKey = AsymmetricKey.anonymous(
			credential.secureArea, credential.alias, Reason.Unspecified
		)

		val deviceResponse = buildDeviceResponse(
			sessionTranscript = RawCbor(sessionTranscript),
			status = DeviceResponse.STATUS_OK,
		) {
			addDocument(
				docType = credential.docType,
				issuerAuth = issuerAuthCoseSign1,
				issuerNamespaces = issuerNamespaces,
				deviceNamespaces = DeviceNamespaces(emptyMap()),
				deviceKey = deviceKey,
			)
		}
		return Cbor.encode(deviceResponse.toDataItem())
	}
    fun generateEncodedDeviceRequest(
        request: DocumentCannedRequest,
        encodedSessionTranscript: ByteArray,
        readerKey: EcPrivateKey,
        readerCert: X509Cert,
        readerRootCert: X509Cert,
    ): ByteArray {
        val mdocRequest = request.mdocRequest!!
        val itemsToRequest = mutableMapOf<String, MutableMap<String, Boolean>>()
        for (ns in mdocRequest.namespacesToRequest) {
            for ((de, intentToRetain) in ns.dataElementsToRequest) {
                itemsToRequest.getOrPut(ns.namespace) { mutableMapOf() }
                    .put(de.attribute.identifier, intentToRetain)
            }
        }

        val readerAsymmetricKey = AsymmetricKey.X509CertifiedExplicit(
            X509CertChain(listOf(readerCert, readerRootCert)),
            readerKey,
        )

        val deviceRequest = runBlocking {
            buildDeviceRequest(
                sessionTranscript = RawCbor(encodedSessionTranscript),
            ) {
                addDocRequest(
                    docType = mdocRequest.docType,
                    nameSpaces = itemsToRequest,
                    docRequestInfo = null,
                    readerKey = readerAsymmetricKey,
                )
            }
        }
        return Cbor.encode(deviceRequest.toDataItem())
    }

    fun generateEncodedSessionTranscript(
        encodedDeviceEngagement: ByteArray,
        handover: DataItem,
        eReaderKey: EcPublicKey
    ): ByteArray {
        val encodedEReaderKey = Cbor.encode(eReaderKey.toCoseKey().toDataItem())
        return Cbor.encode(
            buildCborArray {
                add(Tagged(Tagged.ENCODED_CBOR, Bstr(encodedDeviceEngagement)))
                add(Tagged(Tagged.ENCODED_CBOR, Bstr(encodedEReaderKey)))
                add(handover)
            }
        )
    }


    val provisionedDocumentTypes = listOf(
        DrivingLicense.getDocumentType(),
        PhotoID.getDocumentType(),
        EUPersonalID.getDocumentType(),
        UtopiaMovieTicket.getDocumentType()
    )

	fun initialise() {
		runBlocking {
			documentStoreInit();
		}
	}

	var documentStore: DocumentStore? = null

	private suspend fun documentStoreInit() {
		val storage = EphemeralStorage()
		val softwareSecureArea = SoftwareSecureArea.create(storage)
		val secureAreaRepository: SecureAreaRepository = SecureAreaRepository.Builder()
			.add(softwareSecureArea)
			.build()
		documentStore = buildDocumentStore(
			storage = storage,
			secureAreaRepository = secureAreaRepository
		) {}

		val documentSignerKeyPub = EcPublicKey.fromPem(
			"""-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnmiWAMGIeo2E3usWRLL/EPfh1Bw5
JHgq8RYzJvraMj5QZSh94CL/nlEi3vikGxDP34HjxZcjzGEimGg03sB6Ng==
-----END PUBLIC KEY-----""",
			EcCurve.P256
		)
		val documentSignerKey = EcPrivateKey.fromPem(
			"""-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/ANvinTxJAdR8nQ0
NoUdBMcRJz+xLsb0kmhyMk+lkkGhRANCAASeaJYAwYh6jYTe6xZEsv8Q9+HUHDkk
eCrxFjMm+toyPlBlKH3gIv+eUSLe+KQbEM/fgePFlyPMYSKYaDTewHo2
-----END PRIVATE KEY-----""",
			documentSignerKeyPub
		)

		// The following certificate was generated by using the above key material
/*
# Self-signed certificate using inline config
openssl req -new -x509 -key old-private-key.pem -out old-certificate.pem -days 365 \
  -config <(cat <<'EOF'
[ req ]
default_bits       = 256
distinguished_name = dn
x509_extensions    = v3_ca
prompt             = no

[ dn ]
C  = US
ST = State of Utopia
L  = San Ramon
O  = OpenID Foundation
OU = IT
CN = certification.openid.net

[ v3_ca ]
basicConstraints = critical,CA:true,pathlen:0
keyUsage = critical, keyCertSign, cRLSign
issuerAltName = email:certification@oidf.org
crlDistributionPoints = URI:http://example.com/myca.crl
EOF
)

There is also a tool to generate the certs in the multipaz identity-credential library which could be used instead.

Any new cert should be checked with the mattr checker tool:

https://tools.mattrlabs.com/pem

*/
		val documentSignerCert = X509Cert.fromPem(
			"""-----BEGIN CERTIFICATE-----
MIICqTCCAlCgAwIBAgIUEmctHgzxSGqk6Z8Eb+0s97VZdpowCgYIKoZIzj0EAwIw
gYcxCzAJBgNVBAYTAlVTMRgwFgYDVQQIDA9TdGF0ZSBvZiBVdG9waWExEjAQBgNV
BAcMCVNhbiBSYW1vbjEaMBgGA1UECgwRT3BlbklEIEZvdW5kYXRpb24xCzAJBgNV
BAsMAklUMSEwHwYDVQQDDBhjZXJ0aWZpY2F0aW9uLm9wZW5pZC5uZXQwHhcNMjUw
NzMwMDc0NzIyWhcNMjYwNzMwMDc0NzIyWjCBhzELMAkGA1UEBhMCVVMxGDAWBgNV
BAgMD1N0YXRlIG9mIFV0b3BpYTESMBAGA1UEBwwJU2FuIFJhbW9uMRowGAYDVQQK
DBFPcGVuSUQgRm91bmRhdGlvbjELMAkGA1UECwwCSVQxITAfBgNVBAMMGGNlcnRp
ZmljYXRpb24ub3BlbmlkLm5ldDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJ5o
lgDBiHqNhN7rFkSy/xD34dQcOSR4KvEWMyb62jI+UGUofeAi/55RIt74pBsQz9+B
48WXI8xhIphoNN7AejajgZcwgZQwEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8B
Af8EBAMCAQYwIQYDVR0SBBowGIEWY2VydGlmaWNhdGlvbkBvaWRmLm9yZzAsBgNV
HR8EJTAjMCGgH6AdhhtodHRwOi8vZXhhbXBsZS5jb20vbXljYS5jcmwwHQYDVR0O
BBYEFHhk9LVVH8Gt9ZgfxgyhSl921XOhMAoGCCqGSM49BAMCA0cAMEQCICBxjCq9
efAwMKREK+k0OXBtiQCbFD7QdpyH42LVYfdvAiAurlZwp9PtmQZzoSYDUvXpZM5v
TvFLVc4ESGy3AtdC+g==
-----END CERTIFICATE-----""""
		)

		provisionTestDocuments(
			documentStore = documentStore!!,
			secureArea = softwareSecureArea,
			secureAreaCreateKeySettingsFunc = ::createKeySettings,
			dsKey = documentSignerKey,
			dsCert = documentSignerCert,
			deviceKeyAlgorithm = Algorithm.ESP256,
			deviceKeyMacAlgorithm = Algorithm.ECDH_P256,
			numCredentialsPerDomain = 1
		)
	}

	@JvmStatic
	fun convertToEcPrivateKey(jwk: JWK): EcPrivateKey {

		val key = jwk.toECKey().toECPrivateKey()
		val d = key.s.toByteArray()
		val pubKey = jwk.toECKey().toECPublicKey()
		val x = pubKey.w.affineX.toByteArray()
		val y = pubKey.w.affineY.toByteArray()

		return EcPrivateKeyDoubleCoordinate(EcCurve.P256, d, x, y)
	}
	fun createKeySettings(
		challenge: ByteString,
		algorithm: Algorithm,
		userAuthenticationRequired: Boolean,
		validFrom: Instant,
		validUntil: Instant
	): CreateKeySettings {
			return SoftwareCreateKeySettings.Builder()
				.setAlgorithm(algorithm)
				.setPassphraseRequired(userAuthenticationRequired, "1111", PassphraseConstraints.PIN_FOUR_DIGITS)
				.build()
	}
	var mdlDocumentId: String? = null

	suspend fun provisionTestDocuments(
        documentStore: DocumentStore,
        secureArea: SecureArea,
        secureAreaCreateKeySettingsFunc: (
            challenge: ByteString,
            algorithm: Algorithm,
            userAuthenticationRequired: Boolean,
            validFrom: Instant,
            validUntil: Instant
        ) -> CreateKeySettings,
        dsKey: EcPrivateKey,
        dsCert: X509Cert,
        deviceKeyAlgorithm: Algorithm,
        deviceKeyMacAlgorithm: Algorithm,
        numCredentialsPerDomain: Int,
    ) {
        require(deviceKeyAlgorithm.isSigning)
        require(deviceKeyMacAlgorithm == Algorithm.UNSET || deviceKeyMacAlgorithm.isKeyAgreement)
        mdlDocumentId = provisionDocument(
            documentStore,
            secureArea,
            secureAreaCreateKeySettingsFunc,
            dsKey,
            dsCert,
            deviceKeyAlgorithm,
            deviceKeyMacAlgorithm,
            numCredentialsPerDomain,
            DrivingLicense.getDocumentType(),
            "Erika",
            "Erika's Driving License"
        )
        provisionDocument(
            documentStore,
            secureArea,
            secureAreaCreateKeySettingsFunc,
            dsKey,
            dsCert,
            deviceKeyAlgorithm,
            deviceKeyMacAlgorithm,
            numCredentialsPerDomain,
            PhotoID.getDocumentType(),
            "Erika",
            "Erika's Photo ID"
        )
        provisionDocument(
            documentStore,
            secureArea,
            secureAreaCreateKeySettingsFunc,
            dsKey,
            dsCert,
            deviceKeyAlgorithm,
            deviceKeyMacAlgorithm,
            numCredentialsPerDomain,
            PhotoID.getDocumentType(),
            "Erika #2",
            "Erika's Photo ID #2",
        )
        provisionDocument(
            documentStore,
            secureArea,
            secureAreaCreateKeySettingsFunc,
            dsKey,
            dsCert,
            deviceKeyAlgorithm,
            deviceKeyMacAlgorithm,
            numCredentialsPerDomain,
            EUPersonalID.getDocumentType(),
            "Erika",
            "Erika's EU PID"
        )
        provisionDocument(
            documentStore,
            secureArea,
            secureAreaCreateKeySettingsFunc,
            dsKey,
            dsCert,
            deviceKeyAlgorithm,
            deviceKeyMacAlgorithm,
            numCredentialsPerDomain,
            UtopiaMovieTicket.getDocumentType(),
            "Erika",
            "Erika's Movie Ticket"
        )
    }

    // TODO: also provision SD-JWT credentials, if applicable
    private suspend fun provisionDocument(
        documentStore: DocumentStore,
        secureArea: SecureArea,
        secureAreaCreateKeySettingsFunc: (
            challenge: ByteString,
            algorithm: Algorithm,
            userAuthenticationRequired: Boolean,
            validFrom: Instant,
            validUntil: Instant
        ) -> CreateKeySettings,
        dsKey: EcPrivateKey,
        dsCert: X509Cert,
        deviceKeyAlgorithm: Algorithm,
        deviceKeyMacAlgorithm: Algorithm,
        numCredentialsPerDomain: Int,
        documentType: DocumentType,
        givenNameOverride: String,
        displayName: String
    ): String {
        val document = documentStore.createDocument(
            displayName = displayName,
            typeDisplayName = documentType.displayName,
            cardArt = ByteString(),
        )

        val now = Clock.System.now().truncateToWholeSeconds()
        val signedAt = now - 1.hours
        val validFrom =  now - 1.hours
        val validUntil = now + 365.days

        if (documentType.mdocDocumentType != null) {
            addMdocCredentials(
                document = document,
                documentType = documentType,
                secureArea = secureArea,
                secureAreaCreateKeySettingsFunc = secureAreaCreateKeySettingsFunc,
                deviceKeyAlgorithm = deviceKeyAlgorithm,
                deviceKeyMacAlgorithm = deviceKeyMacAlgorithm,
                signedAt = signedAt,
                validFrom = validFrom,
                validUntil = validUntil,
                dsKey = dsKey,
                dsCert = dsCert,
                numCredentialsPerDomain = numCredentialsPerDomain,
                givenNameOverride = givenNameOverride
            )
        }

        if (documentType.jsonDocumentType != null) {
            addSdJwtVcCredentials(
                document = document,
                documentType = documentType,
                secureArea = secureArea,
                secureAreaCreateKeySettingsFunc = secureAreaCreateKeySettingsFunc,
                deviceKeyAlgorithm = deviceKeyAlgorithm,
                signedAt = signedAt,
                validFrom = validFrom,
                validUntil = validUntil,
                dsKey = dsKey,
                dsCert = dsCert,
                numCredentialsPerDomain = numCredentialsPerDomain,
                givenNameOverride = givenNameOverride
            )
        }
		return document.identifier
    }

    private suspend fun addMdocCredentials(
        document: Document,
        documentType: DocumentType,
        secureArea: SecureArea,
        secureAreaCreateKeySettingsFunc: (
            challenge: ByteString,
            algorithm: Algorithm,
            userAuthenticationRequired: Boolean,
            validFrom: Instant,
            validUntil: Instant
        ) -> CreateKeySettings,
        deviceKeyAlgorithm: Algorithm,
        deviceKeyMacAlgorithm: Algorithm,
        signedAt: Instant,
        validFrom: Instant,
        validUntil: Instant,
        dsKey: EcPrivateKey,
        dsCert: X509Cert,
        numCredentialsPerDomain: Int,
        givenNameOverride: String
    ) {
        val issuerNamespaces = buildIssuerNamespaces {
            for ((nsName, ns) in documentType.mdocDocumentType?.namespaces!!) {
                addNamespace(nsName) {
                    for ((deName, de) in ns.dataElements) {
                        val sampleValue = de.attribute.sampleValueMdoc
                        if (sampleValue != null) {
                            val value = if (deName.startsWith("given_name")) {
                                Tstr(givenNameOverride)
                            } else {
                                sampleValue
                            }
                            addDataElement(deName, value)
                        } else {
                            Logger.w(TAG, "No sample value for data element $deName")
                        }
                    }
                }
            }
        }

        // Create authentication keys...
        for (domain in listOf(
            CREDENTIAL_DOMAIN_MDOC_USER_AUTH,
            CREDENTIAL_DOMAIN_MDOC_NO_USER_AUTH,
            CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH,
            CREDENTIAL_DOMAIN_MDOC_MAC_NO_USER_AUTH
        )) {
            val userAuthenticationRequired = when (domain) {
                CREDENTIAL_DOMAIN_MDOC_USER_AUTH, CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH -> true
                else -> false
            }
            val algorithm = when (domain) {
                CREDENTIAL_DOMAIN_MDOC_USER_AUTH -> deviceKeyAlgorithm
                CREDENTIAL_DOMAIN_MDOC_NO_USER_AUTH -> deviceKeyAlgorithm
                CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH -> deviceKeyMacAlgorithm
                CREDENTIAL_DOMAIN_MDOC_MAC_NO_USER_AUTH ->  deviceKeyMacAlgorithm
                else -> throw IllegalStateException()
            }
            if (algorithm == Algorithm.UNSET) {
                continue
            }

            for (n in 1..numCredentialsPerDomain) {
                val mdocCredential = MdocCredential.create(
                    document = document,
                    asReplacementForIdentifier = null,
                    domain = domain,
                    secureArea = secureArea,
                    docType = documentType.mdocDocumentType!!.docType,
                    createKeySettings = secureAreaCreateKeySettingsFunc(
                        "Challenge".encodeToByteString(),
                        algorithm,
                        userAuthenticationRequired,
                        validFrom,
                        validUntil
                    )
                )

                // Generate an MSO and issuer-signed data for this authentication key.
                val mso = MobileSecurityObject(
                    version = "1.0",
                    docType = documentType.mdocDocumentType!!.docType,
                    signedAt = signedAt,
                    validFrom = validFrom,
                    validUntil = validUntil,
                    expectedUpdate = null,
                    digestAlgorithm = Algorithm.SHA256,
                    valueDigests = issuerNamespaces.getValueDigests(Algorithm.SHA256),
                    deviceKey = mdocCredential.getAttestation().publicKey,
                )
                val taggedEncodedMso = Cbor.encode(Tagged(Tagged.ENCODED_CBOR, Bstr(Cbor.encode(mso.toDataItem()))))

                // IssuerAuth is a COSE_Sign1 where payload is MobileSecurityObjectBytes
                //
                // MobileSecurityObjectBytes = #6.24(bstr .cbor MobileSecurityObject)
                //
                val protectedHeaders = mapOf<CoseLabel, DataItem>(
                    Pair(
                        CoseNumberLabel(Cose.COSE_LABEL_ALG),
                        Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
                    )
                )
                val unprotectedHeaders = mapOf<CoseLabel, DataItem>(
                    Pair(
                        CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN),
                        X509CertChain(listOf(dsCert)).toDataItem()
                    )
                )
                val encodedIssuerAuth = Cbor.encode(
                    Cose.coseSign1Sign(
                        AsymmetricKey.anonymous(dsKey),
                        taggedEncodedMso,
                        true,
                        protectedHeaders,
                        unprotectedHeaders
                    ).toDataItem()
                )
                val issuerProvidedAuthenticationData = Cbor.encode(
                    buildCborMap {
                        put("nameSpaces", issuerNamespaces.toDataItem())
                        put("issuerAuth", RawCbor(encodedIssuerAuth))
                    }
                )

                // Now that we have issuer-provided authentication data we certify the authentication key.
                mdocCredential.certify(
                    issuerProvidedAuthenticationData,
                    validFrom,
                    validUntil
                )
            }
        }

    }

    // Technically - according to RFC 7800 at least - SD-JWT could do MACing too but it would
    // need to be specced out in e.g. SD-JWT VC profile where to get the public key from the
    // recipient. So for now, we only support signing.
    //
    private suspend fun addSdJwtVcCredentials(
        document: Document,
        documentType: DocumentType,
        secureArea: SecureArea,
        secureAreaCreateKeySettingsFunc: (
            challenge: ByteString,
            algorithm: Algorithm,
            userAuthenticationRequired: Boolean,
            validFrom: Instant,
            validUntil: Instant
        ) -> CreateKeySettings,
        deviceKeyAlgorithm: Algorithm,
        signedAt: Instant,
        validFrom: Instant,
        validUntil: Instant,
        dsKey: EcPrivateKey,
        dsCert: X509Cert,
        numCredentialsPerDomain: Int,
        givenNameOverride: String
    ) {
        if (documentType.jsonDocumentType == null) {
            return
        }

        val identityAttributes = buildJsonObject {
            for ((claimName, attribute) in documentType.jsonDocumentType!!.claims) {
                // Skip sub-claims (e.g. "address.street_address")
                if (claimName.contains('.')) {
                    continue
                }
                val sampleValue = attribute.sampleValueJson
                if (sampleValue != null) {
                    val value = if (claimName.startsWith("given_name")) {
                        JsonPrimitive(givenNameOverride)
                    } else {
                        sampleValue
                    }
                    put(claimName, value)
                } else {
                    Logger.w(TAG, "No sample value for claim $claimName")
                }
            }
        }

        val (domains, numCredentialsPerDomainAdj) = if (documentType.jsonDocumentType!!.keyBound) {
            Pair(listOf(CREDENTIAL_DOMAIN_SDJWT_USER_AUTH, CREDENTIAL_DOMAIN_SDJWT_NO_USER_AUTH), numCredentialsPerDomain)
        } else {
            // No point in having multiple credentials for keyless credentials..
            Pair(listOf(CREDENTIAL_DOMAIN_SDJWT_KEYLESS), 1)
        }
        for (domain in domains) {
            for (n in 1..numCredentialsPerDomainAdj) {
                val credential = if (documentType.jsonDocumentType!!.keyBound) {
                    val userAuthenticationRequired = (domain == CREDENTIAL_DOMAIN_SDJWT_USER_AUTH)
                    KeyBoundSdJwtVcCredential.create(
                        document = document,
                        asReplacementForIdentifier = null,
                        domain = domain,
                        secureArea = secureArea,
                        vct = documentType.jsonDocumentType!!.vct,
                        createKeySettings = secureAreaCreateKeySettingsFunc(
                            "Challenge".encodeToByteString(),
                            deviceKeyAlgorithm,
                            userAuthenticationRequired,
                            validFrom,
                            validUntil
                        )
                    )
                } else {
                    KeylessSdJwtVcCredential.create(
                        document = document,
                        asReplacementForIdentifier = null,
                        domain = domain,
                        vct = documentType.jsonDocumentType!!.vct,
                    )
                }

                val nonSdClaims = buildJsonObject {
                    put("iss", "https://example-issuer.com")
                    put("vct", credential.vct)
                    put("iat", signedAt.epochSeconds)
                    put("nbf", validFrom.epochSeconds)
                    put("exp", validUntil.epochSeconds)
                }
                val kbKey = (credential as? SecureAreaBoundCredential)?.getAttestation()?.publicKey
                val sdJwt = SdJwt.create(
                    issuerKey = AsymmetricKey.anonymous(dsKey),
                    kbKey = kbKey,
                    claims = identityAttributes,
                    nonSdClaims = nonSdClaims
                )
                credential.certify(
                    sdJwt.compactSerialization.encodeToByteArray(),
                    validFrom,
                    validUntil
                )
            }
        }
    }

}
