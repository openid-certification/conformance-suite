package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
		return checkMostPreferredOptionReturned(env, option -> DcqlQueryUtils.isClaimSetOptionPresent(decoded, option));
	}
}
