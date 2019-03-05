package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddTLSClientAuthToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureNoClientAssertionSentToTokenEndpoint;
import io.fintechlabs.testframework.condition.as.AddInvalidSHashValueToIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ob-client-test-code-id-token-with-matls-invalid-shash",
	displayName = "FAPI-OB: client test (code id_token with MATLS and an invalid s_hash value)",
	profile = "FAPI-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIOBClientTestCodeIdTokenWithMATLSInvalidSHash extends AbstractFAPIOBClientTestCodeIdToken {

	@Override
	protected void addTokenEndpointAuthMethodSupported() {

		callAndContinueOnFailure(AddTLSClientAuthToServerConfiguration.class);
	}

	@Override
	protected void validateClientAuthentication() {

		//Parent class has already verified the presented TLS certificate so nothing to do here.

		callAndStopOnFailure(EnsureNoClientAssertionSentToTokenEndpoint.class);

	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidSHashValueToIdToken.class, "FAPI-RW-5.2.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an invalid s_hash.");

	}

	@Override
	protected Object authorizationEndpoint(String requestId){

		Object returnValue = super.authorizationEndpoint(requestId);

		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(5 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				//As the client hasn't call the token endpoint after 5 seconds, assume it has correctly detected the error and aborted.
				fireTestFinished();
			}

			return "done";

		});

		return returnValue;
	}

}
