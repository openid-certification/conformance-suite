package net.openid.conformance.openbanking_brasil.generic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Base64;

public class PaymentConsentsErrorValidator extends AbstractJsonAssertingCondition {

	@Override
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
		String response = environment.getString("consent_endpoint_response");
		Base64.Decoder decoder = Base64.getUrlDecoder();
		String body = new String(decoder.decode(response.split("\\.")[1]));
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		return gson.fromJson(body, JsonObject.class);
	}

	private JsonObject getBodyFromJson(Environment environment) {
		String body = "";
		try {
			body = OIDFJSON.getString(environment.getObject("consent_endpoint_response_full").get("body"));
		} catch(OIDFJSON.UnexpectedJsonTypeException exception){
			try {
				return environment.getObject("consent_endpoint_response_full").getAsJsonObject("body");
			} catch(Exception e){
				logFailure("Failed to parse response body, got exception: " + e.getMessage());
			}
		}
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		return gson.fromJson(body, JsonObject.class);
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
