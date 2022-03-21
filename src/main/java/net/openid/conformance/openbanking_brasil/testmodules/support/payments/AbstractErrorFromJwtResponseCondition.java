package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractErrorFromJwtResponseCondition extends AbstractCondition {

	private static final String[] allowedErrors = {"code","title","detail"};
	private static final String[] allowedMetaFields = {"requestDateTime", "totalRecords", "totalPages"};

	protected void validateError(JsonObject response, String errorToExpect) {
		int status = OIDFJSON.getInt(response.get("status"));
		switch(status) {
			case 422:
				try {
					JsonObject jwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(response.get("body")));
					JsonObject claims = jwt.getAsJsonObject("claims");
					JsonArray errors = claims.getAsJsonArray("errors");
					JsonObject meta = claims.getAsJsonObject("meta");
					validateErrorAndMetaFields(errors, meta);
					checkErrorPresent(errors, errorToExpect);
				} catch (ParseException e) {
					throw error("Could not parse JWT");
				}
				break;
			default:
				log("Response status was not 422 - not taking any action", Map.of("status", status));
				break;
		}
	}

	private void checkErrorPresent(JsonArray errors, String errorToExpect) {
		final AtomicBoolean found = new AtomicBoolean(false);
		errors.forEach(e -> {
			JsonObject error = (JsonObject) e;
			String errorCode = OIDFJSON.getString(error.get("code"));
			if(errorCode.equals(errorToExpect)) {
				found.set(true);
				return;
			}
		});
		if(found.get()) {
			logSuccess("Successfully found error code  " + errorToExpect);
		} else{
			throw error("Error code was not as expected", Map.of("expected", errorToExpect, "errors", errors));
		}
	}

	private void validateErrorAndMetaFields(JsonArray errors, JsonObject meta){
		if(errors == null){
			throw error("Errors not found, failing");
		}

		assertAllowedErrorFields(errors);
		if(meta != null){
			assertAllowedMetaFields(meta);
		}
	}

	private void assertAllowedErrorFields(JsonArray errors) {
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
			if ( !ArrayUtils.contains( allowedErrors, entry) ) {
				throw error("non-standard error property '" + entry + "'' found in the error response");
			}
		}
	}

}
