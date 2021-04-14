package net.openid.conformance.fapi1advancedfinal;

/**
 * This class uses to perform only steps that call to token endpoint
 */
public abstract class AbstractFAPI1AdvancedFinalPerformTokenEndpoint extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		requestAuthorizationCode();

	}

}
