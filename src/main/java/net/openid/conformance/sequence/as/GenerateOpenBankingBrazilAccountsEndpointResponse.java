package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.rs.CreateBrazilAccountsEndpointResponse;
import net.openid.conformance.condition.rs.CreateOpenBankingAccountsResponse;
import net.openid.conformance.condition.rs.GenerateOpenBankingAccountId;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateOpenBankingBrazilAccountsEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateBrazilAccountsEndpointResponse.class);
	}

}
