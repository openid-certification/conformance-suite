package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.FAPISetClientCredentialsGrantTypeInServerConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for the FAPI2 client credentials grant only profile.
 * Skips the auth-code-grant configuration (no id_token signing algs registered),
 * skips PAR endpoint registration, and skips userinfo loading.
 */
public class ClientCredentialsGrantClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean isClientCredentialsGrantOnly() {
		return true;
	}

	@Override
	public ConditionSequence addProfileSpecificServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPISetClientCredentialsGrantTypeInServerConfiguration.class);
			}
		};
	}

	@Override
	public ConditionSequence addAuthCodeGrantServerConfiguration() {
		return null;
	}

	@Override
	public boolean shouldRegisterPAREndpoint() {
		return false;
	}

	@Override
	public boolean shouldLoadUserInfo() {
		return false;
	}

	@Override
	public boolean supportsClientCredentialsGrant() {
		return true;
	}
}
