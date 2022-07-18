package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientIdForOperationalLimitsIsPresent extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String id = env.getString("config", "client.client_id_operational_limits");
		if (Strings.isNullOrEmpty(id)) {
			throw error("client_id_operational_limits is not specified");
		} else {
			logSuccess("client_id_operational_limits was found in the config");
		}
		return env;
	}
}
