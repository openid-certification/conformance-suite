package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ob-ensure-wrong-client-id-in-token-endpoint-fails-with-private-key-and-matls",
	displayName = "FAPI-OB: ensure client id in token endpoint fails (with private key authentication and MATLS)",
	summary = "This test should end with the token endpoint returning an error message that the client is invalid.",
	profile = "FAPI-OB",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.resourceUrl",
		"resource.resourceUrlAccountRequests",
		"resource.resourceUrlAccountsResource",
		"resource.institution_id"
	}
)
public class FAPIOBEnsureClientIdInTokenEndpointWithPrivateKeyAndMATLS extends AbstractFAPIOBServerTestModule {

	@Override
	protected void createClientCredentialsRequest() {

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
	}

	@Override
	protected void requestClientCredentialsGrant() {

		createClientCredentialsRequest();

		// Switch to client 2 client
		eventLog.startBlock("Swapping to Client2");
		env.mapKey("client", "client2");

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class, "FAPI-R-5.2.2-19");

		callAndContinueOnFailure(CallTokenEndpoint.class);

		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		callAndContinueOnFailure(CallTokenEndpointExpectingError.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-19");

	}

	@Override
	protected void performPreAuthorizationSteps() {

		// This flow need to focus to check the error of token endpoint flow
		requestClientCredentialsGrant();

	}

	@Override
	protected void performAuthorizationFlow() {

		// This flow need to focus to check the error of token endpoint flow
		performPreAuthorizationSteps();

		fireTestFinished();

	}

	@Override
	protected void createAuthorizationCodeRequest() {
		//Nothings, because it is not quite necessary
	}
}
