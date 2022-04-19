package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetPatchConsentsRevokedByTpp extends AbstractCondition {
	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement patchConsent = env.getElementFromObject("consent_endpoint_request","data");
		log(patchConsent.toString());

		JsonObject obj = patchConsent.getAsJsonObject();

		obj = obj.getAsJsonObject("revocation");
		obj.addProperty("revokedBy","TPP");
		log(obj);

		logSuccess("Set PATCH revokedBy  to TPP");

		return env;
	}
}
