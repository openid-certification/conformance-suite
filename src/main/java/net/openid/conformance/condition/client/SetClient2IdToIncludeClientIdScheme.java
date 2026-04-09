package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClient2IdToIncludeClientIdScheme extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config", strings = "client_id_scheme")
	@PostEnvironment(strings = {"client2_id", "orig_client2_id"})
	public Environment evaluate(Environment env) {
		String scheme = env.getString("client_id_scheme");
		String origClientId = env.getString("config", "client2.client_id");

		if (Strings.isNullOrEmpty(origClientId)) {
			throw error("Second client_id missing/empty in test configuration", args("client2", env.getElementFromObject("config", "client2")));
		}

		String client2Id = scheme + ":" + origClientId;

		env.putString("client2_id", client2Id);
		env.putString("orig_client2_id", origClientId);

		logSuccess("Set second client id to include the client id scheme",
			args("client2_id", client2Id, "orig_client2_id", origClientId));

		return env;
	}

}
