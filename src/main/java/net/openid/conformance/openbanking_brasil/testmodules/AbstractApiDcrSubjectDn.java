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
    boolean registrationFailed = false;

    protected abstract boolean isPaymentsApiTest();

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
    public void start() {
        fireTestFinished();
    }
}
