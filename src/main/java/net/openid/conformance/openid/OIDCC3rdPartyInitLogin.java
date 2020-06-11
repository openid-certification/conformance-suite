package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddInitiateLoginUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CreateInitiateLoginUri;
import net.openid.conformance.condition.client.ValidateInitiateLoginUriInConfigurationResponse;
import net.openid.conformance.condition.client.ValidateInitiateLoginUriInRegistrationResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OP-3rd_party-init-login
// https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_3rd_party_init_login
@PublishTestModule(
	testName = "oidcc-3rd_party-init-login",
	displayName = "OIDCC: 3rd party initiated login",
	summary = "Registers a client including the 'initiate_login_uri' parameter, and verifies that the response includes the initiate_login_uri and that it is returned from the client management endpoint ('registration_client_uri' returned by the OP).",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
public class OIDCC3rdPartyInitLogin extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(CreateInitiateLoginUri.class, "OIDCC-4", "OIDCR-2");
		callAndStopOnFailure(AddInitiateLoginUriToDynamicRegistrationRequest.class, "OIDCC-4", "OIDCR-2");
	}

	@Override
	protected void performAuthorizationFlow() {
		callAndContinueOnFailure(ValidateInitiateLoginUriInRegistrationResponse.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");

		eventLog.startBlock("Call client configuration endpoint");
		// The python test called the client configuration endpoint. I'm not convinced it's a mandatory to implement
		// feature for this profile, but we just do as the python test did.
		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(ValidateInitiateLoginUriInConfigurationResponse.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		eventLog.endBlock();

		fireTestFinished();
	}

}
