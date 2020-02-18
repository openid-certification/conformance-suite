package net.openid.conformance.condition.as;

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

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EnsureClientJwksContainsKeyUseForAllKeysIfJwksContainsBothSigAndEncKeys_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureClientJwksContainsKeyUseForAllKeysIfJwksContainsBothSigAndEncKeys cond;

	private static final String GOOD_JWKS = "{\n" +
		"  \"keys\": [\n" +
		"    {\n" +
		"      \"kty\": \"RSA\",\n" +
		"      \"e\": \"AQAB\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"kid\": \"7acb328a-1fc7-4f4f-91d4-82f419571f83\",\n" +
		"      \"alg\": \"RS256\",\n" +
		"      \"n\": \"mb-w8NobNNbNnmYKga0WqQaaxnrzDCCfkgVxJU4qUg8sBg7FgMrSqiQTlGqdjBikj_l6e-ncG9h6dPxf6tArIRxu08cr-iJJzO-OUC6gYKhW43ntAYWr0rBy7NKKX_zDYm0AHs_2u1XfORpxxBQYvTynvWtWzmyUVvRjnZ9G5AlrBBadEdlrB_ecOzQPztxwX-AtSgF1R1VwSbJUXAjYfSv7GVxUUdBhtamnlEy8rmoksByPJJSgMjn8CH5jRl70QyX2rNsDciFZhaZ2PBDwFc12Db4Atp_PVAEc3m2FbXPPv574oUO1JfG00sqKPBJrZVsZKL-mU9qftrTs_j3q2Q\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"RSA\",\n" +
		"      \"e\": \"AQAB\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"kid\": \"7703ff02-0ec8-4eac-8236-e62db84ca181\",\n" +
		"      \"alg\": \"RS256\",\n" +
		"      \"n\": \"kV3zM_QPqjb2aZ7irsGnySMLuXIXYxSHJ1PK2QeaUQiH28wAErPrW0Zof6_QaULXnLE3EeaK2FkjTmUg3VLXoDpMcGkgSD6raKN0rY2W-Vca_KVAq0FRV-oHRNfG10O7cAy4jctVkW5p7kYRZUvjVhrIhYWm3mcKgfbtdcZcuanngzsIBt3MB6rvIXAgTi8BM4GrzdDrRSEiVN6Rau8gIu6fsiuk0RJWVcjtMCkoraVcXF7iyQ-ArrL-vWr2kJsUiAYnlUeY4boopN3IV_LYnViZdQfzYt0Rc-ArmIVvTqUxohI3FhrlvxwTaOo0FF_3WQ1ylJzjZVEzKA1IFIsJtw\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"RSA\",\n" +
		"      \"e\": \"AQAB\",\n" +
		"      \"use\": \"enc\",\n" +
		"      \"kid\": \"8cea53ac-5b2b-412e-8ace-1777116eace0\",\n" +
		"      \"alg\": \"RSA-OAEP-256\",\n" +
		"      \"n\": \"kCvgeJLta3da--qjrCrWRHYNFXUF4XJgVvCS_KwKYsM2DhW6C1ylgtsiBQRcJKOEJT3aFxYVyR7Fcoo3vydSlX85aO8_s2VzQUtM6Wr57jKAVqGGk419_84kU83X2MwQs2FNS_qIiAOyiymWc4LHo5dkfl60_uzoUY3PfhC6dtAudYKpHrbQcAf1InG3WrPp8sYr1-ehdThEleHR65D2fNU2Bqr0LzG2u31Owj0EkokVvgelRF53D51PLOeoC9Y2RsL2j40ec1u0AMPLnNkl6R0iPixgKOaqrE-cfbpyjDuDueusUraXlr9ukHFtrorzg475L1Fx-j798Pj6yeHx2w\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"EC\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"crv\": \"P-256\",\n" +
		"      \"kid\": \"fdbc1918-37df-44f1-9ee5-d3ea56480e83\",\n" +
		"      \"x\": \"W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw\",\n" +
		"      \"y\": \"qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c\",\n" +
		"      \"alg\": \"ES256\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"EC\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"crv\": \"P-256\",\n" +
		"      \"kid\": \"ab122fb4-f3b3-4fd6-8d59-df5cfa2fadab\",\n" +
		"      \"x\": \"7Yi8yF4i-7kKSSZ9BKJL_-Asi2vzq4vdWJFTlx3OevU\",\n" +
		"      \"y\": \"_XjphaovdNZrw_Qwh_loGLM-djGW29YibYojN2UYBU0\",\n" +
		"      \"alg\": \"ES256\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"EC\",\n" +
		"      \"use\": \"enc\",\n" +
		"      \"crv\": \"P-256\",\n" +
		"      \"kid\": \"cf781dc8-0449-4a4f-af62-d9831fd18ba9\",\n" +
		"      \"x\": \"9ZPcKbHZ_csGHoOcSVxwV0ln6hlWJgBMNFJOkvg_yCI\",\n" +
		"      \"y\": \"Vny8ZIHTWCisBYjG5JuNe8TEqz6lLbFVfIeKINXa6jE\",\n" +
		"      \"alg\": \"ECDH-ES\"\n" +
		"    }\n" +
		"  ]\n" +
		"}";

	private static final String BAD_JWKS = "{\n" +
		"  \"keys\": [\n" +
		"    {\n" +
		"      \"kty\": \"RSA\",\n" +
		"      \"e\": \"AQAB\",\n" +
		"      \"kid\": \"7acb328a-1fc7-4f4f-91d4-82f419571f83\",\n" +
		"      \"alg\": \"RS256\",\n" +
		"      \"n\": \"mb-w8NobNNbNnmYKga0WqQaaxnrzDCCfkgVxJU4qUg8sBg7FgMrSqiQTlGqdjBikj_l6e-ncG9h6dPxf6tArIRxu08cr-iJJzO-OUC6gYKhW43ntAYWr0rBy7NKKX_zDYm0AHs_2u1XfORpxxBQYvTynvWtWzmyUVvRjnZ9G5AlrBBadEdlrB_ecOzQPztxwX-AtSgF1R1VwSbJUXAjYfSv7GVxUUdBhtamnlEy8rmoksByPJJSgMjn8CH5jRl70QyX2rNsDciFZhaZ2PBDwFc12Db4Atp_PVAEc3m2FbXPPv574oUO1JfG00sqKPBJrZVsZKL-mU9qftrTs_j3q2Q\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"RSA\",\n" +
		"      \"e\": \"AQAB\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"kid\": \"7703ff02-0ec8-4eac-8236-e62db84ca181\",\n" +
		"      \"alg\": \"RS256\",\n" +
		"      \"n\": \"kV3zM_QPqjb2aZ7irsGnySMLuXIXYxSHJ1PK2QeaUQiH28wAErPrW0Zof6_QaULXnLE3EeaK2FkjTmUg3VLXoDpMcGkgSD6raKN0rY2W-Vca_KVAq0FRV-oHRNfG10O7cAy4jctVkW5p7kYRZUvjVhrIhYWm3mcKgfbtdcZcuanngzsIBt3MB6rvIXAgTi8BM4GrzdDrRSEiVN6Rau8gIu6fsiuk0RJWVcjtMCkoraVcXF7iyQ-ArrL-vWr2kJsUiAYnlUeY4boopN3IV_LYnViZdQfzYt0Rc-ArmIVvTqUxohI3FhrlvxwTaOo0FF_3WQ1ylJzjZVEzKA1IFIsJtw\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"RSA\",\n" +
		"      \"e\": \"AQAB\",\n" +
		"      \"use\": \"enc\",\n" +
		"      \"kid\": \"8cea53ac-5b2b-412e-8ace-1777116eace0\",\n" +
		"      \"alg\": \"RSA-OAEP-256\",\n" +
		"      \"n\": \"kCvgeJLta3da--qjrCrWRHYNFXUF4XJgVvCS_KwKYsM2DhW6C1ylgtsiBQRcJKOEJT3aFxYVyR7Fcoo3vydSlX85aO8_s2VzQUtM6Wr57jKAVqGGk419_84kU83X2MwQs2FNS_qIiAOyiymWc4LHo5dkfl60_uzoUY3PfhC6dtAudYKpHrbQcAf1InG3WrPp8sYr1-ehdThEleHR65D2fNU2Bqr0LzG2u31Owj0EkokVvgelRF53D51PLOeoC9Y2RsL2j40ec1u0AMPLnNkl6R0iPixgKOaqrE-cfbpyjDuDueusUraXlr9ukHFtrorzg475L1Fx-j798Pj6yeHx2w\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"EC\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"crv\": \"P-256\",\n" +
		"      \"kid\": \"fdbc1918-37df-44f1-9ee5-d3ea56480e83\",\n" +
		"      \"x\": \"W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw\",\n" +
		"      \"y\": \"qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c\",\n" +
		"      \"alg\": \"ES256\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"EC\",\n" +
		"      \"use\": \"sig\",\n" +
		"      \"crv\": \"P-256\",\n" +
		"      \"kid\": \"ab122fb4-f3b3-4fd6-8d59-df5cfa2fadab\",\n" +
		"      \"x\": \"7Yi8yF4i-7kKSSZ9BKJL_-Asi2vzq4vdWJFTlx3OevU\",\n" +
		"      \"y\": \"_XjphaovdNZrw_Qwh_loGLM-djGW29YibYojN2UYBU0\",\n" +
		"      \"alg\": \"ES256\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"kty\": \"EC\",\n" +
		"      \"use\": \"enc\",\n" +
		"      \"crv\": \"P-256\",\n" +
		"      \"kid\": \"cf781dc8-0449-4a4f-af62-d9831fd18ba9\",\n" +
		"      \"x\": \"9ZPcKbHZ_csGHoOcSVxwV0ln6hlWJgBMNFJOkvg_yCI\",\n" +
		"      \"y\": \"Vny8ZIHTWCisBYjG5JuNe8TEqz6lLbFVfIeKINXa6jE\",\n" +
		"      \"alg\": \"ECDH-ES\"\n" +
		"    }\n" +
		"  ]\n" +
		"}";

	@Before
	public void setUp() throws Exception {

		cond = new EnsureClientJwksContainsKeyUseForAllKeysIfJwksContainsBothSigAndEncKeys();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_noError() {
		JsonObject client = new JsonObject();
		JsonObject publicJwks = new JsonParser().parse(GOOD_JWKS).getAsJsonObject();
		client.add("jwks", publicJwks);
		env.putObject("client", client);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_firstKeyHasNoUse() {
		JsonObject client = new JsonObject();
		JsonObject publicJwks = new JsonParser().parse(BAD_JWKS).getAsJsonObject();
		client.add("jwks", publicJwks);
		env.putObject("client", client);
		cond.execute(env);
	}
}
