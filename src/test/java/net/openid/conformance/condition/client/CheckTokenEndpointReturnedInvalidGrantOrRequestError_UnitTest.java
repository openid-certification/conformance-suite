package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckTokenEndpointReturnedInvalidGrantOrRequestError_UnitTest {

	@Test
	public void the_condition_throws_if_the_error_is_invalid_client() {
		CheckTokenEndpointReturnedInvalidGrantOrRequestError condition = new CheckTokenEndpointReturnedInvalidGrantOrRequestError();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		Environment env = mock(Environment.class);
		when(env.getInteger("token_endpoint_response_http_status")).thenReturn(400);
		when(env.getString("token_endpoint_response", "error")).thenReturn("invalid_client");

		Exception expectedException = assertThrows(ConditionError.class, () -> condition.evaluate(env));
		assertEquals("CheckTokenEndpointReturnedInvalidGrantOrRequestError: Unexpected error 'invalid_client' received", expectedException.getMessage());
	}

}
