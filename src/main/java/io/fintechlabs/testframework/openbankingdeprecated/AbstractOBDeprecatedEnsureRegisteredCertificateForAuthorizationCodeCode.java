package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;

public abstract class AbstractOBDeprecatedEnsureRegisteredCertificateForAuthorizationCodeCode extends AbstractOBDeprecatedServerTestModuleCode {

	@Override
	protected Object performPostAuthorizationFlow() {
		setStatus(Status.WAITING);

		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			createAuthorizationCodeRequest();

			// Check that a call to the token endpoint succeeds normally

			callAndStopOnFailure(CallTokenEndpoint.class);

			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

			// Now try with the wrong certificate

			callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);

			callAndStopOnFailure(CallTokenEndpointExpectingError.class, "OB-5.2.2-5");

			fireTestFinished();
			return "done";
		});

		return redirectToLogDetailPage();
	}

}
