package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.StringField;

public class EnsureResponseHasLinks extends AbstractJsonAssertingCondition {

    @Override
	public Environment evaluate(Environment environment) {
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		JsonObject body;
    	try {
			String resource = environment.getString("resource_endpoint_response");
			body = gson.fromJson(resource, JsonElement.class).getAsJsonObject();
		} catch(JsonSyntaxException e) {
    		body = environment.getObject("resource_endpoint_response");
		}

		if (!JsonHelper.ifExists(body, "$.data")) {
			body = environment.getObject("consent_endpoint_response");
		}

		assertHasField(body, ROOT_PATH);

        log("Check for navigation Links in the response body.");

		if (JsonHelper.ifExists(body, "$.links")) {

			log("Ensure that there is a link to self.");
			assertHasField(body, "$.links.self");

			log("Ensure that all links are using HTTPS.");
			assertField(body, new StringField.Builder("$.links.self").setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
			assertField(body, new StringField.Builder("$.links.first").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
			assertField(body, new StringField.Builder("$.links.prev").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
			assertField(body, new StringField.Builder("$.links.next").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
			assertField(body, new StringField.Builder("$.links.last").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		}

        return environment;
	}
}
