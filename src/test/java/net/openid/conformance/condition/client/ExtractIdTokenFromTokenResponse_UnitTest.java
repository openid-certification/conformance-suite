package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExtractIdTokenFromTokenResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodResponse;

	private JsonObject badResponse;

	private ExtractIdTokenFromTokenResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractIdTokenFromTokenResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodResponse = new JsonParser().parse("{"
			+ "\"access_token\":"
			+ "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
			+ "eyJzdWIiOiJhZG1pbiIsImF6cCI6ImNsaWVudCIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU5NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6Ijc3MmJiODBhLWQ2ZTEtNGUwMi1hN2M3LTAxNWMwZGQyMjdkNSJ9."
			+ "OsuhXSzoODk6VdUjPSyq9vp_ZLqgNczSF1Gvz8-wh1mGdN1aadWy2faKyqIXoQ7OrOycNkOigmyhYoGYoqM30SmkSm9bmpoTOxCAQDclPRdTjmV_-cF9bRLTvaz-78xOcotk4c9kJQHPvUdXiistvnUywME80rKr5K4m4oAYrv-Ii6ntXK6Wbatx3wjEE8Erkq1dkBO9FKCjqIpVeTM-106uFD6yw4sRZZOgY9ibzovp9522l3VkjaU88UEjTu7WbSaLLyaEgiBVSDoweTogEzI-BIZhjtkaxiDzgKvXUJFF8jxyU-2YB4wgs5oi6SUkWJk825I3Lk2v4EZjE3pZag\","
			+ "\"token_type\":\"Bearer\","
			+ "\"expires_in\":3599,"
			+ "\"scope\":\"address phone openid email profile\","
			+ "\"id_token\":\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
			+ "eyJzdWIiOiI5MDM0Mi5BU0RGSldGQSIsImF1ZCI6ImNsaWVudCIsImF1dGhfdGltZSI6MTUwNjQ1NDIyNywia2lkIjoicnNhMSIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU2NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6IjM2YzU4M2M5LTE1M2UtNDBhOC05M2MzLWEzNWZkYTgwM2QzOCJ9."
			+ "SuGDMrdIh_tCsoWt51xs7rs036KAL3OcwdTcJxmuEUT24EufZILQ9_2rNX8BLA9S-YwYkS243oFz9UfBmnnqj6H27-BO7yBSwGnofDwV6GN4yLXmJfrzC6EEvSPkYMnHo7ha2eIUDFEHcTuKg1eSyKvkaPhklg3R5QHl4xo43FnKfQ8TrhAEH07FNKGFVS67xr00a17OD8VNn3LlZISr-iVbaueNBeYD9obUEmL5IJR8Y37qNK4egirn41BXQKK7xguF2nebQpN-1lcewW5OnEWy7yGd7M88l-WVzfNyFCM75bKZFAbv_W2w1glh38M2DJNRbe2SJhMxkpMxwUeLBA\""
			+ "}").getAsJsonObject();

		badResponse = new JsonParser().parse("{"
			+ "\"access_token\":"
			+ "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
			+ "eyJzdWIiOiJhZG1pbiIsImF6cCI6ImNsaWVudCIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU5NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6Ijc3MmJiODBhLWQ2ZTEtNGUwMi1hN2M3LTAxNWMwZGQyMjdkNSJ9."
			+ "OsuhXSzoODk6VdUjPSyq9vp_ZLqgNczSF1Gvz8-wh1mGdN1aadWy2faKyqIXoQ7OrOycNkOigmyhYoGYoqM30SmkSm9bmpoTOxCAQDclPRdTjmV_-cF9bRLTvaz-78xOcotk4c9kJQHPvUdXiistvnUywME80rKr5K4m4oAYrv-Ii6ntXK6Wbatx3wjEE8Erkq1dkBO9FKCjqIpVeTM-106uFD6yw4sRZZOgY9ibzovp9522l3VkjaU88UEjTu7WbSaLLyaEgiBVSDoweTogEzI-BIZhjtkaxiDzgKvXUJFF8jxyU-2YB4wgs5oi6SUkWJk825I3Lk2v4EZjE3pZag\","
			+ "\"token_type\":\"Bearer\","
			+ "\"expires_in\":3599,"
			+ "\"scope\":\"address phone openid email profile\""
			+ "}").getAsJsonObject();

	}

	/**
	 * Test method for {@link ExtractIdTokenFromTokenResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_response", goodResponse);

		cond.execute(env);

		verify(env, atLeastOnce()).getElementFromObject("token_endpoint_response", "id_token");

		assertThat(env.getObject("id_token")).isNotNull();
		assertThat(env.getString("id_token", "value")).isEqualTo(OIDFJSON.getString(goodResponse.get("id_token")));
		assertThat(env.getElementFromObject("id_token", "header")).isNotNull();
		assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
	}

	/**
	 * Test method for {@link ExtractIdTokenFromTokenResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.putObject("token_endpoint_response", badResponse);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_encrypted() {
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
		JsonObject response = new JsonObject();
		String decryptedIdToken = "eyJraWQiOiJhdXRobGV0ZS1mYXBpZGV2LWFwaS0yMDE4MDUyNCIsImFsZyI6IlBTMjU2In0.eyJzdWIiOiIxMDAxIiwiYXVkIjpbIjIzMzQzODIzNTQxNTM0OTgiXSwiYWNyIjoidXJuOm1hY2U6aW5jb21tb246aWFwOnNpbHZlciIsImNfaGFzaCI6Ilk5SEFvWDUxM2xCZi1ObDhPY1ZLZGciLCJzX2hhc2giOiJ6WWdCSTFSal9XYXVCOGlXMnJaZ1p3IiwiYXV0aF90aW1lIjoxNTkzMDAxNjIyLCJpc3MiOiJodHRwczovL2ZhcGlkZXYtYXMuYXV0aGxldGUubmV0LyIsImV4cCI6MTU5MzAwMTkyMiwiaWF0IjoxNTkzMDAxNjIyLCJub25jZSI6Ik1LaFFjRzM3ZzkifQ.DaIaQB-ypPnB3humfuqW7VeTAsdWUNJSQFrAsjO3lHlslPT9neFe31zBnfn_1sODEMJHOA2Cep2P6crPFVsL-9UuWepB9El7_2j29QP0TY9O1bfafZ2BsUopTFkTfM8uE8YmdYPuvwQRAnkf3qCvczhAHjGVdS4zVggTirOz8QIWhKOphhkXeEsz4xbbUxWL0mG7cVxagWgoQYJydixOyuUM18JlhS_8XtUROsMxeJib_IvHSGPYV0JUZC4-MeVnsagxTqeX79Qx2nU7_-UE2RaB9KYZpGA9lhUw24mLL-U3lvLDODsoD5-rH3K6S57hsCl4bWrKDCvhMpfJY9YVIg";
		response.addProperty("id_token", idToken);

		String jwks =
			"{\n" +
			"    \"keys\": [\n" +
			"        {\n" +
			"            \"kty\": \"RSA\",\n" +
			"            \"d\": \"ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ\",\n" +
			"            \"e\": \"AQAB\",\n" +
			"            \"kid\": \"fapi-jwt-assertion-20180817-1\",\n" +
			"            \"alg\": \"PS256\",\n" +
			"            \"n\": \"9sLLHll-BiuwIvupyiAOaTR-ChAjuwQhThxtSD5wgL9AG2YJlamxo52ZEhTdNKomGRw3woihBRk8okPSd1YZCkFuOc7iF1sUsxDA0APbdeAzZdwGcqvPlEqoz8HsnormZFTP9tG451Z_cMd20_cCufGqi6XBpE2fOMGhn9CNEo-_3TPyrFZwmP-rqsQ3TKp5Zkb42AACsT9GuxB-Qrrpp1hkCJdSvXhAwIvX2jOxXQORJZkW2ST0DpCTbtAoPEW0aoCIkwWjV08qTSBduA14eIl9xPACfElosTDEbL1bR6BGxoILSJTe4U5Lz4Us7l8TWhO_OaQsyxSLFxXOy13KMQ\"\n" +
			"        },\n" +
			"        {\n" +
			"            \"p\": \"z0FVeicrECBglwnSTGSH-Xq1VtYVcjIVb6g4T2fPmgZnt6a3yu7MJpPKl6h8kaOz-tQcNt_u48G2Zyu7E76Y9RUn43MZncW1hAq4VxQ_rKAZmFyI38pzbELTwcg5E8G0VAL54wdBzAI7R4a_fTR2OsRyl0KBqcSaRAt4PxT1kg8\",\n" +
			"            \"kty\": \"RSA\",\n" +
			"            \"alg\": \"RSA-OAEP\",\n" +
			"            \"q\": \"rpVBITHKspU9lS2JCnITxGfOPNRndBacvNB182IR3FaVv5UlO7VXY6r9_Ry0gsxzIsbNpRcyG1FX81baa9mF_v8KQ_XJ-GjtT-wZy2jPqBpe22jMtMdzGdSMkaw8jqHORUwvoLUOk6Bv5nAOCZRiopSrSXpOUASVZJUUpSxGlRU\",\n" +
			"            \"d\": \"OjDe8EkZXgvB-Gy5A4EdU8fBuAjdHLMyHKAtMaS_W_joEJHDvZRhIYbh1jAyHYoR3kFMXutCIYpRjDrsUEhjYuVKLm90CVtysoRjjkiXyupcEW3o--X_HBJhKm1Y-0I7LQ-cA7CotJpTVMR2fRTqP1T4FsORAjg9l-fbdpVmeDiZBRbL2zCWmKWhtDpHyy7vbSCRghntihz_M5Hrchk7r8ito_K3dFrV9IZSF9RoEY7kyK5bL36Kpgai44PYCzqOzqP2fteO_rZ9fn-uK59pI3ySo_PgSbJ55n14Nd9Z8m70zE9Z4aIeNDEFspZUhavngRwc7MuJ7f_hVGQ9RFbbkQ\",\n" +
			"            \"e\": \"AQAB\",\n" +
			"            \"use\": \"enc\",\n" +
			"            \"kid\": \"fapi20200623\",\n" +
			"            \"qi\": \"IyiL1_cnC5Najrfvu6ypiR3JmpHXDs8FkYJUdfqXnVWaBNxkdDi3iks943JyIfp8JI-NWndiNB6DdSBzecARDqew3lQomIsGsoR0wPFcHDee-d-NmBwEm3TSHrleGjj0oBJe6BDnAdsaHhsL9NLo_1aOd_9W_TM2kcuSntM-DFA\",\n" +
			"            \"dp\": \"jjER1tu2hLrh6d34JSc3zubsMOZyEkXgRRnRgFEFsnPAhtRf9l99Ot5cuU4EINuCaI1Lyi46tJG7de8fy30RbdwU4Myf_4mcbjn4nO2sfd_dj5W05mz8YYM7yxB2cGKOOLFOBf99mdzSFNGS4PC0SL9sqvAbC4FyIUsJNaZIkOs\",\n" +
			"            \"dq\": \"xGck3jMl1cIPhcO0aAvuMQaW_df2iqLlsYTTLPsnHLpLvTwMpx9bMMUs95NTf9KBtJ3yu8dcl17rktYi4zHTjQtegRYCIXPphgAXBL7k3jjMfgloQfgfhO-ZNEiP1-YwJ8WNzz3hKDNVr-hf4mcMj9qw1_jAIUUGzCDrGOa2gQ\",\n" +
			"            \"n\": \"jVc92j0ntTV0V1nwZ3mpGaV2bME4d6AMS2SRrJBM0fLehaTEqDNzGu0warz2SC9bhcBOB5_q3mYBFjmTwWzSbsk6RYETnAgViXg67PgH7Vkx2NCtwgQW3cNdnUZWRNYHsoevkx_Ta1X6Vi9ulebU_BCKjrF-6CjVcGgEsO_S5DKcukGHdf81WlQOq3zGQg4h7MLArrbPSTHHORDsu_87qY9m2EhiYSOBSF5rHsfDo7zWI5FWNG-_HO-CBM005bykIIS1aXCXx1jOW1OrKcp5xv3e-BR6MJTxncZJ4o1GtynJI8kLXRgltLArSOkbzNEr9GjU9lnSSxKLMtRLKkG2Ow\"\n" +
			"        }\n" +
			"    ]\n" +
			"}\n";

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);

		assertThat(env.getObject("id_token")).isNotNull();
		assertThat(env.getString("id_token", "value")).isEqualTo(decryptedIdToken);
		assertThat(env.getElementFromObject("id_token", "header")).isNotNull();
		assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
	}

	@Test
	public void testEvaluate_encryptedNoAlgInEncKey() {
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
		JsonObject response = new JsonObject();
		String decryptedIdToken = "eyJraWQiOiJhdXRobGV0ZS1mYXBpZGV2LWFwaS0yMDE4MDUyNCIsImFsZyI6IlBTMjU2In0.eyJzdWIiOiIxMDAxIiwiYXVkIjpbIjIzMzQzODIzNTQxNTM0OTgiXSwiYWNyIjoidXJuOm1hY2U6aW5jb21tb246aWFwOnNpbHZlciIsImNfaGFzaCI6Ilk5SEFvWDUxM2xCZi1ObDhPY1ZLZGciLCJzX2hhc2giOiJ6WWdCSTFSal9XYXVCOGlXMnJaZ1p3IiwiYXV0aF90aW1lIjoxNTkzMDAxNjIyLCJpc3MiOiJodHRwczovL2ZhcGlkZXYtYXMuYXV0aGxldGUubmV0LyIsImV4cCI6MTU5MzAwMTkyMiwiaWF0IjoxNTkzMDAxNjIyLCJub25jZSI6Ik1LaFFjRzM3ZzkifQ.DaIaQB-ypPnB3humfuqW7VeTAsdWUNJSQFrAsjO3lHlslPT9neFe31zBnfn_1sODEMJHOA2Cep2P6crPFVsL-9UuWepB9El7_2j29QP0TY9O1bfafZ2BsUopTFkTfM8uE8YmdYPuvwQRAnkf3qCvczhAHjGVdS4zVggTirOz8QIWhKOphhkXeEsz4xbbUxWL0mG7cVxagWgoQYJydixOyuUM18JlhS_8XtUROsMxeJib_IvHSGPYV0JUZC4-MeVnsagxTqeX79Qx2nU7_-UE2RaB9KYZpGA9lhUw24mLL-U3lvLDODsoD5-rH3K6S57hsCl4bWrKDCvhMpfJY9YVIg";
		response.addProperty("id_token", idToken);

		String jwks =
			"{\n" +
				"    \"keys\": [\n" +
				"        {\n" +
				"            \"kty\": \"RSA\",\n" +
				"            \"d\": \"ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ\",\n" +
				"            \"e\": \"AQAB\",\n" +
				"            \"kid\": \"fapi-jwt-assertion-20180817-1\",\n" +
				"            \"alg\": \"PS256\",\n" +
				"            \"n\": \"9sLLHll-BiuwIvupyiAOaTR-ChAjuwQhThxtSD5wgL9AG2YJlamxo52ZEhTdNKomGRw3woihBRk8okPSd1YZCkFuOc7iF1sUsxDA0APbdeAzZdwGcqvPlEqoz8HsnormZFTP9tG451Z_cMd20_cCufGqi6XBpE2fOMGhn9CNEo-_3TPyrFZwmP-rqsQ3TKp5Zkb42AACsT9GuxB-Qrrpp1hkCJdSvXhAwIvX2jOxXQORJZkW2ST0DpCTbtAoPEW0aoCIkwWjV08qTSBduA14eIl9xPACfElosTDEbL1bR6BGxoILSJTe4U5Lz4Us7l8TWhO_OaQsyxSLFxXOy13KMQ\"\n" +
				"        },\n" +
				"        {\n" +
				"            \"p\": \"z0FVeicrECBglwnSTGSH-Xq1VtYVcjIVb6g4T2fPmgZnt6a3yu7MJpPKl6h8kaOz-tQcNt_u48G2Zyu7E76Y9RUn43MZncW1hAq4VxQ_rKAZmFyI38pzbELTwcg5E8G0VAL54wdBzAI7R4a_fTR2OsRyl0KBqcSaRAt4PxT1kg8\",\n" +
				"            \"kty\": \"RSA\",\n" +
				"            \"q\": \"rpVBITHKspU9lS2JCnITxGfOPNRndBacvNB182IR3FaVv5UlO7VXY6r9_Ry0gsxzIsbNpRcyG1FX81baa9mF_v8KQ_XJ-GjtT-wZy2jPqBpe22jMtMdzGdSMkaw8jqHORUwvoLUOk6Bv5nAOCZRiopSrSXpOUASVZJUUpSxGlRU\",\n" +
				"            \"d\": \"OjDe8EkZXgvB-Gy5A4EdU8fBuAjdHLMyHKAtMaS_W_joEJHDvZRhIYbh1jAyHYoR3kFMXutCIYpRjDrsUEhjYuVKLm90CVtysoRjjkiXyupcEW3o--X_HBJhKm1Y-0I7LQ-cA7CotJpTVMR2fRTqP1T4FsORAjg9l-fbdpVmeDiZBRbL2zCWmKWhtDpHyy7vbSCRghntihz_M5Hrchk7r8ito_K3dFrV9IZSF9RoEY7kyK5bL36Kpgai44PYCzqOzqP2fteO_rZ9fn-uK59pI3ySo_PgSbJ55n14Nd9Z8m70zE9Z4aIeNDEFspZUhavngRwc7MuJ7f_hVGQ9RFbbkQ\",\n" +
				"            \"e\": \"AQAB\",\n" +
				"            \"use\": \"enc\",\n" +
				"            \"kid\": \"fapi20200623\",\n" +
				"            \"qi\": \"IyiL1_cnC5Najrfvu6ypiR3JmpHXDs8FkYJUdfqXnVWaBNxkdDi3iks943JyIfp8JI-NWndiNB6DdSBzecARDqew3lQomIsGsoR0wPFcHDee-d-NmBwEm3TSHrleGjj0oBJe6BDnAdsaHhsL9NLo_1aOd_9W_TM2kcuSntM-DFA\",\n" +
				"            \"dp\": \"jjER1tu2hLrh6d34JSc3zubsMOZyEkXgRRnRgFEFsnPAhtRf9l99Ot5cuU4EINuCaI1Lyi46tJG7de8fy30RbdwU4Myf_4mcbjn4nO2sfd_dj5W05mz8YYM7yxB2cGKOOLFOBf99mdzSFNGS4PC0SL9sqvAbC4FyIUsJNaZIkOs\",\n" +
				"            \"dq\": \"xGck3jMl1cIPhcO0aAvuMQaW_df2iqLlsYTTLPsnHLpLvTwMpx9bMMUs95NTf9KBtJ3yu8dcl17rktYi4zHTjQtegRYCIXPphgAXBL7k3jjMfgloQfgfhO-ZNEiP1-YwJ8WNzz3hKDNVr-hf4mcMj9qw1_jAIUUGzCDrGOa2gQ\",\n" +
				"            \"n\": \"jVc92j0ntTV0V1nwZ3mpGaV2bME4d6AMS2SRrJBM0fLehaTEqDNzGu0warz2SC9bhcBOB5_q3mYBFjmTwWzSbsk6RYETnAgViXg67PgH7Vkx2NCtwgQW3cNdnUZWRNYHsoevkx_Ta1X6Vi9ulebU_BCKjrF-6CjVcGgEsO_S5DKcukGHdf81WlQOq3zGQg4h7MLArrbPSTHHORDsu_87qY9m2EhiYSOBSF5rHsfDo7zWI5FWNG-_HO-CBM005bykIIS1aXCXx1jOW1OrKcp5xv3e-BR6MJTxncZJ4o1GtynJI8kLXRgltLArSOkbzNEr9GjU9lnSSxKLMtRLKkG2Ow\"\n" +
				"        }\n" +
				"    ]\n" +
				"}\n";

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);

		assertThat(env.getObject("id_token")).isNotNull();
		assertThat(env.getString("id_token", "value")).isEqualTo(decryptedIdToken);
		assertThat(env.getElementFromObject("id_token", "header")).isNotNull();
		assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_encryptedNoJWKS() {
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
		JsonObject response = new JsonObject();
		response.addProperty("id_token", idToken);

		env.putObject("token_endpoint_response", response);

		cond.execute(env);
		assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
	}

	@Test(expected = RuntimeException.class)
	public void testEvaluate_encryptedNoDecryptKey() {
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
		JsonObject response = new JsonObject();
		response.addProperty("id_token", idToken);

		String jwks =
			"{\n" +
				"    \"keys\": [\n" +
				"        {\n" +
				"            \"kty\": \"RSA\",\n" +
				"            \"d\": \"ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ\",\n" +
				"            \"e\": \"AQAB\",\n" +
				"            \"kid\": \"fapi-jwt-assertion-20180817-1\",\n" +
				"            \"alg\": \"PS256\",\n" +
				"            \"n\": \"9sLLHll-BiuwIvupyiAOaTR-ChAjuwQhThxtSD5wgL9AG2YJlamxo52ZEhTdNKomGRw3woihBRk8okPSd1YZCkFuOc7iF1sUsxDA0APbdeAzZdwGcqvPlEqoz8HsnormZFTP9tG451Z_cMd20_cCufGqi6XBpE2fOMGhn9CNEo-_3TPyrFZwmP-rqsQ3TKp5Zkb42AACsT9GuxB-Qrrpp1hkCJdSvXhAwIvX2jOxXQORJZkW2ST0DpCTbtAoPEW0aoCIkwWjV08qTSBduA14eIl9xPACfElosTDEbL1bR6BGxoILSJTe4U5Lz4Us7l8TWhO_OaQsyxSLFxXOy13KMQ\"\n" +
				"        }\n" +
				"    ]\n" +
				"}\n";

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_encryptedWrongDecryptKey() {
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
		JsonObject response = new JsonObject();
		response.addProperty("id_token", idToken);

		String jwks =
			"{\n" +
			"    \"keys\": [\n" +
			"        {\n" +
			"            \"kty\": \"RSA\",\n" +
			"            \"d\": \"Nha1ReQVpOa1vsTz0oEcbtCd2UAbYWXhLr06CXXENXxDHWhTkXCaayh4kEOp6CNfY5b75tbBZB2hhxyY7vPNSmnvrIHn1YE5a321vHEti0GoNI9UGi2KhAO40Qu0ZPqvYCo3nfRsS-CWUWzchcKlqwiwuIwXOWPEGTnuKf-iqsqm6zNXB6zkIXb0Qkm5ByyGiF_wgOJLdlEx9hHlmHPiT_RKYbsQQ4mNqwOVnkInttzPiWmfY1cYN4qlxMYtIF7vb1pz1eWjZLlQpM37qQSxNXVeKNIUrs3NRyP-VDv47YFWDRLaPEPnnS7Y5RwJgeydTPHfZ91C2Q_m79KMGHSqkQ\",\n" +
			"            \"e\": \"AQAB\",\n" +
			"            \"kid\": \"fapi-jwt-assertion-20180817-2\",\n" +
			"            \"alg\": \"PS256\",\n" +
			"            \"n\": \"kne7a8IYQR6jweqpHAplq-XRGOuiVyF5Siy6_647OhOC8ppRIMV2O_wP6qK1AKCFb78Bb8qbRI3Mz-Tr9hCWm1BZQkD-HGbNowjVsOj7oB2nbNbGfqciTyT3kTG1f5PmeX2N4-f9zZM-J4Jmi9PdMjn2fkNl9oMCW9XaLHHzCU6f-vYftxdCnVQD7ZKr40HjoAeXjwdGhgzvuWSZHkhEqx_QMh8JskqP46PjsMykFWiryju9balCdS5yASf-Fno8pXMFEV1wgipy-FPlhB5FZtLwVvH9F2jAxRaWkRQzhM5hWugIUi8YobjoIwhrmJ04JTK-DGOlThJsNvS4QANDZw\"\n" +
			"        },\n" +
			"        {\n" +
			"            \"p\": \"7qedTQzS0HpavGkfXrxaslZaxld9PiFxG_j_qkGa1zq5AYVvw3_YUy8HrO8-2QjXVDX7pOeM5qDR-g1Zh13kdypvGbYW3wlDdaKbsF-9FURK96M2oiNd_6UKaxP1UU1XOVEhN_LANDCfxBgK_eejbVz-3vCDezWVu9ujIBLq6wM\",\n" +
			"            \"kty\": \"RSA\",\n" +
			"            \"q\": \"jc3dAzAN-2Ti0X2QwGDCZfaqDHo-rf_8Kv7Sqn1yiwjVE6VrfRhDwkxHpLBlWciO65pRTF71vnLajE79AHsy8hO0e1egcN8axLoQ5FPOFNbcdANarxUGkdZqx8hENBUmbZjX36OX6ZKTYO74KWv8NBXwv2Va85Oxnk22NuwCWVE\",\n" +
			"            \"d\": \"H2BY7TOeBS55fhfBoPdTKPSkPJuN6L1w7Lqr6DxnSdxa_xu9aESxjdXSEqWkVZGDHJ81c6t4oaN9xJSTug7oNaQcJEgdooffjRfnAelVR6hpoVODrN9tk9HqRBsgV43v07AzREaPE97LMPFPzNkgloyMa2nKiUXjwv6TVh_HQryCZqj8AvUsOIZd-Qi3FCv8cYEqqIlzZ-86nQdEVEmqpkp4SmylgqF_1H0hU8_nguRFh4Ojz0GpFtb6zsSpSGGGBDWOgcu8ti-0QXJDLsyKhFo33fwwmZzJsNJZpXNQD4n-e8JS4CrMAIVSvSEUFonFuq5BwmhA-l3rwOa4Bnnf4Q\",\n" +
			"            \"e\": \"AQAB\",\n" +
			"            \"alg\": \"RSA-OAEP\",\n" +
			"            \"use\": \"enc\",\n" +
			"            \"kid\": \"fapi-2020-06-23-2\",\n" +
			"            \"qi\": \"wSzPUIFPoyDg01qNp3NcDALx4GS5MvTl224ICHR_zXGxR_bEBgDT_MuJv_LIozVJWD-oSXxj2M5YglcbUWFZECBiHbpdln4RAzPVgLzwiIrcCDeU1aBTQ_-2-TzyVeX3StqCcT9E74Hfo_xs3jiiR5ubKlgP4yfUEi9WjoEz0d4\",\n" +
			"            \"dp\": \"Czvupqrc3Z102fuk02PQbSatfTqvFZajaWquNkiCTnFgNhce7Lf-6eOD2_sjHTpSUI99-gRAWLbnS3sHZNzhnU5tDmvI5dTczRPLemD3WKHvWXrgXn-FtDwDooi8-ofGfFc6VhTiQVKsoqFzGwKlacd_4-S0e79I_h_XrPHXBxs\",\n" +
			"            \"dq\": \"MWb3CJVFSb_sBW_pbhxHnZ5Bv_cWvyffglskqaDqtuVs1ltbB0nc0WQh0Y5iwNTOdeZdTG4Iz0DUQu2B1xkUtqGGzzHIA4q2mLL6D5Hiyf_Q7dn9TeHVWBmLOe7bVAnKSYRmMNOdqRIXpj2a11N3me3K4eM9vH5H8w6_3IJ8jwE\",\n" +
			"            \"n\": \"hDI75hJSM_tN3vDFouXZyi4Rf6i_5uhgrO5iXcgAwutkf6AXw914AIQv3eUXX5m1vrN00KIlgkxBXwyP8yP27KJvKXYVNUxqIqanE2jF53CkNgAMpVo-weWJkg1nakWr_lrL60sWAtQf7HTcs2ePKRSrnt60hqGZWeoEljYrMY5mRtOTIzUDdhXRK6QG5w0TQaPfqvpYemhl9lJtbPHQ7Mq0wZu1PBQG9zxjsXgMOKKbUKxzt7sVhjiKmkay_AL173sAQ9l8mwpAXx5Qa1KSUI4SLF6z8mvD6zTbTUWs3UhpX9tH-yir4VbYgaLH3ZHoyMNJ4qHPDscubi7YQg5m8w\"\n" +
			"        }\n" +
			"    ]\n" +
			"}";

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);
	}

}
