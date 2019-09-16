package io.fintechlabs.testframework.condition;

import java.text.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public abstract class AbstractExtractJWT extends AbstractCondition {

	protected Environment extractJWT(Environment env, String key, String path, String dstPath) {

		// Remove any old token
		env.removeObject(dstPath);

		JsonElement tokenElement = env.getElementFromObject(key, path);
		if (tokenElement == null || !tokenElement.isJsonPrimitive()) {
			throw error("Couldn't find "+path+" in "+key);
		}

		String tokenString = OIDFJSON.getString(tokenElement);

		try {
			JWT token = JWTParser.parse(tokenString);

			// Note: we need to round-trip this to get to GSON objects because the JWT library uses a different parser
			JsonObject header = new JsonParser().parse(token.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
			JsonObject claims = new JsonParser().parse(token.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();

			JsonObject o = new JsonObject();
			o.addProperty("value", tokenString); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);

			// save the parsed token
			env.putObject(dstPath, o);

			logSuccess("Found and parsed the "+dstPath+" from "+key, o);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse "+dstPath+" from "+key+" as a JWT", e, args(dstPath, tokenString));
		}

	}

}
