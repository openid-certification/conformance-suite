package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallRicalEndpoint;
import net.openid.conformance.condition.client.DumpRicalCborDiagnostics;
import net.openid.conformance.condition.client.ParseRical;
import net.openid.conformance.condition.client.RegisterRical;
import net.openid.conformance.condition.client.ValidateRicalSignature;
import net.openid.conformance.condition.client.ValidateRicalSignerCertificate;
import net.openid.conformance.condition.client.ValidateRicalStructure;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Registers the optionally configured RICAL (inline or fetched from a URL), then parses and
 * validates it. All steps are skipped when no RICAL is configured. Invoked once at configure
 * time; the per-request chain check against the registered RICAL happens later, when the
 * verifier's request object is validated.
 *
 * Severity policy (two tiers, mirroring {@link SetupVicalFromConfiguration}): the RICAL quality
 * checks here are WARNINGs — the RICAL provider is a third party, so defects that don't prevent
 * using the list (expired signer certificate, structure problems, stale notAfter) should not
 * fail the entity under test, and the chain check still evaluates against such a list. Defects
 * that make the list unusable (unparseable, COSE signature does not verify) additionally
 * surface in {@link net.openid.conformance.condition.as.ValidateRequestObjectX5cChainAgainstRical}
 * at the caller's severity: the check the tester explicitly configured cannot run, and skipping
 * it silently would look like a pass.
 */
public class SetupRicalFromConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(RegisterRical.class);

		// A FAILURE (the user configured a URL the suite cannot fetch from) but the test
		// continues; the downstream RICAL checks then skip as no RICAL was registered.
		call(condition(CallRicalEndpoint.class)
			.skipIfStringsMissing("rical_url")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.dontStopOnFailure()
			.requirements("ISO18013-5-F.3.2.1"));

		// WARNING, not FAILURE — see the severity policy in the class javadoc
		ricalQualityCheck(ParseRical.class, "rical", "ISO18013-5-F.3.2.1");

		// informational: the RICAL in CBOR diagnostic notation, for inspecting the actual encoding
		call(condition(DumpRicalCborDiagnostics.class)
			.skipIfObjectsMissing("rical")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.INFO)
			.dontStopOnFailure());
		ricalQualityCheck(ValidateRicalSignature.class, "rical", "ISO18013-5-F.3.2");
		ricalQualityCheck(ValidateRicalSignerCertificate.class, "rical", "ISO18013-5-F.3.2.5");
		ricalQualityCheck(ValidateRicalStructure.class, "rical", "ISO18013-5-F.3.2.1");
	}

	private void ricalQualityCheck(Class<? extends Condition> conditionClass, String requiredObject, String requirement) {
		call(condition(conditionClass)
			.skipIfObjectsMissing(requiredObject)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements(requirement));
	}
}
