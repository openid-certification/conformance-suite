package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test's result is written by several independent events - a condition failing, a condition
 * warning, an image being uploaded for review - and whichever of them happens last must not
 * decide the verdict. FAILED outranks REVIEW, which outranks WARNING: a test with an image
 * nobody has looked at yet still needs that review however many warnings it also logged.
 */
public class AbstractTestModuleResultPrecedence_UnitTest {

	private static class FindingCondition extends AbstractCondition {
		@Override
		public Environment evaluate(Environment env) {
			throw error("a finding");
		}
	}

	private static class PrecedenceTestModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		@Override
		public String getName() {
			return "precedence-test";
		}

		void initialize(TestExecutionManager executionManager, ImageService imageService) {
			setProperties("precedence-test", Map.of(), mock(TestInstanceEventLog.class), mock(BrowserControl.class),
				mock(TestInfoService.class), executionManager, imageService);
			setStatus(Status.CONFIGURED);
			setStatus(Status.RUNNING);
		}

		void recordFinding(Condition.ConditionResult severity) {
			callAndContinueOnFailure(new FindingCondition(), severity);
		}
	}

	private final ImageService imageService = mock(ImageService.class);

	private PrecedenceTestModule module;

	@BeforeEach
	public void setUp() throws Exception {
		// run the finalisation task on the calling thread, so the result it decides can be asserted on
		TestExecutionManager executionManager = mock(TestExecutionManager.class);
		doAnswer(invocation -> invocation.<Callable<?>>getArgument(0).call())
			.when(executionManager).runFinalisationTaskInBackground(any());

		module = new PrecedenceTestModule();
		module.initialize(executionManager, imageService);
	}

	@Test
	public void reviewReplacesAnEarlierWarning() {
		module.recordFinding(Condition.ConditionResult.WARNING);

		module.fireTestReviewNeeded();

		assertThat(module.getResult()).isEqualTo(TestModule.Result.REVIEW);
	}

	@Test
	public void warningDoesNotReplaceAnEarlierReview() {
		module.fireTestReviewNeeded();

		module.recordFinding(Condition.ConditionResult.WARNING);

		assertThat(module.getResult()).isEqualTo(TestModule.Result.REVIEW);
	}

	@Test
	public void failureReplacesAnEarlierReview() {
		module.fireTestReviewNeeded();

		module.recordFinding(Condition.ConditionResult.FAILURE);

		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}

	@Test
	public void reviewDoesNotReplaceAnEarlierFailure() {
		module.recordFinding(Condition.ConditionResult.FAILURE);

		module.fireTestReviewNeeded();

		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}

	@Test
	public void warningDoesNotReplaceAnEarlierFailure() {
		module.recordFinding(Condition.ConditionResult.FAILURE);

		module.recordFinding(Condition.ConditionResult.WARNING);

		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}

	@Test
	public void finishingWithAnImageFilledByBrowserAutomationNeedsReview() {
		imageWasFilledByBrowserAutomation();

		module.fireTestFinished();

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.REVIEW);
	}

	@Test
	public void finishingWithAnImageFilledByBrowserAutomationNeedsReviewDespiteAnEarlierWarning() {
		module.recordFinding(Condition.ConditionResult.WARNING);
		imageWasFilledByBrowserAutomation();

		module.fireTestFinished();

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.REVIEW);
	}

	@Test
	public void finishingWithAWarningAndNoImagesStaysAWarning() {
		module.recordFinding(Condition.ConditionResult.WARNING);

		module.fireTestFinished();

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.WARNING);
	}

	@Test
	public void finishingWithAFailureAndAnImageStaysFailed() {
		module.recordFinding(Condition.ConditionResult.FAILURE);
		imageWasFilledByBrowserAutomation();

		module.fireTestFinished();

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}

	private void imageWasFilledByBrowserAutomation() {
		when(imageService.getFilledPlaceholders(anyString(), anyBoolean())).thenReturn(List.of(new Document()));
	}
}
