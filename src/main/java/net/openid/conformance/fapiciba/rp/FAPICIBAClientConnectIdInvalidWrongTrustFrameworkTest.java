package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.SetInvalidConnectIdTrustFrameworkInIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-invalid-wrong-trust-framework-test",
	displayName = "ConnectID CIBA: Client test - wrong verified_claims trust_framework in ID Token should be rejected",
	summary = "This test returns a ConnectID CIBA token endpoint response with an ID Token whose verified_claims verification trust_framework is not au_connectid. The client must reject the response and stop the flow.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAClientConnectIdInvalidWrongTrustFrameworkTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(SetInvalidConnectIdTrustFrameworkInIdToken.class, "CID-IDA-5.2-12");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "wrong verified_claims trust_framework";
	}

}
