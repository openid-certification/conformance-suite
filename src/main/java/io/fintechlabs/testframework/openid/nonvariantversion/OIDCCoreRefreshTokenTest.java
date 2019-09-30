package io.fintechlabs.testframework.openid.nonvariantversion;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.CheckTokenTypeIsBearer;
import io.fintechlabs.testframework.condition.client.CompareIdTokenClaims;
import io.fintechlabs.testframework.condition.client.CreateRefreshTokenRequest;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenContainsAllowedCharactersOnly;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenValuesAreDifferent;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumRefreshTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumRefreshTokenLength;
import io.fintechlabs.testframework.condition.client.EnsureRefreshTokenContainsAllowedCharactersOnly;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractRefreshTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.WaitForOneSecond;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "oidccore-refresh-token",
	displayName = "OIDCCore: obtain an id token using a refresh token",
	summary = "This test uses a refresh_token to obtain an id token and ensures that claims satisfy the requirements.",
	profile = "OIDCCore",
	configurationFields = {
		"issuer",
		"response_type",
		"response_mode",
		"authn_request.type",	//one of plain, signed, encrypted. defaults to plain. incomplete, only plain will work
		"authn_request.signing_alg",
		"authn_request.enc_alg",
		"tls.ignore_cert_errors",
		"server.discovery_type",	//allowed values are dynamic and static, defaults to dynamic
		"server.discovery_url",
		"server.static_server_configuration",	//same structure as .well-known/openid-configuration
		"client1.client_config_type",	//static or dynamic
		"client1.client_type",	//public or confidential
		"client1.client_id",
		"client1.scope",
		"client1.authn_type",
		"client1.client_secret",
		"client1.jwks",
		"client2.client_config_type",	//static or dynamic
		"client2.client_type",	//public or confidential
		"client2.client_id",
		"client2.scope",
		"client2.authn_type",
		"client2.client_secret",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
public class OIDCCoreRefreshTokenTest extends AbstractOIDCCoreServerTestNonVariant
{
	/**
	 * variants are not actually used
	 * added to circumvent the missing variant error
	 * always select client_secret_jwt variant from the dropdown
	 */
	public static final String variant_client_secret_jwt = "client_secret_jwt";

	@Variant(
		name = variant_client_secret_jwt,
		configurationFields =  {
		})
	public void setupClientSecretJwt() {

	}

	@Override
	protected void onBeforeCreateAuthorizationRedirect()
	{
		callAndStopOnFailure(AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess.class);
	}


	@Override
	protected void onBeforeRequestAuthorizationCode()
	{
		/*
		 * Instead of using map-unmap keys in environment,
		 * just add-remove items
		 */
		// Store the original access token and ID token separately (see RefreshTokenRequestSteps)
		if(responseType.includesToken()) {
			env.putObject("first_access_token", env.getObject("access_token"));
		}
		if(responseType.includesIdToken()) {
			env.putObject("first_id_token", env.getObject("id_token"));
		}
	}

	@Override
	protected void onAfterRequestAuthorizationCode() {
		if(!responseType.includesToken()) {
			env.putObject("first_access_token", env.getObject("access_token"));
		}
		if(!responseType.includesIdToken()) {
			env.putObject("first_id_token", env.getObject("id_token"));
		}

		sendRefreshTokenRequestAndCheckIdTokenClaims();

	}


	protected void sendRefreshTokenRequestAndCheckIdTokenClaims() {
		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);
		//stop if no refresh token is returned
		if(Strings.isNullOrEmpty(env.getString("refresh_token"))) {
			// This throws an exception: the test will stop here
			fireTestSkipped("Refresh tokens cannot be tested. No refresh token was issued.");
		}
		callAndContinueOnFailure(EnsureRefreshTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.17");

		call(exec().startBlock(currentClientString() + "Refresh Token Request"));
		callAndStopOnFailure(CreateRefreshTokenRequest.class);
		Class<? extends ConditionSequence> authSequence = currentClient.getClientAuthenticationSequence(env);
		if(authSequence!=null) {
			call(sequence(authSequence));
		}

		//wait 1 second to make sure that iat values will be different
		callAndStopOnFailure(WaitForOneSecond.class);

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
		env.putObject("second_access_token", env.getObject("access_token"));

		callAndContinueOnFailure(CheckTokenTypeIsBearer.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.2-1");
		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");
		callAndContinueOnFailure(EnsureAccessTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.12");
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		call(condition(ValidateExpiresIn.class)
			.skipIfObjectMissing("expires_in")
			.requirement("RFC6749-5.1")
			.dontStopOnFailure());

		callAndContinueOnFailure(EnsureAccessTokenValuesAreDifferent.class);

		callAndContinueOnFailure(ExtractIdTokenFromTokenResponse.class);
		env.putObject("second_id_token", env.getObject("id_token"));

		// It's perfectly legal to NOT return a new refresh token; if the server didn't then
		// 'refresh_token' in the environment will be left containing the old (still valid)
		// token. We use that token later to test the refresh token is bound to the client
		// correctly.
		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);

		call(condition(EnsureMinimumRefreshTokenLength.class)
			.skipIfElementMissing("token_endpoint_response", "refresh_token")
			.requirement("RFC6749-10.10")
			.dontStopOnFailure());

		call(condition(EnsureMinimumRefreshTokenEntropy.class)
			.skipIfElementMissing("token_endpoint_response", "refresh_token")
			.requirement("RFC6749-10.10")
			.dontStopOnFailure());

		//compare only when refresh response contains an id_token
		call(condition(CompareIdTokenClaims.class)
			.skipIfObjectMissing("second_id_token")
			.requirement("OIDCC-12.2")
			.dontStopOnFailure());

		call(exec().endBlock());
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
		currentClient = client1;
		currentClient.addClientToEnvironment(env);
		eventLog.startBlock(currentClientString() + "Attempting to use refresh_token issued to client 2 with client 1");
		refreshTokenRequestExpectingError();
		eventLog.endBlock();
		fireTestFinished();
	}

	@Override
	protected void onBeforeSwitchingToSecondClient()
	{
		env.removeObject("first_id_token");
		env.removeObject("second_id_token");
		env.removeObject("first_access_token");
		env.removeObject("second_access_token");
	}

	protected void refreshTokenRequestExpectingError() {
		callAndStopOnFailure(CreateRefreshTokenRequest.class);
		Class<? extends ConditionSequence> authSequence = currentClient.getClientAuthenticationSequence(env);
		if(authSequence!=null)
		{
			call(sequence(authSequence));
		}

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
