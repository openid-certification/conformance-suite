package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClearAccessTokenFromRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ClearAccessTokenFromRequest cond;

	private String accesstoken = "123456asbsdfa";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ClearAccessTokenFromRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void test_present() {

		env.putString("incoming_access_token", accesstoken);

		cond.execute(env);

		verify(env, atLeastOnce()).removeNativeValue("incoming_access_token");

		assertNull(env.getString("incoming_access_token"));
	}

	@Test
	public void test_notPresent() {

		cond.execute(env);

		verify(env, atLeastOnce()).removeNativeValue("incoming_access_token");

		assertNull(env.getString("incoming_access_token"));
	}

}
