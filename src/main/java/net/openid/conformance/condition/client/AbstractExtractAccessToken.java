package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractExtractAccessToken extends AbstractCondition {

	protected Environment extractAccessToken(Environment env, String source) {

		String accessTokenString = env.getString(source, "access_token");
		if (Strings.isNullOrEmpty(accessTokenString)) {
			throw error("Couldn't find access token in " + source);
		}

		String tokenType = env.getString(source, "token_type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Couldn't find token type in " + source);
		}

		JsonObject o = new JsonObject();
		o.addProperty("value", accessTokenString);
		o.addProperty("type", tokenType);

		env.putObject("access_token", o);

		logSuccess("Extracted the access token", o);

		return env;
	}

}
