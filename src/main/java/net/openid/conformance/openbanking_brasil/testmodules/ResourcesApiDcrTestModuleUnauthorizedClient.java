package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "resources-api-dcr-test-unauthorized-client",
	displayName = "Resources API Attempt to use payments with unauthorized client",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the PAGTO role), verify (in several different ways) that it is not possible to obtain a client with the 'resources' scope granted, and that a client credentials grant requesting the 'resources' scope fails with an 'invalid_scope' error. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing. Note also that the Brazil DCR spec says the PAGTO role should get access to the resources role, this is incorrect as per https://github.com/OpenBanking-Brasil/specs-seguranca/issues/212",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl"
	}
)
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment"
})
public class ResourcesApiDcrTestModuleUnauthorizedClient extends AbstractApiDcrTestModuleUnauthorizedClient {
	@Override
	boolean isPaymentsApiTest() {
		return false;
	}
}
