package net.openid.conformance.openid;

import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdEmail;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-email
@PublishTestModule(
	testName = "oidcc-scope-email",
	displayName = "OIDCC: check email scope",
	summary = "This test requests authorization with email scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl"
	}
)
public class OIDCCScopeEmail extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdEmail.class);
		super.skipTestIfScopesNotSupported();
	}

}
