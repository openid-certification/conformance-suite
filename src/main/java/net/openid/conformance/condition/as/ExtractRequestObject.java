package net.openid.conformance.condition.as;

import java.text.ParseException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		String requestObjectString = env.getString("authorization_endpoint_request", "query_string_params.request");

		if (Strings.isNullOrEmpty(requestObjectString)) {
			throw error("Could not find request object in request parameters");
		}

		try {
			JWT jwt = JWTParser.parse(requestObjectString);

			JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
			JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();

			JsonObject o = new JsonObject();
			o.addProperty("value", requestObjectString); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);

			env.putObject("authorization_request_object", o);

			logSuccess("Parsed request object", args("request_object", o));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse request object", e, args("request", requestObjectString));
		}


	}

}
