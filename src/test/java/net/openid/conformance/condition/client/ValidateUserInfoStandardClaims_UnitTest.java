package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

@RunWith(MockitoJUnitRunner.class)
public class ValidateUserInfoStandardClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateUserInfoStandardClaims cond;

	private JsonObject userInfo;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateUserInfoStandardClaims();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		userInfo = new JsonParser().parse("{"
			+ "\"sub\":\"248289761001\","
			+ "\"name\":\"Jane Doe\","
			+ "\"given_name\":\"Jane\","
			+ "\"family_name\":\"Doe\","
			+ "\"preferred_username\":\"j.doe\","
			+ "\"email\":\"janedoe@example.com\","
			+ "\"picture\":\"http://example.com/janedoe/me.jpg\","
			+ "\"birthdate\":\"0000-03-22\","
			+ "\"eye_color\":\"blue\","
			+ "\"phone_number\":\"+1 (310) 123-4567\","
			+ "\"credit_score\": 650,"
			+ "\"address\": {"
			+ "\"street_address\":\"1234 Hollywood Blvd.\","
			+ "\"locality\":\"Los Angeles\","
			+ "\"region\":\"CA\","
			+ "\"postal_code\":\"90210\","
			+ "\"country\":\"US\""
			+ "},"
			+ "\"_claim_names\": {"
			+ "\"payment_info\":\"src1\","
			+ "\"shipping_address\":\"src1\","
			+ "\"credit_score\":\"src2\""
			+ "},"
			+ "\"_claim_sources\": {"
			+ "\"src1\": {"
			+ "\"endpoint\":\"https://bank.example.com/claim_source\""
			+ "},"
			+ "\"src2\": {"
			+ "\"endpoint\":\"https://creditagency.example.com/claims_here\","
			+ "\"access_token\":\"ksj3n283dke\""
			+ "}"
			+ "}"
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError1() {

		userInfo = new JsonParser().parse("{"
			+ "\"sub\":\"248289761001\","
			+ "\"name\":\"Jane Doe\","
			+ "\"given_name\":\"Jane\","
			+ "\"family_name\":\"Doe\","
			+ "\"preferred_username\":\"j.doe\","
			+ "\"email\":\"janedoe@example.com\","
			+ "\"picture\":\"http://example.com/janedoe/me.jpg\""
			+ "}").getAsJsonObject();

		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithEmailVerifiedIsNotBoolean() {
		userInfo.addProperty("email_verified", "true");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithUpdatedAtIsNotNumber() {
		userInfo.addProperty("updated_at", "is not a number");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithClaimsNameIsNotJsonObject() {
		userInfo.addProperty("_claim_names", "is not JSON object");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithClaimsSourceIsNotJsonObject() {
		userInfo.addProperty("_claim_sources", "is not JSON object");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorWithAddressIsEmptyJsonObject() {
		userInfo.add("address", new JsonObject());
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

}
