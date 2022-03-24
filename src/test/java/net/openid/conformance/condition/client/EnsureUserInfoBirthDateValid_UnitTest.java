package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class EnsureUserInfoBirthDateValid_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureUserInfoBirthDateValid cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureUserInfoBirthDateValid();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_notPresentBirthDate() {

		JsonObject userInfo = new JsonObject();

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_birthDateWithFormat_yyyyMMdd() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "1992-01-01");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_birthDateWithFormat_0000MMdd() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "0000-03-22");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_birthDateWithFormat_yyyy() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "1992");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateEmpty() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalid() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "0000-13-32");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalid1() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "0000");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalid2() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "1648113552");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidWithTime() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "2000-01-01T00:00:00.000Z");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidMonth() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "2022-14-22");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidDay() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "2022-02-30");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidYear() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "12345-02-01");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidYearFuture() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "2400-02-01");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidYear2() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "20222");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidYear3() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "a2022");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_birthDateInvalidYear4() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("birthdate", "2022a");

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

}
