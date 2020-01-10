package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractClientAssertion extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_assertion")
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("token_endpoint_request", "body_form_params.client_assertion");

		if (Strings.isNullOrEmpty(clientAssertionString)) {
			throw error("Could not find client assertion in request parameters");
		}

		try {
			JWT jwt = JWTUtil.parseJWT(clientAssertionString);

			JsonObject header = JWTUtil.jwtHeaderAsJsonObject(jwt);
			JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(jwt, false);

			JsonObject o = new JsonObject();
			o.addProperty("value", clientAssertionString); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);

			env.putObject("client_assertion", o);

			logSuccess("Parsed client assertion", args("client_assertion", o));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse client assertion", e, args("client assertion", clientAssertionString));
		}
	}
}
