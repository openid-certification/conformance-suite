package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AbstractRevocationListCwtCondition;
import net.openid.conformance.condition.client.EnsureMdocNotRevoked;
import net.openid.conformance.condition.client.ExtractMdocRevocationStatus;
import net.openid.conformance.condition.client.EnsureContentTypeMdocRevocationListCwt;
import net.openid.conformance.condition.client.EnsureMdocDocTypeMatchesCredentialConfiguration;
import net.openid.conformance.condition.client.FetchMdocRevocationList;
import net.openid.conformance.condition.client.ValidateMdocRevocationListSignerCertificateProfile;
import net.openid.conformance.condition.client.ValidateMdocRevocationListCwtFormat;
import net.openid.conformance.condition.client.VerifyMdocRevocationListCwtSignature;
import net.openid.conformance.condition.client.ValidateMdocDsCertificateChain;
import net.openid.conformance.condition.client.ValidateMdocDsCertificateKeyUsage;
import net.openid.conformance.condition.client.ValidateMdocDsCertificateMatchesIssuingCountry;
import net.openid.conformance.condition.client.ValidateMdocDsCertificateProfile;
import net.openid.conformance.condition.client.ValidateMdocIssuerChainAgainstVical;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedItemDigests;
import net.openid.conformance.condition.client.ValidateMdocTrustAnchorIacaCertificateProfile;
import net.openid.conformance.condition.client.ValidateMdocIssuerSignedSignature;
import net.openid.conformance.condition.client.ValidateMdocMsoRevocationMechanism;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;

/**
 * Shared mdoc credential validation checks.
 * Used by both VCI issuer tests (after ParseMdocCredentialFromVCIIssuance)
 * and VP wallet tests (after ParseCredentialAsMdoc).
 *
 * Pass {@code isIssuance=true} for VCI issuance (adds issuerAuth signature, IssuerSignedItem
 * digest and docType checks, since VP validates signatures and digests internally via
 * DeviceResponseParser and checks docType against the DCQL query).
 */
public class ValidateMdocCredential extends AbstractConditionSequence {

	private final boolean isIssuance;
	private final boolean haip;

