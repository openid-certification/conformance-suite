package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractEnsureResponseType;
import net.openid.conformance.testmodule.Environment;

public class CheckNoPresentationSubmissionParameter extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement presentationSubmission = env.getElementFromObject("authorization_endpoint_response", "presentation_submission");

		if (presentationSubmission != null) {
			throw error("The presentation_submission parameter must not be used when dcql_query was present in the request.",
				args("presentation_submission", presentationSubmission));
		}

		logSuccess("presentation_submission parameter is not present, as expected.");

		return env;
	}

}
