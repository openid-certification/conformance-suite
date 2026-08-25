package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallVicalEndpoint;
import net.openid.conformance.condition.client.EnsureVicalEndpointContentTypeIsApplicationCbor;
import net.openid.conformance.condition.client.ParseVical;
import net.openid.conformance.condition.client.RegisterVical;
import net.openid.conformance.condition.client.ValidateVicalSignature;
import net.openid.conformance.condition.client.ValidateVicalSignerCertificateProfile;
import net.openid.conformance.condition.client.ValidateVicalStructure;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Registers the optionally configured VICAL (inline or fetched from a URL), then parses and
 * validates it. All steps are skipped when no VICAL is configured. Invoked once at configure
 * time, gated on the mdoc credential format by the caller; the per-credential chain checks
 * against the registered VICAL happen later, in {@link ValidateMdocCredential}.
 *
 * Severity policy (two tiers): the VICAL quality checks here are WARNINGs — the VICAL provider
 * is a third party, so defects that don't prevent using the list (expired signer certificate,
 * missing EKU, structure problems, stale notAfter) should not fail the entity under test, and
 * the chain check still evaluates against such a list. Defects that make the list unusable
 * (unparseable, COSE signature does not verify) additionally surface in
 * {@link net.openid.conformance.condition.client.ValidateMdocIssuerChainAgainstVical} at the
 * caller's severity: the check the tester explicitly configured cannot run, and skipping it
 * silently would look like a pass.
 */
public class SetupVicalFromConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(RegisterVical.class);

		// A FAILURE (the user configured a URL the suite cannot fetch from) but the test
		// continues; the downstream VICAL checks then skip as no VICAL was registered.
		call(condition(CallVicalEndpoint.class)
			.skipIfStringsMissing("vical_url")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.dontStopOnFailure()
			.requirements("ISO18013-5-C.1.7.1"));

		// WARNING, not FAILURE — see the severity policy in the class javadoc (multipaz's
		// parser also rejects third-party VICAL defects, so ParseVical must not be a FAILURE)
		vicalQualityCheck(EnsureVicalEndpointContentTypeIsApplicationCbor.class, "vical_endpoint_response", "ISO18013-5-C.1.7.1");
		vicalQualityCheck(ParseVical.class, "vical", "ISO18013-5-C.1.7.1");
		vicalQualityCheck(ValidateVicalSignature.class, "vical", "ISO18013-5-C.1.7.1");
		vicalQualityCheck(ValidateVicalSignerCertificateProfile.class, "vical", "ISO18013-5-C.1.7.2");
		vicalQualityCheck(ValidateVicalStructure.class, "vical", "ISO18013-5-C.1.7.1");
	}

	private void vicalQualityCheck(Class<? extends Condition> conditionClass, String requiredObject, String requirement) {
		call(condition(conditionClass)
			.skipIfObjectsMissing(requiredObject)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements(requirement));
	}
}
