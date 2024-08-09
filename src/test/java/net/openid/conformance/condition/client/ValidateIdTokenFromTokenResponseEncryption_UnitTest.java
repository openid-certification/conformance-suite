package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class ValidateIdTokenFromTokenResponseEncryption_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateIdTokenFromTokenResponseEncryption cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateIdTokenFromTokenResponseEncryption();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_encrypted() {
		// RSA encrypted, kid = "fapi20200623"
		String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";

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

		JsonObject response = new JsonObject();
		response.addProperty("id_token", idToken);

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_encryptedMultipleMatchNoKidMatch() {
		assertThrows(RuntimeException.class, () -> {
			// RSA encrypted, kid = "fapi20200623"
			String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";

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
							    "kid": "fapi20200623NoMatch",
							    "qi": "IyiL1_cnC5Najrfvu6ypiR3JmpHXDs8FkYJUdfqXnVWaBNxkdDi3iks943JyIfp8JI-NWndiNB6DdSBzecARDqew3lQomIsGsoR0wPFcHDee-d-NmBwEm3TSHrleGjj0oBJe6BDnAdsaHhsL9NLo_1aOd_9W_TM2kcuSntM-DFA",
							    "dp": "jjER1tu2hLrh6d34JSc3zubsMOZyEkXgRRnRgFEFsnPAhtRf9l99Ot5cuU4EINuCaI1Lyi46tJG7de8fy30RbdwU4Myf_4mcbjn4nO2sfd_dj5W05mz8YYM7yxB2cGKOOLFOBf99mdzSFNGS4PC0SL9sqvAbC4FyIUsJNaZIkOs",
							    "dq": "xGck3jMl1cIPhcO0aAvuMQaW_df2iqLlsYTTLPsnHLpLvTwMpx9bMMUs95NTf9KBtJ3yu8dcl17rktYi4zHTjQtegRYCIXPphgAXBL7k3jjMfgloQfgfhO-ZNEiP1-YwJ8WNzz3hKDNVr-hf4mcMj9qw1_jAIUUGzCDrGOa2gQ",
							    "n": "jVc92j0ntTV0V1nwZ3mpGaV2bME4d6AMS2SRrJBM0fLehaTEqDNzGu0warz2SC9bhcBOB5_q3mYBFjmTwWzSbsk6RYETnAgViXg67PgH7Vkx2NCtwgQW3cNdnUZWRNYHsoevkx_Ta1X6Vi9ulebU_BCKjrF-6CjVcGgEsO_S5DKcukGHdf81WlQOq3zGQg4h7MLArrbPSTHHORDsu_87qY9m2EhiYSOBSF5rHsfDo7zWI5FWNG-_HO-CBM005bykIIS1aXCXx1jOW1OrKcp5xv3e-BR6MJTxncZJ4o1GtynJI8kLXRgltLArSOkbzNEr9GjU9lnSSxKLMtRLKkG2Ow"
							}
						    ]
						}
						""";

			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedMultipleMatchSameKid() {
		assertThrows(RuntimeException.class, () -> {
			// RSA encrypted, kid = "fapi20200623"
			String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";

			String jwks =
				"""
						{
						    "keys": [
							{
							    "kty": "RSA",
							    "d": "ZHPB6jX2Np7cUjFu2iiYlT1YPdJ6KSyyPjEWT72TYOX6O-0a48Ez3m5OnOy_bCwD3F7_WAL0wbWXWFsRt6DmCqW-MGxG38klMQwV4dhgI6lEYyfhvBazJyQJeqHwKE5VIezgyBJyGkK9GHUqq3k3s-hsS_MLVE6BDcLNf9iPRUlQ5JJLHGMZYAW6uv3ex-LGqhLT_BWX4Y1fowAgUvDLcaLj-_tRonKF4oJRiz6oMO4KTJJofhXzptY0T_K0u8bx7Y7JN-W7fC8havmG5bnKetsLQYHcn7ddWtOA_6Qxjxb6SMIr1bNWeGVA5p15NMHcIfsNmSK03Y-Xic5eiuBhwQ",
							    "e": "AQAB",
							    "kid": "fapi20200623",
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

			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedMultipleMatchNoKid() {
		assertThrows(RuntimeException.class, () -> {
			// RSA encrypted, kid = not specified.
			String idToken = "eyJjdHkiOiJKV1QiLCJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00ifQ.Be7JR7AJ395nfk8dKnhkL60IwnZf1WBQ_1RrSQN-pWx3h2Uba_gOZXSpPICdlj4SjYMKjv2a396zt2VwxMDCD3yEUagWQYtJ6c8YSZbzsF27OPReYtt8xZZ7ZtpnF45fI4cJudLcTQk2XtWzR1CTiIJUdYNA5g0NUQ40gWdvEXQSHwVJ9keiX7_sPdQ2dSMVQ0RsG2KkUYaYid0FTibNJSvZwwlL_XSfwn2ZLOkuNHfIQMkZ4VLg4GcC_667AOp-x0zg9jF2drby0silZCu439UAIg48yAnLoxnElKN-rqICmLp_aMCt2Afn38fu_hctmBBu6M94B84Hy2-N1aaUXw.y18rDSifsvAvK8yG.DESkOXAWWgrdQFoQFM1co6q7HHcYikB5mMvVtr-KKS-VgbC7k_7M48MQs1EISg2De6JUmI2xx_B-T1jcCEyEFuEcemjLJqfgQ1xcDp4sbwGdERRbXhgaOMKIv1n5bZCjbwBMJfxqDROWW2DPI2BTVomJUslRJqQWOYb_nJzsEZYo7Y7VrGWELs782JRJXvsKUv9wjfEUaIV5QmbrJKACuivv1lHnlE4_pPjF58KXXxjOHzq1bW_efm6VnzKdZgac7BiTx37r2Qa_u1I6lbN8dKl04rI9My01stXj_QXhUQPVR_4GSrNtJUlgdpFHn7OEoI3e9phDQPKnaUU.q7v6AxXYcmuQHlWxGacevg";

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

			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedSingleMatchNoKidMatch() {
		assertThrows(RuntimeException.class, () -> {
			// RSA encrypted, kid = "fapi20200623"
			String idToken = "eyJraWQiOiJmYXBpMjAyMDA2MjMiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAifQ.FuPr3fjrF_K5WpXRh7aYsEVj_hOJ9BWQOFNk_zEnz3YmwhEvolnMUUyIxHunSQ18asXggNEl6kTAtwo0V3RAJ00eIDQVxzLXfElH4Cw8Lpn34X4iqCeUDtwkKInyhETdvOkSd_Wzyw_dSLYlb60tpp5MYKzL68fNteBHv5mTmkQK6KZGO6JBg8ongx4mUa82w5syivHQMM7y46oTdBijy45adsDMRN8zB9p_PwQV-SYPOziQvlFeZnOISTiHBOT8PvL7TqSkY4sjZyBBt-cinHz-lf4uWe8F1r55HOTQj78HJl2W88g2viBGOOioq6Ie1FdBfe3h8xKenOD-k5ezfQ.8wRpiZTYGKNQq-l-y8UfEg.QEXO2olUMgqGJABKvlhQ_gf4HLa5lAgJ19BGT1lVGixkdlW7pc3VnXfv4DIXYYCXkvu1uyYhDf-T8BXRyokHzUGwslLFyLW4OjY59WZzfKXCBSb26pDuinY3PACNNOH9Q1WOjCtod0OoBnfMNuitfCGAqC7NryMWy_M-cZVIZxhGNtFkK8FqVCn2OlziXBqbdMXRJgCB7kRHSIWW1WqJE-8LHZ9X5fYWRB5XNrBVY1_t3ja6APR88CziQ4EJoGjVVfqXwH05ZMUQIOrLl3evzIEKfpzi8PDOunkSzZXkLZXH9zAbiQttD5Wy660LZ56ThGsbuEFvrImcn6NXrQlFtm3hmASG2D97Pp2WXtoWsJ3EUeuBuWHtEbe4Txger_0S4_1_c3M_1mk8Gretpq_3AMFOx1b95olZWl8rDMEkXQSWqMlyrh1oNdc1iP7axy6yv7fZwqVQhrrUBA7PfUZKlFlQA6VAeyluCVTvMfyVSnEy7ss9inc62ro2kEzIu5jzccR5GrkkfN86TKIRLupxJGXZgWk8TPo8hLr1Dj-8IDGSS07d9T8QV3C3LG67lKpgGoS2YXu0wm_FAt7q28qD0_sOfQCcGnq4GUv9n3zqzDfsJozOKHEbDFsn9UXA5lxyWzB0Qi5yJyKnO7b89UHzsF2gUlyPT5TUs5fd43ssaIaunfy6zaaf_7rt9KF2XTiRkHqtDENYT2vx7SMhlDfwsDaB8bZYJu69msUbG6Gye8KlhhsEX7Oa2kLZbhl0JcFRn8Hc00OrdlhbyGX3_NIhaV-7tF8AnJDDR5FQP1AMAKS5B6ACljFcjIWphktC6T-mJzemP-zaTMh7EBZQ7AQLWj8mrIU8unE7t4dc-78c2GDrq4NE-3wg1SNIC3Y8_RnhljsXmaXDWv-3svXxnhOxbw4MilKWoVgAA2sLTSHK9IHz7CVfKC4lEmPtgZFI2qgqcFvqHp_s6iA0DyBwCjcyse-z8DfVg0geISGrgj5_cLEgZcMc7cb2fIPrU_8juSZLd-1auuj9Oepq2MM8VMH5fw.p4il_u3aG-Ter7gru7XWOg";

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

			JsonObject response = new JsonObject();
			response.addProperty("id_token", idToken);

			env.putObject("token_endpoint_response", response);
			env.putObjectFromJsonString("client_jwks", jwks);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_encryptedSingleMatchKidMatch() {
		// RSA encrypted, kid = "fapi20200623"
		String idToken = "eyJraWQiOiJmYXBpLWp3dC1hc3NlcnRpb24tMjAxODA4MTctMSIsImN0eSI6IkpXVCIsImVuYyI6IkExMjhDQkMtSFMyNTYiLCJhbGciOiJSU0EtT0FFUCJ9.3-4_q_nOo8OYK055Mt5_8kFja0DF98ZAgqJQW-OWFRgPN7CJcj3F0kOWv2y78v2CigFxk7p_uOqzagZ2xHpN17Y3REIq7bmCZXAYjCThWLONr9u7rOdaM7YuHxRk0x1twfWfNYOva2o8LgT9ljtR2DpHKdR2vathkX913ztljX6013MVAsHyFLhHNBU5-OsGYmrMrJxSR-uLz8TKr_OMx9GNZ0kuhmM15M6dcJD1LWzYnwVYyot895vHQL4zKk4ZnkUxM-UIMt6NZqo07i3iTHlbAJecmlKB7594ygSgCLJdhYJiWtaYiMb8GjcdMjrGXOm7Gz53Tm6cFv0mMRnXng.bOeq7cP1hFkGR9eWPxdq9Q.4IUGWFAWKmE8t-R2vyV0g-DJAJHVmEaXOA6ULKbTXOPZuvUo9atqjbTghkvZHwyy0NgQQ-QjYvRWhS3fvA1YGbI2Ny1jxm0_JYJ-01C44EBvD7W0Lic1Du1YPQMr51d0hDTgUAGw6HFXXJM1xxrGKowAzzFDCkjcUNLDYBlRaF4QB0dmTix527A9h1ZAC9qPNorueNea7Kh_V_yq9TwVWCwcOIGwsgGOZmLuZzZnn9hRAz7ccRKhME7krV28Oco1PD8nH-HEby6SE79rox1d_hcgE4QV-3__ny2fCZA3IszsXboRpG0ObWIvLSFmrThVD2pGQjMAZiWbMkZVORnpIsdwDFMFle1nfoDOoFHJFwjDET8eupIaZHpvla4rUuuYMQ57FsZvsqQKprrBh1cQElSVPO8v9oUQjbFk375qYRld_avOTwtQ-yRhWlhH02ZQxb11ZwK5zpA2EbBzPT0tHCmH6hS70VEtTjlXhrzKl6tdZMETgu9ZdgzXtaq-9AAFpZLqA-yFPnxSZeDCiLSiKyCTGg_gEbfeROg5LE14NZ9xwxZBkd38aSTkS5CCWNj47R8M9_NEmnQ2YU4oJl2XBBYoHqVgaQh1cn0yqMAQ1YHV-Oj_W9xmc032ZeYTiW0W3EwpkuRaZno37qciNNEzzZPQYs7mQuqThhf8uGYwHtqIqo1CX13ri6qcVTsNKdZn9hDFpv4xQLMxi1aaSfSCXLXzFL7p2u3jgKh15vVV79JoK0ip61Na4hj54Va_mVmUoWUpQ3CB86G00Jq-Uo3jB9jBFp9GovcnMxplWKb_lKs5Ez7e-oVGWg8jJMhg3vB1jtYkt1NUvwIp-9-PB2Yi_HdA4d0h2-ncND2WKk-lpZwkLywRdZCY6T50NsQ7nJP2UbzJrhYyv7E02WZ1QDTUpgk_34XwwWkmW5E0JNACcA3H3Qur7GPjQECFCI2t3vBppsrW0Op3MIxANBRJZUodKw.ZPyeMGMoebXcb_3FujDRwg";
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

		JsonObject response = new JsonObject();
		response.addProperty("id_token", idToken);

		env.putObject("token_endpoint_response", response);
		env.putObjectFromJsonString("client_jwks", jwks);

		cond.execute(env);
	}

}
