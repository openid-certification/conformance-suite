package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestClientNotificationTokenLengthCondition extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "client_notification_token")
	public Environment evaluate(Environment env) {

		String clientNotificationToken = env.getString("client_notification_token");

		if(clientNotificationToken.length() > 1024) {
			throw error("The client_notification_token length must not exceed 1024 characters.");
		}

		logSuccess("client_notification_token length accepted");

		return env;
	}

}
