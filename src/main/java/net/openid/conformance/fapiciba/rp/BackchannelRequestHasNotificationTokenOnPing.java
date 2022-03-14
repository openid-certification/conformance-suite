package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.variant.CIBAMode;

public class BackchannelRequestHasNotificationTokenOnPing extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object", strings = "ciba_mode")
	public Environment evaluate(Environment env) {

		CIBAMode cibaMode = CIBAMode.valueOf(env.getString("ciba_mode"));
		String clientNotificationToken = env.getString("backchannel_request_object", "claims.client_notification_token");

		if(CIBAMode.PING.equals(cibaMode) && Strings.isNullOrEmpty(clientNotificationToken)) {
			throw error("'client_notification_token' is required in ping mode");
		}

		logSuccess("Either token delivery mode is non-ping, or the backchannel authentication request correctly " +
			"contains the client_notification_token.");

		return env;
	}

}
