package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Compares the decisions of an evaluations response with the expected decisions by position.
 * An expected element without a {@code decision} (written {@code {}}) is a position whose
 * decision the certification fixture does not define: its response element must still carry a
 * decision, but any value is accepted.
 */
public class EnsureAuthzenEvaluationsResponseValsMatchExpectedVals extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_evaluations_endpoint_expected_response", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = readEvaluationsArray(env, "authzen_evaluations_endpoint_expected_response", "expected");
		JsonArray response = readEvaluationsArray(env, "authzen_evaluations_endpoint_response", "actual");
		if (expected.size() != response.size()) {
			throw error("The number of evaluations elements in the response does not match expected",
				args("expected", expected.size(), "actual", response.size()));
		}
		JsonArray unchecked = new JsonArray();
		for(int i = 0; i < expected.size(); i++) {
			JsonElement expectedElem = expected.get(i);
			JsonElement actualElem = response.get(i);
			if(!expectedElem.isJsonObject()) {
				throw error("An expected evaluations response element is not valid",
					args("position", i, "obj", expectedElem));
			}
			if(!actualElem.isJsonObject() || !actualElem.getAsJsonObject().has("decision")) {
				throw error("An actual evaluations response element is not valid",
					args("position", i, "obj", actualElem));
			}
			if (!expectedElem.getAsJsonObject().has("decision")) {
				unchecked.add(i);
				continue;
			}
			boolean expectedDecisionVal = OIDFJSON.getBoolean(expectedElem.getAsJsonObject().get("decision"));
			boolean actualDecisionVal = OIDFJSON.getBoolean(actualElem.getAsJsonObject().get("decision"));
			if(expectedDecisionVal != actualDecisionVal) {
				throw error("An actual evaluations response decision does not match expected value",
					args("position", i, "expected", expectedDecisionVal, "actual", actualDecisionVal));
			}
		}
		if (unchecked.isEmpty()) {
			logSuccess("The evaluations response match expected values");
		} else {
			logSuccess("The evaluations response match expected values at every position the fixture defines a decision for",
				args("positions not checked", unchecked));
		}
		return env;
	}

	private JsonArray readEvaluationsArray(Environment env, String objectKey, String role) {
		JsonElement elem = env.getElementFromObject(objectKey, "evaluations");
		if (elem == null) {
			throw error(role + " evaluations response has no `evaluations` array", args("env_key", objectKey));
		}
		if (!elem.isJsonArray()) {
			throw error(role + " evaluations response `evaluations` is not an array",
				args("env_key", objectKey, "value", elem));
		}
		return elem.getAsJsonArray();
	}

}
