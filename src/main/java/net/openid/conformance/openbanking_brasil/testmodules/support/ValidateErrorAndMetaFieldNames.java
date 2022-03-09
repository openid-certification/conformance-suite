package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.Map;

import net.openid.conformance.util.field.DatetimeField;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;

public class ValidateErrorAndMetaFieldNames extends AbstractJsonAssertingCondition {

	private static final String[] allowedErrors = {"code","title","detail"};
	private static final String[] allowedMetaFields = {"requestDateTime", "totalRecords", "totalPages"};

	@Override
	public Environment evaluate(Environment env) {

		JsonObject apiResponse = env.getObject("consent_endpoint_response_full");

		if(OIDFJSON.getInt(apiResponse.get("status")) != HttpStatus.SC_UNPROCESSABLE_ENTITY){
			apiResponse = env.getObject("resource_endpoint_response_full");
		}

		JsonObject decodedJwt;
		try {
			decodedJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(apiResponse.getAsJsonObject().get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", apiResponse.getAsJsonObject());
		}
		JsonObject claims = decodedJwt.getAsJsonObject("claims");

		if(JsonHelper.ifExists(claims, "errors")){
			assertAllowedErrorFields(claims);
		}

		if(JsonHelper.ifExists(claims, "meta")){
			final JsonObject metaJson = claims.getAsJsonObject("meta");
			assertAllowedMetaFields(metaJson);
			validateMetaDateTimeFormat(metaJson);
		}

		return env;
	}

	private void assertAllowedErrorFields(JsonObject body) {
		JsonArray errors = body.getAsJsonArray("errors");

		for(JsonElement error: errors){
			assertNoAdditionalErrorFields(error.getAsJsonObject());
		}
	}

	private void assertAllowedMetaFields(JsonObject metaJson) {
		log("Ensure that the 'meta' response " + metaJson + " only contains metadata fields that are defined in the swagger");

		for (Map.Entry<String, JsonElement> meta : metaJson.entrySet())
		{
			log("Checking: " + meta.getKey());
			if ( !ArrayUtils.contains( allowedMetaFields, meta.getKey() ) ) {
				throw error("non-standard meta property '" + meta.getKey() + "'' found in the error response");
			}
		}
	}

	private void validateMetaDateTimeFormat(JsonObject metaJson){
		if (metaJson.has("requestDateTime")){
			final JsonElement requestDateTimeJson = metaJson.get("requestDateTime");
			if(!OIDFJSON.getString(requestDateTimeJson).matches(DatetimeField.ALTERNATIVE_PATTERN)){
				throw error("requestDateTime field " + requestDateTimeJson + " is not compliant with the swagger format");
			}
			logSuccess("requestDateTime field " + requestDateTimeJson + " is compliant with the swagger format");
		}else {
			log("requestDateTime field is missing, skipping");
		}
	}

	private void assertNoAdditionalErrorFields(JsonObject field){
		log("Ensure that the error response " + field + " only contains error fields that are defined in the swagger");

		for (Map.Entry<String, JsonElement> entry : field.entrySet())
		{
			log("Checking: " + entry.getKey());
			if ( !ArrayUtils.contains( allowedErrors, entry.getKey() ) ) {
				throw error("non-standard error property '" + entry.getKey() + "'' found in the error response");
			}
		}
	}

}
