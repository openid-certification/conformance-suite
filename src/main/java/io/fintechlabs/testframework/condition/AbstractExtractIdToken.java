package io.fintechlabs.testframework.condition;

import java.text.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractExtractIdToken extends AbstractCondition {

	protected Environment extractIdToken(Environment env, String key) {

		JsonElement idTokenElement = env.getElementFromObject(key, "id_token");
		if (idTokenElement == null || !idTokenElement.isJsonPrimitive()) {
			throw error("Couldn't find an ID Token in response");
		}

		String idTokenString = env.getString(key, "id_token");

		try {
			JWT idToken = JWTParser.parse(idTokenString);

			// Note: we need to round-trip this to get to GSON objects because the JWT library uses a different parser
			JsonObject header = new JsonParser().parse(idToken.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
			JsonObject claims = new JsonParser().parse(idToken.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();

			JsonObject o = new JsonObject();
			o.addProperty("value", idTokenString); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);

			// save the parsed ID token
			env.putObject("id_token", o);

			logSuccess("Found and parsed the ID Token", o);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse JWT", e, args("id_token_string", idTokenString));
		}

	}

}
