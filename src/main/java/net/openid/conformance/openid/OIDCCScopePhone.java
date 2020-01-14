package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddPhoneScopeToClientConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-phone
@PublishTestModule(
	testName = "oidcc-scope-phone",
	displayName = "OIDCC: check phone scope",
	summary = "This test requests authorization with phone scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope"
	}
)
public class OIDCCScopePhone extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(AddPhoneScopeToClientConfiguration.class);
		super.skipTestIfScopesNotSupported();
	}

}
