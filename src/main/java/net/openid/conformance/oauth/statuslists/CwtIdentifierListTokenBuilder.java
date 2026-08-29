package net.openid.conformance.oauth.statuslists;

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
import org.multipaz.crypto.X509CertChain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an MSO revocation list implementing the identifier list mechanism of ISO/IEC 18013-5
 * 12.3.6.4: the CWT envelope 12.3.6.3 defines for both mechanisms - a COSE_Sign1 (tagged 18)
 * carrying the signature algorithm, the type and the x5chain in its protected header - with the
 * {@code application/identifierlist+cwt} type, no StatusList claim, and the IdentifierList
 * structure at CWT claim key 65530.
 *
 * <p>The CDDL 12.3.6.4 gives is {@code IdentifierList = { "identifiers" : { * Identifier =>
 * IdentifierInfo }, ? "aggregation_uri" : Aggregation_uri, * tstr => RFU }} with
 * {@code IdentifierInfo = { tstr/int => RFU }} and {@code Identifier = bstr}: no IdentifierInfo
 * content is defined yet, so each listed identifier maps to an empty map.
 */
public final class CwtIdentifierListTokenBuilder {

	// CWT claim keys, draft-ietf-oauth-status-list section 5.2 and ISO/IEC 18013-5 12.3.6.4
	private static final long CWT_CLAIM_SUB = 2;
	private static final long CWT_CLAIM_EXP = 4;
	private static final long CWT_CLAIM_IAT = 6;
	private static final long CWT_CLAIM_IDENTIFIER_LIST = 65530;
	private static final long CWT_CLAIM_TTL = 65534;

	public static final String IDENTIFIER_LIST_CWT_CONTENT_TYPE = "application/identifierlist+cwt";

	private CwtIdentifierListTokenBuilder() {
		// utility class
	}

	/**
	 * @param uri the URI the identifier list is published at, used as the {@code sub} claim
	 * @param iat the issuance time
	 * @param exp the expiry time; ISO/IEC 18013-5 12.3.6.3 requires it to be present
	 * @param ttlSeconds the {@code ttl} claim
	 * @param identifiers the Identifiers of the revoked MSOs
	 * @param signingKey the key to sign the COSE_Sign1 with
	 * @param algorithm the COSE signature algorithm; ISO/IEC 18013-5 12.3.6.3 permits only the
	 *   EC based algorithms
	 * @param certChain the certificate chain to place in the protected header, or null to omit it
	 *   (which does not meet the ISO/IEC 18013-5 12.3.6.3 requirement, and is only for callers
	 *   whose signing key has no chain)
	 * @return the CBOR encoded, tagged COSE_Sign1
	 */
	public static byte[] build(String uri, Instant iat, Instant exp, long ttlSeconds,
			List<byte[]> identifiers, AsymmetricKey signingKey, Algorithm algorithm,
			X509CertChain certChain) throws Exception {

		byte[] payload = buildClaimsSet(uri, iat, exp, ttlSeconds, identifiers);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(algorithm.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_TYP),
			new Tstr(IDENTIFIER_LIST_CWT_CONTENT_TYPE));
		if (certChain != null) {
			// ISO/IEC 18013-5 12.3.6.3: the x5chain goes in the protected header
			protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN), certChain.toDataItem());
		}

		CoseSign1 coseSign1 = (CoseSign1) kotlinx.coroutines.BuildersKt.runBlocking(
			kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> Cose.INSTANCE.coseSign1Sign(signingKey, payload, true,
				protectedHeaders, Map.of(), continuation));

		// draft-ietf-oauth-status-list section 5.2: the COSE message is the tagged COSE_Sign1
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	private static byte[] buildClaimsSet(String uri, Instant iat, Instant exp, long ttlSeconds,
			List<byte[]> identifiers) {
		Map<DataItem, DataItem> identifiersMap = new LinkedHashMap<>();
		for (byte[] identifier : identifiers) {
			identifiersMap.put(new Bstr(identifier), new CborMap(new LinkedHashMap<>(), false));
		}

		Map<DataItem, DataItem> identifierList = new LinkedHashMap<>();
		identifierList.put(new Tstr("identifiers"), new CborMap(identifiersMap, false));

		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_SUB), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_EXP),
			DataItemExtensionsKt.toDataItem(exp.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_IAT),
			DataItemExtensionsKt.toDataItem(iat.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_IDENTIFIER_LIST),
			new CborMap(identifierList, false));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_TTL),
			DataItemExtensionsKt.toDataItem(ttlSeconds));

		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}
}
