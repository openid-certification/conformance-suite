package net.openid.conformance.fapi2spfinal;

/**
 * This class finished the test after the token endpoint call
 *
 * i.e. it does not go on to call the resource endpoint.
 */
public abstract class AbstractFAPI2SPFinalPerformTokenEndpoint extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		if (clientCredentailsGrant) {
			createClientCredentialsGrantRequest();

			callSenderConstrainedTokenEndpoint();
			processTokenEndpointResponse();
		}
		else {
			createAuthorizationCodeRequest();

			exchangeAuthorizationCode();
		}

	}

	@Override
	protected void callSenderConstrainedTokenEndpoint() {
		callSenderConstrainedTokenEndpointAndStopOnFailure( "FAPI1-BASE-5.2.2-19");
	}
}
