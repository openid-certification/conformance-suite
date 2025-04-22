package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-invalid-trust-chain-order",
		displayName = "openid-federation-automatic-client-registration-invalid-trust-chain-order",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"deliberately including an invalid trust chain in the authorization request object. " +
			"The test will take the trust chain in the test configuration and reverse the order of it before " +
			"adding it to the request." +
			"<br/><br/>" +
			"If the server does not return an invalid_request, invalid_request_object or a similar well-defined " +
			"and appropriate error back to the client, it must show an error page saying the request is invalid due to " +
			"an invalid aud claim — upload a screenshot of the error page.",
		profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidTrustChainOrderTest extends OpenIDFederationAutomaticClientRegistrationWithJarAndGetAndTrustChainTest {

	@Override
	protected void buildRequestObject() {
		super.buildRequestObject();
		callAndContinueOnFailure(ReverseOrderOfTrustChainInRequestObject.class, Condition.ConditionResult.FAILURE);
	}

}
