package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetPatchConsentsRevokedAndRevokedByTPP extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		JsonElement patchConsent = env.getElementFromObject("resource","brazilPatchPaymentConsent");

		JsonObject obj = patchConsent.getAsJsonObject();
		obj = obj.getAsJsonObject("data");

		obj.addProperty("status","REVOKED");
		logSuccess("Set PATCH status type to REVOKED");

		obj.addProperty("revokedBy","TPP");
		logSuccess("Set PATCH revokedBy  to TPP");

		return null;
	}
}
