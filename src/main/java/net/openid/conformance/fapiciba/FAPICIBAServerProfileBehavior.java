package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.variant.ClientAuthType;

import java.util.function.Supplier;

/**
 * Base class for FAPI CIBA profile-specific behavior. Provides default (plain FAPI) behavior.
 * Subclasses override methods to customize behavior for specific profiles like
 * OpenBanking UK and OpenBanking Brazil.
 *
 * Action methods return ConditionSequence objects (or null for no-op).
 * The module calls these sequences via call().
 */
public class FAPICIBAServerProfileBehavior {

	protected AbstractFAPICIBAID1 module;

	public void setModule(AbstractFAPICIBAID1 module) {
		this.module = module;
	}

	public Environment getEnv() {
		return module.getEnv();
	}

	// --- Data methods ---

	public Class<? extends ConditionSequence> getResourceConfiguration() {
		return AbstractFAPICIBAID1.FAPIResourceConfiguration.class;
	}

	public Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return null;
	}

	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return AbstractFAPICIBAID1.PlainFapiProfileIdTokenValidationSteps.class;
	}

	public boolean shouldKeepBackchannelAuthenticationEndpointAlias(ClientAuthType authType) {
		return authType == ClientAuthType.MTLS;
	}

	// --- Action methods returning ConditionSequence (null = no-op) ---

	/**
	 * Perform profile-specific configuration validation.
	 * Default does nothing.
	 */
	public ConditionSequence onConfigure() {
		return null;
	}

	/**
	 * Add profile-specific headers to resource endpoint request.
	 * Default adds auth date and interaction ID for first client only.
	 */
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			return new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
					callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
					callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);
				}
			};
		}
		return null;
	}

	/**
	 * Set up the resource endpoint request body. Default does nothing.
	 */
	public ConditionSequence setupResourceEndpointRequestBody() {
		return null;
	}

	/**
	 * Create steps for updating a resource request (e.g. for retries).
	 * Default does nothing.
	 */
	public ConditionSequence createUpdateResourceRequestSteps(boolean isSecondClient, Class<? extends ConditionSequence> addTokenEndpointClientAuthentication) {
		return null;
	}

	/**
	 * Validate profile-specific resource endpoint response headers.
	 * Default validates the resource response interaction ID for all clients,
	 * and validates the returned value matches the sent one for the first client.
	 */
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");
				if (!isSecondClient) {
					callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");
				}
			}
		};
	}

	public ConditionSequence validateResourceEndpointResponseStatus() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(EnsureHttpStatusCodeIs200or201.class).onFail(Condition.ConditionResult.FAILURE));
			}
		};
	}

	/**
	 * Validate profile-specific response from the resource endpoint.
	 * Default does nothing.
	 */
	public ConditionSequence validateResourceEndpointResponse() {
		// No-op by default
		return null;
	}

	/**
	 * Profile-specific expires_in validation. Default does nothing.
	 */
	public ConditionSequence validateExpiresIn() {
		return null;
	}
}
