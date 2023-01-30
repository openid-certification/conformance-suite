package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidRedirectUriOrInvalidClientMetadata;
import net.openid.conformance.condition.client.CreateBadRedirectUri;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-brazil-dcr-invalid-redirect-uri",
	displayName = "FAPI2-Security-Profile-ID2: Brazil DCR Invalid Redirect URI",
	summary = "Perform the DCR flow, but requesting a redirect uri not present in the software statement - the server must reject the registration attempt.",
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
public class FAPI2SPID2BrazilDCRInvalidRedirectUri extends AbstractFAPI2SPID2BrazilDCR {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreateBadRedirectUri.class);
		exposeEnvString("redirect_uri");

		super.configureClient();
	}

	@Override
	protected void setupResourceEndpoint() {
	}

	@Override
	protected void validateSsa() {
	}

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidRedirectUriOrInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
	}

	@Override
	public void start() {
		fireTestFinished();
	}
}
