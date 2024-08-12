package net.openid.conformance.condition.as;

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
public class EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys cond;

	private static final String GOOD_JWKS = """
			{
			  "keys": [
			    {
			      "kty": "RSA",
			      "e": "AQAB",
			      "use": "sig",
			      "kid": "7acb328a-1fc7-4f4f-91d4-82f419571f83",
			      "alg": "RS256",
			      "n": "mb-w8NobNNbNnmYKga0WqQaaxnrzDCCfkgVxJU4qUg8sBg7FgMrSqiQTlGqdjBikj_l6e-ncG9h6dPxf6tArIRxu08cr-iJJzO-OUC6gYKhW43ntAYWr0rBy7NKKX_zDYm0AHs_2u1XfORpxxBQYvTynvWtWzmyUVvRjnZ9G5AlrBBadEdlrB_ecOzQPztxwX-AtSgF1R1VwSbJUXAjYfSv7GVxUUdBhtamnlEy8rmoksByPJJSgMjn8CH5jRl70QyX2rNsDciFZhaZ2PBDwFc12Db4Atp_PVAEc3m2FbXPPv574oUO1JfG00sqKPBJrZVsZKL-mU9qftrTs_j3q2Q"
			    },
			    {
			      "kty": "RSA",
			      "e": "AQAB",
			      "use": "sig",
			      "kid": "7703ff02-0ec8-4eac-8236-e62db84ca181",
			      "alg": "RS256",
			      "n": "kV3zM_QPqjb2aZ7irsGnySMLuXIXYxSHJ1PK2QeaUQiH28wAErPrW0Zof6_QaULXnLE3EeaK2FkjTmUg3VLXoDpMcGkgSD6raKN0rY2W-Vca_KVAq0FRV-oHRNfG10O7cAy4jctVkW5p7kYRZUvjVhrIhYWm3mcKgfbtdcZcuanngzsIBt3MB6rvIXAgTi8BM4GrzdDrRSEiVN6Rau8gIu6fsiuk0RJWVcjtMCkoraVcXF7iyQ-ArrL-vWr2kJsUiAYnlUeY4boopN3IV_LYnViZdQfzYt0Rc-ArmIVvTqUxohI3FhrlvxwTaOo0FF_3WQ1ylJzjZVEzKA1IFIsJtw"
			    },
			    {
			      "kty": "RSA",
			      "e": "AQAB",
			      "use": "enc",
			      "kid": "8cea53ac-5b2b-412e-8ace-1777116eace0",
			      "alg": "RSA-OAEP-256",
			      "n": "kCvgeJLta3da--qjrCrWRHYNFXUF4XJgVvCS_KwKYsM2DhW6C1ylgtsiBQRcJKOEJT3aFxYVyR7Fcoo3vydSlX85aO8_s2VzQUtM6Wr57jKAVqGGk419_84kU83X2MwQs2FNS_qIiAOyiymWc4LHo5dkfl60_uzoUY3PfhC6dtAudYKpHrbQcAf1InG3WrPp8sYr1-ehdThEleHR65D2fNU2Bqr0LzG2u31Owj0EkokVvgelRF53D51PLOeoC9Y2RsL2j40ec1u0AMPLnNkl6R0iPixgKOaqrE-cfbpyjDuDueusUraXlr9ukHFtrorzg475L1Fx-j798Pj6yeHx2w"
			    },
			    {
			      "kty": "EC",
			      "use": "sig",
			      "crv": "P-256",
			      "kid": "fdbc1918-37df-44f1-9ee5-d3ea56480e83",
			      "x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw",
			      "y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c",
			      "alg": "ES256"
			    },
			    {
			      "kty": "EC",
			      "use": "sig",
			      "crv": "P-256",
			      "kid": "ab122fb4-f3b3-4fd6-8d59-df5cfa2fadab",
			      "x": "7Yi8yF4i-7kKSSZ9BKJL_-Asi2vzq4vdWJFTlx3OevU",
			      "y": "_XjphaovdNZrw_Qwh_loGLM-djGW29YibYojN2UYBU0",
			      "alg": "ES256"
			    },
			    {
			      "kty": "EC",
			      "use": "enc",
			      "crv": "P-256",
			      "kid": "cf781dc8-0449-4a4f-af62-d9831fd18ba9",
			      "x": "9ZPcKbHZ_csGHoOcSVxwV0ln6hlWJgBMNFJOkvg_yCI",
			      "y": "Vny8ZIHTWCisBYjG5JuNe8TEqz6lLbFVfIeKINXa6jE",
			      "alg": "ECDH-ES"
			    }
			  ]
			}""";
	private static final String BAD_JWKS = """
			{
			  "keys": [
			    {
			      "p": "9XkzC_oubyApeoG0xhSJub-3lpEpe-9dpwh1kyUiAIA6V-GaHT8A8dXhodj2N3pCV1ITjzLwhjlF7xhtNZl6uckkT35W4Duq3q2j79atLRGA9dON2tYwOM4JObtnjYbXrmQ4wVuKqtJK5ewyHBWn-sSqLDoa4_4PWXDEp4JO5-8",
			      "kty": "RSA",
			      "q": "oFeKi2q8jvmn9_Gu45HTiV1m9QlffCC1W2JmJKD1UY4V3JJJRJtp3OwyjPtTCcEiekgiTwPUQKabNv0FFeVQrThjqD8N7i4wEV9dNp6ZAwWF9H_JLfzwJk0OrhAXXRXypke11wHkJ3bSUpHQjP3iAh6X-3xzFiTj3eXZbT9E0bc",
			      "d": "gCxBKxj4s10bf6Cbw61zhaee4jZfZk3QGWB2Cd6XA1_omIg5-Hudkf-PXxxeiQuGLQyTIrg8Ke6Xb-1xXQT735efgOe67sUk9zcCUi_8vXe5L9xWS_2O148H0AxZFVpX3Db4cbd3fodBF47HJuqGEUR-pjfU3zO_mpoUbbP52mH5R9xAEgGWSBcCn7RJZhtXtR5NV0ZARRAZHjuVwJ7gLo0DlrE0mXrRPkXuq0x73tCiilCOUUMEblTmz77WGCgXC4vWmvTVkzd39_8Aeb5OvuvwUo5Cu6JjAN7Rwx2tbKTNmQVhy0qvcd-mYttbBnpufgCjUEq-vOo9plWFIVXUvQ",
			      "e": "AQAB",
			      "use": "sig",
			      "kid": "7acb328a-1fc7-4f4f-91d4-82f419571f83",
			      "qi": "aUQWAE7VxWTc7g7g6WQ5bScJF9jxEzzrObkW9Hrj5ivg_sePtaO9Ylyz7l2jZeFZ3IpMYoycvZDxXZoIPZeGgugU2lvNCfKM5Fy0UQmZtlpKlRaQn1voC8dvTxLOKQyxDBMliEYX8QcVTda0JXQaERlDzHwgSF5IO9y1iPcamhs",
			      "dp": "YHPB-gfQrPtvMOK8PESfpSvOoEvjh_4Sx2oMgMfLZ3T698L-glVaMqv-n1CX1k6kuJjYdKDoX2UrtHndl3z5-vws10tmKW9ogVyrGLc6joXdLTp197HHetqCR3v6gWKlZrGcl2U3xE0i_trjCaP8wAz8bKpcHI-cSmGD8-nAxWM",
			      "alg": "RS256",
			      "dq": "ZZUQgSMuMqaYc8z6L9LDYdN61uMJS2qteyfr7mjLAtAxqXxETuLnPr-lhwvyn8tsPGxW0Bhs8EidLUk_X4_YfOWsvXq1KKC6c0cnluMDrZZiN3R90FTfEGThWAV5x8-P2yizW3QnR5lPxl_68FgoR496WimOAAl_E6X7Cet7fiE",
			      "n": "mb-w8NobNNbNnmYKga0WqQaaxnrzDCCfkgVxJU4qUg8sBg7FgMrSqiQTlGqdjBikj_l6e-ncG9h6dPxf6tArIRxu08cr-iJJzO-OUC6gYKhW43ntAYWr0rBy7NKKX_zDYm0AHs_2u1XfORpxxBQYvTynvWtWzmyUVvRjnZ9G5AlrBBadEdlrB_ecOzQPztxwX-AtSgF1R1VwSbJUXAjYfSv7GVxUUdBhtamnlEy8rmoksByPJJSgMjn8CH5jRl70QyX2rNsDciFZhaZ2PBDwFc12Db4Atp_PVAEc3m2FbXPPv574oUO1JfG00sqKPBJrZVsZKL-mU9qftrTs_j3q2Q"
			    },    {
			      "p": "6fDB1Hr6K6i-eIZOuvRuLFpwi5LTTFbpzf_6RVOSlnI4RtLY6tKy5BPXnVzVKO8TWY7KtAsomd7GJhzZeWuyvgkxO3o8efkCqn-VMhuRz5zEz46xKF6emU_r0V-vGMrdfH54FYgJY-5ghswZsd1Duh4mWF5HlOrejN6Vv5ZG_Dk",
			      "kty": "RSA",
			      "q": "ncQgB2f5n4-hlMe6C8JNASmhDaLnA4L5ekMKUXQEaVl9VPohQ9BWhAz2thpg6LxHWAleoxLncj_63o_nWGrDonp1LXrgAlUnJEhXFMWFe5NJPBCi1zyYjd0NFx2NIvifp648b9ctglZP8OYTWCkj2TwdnhiIFa21mA1kX8eIRrM",
			      "d": "i8NNZ_Yf3bemZ2Mval9YuqOqyWtZ2RWVWZL9G5_4rYfLJlpfslYNSc7_qtdwMECogaX1f730PrXq-cAOjOwk_jKIyDZvjY1C70B9ficrxYQFsHYETZS0WkFQLEH4Bv7FDfC9C3QD2b-j3Y7igHZ-7RN945gSZLPr_-u3A-JZsYViWIlKjK3hp_PJJbzZ1Ev0-GlrYXcHjssTlY1MoPLY-RrG2ziIOfh1QzVw_WnuHrl1ctSQ9Ee_GmKkgpmjvbh7NRvAFsEsH1zAd5tO3OnwDMkYx8LBz3kfxFmcRIIWwAL2BKM7yoC2uPMy1YR9C-nHdiB2scF2NTN4EYSz2JBAMQ",
			      "e": "AQAB",
			      "use": "enc",
			      "kid": "8cea53ac-5b2b-412e-8ace-1777116eace0",
			      "qi": "PjJBfLyXVtnZecpspVmXu3ZID9XIa7gFQtsV1kt8GxKiQ0Q0Dpe0W1othq6WWBxXoNyyR41dnpd-oHPbC4IvD2s-bvIz7QgHGti2d1zrfxOtX4zuU3eoUqU91K1SsD2i5gE1ZmI6Yu2IRn4WeQl80RlrUN4Y7ibpIF1eb0h7U58",
			      "dp": "MnwZaEBNwqQ8zoe1b9UV_Y5a4VQDM2NCO4l1OgbKCJuHl4ki9LIZluzNSBFjktzdISOWjPonfeU8f7cUG4bpYOdc74f2ix_4ulCkItceRV6AfoY9pwecVFuR-XIbuE73M0mbopHj7OPhgtWC6gZk-kJNZh6adgGlm2LhkCHfGck",
			      "alg": "RSA-OAEP-256",
			      "dq": "ckL-U37Zitey1heFRkj2DnaA_2hRWfi9CPCPA4k8PLSkkiwKdZvpXQAqmp8mOslJ_pfio8boO5Xc8t5UVnQUtpNsVIEexnG3xI5kC1Y3I9X7f3YpCiwH7Q8BERHFC2P39bC6cuHsQu6SPU3VCs_Y99eTN9JalSDXWYWxgbD4oQM",
			      "n": "kCvgeJLta3da--qjrCrWRHYNFXUF4XJgVvCS_KwKYsM2DhW6C1ylgtsiBQRcJKOEJT3aFxYVyR7Fcoo3vydSlX85aO8_s2VzQUtM6Wr57jKAVqGGk419_84kU83X2MwQs2FNS_qIiAOyiymWc4LHo5dkfl60_uzoUY3PfhC6dtAudYKpHrbQcAf1InG3WrPp8sYr1-ehdThEleHR65D2fNU2Bqr0LzG2u31Owj0EkokVvgelRF53D51PLOeoC9Y2RsL2j40ec1u0AMPLnNkl6R0iPixgKOaqrE-cfbpyjDuDueusUraXlr9ukHFtrorzg475L1Fx-j798Pj6yeHx2w"
			    },
			    {
			      "kty": "EC",
			      "d": "UdhGNSIsGbMKh4o4KZhYdVGGWVLgpZYf3Y_6y6TywoA",
			      "use": "enc",
			      "crv": "P-256",
			      "kid": "cf781dc8-0449-4a4f-af62-d9831fd18ba9",
			      "x": "9ZPcKbHZ_csGHoOcSVxwV0ln6hlWJgBMNFJOkvg_yCI",
			      "y": "Vny8ZIHTWCisBYjG5JuNe8TEqz6lLbFVfIeKINXa6jE",
			      "alg": "ECDH-ES"
			    }
			  ]
			}""";
	private static final String SYMMETRIC_JWKS = """
			{
			"keys": [
				{"kty":"oct",
			       "alg":"A128KW",
			       "k":"GawgguFyGrWKav7AX4VKUg"},

			      {"kty":"oct",
			       "k":"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75
			  aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow",
			       "kid":"HMAC key used in JWS spec Appendix A.1 example"}]}""";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_noError() {
		JsonObject client = new JsonObject();
		JsonObject publicJwks = JsonParser.parseString(GOOD_JWKS).getAsJsonObject();
		client.add("jwks", publicJwks);
		env.putObject("client", client);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_privateKeys() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			JsonObject publicJwks = JsonParser.parseString(BAD_JWKS).getAsJsonObject();
			client.add("jwks", publicJwks);
			env.putObject("client", client);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_symmetricKeys() {
		assertThrows(ConditionError.class, () -> {
			JsonObject client = new JsonObject();
			JsonObject publicJwks = JsonParser.parseString(SYMMETRIC_JWKS).getAsJsonObject();
			client.add("jwks", publicJwks);
			env.putObject("client", client);
			cond.execute(env);
		});
	}
}
