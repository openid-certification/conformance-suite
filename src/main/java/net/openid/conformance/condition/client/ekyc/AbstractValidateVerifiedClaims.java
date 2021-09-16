package net.openid.conformance.condition.client.ekyc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractValidateVerifiedClaims extends AbstractCondition {

	/**
	 * Compare elements one by one
	 * @param requestedVerifiedClaims
	 * @param returnedVerifiedClaims
	 */
	protected void validateVerifiedClaims(JsonElement requestedVerifiedClaims, JsonObject returnedVerifiedClaims) {

	}


	protected void validateSingleVerifiedClaimObject(JsonElement requestedVerifiedClaim, JsonObject returnedVerifiedClaim) {

	}

	protected void validateOneOnOne(JsonObject requestedVerifiedClaim, JsonObject returnedVerifiedClaim) {

	}

}
