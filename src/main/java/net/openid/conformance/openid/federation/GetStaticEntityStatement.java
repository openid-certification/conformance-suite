package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;

public class GetStaticEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "federation_response_body", } )
	public Environment evaluate(Environment env) {

		JsonElement entityConfiguration = env.getElementFromObject("config", "federation.entity_configuration");
		if (entityConfiguration == null) {
			throw error("Couldn't find entity configuration in test configuration");
		}

		if (!entityConfiguration.isJsonPrimitive() || !entityConfiguration.getAsJsonPrimitive().isString()) {
			throw error("Entity configuration is not a string");
		}

		try {
			String jwtString = OIDFJSON.getString(entityConfiguration);
			SignedJWT jwt = SignedJWT.parse(jwtString);
			JsonObject entityStatementBody = JsonParser.parseString(jwt.getJWTClaimsSet().toString()).getAsJsonObject();
			JsonObject entityStatementHeader = JsonParser.parseString(jwt.getHeader().toString()).getAsJsonObject();
			logSuccess("Successfully parsed signed JWT", entityStatementBody);
			env.putString("federation_response", jwtString);
			env.putObject("federation_response_body", entityStatementBody);
			env.putObject("federation_response_header", entityStatementHeader);
			return env;
		} catch (ParseException e) {
			throw error("Failed to parse entity statement as a signed JWT", e, args("jwt", entityConfiguration));
		} catch (JsonSyntaxException e) {
			throw error(e, args("json", entityConfiguration));
		}
	}

}
