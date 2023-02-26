package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetScopeInClientConfigurationToOpenId extends AbstractSetScopeInClientConfiguration {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String scope = "openid";

		// This is necessary to allow the Australian ConnectID tests to run in our CI against Authlete;
		// in particular as the same Authlete server is supports multiple use cases just requesting
		// the 'openid' scope doesn't trigger FAPI2 behaviour; this override allows us to request
		// an additional scope that does enable FAPI2 behaviour
		String overrideScope = env.getString("client", "override_openid_scope");
		if (overrideScope != null) {
			scope = overrideScope;
		}

		return setScopeInClientConfiguration(env, scope);
	}

}
