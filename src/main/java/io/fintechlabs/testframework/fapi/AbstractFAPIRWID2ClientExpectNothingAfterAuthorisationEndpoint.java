package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.as.AddTLSClientAuthToServerConfiguration;


public abstract class AbstractFAPIRWID2ClientExpectNothingAfterAuthorisationEndpoint extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void addTokenEndpointAuthMethodSupported() {

		callAndStopOnFailure(AddTLSClientAuthToServerConfiguration.class);
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
