package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClientThatHasClientSpecificJwks;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdResources;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-dcr-test-attempt-client-takeover",
	displayName = "Resources API DCR test: attempt to take over client",
	summary = "Obtain a software statement from the Brazil directory (using a client hardcoded into the test suite), register a new client on the target authorization server then, using valid keys/SSA/etc from a different valid client, attempt to take over the original client. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class ResourcesApiDcrTestModuleAttemptClientTakeover extends AbstractDcrTestModuleAttemptClientTakeover {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDadosClient.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdResources.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		super.configureClient();
	}

	@Override
	protected void switchToAlternateClient() {
		callAndStopOnFailure(OverrideClientWithDadosClientThatHasClientSpecificJwks.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdResources.class);
	}

}
