package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.text.RandomStringGenerator;

public class CreateRandomClientNotificationToken extends AbstractCondition {

	protected Integer requestedLength() {
		// The CIBA spec suggests a minimum of 128 bits of entropy, and the bearer token syntax has 70 possible values
		// giving 6.1292 bits of entropy per byte, so requires at least 21 characters.
		// However, the entropy verification on the RP side seemed to calculate entropy to around 90 on occasion,
		// so let's bump it to 28.
		return 28;
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
			.withinRange(pairs).get();
		// we always include exactly one of the allowed trailing =s
		String token = generator.generate(requestedLength()-1) + "=";

		env.putString("client_notification_token", token);

		log("Created token value", args("client_notification_token", token,
			"requested_notification_token_length", requestedLength()));

		return env;
	}

}
