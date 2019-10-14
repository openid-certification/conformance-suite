package net.openid.conformance.fapi;

/**
 * This class uses to perform only steps that call to token endpoint
 */
public abstract class AbstractFAPIRWID2PerformTokenEndpoint extends AbstractFAPIRWID2ServerTestModule {

	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		requestAuthorizationCode();

	}

}
