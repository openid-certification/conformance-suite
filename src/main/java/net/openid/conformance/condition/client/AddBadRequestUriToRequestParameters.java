package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

//PAR-2.1: The request_uri authorization request parameter MUST NOT be provided in this case
public class AddBadRequestUriToRequestParameters extends AbstractCondition {
	private static final String BAD_REQUEST_URI = "urn:fdc:authlete.com:E2ooXxELkEFSKR90ymYV-BbwAvCC2TozHfSb_mMCw2s";
	public static final String REQUEST_URI_KEY = "request_uri";

	@Override
	@PreEnvironment(required = { "pushed_authorization_request_form_parameters"})
	public Environment evaluate(Environment env) {
		JsonObject requestParameters = env.getObject("pushed_authorization_request_form_parameters");
		requestParameters.addProperty(REQUEST_URI_KEY, BAD_REQUEST_URI);
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		return env;
	}
}
