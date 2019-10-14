package net.openid.conformance.condition.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FAPIValidateRequestObjectIdTokenACRClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {


		JsonArray acrValues = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.values").getAsJsonArray();

		Boolean validateEssential = env.getBoolean("authorization_request_object", "claims.claims.id_token.acr.essential");
		if (!validateEssential) {
			throw error("The acr claim must be requested as essential: true", args("received", validateEssential, "required", true));
		}
		//https://openid.net/specs/openid-connect-core-1_0.html#acrSemantics

		if (acrValues == null || acrValues.isJsonPrimitive()) {
			throw error("Invalid or missing acr claim in request object", args("received", env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.values")));
		}

			String[] acrValuesString = new Gson().fromJson(acrValues, String[].class);
			List<String> receivedValues;
			receivedValues = Arrays.asList(acrValuesString);

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
