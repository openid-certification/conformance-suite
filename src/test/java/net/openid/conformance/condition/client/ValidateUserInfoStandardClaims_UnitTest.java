package net.openid.conformance.condition.client;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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

@ExtendWith(MockitoExtension.class)
public class ValidateUserInfoStandardClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateUserInfoStandardClaims cond;

	private JsonObject userInfo;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateUserInfoStandardClaims();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		userInfo = JsonParser.parseString("{"
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

		userInfo = JsonParser.parseString("{"
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

	@Test
	public void testEvaluate_noErrorOnlySub() {
		// technically userinfo must contain sub; EnsureUserInfoContainsSub checks that separately
		userInfo = JsonParser.parseString("""
			{\
			  "sub": "foo"
			}\
			""").getAsJsonObject();

		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError2() {

		userInfo = JsonParser.parseString("""
				{
				  "sub": "foo",
				  "address": {
				    "country": "000",
				    "formatted": "000",
				    "locality": "000",
				    "postal_code": "000",
				    "region": "000",
				    "street_address": "000"
				  },
				  "email": "johndoe@example.com",
				  "email_verified": false,
				  "phone_number": "+49 000 000000",
				  "phone_number_verified": false,
				  "birthdate": "1987-10-16",
				  "family_name": "Doe",
				  "gender": "male",
				  "given_name": "John",
				  "locale": "en-US",
				  "middle_name": "Middle",
				  "name": "John Doe",
				  "nickname": "Johny",
				  "picture": "http://lorempixel.com/400/200/",
				  "preferred_username": "johnny",
				  "profile": "https://johnswebsite.com",
				  "updated_at": 1454704946,
				  "website": "http://example.com",
				  "zoneinfo": "Europe/Berlin"
				}""").getAsJsonObject();

		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_errorWithEmptyName() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("name", "");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithBlankName() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("name", " ");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithNullName() {
		assertThrows(ConditionError.class, () -> {
			userInfo.add("name", JsonNull.INSTANCE);
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithStringContainingNull() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("name", "null");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithEmailVerifiedIsNotBoolean() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("email_verified", "true");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithEmailVerifiedIsNull() {
		assertThrows(ConditionError.class, () -> {
			userInfo.add("email_verified", JsonNull.INSTANCE);
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithBirthDate() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("birthdate", "2022-14-22");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithAddress() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("address", "true");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithAddressCountry() {
		assertThrows(ConditionError.class, () -> {
			JsonObject address = userInfo.get("address").getAsJsonObject();
			address.addProperty("country", 3);
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithUpdatedAtIsNotNumber() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("updated_at", "is not a number");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithClaimsNameIsNotJsonObject() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("_claim_names", "is not JSON object");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithClaimsSourceIsNotJsonObject() {
		assertThrows(ConditionError.class, () -> {
			userInfo.addProperty("_claim_sources", "is not JSON object");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorWithAddressIsEmptyJsonObject() {
		assertThrows(ConditionError.class, () -> {
			userInfo.add("address", new JsonObject());
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
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

	@Test
	public void testEvaluate_birthDateEmpty() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalid() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "0000-13-32");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalid1() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "0000");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalid2() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "1648113552");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidWithTime() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "2000-01-01T00:00:00.000Z");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidMonth() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "2022-14-22");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidDay() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "2022-02-30");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidYear() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "12345-02-01");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidYearFuture() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "2400-02-01");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidYear2() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "20222");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidYear3() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "a2022");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_birthDateInvalidYear4() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("birthdate", "2022a");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

}
