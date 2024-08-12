package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError_UnitTest {

	@Test
	public void condition_fails_when_http_status_code_is_null() {
		EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError condition = new EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError: Invalid pushed authorization request endpoint response http status code", expectedException.getMessage());
	}

	@Test
	public void condition_fails_when_http_status_code_is_200() {
		EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError condition = new EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status")).thenReturn(200);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError: Invalid pushed authorization request endpoint response http status code", expectedException.getMessage());
	}

	@Test
	public void condition_fails_when_http_status_code_is_400_but_the_error_property_is_missing() {
		EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError condition = new EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status")).thenReturn(400);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError: Expected 'error' field not found", expectedException.getMessage());
	}

	@Test
	public void condition_fails_when_http_status_code_is_400_and_error_field_does_not_match() {
		EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError condition = new EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status")).thenReturn(400);
		String error = "foo";
		when(env.getString(CallPAREndpoint.RESPONSE_KEY, "body_json.error")).thenReturn(error);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError: 'error' field has unexpected value", expectedException.getMessage());
	}

	@Captor
	ArgumentCaptor<Map<String, Object>> logParametersCaptor;

	@Test
	public void condition_passes_when_http_status_code_is_400_and_error_field_is_one_of_the_expected_values() {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);

		EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError condition = new EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError();
		condition.setProperties("UNIT-TEST", log, Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status")).thenReturn(400);

		String error = "request_uri_not_supported";
		when(env.getString(CallPAREndpoint.RESPONSE_KEY, "body_json.error")).thenReturn(error);

		condition.evaluate(env);
		verify(log).log(anyString(), logParametersCaptor.capture());

		Map<String, Object> logParameters = logParametersCaptor.getValue();
		assertEquals("Pushed Authorization Request Endpoint returned expected 'error' of '[invalid_request, invalid_request_object, request_uri_not_supported]'", logParameters.get("msg"));
		assertEquals(Condition.ConditionResult.SUCCESS, logParameters.get("result"));
		assertEquals("request_uri_not_supported", logParameters.get("error"));
	}
}
