package net.openid.conformance.vci10wallet.condition.statuslist;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractGenerateCwtStatusListToken;
import net.openid.conformance.fapi2spfinal.VCIClientProfileBehavior;
import net.openid.conformance.oauth.statuslists.CwtStatusListTokenBuilder;
import net.openid.conformance.testmodule.Environment;

import java.time.Duration;

/**
 * Generates the emulated credential issuer's Token Status List as the MSO revocation list of
 * the mdocs it issues. The signing JWK in the test configuration plays no part: it signs SD-JWT
 * VCs (and the JWT format status list that goes with them), which is a separate trust chain.
 *
 * <p>The list contents are the same as {@link VCIGenerateJwtStatusListToken} produces; only the
 * representation differs, see {@link CwtStatusListTokenBuilder}.
 *
 * <p>Stores the token base64 encoded in {@code current_status_list_cwt}.
 */
public class VCIGenerateCwtStatusListToken extends AbstractGenerateCwtStatusListToken {

	@Override
	@PreEnvironment(strings = { "current_status_list_id" })
	@PostEnvironment(strings = { "current_status_list_cwt" })
	public Environment evaluate(Environment env) {

		String currentStatusListId = env.getString("current_status_list_id");
		String currentStatusListUri =
			VCIClientProfileBehavior.getStatusListUrl(env, currentStatusListId);

		// draft-ietf-oauth-status-list section 4.3: optional pointer to the Status List
		// Aggregation this issuer serves (section 9.3)
		String aggregationUri = env.getString("server", "status_list_aggregation_endpoint");

		generateStatusListToken(env, "current_status_list_cwt", currentStatusListUri,
			Duration.ofMinutes(10), Duration.ofMinutes(12), aggregationUri);

		return env;
	}
}
