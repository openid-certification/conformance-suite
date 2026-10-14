package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.lote.AbstractLoteCondition;
import net.openid.conformance.condition.client.lote.CallLoteEndpoint;
import net.openid.conformance.condition.client.lote.ExtractLoteFromEndpointResponse;
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
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Registers the list of trusted entities (ETSI TS 119 602 LoTE) configured for one {@link LoteSlot},
 * fetches it, checks it and extracts its trust anchors into the slot's environment object. Each
 * step is skipped when the one it depends on did not deliver (no response, no fetched value, no
 * parsed payload), so an unusable list is reported once; {@link #forConfigured} leaves out the slots
 * whose URL is not configured altogether. Invoked once at configure time; the chain checks
 * against the extracted anchors happen later, in the conditions that validate the certificate
 * chains the slot is about.
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

	/** The compact JWS as fetched, stored by {@link ExtractLoteFromEndpointResponse}. */
	private static final String VALUE = "value";
	/** The parsed {@code LoTE} payload, stored by {@link ParseLote}. */
	private static final String PAYLOAD = "payload";

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

	/**
	 * Sets up those of {@code slots} whose URL the test configuration names, so a module whose
	 * configuration names no list logs nothing about lists.
	 *
	 * @param env the environment holding the test configuration at the time of the call
	 */
	public static ConditionSequence forConfigured(Environment env, ConditionResult unusableListSeverity, LoteSlot... slots) {
		List<LoteSlot> configured = Arrays.stream(slots).filter(slot -> slot.isConfigured(env)).toList();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				for (LoteSlot slot : configured) {
					call(new SetupLoteFromConfiguration(slot, unusableListSeverity));
				}
			}
		};
	}

	@Override
	public void evaluate() {
		call(exec().mapKey(AbstractLoteCondition.LOTE, slot.envKey).putString(RegisterLote.SLOT, slot.name()));
		callAndStopOnFailure(RegisterLote.class);

		unusableListCheck(condition(CallLoteEndpoint.class).skipIfObjectMissing(AbstractLoteCondition.LOTE), "ETSI119602-6.3.16");
		call(exec().mapKey("endpoint_response", CallLoteEndpoint.RESPONSE));
		unusableListCheck(condition(EnsureHttpStatusCodeIs200.class).skipIfObjectMissing(CallLoteEndpoint.RESPONSE), "ETSI119602-6.3.16");
		call(exec().unmapKey("endpoint_response"));
		unusableListCheck(condition(ExtractLoteFromEndpointResponse.class).skipIfObjectMissing(CallLoteEndpoint.RESPONSE), "ETSI119602-6.3.16");

		// the list cannot be used from here on if it does not parse or verify: ExtractLoteTrustAnchors
		// re-checks the signature and fails at the same severity
		unusableListCheck(ParseLote.class, VALUE, "ETSI119602-A.1");
		unusableListCheck(ValidateLoteSignature.class, PAYLOAD, "ETSI119602-6.8.0");
		qualityCheck(ValidateLoteSignerCertificate.class, "ETSI119602-6.8.0");

		unusableListCheck(ValidateLoteSignerAgainstConfiguredCertificate.class, PAYLOAD, "ETSI119602-6.8.0");
		call(condition(WarnIfLoteSigningCertificateNotConfigured.class)
			.skipIfObjectMissing(AbstractLoteCondition.LOTE)
			.skipIfStringPresent(WarnIfLoteSigningCertificateNotConfigured.REPORTED)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements("ETSI119602-6.8.0"));

		qualityCheck(ValidateLoteStructure.class, "ETSI119602-6.3.0", "ETSI119602-6.4.3", "ETSI119602-6.6.0");

		unusableListCheck(ExtractLoteTrustAnchors.class, PAYLOAD, "ETSI119602-6.6.3.1");

		call(exec().unmapKey(AbstractLoteCondition.LOTE).removeNativeValue(RegisterLote.SLOT).removeObject(CallLoteEndpoint.RESPONSE));
	}

	/** A check that needs the fetched list ({@link #VALUE}) or its parsed payload ({@link #PAYLOAD}). */
	private void unusableListCheck(Class<? extends Condition> conditionClass, String requiredElement, String... requirements) {
		unusableListCheck(condition(conditionClass).skipIfElementMissing(AbstractLoteCondition.LOTE, requiredElement), requirements);
	}

	/** A check whose failure makes the list unusable, with its skip rule already set. */
	private void unusableListCheck(ConditionCallBuilder check, String... requirements) {
		call(check
			.onSkip(ConditionResult.INFO)
			.onFail(unusableListSeverity)
			.dontStopOnFailure()
			.requirements(requirements));
	}

	private void qualityCheck(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.skipIfElementMissing(AbstractLoteCondition.LOTE, PAYLOAD)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements(requirements));
	}
}
