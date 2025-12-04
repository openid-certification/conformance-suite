package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIInjectRandomCNonce;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-invalid-nonce",
	displayName = "OID4VCI 1.0: Issuer with invalid nonce",
	summary = "This test case checks for the proper error handling during the standard credential issuance flow using an emulated wallet when an invalid c_nonce is used.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerInvalidNonce extends VCIIssuerHappyFlow {

	@Override
	protected void afterNonceEndpointResponse() {
		super.afterNonceEndpointResponse();

		callAndContinueOnFailure(VCIInjectRandomCNonce.class, Condition.ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
	}
}
