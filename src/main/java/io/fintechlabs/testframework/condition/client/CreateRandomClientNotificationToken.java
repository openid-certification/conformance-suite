package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.commons.text.RandomStringGenerator;

public class CreateRandomClientNotificationToken extends AbstractCondition {
	protected Integer requestedLength() {
		// the CIBA spec suggests a minimum of 128 bits of entropy, and the bearer token syntax has 70 possible values
		// giving 6.1292 bits of entropy per byte, so requires at least 21 characters
		return 21;
	}

	public CreateRandomClientNotificationToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(strings = "client_notification_token")
	public Environment evaluate(Environment env) {

		// According to CIBA-7.1 Bearer tokens "conform to the syntax for Bearer credentials as defined in Section 2.1 of [RFC6750].", the latter saying:
		//      b64token    = 1*( ALPHA / DIGIT /
		//                       "-" / "." / "_" / "~" / "+" / "/" ) *"="

		char [][] pairs = {
			{'a','z'},
			{'A','Z'},
			{'0','9'},
			{'-','-'},
			{'.','.'},
			{'_','_'},
			{'~','~'},
			{'+','+'},
			{'/','/'},
		};
		RandomStringGenerator generator = new RandomStringGenerator.Builder()
			.withinRange(pairs).build();
		// we always include exactly one of the allowed trailing =s
		String token = generator.generate(requestedLength()-1) + "=";

		env.putString("client_notification_token", token);

		log("Created token value", args("client_notification_token", token,
			"requested_notification_token_length", requestedLength()));

		return env;
	}

}
