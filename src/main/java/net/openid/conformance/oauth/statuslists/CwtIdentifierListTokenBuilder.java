package net.openid.conformance.oauth.statuslists;

import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tstr;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;

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

	/** CWT claim key of the IdentifierList structure, ISO/IEC 18013-5 12.3.6.4. */
	public static final long CLAIM_IDENTIFIER_LIST = 65530;

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
	 * @param signingKey the key to sign the COSE_Sign1 with; its certificate chain goes in the
	 *   protected header, as ISO/IEC 18013-5 12.3.6.3 requires
	 * @param algorithm the COSE signature algorithm; ISO/IEC 18013-5 12.3.6.3 permits only the
	 *   EC based algorithms
	 * @return the CBOR encoded, tagged COSE_Sign1
	 */
	public static byte[] build(String uri, Instant iat, Instant exp, long ttlSeconds,
			List<byte[]> identifiers, AsymmetricKey.X509CertifiedExplicit signingKey, Algorithm algorithm)
			throws Exception {

		Map<DataItem, DataItem> identifiersMap = new LinkedHashMap<>();
		for (byte[] identifier : identifiers) {
			identifiersMap.put(new Bstr(identifier), new CborMap(new LinkedHashMap<>(), false));
		}

		Map<DataItem, DataItem> identifierList = new LinkedHashMap<>();
		identifierList.put(new Tstr("identifiers"), new CborMap(identifiersMap, false));

		return CwtStatusListTokenBuilder.buildRevocationListCwt(IDENTIFIER_LIST_CWT_CONTENT_TYPE, uri,
			iat, exp, ttlSeconds, CLAIM_IDENTIFIER_LIST, new CborMap(identifierList, false), signingKey,
			algorithm);
	}
}
