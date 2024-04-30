package net.openid.conformance.openid;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess;
import net.openid.conformance.condition.client.AddRefreshTokenGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.EnsureRefreshTokenContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.EnsureServerConfigurationDoesNotSupportRefreshToken;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsRefreshToken;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdOfflineAccess;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdOfflineAccessIfServerSupportsOfflineAccess;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.RefreshTokenRequestExpectingErrorSteps;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-refresh-token",
	displayName = "OIDCC: test refresh token behaviours",
	summary = "This tests obtains refresh tokens and performs various checks, including checking that the refresh token works and is correctly bound to the client. Support for refresh tokens is optional and the test will be skipped if the token endpoint response does not return refresh tokens. scope=offline_access will be included in the authorization endpoint request if the server's discovery document indicates support for this scope.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public class OIDCCRefreshToken extends AbstractOIDCCMultipleClient {

	@Override
	protected void completeClientConfiguration() {
		if (serverSupportsDiscovery()) {
			callAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class);
			callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdOfflineAccessIfServerSupportsOfflineAccess.class);
		} else {
			// no discovery, so no idea if server supports offline_access scope or not - request it anyway, servers
			// 'should' ignore unknown scope values
			callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdOfflineAccess.class);
		}

		if (profileCompleteClientConfiguration != null) {
			call(sequence(profileCompleteClientConfiguration));
		}
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddRefreshTokenGrantTypeToDynamicRegistrationRequest.class);
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		Command cmd = new Command();

		if (isSecondClient()) {
			cmd.putInteger("requested_nonce_length", 43);
		}
		else {
			cmd.removeNativeValue("requested_nonce_length");
		}

		return super.createAuthorizationRequestSequence()
			.insertBefore(CreateRandomNonceValue.class, cmd)
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

		sendRefreshTokenRequestAndCheckIdTokenClaims();

		requestProtectedResource();

		onPostAuthorizationFlowComplete();
	}

	protected void sendRefreshTokenRequestAndCheckIdTokenClaims() {
		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);
		//stop if no refresh token is returned
		if(Strings.isNullOrEmpty(env.getString("refresh_token"))) {
			callAndContinueOnFailure(EnsureServerConfigurationDoesNotSupportRefreshToken.class, Condition.ConditionResult.WARNING, "OIDCD-3");
			// This throws an exception: the test will stop here
			fireTestSkipped("Refresh tokens cannot be tested. No refresh token was issued.");
		}
		if (serverSupportsDiscovery()) {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsRefreshToken.class, Condition.ConditionResult.WARNING, "OIDCD-3");
		}
		callAndContinueOnFailure(EnsureRefreshTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.17");
		call(new RefreshTokenRequestSteps(isSecondClient(), addTokenEndpointClientAuthentication));
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!isSecondClient()) {
			//remove refresh token from 1st client
			env.removeNativeValue("refresh_token");

			// Remove token mappings
			// (This must be done before restarting the authorization flow, because
			// handleSuccessfulAuthorizationEndpointResponse extracts an id token)
			env.unmapKey("access_token");
			env.unmapKey("id_token");
		}

		super.onPostAuthorizationFlowComplete();
	}

	@Override
	protected void performSecondClientTests() {
		// try client 2's refresh_token with client 1
		unmapClient();
		eventLog.startBlock("Attempting to use refresh_token issued to client 2 with client 1");
		call(new RefreshTokenRequestExpectingErrorSteps(isSecondClient(), addTokenEndpointClientAuthentication));
		eventLog.endBlock();
	}
}
