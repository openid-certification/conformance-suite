package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.OIDCCValidateClientRedirectUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientGrantTypes;
import net.openid.conformance.condition.as.dynregistration.ValidateClientLogoUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientPolicyUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientSubjectType;
import net.openid.conformance.condition.as.dynregistration.ValidateClientTosUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientUris;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultMaxAge;
import net.openid.conformance.condition.as.dynregistration.ValidateInitiateLoginUri;
import net.openid.conformance.condition.as.dynregistration.ValidateRequireAuthTime;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.client.CheckDiscEndpointAllEndpointsAreHttps;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedIncludesRS256;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedContainsOpenId;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.FetchServerKeys;
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
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-federation-entity-configuration-endpoint-verification",
	displayName = "OpenID Federation: Entity Configuration Endpoint Verification",
	summary = "This test ensures that the server's entity configuration metadata is according to the specifications",
	profile = "OIDFED",
	configurationFields = {
		"server.entityStatementUrl",
	}
)
@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.entityStatement"
})
@VariantNotApplicable(parameter = ClientRegistration.class, values={ "static_client" })
public class OpenIDFederationEntityStatementVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		if (ServerMetadata.STATIC.equals(getVariant(ServerMetadata.class))) {
			// This case is actually not valid, I believe, but it's here for testing purposes
			callAndStopOnFailure(GetStaticEntityStatement.class, Condition.ConditionResult.FAILURE);
		} else {
			callAndStopOnFailure(GetEntityStatement.class);

			env.mapKey("discovery_endpoint_response", "entity_statement_endpoint_response");
			callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
			env.unmapKey("discovery_endpoint_response");

			callAndContinueOnFailure(ValidateEntityStatementEndpointReturnedCorrectContentType.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		}
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		validateEntityStatement();
		fireTestFinished();
	}

	protected void validateEntityStatement() {
		callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(VerifyEntityStatmentSignature.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ValidateEntityStatementMetadataClaim.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ExtractOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		if (env.containsObject("openid_relying_party_metadata")) {
			env.mapKey("client", "openid_relying_party_metadata");
			validateClientRegistrationMetadata();
			callAndContinueOnFailure(ValidateOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("client");
		}
		callAndContinueOnFailure(ExtractOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		if (env.containsObject("openid_provider_metadata")) {
			env.mapKey("server", "openid_provider_metadata");
			performEndpointVerification();
			callAndContinueOnFailure(ValidateOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("server");
		}
		callAndContinueOnFailure(ValidateOAuthAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateOAuthClientMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateOAuthProtectedResourceMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
	}

	// This is the validateClientRegistrationMetadata() methods found in various FAPI classes
	public void validateClientRegistrationMetadata() {
		//check response type - grant type consistency
		callAndContinueOnFailure(ValidateClientGrantTypes.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//basic checks like fragments, https etc
		callAndContinueOnFailure(OIDCCValidateClientRedirectUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//check if logo is image
		callAndContinueOnFailure(ValidateClientLogoUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientPolicyUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientTosUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(ValidateClientSubjectType.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		/*
		skipIfElementMissing("client", "id_token_signed_response_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateIdTokenSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.2");
		*/

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//userinfo
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			ValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		/*
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.2");
		*/

		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//request object
		/*
		skipIfElementMissing("client", "request_object_signing_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateRequestObjectSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		*/
		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		/*
		skipIfElementMissing("client", "token_endpoint_auth_signing_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateTokenEndpointAuthSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		*/
		callAndContinueOnFailure(ValidateDefaultMaxAge.class, Condition.ConditionResult.WARNING,"OIDCR-2");

		skipIfElementMissing("client", "require_auth_time", Condition.ConditionResult.INFO,
			ValidateRequireAuthTime.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		/*
		skipIfElementMissing("client", "default_acr_values", Condition.ConditionResult.INFO,
			FAPIBrazilValidateDefaultAcrValues.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		*/

		skipIfElementMissing("client", "initiate_login_uri", Condition.ConditionResult.INFO,
			ValidateInitiateLoginUri.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//TODO not allow request_uris?
		/*
		skipIfElementMissing("client", "request_uris", Condition.ConditionResult.INFO,
			ValidateRequestUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		 */

	}

	// This is the performEndpointVerification() methods found in OIDCCDiscoveryEndpointVerification
	protected void performEndpointVerification() {

		if (getVariant(ClientRegistration.class) == ClientRegistration.DYNAMIC_CLIENT) {
			callAndContinueOnFailure(OIDCCCheckDiscEndpointResponseTypesSupportedDynamic.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "OIDCC-15.2");
		} else {
			callAndContinueOnFailure(OIDCCCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "OIDCC-3");
		}

		/*
		callAndContinueOnFailure(CheckDiscEndpointDiscoveryUrl.class,Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointIssuer.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3", "OIDCD-7.2");
		*/

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
