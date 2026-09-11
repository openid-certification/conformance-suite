package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.DataItem;

import java.time.Instant;

/**
 * Reads the mdoc's revocation status from the fetched MSO revocation list into
 * {@code mdoc_revocation_status}, for {@link EnsureMdocNotRevoked} to act on.
 *
 * <p>Reading the status is separate from acting on it so the caller can report a list whose
 * status cannot be read at the same severity as the other retrieval and format problems, and
 * only a status that was actually read as revoked at the severity a revoked credential deserves.
 *
 * <p>The list must be within its exp to be read: the format check covers a list when it is
 * fetched, but a list retrieved for an earlier credential of the same test is reused rather than
 * fetched again, so the expiry is checked here for every credential.
 *
 * <p>ISO/IEC 18013-5 12.3.6.5: the status is the value at the MSO's index.
 */
public class ExtractMdocRevocationStatus extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_TOKEN })
	@PostEnvironment(strings = { ENV_STATUS })
	public Environment evaluate(Environment env) {
		ParsedRevocationListCwt parsed = parseRevocationListCwt(env);
		requireNotExpired(env, parsed.claims());

		TokenStatusList.Status status = statusListStatus(env, parsed.claims());

		env.putString(ENV_STATUS, status.name());
		logSuccess("Read the mdoc's status from the MSO revocation list",
			args("status", status.name(), "uri", env.getString(ENV_URI)));
		return env;
	}

	private void requireNotExpired(Environment env, DataItem claims) {
		DataItem exp = getClaim(claims, StatusListCwt.CLAIM_EXP);
		Long expiry = exp == null ? null : numericDateSeconds(exp);
		if (expiry == null) {
			throw error("The MSO revocation list's exp claim (key 4) is missing or not a NumericDate, so"
				+ " the list cannot be relied on", args("uri", env.getString(ENV_URI)));
		}
		if (Instant.ofEpochSecond(expiry).isBefore(Instant.now())) {
			throw error("The MSO revocation list has expired, so the mdoc's status cannot be read from it",
				args("exp", Instant.ofEpochSecond(expiry).toString(), "uri", env.getString(ENV_URI)));
		}
	}

	private TokenStatusList.Status statusListStatus(Environment env, DataItem claims) {
		Integer idx = env.getInteger(ENV_STATUS_LIST_IDX);
		if (idx == null) {
			throw error("The MSO's status list index is missing from the environment");
		}

		DataItem statusListClaim = getClaim(claims, StatusListCwt.CLAIM_STATUS_LIST);
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

		try {
			TokenStatusList.Status status = TokenStatusList.decodeCompressed(lst.getValue(), bits).getStatus(idx);
			log("Read the status at the MSO's index of the status list",
				args("status", status.name(), "idx", idx, "bits", bits));
			return status;
		} catch (TokenStatusList.TokenStatusListException e) {
			throw error("Failed to read the mdoc's status from the MSO revocation list", e,
				args("idx", idx, "bits", bits));
		}
	}
}
