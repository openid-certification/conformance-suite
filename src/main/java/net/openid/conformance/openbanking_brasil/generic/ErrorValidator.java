package net.openid.conformance.openbanking_brasil.generic;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.StringField;

public class ErrorValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		if(environment.getString("resource_endpoint_response").equals("{}")) {
			log("debug: " + environment.getInteger("resource_endpoint_response_status"));
		}
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.errors");
		assertOuterFields(body);
		assertInnerFields(body);

		return environment;
	}

	private void assertOuterFields(JsonObject body) {
		try {
			JsonObject errors = findByPath(body, "$").getAsJsonObject();
			assertField(errors, new ArrayField
				.Builder("errors")
				.setMinItems(1)
				.setMaxItems(13)
				.build()
			);
		} catch (IllegalStateException e){
			throw error("Errors field is not a Json Array. This is not spec compliant.");
		}
	}
	private void assertInnerFields(JsonObject body) {
		JsonArray errors = findByPath(body, "$.errors").getAsJsonArray();
		errors.forEach(error -> {
			assertField(error.getAsJsonObject(),
				new StringField
					.Builder("code")
					.setPattern("[\\w\\W\\s]*")
					.setMaxLength(255)
					.build());

			assertField(error.getAsJsonObject(),
				new StringField
					.Builder("title")
					.setPattern("[\\w\\W\\s]*")
					.setMaxLength(255)
					.build());

			assertField(error.getAsJsonObject(),
				new StringField
					.Builder("detail")
					.setPattern("[\\w\\W\\s]*")
					.setMaxLength(2048)
					.build());
		});

	}
}
