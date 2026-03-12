package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
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
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;

import java.util.function.Supplier;

/**
 * Base class for FAPI2 profile-specific behavior. Provides default (plain FAPI) behavior.
 * Subclasses override methods to customize behavior for specific profiles like
 * OpenBanking UK, Consumer Data Right AU, OpenBanking Brazil, ConnectID AU, and CBUAE.
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

	// --- Action methods with default (plain FAPI) behavior ---

	/**
	 * Set up the resource endpoint. Default calls GetResourceEndpointConfiguration
	 * and the resource configuration sequence.
	 */
	public void setupResourceEndpoint() {
		module.doCallAndStopOnFailure(GetResourceEndpointConfiguration.class);
		module.doCallSequence(getResourceConfiguration());
	}

	/**
	 * Configure client scope if profile requires it. Default does nothing.
	 */
	public void configureClientScope() {
		// plain FAPI: no special scope configuration
	}

	/**
	 * Validate key algorithms in client JWKs.
	 */
	public void validateKeyAlgorithms() {
		module.doCallAndContinueOnFailure(FAPI2CheckKeyAlgInClientJWKs.class,
			ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4");
	}

	/**
	 * Add profile-specific headers to PAR endpoint request. Default does nothing.
	 */
	public void addParEndpointProfileHeaders() {
		// plain FAPI: no additional PAR headers
	}

	/**
	 * Customize the authorization request steps (e.g., add purpose claim).
	 * Default does nothing.
	 */
	public void customizeAuthorizationRequestSteps(ConditionSequence seq) {
		// plain FAPI: no customization
	}

	/**
	 * Add profile-specific headers to token endpoint request. Default does nothing.
	 */
	public void addTokenEndpointProfileHeaders() {
		// plain FAPI: no additional token endpoint headers
	}

	/**
	 * Profile-specific expires_in validation. Default does nothing beyond the
	 * standard validation already done by the module.
	 */
	public void validateExpiresIn() {
		// plain FAPI: no additional expires_in validation
	}

	/**
	 * Validate id_token signing algorithm.
	 */
	public void validateIdTokenSigningAlg() {
		module.doCallAndContinueOnFailure(FAPI2ValidateIdTokenSigningAlg.class,
			ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4");
	}

	/**
	 * Profile-specific id_token validation (e.g., encryption checks).
	 * Default does nothing.
	 */
	public void validateIdTokenEncryption() {
		// plain FAPI: no encryption validation
	}

	/**
	 * Validate interaction ID in token endpoint response. Default does nothing.
	 */
	public void validateTokenEndpointResponseInteractionId() {
		// plain FAPI: no interaction ID validation
	}

	/**
	 * Add profile-specific headers to resource endpoint request.
	 * Default adds auth date, customer IP address, and interaction ID for first client only.
	 */
	public void addResourceEndpointProfileHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			addDefaultFirstClientResourceHeaders();
		}
	}

	/**
	 * Add the default first-client resource endpoint headers
	 * (auth date, customer IP, interaction ID).
	 */
	protected void addDefaultFirstClientResourceHeaders() {
		module.doCallAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
		module.doCallAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		module.doCallAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CID-SP-4.2-12", "CDR-http-headers");
	}

	/**
	 * Set up the resource endpoint request body for payments APIs.
	 * Default does nothing.
	 */
	public void setupResourceEndpointRequestBody(boolean brazilPayments) {
		// plain FAPI: no special request body
	}

	/**
	 * Validate profile-specific resource endpoint response headers.
	 * Default validates interaction ID for first client only (optional).
	 */
	public void validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			module.doSkipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id",
				ConditionResult.INFO, CheckForFAPIInteractionIdInResourceResponse.class,
				ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
			module.doSkipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id",
				ConditionResult.INFO, EnsureMatchingFAPIInteractionId.class,
				ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
		}
	}

	/**
	 * Validate profile-specific PAR response headers. Default does nothing.
	 */
	public void validateParResponseProfileHeaders() {
		// plain FAPI: no PAR response header validation
	}

	/**
	 * Helper to add FAPI interaction ID header to a named headers object.
	 */
	protected void addFapiInteractionIdHeader(String headersKey,
											  Class<? extends Condition> addInteractionIdCondition,
											  String... requirements) {
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		Environment env = module.doGetEnv();
		if (env.getObject(headersKey) == null) {
			env.putObject(headersKey, new JsonObject());
		}
		module.doCallAndStopOnFailure(addInteractionIdCondition, requirements);
	}
}
