package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Base64;

public class CreatePoppySession extends AbstractCondition {

	@Override
	@PostEnvironment(strings = {"poppy_sid", "poppy_user_id"})
	public Environment evaluate(Environment env) {
		byte[] bytes = new byte[16];
		new SecureRandom().nextBytes(bytes);
		String sid = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String userId = "usr_" + RandomStringUtils.secure().nextAlphanumeric(16);

		env.putString("poppy_sid", sid);
		env.putString("poppy_user_id", userId);

		logSuccess("Created the Poppy session identifiers", args("sid", sid, "user_id", userId));
		return env;
	}
}
