package net.openid.conformance.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureOpenIDInScopeRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureOpenIDInScopeRequest cond;

	private String goodScope = "openid foo bar";

	private String onlyScope = "openid";

	private String badScope = "foo bar";

	private String emptyScope = "";

	@Before
	public void setUp() throws Exception {
		cond = new EnsureOpenIDInScopeRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void test_good() {

		env.putString("scope", goodScope);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("scope");
	}

	@Test
	public void test_only() {

		env.putString("scope", onlyScope);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("scope");

	}

	@Test(expected = ConditionError.class)
	public void test_bad() {

		env.putString("scope", badScope);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("scope");

	}

	@Test(expected = ConditionError.class)
	public void test_empty() {

		env.putString("scope", emptyScope);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("scope");

	}
}
