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



	public static class EnsureRequestObjectContainValidRAR extends AbstractCondition {

		@Override
		@PreEnvironment(required = { "authorization_request_object", "config" })
		public Environment evaluate(Environment env) {

			JsonElement claimsElement = env.getElementFromObject("authorization_request_object", "claims");

			JsonObject claims = claimsElement.getAsJsonObject();
			JsonElement authorizationDetailsObject = claims.get("authorization_details");
			if (authorizationDetailsObject == null || !authorizationDetailsObject.isJsonArray()) {
				logFailure("Request Object does not specify an authorization_details claims", claims);
				return env;
			}

			JsonElement rarTypeValues = env.getElementFromObject("config", "resource.authorization_details_types_supported");
			JsonArray supportedTypes = new JsonArray();
			if (rarTypeValues.isJsonArray()) {
				supportedTypes = rarTypeValues.getAsJsonArray();
			} else {
				supportedTypes.add(rarTypeValues);
			}

			JsonArray auth_details = authorizationDetailsObject.getAsJsonArray();
			for (JsonElement element : auth_details) {
				if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) {
					logFailure("The authorization_details claims has entries missing `type` attribute", element.getAsJsonObject());
					return env;
				}
				boolean supported = false;
				String elementType = element.getAsJsonObject().get("type").getAsString();
				for(JsonElement supportedType : supportedTypes) {
					if (supportedType.getAsString().equals(elementType)) {
						supported = true;
					}
				}
				if (!supported) {
					logFailure("The authorization_details claims has entries with unsupported `type`", element.getAsJsonObject());
				}
			}


			logSuccess("Rich request found and every type is supported");
			return env;

		}

	}


	public static class AddRARToAuthorizationEndpointRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = {"authorization_endpoint_request","rar"})
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {

			JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

			JsonObject rar = env.getObject("rar");

			JsonArray payload = rar.getAsJsonArray("payload");

			authorizationEndpointRequest.add("authorization_details", payload);

			logSuccess("Added rich request parameter to request", authorizationEndpointRequest);

			return env;

		}

	}
}
