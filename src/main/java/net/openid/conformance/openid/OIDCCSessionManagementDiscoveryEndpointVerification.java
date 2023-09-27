package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscCheckSessionIframe;
import net.openid.conformance.condition.client.CheckDiscEndSessionEndpoint;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_Session_Discovery
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Session-Discovery.json
@PublishTestModule(
	testName = "oidcc-session-management-discovery-endpoint-verification",
	displayName = "OIDCC: Session Management Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations contains the values required by the specifications, check_session_iframe and end_session_endpoint.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
	}
)
@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantNotApplicable(parameter = ServerMetadata.class, values = { "static"} )
public class OIDCCSessionManagementDiscoveryEndpointVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putObject("config", config);

		// Includes check-http-response assertion (OIDC test)
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDCD-4");
		callAndContinueOnFailure(CheckDiscoveryEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		performEndpointVerification();

		fireTestFinished();
	}

	protected void performEndpointVerification() {

		callAndContinueOnFailure(CheckDiscCheckSessionIframe.class, Condition.ConditionResult.FAILURE, "OIDCSM-3.3");
		callAndContinueOnFailure(CheckDiscEndSessionEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2.1");
	}

}
