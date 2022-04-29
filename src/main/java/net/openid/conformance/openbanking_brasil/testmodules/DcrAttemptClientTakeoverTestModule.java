package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWith2ndClientFull;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "dcr-test-attempt-client-takeover",
	displayName = "Resources API DCR test: attempt to take over client",
	summary = "Obtains a software statement from the Brazil directory (using a client hardcoded into the test suite), registers a new client on the target authorization server then, using valid keys/SSA/etc from a different valid client, attempts to take over the original client. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.\n" +
		"\u2022 Retrieves from the directory its SSA\n" +
		"\u2022 Performs a DCR on the provided authorization server -> Expects a success \n" +
		"\u2022 Performs a PUT on the registration endpoint with the same configuration -> Expects a success\n" +
		"\u2022 Changes the certificates used to the second set of certificates that belong to a client from a different organization\n" +
		"\u2022 Attempts 'GET' on client configuration endpoint using MTLS certificate for the second client -> Expects Failure\n" +
		"\u2022 Using the second client, retrieves from the directory its SSA\n" +
		"\u2022 Performs a PUT on the registration endpoint with the configuration from the second client -> Expects a failure\n" +
		"\u2022 DELETEs the first registered client from the authorization server",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.client_id",
		"directory2.client_id",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
	}
)
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment",
	"resource.consentUrl",
	"resource.brazilCpf",
	"resource.brazilCnpj"
})
public class DcrAttemptClientTakeoverTestModule extends AbstractFAPI1AdvancedFinalBrazilDCR {


	@Override
	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		eventLog.endBlock();

		eventLog.startBlock("Make PUT request to client configuration endpoint with no changes expecting success");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");

		eventLog.startBlock("Attempt 'GET' on client configuration endpoint using MTLS certificate for different software, which must fail");

		callAndStopOnFailure(OverrideClientWith2ndClientFull.class);

		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

		// https://github.com/OpenBanking-Brasil/specs-seguranca/issues/199

		env.removeObject("registration_client_endpoint_request_body"); // so a 'GET' is made
		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");
		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.2");
		call(exec().unmapKey("endpoint_response"));

		getSsa();
		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Calling PUT on configuration endpoint with SSA from another client");

		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(FAPIBrazilExtractJwksUriFromSoftwareStatement.class, "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class, "RFC7591-2", "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		env.unmapKey("client");
		env.unmapKey("mtls");
		deleteClient();
	}

	@Override
	public void start() {
		fireTestFinished();
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

}
