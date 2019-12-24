package net.openid.conformance.condition.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateUserInfoStandardClaims extends AbstractCondition {

	interface ElementValidator {
		String getDescription();
		boolean isValid(JsonElement elt);
	}

	private static ElementValidator VALIDATE_STRING = new ElementValidator() {
		public String getDescription() { return "a string"; }
		public boolean isValid(JsonElement elt) { return elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isString(); }
	};

	private static ElementValidator VALIDATE_BOOLEAN = new ElementValidator() {
		public String getDescription() { return "a boolean"; }
		public boolean isValid(JsonElement elt) { return elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isBoolean(); }
	};

	private static ElementValidator VALIDATE_NUMBER = new ElementValidator() {
		public String getDescription() { return "a number"; }
		public boolean isValid(JsonElement elt) { return elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isNumber(); }
	};

	private static ElementValidator VALIDATE_JSON_OBJECT = new ElementValidator() {
		public String getDescription() { return "a JSON object"; }
		public boolean isValid(JsonElement elt) { return elt.isJsonObject(); }
	};

	@SuppressWarnings("serial")
	private final Map<String, ElementValidator> STANDARD_CLAIMS = new HashMap<>() {{
		put("sub", VALIDATE_STRING);
		put("name", VALIDATE_STRING);
		put("given_name", VALIDATE_STRING);
		put("family_name", VALIDATE_STRING);
		put("middle_name", VALIDATE_STRING);
		put("nickname", VALIDATE_STRING);
		put("preferred_username", VALIDATE_STRING);
		put("profile", VALIDATE_STRING);
		put("picture", VALIDATE_STRING);
		put("website", VALIDATE_STRING);
		put("email", VALIDATE_STRING);
		put("email_verified", VALIDATE_BOOLEAN);
		put("gender", VALIDATE_STRING);
		put("birthdate", VALIDATE_STRING);
		put("zoneinfo", VALIDATE_STRING);
		put("locale", VALIDATE_STRING);
		put("phone_number", VALIDATE_STRING);
		put("phone_number_verified", VALIDATE_BOOLEAN);
		put("address", new ObjectValidator("address", ADDRESS_CLAIMS));
		put("updated_at", VALIDATE_NUMBER);
		put("_claim_names", VALIDATE_JSON_OBJECT);
		put("_claim_sources", VALIDATE_JSON_OBJECT);
	}};

	@SuppressWarnings("serial")
	private static final Map<String, ElementValidator> ADDRESS_CLAIMS = new HashMap<>() {{
		put("formatted", VALIDATE_STRING);
		put("street_address", VALIDATE_STRING);
		put("locality", VALIDATE_STRING);
		put("region", VALIDATE_STRING);
		put("postal_code", VALIDATE_STRING);
		put("country", VALIDATE_STRING);
	}};

	private class ObjectValidator implements ElementValidator {
		private final String context;
		private final Map<String, ElementValidator> claims;

		public ObjectValidator(String context, Map<String, ElementValidator> claims) {
			this.context = context;
			this.claims = claims;
		}

		public String getDescription() {
			return "a valid object";
		}

		public boolean isValid(JsonElement elt) {
			if (!elt.isJsonObject()) {
				return false;
			}

			boolean ok = true;

			for (Map.Entry<String, JsonElement> entry : elt.getAsJsonObject().entrySet()) {
				String name = context != null ? context + "." + entry.getKey() : entry.getKey();
				ElementValidator validator = claims.get(entry.getKey());
				if (validator == null) {
					log("Skipping unknown claim: " + name);
					continue;
				}

				if (validator.isValid(entry.getValue())) {
					log(name + " is " + validator.getDescription());
				} else {
					logFailure(name + " is not " + validator.getDescription());
					ok = false;
				}
			}

			return ok;
		}
	}

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		JsonObject userInfo = env.getObject("userinfo");

		if (new ObjectValidator(null, STANDARD_CLAIMS).isValid(userInfo)) {
			logSuccess("Userinfo is valid");
		} else {
			throw error("Userinfo is not valid");
		}

		return env;
	}

}
