package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
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

	@Test
	public void test_bad() {
		assertThrows(ConditionError.class, () -> {

			env.putString("scope", badScope);
			cond.execute(env);

			verify(env, atLeastOnce()).getString("scope");

		});

	}

	@Test
	public void test_empty() {
		assertThrows(ConditionError.class, () -> {

			env.putString("scope", emptyScope);
			cond.execute(env);

			verify(env, atLeastOnce()).getString("scope");

		});

	}
}
