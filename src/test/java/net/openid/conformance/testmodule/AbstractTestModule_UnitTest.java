package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.EventLog;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractTestModule_UnitTest {

	@PublishTestModule(
		testName = "unit-test-module",
		displayName = "Unit Test Module",
		profile = "UNIT-TEST"
	)
	static class StubTestModule extends AbstractTestModule {
		void moveToConfigured() {
			setStatus(Status.CONFIGURED);
		}

		void moveToRunning() {
			setStatus(Status.RUNNING);
		}

		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}
	}

	@Test
	void late_failure_does_not_change_passed_result_once_test_is_finished() throws Exception {
		TestInfoService testInfo = mock(TestInfoService.class);
		ImageService imageService = mock(ImageService.class);
		BrowserControl browser = mock(BrowserControl.class);
		TestExecutionManager executionManager = mock(TestExecutionManager.class);
		EventLog eventLog = mock(EventLog.class);

		when(browser.runnersActive()).thenReturn(false);
		when(imageService.getFilledPlaceholders("UNIT-TEST", true)).thenReturn(List.of());
		when(imageService.getRemainingPlaceholders("UNIT-TEST", true)).thenReturn(List.of());
		doAnswer(invocation -> {
			Callable<?> callable = invocation.getArgument(0);
			return callable.call();
		}).when(executionManager).runFinalisationTaskInBackground(any());

		StubTestModule testModule = new StubTestModule();
		testModule.setProperties(
			"UNIT-TEST",
			Map.of("sub", "user", "iss", "issuer"),
			new TestInstanceEventLog("UNIT-TEST", Map.of("sub", "user", "iss", "issuer"), eventLog),
			browser,
			testInfo,
			executionManager,
			imageService
		);

		testModule.moveToConfigured();
		testModule.moveToRunning();
		testModule.fireTestFinished();

		assertEquals(TestModule.Status.FINISHED, testModule.getStatus());
		assertEquals(TestModule.Result.PASSED, testModule.getResult());

		testModule.handleException(new TestFailureException("UNIT-TEST", "late failure"), "unit test");

		assertEquals(TestModule.Status.FINISHED, testModule.getStatus());
		assertEquals(TestModule.Result.PASSED, testModule.getResult());
		verify(testInfo).updateTestResult("UNIT-TEST", TestModule.Result.PASSED);
		verify(testInfo, never()).updateTestResult(eq("UNIT-TEST"), eq(TestModule.Result.FAILED));
	}
}
