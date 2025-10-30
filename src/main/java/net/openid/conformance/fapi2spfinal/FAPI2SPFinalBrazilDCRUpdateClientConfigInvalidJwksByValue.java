package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.GeneratePS256ClientJWKsWithKeyID;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-brazildcr-update-client-config-invalid-jwks-by-value",
	displayName = "FAPI2-Security-Profile-Final: Brazil DCR update client config",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server then use a PUT to try and add a jwks by value, the server must return an 'invalid_client_metadata' error.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalBrazilDCRUpdateClientConfigInvalidJwksByValue extends AbstractFAPI2SPFinalBrazilDCR {
	String originalRedirectUri;

	@Override
	protected void callRegistrationEndpoint() {
		super.callRegistrationEndpoint();

		eventLog.startBlock("Make PUT request to client configuration endpoint to change jwks to one passed by value");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		callAndStopOnFailure(GeneratePS256ClientJWKsWithKeyID.class);
		env.mapKey("dynamic_registration_request", "registration_client_endpoint_request_body");
		callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class);
		env.unmapKey("dynamic_registration_request");
		JsonObject request = env.getObject("registration_client_endpoint_request_body");
		request.remove("jwks_uri");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
		env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("dynamic_registration_endpoint_response"));
	}

	@Override
	public void start() {
		fireTestFinished();
	}

}
