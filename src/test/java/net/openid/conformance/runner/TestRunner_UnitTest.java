package net.openid.conformance.runner;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestInterruptedException;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestSkippedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestRunner_UnitTest {

	@Test
	public void if_the_test_is_not_running_then_we_get_a_500_as_a_response(){
		TestInterruptedException error = new TestInterruptedException("testId", "msg");
		TestRunnerSupport support = mock(TestRunnerSupport.class);
		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, "source");

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
	}

	@Test
	public void when_the_test_is_running_then_we_call_handle_exception_on_it_and_return_500(){
		TestInterruptedException error = new TestInterruptedException("testId", "msg");
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);

		verify(testModule, times(1)).handleException(error, source);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
	}

	@Test
	public void a_test_interrupted_exception_without_cause_results_in_an_error_response_with_null_cause(){
		TestInterruptedException error = new TestInterruptedException("testId", "msg");
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);
		JsonObject responseBody = (JsonObject) responseEntity.getBody();

		assertEquals("msg", OIDFJSON.getString(responseBody.get("error")));
		assertNull(responseBody.get("error_description"));
		assertEquals("testId", OIDFJSON.getString(responseBody.get("testId")));
		assertTrue(responseBody.get("cause").isJsonNull());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
	}

	@Test
	public void the_exception_message_of_the_cause_ends_up_in_the_cause_property_in_the_500_response(){
		Exception exception = new Exception("A regular exception");
		TestInterruptedException error = new TestInterruptedException("testId", "msg", exception);
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);
		JsonObject responseBody = (JsonObject) responseEntity.getBody();

		assertEquals(exception.getMessage(), OIDFJSON.getString(responseBody.get("cause")));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
	}

	@Test
	public void runtime_exceptions_as_the_cause_ends_up_in_the_cause_property_in_the_500_response() {
		Exception exception = new RuntimeException("A runtime exception");
		TestInterruptedException error = new TestInterruptedException("testId", "msg", exception);
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);
		JsonObject responseBody = (JsonObject) responseEntity.getBody();

		assertEquals(exception.getMessage(), OIDFJSON.getString(responseBody.get("cause")));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
	}

	@Test
	public void if_the_cause_is_a_condition_error_then_a_400_is_returned() {
		ConditionError exception = new ConditionError("testId", "msg");
		TestInterruptedException error = new TestFailureException(exception);
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);
		JsonObject responseBody = (JsonObject) responseEntity.getBody();

		assertEquals(exception.getMessage(), OIDFJSON.getString(responseBody.get("cause")));
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
	}

	@Test
	public void test_skipped_exceptions_result_in_200_responses() {
		TestInterruptedException error = new TestSkippedException("testId", "msg");
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);
		JsonObject responseBody = (JsonObject) responseEntity.getBody();

		assertEquals("msg", OIDFJSON.getString(responseBody.get("error")));
		assertEquals("testId", OIDFJSON.getString(responseBody.get("testId")));
		assertTrue(responseBody.get("cause").isJsonNull());
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}

	@Test
	public void test_failure_exceptions_with_the_error_message_overloads_result_in_oauth_style_error_responses() {
		TestInterruptedException error = new TestFailureException("testId", "expired_token", "The token has expired");
		String source = "source";

		TestRunnerSupport support = mock(TestRunnerSupport.class);
		TestModule testModule = mock(TestModule.class);
		when(support.getRunningTestById(any())).thenReturn(testModule);

		ResponseEntity<Object> responseEntity = TestRunner.handleTestInterruptedException(error, support, source);
		JsonObject responseBody = (JsonObject) responseEntity.getBody();

		assertEquals("expired_token", OIDFJSON.getString(responseBody.get("error")));
		assertEquals("The token has expired", OIDFJSON.getString(responseBody.get("error_description")));
		assertEquals("testId", OIDFJSON.getString(responseBody.get("testId")));
		assertTrue(responseBody.get("cause").isJsonNull());
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
	}

}
