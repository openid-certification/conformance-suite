package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class SwitchToOperationalLimitsClientId extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"client", "config"})
	@PostEnvironment(strings = "original_client_id")
	public Environment evaluate(Environment env) {
		String originalClientId = env.getString("client", "client_id");
		String operationalLimitsClientId = env.getString("config", "client.client_id_operational_limits");
		env.putString("original_client_id", originalClientId);
		env.putString("client", "client_id", operationalLimitsClientId);

		logSuccess("Switched to the operational client ID",
			Map.of("Current ID", env.getString("client", "client_id"),
				"Original ID", originalClientId,
				"OL Client ID", operationalLimitsClientId));

		return env;
	}
}
