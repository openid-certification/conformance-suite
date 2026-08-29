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
import java.util.Map;

/**
 * Builds a Status List Token in CWT format (draft-ietf-oauth-status-list section 5.2), the
 * representation ISO/IEC 18013-5 12.3.6.3 requires for an MSO revocation list: a COSE_Sign1
 * (tagged 18) carrying the signature algorithm, the {@code application/statuslist+cwt} type and
 * the x5chain in its protected header, over a CWT claims set using the status list claim keys.
 *
 * <p>Shared by the emulated OpenID4VCI issuer and the emulated OpenID4VP wallet, which differ
 * only in which key signs the list and which URI identifies it.
 */
public final class CwtStatusListTokenBuilder {

	// CWT claim keys, draft-ietf-oauth-status-list section 5.2
	private static final long CWT_CLAIM_SUB = 2;
	private static final long CWT_CLAIM_EXP = 4;
	private static final long CWT_CLAIM_IAT = 6;
	private static final long CWT_CLAIM_STATUS_LIST = 65533;
	private static final long CWT_CLAIM_TTL = 65534;

	public static final String STATUS_LIST_CWT_CONTENT_TYPE = "application/statuslist+cwt";

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
	 * @param signingKey the key to sign the COSE_Sign1 with
	 * @param algorithm the COSE signature algorithm; ISO/IEC 18013-5 12.3.6.3 permits only the
	 *   EC based algorithms
	 * @param certChain the certificate chain to place in the protected header, or null to omit it
	 *   (which does not meet the ISO/IEC 18013-5 12.3.6.3 requirement, and is only for callers
	 *   whose signing key has no chain)
	 * @return the CBOR encoded, tagged COSE_Sign1
	 */
	public static byte[] build(String uri, Instant iat, Instant exp, long ttlSeconds, int bits,
			byte[] compressedStatusList, AsymmetricKey signingKey, Algorithm algorithm,
			X509CertChain certChain) throws Exception {
		return build(uri, iat, exp, ttlSeconds, bits, compressedStatusList, signingKey, algorithm,
			certChain, null);
	}

	/**
	 * As {@link #build(String, Instant, Instant, long, int, byte[], AsymmetricKey, Algorithm,
	 * X509CertChain)}, additionally embedding the optional aggregation_uri element in the
	 * status_list claim (draft-ietf-oauth-status-list section 4.3) when non-null.
	 */
	public static byte[] build(String uri, Instant iat, Instant exp, long ttlSeconds, int bits,
			byte[] compressedStatusList, AsymmetricKey signingKey, Algorithm algorithm,
			X509CertChain certChain, String aggregationUri) throws Exception {

		byte[] payload = buildClaimsSet(uri, iat, exp, ttlSeconds, bits, compressedStatusList,
			aggregationUri);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(algorithm.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_TYP),
			new Tstr(STATUS_LIST_CWT_CONTENT_TYPE));
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
			int bits, byte[] compressedStatusList, String aggregationUri) {
		Map<DataItem, DataItem> statusListClaim = new LinkedHashMap<>();
		statusListClaim.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(bits));
		statusListClaim.put(new Tstr("lst"), new Bstr(compressedStatusList));
		if (aggregationUri != null) {
			statusListClaim.put(new Tstr("aggregation_uri"), new Tstr(aggregationUri));
		}

		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_SUB), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_EXP),
			DataItemExtensionsKt.toDataItem(exp.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_IAT),
			DataItemExtensionsKt.toDataItem(iat.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_STATUS_LIST),
			new CborMap(statusListClaim, false));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_TTL),
			DataItemExtensionsKt.toDataItem(ttlSeconds));

		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}
}