	/**
	 * @param isIssuance true for VCI issuance (adds issuerAuth signature, digest and docType checks),
	 *                   false for VP presentation (checked during parsing / against the DCQL query)
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
			callAndContinueOnFailure(ValidateMdocIssuerSignedItemDigests.class,
				ConditionResult.FAILURE, "ISO18013-5-9.1.2.4");
			callAndContinueOnFailure(EnsureMdocDocTypeMatchesCredentialConfiguration.class,
				ConditionResult.FAILURE, "OID4VCI-1FINALA-A.2.2");
		}
		// ISO 18013-5 Annex B DS certificate profile checks on the x5chain leaf; warnings, as
		// they are ISO profile conformance rather than OID4VP/OID4VCI normative requirements
		callAndContinueOnFailure(ValidateMdocDsCertificateKeyUsage.class,
			ConditionResult.WARNING, "ISO18013-5-B.1.4");
		callAndContinueOnFailure(ValidateMdocDsCertificateProfile.class,
			ConditionResult.WARNING, "ISO18013-5-B.1.4");
		callAndContinueOnFailure(ValidateMdocDsCertificateMatchesIssuingCountry.class,
			ConditionResult.WARNING, "ISO18013-5-B.1.4");
		callAndContinueOnFailure(ValidateMdocTrustAnchorIacaCertificateProfile.class,
			ConditionResult.WARNING, "ISO18013-5-B.1.2");
		if (haip) {
			callAndContinueOnFailure(ValidateMdocMsoRevocationMechanism.class,
				ConditionResult.FAILURE, "HAIP-5.3.1");
		}
		validateMsoRevocationList();
		// Skipped unless a VICAL is configured. For issuance the issuer under test owns its IACA,
		// so an unlisted IACA is a FAILURE; for presentation the wallet under test is not
		// responsible for its credentials' provenance, so it is only a WARNING.
		call(condition(ValidateMdocIssuerChainAgainstVical.class)
			.skipIfObjectsMissing("vical")
			.onSkip(ConditionResult.INFO)
			.onFail(isIssuance ? ConditionResult.FAILURE : ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements("ISO18013-5-C.1.7.1"));
		// PKIX-validate the issuerAuth x5chain against the 'Credential Trust Anchor' as the IACA
		// root (the same config field the SD-JWT x5c check uses), mirroring the SD-JWT VC x5c
		// chain validation. A configured VICAL supersedes the trust anchor, in which case the
		// condition only performs the trust-anchor-independent chain checks. HAIP requires a
		// configured trust anchor, so outside HAIP this is skipped when none is configured.
		ConditionCallBuilder chainValidation = condition(ValidateMdocDsCertificateChain.class)
			.onFail(haip || isIssuance ? ConditionResult.FAILURE : ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements(haip
				? new String[] { "HAIP-6.1.1", "ISO18013-5-9.3.1" }
				: new String[] { "ISO18013-5-9.3.1" });
		if (!haip) {
			chainValidation.skipIfStringsMissing("credential_trust_anchor_pem")
				.onSkip(ConditionResult.INFO);
		}
		call(chainValidation);
	}

	/**
	 * ISO/IEC 18013-5 12.3.6: when the MSO carries a status_list element, fetch the referenced
	 * MSO revocation list and check it. The Status structure is optional ("An MSO may contain the
	 * Status structure"), so {@link FetchMdocRevocationList} logs a skip and writes no
	 * {@code mdoc_revocation_list_token} when it is absent, which skips everything below.
	 *
	 * <p>Retrieval and format problems, including a list whose status cannot be read, are only a
	 * warning for presentation, where the wallet under test does not control the credential's
	 * issuer; a credential whose status reads as revoked is always a failure. The Table B.9
	 * certificate profile is a warning either way, matching how the document signer certificate
	 * profile checks are called above.
	 */
	private void validateMsoRevocationList() {
		ConditionResult retrievalSeverity = isIssuance ? ConditionResult.FAILURE : ConditionResult.WARNING;

		callAndContinueOnFailure(FetchMdocRevocationList.class, retrievalSeverity, "ISO18013-5-12.3.6.2");
		checkFetchedList(EnsureContentTypeMdocRevocationListCwt.class, ConditionResult.WARNING,
			"ISO18013-5-12.3.6.5", "OTSL-8.2");
		checkFetchedList(ValidateMdocRevocationListCwtFormat.class, retrievalSeverity,
			"ISO18013-5-12.3.6.3");
		checkFetchedList(VerifyMdocRevocationListCwtSignature.class, retrievalSeverity,
			"ISO18013-5-12.3.6.3");
		checkFetchedList(ValidateMdocRevocationListSignerCertificateProfile.class, ConditionResult.WARNING,
			"ISO18013-5-B.9");
		checkFetchedList(ExtractMdocRevocationStatus.class, retrievalSeverity,
			"ISO18013-5-12.3.6.1");
		call(condition(EnsureMdocNotRevoked.class)
			.skipIfStringsMissing(AbstractRevocationListCwtCondition.ENV_STATUS)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.dontStopOnFailure()
			.requirements("ISO18013-5-12.3.6.1"));
	}

	/**
	 * Calls a check on a fetched MSO revocation list, skipping it when
	 * {@link FetchMdocRevocationList} stored no token because the MSO carries no Status structure.
	 */
	private void checkFetchedList(Class<? extends Condition> conditionClass, ConditionResult onFail,
			String... requirements) {
		call(condition(conditionClass)
			.skipIfStringsMissing(AbstractRevocationListCwtCondition.ENV_TOKEN)
			.onSkip(ConditionResult.INFO)
			.onFail(onFail)
			.dontStopOnFailure()
			.requirements(requirements));
	}
}
