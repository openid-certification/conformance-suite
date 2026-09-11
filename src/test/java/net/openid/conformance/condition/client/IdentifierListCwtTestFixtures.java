package net.openid.conformance.condition.client;

import net.openid.conformance.oauth.statuslists.StatusListCwt;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Tstr;
import org.multipaz.crypto.AsymmetricKey;

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
		return token(identifierListClaims(List.of(LISTED_IDENTIFIER)),
			AbstractRevocationListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/** A well formed identifier list whose {@code identifiers} map is empty. */
	static byte[] emptyIdentifierListToken() throws Exception {
		return token(identifierListClaims(List.of()),
			AbstractRevocationListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/**
	 * An identifier list declaring the status list media type, which ISO/IEC 18013-5 12.3.6.4
	 * forbids ("The value of the type claim shall be 'application/identifierlist+cwt'").
	 */
	static byte[] identifierListTokenWithStatusListType() throws Exception {
		return token(identifierListClaims(List.of(LISTED_IDENTIFIER)), StatusListCwt.CONTENT_TYPE);
	}

	/**
	 * An identifier list that also carries the StatusList claim (key 65533), which ISO/IEC
	 * 18013-5 12.3.6.4 says "shall not be present in the CWT claims set".
	 */
	static byte[] identifierListTokenWithStatusListClaim() throws Exception {
		Map<DataItem, DataItem> statusList = new LinkedHashMap<>();
		statusList.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(1));
		statusList.put(new Tstr("lst"), new Bstr(new byte[] { 0x78, (byte) 0xDA }));

		Map<DataItem, DataItem> claims = identifierListClaims(List.of(LISTED_IDENTIFIER));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_STATUS_LIST), new CborMap(statusList, false));
		return token(claims, AbstractRevocationListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	/** An identifier list with no IdentifierList claim (key 65530) at all. */
	static byte[] identifierListTokenWithoutIdentifierListClaim() throws Exception {
		return token(StatusListCwtTestFixtures.baseClaims(DEFAULT_URI),
			AbstractRevocationListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}

	private static byte[] token(Map<DataItem, DataItem> claims, String type) throws Exception {
		AsymmetricKey.X509CertifiedExplicit signer = StatusListCwtTestFixtures.conformantSigner();
		return StatusListCwtTestFixtures.token(StatusListCwtTestFixtures.encodeClaims(claims),
			StatusListCwtTestFixtures.ES256_ALG, new Tstr(type), signer.getCertChain(), true, signer);
	}

	/** The CWT claims set: the shared sub, exp, iat and ttl plus the IdentifierList claim (65530). */
	private static Map<DataItem, DataItem> identifierListClaims(List<byte[]> identifiers) {
		Map<DataItem, DataItem> identifiersMap = new LinkedHashMap<>();
		for (byte[] identifier : identifiers) {
			// IdentifierInfo is "{ tstr/int => RFU }", so an empty map is the only defined content
			identifiersMap.put(new Bstr(identifier), new CborMap(new LinkedHashMap<>(), false));
		}

		Map<DataItem, DataItem> identifierList = new LinkedHashMap<>();
		identifierList.put(new Tstr("identifiers"), new CborMap(identifiersMap, false));

		Map<DataItem, DataItem> claims = StatusListCwtTestFixtures.baseClaims(DEFAULT_URI);
		claims.put(DataItemExtensionsKt.toDataItem(AbstractRevocationListCwtCondition.CWT_CLAIM_IDENTIFIER_LIST),
			new CborMap(identifierList, false));
		return claims;
	}
}
