package net.openid.conformance.openbanking_brasil.testmodules.dcr;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractApiDcrTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;


@PublishTestModule(
	testName = "dcr-brcac2022-support",
	displayName = "This test will make sure that the tested server supports both the old and the new format of BRCACs specified by the Open Banking Brasil Certificate Standard Document",
	summary = "This tests will make sure that the tested server supports both the old and the new format of BRCACs specified by the Open Banking Brasil Certificate Standard Document\n" +
		"Test Behaviour:\n" +
		"\u2022 Perform a DCR against the target Server using old style BRCAC\n" +
		"\u2022 Expect a success 201 - First client_id (1) created for this set of credentials\n" +
		"\u2022 Request an access token with the first client_id (1) using the client_credentials grant asking for both payments and consents scope - Expect a success\n" +
		"\u2022 Using the new style BRCAC call the GET Registration API for the first client_id (1) - Expect a success\n" +
		"\u2022 Using the new style BRCAC obtain an SSA from the participant directory\n" +
		"\u2022 Using the new style BRCAC call the PUT Registration API for the first client_id (1) with a matching request body - Expect a success\n" +
		"\u2022 Perform a DCR against the target Server using the new style BRCAC\n" +
		"\u2022 Expect a success 201 - Second client_id (2) created\n" +
		"\u2022 Request an access token with the second client_id (2) using the client_credentials grant asking for both payments and consents scope - Expect a success\n" +
		"\u2022 Unregister both created clients using the new style BRCAC",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.brazilPaymentConsent",
		"resource.brazilOrganizationId"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilPixPayment",
	"resource.brazilCpf",
	"resource.brazilCnpj",
	"directory.client_id"
})
public class DcrBrcac2022SupportTestModule extends AbstractApiDcrTestModule {

	private boolean isOldBrcacClient = true;
	private ClientAuthType clientAuthType;

	@Override
	protected void configureClient() {
		clientAuthType = getVariant(ClientAuthType.class);
		callAndStopOnFailure(OverrideClientWithOldBrcacClient.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdPaymentsConsents.class);
		super.configureClient();
	}

	@Override
	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}

	@Override
	protected void requestProtectedResource() {
		// Not needed for this test since all requests are sent to the registration endpoint.
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if(isOldBrcacClient){
			isOldBrcacClient = false;
			eventLog.startBlock("Retrieve client configuration using new BRCAC");

			callAndStopOnFailure(OverrideClientWithNewBrcacClient.class);
			callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

			callClientConfigurationEndpoint();

			eventLog.endBlock();
			eventLog.startBlock("Make PUT request to the client configuration using new BRCAC");

			createClientConfigurationRequestWithSubjectDn();

			callClientConfigurationEndpoint();

			deleteClient();

			super.configureClient();

			performAuthorizationFlow();
		}else {
			deleteClient();
			fireTestFinished();
		}

	}

	protected void createClientConfigurationRequestWithSubjectDn() {
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		if (clientAuthType == ClientAuthType.MTLS) {
			callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);
			callAndStopOnFailure(AddTlsClientAuthSubjectDnToDynamicRegistrationRequest.class);
		}
	}

	protected void callClientConfigurationEndpoint() {

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
	}

}
