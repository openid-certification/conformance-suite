package net.openid.conformance.openbanking_brasil.generic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.JsonHelper;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Locale;

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

		if(JsonHelper.ifExists(body, "meta")){
			assertMetaFields(body);
			assertRequestDateTime(body);
		}

		return environment;
	}

	private JsonObject getBodyFromJwt(Environment environment) {
		String response = environment.getString("consent_endpoint_response");

		if(response == null){
			response = environment.getString("consent_endpoint_response_full", "body");
		}
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
			assertField(errors, new ObjectArrayField
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
	private void assertMetaFields(JsonObject body){
		JsonObject meta = body.getAsJsonObject("meta");
		assertField(meta, new StringField
			.Builder("requestDateTime")
			.build()
		);
		assertField(meta, new IntField
			.Builder("totalRecords")
			.setMinValue(0)
			.build()
		);
		assertField(meta, new IntField
			.Builder("totalPages")
			.setMinValue(1)
			.build()
		);
	}

	private void assertRequestDateTime(JsonObject claims){
		String requestDateTime = OIDFJSON.getString(claims.getAsJsonObject("meta").get("requestDateTime"));

		// Check that we have a Timezone element to this datetime object and that it is not longer than 20 chars
		if (requestDateTime.length() > 20) {
			throw error("requestDateTime is more than 20 characters in length.");
		}

		// Parse the dateTime as RFC3339 and check that we have the 'Z'
		try {
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(requestDateTime);
		} catch (ParseException e) {
			throw error("requestDateTime is not in valid RFC 3339 format.");
		}
	}
}
