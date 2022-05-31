package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnsureBackchannelRequestParametersDoNotAppearOutsideJwt extends EnsureRequiredAuthorizationRequestParametersMatchRequestObject {

	protected Set<String> possibleAuthenticationRequestParameters = ImmutableSet.of(
		"scope", "client_notification_token", "acr_values",
		"login_hint_token", "id_token_hint", "login_hint", "binding_message",
		"user_code", "requested_expiry", "request_context"
	);

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		Map<String, Object> disallowedParametersInHttpRequest = possibleAuthenticationRequestParameters.stream()
			.filter(p -> !Strings.isNullOrEmpty(env.getString("backchannel_endpoint_http_request", "body_form_params." + p)))
			.collect(Collectors.toMap(p -> p, p -> env.getString("backchannel_endpoint_http_request", "body_form_params." + p)));

		if(!disallowedParametersInHttpRequest.isEmpty()) {
			throw error("Authentication request parameters are not allowed in the HTTP request", disallowedParametersInHttpRequest);
		}

		logSuccess("The HTTP request does not contain any of the authentication request parameters");

		return env;
	}
}
