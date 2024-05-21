package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRefreshTokenGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateJwksUri;
import net.openid.conformance.condition.client.EnsureRefreshTokenContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsRefreshToken;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.condition.client.GenerateRS256ClientJWKs;
import net.openid.conformance.condition.client.GenerateRS256ClientJWKsWithKeyID;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdOfflineAccess;
import net.openid.conformance.condition.client.WaitForJWKSRefreshDelay;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@PublishTestModule(
	testName = "oidcc-refresh-token-rp-key-rotation",
	displayName = "OIDCC: Use a refresh token with private_key_jwt client authentication to ensure the server can handle RP key rotation",
	summary = "This tests obtains a refresh token (by registering the client for the refresh_token grant and including scope=offline_access in the authorization endpoint request). Once it has obtained the refresh token it rotates the keys (by placing a new RP key with a new kid into the RP's jwks_uri), waits 60 seconds then tries to use the refresh token with a client assertion containing the new kid. Support for private_key_jwt client authentication, scope=offline_access and refresh tokens are not a requirement of the specification but are required to certify for the 'dynamic' profile.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
@VariantNotApplicable(parameter = ClientRegistration.class, values={"static_client"})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt", "mtls"
}) // this test relies on sending a client assertion containing the new kid so private_key_jwt is required
// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Rotation_RP_Sig
public class OIDCCRefreshTokenRPKeyRotation extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		callAndStopOnFailure(CreateJwksUri.class);
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType)
			.replace(GenerateRS256ClientJWKs.class,
				condition(GenerateRS256ClientJWKsWithKeyID.class))
			.replace(AddPublicJwksToDynamicRegistrationRequest.class,
				condition(AddJwksUriToDynamicRegistrationRequest.class)));
		callAndStopOnFailure(AddRefreshTokenGrantTypeToDynamicRegistrationRequest.class);

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("client1_jwks")) {
			return handleJwksRequest();
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	private Object handleJwksRequest() {
		JsonObject clientPublicJwks = env.getObject("client_public_jwks");
		if (clientPublicJwks == null) {
			throw new TestFailureException(getId(), "jwks endpoint called before key exists - please wait for test to initialise before calling client jwks endpoint");
		}
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.body(clientPublicJwks);
	}

	@Override
	protected void completeClientConfiguration() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdOfflineAccess.class);

		if (profileCompleteClientConfiguration != null) {
			call(sequence(profileCompleteClientConfiguration));
		}
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess.class).requirement("OIDCC-11"));
	}

	@Override
	protected void performPostAuthorizationFlow() {
		createAuthorizationCodeRequest();

		// Store the original access token and ID token separately (see RefreshTokenRequestSteps)
		env.mapKey("access_token", "first_access_token");
		env.mapKey("id_token", "first_id_token");

		requestAuthorizationCode();

		// Set up the mappings for the refreshed access and ID tokens
		env.mapKey("access_token", "second_access_token");
		env.mapKey("id_token", "second_id_token");

		eventLog.startBlock("Cycling keys in RP jwks_uri");
		callAndStopOnFailure(GenerateRS256ClientJWKsWithKeyID.class);
		eventLog.endBlock();

		eventLog.startBlock("Waiting, so that any DoS limits on retrieving the jwks_uri too often are not triggered");
		// the python test does not have this wait, which causes the python test to fail against node oidc provider and Authlete
		// as it hits limits on how frequently jwks_uri is retrieved - the sleep avoids this.
		//
		// The sleep may be decreased via the 'jwks_refresh_delay' server configuration property.
		callAndStopOnFailure(WaitForJWKSRefreshDelay.class);
		eventLog.endBlock();

		sendRefreshTokenRequestAndCheckIdTokenClaims();

		requestProtectedResource();

		onPostAuthorizationFlowComplete();
	}

	protected void sendRefreshTokenRequestAndCheckIdTokenClaims() {
		callAndStopOnFailure(ExtractRefreshTokenFromTokenResponse.class);
		callAndContinueOnFailure(EnsureServerConfigurationSupportsRefreshToken.class, Condition.ConditionResult.WARNING, "OIDCD-3");
		callAndContinueOnFailure(EnsureRefreshTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.17");
		call(new RefreshTokenRequestSteps(false, addTokenEndpointClientAuthentication));
	}

}
