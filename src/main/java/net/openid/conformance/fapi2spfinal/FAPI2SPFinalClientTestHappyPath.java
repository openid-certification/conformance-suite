package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForInvalidCharsInNonce;
import net.openid.conformance.condition.as.CheckForInvalidCharsInState;
import net.openid.conformance.condition.as.CheckNonceMaximumLength;
import net.openid.conformance.condition.as.CheckStateLength;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-happy-path",
	displayName = "FAPI2-Security-Profile-Final: client test for happy path",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the resource endpoint displayed (usually the 'accounts' or 'userinfo' endpoint).",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPFinalClientTestHappyPath extends AbstractFAPI2SPFinalClientTest {

	@Override
	protected void onConfigurationCompleted() {
		super.onConfigurationCompleted();
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, ConditionResult.WARNING, "RFC7517-4.5", "FAPI2-SP-FINAL-5.4.2-3");
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void createAuthorizationEndpointResponse() {

		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");

		if("yes".equals(isOpenIdScopeRequested)) {
			skipIfMissing(null, new String[] {"nonce"}, ConditionResult.INFO,
				CheckForInvalidCharsInNonce.class, ConditionResult.WARNING);
			skipIfMissing(null, new String[] {"nonce"}, ConditionResult.INFO,
				CheckNonceMaximumLength.class, ConditionResult.WARNING);
		} else {
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
				CreateEffectiveAuthorizationRequestParameters.STATE, ConditionResult.INFO,
				CheckForInvalidCharsInState.class, ConditionResult.WARNING);
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
				CreateEffectiveAuthorizationRequestParameters.STATE, ConditionResult.INFO,
				CheckStateLength.class, ConditionResult.WARNING);
		}

		super.createAuthorizationEndpointResponse();
	}
}
