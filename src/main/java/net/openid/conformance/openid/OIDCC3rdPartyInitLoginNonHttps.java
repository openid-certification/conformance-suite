package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddInitiateLoginUriAsNonHttpsToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata;
import net.openid.conformance.condition.client.CreateInitiateLoginUri;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OP-3rd_party-init-login-nohttps
// https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_3rd_party_init_login_nohttps
@PublishTestModule(
	testName = "oidcc-3rd_party-init-login-nohttps",
	displayName = "OIDCC: 3rd party initiated login - no https",
	summary = "Registers a client including the 'initiate_login_uri' parameter as a non-https url, which the server must reject.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
public class OIDCC3rdPartyInitLoginNonHttps extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(CreateInitiateLoginUri.class, "OIDCC-4", "OIDCR-2");
		callAndStopOnFailure(AddInitiateLoginUriAsNonHttpsToDynamicRegistrationRequest.class, "OIDCC-4", "OIDCR-2");
	}

	@Override
	protected void configureDynamicClient() {

		createDynamicClientRegistrationRequest();

		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC6749-3.1.2");

		env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.FAILURE, "OIDCR-3.3");
	}

	@Override
	protected void completeClientConfiguration() {
	}

	@Override
	protected void performAuthorizationFlow() {
		fireTestFinished();
	}

}
