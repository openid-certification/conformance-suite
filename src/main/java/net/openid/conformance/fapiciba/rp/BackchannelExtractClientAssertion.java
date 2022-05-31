package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class BackchannelExtractClientAssertion extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	@PostEnvironment(required = "client_assertion")
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("backchannel_endpoint_http_request", "body_form_params.client_assertion");

		if (Strings.isNullOrEmpty(clientAssertionString)) {
			throw error("Could not find client assertion in request parameters");
		}

		try {
			JsonObject jsonObjectForJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(clientAssertionString);

			env.putObject("client_assertion", jsonObjectForJwt);

			logSuccess("Parsed client assertion", args("client_assertion", jsonObjectForJwt));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse client assertion", e, args("client assertion", clientAssertionString));
		}
	}
}
