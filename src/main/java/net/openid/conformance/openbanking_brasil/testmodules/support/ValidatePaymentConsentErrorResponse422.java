package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

public class ValidatePaymentConsentErrorResponse422 extends AbstractJsonAssertingCondition {

	private static final Set<String> errorCodes = Sets.newHashSet(
		"FORMA_PGTO_INVALIDA", "DATA_PGTO_INVALIDA", "DETALHE_PGTO_INVALIDO", "NAO_INFORMADO"
	);


	@Override
	public Environment evaluate(Environment env) {

		JsonObject apiResponse = bodyFrom(env);
		if (!JsonHelper.ifExists(apiResponse, "$.data")) {
			apiResponse = env.getObject("consent_endpoint_response_full");
		}

		JsonObject decodedJwt;
		try {
			decodedJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(apiResponse.get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", apiResponse);
		}
		JsonObject claims = decodedJwt.getAsJsonObject("claims");

		log("Decoded Jwt: ", decodedJwt);
		assertOuterFields(claims);
		assertErrorFields(claims);
		if(JsonHelper.ifExists(claims, "meta")){
			assertMetaFields(claims);
			assertRequestDateTime(claims);
		}

		return env;
	}

	private void assertOuterFields(JsonObject body) {
		assertField(body,
			new ObjectArrayField
				.Builder("errors")
				.setMinItems(1)
				.setMaxItems(13)
				.build());
	}

	private void assertErrorFields(JsonObject body) {
		JsonArray errors = body.getAsJsonArray("errors");

		for(JsonElement error: errors){
			assertError(error.getAsJsonObject());
		}
	}

	private void assertError(JsonObject error){
		assertField(error, new StringField
			.Builder("code")
			.setMaxLength(21)
			.setEnums(errorCodes)
			.build()
		);
		assertField(error, new StringField
			.Builder("detail")
			.setMaxLength(2048)
			.build()
		);
		assertField(error, new StringField
			.Builder("title")
			.setMaxLength(255)
			.build()
		);
	}

	private void assertMetaFields(JsonObject body){
		JsonObject meta = body.getAsJsonObject("meta");
		assertField(meta, new StringField
			.Builder("requestDateTime")
			.build()
		);
		assertField(meta,
			new IntField
				.Builder("totalRecords")
				.setMinValue(0)
				.build());
		assertField(meta,
			new IntField
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
