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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a Status List Token in CWT format (draft-ietf-oauth-status-list section 5.2), the
 * representation ISO/IEC 18013-5 12.3.6.3 requires for an MSO revocation list: a COSE_Sign1
 * (tagged 18) carrying the signature algorithm, the {@code application/statuslist+cwt} type and
 * the x5chain in its protected header, over a CWT claims set using the status list claim keys.
 *
 * <p>Used by the emulated OpenID4VCI issuer's status list endpoint.
 */
public final class CwtStatusListTokenBuilder {

	private CwtStatusListTokenBuilder() {
		// utility class
	}

	/**
	 * @param uri the URI the status list is published at, used as the {@code sub} claim
	 * @param iat the issuance time
	 * @param exp the expiry time; ISO/IEC 18013-5 12.3.6.3 requires it to be present
	 * @param ttlSeconds the {@code ttl} claim
	 * @param bits bits per status list entry
	 * @param compressedStatusList the zlib compressed status list, as the {@code lst} byte string
	 * @param signingKey the key to sign the COSE_Sign1 with; its certificate chain goes into the
	 *   protected header, as ISO/IEC 18013-5 12.3.6.3 requires
	 * @param algorithm the COSE signature algorithm; ISO/IEC 18013-5 12.3.6.3 permits only the
	 *   EC based algorithms
	 * @return the CBOR encoded, tagged COSE_Sign1
	 */
	public static byte[] build(String uri, Instant iat, Instant exp, long ttlSeconds, int bits,
			byte[] compressedStatusList, AsymmetricKey.X509CertifiedExplicit signingKey, Algorithm algorithm)
			throws Exception {

		byte[] payload = buildClaimsSet(uri, iat, exp, ttlSeconds, bits, compressedStatusList);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(algorithm.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_TYP),
			new Tstr(StatusListCwt.CONTENT_TYPE));
		// ISO/IEC 18013-5 12.3.6.3: the x5chain goes in the protected header
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN), signingKey.getCertChain().toDataItem());

		CoseSign1 coseSign1 = (CoseSign1) kotlinx.coroutines.BuildersKt.runBlocking(
			kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> Cose.INSTANCE.coseSign1Sign(signingKey, payload, true,
				protectedHeaders, Map.of(), continuation));

		// draft-ietf-oauth-status-list section 5.2: the COSE message is the tagged COSE_Sign1
		return Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));
	}

	private static byte[] buildClaimsSet(String uri, Instant iat, Instant exp, long ttlSeconds,
			int bits, byte[] compressedStatusList) {
		Map<DataItem, DataItem> statusListClaim = new LinkedHashMap<>();
		statusListClaim.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(bits));
		statusListClaim.put(new Tstr("lst"), new Bstr(compressedStatusList));

		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_SUB), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_EXP),
			DataItemExtensionsKt.toDataItem(exp.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_IAT),
			DataItemExtensionsKt.toDataItem(iat.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_STATUS_LIST),
			new CborMap(statusListClaim, false));
		claims.put(DataItemExtensionsKt.toDataItem(StatusListCwt.CLAIM_TTL),
			DataItemExtensionsKt.toDataItem(ttlSeconds));

		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}
}
