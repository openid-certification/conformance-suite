package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "registration-data-api-test",
	displayName = "Validate structure of all registration data API resources",
	summary = "Validates the structure of all registration data API resources",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class RegistrationDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void validateResponse() {

	}
}
