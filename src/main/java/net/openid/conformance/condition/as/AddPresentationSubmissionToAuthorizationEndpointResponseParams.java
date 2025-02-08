package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPresentationSubmissionToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		JsonObject presentationSubmission = env.getObject("presentation_submission");
		params.add("presentation_submission", presentationSubmission);

		log("Added presentation_submission to authorization endpoint response params",
			args("presentation_submission", presentationSubmission));

		return env;

	}

}
