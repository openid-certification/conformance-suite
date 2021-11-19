package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideCNPJ;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClientThatHasClientSpecificJwks;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-dcr-test-attempt-client-takeover",
	displayName = "Payments API DCR test: attempt to take over client",
	summary = "Obtain a software statement from the Brazil directory (using a client hardcoded into the test suite), register a new client on the target authorization server then, using valid keys/SSA/etc from a different valid client, attempt to take over the original client. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.client_id",
		"resource.resourceUrl"
	}
)
public class PaymentsApiDcrTestModuleAttemptClientTakeover extends AbstractDcrTestModuleAttemptClientTakeover {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithPagtoClient.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
		callAndStopOnFailure(OverrideCNPJ.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		super.configureClient();
	}

	@Override
	protected void switchToAlternateClient() {
		callAndStopOnFailure(OverrideClientWithPagtoClientThatHasClientSpecificJwks.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
	}

}
