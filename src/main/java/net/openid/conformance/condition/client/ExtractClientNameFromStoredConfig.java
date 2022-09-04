package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractClientNameFromStoredConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_client_config")
	public Environment evaluate(Environment env) {
		// pull out the client name and put it in the root environment for easy access (if there is one)
		String clientName = env.getString("original_client_config", "client_name");
		if (!Strings.isNullOrEmpty(clientName)) {
			env.putString("client_name", clientName);
		}
		log("Extracted client_name from stored client configuration.",
			args("client_name", Strings.emptyToNull(clientName)));

		return env;
	}

}
