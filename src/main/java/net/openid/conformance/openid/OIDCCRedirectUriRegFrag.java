package net.openid.conformance.openid;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFragmentToRedirectUri;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpointExpectingError;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidRedirectUriOrInvalidClientMetadata;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_redirect_uri_RegFrag
@PublishTestModule(
	testName = "oidcc-redirect-uri-regfrag",
	displayName = "OIDCC: Reject registration where a redirect_uri has a fragment",
	summary = "This test calls the dynamic registration endpoint with a redirect URI containing a fragment specifier. This should result in an error from the dynamic registration endpoint.",
	profile = "OIDCC"
)
public class OIDCCRedirectUriRegFrag extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddFragmentToRedirectUri.class);
		exposeEnvString("redirect_uri");
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		configureDynamicClient();

		// No authorization flow in this test

		fireTestFinished();
	}

	@Override
	protected void configureDynamicClient() {

		createDynamicClientRegistrationRequest();

		callAndStopOnFailure(CallDynamicRegistrationEndpointExpectingError.class, "RFC6749-3.1.2");

		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidRedirectUriOrInvalidClientMetadata.class, Condition.ConditionResult.WARNING, "OIDCR-3.3");
	}

	@Override
	protected void performAuthorizationFlow() {
		// Not used in this test
	}
}
