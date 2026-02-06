package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-1_0-wallet-test-credential-issuance-notification",
	displayName = "OID4VCI 1.0: Wallet Test Credential Issuance with Notification",
	summary = """
		This test case validates the credential issuance flow for a wallet using an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) 1.0 specification.\
		It begins by emulating a Credential Issuer and an OAuth 2.0 Authorization Server. (See the 'issuer' in the exported variables).\

		It supports two flows: a) wallet-initiated flow and b) issuer-initiated flow.

		Wallet-initiated Flow:
		In the wallet-initiated flow, it expects an authorization request via Pushed Authorization Requests (PAR), followed by a token request, for which an access token is returned.\

		Issuer-initiated flow:
		In the issuer-initiated flow, the emulated issuer generates a URL (and a QR code) for a credential offer with an issuer state. That URL is expected to be visited by the wallet.\
		It expects an authorization request containing the issuer state using Pushed Authorization Requests (PAR) and a token request for which an access token is returned.\

		Once the access token has been obtained, the test expects a call to the nonce and credential endpoints. After successfully validating the credential request, a credential will be generated. \
		A notification endpoint is provided that the wallet can call to notify the issuer about the credential processing. \

		Depending on the variant configuration, we support immediate or deferred credential issuance, and plain or encrypted credential responses.

		Note that if the vci_grant_type=pre_authorization_code is used, you can use 123456 as the tx_code.
		""",
	profile = "OID4VCI-1_0",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.redirect_uri",
		"client.jwks",
		"credential.signing_jwk",
		"waitTimeoutSeconds",
		"vci.credential_offer_endpoint",
		"vci.key_attestation_jwks",
		"vci.key_attestation_trust_anchor_pem",
		"vci.credential_configuration_id"
	}
)
public class VCIWalletTestCredentialIssuanceWithNotification extends AbstractVCIWalletTest {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		notificationsSupportEnabled = true;

		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}
}
