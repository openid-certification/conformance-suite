package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.JsonUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;

import java.text.ParseException;

public class OptionallyAllow201Or422 extends AbstractCondition {

	private static final String[] allowedErrors = {"code","title","detail"};
	private static final String[] allowedMetaFields = {"requestDateTime", "totalRecords", "totalPages"};

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("endpoint_response", "status");
		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if(statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			logSuccess(endpointName + " endpoint returned an http status of 422 - validating response and ending test now", args("http_status", statusCode));
			validateErrorAndMetaFieldNames(env);
		}

		if(statusCode == HttpStatus.SC_CREATED) {
			logSuccess(endpointName + " endpoint returned an http status of 201 - proceeding with test now", args("http_status", statusCode));
			env.putString("proceed_with_test", "proceed");
		}

		if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			throw error(endpointName + " endpoint returned an unexpected http status - either 201 or 422 accepted", args("http_status", statusCode));
		}

		logSuccess(endpointName + " endpoint returned the expected http status", args("http_status", statusCode));

		return env;

	}

	private void validateErrorAndMetaFieldNames(Environment env) {

		JsonObject apiResponse = env.getObject("resource_endpoint_response_full");
		if(apiResponse == null) {
			log("resource endpoint response was null, fetch consent endpoint response");
			apiResponse = env.getObject("consent_endpoint_response_full");
		}
		log("Validating API response:", apiResponse);
		if(OIDFJSON.getInt(apiResponse.get("status")) != 422){
			logFailure("Couldn't find a 422 response on API response, setting it to consent response");
			log("Additional info (consent endpoint response):", env.getObject("consent_endpoint_response_full"));
			apiResponse = env.getObject("consent_endpoint_response_full");
			if(OIDFJSON.getInt(apiResponse.get("status")) != 422){
				log("Consent endpoint response is also not 422");
				log(apiResponse);
			}
		}

		JsonObject decodedJwt;
		try {
			decodedJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(apiResponse.getAsJsonObject().get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", apiResponse.getAsJsonObject());
		}
		JsonObject claims = decodedJwt.getAsJsonObject("claims");
		log(claims);

		if (JsonHelper.ifExists(claims, "errors")) {

			assertAllowedErrorFields(claims);
		}

		if (JsonHelper.ifExists(claims, "meta")) {
			assertAllowedMetaFields(claims.getAsJsonObject("meta"));
		}
	}

	protected JsonElement bodyFrom(Environment environment) {
		String resource = environment.getString("resource_endpoint_response");
		return JsonUtils.createBigDecimalAwareGson().fromJson(resource, JsonElement.class);
	}

	private void assertAllowedErrorFields(JsonObject body) {
		JsonArray errors = body.getAsJsonArray("errors");

		for(JsonElement error: errors){
			assertNoAdditionalErrorFields(error.getAsJsonObject());
		}
	}

	private void assertAllowedMetaFields(JsonObject metaJson) {
		log("Ensure that the 'meta' response " + metaJson + " only contains metadata fields that are defined in the swagger");

		for (String meta : metaJson.keySet())
		{
			log("Checking: " + meta);
			if ( !ArrayUtils.contains( allowedMetaFields, meta) ) {
				throw error("non-standard meta property '" + meta + "'' found in the error response");
			}
		}
	}

	private void assertNoAdditionalErrorFields(JsonObject field){
		log("Ensure that the error response " + field + " only contains error fields that are defined in the swagger");

		for (String entry : field.keySet())
		{
			log("Checking: " + entry);
			if ( !ArrayUtils.contains( allowedErrors, entry ) ) {
				throw error("non-standard error property '" + entry + "'' found in the error response");
			}
		}
	}
}
