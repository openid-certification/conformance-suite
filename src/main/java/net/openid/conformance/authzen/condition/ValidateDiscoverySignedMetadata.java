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
 * {@code exp} / {@code nbf} claims are valid; it stores the decoded
 * claims in {@code pdp_signed_metadata.claims} for
 * {@link ApplySignedMetadataPrecedence}. An expired or not-yet-valid JWT aborts the
 * test — its values must never be applied — whereas an implausible {@code iat}, such as
 * one in the future, is reported as a warning. A time claim that is not a number, or an
 * {@code iss} that is not a string, fails {@link ExtractPDPSignedMetadata}, whose JWT
 * parser rejects them.
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
		// `exp` and `nbf` are stop-on-failure: a signed_metadata JWT that has expired or is not yet
		// valid MUST NOT have its values applied over the plain discovery metadata, and the endpoints
		// it names MUST NOT be used for the rest of the test.
		callAndStopOnFailure(ValidatePDPSignedMetadataExp.class, "RFC7519-4.1.4");
		callAndStopOnFailure(ValidatePDPSignedMetadataNbf.class, "RFC7519-4.1.5");
		// RFC 7519 requires iat to be a NumericDate, which ExtractPDPSignedMetadata's parser checks,
		// but gives it no validity role, so an iat in the future is only flagged here.
		callAndContinueOnFailure(CheckPDPSignedMetadataTimeClaimsArePlausible.class, Condition.ConditionResult.WARNING, "RFC7519-2");
		callAndContinueOnFailure(EnsurePDPSignedMetadataDoesNotContainSignedMetadata.class, Condition.ConditionResult.WARNING, "AUTHZEN-9.1.3");
	}

}
