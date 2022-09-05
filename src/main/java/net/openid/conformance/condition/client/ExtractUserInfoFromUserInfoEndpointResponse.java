package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractUserInfoFromUserInfoEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo_endpoint_response_full")
	@PostEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		env.removeObject("userinfo");

		String userInfoStr = env.getString("userinfo_endpoint_response_full", "body");
		try {
			JsonElement elt = JsonParser.parseString(userInfoStr);
			JsonObject userInfo = elt.getAsJsonObject();
			env.putObject("userinfo", userInfo);
			logSuccess("Extracted user info", args("userinfo", userInfo));
			return env;
		} catch (JsonParseException e) {
			throw error("UserInfo endpoint response is not JSON", e);
		} catch (IllegalStateException e) {
			throw error("UserInfo endpoint response is not a JSON object", e);
		}
	}

}
