package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ForceNaCurrency extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String updatedData = "{\"data\": {\"overdraftContractedLimit\": 99.9999,\"overdraftContractedLimitCurrency\": \"NA\",\"overdraftUsedLimit\": 10000.9999,\"overdraftUsedLimitCurrency\": \"NA\",\"unarrangedOverdraftAmount\": 99.9999,\"unarrangedOverdraftAmountCurrency\": \"NA\"},\"links\": {\"self\": \"https://matls-api.mockbank.poc.raidiam.io/accounts/v1/accounts/291e5a29-49ed-401f-a583-193caa7aceee/overdraft-limits\"},\"meta\": {\"totalRecords\": 1,\"totalPages\": 1,\"requestDateTime\": \"2021-05-25T15:46:00Z\"}}";
		env.putString("resource_endpoint_response", updatedData);
		return env;
	}

}
