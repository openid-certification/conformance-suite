package io.fintechlabs.testframework.sequence.as;

import io.fintechlabs.testframework.condition.rs.CreateOpenBankingAccountsResponse;
import io.fintechlabs.testframework.condition.rs.GenerateOpenBankingAccountId;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class GenerateOpenBankingUkAccountsEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(GenerateOpenBankingAccountId.class);
		call(exec().exposeEnvironmentString("account_id"));
		callAndStopOnFailure(CreateOpenBankingAccountsResponse.class);
	}

}
