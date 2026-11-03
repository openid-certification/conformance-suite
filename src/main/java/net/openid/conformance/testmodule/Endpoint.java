package net.openid.conformance.testmodule;

import net.openid.conformance.sequence.Steps;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An HTTP endpoint a test module serves, described as steps: the units run on every request to it and how the
 * response is built from the environment once they have run. A module passes the endpoint for a request's path to
 * {@link AbstractTestModule#serve}.
 *
 * <p>While the steps run, the request is stored under {@link #CURRENT_REQUEST}. Conditions that read the request
 * under another key get it through {@link #requestAs}.
 *
 * @param steps run on every request to the endpoint
 * @param respond builds the HTTP response, e.g. a {@code ResponseEntity} or a {@code RedirectView}
 * @param finishesTest whether the test finishes once the response is built, decided from the environment the steps
 * left, e.g. once the client has made the last call the test waits for
 * @param userFacing the endpoint is visited by the user's browser, so a failure shows the test log rather than a
 * JSON error
 */
public record Endpoint(
	TestExecutionUnit steps,
	Function<Environment, Object> respond,
	Predicate<Environment> finishesTest,
	boolean userFacing
) {

	/**
	 * Where the request being served is stored.
	 */
	public static final String CURRENT_REQUEST = "current_request";

	public Endpoint {
		Objects.requireNonNull(steps, "steps");
		Objects.requireNonNull(respond, "respond");
		Objects.requireNonNull(finishesTest, "finishesTest");
	}

	public static Endpoint of(TestExecutionUnit steps, Function<Environment, Object> respond) {
		return new Endpoint(steps, respond, env -> false, false);
	}

	/**
	 * This endpoint, finishing the test once its response is built.
	 */
	public Endpoint finishingTest() {
		return finishingTestWhen(env -> true);
	}

	/**
	 * This endpoint, finishing the test once its response is built if {@code condition} holds for the environment its
	 * steps left. The condition is checked on every request, so it must hold on only one of them.
	 */
	public Endpoint finishingTestWhen(Predicate<Environment> condition) {
		return new Endpoint(steps, respond, condition, userFacing);
	}

	/**
	 * This endpoint, showing the test log in the browser when it fails.
	 */
	public Endpoint visitedByBrowser() {
		return new Endpoint(steps, respond, finishesTest, true);
	}

	/**
	 * This endpoint with {@code checks} run before its steps, e.g. checks every request to the module gets.
	 */
	public Endpoint precededBy(TestExecutionUnit checks) {
		return new Endpoint(Steps.of(null, checks, steps), respond, finishesTest, userFacing);
	}

	/**
	 * Runs {@code body} with each of {@code keys} mapped to the request being served, for conditions that read the
	 * request under that key.
	 */
	public static TestExecutionUnit requestAs(List<String> keys, TestExecutionUnit body) {
		String names = String.join(", ", keys);
		return Steps.of(null,
			Steps.action("Map " + names + " to the request", env -> keys.forEach(key -> env.mapKey(key, CURRENT_REQUEST))),
			body,
			Steps.action("Unmap " + names, env -> keys.forEach(env::unmapKey)));
	}

	/**
	 * Runs {@code units} with {@code key} mapped to the request being served.
	 */
	public static TestExecutionUnit requestAs(String key, TestExecutionUnit... units) {
		return requestAs(List.of(key), Steps.of(null, (Object[]) units));
	}
}
