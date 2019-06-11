package io.fintechlabs.testframework.condition.rs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertNull;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ClearAccessTokenFromRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ClearAccessTokenFromRequest cond;

	private String accesstoken = "123456asbsdfa";

	@Before
	public void setUp() throws Exception {
		cond = new ClearAccessTokenFromRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void test_present() {

		env.putString("incoming_access_token", accesstoken);

		cond.evaluate(env);

		verify(env, atLeastOnce()).removeNativeValue("incoming_access_token");

		assertNull(env.getString("incoming_access_token"));
	}

	@Test
	public void test_notPresent() {

		cond.evaluate(env);

		verify(env, atLeastOnce()).removeNativeValue("incoming_access_token");

		assertNull(env.getString("incoming_access_token"));
	}

}
