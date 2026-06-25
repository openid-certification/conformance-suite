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
 * compact JWS, that the algorithm is a real signing algorithm (not {@code none}),
 * and that the {@code iss} claim is present, and stores the decoded claims in
 * {@code pdp_signed_metadata.claims} for {@link ApplySignedMetadataPrecedence}.
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
		callAndContinueOnFailure(ValidatePDPSignedMetadataAlg.class, Condition.ConditionResult.FAILURE, "AUTHZEN-9.1.3");
		callAndStopOnFailure(VerifyAuthzenSignedMetadataSignature.class, "AUTHZEN-9.1.3");
		callAndContinueOnFailure(ValidatePDPSignedMetadataIss.class, Condition.ConditionResult.FAILURE, "AUTHZEN-9.1.3");
		callAndContinueOnFailure(ValidatePDPSignedMetadataIat.class, Condition.ConditionResult.FAILURE, "AUTHZEN-9.1.3");
		callAndContinueOnFailure(ValidatePDPSignedMetadataExp.class, Condition.ConditionResult.FAILURE, "AUTHZEN-9.1.3");
		callAndContinueOnFailure(ValidatePDPSignedMetadataNbf.class, Condition.ConditionResult.FAILURE, "AUTHZEN-9.1.3");
		callAndContinueOnFailure(EnsurePDPSignedMetadataDoesNotContainSignedMetadata.class, Condition.ConditionResult.WARNING, "AUTHZEN-9.1.3");
	}

}
