package net.openid.conformance.authzen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.ApplySignedMetadataPrecedence;
import net.openid.conformance.authzen.condition.CheckPDPServerConfiguration;
import net.openid.conformance.authzen.condition.CheckStaticPDPServerConfiguration;
import net.openid.conformance.authzen.condition.EnsureDiscoveryMetadataParamsNotEmpty;
import net.openid.conformance.authzen.condition.EnsureDiscoveryMetadataResponseValid;
import net.openid.conformance.authzen.condition.EnsureMetadataCapabilitiesValid;
import net.openid.conformance.authzen.condition.EnsurePDPJwksConfigured;
import net.openid.conformance.authzen.condition.EnsurePolicyDecisionPointMatchesIssuer;
import net.openid.conformance.authzen.condition.ExtractPDPSignedMetadataValues;
import net.openid.conformance.authzen.condition.GetPDPDynamicServerConfiguration;
import net.openid.conformance.authzen.condition.ValidateDiscoverySignedMetadata;
import net.openid.conformance.authzen.condition.ValidatePDPMetadataIssuer;
import net.openid.conformance.authzen.condition.WarnAuthzenCapabilitiesNotAuthzenUrns;
import net.openid.conformance.authzen.condition.WarnAuthzenMetadataUnknownMembers;
import net.openid.conformance.authzen.condition.WarnOnUnusableAuthzenPdpJwksKeys;
import net.openid.conformance.authzen.condition.WarnUnusedSignedMetadataConfiguration;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.common.EnsureJwksHasNoPrivateAsymmetricKeyMaterial;
import net.openid.conformance.condition.common.EnsureJwksHasNoPrivateOrSymmetricKeyMaterial;
import net.openid.conformance.condition.common.WarnOnUnusableJwksKeys;
import net.openid.conformance.sequence.ValidateJwksSequence;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;

/**
 * Fetching and validating PDP metadata (Section 9), shared by the API test modules, which run it
 * under the discovery variant, and by the Discovery certification module, which runs nothing else.
 * Declares no variant parameters, so the Discovery module has none.
 */
public abstract class AbstractAuthzenPDPMetadataTest extends AbstractRedirectServerTestModule {

