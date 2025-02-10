package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractEnsureResponseType;
import net.openid.conformance.testmodule.Environment;

public class ValidatePresentationSubmission extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement presentationSubmission = env.getElementFromObject("authorization_endpoint_response", "presentation_submission");

		if (presentationSubmission == null) {
			throw error("The presentation_submission parameter was not present in the response.");
		}

		if (!presentationSubmission.isJsonObject()) {
			throw error("The presentation_submission parameter in the response is not a JSON object.");
		}

		logSuccess("presentation_submission parameter is present in the response as a JSON object, as expected.");

		return env;
	}

}
