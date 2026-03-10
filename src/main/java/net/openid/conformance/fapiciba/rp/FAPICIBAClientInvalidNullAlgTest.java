package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.SignIdTokenWithNullAlgorithm;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-null-alg-test",
	displayName = "FAPI-CIBA-ID1: Client test - null algorithm used for serialization of id_token from the token endpoint; should be rejected",
	summary = "This test should end with the client displaying an error message that the id_token was signed with alg: none",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAClientInvalidNullAlgTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomSignatureOfIdToken(){
		callAndStopOnFailure(SignIdTokenWithNullAlgorithm.class,"OIDCC-3.1.3.7-7");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "null alg value";
	}

}
