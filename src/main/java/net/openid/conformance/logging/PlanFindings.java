package net.openid.conformance.logging;

import io.swagger.v3.oas.annotations.media.Schema;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The failures, warnings and items awaiting review (uploaded images, and pages the browser
 * automation captured in their place) of the latest runs of a plan's modules, grouped so that a
 * finding logged by several modules is listed once with the modules it occurred in.
 */
@Schema(description = "Failures, warnings and items awaiting review across the latest runs of a plan's modules")
public record PlanFindings(
	@Schema(description = "Failures first, then warnings, then images, then captured pages; within each, the most widespread first")
	List<Finding> findings,
	@Schema(description = "For each module run that logged a failure, its earliest failure entry, in plan order")
	List<FirstFailure> firstFailures) {

	public static final String FAILURE = "FAILURE";
	public static final String WARNING = "WARNING";
	public static final String IMAGE = "IMAGE";
	public static final String PAGE = "PAGE";

	/** Set, alongside the page itself, on a review entry the browser automation satisfied. */
	public static final String CAPTURED_PAGE_MARKER = "content_type";

	private static final List<String> KIND_ORDER = List.of(FAILURE, WARNING, IMAGE, PAGE);

	/**
	 * The latest run of one module of the plan.
	 *
	 * @param moduleIndex the module's position in the plan
	 * @param testModule  the module's name
	 * @param variant     the variant the module runs with in this plan
	 * @param testId      the id of its latest run
	 */
	public record Run(int moduleIndex, String testModule, Map<String, String> variant, String testId) {
	}

	@Schema(description = "One distinct finding and every module run it occurred in")
	public record Finding(
		@Schema(description = "FAILURE, WARNING, IMAGE (an uploaded image awaiting review, served by GET /api/plan/{id}/findings/{entryId}/image) or PAGE (a page the browser automation captured in place of an image, awaiting review)")
		String kind,
		@Schema(description = "The condition or source that logged the entry")
		String src,
		@Schema(description = "The logged message; null if the entry has none")
		String msg,
		@Schema(description = "The specification requirements cited by any of the entries")
		List<String> requirements,
		List<Occurrence> occurrences) {
	}

	@Schema(description = "A module run in which a finding occurred")
	public record Occurrence(
		@Schema(description = "The module's position in the plan's modules")
		int moduleIndex,
		String testModule,
		Map<String, String> variant,
		@Schema(description = "The id of the module's latest run")
		String testId,
		@Schema(description = "The id of the first matching log entry of that run")
		String entryId,
		@Schema(description = "How many matching entries that run logged")
		int count) {
	}

	@Schema(description = "The earliest failure logged by a module run")
	public record FirstFailure(
		@Schema(description = "The module's position in the plan's modules")
		int moduleIndex,
		@Schema(description = "The id of the module's latest run")
		String testId,
		@Schema(description = "The id of the run's earliest FAILURE log entry")
		String entryId) {
	}

	private record Key(String kind, String src, String msg) {
	}

	private static final class Group {
		private final Set<String> requirements = new LinkedHashSet<>();
		private final Map<String, Occurrence> occurrencesByTest = new LinkedHashMap<>();
	}

	/**
	 * @param runs    the latest run of each module that has run, in plan order
	 * @param entries the log entries of those runs that are a failure, a warning, or carry an
	 *                uploaded image or a captured page, in any order; entries of other runs are
	 *                ignored
	 * @return the entries grouped by kind, source and message, and each run's earliest failure
	 */
	public static PlanFindings of(List<Run> runs, Collection<Document> entries) {

		Map<String, Run> runsByTest = new LinkedHashMap<>();
		for (Run run : runs) {
			runsByTest.put(run.testId(), run);
		}

		List<Document> ordered = new ArrayList<>(entries);
		ordered.removeIf(entry -> !runsByTest.containsKey(entry.getString("testId")));
		ordered.sort(Comparator
			.comparingInt((Document entry) -> runsByTest.get(entry.getString("testId")).moduleIndex())
			.thenComparingLong(PlanFindings::time));

		Map<Key, Group> groups = new LinkedHashMap<>();
		Map<String, FirstFailure> firstFailures = new LinkedHashMap<>();
		for (Document entry : ordered) {
			Run run = runsByTest.get(entry.getString("testId"));
			if (FAILURE.equals(entry.get("result"))) {
				firstFailures.putIfAbsent(run.testId(),
					new FirstFailure(run.moduleIndex(), run.testId(), String.valueOf(entry.get("_id"))));
			}
			Key key = new Key(kind(entry), entry.getString("src"), entry.get("msg") instanceof String msg ? msg : null);
			Group group = groups.computeIfAbsent(key, k -> new Group());
			if (entry.get("requirements") instanceof Collection<?> requirements) {
				requirements.forEach(requirement -> group.requirements.add(String.valueOf(requirement)));
			}
			group.occurrencesByTest.merge(run.testId(),
				new Occurrence(run.moduleIndex(), run.testModule(), run.variant(), run.testId(),
					String.valueOf(entry.get("_id")), 1),
				(first, ignored) -> new Occurrence(first.moduleIndex(), first.testModule(), first.variant(),
					first.testId(), first.entryId(), first.count() + 1));
		}

		List<Finding> findings = new ArrayList<>();
		groups.forEach((key, group) -> findings.add(new Finding(key.kind(), key.src(), key.msg(),
			List.copyOf(group.requirements), List.copyOf(group.occurrencesByTest.values()))));
		// the sort is stable, so findings equally widespread stay in plan order
		findings.sort(Comparator
			.comparingInt((Finding finding) -> KIND_ORDER.indexOf(finding.kind()))
			.thenComparing(finding -> finding.occurrences().size(), Comparator.reverseOrder()));

		return new PlanFindings(findings, List.copyOf(firstFailures.values()));
	}

	/**
	 * A failure or a warning is what its result says. Anything else was selected for what it
	 * carries, whatever its result: a page the browser automation captured, or else an uploaded
	 * image. A filled placeholder keeps REVIEW, and an image added on the upload page has no
	 * result.
	 */
	private static String kind(Document entry) {
		Object result = entry.get("result");
		if (FAILURE.equals(result) || WARNING.equals(result)) {
			return (String) result;
		}
		return entry.containsKey(CAPTURED_PAGE_MARKER) ? PAGE : IMAGE;
	}

	private static long time(Document entry) {
		return entry.get("time") instanceof Number time ? time.longValue() : 0L;
	}
}
