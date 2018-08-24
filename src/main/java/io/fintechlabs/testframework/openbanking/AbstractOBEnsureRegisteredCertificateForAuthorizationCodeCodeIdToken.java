package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckForSubscriberInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

public abstract class AbstractOBEnsureRegisteredCertificateForAuthorizationCodeCodeIdToken extends AbstractOBServerTestModuleCodeIdToken {

	@Override
	protected Object performPostAuthorizationFlow() {
		setStatus(Status.WAITING);

		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-2-5.2.2-3");

			callAndStopOnFailure(ValidateIdToken.class, "FAPI-2-5.2.2-3");

			callAndStopOnFailure(ValidateIdTokenNonce.class,"OIDCC-2");

			callAndStopOnFailure(OBValidateIdTokenIntentId.class,"OIDCC-2");

			callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-2-5.2.2-3");

			callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24", "OB-5.2.2-8");

			call(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			skipIfMissing(new String[] { "s_hash" }, null, ConditionResult.INFO,
				ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			call(ExtractCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[] { "c_hash" }, null, ConditionResult.INFO,
				ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			call(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

			skipIfMissing(new String[] { "at_hash" }, null, ConditionResult.INFO,
				ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

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
