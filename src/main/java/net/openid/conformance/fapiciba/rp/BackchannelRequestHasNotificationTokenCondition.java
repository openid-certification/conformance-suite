package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestHasNotificationTokenCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	@PostEnvironment(strings = "client_notification_token")
	public Environment evaluate(Environment env) {

		String clientNotificationToken = env.getString("backchannel_request_object", "claims.client_notification_token");
		env.putString("client_notification_token", clientNotificationToken);

		if(Strings.isNullOrEmpty(clientNotificationToken)) {
			throw error("The client_notification_token is required in ping mode");
		}

		logSuccess("Backchannel authentication request contains the required client_notification_token");

		return env;
	}

}
