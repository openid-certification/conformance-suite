package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractValidateOpenIdStandardClaims extends AbstractCondition {
	private static ElementValidator VALIDATE_STRING = new ElementValidator() {
		@Override
		public String getDescription() {
			return "a string with content";
		}

		@Override
		public boolean isValid(JsonElement elt) {
			// If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object representing the Claims; it SHOULD NOT be present with a null or empty string value.
			if (!elt.isJsonPrimitive() || !elt.getAsJsonPrimitive().isString()) {
				return false;
			}
			if (OIDFJSON.getString(elt).isBlank()) {
				return false;
			}
			// Not explicitly stated in any spec, but we've seen servers return this incorrectly as a user's name
			if (OIDFJSON.getString(elt).equalsIgnoreCase("null")) {
				return false;
			}
			return true;
		}
	};
	private static ElementValidator VALIDATE_BIRTHDATE = new ElementValidator() {
		@Override
		public String getDescription() {
			return "a valid birthdate in the format stated in OpenID Connect Standard - YYYY-MM-DD, 0000-MM-DD or YYYY";
		}

		@Override
		public boolean isValid(JsonElement elt) {
			if (!VALIDATE_STRING.isValid(elt)) {
				return false;
			}
			return isValidBirthDate(OIDFJSON.getString(elt));
		}

		public boolean isValidBirthDate(String date) {
			return isValidFullDate(date) || isValidYearOnly(date);
		}

		private boolean isValidFullDate(String date) {
			// US used as per https://developer.android.com/reference/java/util/Locale.html#be-wary-of-the-default-locale
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US);
			try {
				LocalDate parsedDate = LocalDate.parse(date, dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT));
				if (parsedDate.getYear() == 0) {
					// as per OIDCC, the year can optionally be 0000 to indicate year not held/not released
					return true;
				}
				int year = parsedDate.getYear();
				if (!isSaneBirthYear(year)) {
					return false;
				}

				return true;

			} catch (DateTimeParseException e) {
				return false;
			}
		}

		// true if seems like a real date of birth, or at least a fake that results in a non-negative non-excessive age.
		private boolean isSaneBirthYear(int year) {
			return year >= 1850 && year <= Calendar.getInstance().get(Calendar.YEAR);
		}

		private boolean isValidYearOnly(String yearStr) {
			try {
				int year = Integer.parseInt(yearStr);
				return isSaneBirthYear(year);
			} catch (NumberFormatException e) {
				return false;
			}
		}
	};
	private static ElementValidator VALIDATE_BOOLEAN = new ElementValidator() {
		@Override
		public String getDescription() {
			return "a boolean";
		}

		@Override
		public boolean isValid(JsonElement elt) {
			return elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isBoolean();
		}
	};
	private static ElementValidator VALIDATE_NUMBER = new ElementValidator() {
		@Override
		public String getDescription() {
			return "a number";
		}

		@Override
		public boolean isValid(JsonElement elt) {
			return elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isNumber();
		}
	};
	private static ElementValidator VALIDATE_JSON_OBJECT = new ElementValidator() {
		@Override
		public String getDescription() {
			return "a JSON object";
		}

		@Override
		public boolean isValid(JsonElement elt) {
			return elt.isJsonObject();
		}
	};
	private static final Map<String, ElementValidator> ADDRESS_CLAIMS = new HashMap<>() {{
		put("formatted", VALIDATE_STRING);
		put("street_address", VALIDATE_STRING);
		put("locality", VALIDATE_STRING);
		put("region", VALIDATE_STRING);
		put("postal_code", VALIDATE_STRING);
		put("country", VALIDATE_STRING);
	}};
	@SuppressWarnings("serial")
	protected final Map<String, ElementValidator> STANDARD_CLAIMS = new HashMap<>() {{
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
		put("birthdate", VALIDATE_BIRTHDATE);
		put("zoneinfo", VALIDATE_STRING);
		put("locale", VALIDATE_STRING);
		put("phone_number", VALIDATE_STRING);
		put("phone_number_verified", VALIDATE_BOOLEAN);
		put("address", new ObjectValidator("address", ADDRESS_CLAIMS));
		put("updated_at", VALIDATE_NUMBER);
		put("_claim_names", VALIDATE_JSON_OBJECT);
		put("_claim_sources", VALIDATE_JSON_OBJECT);
		// digitalid-financial-api-04.md
		put("txn", VALIDATE_STRING);
	}};

	protected JsonObject unknownClaims = new JsonObject();

	interface ElementValidator {
		String getDescription();

		boolean isValid(JsonElement elt);
	}

	protected class ObjectValidator implements ElementValidator {
		private final String context;
		private final Map<String, ElementValidator> claims;

		public ObjectValidator(String context, Map<String, ElementValidator> claims) {
			this.context = context;
			this.claims = claims;
		}

		@Override
		public String getDescription() {
			return "a valid object or contains invalid claims";
		}

		@Override
		public boolean isValid(JsonElement elt) {
			if (!elt.isJsonObject() || elt.getAsJsonObject().size() == 0) {
				logFailure("Not a JSON object or no identity claims");
				return false;
			}

			boolean ok = true;

			for (Map.Entry<String, JsonElement> entry : elt.getAsJsonObject().entrySet()) {
				String name = context != null ? context + "." + entry.getKey() : entry.getKey();
				ElementValidator validator = claims.get(entry.getKey());
				if (validator == null) {
					log("Skipping unknown claim: " + name);
					unknownClaims.add(name, entry.getValue());
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
}
