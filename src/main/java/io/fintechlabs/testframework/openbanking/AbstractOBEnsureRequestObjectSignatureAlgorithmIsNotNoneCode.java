package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

public abstract class AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNoneCode extends AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNone {

	public AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNoneCode(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCode();
	}

}
