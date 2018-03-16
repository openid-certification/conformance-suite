package io.fintechlabs.testframework.openbanking;

import java.util.Map;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ob-ensure-redirect-uri-in-authorization-request-code-id-token-with-secret-post-and-matls",
	displayName = "OB: ensure redirect URI in authorization request (code id_token with client_secret_post authentication and MATLS)",
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
		"resource.institution_id"
	}
)
public class OBEnsureRedirectUriInAuthorizationRequestCodeIdTokenWithSecretPostAndMATLS extends AbstractOBEnsureRedirectUriInAuthorizationRequestCodeIdToken {

	public OBEnsureRedirectUriInAuthorizationRequestCodeIdTokenWithSecretPostAndMATLS(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
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

		callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

	@Override
	protected void createAuthorizationCodeRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
