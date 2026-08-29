package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;

import java.util.Base64;
import java.util.Map;

/**
 * Shared plumbing for the conditions that consume an MSO revocation list, i.e. a Token Status
 * List Token in CWT format as required by ISO/IEC 18013-5 12.3.6.3.
 *
 * <p>The raw token bytes are held base64 encoded in the {@code mdoc_status_list_token}
 * environment string (written by {@link FetchMdocStatusListToken}); each condition re-parses
 * them so no non-JSON object needs to live in the environment.
 */
public abstract class AbstractStatusListCwtCondition extends AbstractCondition {

	/** Media type of a Status List Token in CWT format (draft-ietf-oauth-status-list section 5.2). */
	public static final String STATUS_LIST_CWT_CONTENT_TYPE = "application/statuslist+cwt";

	/** Environment string holding the base64 encoded status list token bytes. */
	public static final String ENV_STATUS_LIST_TOKEN = "mdoc_status_list_token";

	/** Environment string holding the URI the status list token was fetched from. */
	public static final String ENV_STATUS_LIST_URI = "mdoc_status_list_uri";

	/** Environment integer holding the MSO's index into the status list. */
	public static final String ENV_STATUS_LIST_IDX = "mdoc_status_list_idx";

	/** Environment object holding the status list token HTTP response (status and headers). */
	public static final String ENV_STATUS_LIST_RESPONSE = "mdoc_status_list_token_endpoint_response";

	// CWT claim keys, draft-ietf-oauth-status-list section 5.2
	protected static final long CWT_CLAIM_SUB = 2;
	protected static final long CWT_CLAIM_EXP = 4;
	protected static final long CWT_CLAIM_IAT = 6;
	protected static final long CWT_CLAIM_STATUS_LIST = 65533;
	protected static final long CWT_CLAIM_TTL = 65534;

	/**
	 * A parsed status list token.
	 *
	 * @param coseSign1 the COSE_Sign1 structure
	 * @param tagged whether it was tagged with the COSE_Sign1 tag (18), which
	 *   draft-ietf-oauth-status-list section 5.2 requires
	 * @param claims the CWT claims set decoded from the COSE_Sign1 payload
	 */
	protected record ParsedStatusListCwt(CoseSign1 coseSign1, boolean tagged, DataItem claims) {
	}

	protected byte[] getStatusListTokenBytes(Environment env) {
		String encoded = env.getString(ENV_STATUS_LIST_TOKEN);
		if (encoded == null) {
			throw error("No status list token found in the environment");
		}
		try {
			return Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException e) {
			throw error("Failed to base64 decode the stored status list token", e);
		}
	}

	/**
	 * Parses the fetched status list token as a COSE_Sign1 with a CWT claims set payload. Only
	 * structural failures that make any further checking impossible throw here; the individual
	 * ISO/IEC 18013-5 12.3.6.3 requirements are reported by
	 * {@link ValidateStatusListTokenCwtFormat}.
	 */
	protected ParsedStatusListCwt parseStatusListCwt(Environment env) {
		byte[] tokenBytes = getStatusListTokenBytes(env);

		DataItem decoded;
		try {
			decoded = Cbor.INSTANCE.decode(tokenBytes);
		} catch (Exception e) {
			throw error("The status list token is not valid CBOR; ISO/IEC 18013-5 12.3.6.3 requires"
				+ " an MSO revocation list to be a Status List Token in CWT format", e);
		}

		boolean tagged = false;
		DataItem coseItem = decoded;
		if (decoded instanceof Tagged tag) {
			if (tag.getTagNumber() != Tagged.COSE_SIGN1) {
				throw error("The status list token is tagged with CBOR tag " + tag.getTagNumber()
					+ "; draft-ietf-oauth-status-list section 5.2 requires the COSE_Sign1 tag (18)");
			}
			tagged = true;
			coseItem = tag.getTaggedItem();
		}

		CoseSign1 coseSign1;
		try {
			coseSign1 = CoseSign1.Companion.fromDataItem(coseItem);
		} catch (Exception e) {
			throw error("The status list token could not be parsed as a COSE_Sign1 structure;"
				+ " ISO/IEC 18013-5 12.3.6.3 requires the MSO revocation list to be a COSE_Sign1 object", e);
		}

		byte[] payload = coseSign1.getPayload();
		if (payload == null || payload.length == 0) {
			throw error("The status list token's COSE_Sign1 structure has no payload,"
				+ " so it carries no CWT claims set");
		}

		DataItem claims;
		try {
			claims = Cbor.INSTANCE.decode(payload);
		} catch (Exception e) {
			throw error("The status list token's CWT claims set is not valid CBOR", e);
		}
		if (!(claims instanceof CborMap)) {
			throw error("The status list token's CWT claims set is not a CBOR map");
		}

		return new ParsedStatusListCwt(coseSign1, tagged, claims);
	}

	/**
	 * Returns the x5chain (COSE header label 33) from the protected headers, or null when it is
	 * absent. ISO/IEC 18013-5 12.3.6.3 requires it to be in the protected header.
	 */
	protected X509CertChain getProtectedX5chain(CoseSign1 coseSign1) {
		Map<CoseLabel, DataItem> protectedHeaders = coseSign1.getProtectedHeaders();
		DataItem x5chainItem = protectedHeaders.get(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN));
		if (x5chainItem == null) {
			return null;
		}
		try {
			return x5chainItem.getAsX509CertChain();
		} catch (Exception e) {
			throw error("The status list token's x5chain protected header could not be parsed"
				+ " as an X.509 certificate chain", e);
		}
	}

	/** Returns the claim with the given CWT claim key, or null when it is absent. */
	protected DataItem getClaim(DataItem claims, long key) {
		try {
			return claims.getOrNull(key);
		} catch (Exception e) {
			return null;
		}
	}
}
