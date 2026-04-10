package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.FAPI2ValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetOauthDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.common.FAPI2CheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.GrantManagement;

import java.util.function.Supplier;

/**
 * Base class for FAPI2 profile-specific behavior. Provides default (plain FAPI) behavior.
 * Subclasses override methods to customize behavior for specific profiles like
 * OpenBanking UK, Consumer Data Right AU, OpenBanking Brazil, ConnectID AU, and CBUAE.
 *
 * Action methods return ConditionSequence objects (or null for no-op).
 * The module calls these sequences via call().
 */
public class FAPI2ProfileBehavior {

	protected AbstractFAPI2SPFinalServerTestModule module;

	public void setModule(AbstractFAPI2SPFinalServerTestModule module) {
		this.module = module;
	}

	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return PlainFAPIDiscoveryEndpointChecks::new;
	}

	public static class PlainFAPIDiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
		}
	}

	// --- Data methods ---

	public boolean requiresMtlsEverywhere() {
		return false;
	}

	public boolean isClientCredentialsGrantOnly() {
		return false;
	}

	/**
	 * Whether RAR (Rich Authorization Requests) should be extracted from user config.
	 * VCI returns false because it generates RAR from the credential configuration.
	 */
	public boolean shouldExtractRARFromConfig() {
		return true;
	}

	public Class<? extends ConditionSequence> getResourceConfiguration() {
		return AbstractFAPI2SPFinalServerTestModule.FAPIResourceConfiguration.class;
	}

	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return null;
	}

	public boolean shouldEncryptRequestObject(boolean isPar) {
		return false;
	}

	// --- Action methods returning ConditionSequence (null = no-op) ---

	/**
	 * Set up the resource endpoint. Default calls GetResourceEndpointConfiguration
	 * and the resource configuration sequence.
	 */
	public ConditionSequence setupResourceEndpoint() {
		Class<? extends ConditionSequence> resourceConfig = getResourceConfiguration();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(GetResourceEndpointConfiguration.class);
				call(sequence(resourceConfig));
			}
		};
	}

	/**
	 * Configure client scope if profile requires it. Default does nothing.
	 */
	public ConditionSequence configureClientScope() {
		// plain FAPI: no special scope configuration
		return null;
	}

	/**
	 * Validate key algorithms in client JWKs.
	 */
	public ConditionSequence validateKeyAlgorithms() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPI2CheckKeyAlgInClientJWKs.class,
					ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4");
			}
		};
	}

	/**
	 * Add profile-specific headers to PAR endpoint request. Default does nothing.
	 */
	public ConditionSequence addParEndpointProfileHeaders() {
		// plain FAPI: no additional PAR headers
		return null;
	}

	/**
	 * Customize the authorization request steps (e.g., add purpose claim).
	 * Default does nothing. This method mutates the given sequence via .then().
	 */
	public void customizeAuthorizationRequestSteps(ConditionSequence seq) {
		// plain FAPI: no customization
	}

	/**
	 * Add profile-specific headers to token endpoint request. Default does nothing.
	 */
	public ConditionSequence addTokenEndpointProfileHeaders() {
		// plain FAPI: no additional token endpoint headers
		return null;
	}

	/**
	 * Profile-specific expires_in validation. Default does nothing beyond the
	 * standard validation already done by the module.
	 */
	public ConditionSequence validateExpiresIn() {
		// plain FAPI: no additional expires_in validation
		return null;
	}

	/**
	 * Validate id_token signing algorithm.
	 */
	public ConditionSequence validateIdTokenSigningAlg() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPI2ValidateIdTokenSigningAlg.class,
					ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4");
			}
		};
	}

	/**
	 * Profile-specific id_token validation (e.g., encryption checks).
	 * Default does nothing.
	 */
	public ConditionSequence validateIdTokenEncryption() {
		// plain FAPI: no encryption validation
		return null;
	}

	/**
	 * Validate interaction ID in token endpoint response. Default does nothing.
	 */
	public ConditionSequence validateTokenEndpointResponseInteractionId() {
		// plain FAPI: no interaction ID validation
		return null;
	}

	/**
	 * Hook called after a successful token-endpoint response has been processed,
	 * including after each successful refresh-token call. Profiles that need to
	 * preserve data from the response across subsequent token-endpoint calls
	 * (which overwrite token_endpoint_response) can return a sequence here.
	 */
	public ConditionSequence afterTokenEndpointResponseProcessed() {
		return null;
	}

	/**
	 * Add profile-specific headers to resource endpoint request.
	 * Default adds auth date, customer IP address, and interaction ID for first client only.
	 */
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			return createDefaultFirstClientResourceHeaders();
		}
		return null;
	}

	/**
	 * Create a sequence with the default first-client resource endpoint headers
	 * (auth date, customer IP, interaction ID).
	 */
	protected ConditionSequence createDefaultFirstClientResourceHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
				callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CID-SP-4.2-12", "CDR-http-headers");
			}
		};
	}

	/**
	 * Set up the resource endpoint request body (e.g. for signed payment requests).
	 * Default does nothing.
	 */
	public ConditionSequence setupResourceEndpointRequestBody() {
		// plain FAPI: no special request body
		return null;
	}

	/**
	 * Validate profile-specific signed response from the resource endpoint.
	 * Default does nothing.
	 */
	public ConditionSequence validateResourceEndpointResponse() {
		// plain FAPI: no signed response validation
		return null;
	}

	/**
	 * Set alternate accept/content-type headers for resource endpoint requests.
	 * Default sets UTF-8 JSON accept headers.
	 */
	public ConditionSequence setAlternateResourceEndpointContentHeaders() {
		return null;
	}

	/**
	 * Perform profile-specific configuration validation (e.g. directory checks).
	 * Default does nothing.
	 */
	public ConditionSequence onConfigure() {
		// plain FAPI: no directory validation
		return null;
	}

	/**
	 * Validate that the discovery endpoint advertises required scopes.
	 * Default does nothing.
	 */
	public ConditionSequence validateDiscoveryEndpointScopes() {
		// plain FAPI: no scope validation
		return null;
	}

	/**
	 * Set the appropriate scope on a client credentials grant token request.
	 * Default does nothing.
	 */
	public ConditionSequence setTokenEndpointScopeForClientCredentials() {
		// plain FAPI: no special scope
		return null;
	}

	/**
	 * Create steps for updating a resource request (e.g. re-signing payment JWTs).
	 * Default only handles DPoP steps.
	 */
	public ConditionSequence createUpdateResourceRequestSteps(
			Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (createDpopForResourceEndpointSteps != null) {
					call(sequence(createDpopForResourceEndpointSteps));
				}
			}
		};
	}

	/**
	 * Validate profile-specific resource endpoint response headers.
	 * Default validates interaction ID for first client only (optional).
	 */
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			return new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					call(condition(CheckForFAPIInteractionIdInResourceResponse.class)
						.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
						.onSkip(ConditionResult.INFO)
						.onFail(ConditionResult.FAILURE)
						.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
						.dontStopOnFailure());
					call(condition(EnsureMatchingFAPIInteractionId.class)
						.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
						.onSkip(ConditionResult.INFO)
						.onFail(ConditionResult.FAILURE)
						.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
						.dontStopOnFailure());
				}
			};
		}
		return null;
	}

	/**
	 * Validate profile-specific PAR response headers. Default does nothing.
	 */
	public ConditionSequence validateParResponseProfileHeaders() {
		// plain FAPI: no PAR response header validation
		return null;
	}

	/**
	 * Helper to create a sequence that adds a FAPI interaction ID header.
	 */
	protected ConditionSequence createFapiInteractionIdHeaderSequence(
			Class<? extends Condition> addInteractionIdCondition,
			String... requirements) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(addInteractionIdCondition, requirements);
			}
		};
	}

	// --- Hook methods for VCI extension ---

	/**
	 * Called at the very beginning of configure() to initialize variant-derived fields.
	 * The default implementation reads standard FAPI2 variants.
	 * VCI overrides to set VCI-specific defaults (e.g. jarm=false, isOpenId=false).
	 */
	public void initializeVariants() {
		module.jarm = module.getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;
		module.isPar = true;
		if (module.getVariant(FAPIOpenIDConnect.class) == FAPIOpenIDConnect.PLAIN_OAUTH && module.scopeContains("openid")) {
			throw new TestFailureException(module.getId(), "openid scope cannot be used with PLAIN_OAUTH");
		}
		module.isOpenId = module.getVariant(FAPIOpenIDConnect.class) == FAPIOpenIDConnect.OPENID_CONNECT;
		module.isSignedRequest = module.getVariant(FAPI2AuthRequestMethod.class) == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION;
		module.isRarRequest = module.getVariant(AuthorizationRequestType.class) == AuthorizationRequestType.RAR;
		module.clientCredentialsGrant = isClientCredentialsGrantOnly();
		module.useDpopAuthCodeBinding = false;
		module.profileRequiresMtlsEverywhere = requiresMtlsEverywhere();
		module.isGrantManagement = module.getVariant(GrantManagement.class) == GrantManagement.ENABLED;
	}

	/**
	 * Fetch and store server configuration. Default fetches OIDC or OAuth server config.
	 * VCI overrides this to fetch credential issuer metadata and derive the AS.
	 */
	public ConditionSequence fetchServerConfiguration(boolean isOpenId) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (isOpenId) {
					callAndStopOnFailure(GetDynamicServerConfiguration.class);
				} else {
					callAndStopOnFailure(GetOauthDynamicServerConfiguration.class);
				}
			}
		};
	}

	/**
	 * Additional client configuration after the standard setup.
	 * VCI overrides to generate client JWKs if missing and handle encryption JWKs.
	 */
	public ConditionSequence configureClientExtra() {
		// plain FAPI: no additional client configuration
		return null;
	}

	/**
	 * Configure client attestation keys if needed.
	 * VCI overrides to generate attestation keys and JWT.
	 */
	public ConditionSequence configureClientAttestation() {
		// plain FAPI: no client attestation
		return null;
	}

	/**
	 * Validate the private part of client JWKs.
	 * VCI overrides to use VCIValidateClientJWKsPrivatePart (which allows multiple signing keys).
	 */
	public ConditionSequence validateClientJwksPrivatePart() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
			}
		};
	}

	/**
	 * Return the condition class for checking that token_endpoint_auth_methods_supported
	 * contains an acceptable auth method. Default requires private_key_jwt or tls_client_auth.
	 * VCI overrides to also accept attest_jwt_client_auth.
	 */
	public Class<? extends AbstractCondition> getDiscoveryTokenEndpointAuthMethodsCheck() {
		return CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient.class;
	}

	// --- Discovery endpoint verification methods ---

	/**
	 * Fetch and store server configuration for the discovery endpoint verification test.
	 * Default fetches OIDC or OAuth server configuration and validates the response.
	 * VCI overrides to fetch credential issuer metadata and derive the AS.
	 */
	public ConditionSequence discoveryFetchServerConfiguration(boolean isOpenId) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				String specRequirements = "OIDCD-4";
				if (isOpenId) {
					callAndStopOnFailure(GetDynamicServerConfiguration.class);
				} else {
					callAndStopOnFailure(GetOauthDynamicServerConfiguration.class);
					specRequirements = "RFC8414-3.2";
				}
				callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, ConditionResult.FAILURE, specRequirements);
				callAndContinueOnFailure(CheckDiscoveryEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, specRequirements);
			}
		};
	}

	/**
	 * Called after server configuration is fetched in the discovery endpoint verification test.
	 * Default does nothing. VCI overrides to set discoveryUrl from the authorization server issuer.
	 */
	public ConditionSequence discoveryAfterServerConfigurationFetched() {
		return null;
	}
}
