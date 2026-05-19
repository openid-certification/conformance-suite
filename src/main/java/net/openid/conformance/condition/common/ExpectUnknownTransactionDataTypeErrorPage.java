package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectUnknownTransactionDataTypeErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "unknown_transaction_data_type_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder(
			"The authorization request contains a transaction_data entry with an unrecognized type. "
				+ "The wallet should reject the request and display an error.");
		env.putString("unknown_transaction_data_type_error", placeholder);

		return env;
	}
}
