package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckCredentialStatus;
import net.openid.conformance.condition.client.ClearStatusListTokenEndpointResponse;
import net.openid.conformance.condition.client.EnsureContentTypeStatusListJwt;
import net.openid.conformance.condition.client.ExtractStatusListTokenFromStatusListTokenEndpointResponse;
import net.openid.conformance.condition.client.FetchStatusListToken;
import net.openid.conformance.condition.client.RegisterCredentialTrustAnchor;
import net.openid.conformance.condition.client.RegisterStatusListTrustAnchor;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.WarnIfSdJwtDigestAlgorithmNotSha256;
import net.openid.conformance.condition.client.WarnIfUnexpectedFieldsInCredentialCnf;
import net.openid.conformance.condition.client.WarnIfUnknownFieldsInCredentialCnfJwk;
import net.openid.conformance.condition.client.ValidateCredentialJWTExp;
import net.openid.conformance.condition.client.ValidateCredentialJWTHeaderTyp;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateSdJwtCredentialX5cCertificateChain;
import net.openid.conformance.condition.client.ValidateSdJwtDisclosureSaltsAreUnique;
import net.openid.conformance.condition.client.ValidateCredentialJWTNbf;
import net.openid.conformance.condition.client.ValidateCredentialJWTVct;
import net.openid.conformance.condition.client.ValidateCredentialValidityInfoIsPresent;
import net.openid.conformance.condition.client.ValidateStatusListTokenX5cCertificateChain;
import net.openid.conformance.condition.client.VerifyStatusListTokenSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Standard SD-JWT VC credential claim checks per SD-JWT VC spec section 3.2.
 * Used by both VCI issuer tests and VP wallet tests.
 *
 * Pass {@code requiresCnf=true} for presentations (wallet always proves possession),
 * or {@code false} when cryptographic binding is not required (some VCI issuance flows).
 *
 * Pass {@code haip=true} to include HAIP-specific checks (validity info presence, x5c header).
 * Outside HAIP the x5c chain checks run, as warnings, when the tester configured a trust source
 * for the credential or its status list.
 */
public class ValidateSdJwtVcCredentialClaims extends AbstractConditionSequence {

	private final boolean requiresCnf;
	private final boolean haip;
	private final boolean isIssuance;

	/**
	 * @param requiresCnf whether to validate the cnf (confirmation) claim is present and is a public key
	 * @param haip whether to include HAIP-specific credential checks
	 * @param isIssuance true for VCI issuance, where the credential's issuer is the implementation under
	 *     test, so a certificate chain the configured trust source rejects is a failure rather than a warning
	 */
	public ValidateSdJwtVcCredentialClaims(boolean requiresCnf, boolean haip, boolean isIssuance) {
		this.requiresCnf = requiresCnf;
		this.haip = haip;
		this.isIssuance = isIssuance;
	}

	@Override
	public void evaluate() {
		// as per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2 these must not be selectively disclosed
		// Note: 'iss' validation is not included here. SD-JWT VC 3.2.2.2 says 'iss' is OPTIONAL and
		// HAIP dropped the 'iss' MUST requirement: https://github.com/openid/OpenID4VC-HAIP/pull/277
		callAndContinueOnFailure(ValidateCredentialJWTIat.class,
			ConditionResult.FAILURE, "SDJWTVC-3.2.2.2");
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
			callAndContinueOnFailure(WarnIfUnexpectedFieldsInCredentialCnf.class,
				ConditionResult.WARNING, "SDJWT-4.1.2");
			callAndContinueOnFailure(WarnIfUnknownFieldsInCredentialCnfJwk.class,
				ConditionResult.WARNING, "SDJWT-4.1.2");
		}
		callAndContinueOnFailure(ValidateSdJwtDisclosureSaltsAreUnique.class,
			ConditionResult.FAILURE, "SDJWT-4.2.1");
		// Clear any status list response carried over from a previous credential
		// so the content-type check below doesn't see stale state.
		callAndContinueOnFailure(ClearStatusListTokenEndpointResponse.class,
			ConditionResult.FAILURE);
		callAndContinueOnFailure(FetchStatusListToken.class,
			ConditionResult.FAILURE, "OTSL-6.2");
		callAndContinueOnFailure(ExtractStatusListTokenFromStatusListTokenEndpointResponse.class,
			ConditionResult.FAILURE, "OTSL-6.2");
		if (haip) {
			callAndContinueOnFailure(ValidateSdJwtCredentialX5cCertificateChain.class,
				ConditionResult.FAILURE, "HAIP-6.1.1");
			callAndContinueOnFailure(ValidateStatusListTokenX5cCertificateChain.class,
				ConditionResult.FAILURE, "OTSL-6.2", "HAIP-6.1");
			callAndContinueOnFailure(ValidateCredentialValidityInfoIsPresent.class,
				ConditionResult.WARNING, "HAIP-6.1-2");
			// valid, but a HAIP verifier need only support sha-256, so it may reject the credential
			callAndContinueOnFailure(WarnIfSdJwtDigestAlgorithmNotSha256.class,
				ConditionResult.WARNING, "HAIP-8");
		} else {
			// Outside HAIP the credential and its status list are trusted through whatever the tester
			// configured: the x5c chains are validated when a trust anchor PEM or a list of trusted
			// entities is set for them (SD-JWT VC 3.5, the X.509 issuer signature mechanism), and the
			// status list must be signed with the credential's key, a key of the issuer's JWK set, or an
			// x5c chain that the status list trust source validates.
			ConditionResult chainSeverity = isIssuance ? ConditionResult.FAILURE : ConditionResult.WARNING;
			call(condition(ValidateSdJwtCredentialX5cCertificateChain.class)
				.skipIfStringsMissing(RegisterCredentialTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED)
				.onSkip(ConditionResult.INFO)
				.onFail(chainSeverity)
				.dontStopOnFailure()
				.requirements("SDJWTVC-3.5"));
			call(condition(ValidateStatusListTokenX5cCertificateChain.class)
				.skipIfStringsMissing(RegisterStatusListTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED)
				.onSkip(ConditionResult.INFO)
				.onFail(chainSeverity)
				.dontStopOnFailure()
				.requirements("OTSL-6.2", "OTSL-11.3"));
			callAndContinueOnFailure(VerifyStatusListTokenSignature.class,
				ConditionResult.FAILURE, "OTSL-6.2");
		}
		callAndContinueOnFailure(CheckCredentialStatus.class,
			ConditionResult.FAILURE, "OTSL-6.2");
		callAndContinueOnFailure(EnsureContentTypeStatusListJwt.class,
			ConditionResult.FAILURE, "OTSL-8.2");
	}
}
