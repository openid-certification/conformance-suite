package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddOpenIdPaymentsScopeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddScopeOpenIdPaymentsToClientConfigurationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationUriFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidScope;
import net.openid.conformance.condition.client.CheckNoClientIdFromDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckNoErrorFromDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ClientManagementEndpointAndAccessTokenRequired;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.DetectIfHttpStatusIsSuccessOrFailure;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.ExtractClientManagementCredentials;
import net.openid.conformance.condition.client.ExtractDynamicRegistrationResponse;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;

import static net.openid.conformance.condition.client.DetectIfHttpStatusIsSuccessOrFailure.endpointResponseWas2xx;

@PublishTestModule(
	testName = "payments-api-dcr-test-unauthorized-client",
	displayName = "Payments API Attempt to use payments with unauthorized client",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the DADOS role), verify (in several different ways) that it is not possible to obtain a client with the 'payments' scope granted, and that a client credentials grant requesting the 'payments' scope fails with an 'invalid_scope' error. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl"
	}
)
public class PaymentsApiDcrTestModuleUnauthorizedClient extends AbstractFAPI1AdvancedFinalBrazilDCR {
	boolean didRegistrationWithScopePayments = false;

	// This can be uncommented to test the failure path through the test against the mock bank
//	@Override
//	protected void setupJwksUri() {
//		if (!didRegistrationWithScopePayments) {
//			callAndStopOnFailure(CreateJwksUri.class);
//		} else {
//			super.setupJwksUri();
//		}
//	}

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDadosClient.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
		callAndStopOnFailure(SetDirectoryInfo.class);

		if (!didRegistrationWithScopePayments) {
			// first we try explicitly asking for payments scope; regardless of outcome we won't have a registered
			// client by the time this returns

			super.configureClient();

			didRegistrationWithScopePayments = true;
		}
		super.configureClient();
	}

	@Override
	protected void callRegistrationEndpoint() {
		if (!didRegistrationWithScopePayments) {
			callAndStopOnFailure(AddOpenIdPaymentsScopeToDynamicRegistrationRequest.class, "RFC7591-2");

			// this call might not result in a client registering (bank may reject as invalid scope), so we can't call super which expects success
			callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");

			call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");

			callAndStopOnFailure(DetectIfHttpStatusIsSuccessOrFailure.class);

			if (env.getBoolean(endpointResponseWas2xx)) {
				eventLog.startBlock("Requesting scope=payments for a DADOS client resulted in a client being created. Verify the response and that the server did not grant the payments scope.");
				callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
				callAndContinueOnFailure(CheckNoErrorFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
				callAndStopOnFailure(ExtractDynamicRegistrationResponse.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
				callAndContinueOnFailure(ExtractClientManagementCredentials.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
				callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");

				validateDcrResponseScope();

				// delete the client; this means the test goes on to try creating a new client without requesting a scope
				deleteClient();
			} else {
				eventLog.startBlock("Requesting scope=payments for a DADOS client resulted in an error. Verify the error response.");
				callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
				callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
				callAndContinueOnFailure(CheckNoClientIdFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
			}

			call(exec().unmapKey("endpoint_response"));

			return;
		}

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

		// This can be uncommented to test the failure path through the test against the mock bank
//		JsonObject request = env.getObject("registration_client_endpoint_request_body");
//		request.addProperty("jwks_uri", "https://foo/jwks");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));
		callAndStopOnFailure(DetectIfHttpStatusIsSuccessOrFailure.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");

		if (env.getBoolean(endpointResponseWas2xx)) {
			eventLog.startBlock("Trying to add payments scope to DADOS client returned success; verifying payments scope was not granted");
			callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
			callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
			callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
			callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
			callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
			env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
			callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments.class, Condition.ConditionResult.FAILURE);
			call(exec().unmapKey("endpoint_response"));
		} else {
			eventLog.startBlock("Trying to add payments scope to DADOS client returned failure; verifying error response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
			env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
			callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
			call(exec().unmapKey("dynamic_registration_endpoint_response"));

		}
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

		deleteClient();

		fireTestFinished();
	}

}
