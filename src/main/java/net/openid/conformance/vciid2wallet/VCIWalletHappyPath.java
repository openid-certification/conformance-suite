package net.openid.conformance.vciid2wallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForInvalidCharsInNonce;
import net.openid.conformance.condition.as.CheckForInvalidCharsInState;
import net.openid.conformance.condition.as.CheckNonceMaximumLength;
import net.openid.conformance.condition.as.CheckStateLength;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-id2-wallet-happy-path",
	displayName = "OID4VCIID2: Wallet happy path test",
	summary = "Tests a 'happy path' flow; TBC.",
	profile = "OID4VCI-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"credential.signing_jwk",
		"waitTimeoutSeconds"
	}
)

public class VCIWalletHappyPath extends AbstractVCIWalletTest {

	@Override
	protected void onConfigurationCompleted() {
		super.onConfigurationCompleted();
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, ConditionResult.WARNING, "RFC7517-4.5", "FAPI2-SP-ID2-5.6.3-3");
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
