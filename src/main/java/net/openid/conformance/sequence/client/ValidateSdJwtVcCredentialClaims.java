package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialJWTExp;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateSdJwtCredentialX5cCertificateChain;
import net.openid.conformance.condition.client.ValidateSdJwtDisclosureSaltsAreUnique;
import net.openid.conformance.condition.client.ValidateCredentialJWTNbf;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateCredentialStatusList;
import net.openid.conformance.condition.client.ValidateCredentialStatusListForHaip;
import net.openid.conformance.condition.client.ValidateCredentialValidityInfoIsPresent;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Standard SD-JWT VC credential claim checks per SD-JWT VC spec section 3.2.
 * Used by both VCI issuer tests and VP wallet tests.
 *
 * Pass {@code requiresCnf=true} for presentations (wallet always proves possession),
 * or {@code false} when cryptographic binding is not required (some VCI issuance flows).
 *
 * Pass {@code haip=true} to include HAIP-specific checks (validity info presence, x5c header).
 */
public class ValidateSdJwtVcCredentialClaims extends AbstractConditionSequence {

	private final boolean requiresCnf;
	private final boolean haip;

	/**
	 * @param requiresCnf whether to validate the cnf (confirmation) claim is present and is a public key
	 * @param haip whether to include HAIP-specific credential checks
	 */
	public ValidateSdJwtVcCredentialClaims(boolean requiresCnf, boolean haip) {
		this.requiresCnf = requiresCnf;
		this.haip = haip;
	}

	@Override
	public void evaluate() {
		// as per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2 these must not be selectively disclosed
		// Note: 'iss' validation is not included here. SD-JWT VC 3.2.2.2 says 'iss' is OPTIONAL and
		// HAIP dropped the 'iss' MUST requirement: https://github.com/openid/OpenID4VC-HAIP/pull/277
		callAndContinueOnFailure(ValidateCredentialJWTIat.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2");
		callAndContinueOnFailure(ValidateCredentialJWTNbf.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
		callAndContinueOnFailure(ValidateCredentialJWTExp.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
		callAndContinueOnFailure(ValidateCredentialJWTHeaderTyp.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.1");
		callAndContinueOnFailure(ValidateCredentialJWTVct.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
		if (requiresCnf) {
			callAndContinueOnFailure(ValidateCredentialCnfJwkIsPublicKey.class,
				ConditionResult.FAILURE, "SDJWT-4.1.2");
		}
		callAndContinueOnFailure(ValidateSdJwtDisclosureSaltsAreUnique.class,
			ConditionResult.FAILURE, "SDJWT-4.2.1");
		if (haip) {
			callAndContinueOnFailure(ValidateSdJwtCredentialX5cCertificateChain.class,
				ConditionResult.FAILURE, "HAIP-6.1.1");
			callAndContinueOnFailure(ValidateCredentialStatusListForHaip.class,
				ConditionResult.FAILURE, "OTSL-6.2", "HAIP-6.1");
			callAndContinueOnFailure(ValidateCredentialValidityInfoIsPresent.class,
				ConditionResult.WARNING, "HAIP-6.1-2.2");
		} else {
			callAndContinueOnFailure(ValidateCredentialStatusList.class,
				ConditionResult.FAILURE, "OTSL-6.2");
		}
	}
}
