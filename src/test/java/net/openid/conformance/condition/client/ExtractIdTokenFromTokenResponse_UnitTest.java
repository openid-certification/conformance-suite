package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ExtractIdTokenFromTokenResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodResponse = JsonParser.parseString("{"
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

		badResponse = JsonParser.parseString("{"
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
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("token_endpoint_response", badResponse);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_encrypted() {
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
		JsonObject response = new JsonObject();
		String decryptedIdToken = "eyJraWQiOiJhdXRobGV0ZS1mYXBpZGV2LWFwaS0yMDE4MDUyNCIsImFsZyI6IlBTMjU2In0.eyJzdWIiOiIxMDAxIiwiYXVkIjpbIjIzMzQzODIzNTQxNTM0OTgiXSwiYWNyIjoidXJuOm1hY2U6aW5jb21tb246aWFwOnNpbHZlciIsImNfaGFzaCI6Ilk5SEFvWDUxM2xCZi1ObDhPY1ZLZGciLCJzX2hhc2giOiJ6WWdCSTFSal9XYXVCOGlXMnJaZ1p3IiwiYXV0aF90aW1lIjoxNTkzMDAxNjIyLCJpc3MiOiJodHRwczovL2ZhcGlkZXYtYXMuYXV0aGxldGUubmV0LyIsImV4cCI6MTU5MzAwMTkyMiwiaWF0IjoxNTkzMDAxNjIyLCJub25jZSI6Ik1LaFFjRzM3ZzkifQ.DaIaQB-ypPnB3humfuqW7VeTAsdWUNJSQFrAsjO3lHlslPT9neFe31zBnfn_1sODEMJHOA2Cep2P6crPFVsL-9UuWepB9El7_2j29QP0TY9O1bfafZ2BsUopTFkTfM8uE8YmdYPuvwQRAnkf3qCvczhAHjGVdS4zVggTirOz8QIWhKOphhkXeEsz4xbbUxWL0mG7cVxagWgoQYJydixOyuUM18JlhS_8XtUROsMxeJib_IvHSGPYV0JUZC4-MeVnsagxTqeX79Qx2nU7_-UE2RaB9KYZpGA9lhUw24mLL-U3lvLDODsoD5-rH3K6S57hsCl4bWrKDCvhMpfJY9YVIg";
		response.addProperty("id_token", idToken);

		String jwks =
				"""
						{
						    "keys": [
						        {
						            "kty": "RSA",
						            "d": "ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ",
						            "e": "AQAB",
						            "kid": "fapi-jwt-assertion-20180817-1",
						            "alg": "PS256",
						            "n": "9sLLHll-BiuwIvupyiAOaTR-ChAjuwQhThxtSD5wgL9AG2YJlamxo52ZEhTdNKomGRw3woihBRk8okPSd1YZCkFuOc7iF1sUsxDA0APbdeAzZdwGcqvPlEqoz8HsnormZFTP9tG451Z_cMd20_cCufGqi6XBpE2fOMGhn9CNEo-_3TPyrFZwmP-rqsQ3TKp5Zkb42AACsT9GuxB-Qrrpp1hkCJdSvXhAwIvX2jOxXQORJZkW2ST0DpCTbtAoPEW0aoCIkwWjV08qTSBduA14eIl9xPACfElosTDEbL1bR6BGxoILSJTe4U5Lz4Us7l8TWhO_OaQsyxSLFxXOy13KMQ"
						        },
						        {
						            "p": "z0FVeicrECBglwnSTGSH-Xq1VtYVcjIVb6g4T2fPmgZnt6a3yu7MJpPKl6h8kaOz-tQcNt_u48G2Zyu7E76Y9RUn43MZncW1hAq4VxQ_rKAZmFyI38pzbELTwcg5E8G0VAL54wdBzAI7R4a_fTR2OsRyl0KBqcSaRAt4PxT1kg8",
						            "kty": "RSA",
						            "alg": "RSA-OAEP",
						            "q": "rpVBITHKspU9lS2JCnITxGfOPNRndBacvNB182IR3FaVv5UlO7VXY6r9_Ry0gsxzIsbNpRcyG1FX81baa9mF_v8KQ_XJ-GjtT-wZy2jPqBpe22jMtMdzGdSMkaw8jqHORUwvoLUOk6Bv5nAOCZRiopSrSXpOUASVZJUUpSxGlRU",
						            "d": "OjDe8EkZXgvB-Gy5A4EdU8fBuAjdHLMyHKAtMaS_W_joEJHDvZRhIYbh1jAyHYoR3kFMXutCIYpRjDrsUEhjYuVKLm90CVtysoRjjkiXyupcEW3o--X_HBJhKm1Y-0I7LQ-cA7CotJpTVMR2fRTqP1T4FsORAjg9l-fbdpVmeDiZBRbL2zCWmKWhtDpHyy7vbSCRghntihz_M5Hrchk7r8ito_K3dFrV9IZSF9RoEY7kyK5bL36Kpgai44PYCzqOzqP2fteO_rZ9fn-uK59pI3ySo_PgSbJ55n14Nd9Z8m70zE9Z4aIeNDEFspZUhavngRwc7MuJ7f_hVGQ9RFbbkQ",
						            "e": "AQAB",
						            "use": "enc",
						            "kid": "fapi20200623",
						            "qi": "IyiL1_cnC5Najrfvu6ypiR3JmpHXDs8FkYJUdfqXnVWaBNxkdDi3iks943JyIfp8JI-NWndiNB6DdSBzecARDqew3lQomIsGsoR0wPFcHDee-d-NmBwEm3TSHrleGjj0oBJe6BDnAdsaHhsL9NLo_1aOd_9W_TM2kcuSntM-DFA",
						            "dp": "jjER1tu2hLrh6d34JSc3zubsMOZyEkXgRRnRgFEFsnPAhtRf9l99Ot5cuU4EINuCaI1Lyi46tJG7de8fy30RbdwU4Myf_4mcbjn4nO2sfd_dj5W05mz8YYM7yxB2cGKOOLFOBf99mdzSFNGS4PC0SL9sqvAbC4FyIUsJNaZIkOs",
						            "dq": "xGck3jMl1cIPhcO0aAvuMQaW_df2iqLlsYTTLPsnHLpLvTwMpx9bMMUs95NTf9KBtJ3yu8dcl17rktYi4zHTjQtegRYCIXPphgAXBL7k3jjMfgloQfgfhO-ZNEiP1-YwJ8WNzz3hKDNVr-hf4mcMj9qw1_jAIUUGzCDrGOa2gQ",
						            "n": "jVc92j0ntTV0V1nwZ3mpGaV2bME4d6AMS2SRrJBM0fLehaTEqDNzGu0warz2SC9bhcBOB5_q3mYBFjmTwWzSbsk6RYETnAgViXg67PgH7Vkx2NCtwgQW3cNdnUZWRNYHsoevkx_Ta1X6Vi9ulebU_BCKjrF-6CjVcGgEsO_S5DKcukGHdf81WlQOq3zGQg4h7MLArrbPSTHHORDsu_87qY9m2EhiYSOBSF5rHsfDo7zWI5FWNG-_HO-CBM005bykIIS1aXCXx1jOW1OrKcp5xv3e-BR6MJTxncZJ4o1GtynJI8kLXRgltLArSOkbzNEr9GjU9lnSSxKLMtRLKkG2Ow"
						        }
						    ]
						}
						""";

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
				"""
						{
						    "keys": [
						        {
						            "kty": "RSA",
						            "d": "ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ",
						            "e": "AQAB",
						            "kid": "fapi-jwt-assertion-20180817-1",
						            "alg": "PS256",
						            "n": "9sLLHll-BiuwIvupyiAOaTR-ChAjuwQhThxtSD5wgL9AG2YJlamxo52ZEhTdNKomGRw3woihBRk8okPSd1YZCkFuOc7iF1sUsxDA0APbdeAzZdwGcqvPlEqoz8HsnormZFTP9tG451Z_cMd20_cCufGqi6XBpE2fOMGhn9CNEo-_3TPyrFZwmP-rqsQ3TKp5Zkb42AACsT9GuxB-Qrrpp1hkCJdSvXhAwIvX2jOxXQORJZkW2ST0DpCTbtAoPEW0aoCIkwWjV08qTSBduA14eIl9xPACfElosTDEbL1bR6BGxoILSJTe4U5Lz4Us7l8TWhO_OaQsyxSLFxXOy13KMQ"
						        },
						        {
						            "p": "z0FVeicrECBglwnSTGSH-Xq1VtYVcjIVb6g4T2fPmgZnt6a3yu7MJpPKl6h8kaOz-tQcNt_u48G2Zyu7E76Y9RUn43MZncW1hAq4VxQ_rKAZmFyI38pzbELTwcg5E8G0VAL54wdBzAI7R4a_fTR2OsRyl0KBqcSaRAt4PxT1kg8",
						            "kty": "RSA",
						            "q": "rpVBITHKspU9lS2JCnITxGfOPNRndBacvNB182IR3FaVv5UlO7VXY6r9_Ry0gsxzIsbNpRcyG1FX81baa9mF_v8KQ_XJ-GjtT-wZy2jPqBpe22jMtMdzGdSMkaw8jqHORUwvoLUOk6Bv5nAOCZRiopSrSXpOUASVZJUUpSxGlRU",
						            "d": "OjDe8EkZXgvB-Gy5A4EdU8fBuAjdHLMyHKAtMaS_W_joEJHDvZRhIYbh1jAyHYoR3kFMXutCIYpRjDrsUEhjYuVKLm90CVtysoRjjkiXyupcEW3o--X_HBJhKm1Y-0I7LQ-cA7CotJpTVMR2fRTqP1T4FsORAjg9l-fbdpVmeDiZBRbL2zCWmKWhtDpHyy7vbSCRghntihz_M5Hrchk7r8ito_K3dFrV9IZSF9RoEY7kyK5bL36Kpgai44PYCzqOzqP2fteO_rZ9fn-uK59pI3ySo_PgSbJ55n14Nd9Z8m70zE9Z4aIeNDEFspZUhavngRwc7MuJ7f_hVGQ9RFbbkQ",
						            "e": "AQAB",
						            "use": "enc",
						            "kid": "fapi20200623",
						            "qi": "IyiL1_cnC5Najrfvu6ypiR3JmpHXDs8FkYJUdfqXnVWaBNxkdDi3iks943JyIfp8JI-NWndiNB6DdSBzecARDqew3lQomIsGsoR0wPFcHDee-d-NmBwEm3TSHrleGjj0oBJe6BDnAdsaHhsL9NLo_1aOd_9W_TM2kcuSntM-DFA",
						            "dp": "jjER1tu2hLrh6d34JSc3zubsMOZyEkXgRRnRgFEFsnPAhtRf9l99Ot5cuU4EINuCaI1Lyi46tJG7de8fy30RbdwU4Myf_4mcbjn4nO2sfd_dj5W05mz8YYM7yxB2cGKOOLFOBf99mdzSFNGS4PC0SL9sqvAbC4FyIUsJNaZIkOs",
						            "dq": "xGck3jMl1cIPhcO0aAvuMQaW_df2iqLlsYTTLPsnHLpLvTwMpx9bMMUs95NTf9KBtJ3yu8dcl17rktYi4zHTjQtegRYCIXPphgAXBL7k3jjMfgloQfgfhO-ZNEiP1-YwJ8WNzz3hKDNVr-hf4mcMj9qw1_jAIUUGzCDrGOa2gQ",
						            "n": "jVc92j0ntTV0V1nwZ3mpGaV2bME4d6AMS2SRrJBM0fLehaTEqDNzGu0warz2SC9bhcBOB5_q3mYBFjmTwWzSbsk6RYETnAgViXg67PgH7Vkx2NCtwgQW3cNdnUZWRNYHsoevkx_Ta1X6Vi9ulebU_BCKjrF-6CjVcGgEsO_S5DKcukGHdf81WlQOq3zGQg4h7MLArrbPSTHHORDsu_87qY9m2EhiYSOBSF5rHsfDo7zWI5FWNG-_HO-CBM005bykIIS1aXCXx1jOW1OrKcp5xv3e-BR6MJTxncZJ4o1GtynJI8kLXRgltLArSOkbzNEr9GjU9lnSSxKLMtRLKkG2Ow"
						        }
						    ]
						}
						""";

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);

		assertThat(env.getObject("id_token")).isNotNull();
		assertThat(env.getString("id_token", "value")).isEqualTo(decryptedIdToken);
		assertThat(env.getElementFromObject("id_token", "header")).isNotNull();
		assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
	}

	@Test
	public void testEvaluate_encryptedWithAudInJWEHeader() {
		// has an aud/iss in JWE header as per https://datatracker.ietf.org/doc/html/rfc7519#section-5.3
		String idToken = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00iLCJjdHkiOiJKV1QiLCJraWQiOiIzNzNhZDVjNjAzOGViZWRjZDkyMjJjZjU4OGZiYjRkNmRhMzg3OWE1OGIyYWFkNjMxY2FhOGRmNWQwNWE1YWM0IiwiaXNzIjoiaHR0cHM6Ly9hdXRoLm1vY2tiYW5rLnBvYy5yYWlkaWFtLmlvIiwiYXVkIjoiM1prclFNR1R5WGE2QVh0THdjR1JMIn0.Xwtc2_Pyua2OmrakO2VHL2MFeSXhB1NKKAWDKqmyQVhWFkI4LEwhcmnJSFnPmn33z0LsN_7eq-XKLxD5PBUio6h0SUCxpfiL9ZxKsERLz0s-gXt0i2y0ZgI9bQL6voEkw3FIjNJOGKssHQgG6us8zsKIpJehLL1ziD5FOAj-BaA5I5ovBR_14lSnHPC-iayeRu78IlBbVnPWk4opLWqin6_APbojotQD1JfHwe7Y7yb7rbn7fobt5RDnx3kD7rhVrG6t8TU59CTmspTzct2CpbLEhBYO_xFRddWWxgwRFGu0Id2gnErb3oAiMBccWviSgf8z1R0DLTumOxeX0LZCkA.ikl8jye8HEwu6int.IciQKV7VSUUh2DzQKTpLcmQQR_2WnriGSZabHW_BWB58JdrSSkQxjW93ElN2Ay8V0ymCbo2buVDUlHze56-E17zxCJvHth6D5XvOj13JzQnJ8IeY9ds4eBogXtdqkDPAfWayCqjp5OLFokabxFeyWq4wKdp5fVYGd2EXF_emzqvsMzdT-OFXhakq_q1NUHhMm-2GTcqiAHLUHdvU1xC1e9QejZECmsup5jGByQZ3M70RBNrBBD71IX04yXzqTuUOaPRr7QMpQNFVK9enij7DApYIDY9egtv4B64lX9bWsYPpSXbopEpqLVR0PEqAjD5x9UTPXzCrp2X7eqfBHIOAIsDJKsmlxgnHVcRxSoZPJhY2Rp32Wl-OYyXixRirQFVosdRHtfpsiay8YMQj908_ynbiqdaUE97rVAqjA44gJs-Hf7ilc_7UbaiIvV8wCgV_eKtZv21qtFHhTzydyrIwmXvNo_286g5cgIDYy8aLfM3UPEYWuz05omm5eWFCuO9tGx9E99-QmXbIvfQBlg6rQ8g4J6NbbyO7QcbrDymVwOeugAlT3lN8KpWCFr6w4FnqBLvTGd5Qg9b66F3pltZ4mZNoUNghoDu6EsGAomd1xSda_w69rsLNoyjKqalaT6ctCoOJePkmRc_H00DRv44Qt59m8sOIU_CmeHWh_9zVkIWh0V7_D_r0NUSrnxl1xnlT9QCek-18C6B5AALpoLhw_lguseji_C4m5GFtsBkz2PJcsF4jmOvkb9X9fnT_7bo6wBLnu6Jnc9PntaekUr28l9tV3n5WoPpfgf8JW1dbAxif4_zF7kzU1tBa3Mj7aZ3r733Oz0u6q_LarOfSwN0xl2w6cMGabGy90-C9Tco4GULbxVZrDR4SNVuA_LxHG500X4N1MXP_N2pmvJ8neQwOrfhEmLeCge_Bmcr1WyvaRf1bKYr0Sy5YBCvtvVWJ_tyQ_90MsNr5hq6ctYTULdOXDI0-nlqU_-w_CIuRf8vVn0jbYyfBd0KXd8orKih3EEde5RmIIpvbd254cw.FFWEjn6Eo7lk8ZU6TN0E9Q";
		JsonObject response = new JsonObject();
		String decryptedIdToken = "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjE2MjI2MzA0MDk2MjEtYzliZWRiMWVhYSJ9.eyJzdWIiOiJyYWxwaC5icmFnZ0BnbWFpbC5jb20iLCJhY3IiOiJ1cm46YnJhc2lsOm9wZW5iYW5raW5nOmxvYTIiLCJub25jZSI6IloxdXhybXFtOVIiLCJjX2hhc2giOiJGOVFQNTc3a2Z0SG1jbC0tLU1raFhRIiwic19oYXNoIjoiUk53cE91cmE5ZTBZaXVsNkNEYzhSZyIsImF1ZCI6IjNaa3JRTUdUeVhhNkFYdEx3Y0dSTCIsImV4cCI6MTcyNzc3NzAwMCwiaWF0IjoxNzI3NzczNDAwLCJpc3MiOiJodHRwczovL2F1dGgubW9ja2JhbmsucG9jLnJhaWRpYW0uaW8ifQ.DZs20ZE5mlKPC_URxMslxa4wG6TeyIvjWbhJg4rOysw5YyEHNZfKm-mthAg7AAnpfitUFDMJ28X_f7J_BYo3J_uN5n71TFz-NJGOU_luzb5N_Uiy-TA7x6o2lOQnW31ICQsTPWxAjSoEBUGX0D5gho7Y1_p4Q_URIRGlMn_SA4j6nqbVt9NSm53jj4Ef2MnnkYZTRhl2EJeC5O7fH-EIp4mwpuBQZshmJ_tXh7-qJS_MoDirdSsit6Hrb_nNidaSEQutzdmG6LGxWzg36sN2Bs3D4DGZtCW_lRGvc7k3PCYWIwpufQHA_ftTUeGIaVMEwH29QleFWvHXwqFH_zU9UQ";
		response.addProperty("id_token", idToken);

		String jwks =
			"""
				{
				    "keys": [
				        {
				            "kid": "cch81rlolSC_10P_vwg3HBTk9tgsfwk6AOZ01Avw2gE",
				            "alg": "PS256",
				            "use": "sig",
				            "kty": "RSA",
				            "d": "BGzRkD7H_tAlgq8wEZqE2TD6x7aOYm0tPd7weQy8Cl1bJASykwKZCaXHp_flBPV3CBYqVeuEvxz3d2ITSkDdq5-Tiu_yqujytuh3jaKX7uhwIZkH4lVcfvB2e7fnNEEyAohaNMI0a9iHxZkl_vOxRhGHGH8Qw_sRPURxLkQYmAo8q_mguWCc4if9qFaxyKrCSyK6hh3qXXP8mNkngEBDJ8lV_1w_xLYpx0kgm6aiBkFGEDoFhoAVIhypv2LyHNZVfltlcV__901fJxuhsbHKP6eumOhenoSTOcanSlUbPoHlMyCcDQEaNSOam48rhtUqhJdfQKmBmj0drCbLuBABYQ",
				            "dp": "jpTjr6VnHuPjQmg7lZvsLXUXDpqxM2-rZoYG9nU7HztlqTj3SVr0wl3poi8zDIwLVdFq5wfgp62vmN_Zro1E6j8EOo5Yie9YPK0Tjks2o7Rw8nlzN_cI5xKIfCcHH5wwtZ5WHyXNGKoqauEUO5J8owZ4M1-UjzNtyKXhjTLX2uE",
				            "dq": "iPFs-xkHZNzvlBtf7TqslsLILtPcGy3gvPRpyYpkTr5NCJgxfXmBjx4joNdIAhD0QJLQffWUXh2ouf9ngJspZZ-sSSFlJzBLLsobo4ec-FsU5kwJ8piI32L_jBTTOqLwnYPdQZlWA8W3caeln2sd52EV-t-xgFsT7qgSJVb-hVk",
				            "e": "AQAB",
				            "n": "qZxVTSIymGN2x3wOyo_zYUTLBvZoTcozNk0ojFIcdko88W2CvRMyUYV5EAciifd24xuv3Is92uA4jCvR1cB020e3gvSjBrR1deIsrrKnXVuC1m0rcvLS511Qs9MFlpsAiIBF1kn9XMuqLbQHs2U4lYrxGzLesZhs2SY4pq6nqMr4jEWJs8k0k8yLz9K5GOO9-AYbSuYZHkJ_12cQepW6ORF43cMKQqPQt42FvPw8bnfwk6Wnu0HdvHcGzx4F2aW2PUgeesNcknrvJ2EUiPhriJWpDhZbwlS1ZnomrPlzsxMRekvQ1LqAQcWgVOzK5KQDDTcvtkmKhX4_rF7xyzAYTw",
				            "p": "6HnW-YEVnJmObkTPYoFj5-HqlexrrJ-MLOxFp2i8Fz_-mk-aNqWrfhCfeqo5dYU-fCK6ZYarYrovcIRJYrfrT4DQqCgeXnNoD_tQXaQnJkCfn96crCeEvGpb0bUVcsU_mwIBKylxvv5ohWngzxwyWY3z3101jHrZwUOBezW6k6E",
				            "q": "usYA9jU_C7juw_wZtBBrJ9NT_gS0Mb-p9t0xk_q0HskwWNlVP_fPzK7t3As2tejONMJa6eQYkbAJrEtGmdTg9IT8cgpDLEpXwC7EgrEXP9MQOXUZ4norN32-_7QhwKf8FNKaRc4L9gEKPzOLrwZmbispFgBHbqVZBaslmUpqJe8",
				            "qi": "1OXGalYr90TfS6kI3oP3xq01h1-rLz_D3Gh2wLj2M5KdRzGrcSJEnjezsibsn1oY3lE-MpP-gAjN4Xa8WZfpJOnxIms_ThsmrijPtFLU1TBXHVfsjI66h_nnbRYWUE6h16J1_wtzJ4oos-AFSxFBj7eNKPWaeY1PJk1cGv-q4Mw"
				        },
				        {
				            "kty": "RSA",
				            "use": "enc",
				            "kid": "373ad5c6038ebedcd9222cf588fbb4d6da3879a58b2aad631caa8df5d05a5ac4",
				            "n": "ALExYKPuup7mvCjQMMDzUA2yB33N5s1XxUFyMkybzSb30Q3Z3TkjnKePRl5hgRFG2gvaKsKeCkbEKTERp366Siwe4OZNpMNYCvVZfrJKJrHRcrO4ORRvU9caRWSmd_VcVUB5maYOcgOpofSyc4UvwAUi5jNPkm-rU5XE5ijOmQF5C6Ot_1zRrZ_mr6DVd7W-e8E0EF6rTs3DG-vkP4muz5V1UjbGgRsjFZ3U0ig-HXdzQjj9Qq75sD-y9Dr0IKSdUHzYCR_xeVsnW6LoJ7p6TwsWmX-lxfIj-9tJi-jGsW6aiQXuij_wXKzTdclRICZpZxfxPF1ArWtOuhpWQMAeB8E",
				            "e": "AQAB",
				            "d": "AzEVAQ7xTKByXjfV0PvxArYkvWbik-tTUcj5T_j0epG3Mc0tV3SlQz1o8pVGCmYg0lrVL1lCbSDmbwxW3IkFggLyYSOf5qbk16OLjTGZvdU8gDE-0ffpZjQCcPwwp_pITNeD5YJYGmnJUYuPWiRQjKcix2TVCe7nnAiGbtC939FjJOQ22egXCwJhPEd6mz2JgLe83De_YXsm7-CF6JP2nlmVSmTysalPI33isCPdi0UMa369PwIq_l8E1u_jrjuprmXHu2FxJyKPe5AXAQg70mIA66XlMFRLujLwLTNGfu6ycOcaA-D24zQlMIlWAGHblYB3qEDzMxtaIK9fkukSWQ",
				            "p": "AO11Mizc-mIobXmVR-HZszdw-XNhlB7KPAQBn4NG038-XoKFIhed5WZTTkn2MDc_E2Qi0ngY-dGGscgdluFZAjZNw0RtqkbmY9c68hcc50jiU9_vmK7_8_DzAgwJEg07ciEQPIoBL9L129lh9Iu9MfNdeGqIyLTpm-CX751YDU0N",
				            "q": "AL8Heub3V5Oom78sQIR2odJMtQMjV65kWOurfzmVLotdqNWNzx0hufzAmVwPPLNWf2EPFbQyJ8VzSEF8FMqZfdhayUYgFB60lPnOvF3KTcUTp6kMdW7w_5APD5PLcJ03ILwv6YjC0pZ5DCCJ8eCyi33XvI2UlLblHdFegsYKyQCF",
				            "dp": "ANHo_hfmqHhJIK2QJ2Ehn2ntNramlde6Et-_x5C29ZPQQJ_ErOSPshCIp_Z96RWNbsjgd4mvGnf8SACNF6nRk0EJ8Qn2n8XZOH01IHkKFfK6EEcveJuKq8xeJXsaMZj86eEMh1e_wNUOffBmxVUaRKPMBvy2OiajNCaQaUOAaRGp",
				            "dq": "AKn8sAMDKq08posxh5_YKRuIIrNOKeIHbZEZJLdK5wYhK_IMpLbZojUl9dITxQIZAHSQBdI5p2ZutWeAUMgQWgnCj8SVGxJPwxmgO8KCGT47xUAK1B0K23-Hx2bO68Eao0iSog-OvfIkeQQjfFdkyfgLl7-s7cDqlOQQY_Jy-nY5",
				            "qi": "AJkrmYZvsut7QxHLyzfzsxDn-v-EQzo6E4q_Fb0lKAF0CE7R3ren4SyX2013TOf3tUlMgTlQN3yoJjtZ1zbVrDX7RouKFX6HUzEDNSAwYrzhtNXZB_iQsOm_UUI1A4zA8kRSStkTHs1vqpPRhtgmNyl4vWrCphlTZqzO3Tp1g6Nw"
				        }
				    ]
				}
			""";

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);

		assertThat(env.getObject("id_token")).isNotNull();
		assertThat(env.getString("id_token", "value")).isEqualTo(decryptedIdToken);
		assertThat(env.getElementFromObject("id_token", "header")).isNotNull();
		assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
	}


	@Test
	public void testEvaluate_encryptedButNotSigned() {
		assertThrows(ConditionError.class, () -> {
			String idToken = "eyJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwia2lkIjoiY2VydGlmaWNhdGlvbl9jbGllbnRfMTYiLCJjdHkiOiJKU09OIiwiZXhwIjoxNjIzODQ0MDc2LCJuYmYiOjE2MjM3NTc2NzYsImFsZyI6IlJTQTFfNSJ9.RmdnvchWM7VV2BuSE_3DZbD0Xzo4T-j4fK9p50P0rC0_zRXmDQDJ79xErYH-XjoZ-qyUrAqv4JCW6vpXPTOZKMroJXFXJ4OqZI8Sxw-If_pYfB_c5gS9WVzk6rnGh1No7Sdd-OwFpZWMVH-y0a9_zTuhhujtIswZxQytnONsSkefLFx4C1rxPEK3JzqOB46lQnle6wfSc8eDO06loSQQenwUeS9TohxO4RqV2NbXy-EtrWorjudmQu1ODAPikRhCECPtk6UpN613uvXmVfeqc1tWDb0xXWArsWzF9mjNqpnmwROSMRidNLNKI-WYTNqd2QuBRc07z0BPN9d4bHIEqg.FEmCtUg_Fk8F-W_Rc65VdQ.A0lzf6eQLnIFzWH1nZbduwPTdhZl23G4brvzYSmTtH05bd0gIOrlEcIR9pBF5dg6-lYHaOGKgPTg5xZ6LVMIRG2MPz6dZS3S9BQurT6cMAde7AhLxJ6FKgVEyXxpFU_CrwMZFulmrC0q-6FY-aeNPMCb_RalF7JsbbIGQMwYfNG8gEVPh8Mx4ZseVlKfN6EAR2HdQlsluy5dKhYZowZ3vO93VW_Ll6M7pTULEb0AJoRVScVMINTtR75-Kl7ui5cv5ggwpRabdggCrm_UHylsRdbE5iwUQZBWIWFwUHS7k-KNK-PcV9HxIzHZJIgmXZlVaR44vTk1eutDoRL571_6lu2bcAIl0SiUXcD_DbiBqKq2FgxRr-HbPecbadxNuY3vgAI1iv-1DzTG9vcP8O2h2Q.111GdTQtllmt64vjWBCMulKRW3K-y6pf-knRP_UOlo8";
			JsonObject response = new JsonObject();
			String decryptedIdToken = "eyJraWQiOiJhdXRobGV0ZS1mYXBpZGV2LWFwaS0yMDE4MDUyNCIsImFsZyI6IlBTMjU2In0.eyJzdWIiOiIxMDAxIiwiYXVkIjpbIjIzMzQzODIzNTQxNTM0OTgiXSwiYWNyIjoidXJuOm1hY2U6aW5jb21tb246aWFwOnNpbHZlciIsImNfaGFzaCI6Ilk5SEFvWDUxM2xCZi1ObDhPY1ZLZGciLCJzX2hhc2giOiJ6WWdCSTFSal9XYXVCOGlXMnJaZ1p3IiwiYXV0aF90aW1lIjoxNTkzMDAxNjIyLCJpc3MiOiJodHRwczovL2ZhcGlkZXYtYXMuYXV0aGxldGUubmV0LyIsImV4cCI6MTU5MzAwMTkyMiwiaWF0IjoxNTkzMDAxNjIyLCJub25jZSI6Ik1LaFFjRzM3ZzkifQ.DaIaQB-ypPnB3humfuqW7VeTAsdWUNJSQFrAsjO3lHlslPT9neFe31zBnfn_1sODEMJHOA2Cep2P6crPFVsL-9UuWepB9El7_2j29QP0TY9O1bfafZ2BsUopTFkTfM8uE8YmdYPuvwQRAnkf3qCvczhAHjGVdS4zVggTirOz8QIWhKOphhkXeEsz4xbbUxWL0mG7cVxagWgoQYJydixOyuUM18JlhS_8XtUROsMxeJib_IvHSGPYV0JUZC4-MeVnsagxTqeX79Qx2nU7_-UE2RaB9KYZpGA9lhUw24mLL-U3lvLDODsoD5-rH3K6S57hsCl4bWrKDCvhMpfJY9YVIg";
			response.addProperty("id_token", idToken);

			String jwks =
				"""
						{
						            "keys": [
						                {
						                    "p": "zjaMZNPo374rnLWABlXdtf6p7ogsrqAgXVodzGTWza6-K_ZR6mmu3zoBlTPk_8GYx5Y2WNzZAf3WZjgUyB3Zm9X3kfwIN0uG7DM2tCOOpBpqib4WC5pqgX_U5guK50L2ub9dikkgGamhPje6FicJGpqCSeSXqAaQ7Jtxkbswpp8",
						                    "kty": "RSA",
						                    "q": "qieioR_qmaOZlMYDifNyubqImRW3jAYCIy9AB7wV3xm5zS_ys_fn5beHqS0CAAvJooW3CR50OZUdV6L-urtRmegU55WHPZqGMvEpsytsVgIuPLkOrKcv6ZoHb3BuzjKM1JpO4-8cyaxtI1mPexgeKowL7R4-fXHjVjTnmGqXMRc",
						                    "d": "c5a4OjQBM47fU7W5T0imCvG53R-OUC343ek6vBYpiCvpXaX236KcyQp1uW563_mWvj_ER2eJqdqin0TT23lweobircZaZttat_MA00UD1OJzjQx9sKXvnAJ7g2oXaPJYrSEzhZyws64pT2QkDJUe6FyAYLtLkwzZpB_447N4-pNemTwPG-lw18_Z6DvY-LPS8ra6QfcPEMC3l1ZluLSx4s8EzYHiPCyrJN6oVpYInIaTSNlFyIrdkGjtufZDNm2wcBWZyKUDAFL5n-hvOt5_0x3oRIhAXAlgHUMpZkR9IUg2LnwVhnNufwXDtVB5-3EtLubjdH7Rswtqn2PMc4S7ZQ",
						                    "e": "AQAB",
						                    "use": "enc",
						                    "kid": "certification_client_16",
						                    "qi": "Y-cSENMCMH7IbyoyeETU9tJRhlQq1MgjjLFJgfY2tFj9581pAZ4rCGQZMPyu6_ciMAZZ9c5NnQ5cLkbOB_tFZ1h7KzoPG5WvoqB5QnkDuX0nJudxy2EHts5Cmdo5EPhM0OHAPuiE7v8lM9nvrKpvLi7zcSJQUOjybE0onWx_uMk",
						                    "dp": "Zdl5VB96CHBT-A2JExf77nS-l6q4bvN1qMXmq9ouYd5p3SrG4MGb9nMe7wN3mn6TKBjf9Nn3cM1fxJL0aLadkSOIKJlTsYPjOuw8m5JoVFMbxbaWCtsUuataitiunXRsieejmrZtXV9z3VgG2AQlsfcYXMjDwuCjWcmYuUK8T78",
						                    "alg": "RSA1_5",
						                    "dq": "VtIiiVRfwEVZ-C-RXrJ5t4sWaAZMwYXPUWSqp0exYUUfKTNuA6ZbDTu4XPo6KwvYtrTD0ENZyofrkOv6Tg2GdaGWCRSjDSvrjdLAFEvz228BaLqEYlRsfiW55YLZBx2D_nc-Cc_YQI9aoM7yBz0nJ06OR09HU3rpJPOZuIuq2cE",
						                    "n": "iRAmiqbwmy_uJsFZYIO39oNH4uGZKVmntNMpYf3nlQwLn78GuBGoobSA84u48IunpCuk5WhPfV0PwKuUaW2HtaoaP1Ker1f_1HyZ-X7_ZurUY3AI9D9F2BLSYUlScEaidN8Obp6r6OQPdYzbnE7_ygbGdra_gKS7VjPMavFaO-9M0lhlmwHpkBeofqOp4CCs5QlnLs5sG4BSmQWyqeaWNHcjQE1tZjOtFR4MfCO7xrqxXQpUkkuK5X11KGC3X149JdEyAJvo_6DiJOjWJ9zgpy6FfcPK06xc7i24JULiogAKT-uGtgHkMfYHr6CKsTNX_lZPQsRje02ktA8EPwxnSQ"
						                }
						            ]
						        }""";

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);

			assertThat(env.getObject("id_token")).isNotNull();
			assertThat(env.getString("id_token", "value")).isEqualTo(decryptedIdToken);
			assertThat(env.getElementFromObject("id_token", "header")).isNotNull();
			assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
		});
	}

	@Test
	public void testEvaluate_invalidQuoted() {
		assertThrows(ConditionError.class, () -> {
			String idToken = "\"eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ."
				+ "eyJzdWIiOiI5MDM0Mi5BU0RGSldGQSIsImF1ZCI6ImNsaWVudCIsImF1dGhfdGltZSI6MTUwNjQ1NDIyNywia2lkIjoicnNhMSIsImlzcyI6Imh0dHBzOlwvXC9taXRyZWlkLm9yZ1wvIiwiZXhwIjoxNTA2NDU2NzU3LCJpYXQiOjE1MDY0NTYxNTcsImp0aSI6IjM2YzU4M2M5LTE1M2UtNDBhOC05M2MzLWEzNWZkYTgwM2QzOCJ9."
				+ "SuGDMrdIh_tCsoWt51xs7rs036KAL3OcwdTcJxmuEUT24EufZILQ9_2rNX8BLA9S-YwYkS243oFz9UfBmnnqj6H27-BO7yBSwGnofDwV6GN4yLXmJfrzC6EEvSPkYMnHo7ha2eIUDFEHcTuKg1eSyKvkaPhklg3R5QHl4xo43FnKfQ8TrhAEH07FNKGFVS67xr00a17OD8VNn3LlZISr-iVbaueNBeYD9obUEmL5IJR8Y37qNK4egirn41BXQKK7xguF2nebQpN-1lcewW5OnEWy7yGd7M88l-WVzfNyFCM75bKZFAbv_W2w1glh38M2DJNRbe2SJhMxkpMxwUeLBA\"";
			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			env.putObject("token_endpoint_response", response);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedNoJWKS() {
		assertThrows(ConditionError.class, () -> {
			String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			env.putObject("token_endpoint_response", response);

			cond.execute(env);
			assertThat(env.getElementFromObject("id_token", "claims")).isNotNull();
		});
	}

	@Test
	public void testEvaluate_encryptedNoDecryptKey() {
		assertThrows(RuntimeException.class, () -> {
			String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			String jwks =
				"""
						{
						    "keys": [
						        {
						            "kty": "RSA",
						            "d": "ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ",
						            "e": "AQAB",
						            "kid": "fapi-jwt-assertion-20180817-1",
						            "alg": "PS256",
						            "n": "9sLLHll-BiuwIvupyiAOaTR-ChAjuwQhThxtSD5wgL9AG2YJlamxo52ZEhTdNKomGRw3woihBRk8okPSd1YZCkFuOc7iF1sUsxDA0APbdeAzZdwGcqvPlEqoz8HsnormZFTP9tG451Z_cMd20_cCufGqi6XBpE2fOMGhn9CNEo-_3TPyrFZwmP-rqsQ3TKp5Zkb42AACsT9GuxB-Qrrpp1hkCJdSvXhAwIvX2jOxXQORJZkW2ST0DpCTbtAoPEW0aoCIkwWjV08qTSBduA14eIl9xPACfElosTDEbL1bR6BGxoILSJTe4U5Lz4Us7l8TWhO_OaQsyxSLFxXOy13KMQ"
						        }
						    ]
						}
						""";

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedWrongDecryptKey() {
		assertThrows(ConditionError.class, () -> {
			String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";
			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			String jwks =
				"""
						{
						    "keys": [
						        {
						            "kty": "RSA",
						            "d": "Nha1ReQVpOa1vsTz0oEcbtCd2UAbYWXhLr06CXXENXxDHWhTkXCaayh4kEOp6CNfY5b75tbBZB2hhxyY7vPNSmnvrIHn1YE5a321vHEti0GoNI9UGi2KhAO40Qu0ZPqvYCo3nfRsS-CWUWzchcKlqwiwuIwXOWPEGTnuKf-iqsqm6zNXB6zkIXb0Qkm5ByyGiF_wgOJLdlEx9hHlmHPiT_RKYbsQQ4mNqwOVnkInttzPiWmfY1cYN4qlxMYtIF7vb1pz1eWjZLlQpM37qQSxNXVeKNIUrs3NRyP-VDv47YFWDRLaPEPnnS7Y5RwJgeydTPHfZ91C2Q_m79KMGHSqkQ",
						            "e": "AQAB",
						            "kid": "fapi-jwt-assertion-20180817-2",
						            "alg": "PS256",
						            "n": "kne7a8IYQR6jweqpHAplq-XRGOuiVyF5Siy6_647OhOC8ppRIMV2O_wP6qK1AKCFb78Bb8qbRI3Mz-Tr9hCWm1BZQkD-HGbNowjVsOj7oB2nbNbGfqciTyT3kTG1f5PmeX2N4-f9zZM-J4Jmi9PdMjn2fkNl9oMCW9XaLHHzCU6f-vYftxdCnVQD7ZKr40HjoAeXjwdGhgzvuWSZHkhEqx_QMh8JskqP46PjsMykFWiryju9balCdS5yASf-Fno8pXMFEV1wgipy-FPlhB5FZtLwVvH9F2jAxRaWkRQzhM5hWugIUi8YobjoIwhrmJ04JTK-DGOlThJsNvS4QANDZw"
						        },
						        {
						            "p": "7qedTQzS0HpavGkfXrxaslZaxld9PiFxG_j_qkGa1zq5AYVvw3_YUy8HrO8-2QjXVDX7pOeM5qDR-g1Zh13kdypvGbYW3wlDdaKbsF-9FURK96M2oiNd_6UKaxP1UU1XOVEhN_LANDCfxBgK_eejbVz-3vCDezWVu9ujIBLq6wM",
						            "kty": "RSA",
						            "q": "jc3dAzAN-2Ti0X2QwGDCZfaqDHo-rf_8Kv7Sqn1yiwjVE6VrfRhDwkxHpLBlWciO65pRTF71vnLajE79AHsy8hO0e1egcN8axLoQ5FPOFNbcdANarxUGkdZqx8hENBUmbZjX36OX6ZKTYO74KWv8NBXwv2Va85Oxnk22NuwCWVE",
						            "d": "H2BY7TOeBS55fhfBoPdTKPSkPJuN6L1w7Lqr6DxnSdxa_xu9aESxjdXSEqWkVZGDHJ81c6t4oaN9xJSTug7oNaQcJEgdooffjRfnAelVR6hpoVODrN9tk9HqRBsgV43v07AzREaPE97LMPFPzNkgloyMa2nKiUXjwv6TVh_HQryCZqj8AvUsOIZd-Qi3FCv8cYEqqIlzZ-86nQdEVEmqpkp4SmylgqF_1H0hU8_nguRFh4Ojz0GpFtb6zsSpSGGGBDWOgcu8ti-0QXJDLsyKhFo33fwwmZzJsNJZpXNQD4n-e8JS4CrMAIVSvSEUFonFuq5BwmhA-l3rwOa4Bnnf4Q",
						            "e": "AQAB",
						            "alg": "RSA-OAEP",
						            "use": "enc",
						            "kid": "fapi-2020-06-23-2",
						            "qi": "wSzPUIFPoyDg01qNp3NcDALx4GS5MvTl224ICHR_zXGxR_bEBgDT_MuJv_LIozVJWD-oSXxj2M5YglcbUWFZECBiHbpdln4RAzPVgLzwiIrcCDeU1aBTQ_-2-TzyVeX3StqCcT9E74Hfo_xs3jiiR5ubKlgP4yfUEi9WjoEz0d4",
						            "dp": "Czvupqrc3Z102fuk02PQbSatfTqvFZajaWquNkiCTnFgNhce7Lf-6eOD2_sjHTpSUI99-gRAWLbnS3sHZNzhnU5tDmvI5dTczRPLemD3WKHvWXrgXn-FtDwDooi8-ofGfFc6VhTiQVKsoqFzGwKlacd_4-S0e79I_h_XrPHXBxs",
						            "dq": "MWb3CJVFSb_sBW_pbhxHnZ5Bv_cWvyffglskqaDqtuVs1ltbB0nc0WQh0Y5iwNTOdeZdTG4Iz0DUQu2B1xkUtqGGzzHIA4q2mLL6D5Hiyf_Q7dn9TeHVWBmLOe7bVAnKSYRmMNOdqRIXpj2a11N3me3K4eM9vH5H8w6_3IJ8jwE",
						            "n": "hDI75hJSM_tN3vDFouXZyi4Rf6i_5uhgrO5iXcgAwutkf6AXw914AIQv3eUXX5m1vrN00KIlgkxBXwyP8yP27KJvKXYVNUxqIqanE2jF53CkNgAMpVo-weWJkg1nakWr_lrL60sWAtQf7HTcs2ePKRSrnt60hqGZWeoEljYrMY5mRtOTIzUDdhXRK6QG5w0TQaPfqvpYemhl9lJtbPHQ7Mq0wZu1PBQG9zxjsXgMOKKbUKxzt7sVhjiKmkay_AL173sAQ9l8mwpAXx5Qa1KSUI4SLF6z8mvD6zTbTUWs3UhpX9tH-yir4VbYgaLH3ZHoyMNJ4qHPDscubi7YQg5m8w"
						        }
						    ]
						}""";

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedTwoAcrClaims() {
		assertThrows(ConditionError.class, () -> {
			// this id token has invalid json in the payload - it contains the 'acr' claim twice
			String idToken = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00iLCJjdHkiOiJKV1QiLCJraWQiOiJjMmZlY2NhYjVkZTE3Y2VjMjM1MzQ5ODE3ZTBjZjg5OTNiMzJiNzBkMTc2NGNmNWJhMWI3ZWZkZDU5YTgyNjgwIn0.54SKsSh5lZXePh8Nxj1HDecMV_rsbHONwJ8lkuT5lf7xrNf2YcV_IAETvDBMMn7V8NEzGO3VXyuNLuGf25tGs7QlmCumt7pGOKesMc9mA2zrN8BzEk6R683ijfgiJ-cZatgUlSdjf8LjuyJ1S1MSpkESzEOR8JyNu7X0GDkdxy704OM4xvc70MBpw-El6sOwrJuJjA83cBfbKF3KtqtphOS2OqMlO2SrvDGrpsO0eeU0Fy4380jscr-dC4q7yX5BXJAlc4Q0cNIEK2yHe9tuWJzcM3ZTuFB0OGrZpiJPK5mXNyHejYmpJqWWsR3OJLrdNP7_uuFDwLc6dOpwVuc8wRjZ9kxIUUC0kjR_XftXkZhb86oIx1YiOB2qKpDu5PyuYO7OhNvroieZ3mG27orUyd-zjBrULVo1m_wDLJi44UPJBg3vKdpINpyfpGs6a9G8i-PIrYujDxRT4s2RQAtxtl-c3z9bMbwZLHLDtzGH2UJy03L8FEx10aYAPQL4qDlzkJCFvKKWT_kLogWH9dhjTgnPCa-bfciz4OFOAWGbfRqNdd4mCg7oEQ6pilvEYGWcqMpiUW9SjAzpO_KGSyN5b7Wtpix9dn7MEgUT9iYSSr9ieN2g5mpdcrzmA8LY5x2AeIHXxneXsi0J7HEKIXneY2kU5UhbKwk0Ro7_FV7CB5s.YpiJz9AyM1cBrYhd.r1c6YRN3jRc4JhG6Z1f7kKjEitcQcofzqqX44o_0owDCfUZolHypHHFskRbKCYym91XDpdafyU3Mdj-Vjg3ZIBpCLg5Ty8nPJLoTcO_Zcf6JpkSNMknb6UXxGm5VWv8UPWx9DnZAKN5aYGKRpCVf1UazGDs3OL0xr6kNYmBnqWqqblDyugnibRc0Fa4yudFV2ptTBsaCBCfAodg1nM3Tivy-guiJJ48S9l6zfvgqtPxsLWaEEm2YvmUZZYsb2jXnNXh8gG0YQpIthNEj_XUdVRrOTfFefqQeui-fvBTbVsP62cNw_wz3PfMUjNMJgirB6a-IW6OmAawTnoF7jBPCGUr3CMX_S523rUnV_yD6R-fUH52vBwE6Sc8n9F-WybqFjLFAQdslUeWfHX3QoBZ1jvD39dnrDR-FUsDGC5Ck_Yh7764tug24raiSWRtJdhku9UFr1GePLrP1LtIzz6oKvH3UFnwjYthOJZjKDT0oBEX5J_tD4T4Iq_1F6slAUPIOlX2pb7Gp8V7okBRPDevhvCPcKOWEic6utj4-DcofyhUpfwCuakJLVplmy2d-jT8y7IGu4wQk59wB2FyKNUnRK5cNI6K7SRvJ_C-XiSngCZOFWZdcv7ZAb5vcM5G2G-so_pdgvJUa_Ldxa8nsrV63OemZ3h76B_U4erKlQzwPHVJe0rdYXmp5s5DoCr0sydaeDJdFlawHHETtLBh608MxoDXoBjffT1cUAcUoeDSHWSipxM5-Ztjg8hjElMihZoiqLpCzXOuCf2zm8V6UHAt_Ogboiz0DmfD-w-A8gnr5EfzcGIf2COpiRSJiUJ69syi4Og3eCNZ3W1HWTH3Fka7wikui89YidfvI7BlTHl1EvAJqlm1aZJWX2y8IOgqtvsvLGgEA_9Z1Flqtcd7HWZji8iDH6NxrIdM634fwsmnfVv2n06829MAu2jOs8IZ6huVYMrD18Ddct3NgA1L9W9ST4wB0yVvzRWIiZITvyw8sFEZzyYGU-oRaY2Yyym4CZsfm7YTgIyGdJQPuQBjygO85VdLxTkTKVgVSwaBvIvKclUUP-hds59OdY2MgTxdkKUWacQptPa-kwGeHeSSjQZMfu4mFx_vjdWicIl9nTEy29icYWp903Vcvd3vmhrPivBzWq5o5dygGbcKRK8oJqaCW0WHp9PCj1isvvxPHeqGlN3Cp1eQdOKLavn8_UjVoRBH2RKnPNpDiBNTOSm4O-T0A73mcwv5rIpxXOLUfL5Zu85I7PjoW2kFiGZgSGpm9rCumSGZhUg1ZwjFml-cwbsX8OPAm2kf-S-33j8jNBP76DE1nYNZ0ZKhwSUQVYplgAMMGErfyZNpjKuXw4kSv24H7_Oc4XRaaFIXanvWnMXA9epEQlAF5J7ET7x3Zverk21PAH62DKMMYhn44RNEw-Sn0gN7RI5XngEpUSIKdxuu-yIdC8BXMlz7RU6vCbRdBiC-8x0L25k3nJeb1rumSqV91hlKcqPLk1CWSThstWd6hLVYxSt8iOV85Jey0C-S_fYcMc93ONjXuDhHpxEs75YGPpa1I7o6XL0NaU8SPcRPh_8vLMEG4jl2VWPqJmzAWxDSiVgGcGJcMIwUUHbE-vCqG4Oi8dI0Wd-WFV0e5G8lgMvkESQUpEfT5g3zlCCrLu1VdD1S1bhccxQIZ17Fa20uOufzTEOYY5L29yGXQXFl7vnWZ2SYY8abVYN0guGPZR-DYsHZOLMasE9BBTJePMv4wSGK2b3fI4_HXWQ.NIWlvvYiq6dDMAYkSTDAAg";
			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			String jwks =
				"""
				{
				    "keys": [
				        {
				            "kid": "c2feccab5de17cec235349817e0cf8993b32b70d1764cf5ba1b7efdd59a82680",
				            "p": "_BGYafDEa1q5GADeXIfEq2A6fGsgi4URZZGPGhMq7Ny70LQjpYiLglPU1n7hnk0da-wykcpPAbBO212e44xFzE2YCBpjwwW1ZiTkmSQLXcA07EfXtGESz9zwJqSYOVCXTpMn9I7JrhYZNIX_8Q89jn8ck_QqgdX6vKuUL7_bL7NLMbssnKPEKOW87B9fAmjcQCOilzajK5lLNGHgqoWpPZyYz3cVYMrprEL-mm5VlOnatrBtRZlTPcJW45hPtylRL7-r9xG1H7udAhrWyY8XQQ48u4oEM8AMWmORsv8KcFRV_VSOeJXDxCfTuuKX9xHoos16W1vskMvhRui5CHvqBQ",
				            "kty": "RSA",
				            "q": "98FmaP_tkA3FbTNR4B9Y6iT9xpj0oKASf90-m2TwTjgS9AndjGB4pcTzx9EOlrw9bovf6d9TFUpXRajfZd5ls3vfAAnYmM_P92mQM8kdGtd_Y-l6t7iwKDtIntjrLbtBw66RaajzvZX0QVZqOv2wJawU9zm3WT8tY64fmVEJ-LzgHPTPKDeEbt1TdG_2qWqecUcz516QKZyaE-Zq0Lkk6JGp_BQBXReDwmmCae9ViQmU_dMFxTsF5YqjWObZYJecQ8CD3965BRD05jd020dO5pefq_xbv8TeEmt6ZKgPjuxwk_Tz-N4V_nevt-KVJXROyAp1ZdvgKqCUineTiIEBTw",
				            "d": "dqsJF-zgX601A1aaYOAm83IJjckikHxoeImiqHgx-XpACoC24pRxqy7E28njiON8t7xpBR-FJJGeNyL2F_7RpQFq4Tx8ZLJvacV3W7VgFFkM1C-f013jEwyQc_Q1Re5mYQ0DWeqt3pdqm-nUFvtGY1ZPRVOq_GHng5vHG96tHKppcA3UrJ8vL12eHfx9sdneiGVJExYjRmu19jGMntL_ah1enesdqS04lodDmUVOW2rPiyLVODGoJzkwHUJHMc8ym-8pHDdIKIOowzfQ9e-H8QTUsuBjFi3Bd8YNyoOk-nghs8MMd29-U10iXYap4MXbZT5RruXAFy1QC6P6QWKdIUs8AS8HNruu_GOtbhRdYuXXEWIjv6ZEsuItlBClFlMKEVFXJz9LZSjStFFD-6WUBw8-QMG_FS1dZJRyjlrqhOPv8vEezys-wfT8xoan-9Gu-NbLXIcqfqj0b7qrXsNh1NtBog3R6Mtpd5SucyeVZ7y5pk6NLE7DgrZyKKoRgPGQoB1mZFmV_T2wckGcM6LXas2Ras-oJ7TVykYgHkIAz69gJbgiobEaCXnQlxtp2jlO2I4FG4l8tno_H_lynuJ97W_FpMXSCV1I3tMwWK9r4diJ-dzSyOfaUukLy7DfMfVSD3fTGfqfP5utmHdLjvsP43AjDVZt31sx2K7Dd2q5RcE",
				            "e": "AQAB",
				            "use": "enc",
				            "qi": "3Pl3Nd5Wxt-mvFLLPcSKXoHk-UJscljiXAET0NdrpKj2Y8QxXQsKxPEecZwkjpPczkQy_UAtJZkCppTjcnTa6eiKSXjuTXEOD5fuiTDlaswCfhlHkNiVFytE-bynK8xPOXW_fYKkfbqZLMtzlw5A8wg1ScIE3TBzLwcJZK-LmtJpbRrFb8pPwOAlzHJ9Ve8aQ_YOiYRBziouobObMr4UtZMsH5GYIJ1OpAX3eq6C_CT1wZ5jXvxPW_ngzrsaK8H-DidJnMIRssMfxJk7v28Ub444rUza0oli1MRe31DxrdQSOxVYzhuVM7aJYXydfyrCKWuzUmxg4Or2qtrHd5vXfA",
				            "dp": "iijWwlJuRm1OgGKRfmQIHuq6g9Fv4CkKdQJITkFtSLBf3S-vTh6-YKu0P_EBlvOriajHuvMJROKvBjuVSCFtbGKnTuGwBofNenQEPA_uju830r4586s1VWtC0dtreBa9KBsuTjQpRNrl68z6kNUKp1zyfAHbfpibOuhMa7Ob3tRGBbcS57Q3YzcwyrcTK03wUtN1Xa5b1xzJhKeVLcHfFl8LvsMFm6z8USfTRy_shpd7AqFG81vrrTEk0NB9-OWTkzladyxke0Uo9KtjSMy-MTEthplKXlcILk7e4B48SX9gNl8vS9bCm3Q87_Uc2BYfzYQUaZjD3EVzb5tFN-o7RQ",
				            "alg": "PS256",
				            "dq": "DVJ080hvucNDzm9oA6kr2stKahdncAurcvyulSSEV3BkdILjSt0VH7TLdv8izbApjXwv-h9sFET_FFvUR2ICDfinMRVWdEEYabFfc6loyzyzOOghFSf1MPSfHwuC-d6L1r3YS9ot4Q_jJnBDHhDuG9h4-CI-XaosAqQmRtQ4uQtO8b-oM_1oPZ3hEu3CUbHN-0Fc5SAMmHA3SMKnE7og5fCPN2LL__l3uEh8Hr4j106QmGV0dfqKgqFFz036elssp-f-L1eGk2MqCAcLXY8kCvo0N8dGF7W6SztERY1T6NeF5i7Rv5wRirFc3YJtiqoKYm9NiMCd-SzRT_DGGhz1uQ",
				            "n": "8_NoKIULmZSgemhv97u15RdLnDD0UjhY0hWswryx9fK23iavJawBy7YfkBUQygegHT0ZYzE6CtjFaxlaWLEgFkB_N-JuHk_y3UjsKLPXOIqDSUU4PBxyOlPtPu3k1-EBmgM_VTtbXFtLxEql4GNsfeK6IpXP8r3MheeLyQ5l2UJICKqpbEbjZrIOOebpT0UP4ryfg13HT9fidufuqG_D2OprwiZhbHY3qai2tJakVYOW0jcfHe1feBO3daTxMXPcoN-ZTlAUTGnAMyPxuHCIc7x26FYAkRAOsPrXVw7V6DUpJTvPY7vSilakitU19TSVIMbRJWI3Wdon5ris-sV28uxJTGMZmUXH5H31kVHAa6mkwX1zv21R_Ve4mixjaZsPckn7S5LOhUAfL06n7G3zjNvszESK6tifL32nYAJPG-iJ24NeohGOEAcDiAk8lGi_XHpeUTjUG2Rt_SYRuoLeC8xZuoUdhL0VXhFfVhz1WL8JXOhrtHgWAPSDUsBYvZw2fow33fYFB94WUdrtKyLTI59V1YobEs2DfdJa9byilb9D83W4ZLIoiPVW4NUXoHp3ZvF1lfpixR5Vu2nOsYRscz8a2enAkHFkZ3oT0fhCRQrFKmIaC-GOxEsB9s1I8nYAoy-Y7QAkNbZlFmgTBbh9-0m_T978vpuR4StYxK6sPIs"
				        },
				        {
				            "kty": "RSA",
				            "n": "7DAw15x3fYDx4kn_mGCMNVA0JTYN0MNOyOEtnVKX8RDo2WUv6FxkXM_buBED6xsgmKq76PKBxvdQ1xJRB-65Zs3BfE1bTMNTk4kdqJyZv63H0FerjnRAynR5su7rchWGjnf1YOBYTB1d2fTZd4C-NktgcSNe2gQvwycBwvqP59fq6Epez3CWD71HKXkw_Ar5MHhweWvNLCjSLA0NTwGUNXs2MkvFXu9Oo04A2BDBgrJcJUz-iH_JxMiNGRWQ0zeY_dfJXgzKSVmnCEot9ZkIGbQz_kCYYI-G4oZH5ehihNpbzdsaY1Z0i40LMK3TeRCzz4kvaNpaNP0Uj2ON7pWJxw",
				            "e": "AQAB",
				            "d": "XjO-V1C9y93RG9s9yW3Ankb19V2A1yH-21oMknHtziVbadHU8M1g7i-tR9XphS0VrC50si1_G3dq080nao2JfvpT0Jsd7hj9zZtjq-uB2xSZ3HwLHc86REV-gKKQX7k8KLipstDoBruzwgnJbsN3mjooJhfgbf47eo-lQTokiWgtu7fjIl-QApFWCWjqzxewBG6TPO8bDSVWaWG0tnch4adqkPifl48Dj1vj6EHoYQH6htf3FEB30u_CfKrQW94dRjLRRdsrUZsbEO1aBnabVu7T6k7FUtqWCUsnwiW3TTSuOTY6HZmbWHgXqtzgCipdKfYudd24ZMlv4FujNhlKIQ",
				            "p": "_PE4MGsGfNobzZ8IteVhEgIQ4kEDCofRzjLk8_UIsisx_sbtH23FM2_MHLq5uMcB_n7xHZLXPKdFfGXHYtgmPcQdmIKZNwn4aLSRnbPLizYklFk3xi394CbsjZ0ld3Mh10RpnTchxfVv8RCUoLpAptQ8YaIOJE59LwcNAkno3Nc",
				            "q": "7wsfZP13DUoHIOFKjBwsN9yDRnTLXUiT-fd9lA5lHt8crpOj03i76kSLT2J046ATo-mKHbLW-51dP0uMBvSg7pb0G1LpbpdziBz4NDyCAk3zCrqLcc0HG_6-sF60Y7_GuWynqj7Tljp9Cm0mk9c0gJ-i8iZ2MusJKPH9DdMwrJE",
				            "dp": "Y51UNa1Qpsb3ACnLsmcdrYvmvlRAMFuBAyYECRfNTu24CNCq_h-q9DDpP79B-UVWwbWYm78CyT5PHUiwF3tsxBKCXVfBo21nyUSjvGnclXzDNzFTx9bBDxAFRVhIMxzCjRbCSUo3INEiPtv6zLnNUGZxSTobO601m9m5A9e7v40",
				            "dq": "kOnbRGvsKmBAZgxuFvZVUCAcbTAMwSZontcMiB8Z6lczmhni7SZ6NREMqzlyxzOZ9Hp1wn9SmQLTLrXr06OllD5rCc1_ug2SemeliHVOE3uR5WUld_3lW3FZxK4FCZuPK4XVNv4TYzVd7F4zTtjd-UPNkqMCcOfPi8T3tdPb5CE",
				            "qi": "9x2ylatmjWBaOz1mcxElBXCqRs0eas8Dkt1zMusb1N29S8WVGGjCbnjQaSjnYGJypw2MLh6DvnT940EbfPOC-6bjbPtjd0u7-QlAW-UBZBKIC1sgvmXug0BDhkgHQEwzEYLORspvD3iQLQhPmt6Y8FSMVpwOx-vsGDkgC14wkXE",
				            "kid": "Es25PBkrxmWLFeYpLIkpQTJ98oFE5pl_CdMaPJXuNt4",
				            "use": "sig",
				            "alg": "PS256"
				        }
				    ]
				}
				""";

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

}
