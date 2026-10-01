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
	 * The returned option is chosen as the data minimization checks choose it (see
	 * {@link DcqlQueryUtils#selectRequestedClaimPaths}), so a presentation of a less preferred superset
	 * of the first option is not mistaken for the first option.
	 *
	 * @param isSatisfied whether the presentation contains every claim path of a claim_sets option
	 * @param fitsWithin  whether every disclosure in the presentation is requested by a claim_sets option
	 */
	protected Environment checkMostPreferredOptionReturned(Environment env, Predicate<Set<List<String>>> isSatisfied,
			Predicate<Set<List<String>>> fitsWithin) {

		String credentialId = env.getString("credential_id");
		JsonObject matchingCredential = DcqlQueryUtils.findCredentialById(env.getObject("dcql_query"), credentialId);
		List<Set<List<String>>> options = matchingCredential == null
			? List.of() : DcqlQueryUtils.extractClaimSetOptions(matchingCredential);
		if (options.size() < 2) {
			log("DCQL credential entry has fewer than two claim_sets options, so there is no preference order to check");
			return env;
		}

		boolean anySatisfied = options.stream().anyMatch(isSatisfied);
		if (!anySatisfied) {
			log("The presentation does not contain all the claims of any claim_sets option, so the preference order cannot be checked",
				args("claim_sets_options", options));
			return env;
		}
		int returned = options.indexOf(DcqlQueryUtils.selectRequestedClaimPaths(options, Set.of(), isSatisfied, fitsWithin));
		if (returned == 0) {
			logSuccess("The presentation contains the claims of the verifier's most preferred claim_sets option",
				args("returned_claim_paths", options.get(0)));
			return env;
		}
		throw error("The presentation does not contain the claims of the verifier's most preferred "
				+ "claim_sets option; a less preferred option was returned instead. The wallet "
				+ "should return the first option it can satisfy, so this is only correct if the "
				+ "credential does not contain the claims of the more preferred options.",
			args("returned_option_number", returned + 1,
				"returned_claim_paths", options.get(returned),
				"most_preferred_claim_paths", options.get(0),
				"claim_sets_options", options,
				"credential_id", credentialId));
	}
}
