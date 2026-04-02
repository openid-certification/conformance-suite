package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureX5cHeaderPresentForSdJwtCredential;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialJWTExp;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateCredentialJWTIssIsHttpsUri;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateCredentialValidityByStatusListIfPresent;
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

	/**
	 * Non-HAIP constructor.
	 *
	 * @param requiresCnf whether to validate the cnf (confirmation) claim is present and is a public key
	 */
	public ValidateSdJwtVcCredentialClaims(boolean requiresCnf) {
		this(requiresCnf, false);
	}

	@Override
	public void evaluate() {
		// as per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2 these must not be selectively disclosed
		callAndContinueOnFailure(ValidateCredentialJWTIssIsHttpsUri.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
		callAndContinueOnFailure(ValidateCredentialJWTIat.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2");
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
		callAndContinueOnFailure(ValidateCredentialValidityByStatusListIfPresent.class,
			ConditionResult.FAILURE, "OTSL-6.2");
		if (haip) {
			callAndContinueOnFailure(ValidateCredentialValidityInfoIsPresent.class,
				ConditionResult.WARNING, "HAIP-6.1-2.2");
			callAndContinueOnFailure(EnsureX5cHeaderPresentForSdJwtCredential.class,
				ConditionResult.FAILURE, "HAIP-6.1.1");
		}
	}
}
