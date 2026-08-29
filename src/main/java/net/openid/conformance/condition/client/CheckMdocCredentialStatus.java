package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.DataItem;

/**
 * Reads the mdoc's status from the fetched MSO revocation list and fails when the MSO has been
 * revoked. ISO/IEC 18013-5 12.3.6.1: for an mdoc "no other status besides 'revoked' shall be
 * used", so any status other than VALID means the credential is revoked.
 */
public class CheckMdocCredentialStatus extends AbstractStatusListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_STATUS_LIST_TOKEN })
	public Environment evaluate(Environment env) {
		ParsedStatusListCwt parsed = parseStatusListCwt(env);

		Integer idx = env.getInteger(ENV_STATUS_LIST_IDX);
		if (idx == null) {
			throw error("The MSO's status list index is missing from the environment");
		}

		DataItem statusListClaim = getClaim(parsed.claims(), CWT_CLAIM_STATUS_LIST);
		if (statusListClaim == null) {
			throw error("The MSO revocation list's CWT claims set does not contain the status_list"
				+ " claim (key 65533)");
		}

		DataItem bitsItem = statusListClaim.getOrNull("bits");
		DataItem lstItem = statusListClaim.getOrNull("lst");
		if (bitsItem == null || lstItem == null) {
			throw error("The MSO revocation list's status_list claim is missing the 'bits' or 'lst' element");
		}
		if (!(lstItem instanceof Bstr lst)) {
			throw error("The MSO revocation list's status_list 'lst' element is not a CBOR byte string");
		}

		int bits;
		try {
			bits = (int) bitsItem.getAsNumber();
		} catch (Exception e) {
			throw error("The MSO revocation list's status_list 'bits' element is not a number", e);
		}

		TokenStatusList.Status status;
		try {
			status = TokenStatusList.decodeCompressed(lst.getValue(), bits).getStatus(idx);
		} catch (TokenStatusList.TokenStatusListException e) {
			throw error("Failed to read the mdoc's status from the MSO revocation list", e,
				args("idx", idx, "bits", bits));
		}

		if (!TokenStatusList.Status.VALID.equals(status)) {
			throw error("The mdoc's MSO has been revoked according to the MSO revocation list",
				args("status", status.name(),
					"idx", idx,
					"uri", env.getString(ENV_STATUS_LIST_URI)));
		}

		logSuccess("The mdoc's MSO is not revoked according to the MSO revocation list",
			args("status", status.name(), "idx", idx, "uri", env.getString(ENV_STATUS_LIST_URI)));
		return env;
	}
}
