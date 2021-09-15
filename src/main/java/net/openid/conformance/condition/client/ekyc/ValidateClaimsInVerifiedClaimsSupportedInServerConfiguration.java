package net.openid.conformance.condition.client.ekyc;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClaimsInVerifiedClaimsSupportedInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(jsonElement == null) {
			throw error("claims_in_verified_claims_supported is not set");
		}
		if(!jsonElement.isJsonArray()) {
			throw error("claims_in_verified_claims_supported must be a json array", args("actual", jsonElement));
		}
		//TODO requiring at least one claim? or is an empty value is also allowed?
		if(jsonElement.getAsJsonArray().size()<1) {
			throw error("At least one claim is required in claims_in_verified_claims_supported for this test to work as expected");
		}
		logSuccess("claims_in_verified_claims_supported is valid", args("actual", jsonElement));
		return env;
	}
}