	/**
	 * Stores the configuration in the environment. Returns false, having finished the test, when
	 * the configuration asks for the test to be skipped.
	 */
	protected boolean storeConfigurationUnlessSkipped(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, ConditionResult.FAILURE);
			fireTestFinished();
			return false;
		}
		return true;
	}

	/**
	 * Fetches the metadata from the well-known URL derived from the configured PDP Identifier into
	 * the {@code pdp} environment object, validates the response and, when present, the
	 * signed_metadata JWT, and applies its values over the plain ones.
	 */
	protected void fetchAndValidateDiscoveryMetadata() {
		eventLog.startBlock("Fetch the PDP metadata");
		try {
			callAndStopOnFailure(GetPDPDynamicServerConfiguration.class, "AUTHZEN-9.2", "AUTHZEN-9.2.2");
			callAndContinueOnFailure(EnsureDiscoveryMetadataResponseValid.class, ConditionResult.FAILURE, "AUTHZEN-9.2.2");
		} finally {
			eventLog.endBlock();
		}
		// signed_metadata is OPTIONAL (§9.1.3): only when the PDP metadata carries
		// it do we validate it, verify its signature against the configured PDP JWK
		// Set, and apply its precedence over the plain JSON metadata so the
		// downstream checks validate the signed (authoritative) values.
		// The gate inspects the raw JsonElement rather than reading it as a string:
		// a non-string signed_metadata (number, object, array) is a protocol error that
		// ExtractPDPSignedMetadata is there to report, so it has to reach the sequence
		// instead of throwing an UnexpectedTypeException out of the gate itself.
		JsonElement signedMetadataElement = env.getElementFromObject("pdp", "signed_metadata");
		boolean hasSignedMetadata = signedMetadataElement != null && !signedMetadataElement.isJsonNull();
		// Sections 9.2.2 and 9.2.3 apply to the response as the PDP published it, so these run
		// before any signed_metadata values replace plain ones. A mismatched policy_decision_point
		// is reported here, before the signed metadata checks that need the configured keys.
		eventLog.startBlock("Check the published PDP metadata");
		try {
			checkMetadataDocument("pdp");
			if (!hasSignedMetadata) {
				// A 'Signed Metadata Issuer' and a 'PDP JWK Set' only apply to a signed_metadata JWT.
				// Ignoring them in silence would leave a tester who expected signed metadata reading a
				// clean log. The check is on the test configuration, not the PDP, so it cites no
				// requirement.
				callAndContinueOnFailure(WarnUnusedSignedMetadataConfiguration.class, ConditionResult.WARNING);
			}
		} finally {
			eventLog.endBlock();
		}
		if (!hasSignedMetadata) {
			return;
		}
		// Log blocks do not nest, and ValidateJwksSequence opens its own, so the configuration
		// checks before it and the signature checks after it each get a block of their own.
		eventLog.startBlock("Check the signed metadata configuration");
		try {
			callAndStopOnFailure(EnsurePDPJwksConfigured.class);
			// Check the expected issuer before it is compared against the JWT, so a malformed
			// value is reported as the configuration error it is rather than as a PDP mismatch.
			callAndStopOnFailure(ValidatePDPMetadataIssuer.class);
		} finally {
			eventLog.endBlock();
		}
		// Validate the PDP keys. signed_metadata may be MACed (Section 9.1.3), so a symmetric "oct"
		// key with an HMAC algorithm is allowed: only private asymmetric key material is rejected,
		// and an HMAC algorithm is not reported as unusable.
		call(new ValidateJwksSequence("config", "pdp.jwks", "PDP signing keys", "RFC7517-4")
			.replace(EnsureJwksHasNoPrivateOrSymmetricKeyMaterial.class, condition(EnsureJwksHasNoPrivateAsymmetricKeyMaterial.class).requirement("RFC7517-9.2"))
			.replace(WarnOnUnusableJwksKeys.class, condition(WarnOnUnusableAuthzenPdpJwksKeys.class)
				.skipIfObjectMissing("jwks_to_validate")
				.onFail(ConditionResult.WARNING)
				.dontStopOnFailure()
				.requirements("RFC7517-4", "AUTHZEN-9.1.3")));
		eventLog.startBlock("Verify signed metadata");
		try {
			call(sequence(ValidateDiscoverySignedMetadata.class));
			// The signed values become the PDP's metadata, so they are checked like the published
			// document before they are put in effect.
			callAndStopOnFailure(ExtractPDPSignedMetadataValues.class, "AUTHZEN-9.1.3");
			checkMetadataDocument("pdp_signed_metadata_values");
			callAndContinueOnFailure(ApplySignedMetadataPrecedence.class, ConditionResult.FAILURE, "AUTHZEN-9.1.3");
		} finally {
			eventLog.endBlock();
		}
	}

	/**
	 * Checks a set of PDP metadata values, the published document or the values carried in
	 * signed_metadata, against Section 9.2: a policy_decision_point identical to the PDP
	 * Identifier, no parameters without values, and only registered parameters. Signed metadata
	 * need not repeat policy_decision_point, so it is checked only where present.
	 */
	private void checkMetadataDocument(String envKey) {
		boolean signedValues = !"pdp".equals(envKey);
		env.mapKey("server", envKey);
		if (signedValues) {
			env.mapKey("pdp", envKey);
		}
		try {
			if (!signedValues || env.getElementFromObject(envKey, "policy_decision_point") != null) {
				// §9.2.3: "If these values are not identical, the data contained in the response MUST
				// NOT be used." Everything after this point runs against the endpoints that response
				// names, so this is stop-on-failure.
				callAndStopOnFailure(new EnsurePolicyDecisionPointMatchesIssuer(signedValues
					? EnsurePolicyDecisionPointMatchesIssuer.SIGNED_METADATA
					: EnsurePolicyDecisionPointMatchesIssuer.PUBLISHED_DOCUMENT), "AUTHZEN-9.2.3");
			}
			callAndContinueOnFailure(EnsureDiscoveryMetadataParamsNotEmpty.class, ConditionResult.FAILURE, "AUTHZEN-9.2.2");
			callAndContinueOnFailure(WarnAuthzenMetadataUnknownMembers.class, ConditionResult.WARNING, "AUTHZEN-9.2.2", "AUTHZEN-12.1.3");
		} finally {
			if (signedValues) {
				env.unmapKey("pdp");
			}
			env.unmapKey("server");
		}
	}

	/**
	 * Checks the endpoints and capabilities in the {@code pdp} environment object, the values the
	 * test uses: from the test configuration, or the discovered metadata with any signed values
	 * applied.
	 */
	protected void checkPDPServerConfiguration(boolean fromDiscovery) {
		eventLog.startBlock("Check PDP Server Configuration");
		// make sure the server configuration passes some basic sanity checks
		env.mapKey("server", "pdp");
		try {
			if (fromDiscovery) {
				callAndContinueOnFailure(CheckPDPServerConfiguration.class, ConditionResult.FAILURE, "AUTHZEN-9.1.1", "AUTHZEN-10.1");
			} else {
				// No metadata is fetched, so Section 9.1.1 does not apply; Section 10.1 requires the
				// endpoints the test configuration names to be https.
				callAndContinueOnFailure(CheckStaticPDPServerConfiguration.class, ConditionResult.FAILURE, "AUTHZEN-10.1");
			}
			callAndContinueOnFailure(EnsureMetadataCapabilitiesValid.class, ConditionResult.FAILURE, "AUTHZEN-9.1.2");
			callAndContinueOnFailure(WarnAuthzenCapabilitiesNotAuthzenUrns.class, ConditionResult.WARNING, "AUTHZEN-9.1.2", "AUTHZEN-12.4");
		} finally {
			env.unmapKey("server");
			eventLog.endBlock();
		}
	}
}
