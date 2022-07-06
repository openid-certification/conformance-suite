package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.AddTlsClientAuthSubjectDnToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationUriFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilExtractClientMTLSCertificateSubject;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDCMSubjectDnTestClient2;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.client_id",
	"client.org_jwks"
})
public abstract class AbstractDcmSubjectDnTestModule extends AbstractApiDcrTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		ClientAuthType clientAuthType = getVariant(ClientAuthType.class);
		if (clientAuthType != ClientAuthType.MTLS) {
			throw new TestFailureException(getId(), "This test is only applicable when using MTLS client authentication.");
		}
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock("Switch to certificate with different subjectdn, verify that client credentials grant fails");
		env.putObject("altconfig", env.getObject("config").deepCopy());
		env.mapKey("config", "altconfig");
		env.mapKey("mutual_tls_authentication", "altmtls");
		callAndStopOnFailure(OverrideClientWithDCMSubjectDnTestClient2.class);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

		callClientCredentialsGrantExpectingFailure();

		eventLog.startBlock("Make PUT request to client configuration endpoint with subjectdn for second certificate");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);
		callAndStopOnFailure(AddTlsClientAuthSubjectDnToClientConfigurationRequest.class);

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");

		performPreAuthorizationSteps();

		eventLog.startBlock("Switch back to original certificate, verify that client credentials grant now fails");
		env.unmapKey("config");
		env.unmapKey("mutual_tls_authentication");
		callAndStopOnFailure(OverrideClientWithDCMSubjectDnTestClient2.class);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

		callClientCredentialsGrantExpectingFailure();

		fireTestFinished();
	}

	private void callClientCredentialsGrantExpectingFailure() {
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		if (brazilPayments.isTrue()) {
			callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
		} else {
			callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
		}

		call(sequence(addTokenEndpointClientAuthentication));

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-19");
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC7636-4.6", "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
