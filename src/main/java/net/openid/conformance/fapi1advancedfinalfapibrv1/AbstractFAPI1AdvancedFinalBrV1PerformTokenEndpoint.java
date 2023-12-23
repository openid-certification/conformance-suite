package net.openid.conformance.fapi1advancedfinalfapibrv1;

/**
 * This class finished the test after the token endpoint call
 *
 * i.e. it does not go on to call the resource endpoint.
 */
public abstract class AbstractFAPI1AdvancedFinalBrV1PerformTokenEndpoint extends AbstractFAPI1AdvancedFinalBrV1ServerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		requestAuthorizationCode();

	}

}
