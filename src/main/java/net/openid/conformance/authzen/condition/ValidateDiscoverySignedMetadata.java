package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Structurally validate the optional {@code signed_metadata} JWT in the PDP
 * discovery metadata (certification profile
 * https://github.com/openid/authzen/issues/433 §6.4 / §6.5).
 *
 * <p>When {@code signed_metadata} is present it MUST be a JWS-signed (or MACed)
 * JWT carrying an {@code iss} claim. This condition checks that it parses as a
 * compact JWS, that the algorithm is a real signing algorithm (not {@code none} and
 * not an unregistered name), that the {@code iss} claim is present, and that any
 * {@code iat} / {@code exp} / {@code nbf} claims are valid; it stores the decoded
 * claims in {@code pdp_signed_metadata.claims} for
 * {@link ApplySignedMetadataPrecedence}. An expired or not-yet-valid JWT aborts the
 * test — its values must never be applied — whereas an out-of-range {@code iat} is
 * reported as a failure without stopping the run.
 *
 * <p>{@code iss} identifies the party attesting to the metadata, which AuthZEN
 * (§11.8) allows to differ from the PDP itself, so the issuer it is checked against is
 * configurable — see {@link ValidatePDPSignedMetadataIss};
 * {@link EnsurePolicyDecisionPointMatchesIssuer} separately enforces the §9.2.3 rule
 * that {@code policy_decision_point} matches the discovery URL.
 *
 * <p>The cryptographic signature is verified separately by
 * {@link VerifyAuthzenSignedMetadataSignature} against the PDP key(s) supplied in
 * the test configuration (the AuthZEN metadata defines no {@code jwks_uri}, so the
 * trusted verification key is provided out of band).
 */
public class ValidateDiscoverySignedMetadata extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(ExtractPDPSignedMetadata.class, "AUTHZEN-9.1.3");
		callAndStopOnFailure(ValidatePDPSignedMetadataAlg.class, "AUTHZEN-9.1.3");
		callAndStopOnFailure(VerifyAuthzenSignedMetadataSignature.class, "AUTHZEN-9.1.3", "AUTHZEN-9.2.3");
		callAndStopOnFailure(ValidatePDPSignedMetadataIss.class, "AUTHZEN-9.1.3", "RFC7519-4.1.1");
		// `iat` is continue-on-failure: RFC 7519 gives it no validity role — a JWT is not unusable
		// because it was issued long ago, and the lower bound checked here is a sanity heuristic for
		// values that are not unix seconds. Report it as a failure, but let the rest of the test run.
		callAndContinueOnFailure(ValidatePDPSignedMetadataIat.class, Condition.ConditionResult.FAILURE, "RFC7519-4.1.6");
		// `exp` and `nbf` are stop-on-failure: a signed_metadata JWT that has expired or is not yet
		// valid MUST NOT have its values applied over the plain discovery metadata, and the endpoints
		// it names MUST NOT be used for the rest of the test.
		callAndStopOnFailure(ValidatePDPSignedMetadataExp.class, "RFC7519-4.1.4");
		callAndStopOnFailure(ValidatePDPSignedMetadataNbf.class, "RFC7519-4.1.5");
		callAndContinueOnFailure(EnsurePDPSignedMetadataDoesNotContainSignedMetadata.class, Condition.ConditionResult.WARNING, "AUTHZEN-9.1.3");
	}

}
