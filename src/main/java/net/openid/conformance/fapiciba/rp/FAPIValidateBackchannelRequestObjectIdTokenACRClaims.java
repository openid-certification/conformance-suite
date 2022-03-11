package net.openid.conformance.fapiciba.rp;

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

public class FAPIValidateBackchannelRequestObjectIdTokenACRClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {

		JsonElement acrClaim = env.getElementFromObject("backchannel_request_object", "claims.claims.id_token.acr");

		if (acrClaim == null || !acrClaim.isJsonObject()) {
			throw error("The acr claim is missing or is not a JsonObject", args("acrClaim", acrClaim));
		}

		Boolean validateEssential = OIDFJSON.getBoolean(acrClaim.getAsJsonObject().get("essential"));
		if (!validateEssential) {
			throw error("The acr claim must be requested as essential: true", args("received", validateEssential, "required", true));
		}

		// https://openid.net/specs/openid-connect-core-1_0.html#acrSemantics
		List<String> receivedValues = new ArrayList<>();
		if (acrClaim.getAsJsonObject().has("values")) {

			JsonElement acrValues = acrClaim.getAsJsonObject().get("values");
			if (acrValues == null || !acrValues.isJsonArray()) {
				throw error("Acr values is missing or is not an array in request object", args("received", acrValues));
			}

			String[] acrValuesString = new Gson().fromJson(acrValues.getAsJsonArray(), String[].class);
			receivedValues = Arrays.asList(acrValuesString);

		} else if (acrClaim.getAsJsonObject().has("value")) {

			JsonElement acrValue = acrClaim.getAsJsonObject().get("value");
			if (acrValue == null) {
				throw error("Acr values is null in request object", args("acrClaim", acrClaim));
			}

			receivedValues.add(OIDFJSON.getString(acrValue));

		} else {
			throw error("Acr values is missing in request object", args("acrClaim", acrClaim));
		}

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("urn:openbanking:psd2:sca");
		expectedValues.add("urn:openbanking:psd2:ca");

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
			throw error("An acr value in the request object does not match the defined standard", args("received", matchedAcrValues, "required", expectedValues));
		}

		return env;
	}
}
