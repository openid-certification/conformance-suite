package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithGet;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.MediaType;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

public class CallAuthorizationEndpointAndReturnFullResponse extends AbstractCallEndpointWithGet {

	protected List<MediaType> getAcceptHeader() {
		return Collections.emptyList();
	}

	@Override
	@PreEnvironment(required = { "primary_entity_statement_jwt", "request_object_claims" }, strings = "request_object")
    @PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("primary_entity_statement_jwt", "claims.metadata.openid_provider.authorization_endpoint");
		final JsonObject requestObjectClaims = env.getObject("request_object_claims");
		final String requestObject = env.getString("request_object");
		final String authorizationEndpointUrl;
		try {
			URIBuilder uriBuilder = new URIBuilder(endpointUri);
			uriBuilder.addParameter("client_id", OIDFJSON.getString(requestObjectClaims.get("client_id")));
			uriBuilder.addParameter("scope", OIDFJSON.getString(requestObjectClaims.get("scope")));
			uriBuilder.addParameter("response_type", OIDFJSON.getString(requestObjectClaims.get("response_type")));
			uriBuilder.addParameter("request", requestObject);
			authorizationEndpointUrl = uriBuilder.build().toString();
		} catch (URISyntaxException e) {
			throw error("Invalid authorization endpoint URI", e);
		}
		final String endpointName = "authorization endpoint";
		final String envResponseKey = "authorization_endpoint_response";

		return callEndpointWithGet(env, new IgnoreErrorsErrorHandler(), getAcceptHeader(), authorizationEndpointUrl, endpointName, envResponseKey);
	}
}
