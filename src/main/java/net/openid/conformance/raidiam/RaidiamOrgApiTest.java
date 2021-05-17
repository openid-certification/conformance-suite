package net.openid.conformance.raidiam;

import net.openid.conformance.fapirwid2.AbstractFAPIRWID2ServerTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "organisation-api-test",
	displayName = "Raidiam Directory Org API test",
	summary = "Calls the org api using a FAPI security profile",
	profile = "PROFILE",
	configurationFields = {
		"resource.resourceUrl"
	}
)
@VariantParameters({
	ClientAuthType.class
})
public class RaidiamOrgApiTest extends AbstractFAPIRWID2ServerTestModule {

	protected void requestProtectedResource() {
		super.requestProtectedResource();
		eventLog.startBlock(currentClientString() + "Validate response");
		callAndStopOnFailure(OrgApiValidator.class);
		eventLog.endBlock();

	}

}
