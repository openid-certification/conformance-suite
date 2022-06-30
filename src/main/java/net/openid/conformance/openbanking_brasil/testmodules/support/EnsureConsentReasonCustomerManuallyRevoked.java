package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureConsentReasonCustomerManuallyRevoked extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement consentResponse = bodyFrom(env);
		if (consentResponse.getAsJsonObject().entrySet().isEmpty()) {
			throw error("Consent response was not found.");
		}
		String expectedResult = "CUSTOMER_MANUALLY_REVOKED";
		JsonElement rejectionElement = findByPath(consentResponse, "$.data.rejection.reason.code");
		String reason = OIDFJSON.getString(rejectionElement);
		if(Strings.isNullOrEmpty(reason)) {
			throw error("Consent Reason was not found.");
		}

		if(!reason.equals(expectedResult)) {
			throw error(String.format("Expected Reason to be %s but it was not.", expectedResult), args("reason", reason));
		}

		 logSuccess(String.format("Reason is %s as expected", expectedResult), args("reason", reason));

		return env;
	}

}
