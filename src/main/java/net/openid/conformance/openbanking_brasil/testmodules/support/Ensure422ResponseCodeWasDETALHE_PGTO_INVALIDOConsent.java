package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class Ensure422ResponseCodeWasDETALHE_PGTO_INVALIDOConsent extends AbstractJsonAssertingCondition {
	@Override
	public Environment evaluate(Environment env) {

		JsonElement apiResponse;

		String resourceEndpointResponse = env.getString("resource_endpoint_response");
		JsonObject consentEndpointResponse = env.getObject("consent_endpoint_response");

		if (!Strings.isNullOrEmpty(resourceEndpointResponse) && JsonHelper.ifExists(bodyFrom(env), "$.data")) {
			apiResponse = bodyFrom(env);
		} else {
			apiResponse = consentEndpointResponse;
		}

		if (apiResponse == null) {
			throw error("Could not find API response in the environment");
		}

		JsonObject decodedJwt;
		try {
			decodedJwt =
				JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(apiResponse.getAsJsonObject().get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", apiResponse.getAsJsonObject());
		}
		JsonObject claims = decodedJwt.getAsJsonObject("claims");

		JsonArray errors = claims.getAsJsonArray("errors");

		String status = OIDFJSON.getString(errors.get(0).getAsJsonObject().get("code"));

		if (status.equalsIgnoreCase("DETALHE_PGTO_INVALIDO")) {
			logSuccess("Error code is DETALHE_PGTO_INVALIDO as expected");
		} else {
			throw error ("Incorrect error code "+ status + " it should be DETALHE_PGTO_INVALIDO");
		}
		return env;
	}
}
