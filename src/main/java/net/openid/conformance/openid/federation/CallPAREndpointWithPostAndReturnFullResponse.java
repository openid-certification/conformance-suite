package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithPost;
import net.openid.conformance.testmodule.Environment;

public class CallPAREndpointWithPostAndReturnFullResponse extends AbstractCallEndpointWithPost {

	@Override
	@PreEnvironment(required = { "primary_entity_statement_jwt", "request_object_claims" }, strings = "request_object")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("primary_entity_statement_jwt", "claims.metadata.openid_provider.pushed_authorization_request_endpoint");
		if (endpointUri == null) {
			throw error("Missing pushed_authorization_request_endpoint in openid_provider metadata",
				args("metadata", env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata.openid_provider")));
		}

		final String requestObject = env.getString("request_object");

		final JsonObject authorizationRequestForm = new JsonObject();
		authorizationRequestForm.addProperty("request", requestObject);
		env.putObject("authorization_request_form", authorizationRequestForm);

		final String endpointName = "authorization endpoint";
		final String envResponseKey = "authorization_endpoint_response";

		return callEndpointWithPost(env, new IgnoreErrorsErrorHandler(), "authorization_request_form", null, endpointUri, endpointName, envResponseKey);
	}
}
