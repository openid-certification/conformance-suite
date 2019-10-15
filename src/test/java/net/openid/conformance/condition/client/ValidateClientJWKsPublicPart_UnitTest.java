package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class ValidateClientJWKsPublicPart_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateClientJWKsPublicPart cond;

	private JsonObject client;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateClientJWKsPublicPart();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		client = new JsonObject();
	}

	@Test
	public void testEvaluate_RSAPublicKeyNoError() {
		client.add("jwks", new JsonParser().parse("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"," +
			"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
			+
			"    }" +
			"  ]" +
			"}").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_ECKeyNoError() {
		client.add("jwks", new JsonParser().parse("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"EC\"," +
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"crv\":\"P-256\"," +
			"      \"alg\":\"ES256\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"x\":\"RsJ58leViXVAIvcR0jx7LfnALhm_0qcns3h4v6b8Pdk\"," +
			"      \"y\":\"7Y0pNoArqzvFS_Li45WK3MfUf_YJaxWVVCbfEHPtdo0\"" +
			"    }" +
			"  ]" +
			"}").getAsJsonObject());

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingJWKs() {
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingKeys() {
		client.add("jwks", new JsonObject());
		env.putObject("client", client);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingE() {

		client.add("jwks", new JsonParser().parse("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"," +
			"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
			+
			"    }" +
			"  ]" +
			"}").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingN() {

		client.add("jwks", new JsonParser().parse("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"" +
			"    }" +
			"  ]" +
			"}").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_twoKeys() {
		client.add("jwks", new JsonParser().parse("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"," +
			"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
			+
			"    }," +
			"    {" +
			"      \"kty\": \"EC\"," +
			"      \"use\": \"sig\"," +
			"      \"crv\": \"P-256\"," +
			"      \"kid\": \"mrflibble\"," +
			"      \"x\": \"rCEAb67rCh4INKUdzeYR-msNdWFq-gBS-AfKFGmNp9E\"," +
			"      \"y\": \"YmJrJEmyQSnFJThQT1l8JJgubRRoyX0l9A6LNO7LQo8\"," +
			"      \"alg\": \"ES256\"" +
			"    }" +
			"  ]" +
			"}").getAsJsonObject());
		env.putObject("client", client);

		cond.execute(env);
	}
}
