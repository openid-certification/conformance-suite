package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;

import static net.openid.conformance.condition.client.DetectIfHttpStatusIsSuccessOrFailure.endpointResponseWas2xx;

public abstract class AbstractApiDcrTestModuleUnauthorizedClient extends AbstractApiDcrTestModule {
    boolean didRegistrationWithExplicitScopeRequested = false;

    abstract boolean isPaymentsApiTest();

	// This can be uncommented to test the failure path through the test against the mock bank
	// (There are two possible 'pass' routes through the test - one where DCR for accounts registers a client which does
	// not have the explictly requested scope (the option the mock bank takes), and one where the DCR request is
	// rejected and does not create a client. The latter can be simulated on the mock bank by adding an invalid jwks uri
	// to the request.)
//	@Override
//	protected void setupJwksUri() {
//		if (!didRegistrationWithExplicitScopeRequested) {
//			callAndStopOnFailure(CreateJwksUri.class);
//		} else {
//			super.setupJwksUri();
//		}
//	}

	@Override
    protected void configureClient() {
		if (isPaymentsApiTest()) {
			callAndStopOnFailure(OverrideClientWithDadosClient.class);
			callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
		} else {
			callAndStopOnFailure(OverrideClientWithPagtoClient.class);
			callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
		}
        callAndStopOnFailure(SetDirectoryInfo.class);

        if (!didRegistrationWithExplicitScopeRequested) {
            // first we try explicitly asking for payments scope; regardless of outcome we won't have a registered
            // client by the time this returns

            super.configureClient();

            didRegistrationWithExplicitScopeRequested = true;
        }
        super.configureClient();
    }

    @Override
    protected void callRegistrationEndpoint() {
        if (!didRegistrationWithExplicitScopeRequested) {
        	if (isPaymentsApiTest()) {
				callAndStopOnFailure(AddOpenIdPaymentsScopeToDynamicRegistrationRequest.class, "RFC7591-2");
			} else {
				callAndStopOnFailure(AddOpenIdResourcesScopeToDynamicRegistrationRequest.class, "RFC7591-2");
			}

            // this call might not result in a client registering (bank may reject as invalid scope), so we can't call super which expects success
            callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");

            call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

            callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");

            callAndStopOnFailure(DetectIfHttpStatusIsSuccessOrFailure.class);

            if (env.getBoolean(endpointResponseWas2xx)) {
            	if (isPaymentsApiTest()) {
					eventLog.startBlock("Requesting scope=payments for a DADOS client resulted in a client being created. Verify the response and that the server did not grant the payments scope.");
				} else {
					eventLog.startBlock("Requesting scope=resources for a PAGTO client resulted in a client being created. Verify the response and that the server did not grant the resources scope.");
				}
                callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
                callAndContinueOnFailure(CheckNoErrorFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
                callAndStopOnFailure(ExtractDynamicRegistrationResponse.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
				// TODO figure out how to cope with that being deleted
				// suspect it was replaced by the condition I use below but need to check
//                callAndContinueOnFailure(ExtractClientManagementCredentials.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
                callAndContinueOnFailure(VerifyClientManagementCredentials.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
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
		if (isPaymentsApiTest()) {
			callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments.class, Condition.ConditionResult.FAILURE);
		} else {
			callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainResources.class, Condition.ConditionResult.FAILURE);
		}
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

        if (isPaymentsApiTest()) {
			callAndStopOnFailure(AddScopeOpenIdPaymentsToClientConfigurationRequest.class);
		} else {
			callAndStopOnFailure(AddScopeOpenIdResourcesToClientConfigurationRequest.class);
		}

        // This can be uncommented to test the failure path through the test against the mock bank
//		JsonObject request = env.getObject("registration_client_endpoint_request_body");
//		request.addProperty("jwks_uri", "https://foo/jwks");

        callAndStopOnFailure(CallClientConfigurationEndpoint.class);

        call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));
        callAndStopOnFailure(DetectIfHttpStatusIsSuccessOrFailure.class);
        callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");

        if (env.getBoolean(endpointResponseWas2xx)) {
        	if (isPaymentsApiTest()) {
				eventLog.startBlock("Trying to add payments scope to DADOS client returned success; verifying payments scope was not granted");
			} else {
				eventLog.startBlock("Trying to add resources scope to PAGTO client returned success; verifying resources scope was not granted");
			}
            callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
            callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
            callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
            callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
            callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
            env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
            if (isPaymentsApiTest()) {
				callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainPayments.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoesNotContainResources.class, Condition.ConditionResult.FAILURE);
			}
            call(exec().unmapKey("endpoint_response"));
        } else {
			if (isPaymentsApiTest()) {
				eventLog.startBlock("Trying to add payments scope to DADOS client returned failure; verifying error response");
			} else {
				eventLog.startBlock("Trying to add resources scope to PAGTO client returned failure; verifying error response");
			}
            callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
            env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
            callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
            call(exec().unmapKey("dynamic_registration_endpoint_response"));

        }
        call(exec().unmapKey("endpoint_response"));

		if (isPaymentsApiTest()) {
			eventLog.startBlock("Try using client credentials grant to obtain an access token with scope=payments, expecting failure");
		} else {
			eventLog.startBlock("Try using client credentials grant to obtain an access token with scope=resources, expecting failure");
		}

        callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		if (isPaymentsApiTest()) {
			callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
		} else {
			callAndStopOnFailure(SetResourcesScopeOnTokenEndpointRequest.class);
		}
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
