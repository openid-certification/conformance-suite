package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Objects;

/**
 * A condition whose logic is given as a lambda instead of a dedicated class.
 *
 * <p>The name takes the place of the class simple name: it is the source of every log entry, the name matched by
 * expected-failure lists, and the key used by {@code ConditionSequence.replace/skip/insertBefore/insertAfter}. It
 * must therefore be stable and unique within the test module.
 *
 * <p>The body has the same contract as {@link AbstractCondition#evaluate(Environment)}: it must log something, and
 * report failures by throwing the {@link ConditionError} returned from one of the {@code error(...)} methods.
 */
public final class FunctionalCondition extends AbstractCondition {

	@FunctionalInterface
	public interface Body {
		void apply(Environment env, Scope condition);
	}

	/**
	 * The logging and error helpers of {@link AbstractCondition} available to the lambda body.
	 */
	public final class Scope {

		private Scope() {
		}

		public void log(String msg) {
			FunctionalCondition.this.log(msg);
		}

		public void log(String msg, Map<String, Object> map) {
			FunctionalCondition.this.log(msg, map);
		}

		public void log(String msg, JsonObject in) {
			FunctionalCondition.this.log(msg, in);
		}

		public void logSuccess(String msg) {
			FunctionalCondition.this.logSuccess(msg);
		}

		public void logSuccess(String msg, Map<String, Object> map) {
			FunctionalCondition.this.logSuccess(msg, map);
		}

		public void logSuccess(String msg, JsonObject in) {
			FunctionalCondition.this.logSuccess(msg, in);
		}

		public ConditionError error(String message) {
			return FunctionalCondition.this.error(message);
		}

		public ConditionError error(String message, Map<String, Object> map) {
			return FunctionalCondition.this.error(message, map);
		}

		public ConditionError error(String message, JsonObject in) {
			return FunctionalCondition.this.error(message, in);
		}

		public ConditionError error(String message, Throwable cause) {
			return FunctionalCondition.this.error(message, cause);
		}

		public ConditionError error(String message, Throwable cause, Map<String, Object> map) {
			return FunctionalCondition.this.error(message, cause, map);
		}
	}

	private final String name;
	private final Body body;

	public FunctionalCondition(String name, Body body) {
		this.name = Objects.requireNonNull(name, "a functional condition needs a name");
		this.body = Objects.requireNonNull(body, "a functional condition needs a body");
	}

	@Override
	public String getMessage() {
		return name;
	}

	@Override
	public Environment evaluate(Environment env) {
		body.apply(env, new Scope());
		return env;
	}
}
