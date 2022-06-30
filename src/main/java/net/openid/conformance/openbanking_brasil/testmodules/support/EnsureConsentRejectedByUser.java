package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureConsentRejectedByUser extends AbstractJsonAssertingCondition {


	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement consentResponse = bodyFrom(env);
		if (consentResponse.getAsJsonObject().entrySet().isEmpty()) {
			throw error("Consent response was not found.");
		}
		String expectedResult = "USER";
		JsonElement rejectionElement = findByPath(consentResponse, "$.data.rejection.rejectedBy");
		String status = OIDFJSON.getString(rejectionElement);
		if(Strings.isNullOrEmpty(status)) {
			throw error("Consent RejectedBy was not found.");
		}

		if(!status.equals(expectedResult)) {
			throw error(String.format("Expected RejectedBy to be %s but it was not.", expectedResult), args("RejectedBy", expectedResult));
		}

		 logSuccess(String.format("RejectedBy is %s as expected", expectedResult), args("RejectedBy", expectedResult));

		return env;
	}

}
