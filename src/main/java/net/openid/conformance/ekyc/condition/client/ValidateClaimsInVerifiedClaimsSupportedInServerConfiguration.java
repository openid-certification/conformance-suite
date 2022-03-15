package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
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
		//it makes sense to require at least one entry, otherwise eKYC tests would be pointless
		if(jsonElement.getAsJsonArray().size()<1) {
			throw error("At least one claim is required in claims_in_verified_claims_supported for eKYC tests to work as expected");
		}
		logSuccess("claims_in_verified_claims_supported is valid", args("actual", jsonElement));
		return env;
	}
}
