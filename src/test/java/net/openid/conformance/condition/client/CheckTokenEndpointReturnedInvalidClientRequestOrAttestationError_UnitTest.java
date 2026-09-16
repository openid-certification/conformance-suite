package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
public class CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError_UnitTest {

	@Captor
	ArgumentCaptor<Map<String, Object>> logParametersCaptor;

	private CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError createCondition(TestInstanceEventLog log) {
		CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError condition = new CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError();
		condition.setProperties("UNIT-TEST", log, Condition.ConditionResult.FAILURE);
		return condition;
	}

	@Test
	public void the_condition_throws_if_the_http_status_code_is_null() {
		CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError condition = createCondition(mock(TestInstanceEventLog.class));

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(null);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError: Http status can not be null.", expectedException.getMessage());
	}

	@Test
	public void the_condition_throws_if_the_error_property_is_not_set_in_environment() {
		CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError condition = createCondition(mock(TestInstanceEventLog.class));

		Environment env = mock(Environment.class);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError: Couldn't find error field", expectedException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({"foo", "invalid_grant"})
	public void the_condition_throws_if_the_error_property_is_not_recognized(String error) {
		CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError condition = createCondition(mock(TestInstanceEventLog.class));

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(400);
		when(env.getString("token_endpoint_response", "error")).thenReturn(error);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError: Unexpected error '" + error + "' received", expectedException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
		"invalid_request, 401",
		"invalid_client, 404",
		"invalid_client_attestation, 403",
		"use_fresh_attestation, 403"
	})
	public void the_condition_throws_if_the_status_code_is_not_permitted_for_the_error(String error, int httpStatus) {
		CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError condition = createCondition(mock(TestInstanceEventLog.class));

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(httpStatus);
		when(env.getString("token_endpoint_response", "error")).thenReturn(error);

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError: Invalid http status with error " + error, expectedException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
		"invalid_request, 400",
		"invalid_client, 400",
		"invalid_client, 401",
		"invalid_client_attestation, 400",
		"invalid_client_attestation, 401",
		"use_fresh_attestation, 400",
		"use_fresh_attestation, 401"
	})
	public void the_condition_succeeds_if_the_status_code_is_permitted_for_the_error(String error, int httpStatus) {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);
		CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError condition = createCondition(log);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(httpStatus);
		when(env.getString("token_endpoint_response", "error")).thenReturn(error);

		condition.evaluate(env);
		verify(log).log(anyString(), logParametersCaptor.capture());

		Map<String, Object> logParameters = logParametersCaptor.getValue();
		assertEquals("Token endpoint returned error " + error + " and the http status code was " + httpStatus, logParameters.get("msg"));
		assertEquals(Condition.ConditionResult.SUCCESS, logParameters.get("result"));
	}
}
