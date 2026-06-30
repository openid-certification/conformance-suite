package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.RemoveConnectIdTrustFrameworkFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-invalid-missing-trust-framework-test",
	displayName = "ConnectID CIBA: Client test - missing verified_claims trust_framework in ID Token should be rejected",
	summary = "This test returns a ConnectID CIBA token endpoint response with an ID Token whose verified_claims verification does not contain trust_framework. The client must reject the response and stop the flow.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAClientConnectIdInvalidMissingTrustFrameworkTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveConnectIdTrustFrameworkFromIdToken.class, "CID-IDA-5.2-12");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing verified_claims trust_framework";
	}

}
