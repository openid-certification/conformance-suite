package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.RemoveExpFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-missing-exp-test",
	displayName = "FAPI-CIBA-ID1: Client test - missing exp value in id_token; should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the token endpoint is missing",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAClientInvalidMissingExpTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveExpFromIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing exp value";
	}

}
