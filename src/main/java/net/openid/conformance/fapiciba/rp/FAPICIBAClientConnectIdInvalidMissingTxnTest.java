package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.RemoveTxnFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-invalid-missing-txn-test",
	displayName = "ConnectID CIBA: Client test - missing txn in ID Token should be rejected",
	summary = "This test returns a ConnectID CIBA token endpoint response with an ID Token that does not contain txn. The client must reject the response and stop the flow.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAClientConnectIdInvalidMissingTxnTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveTxnFromIdToken.class, "CID-IDA-5.1-6", "CID-IDA-5.2-7");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing txn value";
	}

}
