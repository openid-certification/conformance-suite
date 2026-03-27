package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;

/**
 * This class finished the test after the token endpoint call
 *
 * i.e. it does not go on to call the resource endpoint.
 */
public abstract class AbstractFAPI2SPFinalPerformTokenEndpoint extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		if (clientCredentialsGrant) {
			createClientCredentialsGrantRequest();

			callSenderConstrainedTokenEndpoint("FAPI1-BASE-5.2.2-19");
			callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class);
			processTokenEndpointResponse();
		}
		else {
			createAuthorizationCodeRequest();

			exchangeAuthorizationCode();
		}

	}

	@Override
	protected void exchangeAuthorizationCode() {
		callSenderConstrainedTokenEndpoint("FAPI1-BASE-5.2.2-19");

		eventLog.startBlock(currentClientString() + "Verify token endpoint response");
		processTokenEndpointResponse();
		eventLog.endBlock();
	}
}
