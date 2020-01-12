package net.openid.conformance.condition;

import java.text.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

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
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(tokenString);

			// save the parsed token
			env.putObject(dstPath, jwtAsJsonObject);

			logSuccess("Found and parsed the "+dstPath+" from "+key, jwtAsJsonObject);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse "+dstPath+" from "+key+" as a JWT", e, args(dstPath, tokenString));
		}

	}

}
