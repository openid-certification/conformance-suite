package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddUnexpectedAcrToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-invalid-unexpected-acr-test",
	displayName = "ConnectID CIBA: Client test - unexpected acr in ID Token should be rejected",
	summary = "This test returns a ConnectID CIBA token endpoint response with an ID Token that contains an acr claim. The client must reject the response and stop the flow.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAClientConnectIdInvalidUnexpectedAcrTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddUnexpectedAcrToIdToken.class, "CID-IDA-5.1-2.2");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "unexpected acr value";
	}

}
