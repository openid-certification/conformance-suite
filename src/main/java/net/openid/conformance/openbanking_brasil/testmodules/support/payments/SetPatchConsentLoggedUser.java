package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetPatchConsentLoggedUser extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"consent_endpoint_response","config"})
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("consent_endpoint_response");
		response = response.getAsJsonObject("data");
		JsonObject loggedUser = response.getAsJsonObject("loggedUser");
		JsonElement patchConsent = env.getElementFromObject("resource","brazilPatchPaymentConsent");

		JsonObject obj = patchConsent.getAsJsonObject();
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("revocation");
		obj.add("loggedUser",loggedUser);
		obj = obj.getAsJsonObject("reason");
		obj.addProperty("additionalInformation", DictHomologKeys.PROXY_EMAIL_STANDARD_ADDITIONALINFORMATION);
		return env;
	}
}
