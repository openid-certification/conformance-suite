package net.openid.conformance.condition.client;

import kotlin.Pair;
import net.openid.conformance.oauth.statuslists.CwtStatusListTokenBuilder;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
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
import org.multipaz.crypto.EcPrivateKey;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.X509KeyUsage;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds Token Status List Tokens in CWT format (and deliberately malformed variants of them)
 * for the MSO revocation list conditions. The well formed tokens come from the suite's own
 * {@link CwtStatusListTokenBuilder}; the malformed ones are assembled by hand with
 * {@link #token}.
 */
final class StatusListCwtTestFixtures {

	static final String DEFAULT_URI = "https://issuer.example.com/statuslists/1";

	static final DataItem ES256_ALG =
		DataItemExtensionsKt.toDataItem(Algorithm.ES256.getCoseAlgorithmIdentifier().intValue());

	/** One IACA for every fixture signer; the conditions under test only look at the leaf. */
	private static final VicalTestFixtures.IssuerPki PKI = VicalTestFixtures.generateIssuerPki();

	private static final long TTL_SECONDS = 720;

	private StatusListCwtTestFixtures() {
		// utility class
	}

	/** A well formed, Table B.9 conformant status list token whose entries follow even=valid. */
	static byte[] validStatusListToken() throws Exception {
		return statusListToken(DEFAULT_URI);
	}

	static byte[] statusListToken(String uri) throws Exception {
		return wellFormedStatusListToken(uri, conformantSigner());
	}

	/** A well formed status list token signed by a Table B.9 conformant P-256 key under the given algorithm. */
	static byte[] statusListTokenWithAlgorithm(Algorithm algorithm) throws Exception {
		return wellFormedStatusListToken(DEFAULT_URI, conformantSigner(), algorithm);
	}

	/** A well formed status list token whose signer certificate asserts the given key usage. */
	static byte[] statusListTokenWithSignerKeyUsage(Set<X509KeyUsage> keyUsage) throws Exception {
		return wellFormedStatusListToken(DEFAULT_URI, signer(keyUsage));
	}

	private static byte[] wellFormedStatusListToken(String uri, AsymmetricKey.X509CertifiedExplicit signer)
			throws Exception {
		return wellFormedStatusListToken(uri, signer, Algorithm.ES256);
	}

	private static byte[] wellFormedStatusListToken(String uri, AsymmetricKey.X509CertifiedExplicit signer,
			Algorithm algorithm) throws Exception {
		Instant iat = Instant.now();
		return CwtStatusListTokenBuilder.build(uri, iat, iat.plusSeconds(600), TTL_SECONDS,
			EvenOddStatusListContents.BITS, EvenOddStatusListContents.create().compressStatusList(),
			signer, algorithm);
	}

	/**
	 * A status list token whose protected header declares RS256 (COSE algorithm identifier -257),
	 * which ISO 18013-5 12.3.6.3 does not permit. The signature bytes are arbitrary; the format
	 * condition rejects the token before any signature check.
	 */
	static byte[] statusListTokenWithDisallowedAlgorithm() throws Exception {
		return token(statusListClaimsSet(DEFAULT_URI), DataItemExtensionsKt.toDataItem(-257),
			new Tstr(StatusListCwt.CONTENT_TYPE), conformantSigner().getCertChain(), true, null);
	}

	/** A well formed status list token whose protected header carries the given type item. */
	static byte[] statusListTokenWithType(DataItem type) throws Exception {
		AsymmetricKey.X509CertifiedExplicit signer = conformantSigner();
		return token(statusListClaimsSet(DEFAULT_URI), ES256_ALG, type, signer.getCertChain(), true, signer);
	}

	/** A well formed status list token with the given claim replaced (or added). */
	static byte[] statusListTokenWithClaim(long key, DataItem value) throws Exception {
		Map<DataItem, DataItem> claims = statusListClaims(DEFAULT_URI);
		claims.put(DataItemExtensionsKt.toDataItem(key), value);
		AsymmetricKey.X509CertifiedExplicit signer = conformantSigner();
		return token(encodeClaims(claims), ES256_ALG, new Tstr(StatusListCwt.CONTENT_TYPE),
			signer.getCertChain(), true, signer);
	}

	/** A status list token carrying the x5chain in the unprotected rather than protected header. */
	static byte[] statusListTokenWithX5chainInUnprotectedHeader() throws Exception {
		AsymmetricKey.X509CertifiedExplicit signer = conformantSigner();
		return token(statusListClaimsSet(DEFAULT_URI), ES256_ALG, new Tstr(StatusListCwt.CONTENT_TYPE),
			signer.getCertChain(), false, signer);
	}

	/** Base64 (standard, as the environment stores it) of the given token bytes. */
	static String encode(byte[] token) {
		return Base64.getEncoder().encodeToString(token);
	}

	/** A fresh signer whose certificate follows the ISO/IEC 18013-5 Table B.9 profile. */
	static AsymmetricKey.X509CertifiedExplicit conformantSigner() {
		return signer(Set.of(X509KeyUsage.DIGITAL_SIGNATURE));
	}

	private static AsymmetricKey.X509CertifiedExplicit signer(Set<X509KeyUsage> keyUsage) {
		Pair<EcPrivateKey, X509Cert> leaf =
			VicalTestFixtures.mintLeafUnderIaca(PKI, "OIDF Status List Signer", keyUsage);
		return new AsymmetricKey.X509CertifiedExplicit(new X509CertChain(List.of(leaf.getSecond())),
			leaf.getFirst(), Algorithm.ES256);
	}

	/**
	 * Assembles a CWT format revocation list token: a tagged COSE_Sign1 over {@code payload} whose
	 * protected header carries the algorithm, the type and (unless {@code x5chainProtected} is
	 * false) the certificate chain.
	 *
	 * @param signer the key to sign with, or null to emit an arbitrary signature - for fixtures
	 *   that are rejected on their format before any signature check
	 */
	static byte[] token(byte[] payload, DataItem alg, DataItem type, X509CertChain chain,
			boolean x5chainProtected, AsymmetricKey signer) throws Exception {
		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_ALG), alg);
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_TYP), type);
		Map<CoseLabel, DataItem> unprotectedHeaders = Map.of();
		if (x5chainProtected) {
			protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN), chain.toDataItem());
		} else {
			unprotectedHeaders = Map.of(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN), chain.toDataItem());
		}

		CoseSign1 coseSign1 = signer == null
			? new CoseSign1(protectedHeaders, unprotectedHeaders, new byte[64], payload)
			: sign(signer, payload, protectedHeaders, unprotectedHeaders);
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	/** The claims ISO/IEC 18013-5 12.3.6.3 requires of both mechanisms: sub, exp, iat, plus ttl. */
	static Map<DataItem, DataItem> baseClaims(String uri) {
		long now = Instant.now().getEpochSecond();
		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_SUB), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_EXP), DataItemExtensionsKt.toDataItem(now + 600));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_IAT), DataItemExtensionsKt.toDataItem(now));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_TTL), DataItemExtensionsKt.toDataItem(TTL_SECONDS));
		return claims;
	}

	static byte[] encodeClaims(Map<DataItem, DataItem> claims) {
		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}

	private static byte[] statusListClaimsSet(String uri) {
		return encodeClaims(statusListClaims(uri));
	}

	/** The status list token's claims: the shared base claims plus the status_list claim. */
	private static Map<DataItem, DataItem> statusListClaims(String uri) {
		Map<DataItem, DataItem> statusList = new LinkedHashMap<>();
		statusList.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(EvenOddStatusListContents.BITS));
		statusList.put(new Tstr("lst"), new Bstr(EvenOddStatusListContents.create().compressStatusList()));

		Map<DataItem, DataItem> claims = baseClaims(uri);
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_STATUS_LIST), new CborMap(statusList, false));
		return claims;
	}

	private static CoseSign1 sign(AsymmetricKey key, byte[] payload,
			Map<CoseLabel, DataItem> protectedHeaders, Map<CoseLabel, DataItem> unprotectedHeaders)
			throws InterruptedException {
		return (CoseSign1) kotlinx.coroutines.BuildersKt.runBlocking(
			kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> Cose.INSTANCE.coseSign1Sign(key, payload, true,
				protectedHeaders, unprotectedHeaders, continuation));
	}
}
