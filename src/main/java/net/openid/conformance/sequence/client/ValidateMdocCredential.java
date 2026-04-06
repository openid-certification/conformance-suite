package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.condition.client.ValidateMdocMsoRevocationMechanism;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Shared mdoc credential validation checks.
 * Used by both VCI issuer tests (after ParseMdocCredentialFromVCIIssuance)
 * and VP wallet tests (after ParseCredentialAsMdoc).
 *
 * Pass {@code isIssuance=true} for VCI issuance (adds issuerAuth signature validation,
 * since VP validates signatures internally via DeviceResponseParser).
 */
public class ValidateMdocCredential extends AbstractConditionSequence {

	private final boolean isIssuance;
	private final boolean haip;

	/**
	 * @param isIssuance true for VCI issuance (adds issuerAuth signature check),
	 *                   false for VP presentation (signature checked during parsing)
	 * @param haip whether to include HAIP-specific credential checks
	 */
	public ValidateMdocCredential(boolean isIssuance, boolean haip) {
		this.isIssuance = isIssuance;
		this.haip = haip;
	}

	@Override
	public void evaluate() {
		if (isIssuance) {
			callAndContinueOnFailure(ValidateMdocIssuerSignedSignature.class,
				ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2");
		}
		if (haip) {
			callAndContinueOnFailure(ValidateMdocMsoRevocationMechanism.class,
				ConditionResult.FAILURE, "HAIP-5.3.1");
		}
	}
}
