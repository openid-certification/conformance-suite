package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCLoadUserInfo extends AbstractCondition {

	@Override
	@PostEnvironment(required = "user_info")
	public Environment evaluate(Environment env) {

		JsonObject user = new JsonObject();

		user.addProperty("sub", "user-subject-1234531");

		user.addProperty("name", "Demo T. User");
		user.addProperty("given_name", "Demo");
		user.addProperty("family_name", "User");
		user.addProperty("middle_name", "Theresa");
		user.addProperty("nickname", "Dee");
		user.addProperty("preferred_username", "d.tu");
		user.addProperty("gender", "female");
		user.addProperty("birthdate", "2000-02-03");

		JsonObject address = new JsonObject();
		address.addProperty("street_address", "100 Universal City Plaza");
		address.addProperty("locality", "Hollywood");
		address.addProperty("region", "CA");
		address.addProperty("postal_code", "91608");
		address.addProperty("country", "USA");
		user.add("address", address);

		user.addProperty("zoneinfo", "America/Los_Angeles");
		user.addProperty("locale", "en-US");

		user.addProperty("phone_number", "+1 555 5550000");
		user.addProperty("phone_number_verified", false);
		user.addProperty("email", "user@example.com");
		user.addProperty("email_verified", false);
		user.addProperty("website", "https://openid.net/");
		user.addProperty("updated_at", "1580000000");

		//TODO add a picture?
		//user.addProperty("picture", );

		env.putObject("user_info", user);

		logSuccess("Added user information", args("user_info", user));

		return env;
	}

}
