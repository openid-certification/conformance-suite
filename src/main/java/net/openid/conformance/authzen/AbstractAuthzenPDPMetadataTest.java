package net.openid.conformance.authzen;

import com.google.gson.JsonElement;
import net.openid.conformance.authzen.condition.ApplySignedMetadataPrecedence;
import net.openid.conformance.authzen.condition.CheckPDPServerConfiguration;
import net.openid.conformance.authzen.condition.EnsureDiscoveryMetadataParamsNotEmpty;
import net.openid.conformance.authzen.condition.EnsureDiscoveryMetadataResponseValid;
import net.openid.conformance.authzen.condition.EnsureMetadataCapabilitiesValid;
import net.openid.conformance.authzen.condition.EnsurePDPJwksConfigured;
import net.openid.conformance.authzen.condition.EnsurePolicyDecisionPointMatchesIssuer;
import net.openid.conformance.authzen.condition.GetPDPDynamicServerConfiguration;
import net.openid.conformance.authzen.condition.ParseUsableAuthzenPdpJwksKeys;
import net.openid.conformance.authzen.condition.ValidateDiscoverySignedMetadata;
import net.openid.conformance.authzen.condition.ValidatePDPMetadataIssuer;
import net.openid.conformance.authzen.condition.WarnOnUnusableAuthzenPdpJwksKeys;
import net.openid.conformance.authzen.condition.WarnUnusedSignedMetadataIssuer;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.common.EnsureJwksHasNoPrivateAsymmetricKeyMaterial;
import net.openid.conformance.condition.common.EnsureJwksHasNoPrivateOrSymmetricKeyMaterial;
import net.openid.conformance.condition.common.ParseUsableJwksKeys;
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
	 * Fetches the metadata from the well-known URL derived from the configured PDP Identifier into
	 * the {@code pdp} environment object, validates the response and, when present, the
	 * signed_metadata JWT, and applies its values over the plain ones.
	 */
	protected void fetchAndValidateDiscoveryMetadata() {
		callAndStopOnFailure(GetPDPDynamicServerConfiguration.class);
		callAndContinueOnFailure(EnsureDiscoveryMetadataResponseValid.class, ConditionResult.FAILURE, "AUTHZEN-9.2.2");
		// signed_metadata is OPTIONAL (§9.1.3): only when the PDP metadata carries
		// it do we validate it, verify its signature against the configured PDP JWK
		// Set, and apply its precedence over the plain JSON metadata so the
		// downstream checks validate the signed (authoritative) values.
		// The gate inspects the raw JsonElement rather than reading it as a string:
		// a non-string signed_metadata (number, object, array) is a protocol error that
		// ExtractPDPSignedMetadata is there to report, so it has to reach the sequence
		// instead of throwing an UnexpectedTypeException out of the gate itself.
		JsonElement signedMetadataElement = env.getElementFromObject("pdp", "signed_metadata");
		if(signedMetadataElement != null && !signedMetadataElement.isJsonNull()) {
			eventLog.startBlock("Verify signed metadata");
			callAndStopOnFailure(EnsurePDPJwksConfigured.class, "RFC7517-4");
			// Check the expected issuer before it is compared against the JWT, so a malformed
			// value is reported as the configuration error it is rather than as a PDP mismatch.
			callAndStopOnFailure(ValidatePDPMetadataIssuer.class, "AUTHZEN-9.1.3", "AUTHZEN-11.8", "RFC7519-4.1.1");
			// Validate the PDP keys. signed_metadata may be MACed (Section 9.1.3), so a symmetric "oct"
			// key with an HMAC algorithm is allowed: only private asymmetric key material is rejected,
			// such a key is parsed and its length checked, and an HMAC algorithm is not reported as
			// unusable.
			call(new ValidateJwksSequence("config", "pdp.jwks", "PDP signing keys", "RFC7517-4")
				.replace(EnsureJwksHasNoPrivateOrSymmetricKeyMaterial.class, condition(EnsureJwksHasNoPrivateAsymmetricKeyMaterial.class).requirement("RFC7517-9.2"))
				.replace(ParseUsableJwksKeys.class, condition(ParseUsableAuthzenPdpJwksKeys.class)
					.skipIfObjectMissing("jwks_to_validate")
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure()
					.requirements("RFC7517-4", "RFC7518-3.2"))
				.replace(WarnOnUnusableJwksKeys.class, condition(WarnOnUnusableAuthzenPdpJwksKeys.class)
					.skipIfObjectMissing("jwks_to_validate")
					.onFail(ConditionResult.WARNING)
					.dontStopOnFailure()
					.requirements("RFC7517-4", "AUTHZEN-9.1.3")));
			call(sequence(ValidateDiscoverySignedMetadata.class));
			callAndContinueOnFailure(ApplySignedMetadataPrecedence.class, ConditionResult.FAILURE, "AUTHZEN-9.1.3");
			eventLog.endBlock();
		} else {
			// A 'Signed Metadata Issuer' only applies to a signed_metadata JWT. Ignoring it in
			// silence would leave a tester who expected signed metadata reading a clean log.
			callAndContinueOnFailure(WarnUnusedSignedMetadataIssuer.class, ConditionResult.WARNING, "AUTHZEN-9.1.3");
		}
	}

	/**
	 * Checks the endpoints and capabilities in the {@code pdp} environment object. With
	 * {@code fromDiscovery}, also checks the metadata document's own requirements.
	 */
	protected void checkPDPServerConfiguration(boolean fromDiscovery) {
		eventLog.startBlock("Check PDP Server Configuration");
		// make sure the server configuration passes some basic sanity checks
		env.mapKey("server", "pdp");
		callAndContinueOnFailure(CheckPDPServerConfiguration.class, ConditionResult.FAILURE, "AUTHZEN-9.1.1");
		if (fromDiscovery) {
			// §9.2.3: "If these values are not identical, the data contained in the response MUST NOT
			// be used." Everything after this point runs against the endpoints that response names, so
			// this is stop-on-failure, matching how ValidateDiscoverySignedMetadata treats a
			// signed_metadata JWT whose values must not be applied.
			callAndStopOnFailure(EnsurePolicyDecisionPointMatchesIssuer.class, "AUTHZEN-9.2.3");
			callAndContinueOnFailure(EnsureDiscoveryMetadataParamsNotEmpty.class, ConditionResult.FAILURE, "AUTHZEN-9.2.2");
		}
		callAndContinueOnFailure(EnsureMetadataCapabilitiesValid.class, ConditionResult.FAILURE, "AUTHZEN-9.1.2");
		env.unmapKey("server");
		eventLog.endBlock();
	}
}
