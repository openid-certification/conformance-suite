package net.openid.conformance.info;

import com.mongodb.client.result.DeleteResult;
import jakarta.annotation.PreDestroy;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.testmodule.TestModule;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Deletes every plan a filtered listing would show, along with the tests of those plans and
 * every log entry of those tests.
 *
 * <p>What is deleted is decided by {@link DBTestPlanService#listingQuery}, the same seam
 * {@code GET /api/plan} lists through, so that what an admin sees before deleting is what is
 * deleted - there is no second definition of "which plans does this mean" to drift from the
 * first. Two rules live here rather than in the query, and hold whatever was asked for:
 * a plan a certification package was downloaded for is never deleted, and neither is a
 * published one.
 *
 * <p>Those two rules are checked when a batch is selected, not again when it is removed, so a
 * certification download or a publish landing in the seconds a batch takes to process is
 * deleted anyway. Known and deliberately left: the suite runs against standalone mongod, so no
 * transaction can close the window, and re-checking at the plan removal alone would not keep
 * the promise either - the plan's tests and logs are already gone by then, and a "protected"
 * plan document stripped of its evidence is a worse residue than a clean delete. Reaching it
 * takes an admin's filter covering a plan somebody is certifying or publishing at that moment.
 *
 * <p>The bulk delete also inherits the race the single-plan delete documents in
 * {@code TestPlanApi.deleteMutableTestPlan}: each plan's module instances are snapshotted when
 * the batch is selected, so a test created in one of these plans at just that moment can
 * outlive its plan, or leave a few log rows with no test. The reasoning there carries at this
 * scale: the residue is inert - a stopped module cannot resurrect - and the trigger still
 * needs the plan's owner to be creating tests in it in the moment a sweep covers it, which for
 * the old, idle plans a sweep is for is rarer still.
 *
 * <p>The work runs on one background thread because it is far too long for a request: at
 * production scale this is millions of documents. One job runs at a time; progress is polled.
 */
@Service
public class BulkPlanDeleter {

	private static final Logger logger = LoggerFactory.getLogger(BulkPlanDeleter.class);

	/**
	 * How many plans are taken at a time. Each round re-runs the listing query, so the batch is
	 * also how far the job can get ahead of a cancellation.
	 */
	static final int BATCH = 50;

	/** How long to wait between batches, so that a delete of millions does not starve the app. */
	private static final long PAUSE_MS = 200;

	public enum State { IDLE, RUNNING, DONE, CANCELLED, FAILED }

	/**
	 * @param state       where the job is
	 * @param plans       plans deleted so far
	 * @param tests       test runs deleted so far
	 * @param logEntries  log entries deleted so far
	 * @param target      how many plans the job set out to delete
	 * @param startedAt   when it started, or null if nothing has run
	 * @param finishedAt  when it stopped, or null while it runs
	 * @param error       why it failed, or null
	 */
	public record Progress(State state, long plans, long tests, long logEntries, Long target,
						String startedAt, String finishedAt, String error) {

		static Progress idle() {
			return new Progress(State.IDLE, 0, 0, 0, null, null, null, null);
		}

		Progress with(State newState, String error) {
			return new Progress(newState, plans, tests, logEntries, target, startedAt,
				newState == State.RUNNING ? null : Instant.now().toString(), error);
		}

		Progress plus(long morePlans, long moreTests, long moreLogEntries) {
			return new Progress(state, plans + morePlans, tests + moreTests, logEntries + moreLogEntries,
				target, startedAt, finishedAt, error);
		}
	}

	private final MongoTemplate mongoTemplate;

	private final TestInfoService infoService;

	private final TestRunnerSupport testRunnerSupport;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "plan-delete");
		thread.setDaemon(true);
		return thread;
	});

	private volatile Progress progress = Progress.idle();

	private volatile boolean cancelled;

	private Future<?> inFlight;

	public BulkPlanDeleter(MongoTemplate mongoTemplate, TestInfoService infoService,
							TestRunnerSupport testRunnerSupport) {
		this.mongoTemplate = mongoTemplate;
		this.infoService = infoService;
		this.testRunnerSupport = testRunnerSupport;
	}

	@PreDestroy
	public void shutdown() {
		cancelled = true;
		executor.shutdownNow();
	}

	/**
	 * The plans a bulk delete would remove: the ones the listing would show, minus the ones that
	 * are never deleted whatever was asked for.
	 *
	 * <p>An immutable plan is one a certification package has been downloaded for, which
	 * {@code DELETE /api/plan/{id}} refuses with a 405 as well. A published plan is kept because
	 * a link to it may be in circulation; {@code publish} is free text - values such as
	 * {@code "no"} and {@code "d"} exist in production - so <b>anything</b> non-null counts as
	 * published rather than only the values the suite writes, which errs towards keeping a plan.
	 *
	 * @param scope  what the caller may see at all, or null for an admin who may see everything
	 * @param filter the narrowing the caller asked for
	 * @param search the term the listing was searched for, quoted as
	 *               {@link net.openid.conformance.pagination.PaginationRequest#searchTerm}
	 *               returns it, or null
	 * @return the query document, which is also what the count is taken with
	 */
	static Document selection(Criteria scope, PlanListFilter filter, String search) {

		Document listed = DBTestPlanService.listingQuery(scope, filter, search, page()).getQueryObject();

		// one document, so both are required; $ne rather than $eq false because the field is
		// absent on every plan no package was ever downloaded for
		Document keep = new Document("immutable", new Document("$ne", true)).append("publish", null);

		// combined as a document rather than by adding another Criteria to the query: two
		// criteria that are not about a single field both key on null, and Query.addCriteria
		// refuses the second one
		return listed.isEmpty() ? keep : new Document("$and", List.of(listed, keep));
	}

	/** Oldest first, so a limited run deletes the oldest plans and a full run is deterministic. */
	private static PageRequest page() {
		return PageRequest.of(0, BATCH, Sort.by(Sort.Direction.ASC, "started"));
	}

	/**
	 * What a bulk delete would do, without doing any of it.
	 *
	 * @param listed    how many plans the listing shows
	 * @param deletable how many of those may be deleted
	 * @param kept      how many are immutable or published, and so never deleted
	 * @param target    how many this run would delete, which is {@code deletable} capped by any
	 *                  limit, and the number {@code confirm} has to be
	 */
	public record Preview(long listed, long deletable, long kept, long target) { }

	/**
	 * @param scope  what the caller may see at all, or null for an admin
	 * @param filter the narrowing the caller asked for
	 * @param search the quoted term the listing was searched for, or null
	 * @param limit  the most plans to delete, or null for all of them
	 * @return what deleting that would do right now
	 */
	public Preview preview(Criteria scope, PlanListFilter filter, String search, Integer limit) {

		var plans = mongoTemplate.getCollection(DBTestPlanService.COLLECTION);
		long listed = plans.countDocuments(DBTestPlanService.listingQuery(scope, filter, search, page())
			.getQueryObject());
		long deletable = plans.countDocuments(selection(scope, filter, search));

		return new Preview(listed, deletable, listed - deletable,
			Math.min(deletable, limit == null ? Long.MAX_VALUE : limit));
	}

	/** @return what the running or last job has done */
	public Progress progress() {
		return progress;
	}

	/** Asks the running job to stop; it does so at the end of the batch it is in. */
	public void cancel() {
		cancelled = true;
	}

	/**
	 * Starts deleting, in the background.
	 *
	 * @param scope  what the caller may see at all, or null for an admin
	 * @param filter the narrowing the caller asked for
	 * @param search the quoted term the listing was searched for, or null
	 * @param target the most plans to delete: what the caller confirmed, which is the count the
	 *               selection matched capped by any limit they sent
	 * @return the progress of the job just started
	 * @throws IllegalStateException if a job is already running
	 */
	public synchronized Progress start(Criteria scope, PlanListFilter filter, String search, long target) {

		if (inFlight != null && !inFlight.isDone()) {
			throw new IllegalStateException("a bulk delete is already running");
		}

		cancelled = false;
		progress = new Progress(State.RUNNING, 0, 0, 0, target, Instant.now().toString(), null, null);
		logger.info("Bulk plan delete starting: up to {} plans, selection {}",
			target, selection(scope, filter, search).toJson());

		// assigned, because an ignored future would swallow anything thrown outside run()
		inFlight = CompletableFuture.runAsync(() -> run(scope, filter, search, target), executor);

		return progress;
	}

	/**
	 * The confirmed target is a hard ceiling, not just the figure that was shown: each round
	 * re-runs the selection against live data, so a plan created after the caller confirmed
	 * could match it. Stopping at the number they agreed to means such a plan is left for a
	 * later run - which they get to confirm - rather than deleted by a run that was authorised
	 * for something smaller.
	 */
	private void run(Criteria scope, PlanListFilter filter, String search, long target) {
		try {
			while (!cancelled && progress.plans() < target) {
				int room = (int) Math.min(BATCH, target - progress.plans());
				if (deleteBatch(scope, filter, search, room) == 0) {
					break;
				}
				Thread.sleep(PAUSE_MS);
			}
			progress = progress.with(cancelled ? State.CANCELLED : State.DONE, null);
			logger.info("Bulk plan delete {}: {} plans, {} tests, {} log entries",
				progress.state(), progress.plans(), progress.tests(), progress.logEntries());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			progress = progress.with(State.CANCELLED, null);
		} catch (RuntimeException e) {
			logger.error("Bulk plan delete failed after {} plans", progress.plans(), e);
			progress = progress.with(State.FAILED, e.getMessage());
		}
	}

	/**
	 * Deletes one batch. Log entries go before test runs and test runs before plans, so that an
	 * interruption leaves at worst a plan whose tests are gone - re-running the same selection
	 * finishes the job, because a plan is only out of the selection once it is deleted.
	 *
	 * @return how many plans were deleted, 0 when there is nothing left to do
	 */
	private int deleteBatch(Criteria scope, PlanListFilter filter, String search, int room) {

		Query query = new BasicQuery(selection(scope, filter, search)).with(page()).limit(room);
		query.fields().include("modules.instances");

		List<Document> batch = mongoTemplate.find(query, Document.class, DBTestPlanService.COLLECTION);
		if (batch.isEmpty()) {
			return 0;
		}

		List<String> planIds = batch.stream().map(plan -> plan.getString("_id")).toList();
		List<String> testIds = testIdsOf(batch);

		long logEntries = 0;
		long tests = 0;
		if (!testIds.isEmpty()) {
			stopAnyRunning(testIds);
			// through the service rather than removing the documents here, so that a bulk delete
			// means exactly what deleting one plan means - the same collections, the same indexed
			// fields, and the same cached owners dropped afterwards. The owner is null because
			// only an admin can start one of these, and this thread has no security context to
			// read it from anyway
			TestInfoService.Deleted deleted = infoService.deleteTests(testIds, null);
			tests = deleted.tests();
			logEntries = deleted.logEntries();
		}
		// by _id alone: the immutable/publish guard was applied when this batch was selected,
		// not here - the class javadoc records the window that leaves open, and why it stays
		long plans = remove(Query.query(Criteria.where("_id").in(planIds)), DBTestPlanService.COLLECTION);

		progress = progress.plus(plans, tests, logEntries);
		logger.info("Bulk plan delete: removed {} plans, {} tests, {} log entries (running total {}/{})",
			plans, tests, logEntries, progress.plans(), progress.target());

		if (plans == 0) {
			// nothing was removed although the selection matched, so the next round would find
			// the same plans again and never end
			throw new IllegalStateException("matched " + planIds.size() + " plans but deleted none");
		}
		return (int) plans;
	}

	/**
	 * Stop any module of this batch that is still running, before its rows go.
	 *
	 * <p>Synchronously, and first: {@code stop()} writes the module's last log entries, which the
	 * delete then removes. A module left running would keep writing EVENT_LOG rows against a test
	 * that no longer exists. This mirrors what deleting a single plan does.
	 *
	 * @param testIds the tests about to be deleted
	 */
	private void stopAnyRunning(List<String> testIds) {
		for (String testId : testIds) {
			TestModule runningTest = testRunnerSupport.getRunningTestById(testId);
			if (runningTest != null) {
				logger.info("Bulk plan delete: stopping {}, which is still running", testId);
				runningTest.stop("The test was stopped because its test plan was deleted.");
			}
		}
	}

	private long remove(Query query, String collection) {
		DeleteResult result = mongoTemplate.remove(query, collection);
		return result.wasAcknowledged() ? result.getDeletedCount() : 0;
	}

	/**
	 * @param plans plan documents projected to {@code modules.instances}
	 * @return every test id those plans list, which is where the ids come from rather than a
	 *         query on TEST_INFO: nothing indexes {@code planId}, so asking that collection
	 *         which runs belong to a plan would scan all of it, once per batch
	 */
	static List<String> testIdsOf(List<Document> plans) {

		List<String> testIds = new ArrayList<>();
		for (Document plan : plans) {
			for (Document module : plan.getList("modules", Document.class, List.of())) {
				testIds.addAll(module.getList("instances", String.class, List.of()));
			}
		}
		return testIds;
	}
}
