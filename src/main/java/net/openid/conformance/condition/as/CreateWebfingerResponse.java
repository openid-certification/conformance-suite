package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateWebfingerResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_webfinger_request"}, strings = {"incoming_webfinger_resource"})
	@PostEnvironment(required = "webfinger_response")
	public Environment evaluate(Environment env) {

		JsonObject response = new JsonObject();
		response.addProperty("subject", env.getString("incoming_webfinger_resource"));
		JsonArray linksArray = new JsonArray();
		JsonObject linkEntry = new JsonObject();
		linkEntry.addProperty("rel", "http://openid.net/specs/connect/1.0/issuer");
		linkEntry.addProperty("href", env.getString("issuer"));
		linksArray.add(linkEntry);
		response.add("links", linksArray);

		env.putObject("webfinger_response", response);

		log("Created webfinger response", args("webfinger_response", response));

		return env;

	}

}
