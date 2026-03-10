package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.RemoveIssFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-missing-iss-test",
	displayName = "FAPI-CIBA-ID1: Client test - missing iss value in id_token in id_token; should be rejected",
	summary = "This test should end with the client displaying an error message that the iss value in the id_token is missing",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAClientInvalidMissingIssTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveIssFromIdToken.class, "OIDCC-3.1.3.7-2");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing iss value";
	}

}
