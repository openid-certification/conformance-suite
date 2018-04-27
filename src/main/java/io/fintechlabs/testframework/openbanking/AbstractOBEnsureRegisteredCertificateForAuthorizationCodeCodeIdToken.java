package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckForSubscriberInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractStateHash;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureRegisteredCertificateForAuthorizationCodeCodeIdToken extends AbstractOBServerTestModuleCodeIdToken {

	public AbstractOBEnsureRegisteredCertificateForAuthorizationCodeCodeIdToken(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	@Override
	protected Object performPostAuthorizationFlow() {

		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-2-5.2.2-3");
		
		callAndStopOnFailure(OBValidateIdTokenIntentId.class,"OIDCC-2");

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24", "OB-5.2.2-8");

		call(ExtractStateHash.class, "FAPI-2-5.2.2-4");

		skipIfMissing(new String[] { "state_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");
		
		createAuthorizationCodeRequest();

		// Check that a call to the token endpoint succeeds normally

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		// Now try with the wrong certificate

		callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);

		callAndStopOnFailure(CallTokenEndpointExpectingError.class, "OB-5.2.2-5");

		fireTestFinished();
		stop();

		return new ModelAndView("complete", ImmutableMap.of("test", this));
	}

}
