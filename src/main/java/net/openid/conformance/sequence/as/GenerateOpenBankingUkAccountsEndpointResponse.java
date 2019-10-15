package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.rs.CreateOpenBankingAccountsResponse;
import net.openid.conformance.condition.rs.GenerateOpenBankingAccountId;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateOpenBankingUkAccountsEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(GenerateOpenBankingAccountId.class);
		call(exec().exposeEnvironmentString("account_id"));
		callAndStopOnFailure(CreateOpenBankingAccountsResponse.class);
	}

}
