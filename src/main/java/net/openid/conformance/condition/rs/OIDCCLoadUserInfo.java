package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCLoadUserInfo extends AbstractCondition {

	@SuppressWarnings("MutablePublicArray")
	public static final String[] SUPPORTED_CLAIMS =
		{
			"sub",
			"name",
			"given_name",
			"family_name",
			"middle_name",
			"nickname",
			"preferred_username",
			"gender",
			"birthdate",
			"address",
			"zoneinfo",
			"locale",
			"phone_number",
			"phone_number_verified",
			"email",
			"email_verified",
			"website",
			"profile",
			"updated_at",
			"txn",
			// TODO add a picture?
			// "picture"
		};

	public static JsonObject getUserInfoClaimsValues(String ... claimsList) {
		JsonObject user = new JsonObject();

		for(String claim : claimsList) {
			switch (claim) {
				case "sub":
					user.addProperty("sub", "user-subject-1234531");
					break;

				case "name":
					user.addProperty("name", "Demo T. User");
					break;

				case "given_name":
					user.addProperty("given_name", "Demo");
					break;

				case "family_name":
					user.addProperty("family_name", "User");
					break;

				case "middle_name":
					user.addProperty("middle_name", "Theresa");
					break;

				case "nickname":
					user.addProperty("nickname", "Dee");
					break;

				case "preferred_username":
					user.addProperty("preferred_username", "d.tu");
					break;

				case "gender":
					user.addProperty("gender", "female");
					break;

				case "birthdate":
					user.addProperty("birthdate", "2000-02-03");
					break;

				case "address":
					JsonObject address = new JsonObject();
					address.addProperty("street_address", "100 Universal City Plaza");
					address.addProperty("locality", "Hollywood");
					address.addProperty("region", "CA");
					address.addProperty("postal_code", "91608");
					address.addProperty("country", "USA");
					user.add("address", address);
					break;

				case "zoneinfo":
					user.addProperty("zoneinfo", "America/Los_Angeles");
					break;

				case "locale":
					user.addProperty("locale", "en-US");
					break;

				case "phone_number":
					user.addProperty("phone_number", "+1 555 5550000");
					break;

				case "phone_number_verified":
					user.addProperty("phone_number_verified", false);
					break;

				case "email":
					user.addProperty("email", "user@example.com");
					break;

				case "email_verified":
					user.addProperty("email_verified", false);
					break;

				case "website":
					user.addProperty("website", "https://openid.net/");
					break;

				case "profile":
					user.addProperty("profile", "https://example.com/user");
					break;

				case "updated_at":
					user.addProperty("updated_at", 1580000000);
					break;

				case "txn":
					user.addProperty("txn", "2c6fb585-d51b-465a-9dca-b8cd22a11451");
					break;

				default:
					break;

				// TODO add a picture?
				// user.addProperty("picture");
			}
		}
		return user;
	}

	public static JsonObject getUserInfoClaimsValues() {
		return getUserInfoClaimsValues(SUPPORTED_CLAIMS);
	}


	@Override
	@PostEnvironment(required = "user_info")
	public Environment evaluate(Environment env) {
		JsonObject user = getUserInfoClaimsValues();

		env.putObject("user_info", user);

		logSuccess("Added user information", args("user_info", user));

		return env;
	}

}
