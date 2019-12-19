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

public class ExtractAssertionFromIntrospectionRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "introspection_request")
	@PostEnvironment(required = "resource_assertion")
	public Environment evaluate(Environment env) {

		String assertion = env.getString("introspection_request", "query_string_params.client_assertion");
		String assertionType = env.getString("introspection_request", "query_string_params.client_assertion_type");

		if (Strings.isNullOrEmpty(assertion) || Strings.isNullOrEmpty(assertionType)) {
			throw error("Couldn't find assertion or assertion type in request");
		}

		try {
			JWT parsed = JWTParser.parse(assertion);

			JsonParser parser = new JsonParser();

			JsonObject o = new JsonObject();

			o.addProperty("assertion", assertion);
			o.addProperty("assertion_type", assertionType);
			o.add("assertion_header", parser.parse(parsed.getHeader().toString()));
			o.add("assertion_payload", parser.parse(parsed.getJWTClaimsSet().toString()));

			env.putObject("resource_assertion", o);

			logSuccess("Extracted assertion from resource server", o);

			return env;
		} catch (ParseException e) {
			throw error("Couldn't parse client assertion", e);
		}

	}

}
