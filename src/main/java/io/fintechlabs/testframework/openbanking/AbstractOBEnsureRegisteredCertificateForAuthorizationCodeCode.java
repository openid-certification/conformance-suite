package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public abstract class AbstractOBEnsureRegisteredCertificateForAuthorizationCodeCode extends AbstractOBServerTestModuleCode {

	public AbstractOBEnsureRegisteredCertificateForAuthorizationCodeCode(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	@Override
	protected Object performPostAuthorizationFlow() {

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
