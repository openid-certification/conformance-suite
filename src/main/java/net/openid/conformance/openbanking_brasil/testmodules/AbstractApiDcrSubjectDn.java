package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsurePaymentDateIsToday;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideCNPJ;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDadosClientThatHasClientSpecificJwks;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithPagtoClientThatHasClientSpecificJwks;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithAllDadosScopes;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;

import static net.openid.conformance.condition.client.DetectIfHttpStatusIsSuccessOrFailure.endpointResponseWas2xx;

public abstract class AbstractApiDcrSubjectDn extends AbstractApiDcrTestModule {
    boolean useBrazilShortNames = true;
    boolean registrationFailed = false;

    abstract boolean isPaymentsApiTest();

    @Override
    protected void configureClient() {
        if (isPaymentsApiTest()) {
            brazilPayments = Troolean.IS;
            callAndStopOnFailure(EnsurePaymentDateIsToday.class);
            callAndStopOnFailure(OverrideClientWithPagtoClient.class);
            callAndStopOnFailure(OverrideCNPJ.class);
            callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
        } else {
			brazilPayments = Troolean.ISNT;
            callAndStopOnFailure(OverrideClientWithDadosClient.class);
            callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
        }
        callAndStopOnFailure(SetDirectoryInfo.class);
        callAndStopOnFailure(GetResourceEndpointConfiguration.class);
        super.configureClient();

        if (!registrationFailed) {
            performPreAuthorizationSteps();

            deleteClient();
        }

        // again but with non-RFC OIDs in numeric form
        useBrazilShortNames = false;
        super.configureClient();

        performPreAuthorizationSteps();

        eventLog.startBlock("Trying using a different client's MTLS certificate with issued access token");

        env.putObject("altconfig", env.getObject("config").deepCopy());
        env.mapKey("config", "altconfig");
        env.mapKey("mutual_tls_authentication", "altmtls");
        if (isPaymentsApiTest()) {
            callAndStopOnFailure(OverrideClientWithPagtoClientThatHasClientSpecificJwks.class);
            callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
        } else {
            callAndStopOnFailure(OverrideClientWithDadosClientThatHasClientSpecificJwks.class);
            callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
        }
        callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

        if (isPaymentsApiTest()) {
            call(exec().mapKey("request_object_claims", "consent_endpoint_request"));
            callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
            callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
            call(exec().unmapKey("request_object_claims"));
            callAndStopOnFailure(FAPIBrazilSignPaymentConsentRequest.class);
            callAndStopOnFailure(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class);
        } else {
            callAndStopOnFailure(CallConsentEndpointWithBearerToken.class);
        }
		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));

        callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE);

        env.unmapKey("config");

        eventLog.startBlock("Trying using a different client's MTLS certificate to authenticate at token endpoint");

        callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
        if (isPaymentsApiTest()) {
            callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
        } else {
            callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
        }
        call(sequence(addTokenEndpointClientAuthentication));
        callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
        callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE);
        callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, Condition.ConditionResult.FAILURE);
        callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE);

        env.unmapKey("mutual_tls_authentication");

        eventLog.startBlock("Delete client");

        deleteClient();

    }

    @Override
    protected void callRegistrationEndpoint() {
        if (useBrazilShortNames) {
            // this form only generates a warning on failure
            callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");

            call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

            callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE,"OIDCR-3.2");
            callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING,"OIDCR-3.2");
            callAndStopOnFailure(DetectIfHttpStatusIsSuccessOrFailure.class);
            if (env.getBoolean(endpointResponseWas2xx)) {
                // this is all lifted out of 'super'
                callAndContinueOnFailure(CheckNoErrorFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
                callAndStopOnFailure(ExtractDynamicRegistrationResponse.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
                callAndContinueOnFailure(VerifyClientManagementCredentials.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
                callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
                validateDcrResponseScope();
                // The tests expect scope to be part of the 'client' object, but it may not be in the dcr response so copy across
                callAndStopOnFailure(CopyScopeFromDynamicRegistrationTemplateToClientConfiguration.class);
                callAndStopOnFailure(CopyOrgJwksFromDynamicRegistrationTemplateToClientConfiguration.class);
            } else {
                registrationFailed = true; // don't try to use/deregister this client
                callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
                callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
                callAndContinueOnFailure(CheckNoClientIdFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
            }

            call(exec().unmapKey("endpoint_response"));
        } else {
            super.callRegistrationEndpoint();
        }
    }

    @Override
    protected void addTlsClientAuthSubjectDn() {
        if (useBrazilShortNames) {
            callAndStopOnFailure(AddTlsClientAuthSubjectDnWithBrazilShortnameToDynamicRegistrationRequest.class);
            // uncomment this to test the 'failure' flow against the mock bank
            //callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class);
        } else {
            super.addTlsClientAuthSubjectDn();
        }
    }

    @Override
    public void start() {
        fireTestFinished();
    }
}
