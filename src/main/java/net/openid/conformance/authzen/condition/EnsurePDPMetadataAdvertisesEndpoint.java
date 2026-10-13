package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

/**
 * Section 9.1.1: "the absence of any of these parameters is sufficient for the PEP to determine
 * that the PDP is not capable and therefore will not return a result for the associated API".
 * Section 10.1 adds that "If the parameter is not provided, the URL SHOULD be formed by
 * appending the default path [...] to the PDP's base URL (which is the policy_decision_point
 * value from the Policy Decision Point metadata, if available". The two can be read together
 * either way: the default path as a fallback only when there is no metadata, or also when the
 * metadata omits the parameter. This condition takes the first reading, which keeps the 9.1.1
 * capability signal meaningful: when the PDP's metadata omits the endpoint of the API a test
 * module exercises, the module stops and tells the tester to use static metadata instead.
 */
public class EnsurePDPMetadataAdvertisesEndpoint extends AbstractCondition {

	private static final Map<String, String> API_NAMES = Map.of(
		"access_evaluation_endpoint", "Access Evaluation API",
		"access_evaluations_endpoint", "Access Evaluations API",
		"search_subject_endpoint", "Subject Search API",
		"search_resource_endpoint", "Resource Search API",
		"search_action_endpoint", "Action Search API");

	private final String endpointParameter;

	public EnsurePDPMetadataAdvertisesEndpoint(String endpointParameter) {
		if (!API_NAMES.containsKey(endpointParameter)) {
			throw new IllegalArgumentException("Not an AuthZEN endpoint metadata parameter: " + endpointParameter);
		}
		this.endpointParameter = endpointParameter;
	}

	@Override
	@PreEnvironment(required = "pdp")
	public Environment evaluate(Environment env) {
		String apiName = API_NAMES.get(endpointParameter);
		String endpoint = env.getString("pdp", endpointParameter);
		if (Strings.isNullOrEmpty(endpoint)) {
			throw error("The PDP metadata does not contain " + endpointParameter + ", which means the PDP does not support the "
					+ apiName + ", so this test cannot run. Add " + endpointParameter + " to the PDP's metadata, or, if the PDP serves the "
					+ apiName + " without advertising it, set 'PDP server metadata location' to static and enter the endpoint in the test configuration.",
				args("endpoint_parameter", endpointParameter, "pdp_metadata", env.getObject("pdp")));
		}
		logSuccess("The PDP metadata advertises the " + apiName + " endpoint",
			args(endpointParameter, endpoint));
		return env;
	}
}
