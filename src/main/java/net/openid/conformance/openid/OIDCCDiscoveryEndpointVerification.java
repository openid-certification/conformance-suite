package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.client.CheckDiscEndpointAllEndpointsAreHttps;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointDiscoveryUrl;
import net.openid.conformance.condition.client.CheckDiscEndpointIssuer;
import net.openid.conformance.condition.client.CheckDiscEndpointRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedIncludesRS256;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedContainsOpenId;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointGrantTypesSupportedDynamic;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointIdTokenSigningAlgValuesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointResponseTypesSupported;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointResponseTypesSupportedDynamic;
import net.openid.conformance.condition.client.OIDCCCheckDiscEndpointUserinfoSigningAlgValuesSupported;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

// Corresponds to OP-Discovery-* tests
@PublishTestModule(
	testName = "oidcc-discovery-endpoint-verification",
	displayName = "OIDCC: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) contains values required by the specifications",
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
public class OIDCCDiscoveryEndpointVerification extends AbstractTestModule {

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


		if (getVariant(ClientRegistration.class) == ClientRegistration.DYNAMIC_CLIENT) {
			callAndContinueOnFailure(OIDCCCheckDiscEndpointResponseTypesSupportedDynamic.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "OIDCC-15.2");
		} else {
			callAndContinueOnFailure(OIDCCCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "OIDCC-3");
		}

		callAndContinueOnFailure(CheckDiscEndpointDiscoveryUrl.class,Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointIssuer.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3", "OIDCD-7.2");

		callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

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
			.onSkip(Condition.ConditionResult.WARNING) // userinfo endpoint is recommended in the spec
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
		callAndStopOnFailure(FetchServerKeys.class);
		callAndContinueOnFailure(ValidateServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.INFO);
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.INFO);
		call(condition(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedIncludesRS256.class)
				.skipIfElementMissing("server", "request_object_signing_alg_values_supported")
				.onFail(Condition.ConditionResult.WARNING)
				.onSkip(Condition.ConditionResult.INFO)
				.requirement("OIDCD-3")
				.dontStopOnFailure());

		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.INFO, "OIDCD-3");

		// Includes providerinfo-has-claims_supported assertion (OIDC test)
		// claims_supported is recommended to be present, but not required
		callAndContinueOnFailure(OIDCCCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.WARNING, "OIDCD-3");

		if (getVariant(ClientRegistration.class) == ClientRegistration.DYNAMIC_CLIENT) {
			callAndContinueOnFailure(OIDCCCheckDiscEndpointGrantTypesSupportedDynamic.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		} else {
			callAndContinueOnFailure(OIDCCCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		}

		call(condition(CheckDiscEndpointScopesSupportedContainsOpenId.class)
				.skipIfElementMissing("server", "scopes_supported")
				.onFail(Condition.ConditionResult.FAILURE)
				.onSkip(Condition.ConditionResult.WARNING)
				.requirement("OIDCD-3")
				.dontStopOnFailure());

		// Equivalent of VerifyOPEndpointsUseHTTPS
		// https://github.com/rohe/oidctest/blob/a306ff8ccd02da456192b595cf48ab5dcfd3d15a/src/oidctest/op/check.py#L1714
		// I'm not convinced the standards actually says every endpoint (including ones not defined by OIDC) must be https,
		// but equally it seems reasonable.
		callAndContinueOnFailure(CheckDiscEndpointAllEndpointsAreHttps.class, Condition.ConditionResult.FAILURE);
	}

}
