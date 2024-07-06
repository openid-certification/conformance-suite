package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
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
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
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
	ServerMetadata.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.entityStatement"
})
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
		performEndpointVerification();
		fireTestFinished();
	}

	protected void performEndpointVerification() {
		callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ValidateEntityStatementMetadataClaim.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ExtractOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		if (env.containsObject("openid_relying_party_metadata")) {
			env.mapKey("client", "openid_relying_party_metadata");
			validateClientRegistrationMetadata();
			callAndContinueOnFailure(ValidateOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("client");
		}
	}

	// Adapted from the validateClientRegistrationMetadata() methods found in various FAPI classes
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

}
