package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestClientNotificationTokenEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The actual amount of required entropy is 128 bits, but we can't accurately measure entropy so a bit of
	 * slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	@Override
	@PreEnvironment(strings = "client_notification_token")
	public Environment evaluate(Environment env) {
		String clientNotificationToken = env.getString("client_notification_token");
		return ensureMinimumEntropy(env, clientNotificationToken, requiredEntropy);
	}

}
