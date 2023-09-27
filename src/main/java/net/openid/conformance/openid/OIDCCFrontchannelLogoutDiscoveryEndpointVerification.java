package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndSessionEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointAllEndpointsAreHttps;
import net.openid.conformance.condition.client.CheckDiscEndpointFrontchannelLogoutSessionSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointFrontchannelLogoutSupported;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_BackChannel_Discovery
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-BackChannel-Discovery.json
@PublishTestModule(
	testName = "oidcc-frontchannel-logout-discovery-endpoint-verification",
	displayName = "OIDCC: Frontchannel Logout Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations contains the values required by the specifications, frontchannel_logout_supported,  frontchannel_logout_session_supported and end_session_endpoint. Certification for this profile requires that frontchannel logout session (i.e. 'sid' in the id_token & logout request) is supported.",
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
public class OIDCCFrontchannelLogoutDiscoveryEndpointVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
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

		// Equivalent of VerifyOPEndpointsUseHTTPS
		// https://github.com/rohe/oidctest/blob/a306ff8ccd02da456192b595cf48ab5dcfd3d15a/src/oidctest/op/check.py#L1714
		// I'm not convinced the standards actually says every endpoint (including ones not defined by OIDC) must be https,
		// but equally it seems reasonable. Individual endpoint checks (e.g. CheckDiscEndSessionEndpoint) also check
		// the relevant urls are https.
		callAndContinueOnFailure(CheckDiscEndpointAllEndpointsAreHttps.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointFrontchannelLogoutSupported.class, Condition.ConditionResult.FAILURE, "OIDCFCL-2");

		// It's optional in the spec, but for certification OPs must support the session id (sid) in the id_token / logout request, due to "the mess with cookies and SameSite"
		callAndContinueOnFailure(CheckDiscEndpointFrontchannelLogoutSessionSupported.class, Condition.ConditionResult.FAILURE, "OIDCFCL-2");

		// python didn't check this; I have no idea why not, it's definitely needed to complete the test...
		callAndContinueOnFailure(CheckDiscEndSessionEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCBCL-3", "OIDCRIL-2.1");
	}

}
