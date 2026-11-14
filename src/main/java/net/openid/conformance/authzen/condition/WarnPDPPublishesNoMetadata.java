package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Flags a PDP whose well-known metadata URL answered HTTP 404 under the discovery variant, which
 * {@link GetPDPDynamicServerConfiguration} records in {@code pdp_metadata_not_found}. Section 9
 * makes metadata RECOMMENDED, so the API tests go on at the default paths, but a PDP without it, or
 * a 'PDP Identifier' with a mistyped path, is worth a warning. Intended to be called at WARNING.
 */
public class WarnPDPPublishesNoMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String identifier = env.getString("config", "pdp.policy_decision_point");
		if (Boolean.TRUE.equals(env.getBoolean(GetPDPDynamicServerConfiguration.METADATA_NOT_FOUND))) {
			throw error("The PDP publishes no metadata at the well-known URL derived from the 'PDP Identifier' (HTTP 404). "
				+ "PDPs are recommended to publish metadata describing their configuration. If the PDP does, check the path of "
				+ "the 'PDP Identifier' in the test configuration; if it does not, the test calls the API endpoints at their "
				+ "default paths, or 'static' can be selected for 'PDP server metadata location' to enter them",
				args("policy_decision_point", identifier));
		}
		logSuccess("The PDP publishes metadata", args("policy_decision_point", identifier));
		return env;
	}
}
