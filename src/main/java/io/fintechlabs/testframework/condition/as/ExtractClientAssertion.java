package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.text.ParseException;

public class ExtractClientAssertion extends AbstractCondition {

	public ExtractClientAssertion(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_assertion")
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("token_endpoint_request", "params.client_assertion");

		if (Strings.isNullOrEmpty(clientAssertionString)) {
			throw error("Could not find client assertion in request parameters");
		}

		try {
			JWT jwt = JWTParser.parse(clientAssertionString);

			JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
			JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();

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
