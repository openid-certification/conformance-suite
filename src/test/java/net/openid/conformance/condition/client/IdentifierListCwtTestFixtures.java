package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
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
import org.multipaz.crypto.X509CertChain;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds identifier lists in CWT format (ISO/IEC 18013-5 12.3.6.4), and deliberately malformed
 * variants of them, for the conditions that consume an identifier list.
 */
final class IdentifierListCwtTestFixtures {

	static final String DEFAULT_URI = "https://issuer.example.com/identifierlists/1";

	/** An identifier the default fixtures put on the list, i.e. a revoked MSO. */
	static final byte[] LISTED_IDENTIFIER = { 1, 2, 3, 4, 5, 6, 7, 8 };

	/** An identifier the default fixtures leave off the list, i.e. an MSO that is not revoked. */
	static final byte[] UNLISTED_IDENTIFIER = { 9, 9, 9, 9 };

	private IdentifierListCwtTestFixtures() {
		// utility class
	}

	/** A well formed, Table B.9 conformant identifier list containing {@link #LISTED_IDENTIFIER}. */
	static byte[] validIdentifierListToken() throws Exception {
		return identifierListToken(DEFAULT_URI, claimsSet(DEFAULT_URI, List.of(LISTED_IDENTIFIER)),
			AbstractIdentifierListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/** A well formed identifier list whose {@code identifiers} map is empty. */
	static byte[] emptyIdentifierListToken() throws Exception {
		return identifierListToken(DEFAULT_URI, claimsSet(DEFAULT_URI, List.of()),
			AbstractIdentifierListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/**
	 * An identifier list declaring the status list media type, which ISO/IEC 18013-5 12.3.6.4
	 * forbids ("The value of the type claim shall be 'application/identifierlist+cwt'").
	 */
	static byte[] identifierListTokenWithStatusListType() throws Exception {
		return identifierListToken(DEFAULT_URI, claimsSet(DEFAULT_URI, List.of(LISTED_IDENTIFIER)),
			AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE);
	}

	/**
	 * An identifier list that also carries the StatusList claim (key 65533), which ISO/IEC
	 * 18013-5 12.3.6.4 says "shall not be present in the CWT claims set".
	 */
	static byte[] identifierListTokenWithStatusListClaim() throws Exception {
		Map<DataItem, DataItem> statusList = new LinkedHashMap<>();
		statusList.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(1));
		statusList.put(new Tstr("lst"), new Bstr(new byte[] { 0x78, (byte) 0xDA }));

		byte[] payload = claimsSet(DEFAULT_URI, List.of(LISTED_IDENTIFIER),
			Map.of(DataItemExtensionsKt.toDataItem(65533L), new CborMap(statusList, false)));
		return identifierListToken(DEFAULT_URI, payload,
			AbstractIdentifierListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/** An identifier list with no IdentifierList claim (key 65530) at all. */
	static byte[] identifierListTokenWithoutIdentifierListClaim() throws Exception {
		long now = Instant.now().getEpochSecond();
		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(2L), new Tstr(DEFAULT_URI));
		claims.put(DataItemExtensionsKt.toDataItem(4L), DataItemExtensionsKt.toDataItem(now + 600));
		claims.put(DataItemExtensionsKt.toDataItem(6L), DataItemExtensionsKt.toDataItem(now));

		return identifierListToken(DEFAULT_URI, Cbor.INSTANCE.encode(new CborMap(claims, false)),
			AbstractIdentifierListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/** Base64 (standard, as the environment stores it) of the given token bytes. */
	static String encode(byte[] token) {
		return Base64.getEncoder().encodeToString(token);
	}

	private static byte[] identifierListToken(String uri, byte[] payload, String type) throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		X509CertChain chain = StatusListCwtTestFixtures.signerCertChain(key,
			StatusListCwtTestFixtures.CertProfile.CONFORMANT);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(Algorithm.ES256.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_TYP), new Tstr(type));
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN), chain.toDataItem());

		CoseSign1 coseSign1 = StatusListCwtTestFixtures.sign(
			StatusListCwtTestFixtures.signingKey(chain, key), payload, protectedHeaders);
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	private static byte[] claimsSet(String uri, List<byte[]> identifiers) {
		return claimsSet(uri, identifiers, Map.of());
	}

	/** The CWT claims set: sub, exp, iat, the IdentifierList claim (65530) and ttl. */
	private static byte[] claimsSet(String uri, List<byte[]> identifiers,
			Map<DataItem, DataItem> extraClaims) {
		Map<DataItem, DataItem> identifiersMap = new LinkedHashMap<>();
		for (byte[] identifier : identifiers) {
			// IdentifierInfo is "{ tstr/int => RFU }", so an empty map is the only defined content
			identifiersMap.put(new Bstr(identifier), new CborMap(new LinkedHashMap<>(), false));
		}

		Map<DataItem, DataItem> identifierList = new LinkedHashMap<>();
		identifierList.put(new Tstr("identifiers"), new CborMap(identifiersMap, false));

		long now = Instant.now().getEpochSecond();
		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(2L), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(4L), DataItemExtensionsKt.toDataItem(now + 600));
		claims.put(DataItemExtensionsKt.toDataItem(6L), DataItemExtensionsKt.toDataItem(now));
		claims.put(DataItemExtensionsKt.toDataItem(65530L), new CborMap(identifierList, false));
		claims.put(DataItemExtensionsKt.toDataItem(65534L), DataItemExtensionsKt.toDataItem(720L));
		claims.putAll(extraClaims);

		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}
}
