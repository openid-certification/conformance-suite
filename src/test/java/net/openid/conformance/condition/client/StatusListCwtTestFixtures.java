package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import kotlinx.io.bytestring.ByteString;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;
import org.multipaz.crypto.EcCurve;
import org.multipaz.crypto.EcPrivateKey;
import org.multipaz.crypto.EcPrivateKeyDoubleCoordinate;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Token Status List Tokens in CWT format (and deliberately malformed variants of them)
 * for the MSO revocation list conditions.
 */
final class StatusListCwtTestFixtures {

	static final String DEFAULT_URI = "https://issuer.example.com/statuslists/1";

	/** Size of the generated list; even indices are valid, odd indices revoked. */
	static final int STATUS_LIST_ENTRIES = 256;

	private static final long COSE_LABEL_ALG = Cose.COSE_LABEL_ALG;
	private static final long COSE_LABEL_TYP = Cose.COSE_LABEL_TYP;
	private static final long COSE_LABEL_X5CHAIN = Cose.COSE_LABEL_X5CHAIN;

	private StatusListCwtTestFixtures() {
		// utility class
	}

	/** How the signer certificate should deviate from the ISO 18013-5 Table B.9 profile. */
	enum CertProfile {
		/** Table B.9 conformant: critical digitalSignature-only key usage, SKI, AKI. */
		CONFORMANT,
		/** Critical key usage asserting keyCertSign and cRLSign instead of digitalSignature. */
		WRONG_KEY_USAGE
	}

	/** A well formed, Table B.9 conformant status list token whose entries follow even=valid. */
	static byte[] validStatusListToken() throws Exception {
		return statusListToken(DEFAULT_URI, CertProfile.CONFORMANT);
	}

