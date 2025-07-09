package net.openid.conformance.vciid2wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vciid2wallet.condition.VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse;

@PublishTestModule(
	testName = "oid4vci-id2-wallet-happy-path-with-scopes",
	displayName = "OID4VCIID2: Wallet happy path test with Scopes",
	summary = "This test case validates the standard credential issuance flow for a wallet using an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by emulating a Credential Issuer and the OAuth 2.0 Authorization Server. See the 'issuer' in the exported variables. In the wallet-initiated flow, it expects an authorization request using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. " +
		"In the issuer-initiated flow, the emulated issuer generates a URL (and a QR code) for a credential offer with an issuer state. That URL is expected to be visited by the wallet. It expects an authorization request containing the issuer state using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. After both variants, it expects a token. The test then expects a call to the nonce and the Credential Endpoint. After successfully validating the credential request, a credential will be generated." +
		"The credential configurations must be requested using scopes.",
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
public class VCIWalletHappyPathUsingScopes extends VCIWalletHappyPath {

	@Override
	protected JsonObject getSupportedCredentialConfigurations() {
		JsonObject supportedCredentialConfigurations = super.getSupportedCredentialConfigurations();

		JsonObject euidPid = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1");
		euidPid.addProperty("scope", "eudi.pid.1");
		return supportedCredentialConfigurations;
	}

	@Override
	protected void createTokenEndpointResponse() {
		// super.createTokenEndpointResponse();
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
		callAndStopOnFailure(VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse.class);

		// we always add authorization_details to the token endpoint response
		callAndStopOnFailure(RARSupport.AddRarToTokenEndpointResponse.class);
	}
}
