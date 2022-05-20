package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.field.DatetimeField;
import org.apache.commons.lang3.ArrayUtils;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class ValidateErrorAndMetaFieldNames extends AbstractJsonAssertingCondition {

	private static final String[] allowedErrors = {"code","title","detail"};
	private static final String[] allowedMetaFields = {"requestDateTime", "totalRecords", "totalPages"};

	private static final Set<String> errorCodes = Sets.newHashSet(
		"FORMA_PGTO_INVALIDA", "DATA_PGTO_INVALIDA", "DETALHE_PGTO_INVALIDO", "NAO_INFORMADO"
	);

	@Override
	public Environment evaluate(Environment env) {

		JsonObject apiResponse;
		if(env.getObject("resource_endpoint_response_full") != null){
			apiResponse = env.getObject("resource_endpoint_response_full");
		}else {
			apiResponse = env.getObject("consent_endpoint_response_full");
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
		}else {
			throw error("errors JSON Array field is missing in the response", Map.of("body", claims));
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
		log("Ensure that the 'meta' response only contains metadata fields that are defined in the swagger", Map.of("meta", metaJson));

		for (Map.Entry<String, JsonElement> meta : metaJson.entrySet())
		{
			log("Checking: " + meta.getKey());
			if ( !ArrayUtils.contains( allowedMetaFields, meta.getKey() ) ) {
				throw error("non-standard meta property found in the error response", Map.of("meta",  meta.getKey()));
			}
		}
	}

	private void validateMetaDateTimeFormat(JsonObject metaJson){
		if (metaJson.has("requestDateTime")){
			final JsonElement requestDateTimeJson = metaJson.get("requestDateTime");
			if(!OIDFJSON.getString(requestDateTimeJson).matches(DatetimeField.ALTERNATIVE_PATTERN)){
				throw error("requestDateTime field is not compliant with the swagger format", Map.of("requestedDateTime", requestDateTimeJson));
			}
			logSuccess("requestDateTime field is compliant with the swagger format", Map.of("requestedDateTime", requestDateTimeJson));
		}else {
			log("requestDateTime field is missing, skipping");
		}
	}

	private void assertNoAdditionalErrorFields(JsonObject field) {
		log("Ensure that the error response only contains error fields that are defined in the swagger", Map.of("error response", field));

		for (Map.Entry<String, JsonElement> entry : field.entrySet()) {
			log("Checking: " + entry.getKey());
			if (!ArrayUtils.contains(allowedErrors, entry.getKey())) {
				throw error("non-standard error property found in the error response", Map.of("property", entry.getKey()));
			}
			if (entry.getKey().equals("code") && !errorCodes.contains(entry.getKey())) {
				throw error("Code field in error object is not specification compliant ", Map.of("actual code", entry.getValue(), "expected code", errorCodes));
			}
		}
	}

}
