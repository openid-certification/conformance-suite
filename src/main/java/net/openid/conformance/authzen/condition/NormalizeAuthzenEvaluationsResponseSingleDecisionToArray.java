package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 7.1, when an Evaluations request omits the `evaluations`
 * member the PDP MAY return either the single-decision form
 * (`{"decision": <bool>}`) or the one-element evaluations array form
 * (`{"evaluations": [{"decision": <bool>}]}`).
 *
 * This condition normalizes the single-decision form into the array form so
 * the downstream validators ({@code EnsureValidEvaluationsResponse} and the
 * val-match conditions) accept either shape without modification. Any
 * additional top-level fields the PDP returned (e.g. {@code context}) are
 * preserved on the normalized object; only the original top-level
 * {@code decision} is moved into the one-element evaluations array.
 *
 * <p><b>Ordering caveat:</b> the post-normalization response no longer carries
 * the original top-level {@code decision} field. Section 7.2 RECOMMENDED-omit
 * checks ({@link EnsureNoTopLevelDecisionWhenEvaluationsPresent}) that need to
 * observe whether the PDP combined both forms MUST run on the
 * {@code authzen_evaluations_endpoint_response} env value BEFORE this
 * normalizer rewrites it. {@code AbstractAuthzenPDPEvaluationsBackwardCompatTest}
 * is wired with the ordering already; new call sites must follow the same rule.
 */
public class NormalizeAuthzenEvaluationsResponseSingleDecisionToArray extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_evaluations_endpoint_response")
	@PostEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_evaluations_endpoint_response");
		if (response.has("evaluations")) {
			log("Response already has `evaluations`; no normalization needed", args("response", response));
			return env;
		}
		if (!response.has("decision")) {
			throw error("Backward-compat response has neither `evaluations` nor top-level `decision`",
				args("response", response));
		}
		JsonObject normalized = new JsonObject();
		JsonArray evaluations = new JsonArray();
		JsonObject entry = new JsonObject();
		entry.add("decision", response.get("decision"));
		evaluations.add(entry);
		normalized.add("evaluations", evaluations);
		// Preserve every other top-level field the PDP returned so downstream
		// checks (e.g. EnsureNoTopLevelDecisionWhenEvaluationsPresent, context
		// validation, unknown-field warnings) see what the PDP actually sent.
		for (Map.Entry<String, JsonElement> field : response.entrySet()) {
			if (!"decision".equals(field.getKey()) && !"evaluations".equals(field.getKey())) {
				normalized.add(field.getKey(), field.getValue());
			}
		}
		env.putObject("authzen_evaluations_endpoint_response", normalized);
		logSuccess("Normalized single-decision response to one-element evaluations array",
			args("normalized", normalized));
		return env;
	}
}
