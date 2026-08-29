package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;

import java.util.Base64;
import java.util.Map;

/**
 * Shared plumbing for the conditions that consume an MSO revocation list implementing the
 * identifier list mechanism of ISO/IEC 18013-5 12.3.6.4.
 *
 * <p>The envelope is the one 12.3.6.3 defines for both mechanisms - a Status List Token in CWT
 * format - so the CWT parsing lives in {@link AbstractStatusListCwtCondition} and is reused here;
 * only the token's storage location, its media type and the claim it carries differ.
 */
public abstract class AbstractIdentifierListCwtCondition extends AbstractStatusListCwtCondition {

	/** Media type of an identifier list in CWT format (ISO/IEC 18013-5 12.3.6.4). */
	public static final String IDENTIFIER_LIST_CWT_CONTENT_TYPE = "application/identifierlist+cwt";

	/** Environment string holding the base64 encoded identifier list token bytes. */
	public static final String ENV_IDENTIFIER_LIST_TOKEN = "mdoc_identifier_list_token";

	/** Environment string holding the URI the identifier list token was fetched from. */
	public static final String ENV_IDENTIFIER_LIST_URI = "mdoc_identifier_list_uri";

	/**
	 * Environment string holding the MSO's own Identifier - the {@code id} element of the MSO's
	 * identifier_list structure - base64 encoded.
	 */
	public static final String ENV_IDENTIFIER_LIST_ID = "mdoc_identifier_list_id";

	/** Environment object holding the identifier list token HTTP response (status and headers). */
	public static final String ENV_IDENTIFIER_LIST_RESPONSE = "mdoc_identifier_list_token_endpoint_response";

	/** CWT claim key of the IdentifierList structure, ISO/IEC 18013-5 12.3.6.4. */
	protected static final long CWT_CLAIM_IDENTIFIER_LIST = 65530;

	@Override
	protected String getTokenEnvKey() {
		return ENV_IDENTIFIER_LIST_TOKEN;
	}

	/** The MSO's own Identifier, as stored by {@link FetchMdocIdentifierListToken}. */
	protected byte[] getMsoIdentifier(Environment env) {
		String encoded = env.getString(ENV_IDENTIFIER_LIST_ID);
		if (encoded == null) {
			throw error("The mdoc's own identifier is missing from the environment");
		}
		try {
			return Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException e) {
			throw error("Failed to base64 decode the stored mdoc identifier", e);
		}
	}

	/**
	 * Returns the {@code identifiers} map of the IdentifierList claim (CWT claim key 65530), or
	 * null when the claim or the map is absent or not of the CDDL-required shape.
	 */
	protected Map<DataItem, DataItem> getIdentifiers(DataItem claims) {
		DataItem identifierList = getClaim(claims, CWT_CLAIM_IDENTIFIER_LIST);
		if (identifierList == null) {
			return null;
		}
		DataItem identifiers = identifierList.getOrNull("identifiers");
		if (!(identifiers instanceof CborMap map)) {
			return null;
		}
		return map.getItems();
	}

	/**
	 * Whether the given identifier is a key of the IdentifierList's {@code identifiers} map.
	 * ISO/IEC 18013-5 12.3.6.4: "Presence of the Identifier in the IdentifierList indicates that
	 * the MSO that contains that identifier in the status element is revoked."
	 */
	protected boolean containsIdentifier(Map<DataItem, DataItem> identifiers, byte[] identifier) {
		for (DataItem key : identifiers.keySet()) {
			if (key instanceof Bstr bstr && java.util.Arrays.equals(bstr.getValue(), identifier)) {
				return true;
			}
		}
		return false;
	}
}
