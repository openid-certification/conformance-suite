package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
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
public class ValidateCredentialCnfJwkIsPublicKey_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateCredentialCnfJwkIsPublicKey cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateCredentialCnfJwkIsPublicKey();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		String goodJwk = """
			{
				"kty": "EC",
				"use": "sig",
				"crv": "P-256",
				"kid": "fdbc1918-37df-44f1-9ee5-d3ea56480e83",
				"x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw",
				"y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c",
				"alg": "ES256"
			}
			""";
		env.putObjectFromJsonString("sdjwt", "credential.claims.cnf.jwk", goodJwk);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingIat() {
		assertThrows(ConditionError.class, () -> {
			String badJwk = """
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
			""";
			env.putObjectFromJsonString("sdjwt", "credential.claims.cnf.jwk", badJwk);

			cond.execute(env);
		});

	}

}
