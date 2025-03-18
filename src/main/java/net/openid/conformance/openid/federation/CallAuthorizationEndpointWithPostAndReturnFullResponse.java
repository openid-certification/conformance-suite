package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithPost;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CallAuthorizationEndpointWithPostAndReturnFullResponse extends AbstractCallEndpointWithPost {

	@Override
	@PreEnvironment(required = { "primary_entity_statement_jwt", "request_object_claims" }, strings = "request_object")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("primary_entity_statement_jwt", "claims.metadata.openid_provider.authorization_endpoint");

		final JsonObject requestObjectClaims = env.getObject("request_object_claims");
		final String requestObject = env.getString("request_object");

		final JsonObject authorizationRequestForm = new JsonObject();
		authorizationRequestForm.addProperty("client_id", OIDFJSON.getString(requestObjectClaims.get("client_id")));
		authorizationRequestForm.addProperty("scope", OIDFJSON.getString(requestObjectClaims.get("scope")));
		authorizationRequestForm.addProperty("response_type", OIDFJSON.getString(requestObjectClaims.get("response_type")));
		authorizationRequestForm.addProperty("request", requestObject);
		env.putObject("authorization_request_form", authorizationRequestForm);

		final String endpointName = "authorization endpoint";
		final String envResponseKey = "authorization_endpoint_response";

		return callEndpointWithPost(env, new IgnoreErrorsErrorHandler(), "authorization_request_form", null, endpointUri, endpointName, envResponseKey);
	}
}
