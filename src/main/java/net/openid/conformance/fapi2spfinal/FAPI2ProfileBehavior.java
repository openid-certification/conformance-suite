package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.FAPI2ValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.common.FAPI2CheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Base class for FAPI2 profile-specific behavior. Provides default (plain FAPI) behavior.
 * Subclasses override methods to customize behavior for specific profiles like
 * OpenBanking UK, Consumer Data Right AU, OpenBanking Brazil, ConnectID AU, and CBUAE.
 *
 * Action methods return ConditionSequence objects (or null for no-op).
 * The module calls these sequences via callProfileSequence() or call().
 */
public class FAPI2ProfileBehavior {

	protected AbstractFAPI2SPFinalServerTestModule module;

	public void setModule(AbstractFAPI2SPFinalServerTestModule module) {
		this.module = module;
	}

	// --- Data methods ---

	public boolean requiresMtlsEverywhere() {
		return false;
	}

	public boolean isClientCredentialsGrantOnly() {
		return false;
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
	 * Set up the resource endpoint request body for payments APIs.
	 * Default does nothing.
	 */
	public ConditionSequence setupResourceEndpointRequestBody(boolean brazilPayments) {
		// plain FAPI: no special request body
		return null;
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
}
