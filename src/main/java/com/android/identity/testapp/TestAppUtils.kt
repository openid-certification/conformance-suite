package org.multipaz.testapp

import com.nimbusds.jose.jwk.JWK
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.multipaz.cbor.*
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.credential.CredentialLoader
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.*
import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.document.NameSpacedData
import org.multipaz.documenttype.DocumentCannedRequest
import org.multipaz.documenttype.DocumentType
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.EUPersonalID
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.documenttype.knowntypes.UtopiaMovieTicket
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.issuersigned.IssuerNamespaces
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObjectGenerator
import org.multipaz.mdoc.mso.MobileSecurityObjectParser
import org.multipaz.mdoc.request.DeviceRequestGenerator
import org.multipaz.mdoc.response.DeviceResponseGenerator
import org.multipaz.mdoc.response.DocumentGenerator
import org.multipaz.sdjwt.Issuer
import org.multipaz.sdjwt.SdJwtVcGenerator
import org.multipaz.sdjwt.credential.KeyBoundSdJwtVcCredential
import org.multipaz.sdjwt.credential.KeylessSdJwtVcCredential
import org.multipaz.sdjwt.util.JsonWebKey
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.PassphraseConstraints
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.StorageTableSpec
import org.multipaz.storage.base.BaseStorageTable
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.multipaz.util.Constants
import org.multipaz.util.Logger
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
	):ByteArray {
		val deviceResponseGenerator = DeviceResponseGenerator(Constants.DEVICE_RESPONSE_STATUS_OK)
		val document = documentStore!!.lookupDocument(mdlDocumentId!!)
		val credential = document!!.findCredential(CREDENTIAL_DOMAIN_MDOC_NO_USER_AUTH, Clock.System.now())
		deviceResponseGenerator.addDocument(calcDocument(
			credential = credential as MdocCredential,
			encodedSessionTranscript = sessionTranscript
		))
		return deviceResponseGenerator.generate()
	}

	private suspend fun calcDocument(
		credential: MdocCredential,
		encodedSessionTranscript: ByteArray
	): ByteArray {
		// TODO: support MAC keys from v1.1 request and use setDeviceNamespacesMac() when possible
		//   depending on the value of PresentmentSource.preferSignatureToKeyAgreement(). See also
		//   calcDocument in mdocPresentment.kt.
		//
		val issuerSigned = Cbor.decode(credential.issuerProvidedData)
		val issuerNamespaces = IssuerNamespaces.fromDataItem(issuerSigned["nameSpaces"])
		val issuerAuthCoseSign1 = issuerSigned["issuerAuth"].asCoseSign1
		val encodedMsoBytes = Cbor.decode(issuerAuthCoseSign1.payload!!)
		val encodedMso = Cbor.encode(encodedMsoBytes.asTaggedEncodedCbor)
		val mso = MobileSecurityObjectParser(encodedMso).parse()

		val documentGenerator = DocumentGenerator(
			mso.docType,
			Cbor.encode(issuerSigned["issuerAuth"]),
			encodedSessionTranscript,
		)

		documentGenerator.setIssuerNamespaces(issuerNamespaces)
		val keyInfo = credential.secureArea.getKeyInfo(credential.alias)
		if (!keyInfo.algorithm.isSigning) {
			throw IllegalStateException(
				"Signing is required for W3C DC API but its algorithm ${keyInfo.algorithm.name} is not for signing"
			)
		} else {
			documentGenerator.setDeviceNamespacesSignature(
				dataElements = NameSpacedData.Builder().build(),
				secureArea = credential.secureArea,
				keyAlias = credential.alias,
				keyUnlockData = null,
			)
		}

		return documentGenerator.generate()
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

        val deviceRequestGenerator = DeviceRequestGenerator(encodedSessionTranscript)
        deviceRequestGenerator.addDocumentRequest(
            docType = mdocRequest.docType,
            itemsToRequest = itemsToRequest,
            requestInfo = null,
            readerKey = readerKey,
            signatureAlgorithm = readerKey.curve.defaultSigningAlgorithm,
            readerKeyCertificateChain = X509CertChain(listOf(readerCert, readerRootCert)),
        )
        return deviceRequestGenerator.generate()
    }

    fun generateEncodedSessionTranscript(
        encodedDeviceEngagement: ByteArray,
        handover: DataItem,
        eReaderKey: EcPublicKey
    ): ByteArray {
        val encodedEReaderKey = Cbor.encode(eReaderKey.toCoseKey().toDataItem())
        return Cbor.encode(
            buildCborArray {
                add(Tagged(24, Bstr(encodedDeviceEngagement)))
                add(Tagged(24, Bstr(encodedEReaderKey)))
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

	private val testDocumentTableSpec = object: StorageTableSpec(
		name = "TestAppDocuments",
		supportExpiration = false,
		supportPartitions = false,
		schemaVersion = 1L,           // Bump every time incompatible changes are made
	) {
		override suspend fun schemaUpgrade(oldTable: BaseStorageTable) {
			oldTable.deleteAll()
		}
	}
	private suspend fun documentStoreInit() {
		val storage = EphemeralStorage()
		val softwareSecureArea = SoftwareSecureArea.create(storage)
		val secureAreaRepository: SecureAreaRepository = SecureAreaRepository.build {
			add(softwareSecureArea)
		}
		val credentialLoader: CredentialLoader = CredentialLoader()
		credentialLoader.addCredentialImplementation(MdocCredential::class) {
				document -> MdocCredential(document)
		}
		credentialLoader.addCredentialImplementation(KeyBoundSdJwtVcCredential::class) {
				document -> KeyBoundSdJwtVcCredential(document)
		}
		credentialLoader.addCredentialImplementation(KeylessSdJwtVcCredential::class) {
				document -> KeylessSdJwtVcCredential(document)
		}
		documentStore = DocumentStore(
			storage = storage,
			secureAreaRepository = secureAreaRepository,
			credentialLoader = credentialLoader,
			documentMetadataFactory = TestAppDocumentMetadata::create,
			documentTableSpec = testDocumentTableSpec
		)

		// FIXME need to have better keys here - animo verifier fails, Timo said:
		// "Mdoc at index 0 is not valid. Country name (C) must be present in the issuer certificate's subject distinguished name"
		// DavidZ has a tool to generate them in identity-credential library
		// mattr have tool to validate:
		// https://tools.mattrlabs.com/pem?cert=MIIBITCBx6ADAgECAgEBMAoGCCqGSM49BAMCMBoxGDAWBgNVBAMMD1N0YXRlIE9mIFV0b3BpYTAeFw0yNDExMDcyMTUzMDdaFw0zNDExMDUyMTUzMDdaMBoxGDAWBgNVBAMMD1N0YXRlIE9mIFV0b3BpYTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJ5olgDBiHqNhN7rFkSy%252FxD34dQcOSR4KvEWMyb62jI%252BUGUofeAi%252F55RIt74pBsQz9%252BB48WXI8xhIphoNN7AejYwCgYIKoZIzj0EAwIDSQAwRgIhALkqUIVeaSW0xhLuMdwHyjiwTV8USD4zq68369ZW6jBvAiEAj2smZAXJB04x%252Fs3exzjnI5BQprUOSfYEuku1Jv7gA%252BA%253D&mode=IACA
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
        val document = documentStore.createDocument {
            val metadata = it as TestAppDocumentMetadata
            metadata.initialize(
                displayName = displayName,
                typeDisplayName = documentType.displayName,
                cardArt = ByteString(),
            )
        }

        val now = Clock.System.now()
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

        if (documentType.vcDocumentType != null) {
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
                val msoGenerator = MobileSecurityObjectGenerator(
                    Algorithm.SHA256,
                    documentType.mdocDocumentType!!.docType,
                    mdocCredential.getAttestation().publicKey
                )
                msoGenerator.setValidityInfo(signedAt, validFrom, validUntil, null)
                msoGenerator.addValueDigests(issuerNamespaces)

                val mso = msoGenerator.generate()
                val taggedEncodedMso = Cbor.encode(Tagged(24, Bstr(mso)))

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
                        dsKey,
                        taggedEncodedMso,
                        true,
                        dsKey.publicKey.curve.defaultSigningAlgorithm,
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
        if (documentType.vcDocumentType == null) {
            return
        }

        val identityAttributes = buildJsonObject {
            for ((claimName, attribute) in documentType.vcDocumentType!!.claims) {
                val sampleValue = attribute.sampleValueVc
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

        val (domains, numCredentialsPerDomainAdj) = if (documentType.vcDocumentType!!.keyBound) {
            Pair(listOf(CREDENTIAL_DOMAIN_SDJWT_USER_AUTH, CREDENTIAL_DOMAIN_SDJWT_NO_USER_AUTH), numCredentialsPerDomain)
        } else {
            // No point in having multiple credentials for keyless credentials..
            Pair(listOf(CREDENTIAL_DOMAIN_SDJWT_KEYLESS), 1)
        }
        for (domain in domains) {
            for (n in 1..numCredentialsPerDomainAdj) {
                val credential = if (documentType.vcDocumentType!!.keyBound) {
                    val userAuthenticationRequired = (domain == CREDENTIAL_DOMAIN_SDJWT_USER_AUTH)
                    KeyBoundSdJwtVcCredential.create(
                        document = document,
                        asReplacementForIdentifier = null,
                        domain = domain,
                        secureArea = secureArea,
                        vct = documentType.vcDocumentType!!.type,
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
                        vct = documentType.vcDocumentType!!.type,
                    )
                }

                val sdJwtVcGenerator = SdJwtVcGenerator(
                    vct = credential.vct,
                    payload = identityAttributes,
                    issuer = Issuer(
                        "https://example-issuer.com",
                        dsKey.publicKey.curve.defaultSigningAlgorithmFullySpecified,
                        null
                    ),
                )
                sdJwtVcGenerator.publicKey =
                    (credential as? SecureAreaBoundCredential)?.let { JsonWebKey(it.getAttestation().publicKey) }
                sdJwtVcGenerator.timeSigned = signedAt
                sdJwtVcGenerator.timeValidityBegin = validFrom
                sdJwtVcGenerator.timeValidityEnd = validUntil
                val sdJwt = sdJwtVcGenerator.generateSdJwt(dsKey)
                credential.certify(
                    sdJwt.toString().encodeToByteArray(),
                    validFrom,
                    validUntil
                )
            }
        }
    }

}
