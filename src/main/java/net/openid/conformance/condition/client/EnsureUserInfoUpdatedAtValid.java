package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class EnsureUserInfoUpdatedAtValid extends AbstractUpdatedAtValid {
	public static final String location = "userinfo";

	@Override
	@PreEnvironment(required = location )
	public Environment evaluate(Environment env) {
		return validateUpdatedAt(env, location);
	}

}
