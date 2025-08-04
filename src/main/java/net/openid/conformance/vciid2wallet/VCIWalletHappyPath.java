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
	summary = "This test case validates the standard credential issuance flow for a wallet using an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by emulating a Credential Issuer and the OAuth 2.0 Authorization Server. See the 'issuer' in the exported variables. In the wallet-initiated flow, it expects an authorization request using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. " +
		"In the issuer-initiated flow, the emulated issuer generates a URL (and a QR code) for a credential offer with an issuer state. That URL is expected to be visited by the wallet. It expects an authorization request containing the issuer state using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. After both variants, it expects a token. The test then expects a call to the nonce and the Credential Endpoint. After successfully validating the credential request, a credential will be generated. " +
		"If the vci_grant_type=pre_authorization_code is used, you can use 123456 as the tx_code.",
	profile = "OID4VCI-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"credential.signing_jwk",
		"waitTimeoutSeconds",
		"vci.credential_offer_endpoint"
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
