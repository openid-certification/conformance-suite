package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule.FAPIResourceConfiguration;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Abstract base class for FAPI2 profile-specific behavior.
 *
 * Each profile (Plain FAPI, OpenBanking UK, CDR AU, Brazil, ConnectID AU, CBUAE, Client Credentials)
 * extends this class and overrides the methods that differ from the default (Plain FAPI) behavior.
 *
 * The default implementation provides Plain FAPI behavior.
 */
public class FAPI2ProfileBehavior {

	// --- Properties ---

	/**
	 * Whether this profile requires mTLS on all endpoints (PAR, token, resource),
	 * regardless of the sender constraining method.
	 */
	public boolean requiresMtlsEverywhere() {
		return false;
	}

	/**
	 * Whether this profile uses only the client credentials grant (no authorization code flow).
	 */
	public boolean isClientCredentialsGrantOnly() {
		return false;
	}

	// --- ConditionSequence configuration (replaces @VariantSetup fields) ---

	public Class<? extends ConditionSequence> resourceConfiguration() {
		return FAPIResourceConfiguration.class;
	}

	public Supplier<? extends ConditionSequence> preAuthorizationSteps(
		AbstractFAPI2SPFinalServerTestModule module) {
		return null;
	}

	public Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> profileIdTokenValidationSteps() {
		return null;
	}

	// --- Hook methods (one per inline getVariant() check location) ---

	/**
	 * Set up the resource endpoint URL.
	 * Default: calls GetResourceEndpointConfiguration + resourceConfiguration sequence.
	 */
	public void setupResourceEndpoint(AbstractFAPI2SPFinalServerTestModule module) {
		module.defaultSetupResourceEndpoint();
	}

	/**
	 * Configure client scope overrides (e.g., ConnectID forces openid scope).
	 */
	public void configureClientScope(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Validate key algorithms in client JWKs.
	 */
	public void validateKeyAlgorithms(AbstractFAPI2SPFinalServerTestModule module) {
		module.defaultValidateKeyAlgorithms();
	}

	/**
	 * Add profile-specific headers to the PAR endpoint request.
	 */
	public void addParEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Validate profile-specific headers in the PAR response.
	 */
	public void validateParResponseProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Customize the authorization request sequence (e.g., add purpose claim for ConnectID).
	 */
	public void customizeAuthorizationRequest(ConditionSequence seq, AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Whether the request object should be encrypted (e.g., Brazil non-PAR).
	 */
	public boolean encryptRequestObject(boolean isPar) {
		return false;
	}

	/**
	 * Add profile-specific headers to the token endpoint request.
	 */
	public void addTokenEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Validate profile-specific expires_in constraints on the token response.
	 */
	public void validateTokenExpiresIn(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Validate the id_token signing algorithm.
	 */
	public void validateIdTokenSigningAlg(AbstractFAPI2SPFinalServerTestModule module) {
		module.defaultValidateIdTokenSigningAlg();
	}

	/**
	 * Validate id_token encryption requirements (e.g., CDR requires encrypted id_tokens).
	 */
	public void validateIdTokenEncryption(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Validate profile-specific headers in the token endpoint response.
	 */
	public void validateTokenResponseProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Add profile-specific headers to the resource endpoint request.
	 * @param isSecondClient whether this is the second client in a multi-client test
	 */
	public void addResourceEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module, boolean isSecondClient) {
		module.defaultAddResourceEndpointProfileHeaders(isSecondClient);
	}

	/**
	 * Set up the resource request body (e.g., Brazil payments signed JWT body).
	 */
	public void setupResourceRequestBody(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Validate profile-specific headers in the resource endpoint response.
	 * @param isSecondClient whether this is the second client in a multi-client test
	 */
	public void validateResourceResponseProfileHeaders(AbstractFAPI2SPFinalServerTestModule module, boolean isSecondClient) {
		module.defaultValidateResourceResponseProfileHeaders(isSecondClient);
	}

	/**
	 * Validate the resource response body (e.g., Brazil signed payment response).
	 */
	public void validateResourceResponseBody(AbstractFAPI2SPFinalServerTestModule module) {
		// default: no-op
	}

	/**
	 * Get the skip result level for missing interaction ID headers.
	 * ConnectID treats missing interaction ID as FAILURE; others as INFO.
	 */
	public ConditionResult interactionIdSkipResult() {
		return ConditionResult.INFO;
	}
}
