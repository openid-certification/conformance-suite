package net.openid.conformance.openbanking_brasil.generic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Base64;

public class ErrorValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		boolean jwt = false;
		String contentTypeStr = environment.getString("resource_endpoint_response_headers", "content-type");
		// just for tests
		if(contentTypeStr != null) {
			jwt = contentTypeStr.contains("application/jwt");
		}
		JsonObject body;

		body = jwt ? getBodyFromJwt(environment) : getBodyFromJson(environment);

		assertHasField(body, "$.errors");
		assertOuterFields(body);
		assertInnerFields(body);

		return environment;
	}

	private JsonObject getBodyFromJwt(Environment environment) {
		String response = environment.getString("resource_endpoint_response");
		Base64.Decoder decoder = Base64.getUrlDecoder();
		String body = new String(decoder.decode(response.split("\\.")[1]));
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		return gson.fromJson(body, JsonObject.class);
	}

	private JsonObject getBodyFromJson(Environment environment) {
		if(environment.getString("resource_endpoint_response").equals("{}")) {
			// for specific permission sets
			JsonObject response = environment.getObject("errored_response");
			environment.putInteger(
				"resource_endpoint_response_status",
				OIDFJSON.getInt(response.get("status_code"))
			);
			// for debugging
			log("Check status stored is same as response: " + environment.getInteger("resource_endpoint_response_status").toString());
			return response;
		} else {
			return bodyFrom(environment).getAsJsonObject();
		}
	}

	private void assertOuterFields(JsonObject body) {
		try {
			JsonObject errors = findByPath(body, "$").getAsJsonObject();
			assertField(errors,
				new ObjectArrayField
					.Builder("errors")
					.setValidator(error -> {
						new StringField.
							Builder("code")
							.setOptional()
							.build();

						new StringField.
							Builder("title")
							.setOptional()
							.build();

						new StringField.
							Builder("detail")
							.setOptional()
							.build();

					})
					.setMinItems(1)
					.setMaxItems(13)
					.build());
		} catch (IllegalStateException e){
			throw error("Errors field is not a Json Array. This is not spec compliant.");
		}
	}
	private void assertInnerFields(JsonObject body) {
		JsonArray errors = findByPath(body, "$.errors").getAsJsonArray();
		final String PATTERN = "[\\w\\W\\s]*";
		errors.forEach(error -> {
			assertField(error.getAsJsonObject(),
				new StringField
					.Builder("code")
					.setPattern(PATTERN)
					.setMaxLength(255)
					.build());

			assertField(error.getAsJsonObject(),
				new StringField
					.Builder("title")
					.setPattern(PATTERN)
					.setMaxLength(255)
					.build());

			assertField(error.getAsJsonObject(),
				new StringField
					.Builder("detail")
					.setPattern(PATTERN)
					.setMaxLength(2048)
					.build());
		});

	}
}
