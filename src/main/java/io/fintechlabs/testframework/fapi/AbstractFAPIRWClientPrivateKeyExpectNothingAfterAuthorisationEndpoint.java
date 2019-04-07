package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.as.AddPrivateKeyJWTToServerConfiguration;
import io.fintechlabs.testframework.condition.as.EnsureClientAssertionTypeIsJwt;
import io.fintechlabs.testframework.condition.as.ExtractClientAssertion;
import io.fintechlabs.testframework.condition.as.ValidateClientAssertionClaims;
import io.fintechlabs.testframework.condition.as.ValidateClientSigningKeySize;


public abstract class AbstractFAPIRWClientPrivateKeyExpectNothingAfterAuthorisationEndpoint extends AbstractFAPIRWClientTestCodeIdToken {

	@Override
	protected void addTokenEndpointAuthMethodSupported() {

		callAndStopOnFailure(AddPrivateKeyJWTToServerConfiguration.class);
	}

	@Override
	protected void validateClientAuthentication() {

		callAndStopOnFailure(ExtractClientAssertion.class, "RFC7523-2.2");

		callAndStopOnFailure(EnsureClientAssertionTypeIsJwt.class, "RFC7523-2.2");

		callAndStopOnFailure(ValidateClientAssertionClaims.class, "RFC7523-3");

		callAndStopOnFailure(ValidateClientSigningKeySize.class, "FAPI-R-5.2.2.5");

	}

	@Override
	protected Object authorizationEndpoint(String requestId){

		Object returnValue = super.authorizationEndpoint(requestId);

		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(5 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				//As the client hasn't called the token endpoint after 5 seconds, assume it has correctly detected the error and aborted.
				fireTestFinished();
			}

			return "done";

		});

		return returnValue;
	}

}
