package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.GeneratePS256ClientJWKsWithKeyID;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-brazil-dcr-invalid-jwks-by-value",
	displayName = "FAPI2-Security-Profile-ID2: Brazil DCR Invalid JWKS by value",
	summary = "Perform the DCR flow, but passing a jwks by value - the server must reject the registration attempt.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPID2BrazilDCRInvalidJwksByValue extends AbstractFAPI2SPID2BrazilDCR {

	@Override
	protected void setupJwksUri() {
		callAndStopOnFailure(GeneratePS256ClientJWKsWithKeyID.class);
	}

	@Override
	protected void addJwksToRequest() {
		callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class);
	}

	@Override
	protected void setupResourceEndpoint() {
	}

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
	}

	@Override
	public void start() {
		fireTestFinished();
	}
}
