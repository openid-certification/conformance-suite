package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateConsentRejection extends AbstractJsonAssertingCondition {


	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement consentResponse = bodyFrom(env);
		if (consentResponse.getAsJsonObject().entrySet().isEmpty()) {
			throw error("Consent response was not found.");
		}

		JsonElement statusElement = findByPath(consentResponse, "$.data.status");
		String status = OIDFJSON.getString(statusElement);
		if(Strings.isNullOrEmpty(status)) {
			throw error("Consent status was not found.");
		}

		if(!status.equals("REJECTED")) {
			throw error("Expected consent to be in the REJECTED state after redirect but it was not.", args("status", status));
		}

		 findOrThrowError(consentResponse, "$.data.rejection");
		 findOrThrowError(consentResponse, "$.data.rejection.rejectedBy");
		 findOrThrowError(consentResponse,"$.data.rejection.reason");
		 findOrThrowError(consentResponse, "$.data.rejection.reason.code");

		 logSuccess("All mandatory fields are present.", args("consentResponse", consentResponse));

		return env;
	}

	protected JsonElement findOrThrowError(JsonElement consentResponse, String path) {
		JsonElement fieldElement = findByPath(consentResponse, path);
		if (Strings.isNullOrEmpty(fieldElement.toString())) {
			throw error(String.format("Expected field %s but it was not found.", path), args("field", path));
		}

		return fieldElement;
	}
}
