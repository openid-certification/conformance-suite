package net.openid.conformance.raidiam;

import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "organisation-api-test",
	displayName = "Raidiam Directory Org API test",
	summary = "Calls the org api using a FAPI security profile",
	profile = "Raidiam Directory Tests",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.resourceUrl"
	}
)
public class RaidiamOrgApiTest extends AbstractFunctionalTestModule {

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(OrgApiStructureValidator.class);
	}

}
