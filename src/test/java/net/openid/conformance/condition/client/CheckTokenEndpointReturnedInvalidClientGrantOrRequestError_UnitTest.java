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
public class CheckTokenEndpointReturnedInvalidClientGrantOrRequestError_UnitTest {

	@Test
	public void the_condition_throws_if_the_http_status_code_is_null() {
		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(null);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientGrantOrRequestError: Http status can not be null.", expectedException.getMessage());
	}

	@Test
	public void the_condition_throws_if_the_error_property_is_not_set_in_environment() {
		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientGrantOrRequestError: Couldn't find error field", expectedException.getMessage());
	}

	@Test
	public void the_condition_throws_if_the_error_property_is_not_recognized() {
		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(400);
		when(env.getString("token_endpoint_response", "error")).thenReturn("foo");

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientGrantOrRequestError: Unexpected error 'foo' received", expectedException.getMessage());
	}

	@Test
	public void the_condition_throws_if_the_error_is_invalid_request_but_the_status_code_is_not_400() {
		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(401);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_request");

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientGrantOrRequestError: Invalid http status with error invalid_request", expectedException.getMessage());
	}

	@Test
	public void the_condition_throws_if_the_error_is_invalid_grant_but_the_status_code_is_not_400() {
		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(401);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_grant");

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientGrantOrRequestError: Invalid http status with error invalid_grant", expectedException.getMessage());
	}

	@Test
	public void the_condition_throws_if_the_error_is_invalid_client_but_the_status_code_is_not_400_or_401() {
		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(404);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_client");

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientGrantOrRequestError: Invalid http status with error invalid_client", expectedException.getMessage());
	}

	@Captor
	ArgumentCaptor<Map<String, Object>> logParametersCaptor;

	@Test
	public void the_condition_succeeds_if_the_error_is_invalid_request_and_the_http_status_code_is_400() {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);

		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", log, Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(400);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_request");

		condition.evaluate(env);
		verify(log).log(anyString(), logParametersCaptor.capture());

		Map<String, Object> logParameters = logParametersCaptor.getValue();
		assertEquals("Token endpoint returned error invalid_request and the http status code was 400", logParameters.get("msg"));
		assertEquals(Condition.ConditionResult.SUCCESS, logParameters.get("result"));
	}

	@Test
	public void the_condition_succeeds_if_the_error_is_invalid_grant_and_the_http_status_code_is_400() {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);

		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", log, Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(400);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_grant");

		condition.evaluate(env);
		verify(log).log(anyString(), logParametersCaptor.capture());

		Map<String, Object> logParameters = logParametersCaptor.getValue();
		assertEquals("Token endpoint returned error invalid_grant and the http status code was 400", logParameters.get("msg"));
		assertEquals(Condition.ConditionResult.SUCCESS, logParameters.get("result"));
	}

	@Test
	public void the_condition_succeeds_if_the_error_is_invalid_client_and_the_http_status_code_is_400() {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);

		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", log, Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(400);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_client");

		condition.evaluate(env);
		verify(log).log(anyString(), logParametersCaptor.capture());

		Map<String, Object> logParameters = logParametersCaptor.getValue();
		assertEquals("Token endpoint returned error invalid_client and the http status code was 400", logParameters.get("msg"));
		assertEquals(Condition.ConditionResult.SUCCESS, logParameters.get("result"));
	}

	@Test
	public void the_condition_succeeds_if_the_error_is_invalid_client_and_the_http_status_code_is_401() {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);

		CheckTokenEndpointReturnedInvalidClientGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidClientGrantOrRequestError();
		condition.setProperties("UNIT-TEST", log, Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(401);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_client");

		condition.evaluate(env);
		verify(log).log(anyString(), logParametersCaptor.capture());

		Map<String, Object> logParameters = logParametersCaptor.getValue();
		assertEquals("Token endpoint returned error invalid_client and the http status code was 401", logParameters.get("msg"));
		assertEquals(Condition.ConditionResult.SUCCESS, logParameters.get("result"));
	}
}
