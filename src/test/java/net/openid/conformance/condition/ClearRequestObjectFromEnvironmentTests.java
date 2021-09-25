package net.openid.conformance.condition;

import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.ClearRequestObjectFromEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class ClearRequestObjectFromEnvironmentTests {

	@Test
	public void resourceEntityIsClearedIfPresent() {

		Environment environment = new Environment();
		environment.putString("resource_request_entity", "foo");

		ClearRequestObjectFromEnvironment condition = new ClearRequestObjectFromEnvironment();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.execute(environment);

		String requestEntity = environment.getString("resource_request_entity");

		assertNull(requestEntity);

	}

	@Test
	public void resourceEntityIsOptional() {

		Environment environment = new Environment();

		ClearRequestObjectFromEnvironment condition = new ClearRequestObjectFromEnvironment();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.execute(environment);

		String requestEntity = environment.getString("resource_request_entity");

		assertNull(requestEntity);

	}

}
