package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddOpenIdPaymentsScopeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddScopeOpenIdPaymentsToClientConfigurationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationUriFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidScope;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-dcr-test-unauthorized-client",
	displayName = "Payments API Attempt to use payments with unauthorized client",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the DADOS role), register a new client on the target authorization server (without requesting any scopes) - verify that the scope returned in the DCR response does not contain 'payments', and that a client credentials grant requesting the 'payments' scope fails with an 'invalid_scope' error. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl"
	}
)
public class PaymentsApiDcrTestModuleUnauthorizedClient extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDadosClient.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		super.configureClient();
	}

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(AddOpenIdPaymentsScopeToDynamicRegistrationRequest.class, "RFC7591-2");

		super.callRegistrationEndpoint();
	}


	@Override
	protected void validateDcrResponseScope() {
		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments.class, Condition.ConditionResult.FAILURE);
	}

	@Override
	protected void performAuthorizationFlow() {
		eventLog.startBlock("Make PUT request to client configuration endpoint with no changes expecting success");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");

		eventLog.startBlock("Calling PUT on configuration endpoint to try and add payments scope, expecting a successful response with the scope not including payments");
		// get a new SSA (there is already one, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		callAndStopOnFailure(AddScopeOpenIdPaymentsToClientConfigurationRequest.class);
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		eventLog.startBlock("Try using client credentials grant to obtain an access token with scope=payments, expecting failure");

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
		call(sequence(addTokenEndpointClientAuthentication));
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidScope.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");

		eventLog.startBlock("Deregister client");

		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClient.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		// we just deregistered the client, so prevent cleanup from trying to do so again
		env.removeNativeValue("registration_client_uri");
		env.removeNativeValue("registration_access_token");

		fireTestFinished();
	}
}
