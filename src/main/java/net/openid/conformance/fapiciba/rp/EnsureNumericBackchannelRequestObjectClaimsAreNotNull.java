package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This condition should only be used to log WARNINGs and should not result in failures
 */
public class EnsureNumericBackchannelRequestObjectClaimsAreNotNull extends AbstractCondition {


	/**
	 * Names of numeric claims
	 * Also used by CreateEffectiveAuthorizationRequestParameters
	 */
	public static final Set<String> numericClaimNames = Set.of("max_age");

	@Override
	@PreEnvironment(required = {"backchannel_request_object"})
	public Environment evaluate(Environment env) {
		Map<String, Object> argsForLog = new HashMap<>();

		for(String claimName : numericClaimNames) {
			JsonElement jsonElement = env.getElementFromObject("backchannel_request_object", "claims." + claimName);
			if(jsonElement!=null && jsonElement.isJsonNull()) {
				argsForLog.put(claimName, "Should have a numeric value.");
			}
		}
		if(!argsForLog.isEmpty()) {
			throw error("Request object contains null value(s) for claim(s) that are expected to have numeric values." +
					" This is allowed but not recommended.",
						args("claims", argsForLog));
		}
		logSuccess("None of the claims expected to have numeric values, have null values",
						args("numeric_claims", numericClaimNames));
		return env;
	}

}
