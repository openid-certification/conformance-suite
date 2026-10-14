package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.lote.AbstractLoteCondition;
import net.openid.conformance.condition.client.lote.CallLoteEndpoint;
import net.openid.conformance.condition.client.lote.ExtractLoteTrustAnchors;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.ParseLote;
import net.openid.conformance.condition.client.lote.RegisterLote;
import net.openid.conformance.condition.client.lote.ValidateLoteSignature;
import net.openid.conformance.condition.client.lote.ValidateLoteSignerAgainstConfiguredCertificate;
import net.openid.conformance.condition.client.lote.ValidateLoteSignerCertificate;
import net.openid.conformance.condition.client.lote.ValidateLoteStructure;
import net.openid.conformance.condition.client.lote.WarnIfLoteSigningCertificateNotConfigured;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Registers the list of trusted entities (ETSI TS 119 602 LoTE) configured for one {@link LoteSlot},
 * fetches it, checks it and extracts its trust anchors into the slot's environment object. Every
 * step after registration is skipped when the slot's URL is not configured. Invoked once at
 * configure time; the chain checks against the extracted anchors happen later, in the conditions
 * that validate the certificate chains the slot is about.
 *
 * <p>Severity policy (two tiers), as for the VICAL: the list quality checks are WARNINGs — the
 * scheme operator is a third party, so a defect that does not prevent using the list (signer
 * certificate not matching the scheme information, a structural slip, an overdue NextUpdate)
 * should not fail the entity under test, and the anchors are still extracted. Defects that make
 * the list unusable (unfetchable, unparseable, signature does not verify, signer is not the
 * configured sealing certificate) are reported at the caller's severity, and
 * {@link ExtractLoteTrustAnchors} re-verifies the signature so a bad list yields no anchors.
 */
public class SetupLoteFromConfiguration extends AbstractConditionSequence {

	private final LoteSlot slot;
	private final ConditionResult unusableListSeverity;

	/**
	 * @param slot the list to set up
	 * @param unusableListSeverity severity for a configured list the suite cannot use. FAILURE where
	 *   the list is the trust source for the entity under test, so an unusable list means the
	 *   configured check cannot run; WARNING where the list only drives a pre-flight check of the
	 *   suite's own certificates, which is no verdict on that entity.
	 */
	public SetupLoteFromConfiguration(LoteSlot slot, ConditionResult unusableListSeverity) {
		this.slot = slot;
		this.unusableListSeverity = unusableListSeverity;
	}

	@Override
	public void evaluate() {
		call(exec().mapKey(AbstractLoteCondition.LOTE, slot.envKey).putString(RegisterLote.SLOT, slot.name()));
		callAndStopOnFailure(RegisterLote.class);

		call(condition(CallLoteEndpoint.class)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.onSkip(ConditionResult.INFO)
			.onFail(unusableListSeverity)
			.dontStopOnFailure()
			.requirements("ETSI119602-6.3.16"));

		// the list cannot be used from here on if it does not parse or verify: ExtractLoteTrustAnchors
		// re-checks the signature and fails at the same severity
		unusableListCheck(ParseLote.class, "ETSI119602-A.1");
		unusableListCheck(ValidateLoteSignature.class, "ETSI119602-6.8.0");
		qualityCheck(ValidateLoteSignerCertificate.class, "ETSI119602-6.8.0");

		call(condition(ValidateLoteSignerAgainstConfiguredCertificate.class)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.skipIfElementMissing("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(unusableListSeverity)
			.dontStopOnFailure()
			.requirements("ETSI119602-6.8.0"));
		call(condition(WarnIfLoteSigningCertificateNotConfigured.class)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements("ETSI119602-6.8.0"));

		qualityCheck(ValidateLoteStructure.class, "ETSI119602-6.3.0", "ETSI119602-6.4.3", "ETSI119602-6.6.0");

		call(condition(ExtractLoteTrustAnchors.class)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.onSkip(ConditionResult.INFO)
			.onFail(unusableListSeverity)
			.dontStopOnFailure()
			.requirements("ETSI119602-6.6.3.1"));

		call(exec().unmapKey(AbstractLoteCondition.LOTE).removeNativeValue(RegisterLote.SLOT));
	}

	private void unusableListCheck(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.onSkip(ConditionResult.INFO)
			.onFail(unusableListSeverity)
			.dontStopOnFailure()
			.requirements(requirements));
	}

	private void qualityCheck(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements(requirements));
	}
}
