package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
public class SignClientAuthenticationAssertion_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SignClientAuthenticationAssertion cond;

	private JsonObject claims;

	private JsonObject rsaJwks;

	private JsonObject octJwks;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new SignClientAuthenticationAssertion();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		claims = JsonParser.parseString("{" +
			"	\"iss\": \"client\"," +
			"	\"sub\": \"client\"," +
			"	\"aud\": \"https://server.example.com/token\"," +
			"	\"iat\": 1509992000," +
			"	\"exp\": 1509992060," +
			"	\"jti\": \"987yhjio8765rfghjkoi8ujlloi9876tgh\"" +
			"}").getAsJsonObject();

		rsaJwks = JsonParser.parseString("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"RSA\"," +
			"      \"d\": \"ePRRljk3VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
			+
			"      \"e\": \"AQAB\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"alg\": \"RS256\"," +
			"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
			+
			"    }" +
			"  ]" +
			"}").getAsJsonObject();

		octJwks = JsonParser.parseString("{" +
			"  \"keys\": [" +
			"    {" +
			"      \"kty\": \"oct\"," +
			"      \"use\": \"sig\"," +
			"      \"kid\": \"fapi-test-suite\"," +
			"      \"k\": \"2MNGbRHuWakmiDjTpiewW8_RQsf-FrnWuqvucIiWsIuBBV5WGDgnji6De5pSHDHTVVBA1buLB5V4SqoQoFH45pAToVjyC1TRAmqp6W6iK2-AZqLkpZsh-yvJAT565317m2WEbWZwVjY2EG9qRuiIAjNgv1OSjj7Ct5BpK-mWfRMk0oKeFxbj1_pRURnhc6F36mLbjybXvJ98euMK7OVeCznNP2XUBDzgSUfi-ChfNk98TMsxbqzq0j1SfM6nLayMO_B-pSHfbVl6UrrYIJDY_xTWp-YpB3yKOvm0b8wvr4YMbFQgpH8MLoPjnk6rBqSkwxxnBAkfXA7LHvevFXP61Q\","
			+
			"      \"alg\": \"HS256\"" +
			"    }" +
			"  ]" +
			"}").getAsJsonObject();
	}

	/**
	 */
	@Test
	public void testEvaluate_rsa() {

		env.putObject("client_assertion_claims", claims);
		env.putObject("client_jwks", rsaJwks);

		cond.execute(env);

		assertThat(env.getString("client_assertion")).isNotNull();

		String clientAssertion = env.getString("client_assertion");

		// make sure it's a signed JWT
		try {
			SignedJWT jwt = SignedJWT.parse(clientAssertion);

			// get out the claims as a JsonObject
			JsonObject foundClaims = JWTUtil.jwtClaimsSetAsJsonObject(jwt);

			assertThat(foundClaims).isEqualTo(claims);

		} catch (ParseException e) {
			fail();
		}

	}

	@Test
	public void testEvaluate_oct() {

		env.putObject("client_assertion_claims", claims);
		env.putObject("client_jwks", octJwks);

		cond.execute(env);

		assertThat(env.getString("client_assertion")).isNotNull();

		String clientAssertion = env.getString("client_assertion");

		// make sure it's a signed JWT
		try {
			SignedJWT jwt = SignedJWT.parse(clientAssertion);

			// get out the claims as a JsonObject
			JsonObject foundClaims = JWTUtil.jwtClaimsSetAsJsonObject(jwt);

			assertThat(foundClaims).isEqualTo(claims);

		} catch (ParseException e) {
			fail();
		}

	}

	@Test
	public void testEvaluate_noAlg() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client_assertion_claims", claims);

			env.putObjectFromJsonString("client_jwks", "{" +
				"  \"keys\": [" +
				"    {" +
				"      \"kty\": \"RSA\"," +
				"      \"d\": \"ePRRljk3VGde-JOxwH5LMAY4dERXTLCs7x1NcbuVXF7dyggrbQpDMhCrdH2UJQyvsb4c7H6w9LvosC7UnDgcTzrdd-rccz02XtiTCjP4mXqDUv1qZkYoy-HUq0UH7-WdRJNnx9sZTmWox-yucUbtfdmVRVuI355vNfG3ldsl6kC3DRNKH8nLay0eiS1x62p5S00Fz7TY_hzRqgexsjKu_UNQYDpQcOBVR9qi_mp3Yp8MdsJ2DoBrFJnDTTrG9uNlos1n1kBw-MVQQIPyHKIcElS5b1D-uc53MyxmWUAhiSnf0gpYgk3bYHccAmi_vsRqajLTt0w6hbNozhFht94mq3aLjGQJjt78zSu42hrUqTR-FLHwSoIGilS9SbdYypyYku0ocinwVb51IraDIwJY0_b42aHxE5an9HNaephGvPYPq0jA0xXehLSHqtS9D49OlxcnAvRkyn3TnPbxrdPAaZ6sJEHC8jpdYgKZ9LM7QRAFxwIk3Bxrf4rcEoC_mk5f5in4JYlYmx7VfXSzmSTXLfAWbqpLYRgtaCTFZ-HBLU1VJiAr2nkzw6cewwpxUCrowADO_w0GYRIU3BWamY2i--9wbI2nRUlxaROSMYJx5-A4sWDNPKJWQpX1SUyBpgTO82tHicZ2wzVEqMA4-np_leFBXFvAaMSuIxJ-aQlkLyE\","
				+
				"      \"e\": \"AQAB\"," +
				"      \"use\": \"sig\"," +
				"      \"kid\": \"fapi-test-suite\"," +
				"      \"n\": \"qVGMeRQ8KG9ZVQFXa2YxCiapfesToXILzIvmHqNOJ8EbVwZKcwZ1iHpAzaZhnrSHJBU7Fgz9PEeTAqY-2FrRLE2xz3pW_LCg3y4fb1Sfi5GBSl1es0e4UD4Ie0g5SfQeZIpHfGM9zZx8rmsiDLRUyBsB8kHyUG6Ul5pHpAUHhrtghCpzL5lvnVvCM3-apeSyOdquS09tjwGUPUWifrKr0X1xEoNm7rsChZXQbB-MwqJG-ZGw51udn5-3mMunqp-B37jXdo4tYYOg0kKrpZSqQdRPANmTbge3LIN7uYprFVmGDMvXwiLMF9Unkeudox6nuIy3MWSeFRWpcrSQ_7s5p4mSupZxWHWrN_qSM-H9mTosvPg32m7XjQvlEJhuXqn13VYd8ZhmYSqV7s_x8bveZJLBnNPAkjPDktz8eUsdX3S8X6MNoYh4Ch7rsE_FO21B3FcMwtFql11z3_3PCAQOB3lzggy1hs-4Zb1As84R1Fmy0WKHTPAKPlJtLNFKz_bVf7xRbrt8BlPbkF4HduQixi-twkWRkUz8_-zCzWhz6s9y5ltQgs0_rl9aYSAt5z8UarP1PNfQIYGBmhwqGHi2LmrYFRjBEHRIsDXJcUGWgkfKMsiECs-mT-6AIw2L5DZIMIo8aroEnVpS24Ln2fwpdrRjO-b65VW-5cFOM6gERCc\""
				+
				"    }" +
				"  ]" +
				"}");

			cond.execute(env);

		});

	}
	@Test
	public void testEvaluate_noClaims() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client_jwks", rsaJwks);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_noJwks() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client_assertion_claims", claims);

			cond.execute(env);

		});

	}
}
