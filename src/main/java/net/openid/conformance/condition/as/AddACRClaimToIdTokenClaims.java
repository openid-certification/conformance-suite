package net.openid.conformance.condition.as;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AddACRClaimToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "requested_id_token_acr_values")
	@PostEnvironment(required = "id_token_claims")

	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String acr = env.getString("requested_id_token_acr_values");

		JsonArray acrValues = (JsonArray) JsonParser.parseString(acr);
		if (acrValues == null || !acrValues.isJsonPrimitive()) {

			String[] acrValuesString = new Gson().fromJson(acrValues, String[].class);
			List<String> requestedACRs;
			requestedACRs = Arrays.asList(acrValuesString);

			List<String> acceptableAcrValues = new ArrayList<>();
			acceptableAcrValues.add("urn:openbanking:psd2:sca");
			acceptableAcrValues.add("urn:openbanking:psd2:ca");

			String acrValue = null;
			for (String singleACRValue : requestedACRs) {
				if (singleACRValue.contains(acceptableAcrValues.get(0))) {
					acrValue = singleACRValue;
					break;
				} else if (singleACRValue.contains(acceptableAcrValues.get(1))) {
					acrValue = singleACRValue;
				} else {
					throw error("Unsupported acr value in id_token_claims", args("supported_acr_values", acceptableAcrValues, "received_value", requestedACRs));
				}
			}

			claims.addProperty("acr", acrValue);
			env.putObject("id_token_claims", claims);
			logSuccess("Added acr value to id_token_claims", args("claims", claims, "acr_value", acrValue));
		}
		return env;
	}
}
