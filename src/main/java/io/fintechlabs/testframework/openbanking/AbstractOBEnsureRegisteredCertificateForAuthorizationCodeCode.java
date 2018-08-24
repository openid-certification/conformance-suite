package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

public abstract class AbstractOBEnsureRegisteredCertificateForAuthorizationCodeCode extends AbstractOBServerTestModuleCode {

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
