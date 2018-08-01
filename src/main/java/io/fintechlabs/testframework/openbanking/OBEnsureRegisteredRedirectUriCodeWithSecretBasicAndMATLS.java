package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ob-ensure-registered-redirect-uri-code-with-secret-basic-and-matls",
	displayName = "OB: ensure registered redirect URI (code with client_secret_basic authentication and MATLS)",
	profile = "OB",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.client_secret",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	}
)
public class OBEnsureRegisteredRedirectUriCodeWithSecretBasicAndMATLS extends AbstractOBEnsureRegisteredRedirectUriCode {

	public OBEnsureRegisteredRedirectUriCodeWithSecretBasicAndMATLS(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
		logClientSecretWarning();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		super.onConfigure(config, baseUrl);

		call(EnsureMinimumClientSecretEntropy.class, ConditionResult.FAILURE, "RFC6819-5.1.4.2-2", "RFC6749-10.10");
	}

	@Override
	protected void createClientCredentialsRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParameters.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParameters.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
