package net.openid.conformance.condition.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RARSupport {

	public static class ExtractRARFromConfig extends AbstractCondition {

		@Override
		@PreEnvironment(required = "config")
		@PostEnvironment(required = "rar")
		public Environment evaluate(Environment env) {

			JsonElement rarElement = env.getElementFromObject("config", "resource.richAuthorizationRequest");

			if (rarElement == null || rarElement.toString().equals("")) {
				throw error("Couldn't find Authorization Request JSON on test config");
			}

			JsonElement rarObj = JsonParser.parseString(rarElement.toString());

			JsonArray rarArray = new JsonArray();

			if (rarObj.isJsonArray()){
				if (rarObj.getAsJsonArray().size() == 0) {
					throw error("Couldn't find Authorization Request JSON on test config");
				}
				rarArray = rarObj.getAsJsonArray();
			} else {
				rarArray.add(rarObj);
			}

			for (JsonElement element : rarArray) {
				if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) {
					throw error("The Authorization Request JSON on test config is not a valid RAR payload");
				}
			}
			JsonObject rar = new JsonObject();
			rar.add("payload", rarArray);
			env.putObject("rar", rar );
			return env;
		}
	}


}
