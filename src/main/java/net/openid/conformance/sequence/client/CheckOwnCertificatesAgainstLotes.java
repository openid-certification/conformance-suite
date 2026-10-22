package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.ValidateOwnMdocSigningCertificateAgainstLote;
import net.openid.conformance.condition.client.lote.ValidateOwnSdJwtSigningCertificateAgainstLote;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Sets up the lists of trusted entities that cover what the suite itself signs in a module, then
 * pre-flight checks that the suite's own certificates chain to a listed entity: an implementation
 * trusting the lists is expected to reject what the suite signs otherwise. Both report on the
 * suite's listing, not on the implementation under test, so an unusable list and an uncovered
 * certificate are WARNINGs.
 */
public class CheckOwnCertificatesAgainstLotes extends AbstractConditionSequence {

	private final Environment env;
	private final List<Class<? extends Condition>> checks;
	private final LoteSlot[] slots;

	/**
	 * @param env the environment holding the test configuration at the time of the call, which says which lists are configured
	 * @param check the condition checking the suite's certificates against the lists, which logs
	 *              and passes when none of them is configured
	 * @param slots the lists to set up
	 */
	public CheckOwnCertificatesAgainstLotes(Environment env, Class<? extends Condition> check, LoteSlot... slots) {
		this(env, List.of(check), slots);
	}

	/** As {@link #CheckOwnCertificatesAgainstLotes(Environment, Class, LoteSlot...)}, with several checks run against one setup of the lists. */
	public CheckOwnCertificatesAgainstLotes(Environment env, List<Class<? extends Condition>> checks, LoteSlot... slots) {
		this.env = env;
		this.checks = List.copyOf(checks);
		this.slots = slots;
	}

	/**
	 * The check of the suite's own credential signer against the PID and mDL providers lists: the
	 * mdoc document signer for {@code mdoc} credentials, the SD-JWT VC signer otherwise.
	 */
	public static CheckOwnCertificatesAgainstLotes forCredentialSigner(Environment env, boolean mdoc) {
		return new CheckOwnCertificatesAgainstLotes(env,
			mdoc ? ValidateOwnMdocSigningCertificateAgainstLote.class : ValidateOwnSdJwtSigningCertificateAgainstLote.class,
			LoteSlot.PID, LoteSlot.MDL);
	}

	@Override
	public void evaluate() {
		call(SetupLoteFromConfiguration.forConfigured(env, ConditionResult.WARNING, slots));
		for (Class<? extends Condition> check : checks) {
			call(condition(check)
				.onFail(ConditionResult.WARNING)
				.dontStopOnFailure()
				.requirements("ETSI119602-6.6.3"));
		}
	}
}
