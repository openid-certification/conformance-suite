package net.openid.conformance.openid;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointDiscoveryUrl;
import net.openid.conformance.condition.client.CheckDiscEndpointIssuer;
import net.openid.conformance.condition.client.CheckDiscEndpointRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointIdTokenSigningAlgValuesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointResponseTypesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointScopesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointUserinfoSigningAlgValuesSupported;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-Discovery-* tests
@PublishTestModule(
	testName = "oidcc-discovery-endpoint-verification",
	displayName = "OIDCC: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) is containing the required value in the specification",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class OIDCCDiscoveryEndpointVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {

		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		// Includes check-http-response assertion (OIDC test)
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

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

		callAndContinueOnFailure(OIDCCCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		callAndContinueOnFailure(CheckDiscEndpointDiscoveryUrl.class,Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointIssuer.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3", "OIDCD-7.2");

		// Includes verify-id_token_signing-algorithm-is-supported assertion (OIDC test)
		callAndContinueOnFailure(OIDCCCheckDiscEndpointIdTokenSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		call(condition(OIDCCCheckDiscEndpointUserinfoSigningAlgValuesSupported.class)
			.skipIfElementMissing("server", "userinfo_signing_alg_values_supported")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("OIDCD-3")
			.dontStopOnFailure()
		);

		// Includes verify-op-endpoints-use-https assertion (OIDC test) for each endpoint tested
		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		callAndContinueOnFailure(CheckDiscEndpointTokenEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		call(condition(CheckDiscEndpointUserinfoEndpoint.class)
			.skipIfElementMissing("server", "userinfo_endpoint")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("OIDCD-3")
			.dontStopOnFailure());

		// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#verify_op_has_registration_endpoint
		call(condition(CheckDiscEndpointRegistrationEndpoint.class)
			.skipIfElementMissing("server", "registration_endpoint")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("OIDCD-3")
			.dontStopOnFailure());

		// Includes providerinfo-has-jwks_uri
		callAndContinueOnFailure(CheckJwksUri.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.INFO);
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.INFO);
		call(condition(OIDCCCheckDiscEndpointRequestObjectSigningAlgValuesSupported.class)
				.skipIfElementMissing("server", "request_object_signing_alg_values_supported")
				.onFail(Condition.ConditionResult.WARNING)
				.onSkip(Condition.ConditionResult.INFO)
				.requirement("OIDCD-3")
				.dontStopOnFailure());

		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.INFO, "OIDCD-3");

		// Includes providerinfo-has-claims_supported assertion (OIDC test)
		// claims_supported is recommended to be present, but not required
		callAndContinueOnFailure(OIDCCCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.WARNING, "OIDCD-3");
		call(condition(OIDCCCheckDiscEndpointGrantTypesSupported.class)
				.skipIfElementMissing("server", "grant_types_supported")
				.onFail(Condition.ConditionResult.FAILURE)
				.onSkip(Condition.ConditionResult.INFO)
				.requirement("OIDCD-3")
				.dontStopOnFailure());
		call(condition(OIDCCCheckDiscEndpointScopesSupported.class)
				.skipIfElementMissing("server", "scopes_supported")
				.onFail(Condition.ConditionResult.FAILURE)
				.onSkip(Condition.ConditionResult.WARNING)
				.requirement("OIDCD-3")
				.dontStopOnFailure());
	}

}
