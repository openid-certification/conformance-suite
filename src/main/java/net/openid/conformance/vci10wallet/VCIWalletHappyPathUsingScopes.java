package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.vci10wallet.condition.VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCIInjectRequestScopePreAuthorizedCodeFlow;

@PublishTestModule(
	testName = "oid4vci-1_0-wallet-happy-path-with-scopes",
	displayName = "OID4VCI 1.0: Wallet happy path test with Scopes",
	summary = "This test case validates the standard credential issuance flow for a wallet using an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by emulating a Credential Issuer and the OAuth 2.0 Authorization Server. See the 'issuer' in the exported variables. In the wallet-initiated flow, it expects an authorization request using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. " +
		"In the issuer-initiated flow, the emulated issuer generates a URL (and a QR code) for a credential offer with an issuer state. That URL is expected to be visited by the wallet. It expects an authorization request containing the issuer state using Pushed Authorization Requests (PAR) and a token request for which an access token is returned. After both variants, it expects a token. The test then expects a call to the nonce and the Credential Endpoint. After successfully validating the credential request, a credential will be generated." +
		"The credential configurations must be requested using scopes. " +
		"If the vci_grant_type=pre_authorization_code is used, you can use 123456 as the tx_code.",
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
public class VCIWalletHappyPathUsingScopes extends VCIWalletHappyPath {

	@Override
	protected JsonObject getSupportedCredentialConfigurations() {
		JsonObject supportedCredentialConfigurations = super.getSupportedCredentialConfigurations();

		JsonObject euidPid = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1");
		euidPid.addProperty("scope", "eudi.pid.1");

		JsonObject euidPidAttestation = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1.attestation");
		euidPidAttestation.addProperty("scope", "eudi.pid.1.attestation");

		JsonObject euidPidJwtKeyAttest = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1.jwt.keyattest");
		euidPidJwtKeyAttest.addProperty("scope", "eudi.pid.1.jwt.keyattest");

		JsonObject euidPidAttestationKeyAttest = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1.attestation.keyattest");
		euidPidAttestationKeyAttest.addProperty("scope", "eudi.pid.1.attestation.keyattest");

		JsonObject euidPidJwtAndAttestationkeyAttest = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1.jwt_and_attestation.keyattest");
		euidPidJwtAndAttestationkeyAttest.addProperty("scope", "eudi.pid.1.jwt_and_attestation.keyattest");

		JsonObject euidPidNonBinding = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.1.nobinding");
		euidPidNonBinding.addProperty("scope", "eudi.pid.1.nobinding");

		JsonObject euidPidMdoc = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.mdoc.1");
		euidPidMdoc.addProperty("scope", "eudi.pid.mdoc.1");

		JsonObject euidPidMdocAttest = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.mdoc.1.attestation");
		euidPidMdocAttest.addProperty("scope", "eudi.pid.mdoc.1.attestation");

		JsonObject euidPidMdocKeyAttest = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.mdoc.1.jwt.keyattest");
		euidPidMdocKeyAttest.addProperty("scope", "eudi.pid.mdoc.1.jwt.keyattest");

		JsonObject euidPidMdocAttestationKeyAttest = supportedCredentialConfigurations.getAsJsonObject("eu.europa.ec.eudi.pid.mdoc.1.attestation.keyattest");
		euidPidMdocAttestationKeyAttest.addProperty("scope", "eudi.pid.mdoc.1.attestation.keyattest");

		JsonObject mDL = supportedCredentialConfigurations.getAsJsonObject("org.iso.18013.5.1.mDL");
		mDL.addProperty("scope", "org.iso.18013.5.1.mDL");

		JsonObject mDLAttestation = supportedCredentialConfigurations.getAsJsonObject("org.iso.18013.5.1.mDL.attestation");
		mDLAttestation.addProperty("scope", "org.iso.18013.5.1.mDL.attestation");

		return supportedCredentialConfigurations;
	}

	@Override
	protected void injectCredentialConfigurationDetailsIntoRequestContextForPreAuthorizedCodeFlow() {
		super.injectCredentialConfigurationDetailsIntoRequestContextForPreAuthorizedCodeFlow();
		if (authorizationRequestType == AuthorizationRequestType.SIMPLE) {
			callAndStopOnFailure(VCIInjectRequestScopePreAuthorizedCodeFlow.class, Condition.ConditionResult.FAILURE);
		}
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
