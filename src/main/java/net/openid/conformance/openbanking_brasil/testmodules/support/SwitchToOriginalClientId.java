package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class SwitchToOriginalClientId extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "original_client_id", required = "client")
	public Environment evaluate(Environment env) {
		String originalClientId = env.getString("original_client_id");
		env.putString("client", "client_id", originalClientId);
		logSuccess("Switched to original client ID", Map.of("Original ID", originalClientId));
		return env;
	}
}
