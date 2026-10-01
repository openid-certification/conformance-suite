package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Checks that, where the DCQL query uses claim_sets, the presentation contains the claims of the
 * first option, which is the one the Verifier prefers.
 *
 * OID4VP 1.0 section 6.4.1: "The order of the options conveyed in the claim_sets array expresses
 * the Verifier's preference for what is returned; the Wallet SHOULD return the first option that
 * it can satisfy." The suite cannot see which claims the wallet's credential contains, so it cannot
 * tell a wallet that could not satisfy the first option from one that chose not to; callers invoke
 * this at WARNING severity.
 */
public abstract class AbstractEnsureMostPreferredDcqlClaimSetReturned extends AbstractCondition {

	/**
	 * @param isOptionPresent whether the presentation contains every claim path of a claim_sets option
	 */
	protected Environment checkMostPreferredOptionReturned(Environment env, Predicate<Set<List<String>>> isOptionPresent) {

		String credentialId = env.getString("credential_id");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(env.getObject("dcql_query"), credentialId);
		List<Set<List<String>>> options = matchingCredential == null
			? List.of() : DcqlQueryUtils.extractClaimSetOptions(matchingCredential);
		if (options.size() < 2) {
			log("DCQL credential entry has fewer than two claim_sets options, so there is no preference order to check");
			return env;
		}

		for (int i = 0; i < options.size(); i++) {
			if (!isOptionPresent.test(options.get(i))) {
				continue;
			}
			if (i == 0) {
				logSuccess("The presentation contains the claims of the verifier's most preferred claim_sets option",
					args("returned_claim_paths", options.get(0)));
				return env;
			}
			throw error("The presentation does not contain the claims of the verifier's most preferred "
					+ "claim_sets option; a less preferred option was returned instead. The wallet "
					+ "should return the first option it can satisfy, so this is only correct if the "
					+ "credential does not contain the claims of the more preferred options.",
				args("returned_option_number", i + 1,
					"returned_claim_paths", options.get(i),
					"most_preferred_claim_paths", options.get(0),
					"claim_sets_options", options,
					"credential_id", credentialId));
		}

		log("The presentation does not contain all the claims of any claim_sets option, so the preference order cannot be checked",
			args("claim_sets_options", options));
		return env;
	}
}
