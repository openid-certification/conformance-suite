package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import net.openid.conformance.openbanking_brasil.testmodules.AbstractDcrTestModuleAttemptClientTakeover;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildResourcesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClientThatHasClientSpecificJwks;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithAllDadosScopes;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "resources-api-dcr-test-attempt-client-takeover-v2",
	displayName = "Resources API V2 DCR test: attempt to take over client",
	summary = "Obtains a software statement from the Brazil directory (using a client hardcoded into the test suite), registers a new client on the target authorization server then, using valid keys/SSA/etc from a different valid client, attempts to take over the original client. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.\n" +
		"\u2022 Using a hardcoded client with only the DADOS role, retrieves from the directory its SSA\n" +
		"\u2022 Performs a DCR on the provided authorization server -> Expects a success \n" +
		"\u2022 Performs a PUT on the registration endpoint with the same configuration -> Expects a success\n" +
		"\u2022 Changes the certificates used to the second set of certificates that do not belong to the DADOS client created but to a client with a DADOS role from a different organization\n" +
		"\u2022 Attempts 'GET' on client configuration endpoint using MTLS certificate for the second client -> Expects Failure\n" +
		"\u2022 Using the second client with only the DADOS role, retrieves from the directory its SSA\n" +
		"\u2022 Performs a PUT on the registration endpoint with the configuration from the second client -> Expects a failure\n" +
		"\u2022 DELETEs the first registered client from the authorization server",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.client_id"
	}
)
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment"

})
public class ResourcesApiDcrTestModuleAttemptClientTakeoverV2 extends AbstractDcrTestModuleAttemptClientTakeover {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDadosClient.class);
		callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void switchToAlternateClient() {
		callAndStopOnFailure(OverrideClientWithDadosClientThatHasClientSpecificJwks.class);
		callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
	}

}
