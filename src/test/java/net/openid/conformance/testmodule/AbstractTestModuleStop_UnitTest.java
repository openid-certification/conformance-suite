package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A test stopped before it ran to completion has no verdict. WARNING and REVIEW are written
 * while the test is still running, so a stop must not leave them behind as a "passed with
 * warnings" result; FAILED is a verdict and stays.
 */
public class AbstractTestModuleStop_UnitTest {

	private static class WarnCondition extends AbstractCondition {
		@Override
		public Environment evaluate(Environment env) {
			throw error("a warning-level finding");
		}
	}

	private static class StopTestModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		@Override
		public String getName() {
			return "stop-test";
		}

		void initialize() {
			setProperties("stop-test", Map.of(), mock(TestInstanceEventLog.class), mock(BrowserControl.class),
				mock(TestInfoService.class), mock(TestExecutionManager.class), mock(ImageService.class));
			setStatus(Status.CONFIGURED);
			setStatus(Status.RUNNING);
		}

		void recordFinding(Condition.ConditionResult severity) {
			callAndContinueOnFailure(new WarnCondition(), severity);
		}

		void waitForInteraction() {
			setStatus(Status.WAITING);
		}
	}

	private StopTestModule module;

	@BeforeEach
	public void setUp() {
		module = new StopTestModule();
		module.initialize();
	}

	@Test
	public void stopWhileWaitingDropsInterimWarning() {
		module.recordFinding(Condition.ConditionResult.WARNING);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.WARNING);
		module.waitForInteraction();

		module.stop("stopped by the tester");

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.INTERRUPTED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
	}

	@Test
	public void stopWhileRunningDropsInterimReview() {
		module.fireTestReviewNeeded();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.REVIEW);

		module.stop("stopped by the tester");

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.INTERRUPTED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
	}

	@Test
	public void stopKeepsFailureVerdict() {
		module.recordFinding(Condition.ConditionResult.FAILURE);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
		module.waitForInteraction();

		module.stop("stopped by the tester");

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.INTERRUPTED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}
}
