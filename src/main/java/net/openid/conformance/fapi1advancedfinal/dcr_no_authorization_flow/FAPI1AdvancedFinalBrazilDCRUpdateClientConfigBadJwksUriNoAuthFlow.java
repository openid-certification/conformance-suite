package net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUri;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-update-client-config-bad-jwks-uri-no-authorization-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR update client config",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration).\n" +
		"\u2022 Register a new client on the target authorization server.\n" +
		"\u2022 The test will then use a PUT to try and change the jwks_uri to an invalid one, the server must return an 'invalid_client_metadata' error.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	}
)
public class FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUriNoAuthFlow
	extends FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUri {

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		eventLog.endBlock();

		eventLog.startBlock("Make PUT request to client configuration endpoint to change jwks uri to non-directory hosted one");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		callAndStopOnFailure(CreateJwksUri.class);
		env.mapKey("dynamic_registration_request", "registration_client_endpoint_request_body");
		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class);
		env.unmapKey("dynamic_registration_request");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
		env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("dynamic_registration_endpoint_response"));
	}
}
