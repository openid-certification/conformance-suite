package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class GenerateKSAAccountConsentId extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "account_request_id")
	public Environment evaluate(Environment env) {

		String acct = "urn:SAMA:"+ RandomStringUtils.secure().nextAlphanumeric(10);

		env.putString("account_request_id", acct);

		logSuccess("Created account request", args("account_request_id", acct));

		return env;

	}

}
