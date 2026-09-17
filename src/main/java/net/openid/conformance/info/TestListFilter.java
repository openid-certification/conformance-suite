package net.openid.conformance.info;

import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The test-log listing filter: the two comma-separated parameters {@code GET /api/log} accepts
 * for narrowing by status and by result, as the filter chips on {@code logs.html} send them.
 *
 * <p>Each is a comma-separated list of the values a test can have, matched case-insensitively;
 * a test is listed when its status is any of those asked for AND its result is any of those
 * asked for. Either list may be left out.
 *
 * @param statuses the statuses to list, or empty for any
 * @param results  the results to list, or empty for any
 */
public record TestListFilter(Set<Status> statuses, Set<Result> results) {

	public TestListFilter {
		statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
		results = results == null ? Set.of() : Set.copyOf(results);
	}

	/** A filter that narrows nothing. */
	public static final TestListFilter NONE = new TestListFilter(Set.of(), Set.of());

	/**
	 * @param status the {@code status} parameter, or null
	 * @param result the {@code result} parameter, or null
	 * @return the filter they describe
	 * @throws IllegalArgumentException if either names a value a test cannot have, with a message
	 *                                  fit to answer the request with
	 */
	public static TestListFilter parse(String status, String result) {
		return new TestListFilter(parse(status, Status.class, "status"), parse(result, Result.class, "result"));
	}

	private static <E extends Enum<E>> Set<E> parse(String raw, Class<E> type, String parameter) {
		Set<E> values = EnumSet.noneOf(type);
		if (raw == null) {
			return values;
		}
		for (String token : raw.split(",")) {
			String name = token.trim();
			if (name.isEmpty()) {
				continue;
			}
			try {
				values.add(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("'" + name + "' is not a " + parameter + " a test log can have");
			}
		}
		return values;
	}

	public boolean isEmpty() {
		return statuses.isEmpty() && results.isEmpty();
	}

	/** @return a clause per active list, to be added to the criteria that scope the listing */
	public Criteria toCriteria() {
		Criteria criteria = new Criteria();
		if (!statuses.isEmpty()) {
			criteria.and("status").in(names(statuses));
		}
		if (!results.isEmpty()) {
			criteria.and("result").in(names(results));
		}
		return criteria;
	}

	/** Sorted, so the same filter always produces the same query document. */
	private static List<String> names(Set<? extends Enum<?>> values) {
		return values.stream().map(Enum::name).sorted().toList();
	}
}
