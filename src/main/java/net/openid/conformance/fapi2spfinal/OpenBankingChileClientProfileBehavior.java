package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.ExtractChileResponseSigningJwksFromConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for Chile Open Finance client tests.
 * Requires mTLS everywhere, and loads the key the emulated bank signs its resource responses with.
 */
public class OpenBankingChileClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence additionalServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ExtractChileResponseSigningJwksFromConfiguration.class, "CMF-XJWS-5.3.1");
			}
		};
	}
}
