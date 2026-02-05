package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-1_0-wallet-happy-path-with-scopes-without-notifications",
	displayName = "OID4VCI 1.0: Wallet happy path test with Scopes but no Notifications",
	summary = """
		This test case validates the standard credential issuance flow for a wallet using an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by emulating a Credential Issuer and the OAuth 2.0 Authorization Server. See the 'issuer' in the exported variables. In the wallet-initiated flow, it expects an authorization request using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. \
		In the issuer-initiated flow, the emulated issuer generates a URL (and a QR code) for a credential offer with an issuer state. That URL is expected to be visited by the wallet. It expects an authorization request containing the issuer state using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. After both variants, it expects a token. The test then expects a call to the nonce and the Credential Endpoint. After successfully validating the credential request, a credential will be generated.\
		The credential configurations must be requested using scopes. \
		If the vci_grant_type=pre_authorization_code is used, you can use 123456 as the tx_code.

		This test will not expose an notification_endpoint.
		""",
	profile = "OID4VCI-1_0",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.redirect_uri",
		"client.jwks",
		"credential.signing_jwk",
		"waitTimeoutSeconds",
		"vci.credential_offer_endpoint"
	}
)
public class VCIWalletHappyPathUsingScopesWithoutNotification extends VCIWalletHappyPathUsingScopes {

	@Override
	protected JsonObject getCredentialIssuerMetadata() {
		JsonObject credentialIssuerMetadata = super.getCredentialIssuerMetadata();

		// remove notification_endpoint support
		credentialIssuerMetadata.remove("notification_endpoint");
		env.removeNativeValue("credential_issuer_notification_endpoint_url");
		return credentialIssuerMetadata;
	}
}
