package net.openid.conformance.condition.client;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FAPIBrazilValidateRequestObjectIdTokenACRClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		boolean newSecurityProfile = false;
		String scope = env.getString("authorization_request_object", "claims.scope");
		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		List<String> expectedValues = new ArrayList<>();
		//Read-and-Write APIs (Transactional): shall require resource owner authentication to at least LoA3.

		if (scopes.contains("accounts")) {
			newSecurityProfile = true;
			expectedValues.add("urn:brasil:openbanking:loa2");
			expectedValues.add("urn:brasil:openbanking:loa3");
		}
		if (scopes.contains("payments")) {
			newSecurityProfile = true;
			expectedValues.add("urn:brasil:openbanking:loa3");
		}
		if (scopes.contains("resources")) {
			expectedValues.add("urn:brasil:openinsurance:loa2");
		}

		JsonElement acrClaim = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr");
		if (acrClaim == null) {
			if (newSecurityProfile) {
				throw error("Client has not requested the acr claim");
			}
			log("acr claim is not requested");
			return env;
		}
		if (!acrClaim.isJsonObject()) {
			throw error("The acr claim is not a JsonObject", args("acrClaim", acrClaim));
		}

		if (newSecurityProfile) {
			if (acrClaim.getAsJsonObject().has("essential") == false ||
				OIDFJSON.getBoolean(acrClaim.getAsJsonObject().get("essential")) != true) {
				throw error("Client has not requested the acr claim as an 'essential' claim", args("acrClaim", acrClaim));
			}
		}

		// https://openid.net/specs/openid-connect-core-1_0.html#acrSemantics
		List<String> receivedValues = new ArrayList<>();
		if (acrClaim.getAsJsonObject().has("values")) {
			if (newSecurityProfile) {
				throw error("Client has requested 'values' for the acr claim", args("acrClaim", acrClaim));
			}

			JsonElement acrValues = acrClaim.getAsJsonObject().get("values");
			if (acrValues == null || !acrValues.isJsonArray()) {
				throw error("Acr values is missing or is not an array in request object", args("received", acrValues));
			}

			String[] acrValuesString = new Gson().fromJson(acrValues.getAsJsonArray(), String[].class);
			receivedValues = Arrays.asList(acrValuesString);

		} else if (acrClaim.getAsJsonObject().has("value")) {
			if (newSecurityProfile) {
				throw error("Client has requested 'value' for the acr claim", args("acrClaim", acrClaim));
			}

			JsonElement acrValue = acrClaim.getAsJsonObject().get("value");
			if (acrValue == null) {
				throw error("Acr values is null in request object", args("acrClaim", acrClaim));
			}

			receivedValues.add(OIDFJSON.getString(acrValue));

		} else {
			if (newSecurityProfile) {
				// ensure we return an acr value, even though there was no request for an explicit value
				JsonArray matchedAcrValues = new JsonArray();
				matchedAcrValues.add("urn:brasil:openbanking:loa3");
				env.putString("requested_id_token_acr_values", matchedAcrValues.toString());
				logSuccess("Acr value in request object is as expected");
				return env;
			}

			throw error("Acr values is missing in request object", args("acrClaim", acrClaim));
		}

		JsonArray matchedAcrValues = new JsonArray();
		for (String possibleAcrValues : receivedValues) {
			if (expectedValues.contains(possibleAcrValues)) {
				matchedAcrValues.add(possibleAcrValues);
			}
		}

		if (matchedAcrValues.size() > 0) {
			env.putString("requested_id_token_acr_values", matchedAcrValues.toString());
			logSuccess("Acr value in request object is as expected", args("received", matchedAcrValues));
		} else {
			throw error("An acr value in the request object does not match the defined standard",
				args("received", receivedValues, "required", expectedValues));
		}

		return env;
	}
}
