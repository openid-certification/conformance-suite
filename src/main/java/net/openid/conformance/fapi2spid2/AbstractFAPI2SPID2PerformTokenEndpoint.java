package net.openid.conformance.fapi2spid2;

/**
 * This class finished the test after the token endpoint call
 *
 * i.e. it does not go on to call the resource endpoint.
 */
public abstract class AbstractFAPI2SPID2PerformTokenEndpoint extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		exchangeAuthorizationCode();

	}

	@Override
	protected void callSenderConstrainedTokenEndpoint() {
		callSenderConstrainedTokenEndpointAndStopOnFailure( "FAPI1-BASE-5.2.2-19");
	}
}
