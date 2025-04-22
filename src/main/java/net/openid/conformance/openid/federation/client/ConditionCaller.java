package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;

@FunctionalInterface
public interface ConditionCaller {
	void call(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements);
}
