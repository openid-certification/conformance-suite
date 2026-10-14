package net.openid.conformance.authzen.condition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Checks the endpoints entered in the test configuration when the PDP's metadata is not
 * fetched. Only the PDP Identifier is required: an empty endpoint field falls back to the
 * default path under the PDP Identifier (Section 10.1), and each module family uses only its
 * own endpoint. Every endpoint that is entered must be an https URL. The values are fields of
 * the test configuration, so the messages name each by its label on the form.
 */
public class CheckStaticPDPServerConfiguration extends CheckPDPServerConfiguration {

	private static final Map<String, String> LABELS = ImmutableMap.of(
		"policy_decision_point", "PDP Identifier",
		"access_evaluation_endpoint", "Access Evaluation API Endpoint",
		"access_evaluations_endpoint", "Access Evaluations API Endpoint",
		"search_subject_endpoint", "Subject Entity Search API Endpoint",
		"search_resource_endpoint", "Resource Entity Search API Endpoint",
		"search_action_endpoint", "Action Entity Search API Endpoint");

	@Override
	protected List<String> getRequiredEndpoints() {
		return ImmutableList.of("policy_decision_point");
	}

	@Override
	protected List<String> getOptionalEndpoints() {
		return ImmutableList.of("access_evaluation_endpoint",
			"access_evaluations_endpoint",
			"search_subject_endpoint",
			"search_resource_endpoint",
			"search_action_endpoint");
	}

	/**
	 * A field the tester left empty, or cleared after typing in it, is not entered: the module
	 * calls the default path, as {@link AbstractSetAuthzenApiEndpoint} does for the same value.
	 */
	@Override
	protected boolean isProvided(JsonObject server, String endpoint) {
		JsonElement value = server.get(endpoint);
		if (value == null || value.isJsonNull()) {
			return false;
		}
		if (OIDFJSON.isString(value) && OIDFJSON.getString(value).isEmpty()) {
			log("'" + LABELS.get(endpoint) + "' is empty in the test configuration, so the default path under the PDP Identifier is used");
			return false;
		}
		return true;
	}

	@Override
	public Environment validate(Environment env, String endpoint) {
		String field = "'" + LABELS.get(endpoint) + "' field in the 'AuthZEN' section of the test configuration";
		JsonElement value = env.getElementFromObject("server", endpoint);
		if (value == null) {
			throw error(field + " is missing");
		}
		URL url = extractURLOrDie(value, field);
		if (!url.getProtocol().equals(requiredProtocol)) {
			throw error(field + " must use the " + requiredProtocol + " scheme",
				args("required", requiredProtocol, "actual_scheme", url.getProtocol(), "actual", value));
		}
		logSuccess(field + " is an " + requiredProtocol + " URL", args("actual", value));
		return env;
	}
}
