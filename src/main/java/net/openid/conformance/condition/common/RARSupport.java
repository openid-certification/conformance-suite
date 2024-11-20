package net.openid.conformance.condition.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TemplateProcessor;

public class RARSupport {


	// OP Test SUPPORT

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

			if (rarObj.isJsonArray()) {
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
			env.putObject("rar", rar);
			return env;
		}
	}


	public static class AddRARToAuthorizationEndpointRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = {"authorization_endpoint_request", "rar"})
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {

			JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

			JsonArray payload = getJsonArrayFromEnvironment(env, "rar", "payload", "Rich Authorization Request on Test config", true);

			String payloadString = payload.toString();

			JsonElement rarObj = JsonParser.parseString(TemplateProcessor.process(payloadString));

			authorizationEndpointRequest.add("authorization_details", rarObj);

			logSuccess("Added rich request parameter to request", authorizationEndpointRequest);

			return env;

		}

	}

	public static class CheckForAuthorizationDetailsInTokenResponse extends AbstractCondition {

		@Override
		@PreEnvironment(required = "token_endpoint_response")
		public Environment evaluate(Environment env) {
			JsonArray permissions = getJsonArrayFromEnvironment(env, "token_endpoint_response", "authorization_details", "authorization_details in token response", true);

			for (JsonElement element : permissions) {
				if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) {
					throw error("The authorization_details claims has entries missing `type` attribute", element.getAsJsonObject());
				}
			}

			logSuccess("Found an authorization_details", args("authorization_details", env.getElementFromObject("token_endpoint_response", "authorization_details")));
			return env;
		}

	}


	// RP Test SUPPORT


	public static class EnsureRequestObjectContainValidRAR extends AbstractCondition {

		@Override
		@PreEnvironment(required = {"authorization_request_object", "config"})
		@PostEnvironment(required = "rich_authorization_request")
		public Environment evaluate(Environment env) {

			JsonElement rarTypeValues = env.getElementFromObject("config", "resource.authorization_details_types_supported");
			JsonArray supportedTypes = new JsonArray();
			if (rarTypeValues.isJsonArray()) {
				supportedTypes = rarTypeValues.getAsJsonArray();
			} else {
				supportedTypes.add(rarTypeValues);
			}

			JsonArray auth_details = getJsonArrayFromEnvironment(env, "authorization_request_object", "claims.authorization_details", "authorization_details claim under request object", true);
			for (JsonElement element : auth_details) {
				if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) {
					throw error("The authorization_details claims has entries missing `type` attribute", element.getAsJsonObject());
				}
				boolean supported = false;
				String elementType = OIDFJSON.getString(element.getAsJsonObject().get("type"));
				for (JsonElement supportedType : supportedTypes) {
					if (OIDFJSON.getString(supportedType).equals(elementType)) {
						supported = true;
					}
				}
				if (!supported) {
					throw error("The authorization_details claims has entries with unsupported `type`", args("bad_entry", element.getAsJsonObject(), "supportedTypes", supportedTypes));
				}
			}

			JsonObject rich_authorization_request = new JsonObject();
			rich_authorization_request.add("rar", auth_details);
			env.putObject("rich_authorization_request", rich_authorization_request);
			logSuccess("Rich request found and every type is supported", rich_authorization_request);
			return env;

		}

	}

	public static class AddRarToTokenEndpointResponse extends AbstractCondition {

		@Override
		@PostEnvironment(required = "token_endpoint_response")
		public Environment evaluate(Environment env) {

			JsonArray auth_details = getJsonArrayFromEnvironment(env, "rich_authorization_request", "rar", "authorization_details from request object", true);

			JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");

			tokenEndpointResponse.add("authorization_details", auth_details);

			logSuccess("RAR payload included on token endpoint response", tokenEndpointResponse);

			return env;

		}

	}


}
