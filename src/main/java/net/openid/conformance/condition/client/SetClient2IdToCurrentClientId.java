package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClient2IdToCurrentClientId extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "client_id")
	@PostEnvironment(strings = "client2_id")
	public Environment evaluate(Environment env) {
		String clientId = env.getString("client_id");

		if (Strings.isNullOrEmpty(clientId)) {
			throw error("client_id missing/empty in environment");
		}

		env.putString("client2_id", clientId);

		logSuccess("Set second client_id to match the first client_id",
			args("client2_id", clientId));

		return env;
	}

}
