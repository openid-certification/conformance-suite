package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractApiDcrTestModuleUnauthorizedClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildResourcesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "resources-api-dcr-test-unauthorized-client-v2",
	displayName = "Resources API V2 Attempt to use payments with unauthorized client",
	summary = "Obtains a software statement from the Brazil sandbox directory (using a hardcoded client that has the PAGTO role), verifies (in several different ways) that it is not possible to obtain a client with the 'resources' scope granted and that a client credentials grant requesting the 'resources' scope fails with an 'invalid_scope' error. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing. Note also that the Brazil DCR spec says the PAGTO role should get access to the resources role; this is incorrect as per https://github.com/OpenBanking-Brasil/specs-seguranca/issues/212\n" +
		"\u2022 Using a hardcoded client with only the PAGTO role, retrieves from the directory its SSA\n" +
		"\u2022 Performs a DCR on the provided authorization server passing the parameter dados related scopes -> Can either expect a 400 or, if it resulted in a client being created, make sure that the server did not grant the payments scope.\n" +
		"\u2022 Performs a DCR on the provided authorization server without providing the scope parameter -> Make sure that the server did not grant any dados scope.\n" +
		"\u2022 Performs a PUT on the registration endpoint providing only PAGTO scopes -> Expects a success, also making sure that the server did not grant any dados scope.\n" +
		"\u2022 Performs a PUT on the registration endpoint providing only PAYMENTS scopes -> Can either expect a 400 or, if it resulted in a client being created, make sure that the server did not grant the dados scope.\n" +
		"\u2022 Tries using client credentials grant to obtain an access token with dados scope -> expects a 400 return \n" +
		"\u2022 DELETEs the registered client from the authorization server",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment"
})
public class ResourcesApiDcrTestModuleUnauthorizedClientV2 extends AbstractApiDcrTestModuleUnauthorizedClient {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected boolean isPaymentsApiTest() {
		return false;
	}
}
