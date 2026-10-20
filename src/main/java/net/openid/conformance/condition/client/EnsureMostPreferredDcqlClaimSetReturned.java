package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * {@link AbstractEnsureMostPreferredDcqlClaimSetReturned} for an SD-JWT VC presentation.
 */
public class EnsureMostPreferredDcqlClaimSetReturned extends AbstractEnsureMostPreferredDcqlClaimSetReturned {

	@Override
	@PreEnvironment(required = {"sdjwt", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {
		JsonObject decoded = DcqlQueryUtils.getDecodedSdJwtClaims(env);
		if (decoded == null) {
			throw error("No decoded SD-JWT claims found in environment");
		}
		List<DcqlQueryUtils.DisclosedClaim> disclosedClaims = DcqlQueryUtils.extractDisclosedSdJwtClaims(env, decoded);
		return checkMostPreferredOptionReturned(env,
			option -> DcqlQueryUtils.isClaimSetOptionPresent(decoded, option),
			option -> DcqlQueryUtils.findUnrequestedDisclosures(disclosedClaims, option).isEmpty());
	}
}