	static byte[] statusListToken(String uri, CertProfile certProfile) throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		X509CertChain chain = signerCertChain(key, certProfile);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(
				Algorithm.ES256.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_TYP),
			new Tstr(AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_X5CHAIN), chain.toDataItem());

		CoseSign1 coseSign1 = sign(signingKey(chain, key), claimsSet(uri), protectedHeaders);
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	/**
	 * A well formed status list token signed by the given key, with the given certificate chain
	 * in the protected header - for the certification path checks, whose fixtures need chains
	 * issued by a specific CA rather than the self-signed signer the other helpers use.
	 */
	static byte[] statusListTokenSignedBy(String uri, org.multipaz.crypto.EcPrivateKey key,
			X509CertChain chain) throws Exception {
		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(
				Algorithm.ES256.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_TYP),
			new Tstr(AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_X5CHAIN), chain.toDataItem());

		CoseSign1 coseSign1 = sign(
			new org.multipaz.crypto.AsymmetricKey.X509CertifiedExplicit(chain, key, Algorithm.ES256),
			claimsSet(uri), protectedHeaders);
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	/**
	 * A status list token whose protected header declares RS256 (COSE algorithm identifier -257),
	 * which ISO 18013-5 12.3.6.3 does not permit. The signature bytes are arbitrary; the format
	 * condition rejects the token before any signature check.
	 */
	static byte[] statusListTokenWithDisallowedAlgorithm() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		X509CertChain chain = signerCertChain(key, CertProfile.CONFORMANT);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_ALG), DataItemExtensionsKt.toDataItem(-257));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_TYP),
			new Tstr(AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_X5CHAIN), chain.toDataItem());

		CoseSign1 coseSign1 = new CoseSign1(protectedHeaders, Map.of(),
			new byte[64], claimsSet(DEFAULT_URI));
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	/** A status list token carrying the x5chain in the unprotected rather than protected header. */
	static byte[] statusListTokenWithX5chainInUnprotectedHeader() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		X509CertChain chain = signerCertChain(key, CertProfile.CONFORMANT);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(
				Algorithm.ES256.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_TYP),
			new Tstr(AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE));

		Map<CoseLabel, DataItem> unprotectedHeaders = Map.of(
			new CoseNumberLabel(COSE_LABEL_X5CHAIN), chain.toDataItem());

		CoseSign1 coseSign1 = sign(signingKey(chain, key), claimsSet(DEFAULT_URI), protectedHeaders,
			unprotectedHeaders);
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	/** Base64 (standard, as the environment stores it) of the given token bytes. */
	static String encode(byte[] token) {
		return Base64.getEncoder().encodeToString(token);
	}

	/** A well formed status list token whose status_list claim carries the given aggregation_uri item. */
	static byte[] statusListTokenWithAggregationUri(DataItem aggregationUri) throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		X509CertChain chain = signerCertChain(key, CertProfile.CONFORMANT);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(
				Algorithm.ES256.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_TYP),
			new Tstr(AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE));
		protectedHeaders.put(new CoseNumberLabel(COSE_LABEL_X5CHAIN), chain.toDataItem());

		CoseSign1 coseSign1 = sign(signingKey(chain, key), claimsSet(DEFAULT_URI, aggregationUri),
			protectedHeaders);
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	/** The CWT claims set: sub, iat, exp, ttl and the status_list claim. */
	private static byte[] claimsSet(String uri) {
		return claimsSet(uri, null);
	}

	private static byte[] claimsSet(String uri, DataItem aggregationUri) {
		byte[] rawEntries = new byte[STATUS_LIST_ENTRIES];
		for (int i = 0; i < rawEntries.length; i++) {
			rawEntries[i] = (byte) (i % 2 == 0
				? TokenStatusList.Status.VALID.getTypeValue()
				: TokenStatusList.Status.INVALID.getTypeValue());
		}
		byte[] compressed = Base64.getUrlDecoder().decode(
			TokenStatusList.create(rawEntries, 1).encodeStatusList());

		Map<DataItem, DataItem> statusList = new LinkedHashMap<>();
		statusList.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(1));
		statusList.put(new Tstr("lst"), new Bstr(compressed));
		if (aggregationUri != null) {
			statusList.put(new Tstr("aggregation_uri"), aggregationUri);
		}

		long now = Instant.now().getEpochSecond();
		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(2L), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(4L), DataItemExtensionsKt.toDataItem(now + 600));
		claims.put(DataItemExtensionsKt.toDataItem(6L), DataItemExtensionsKt.toDataItem(now));
		claims.put(DataItemExtensionsKt.toDataItem(65533L), new CborMap(statusList, false));
		claims.put(DataItemExtensionsKt.toDataItem(65534L), DataItemExtensionsKt.toDataItem(720L));

		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}

	static CoseSign1 sign(AsymmetricKey key, byte[] payload,
			Map<CoseLabel, DataItem> protectedHeaders) throws InterruptedException {
		return sign(key, payload, protectedHeaders, Map.of());
	}

	static CoseSign1 sign(AsymmetricKey key, byte[] payload,
			Map<CoseLabel, DataItem> protectedHeaders, Map<CoseLabel, DataItem> unprotectedHeaders)
			throws InterruptedException {
		return (CoseSign1) kotlinx.coroutines.BuildersKt.runBlocking(
			kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> Cose.INSTANCE.coseSign1Sign(key, payload, true,
				protectedHeaders, unprotectedHeaders, continuation));
	}

	static X509CertChain signerCertChain(ECKey key, CertProfile profile) throws Exception {
		byte[] der = signerCertificate(key, profile);
		return new X509CertChain(List.of(new X509Cert(new ByteString(der, 0, der.length))));
	}

	static AsymmetricKey signingKey(X509CertChain chain, ECKey key) {
		EcPrivateKey privateKey = new EcPrivateKeyDoubleCoordinate(EcCurve.P256,
			key.getD().decode(), key.getX().decode(), key.getY().decode());
		return new AsymmetricKey.X509CertifiedExplicit(chain, privateKey, Algorithm.ES256);
	}

	private static byte[] signerCertificate(ECKey key, CertProfile profile) throws Exception {
		X500Name name = new X500Name("C=UT,CN=OIDF Status List Signer");
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
			name,
			new BigInteger(80, new java.security.SecureRandom()),
			new Date(System.currentTimeMillis() - 60_000),
			new Date(System.currentTimeMillis() + 3600_000),
			name,
			SubjectPublicKeyInfo.getInstance(key.toECPublicKey().getEncoded()));
		JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
		builder.addExtension(Extension.subjectKeyIdentifier, false,
			extensionUtils.createSubjectKeyIdentifier(key.toECPublicKey()));
		builder.addExtension(Extension.authorityKeyIdentifier, false,
			extensionUtils.createAuthorityKeyIdentifier(key.toECPublicKey()));
		if (profile == CertProfile.WRONG_KEY_USAGE) {
			builder.addExtension(Extension.keyUsage, true,
				new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		} else {
			builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
		}
		return builder
			.build(new JcaContentSignerBuilder("SHA256withECDSA").build(key.toECPrivateKey()))
			.getEncoded();
	}
}
