package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AbstractSignJWT_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject claims;

	// Concrete implementation for testing
	private static class TestSignJWT extends AbstractSignJWT {
		@Override
		public Environment evaluate(Environment env) {
			// Not used in direct calls to signJWT but required by AbstractCondition
			return env;
		}

		@Override
		protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
			env.putString("signed_jwt", jws);
			env.putObject("last_header", JsonParser.parseString(header.toJSONObject().toString()).getAsJsonObject());
		}
	}

	private TestSignJWT cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new TestSignJWT();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		claims = JsonParser.parseString("{\"iss\":\"me\",\"sub\":\"you\"}").getAsJsonObject();

		jwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();
	}

	@Test
	public void testSignJWT_withHeaderParams() throws JOSEException, ParseException {

		JsonObject headerParams = new JsonObject();
		headerParams.addProperty("test_param", "test_value");
		JsonArray arrayParam = new JsonArray();
		arrayParam.add("v1");
		arrayParam.add("v2");
		headerParams.add("array_param", arrayParam);

		cond.signJWT(env, claims, headerParams, jwks, true);

		String signedJwt = env.getString("signed_jwt");
		assertThat(signedJwt).isNotNull();

		JsonObject header = env.getObject("last_header");
		assertThat(header.get("test_param").getAsString()).isEqualTo("test_value");

		JsonArray resultList = header.getAsJsonArray("array_param");
		assertThat(resultList).isNotNull();
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.get(0).getAsString()).isEqualTo("v1");
		assertThat(resultList.get(1).getAsString()).isEqualTo("v2");
	}

	@Test
	public void testSignJWT_withJsonObjectInHeaderParams() throws JOSEException, ParseException {

		JsonObject headerParams = new JsonObject();
		JsonObject nestedObj = new JsonObject();
		nestedObj.addProperty("k1", "v1");
		headerParams.add("object_param", nestedObj);

		cond.signJWT(env, claims, headerParams, jwks, true);

		String signedJwt = env.getString("signed_jwt");
		assertThat(signedJwt).isNotNull();

		JsonObject header = env.getObject("last_header");
		JsonObject resultObj = header.getAsJsonObject("object_param");
		assertThat(resultObj).isNotNull();
		assertThat(resultObj.get("k1").getAsString()).isEqualTo("v1");
	}

	@Test
	public void testSignJWT_nullHeaderParams() throws JOSEException, ParseException {

		cond.signJWT(env, claims, null, jwks, false);

		String signedJwt = env.getString("signed_jwt");
		assertThat(signedJwt).isNotNull();

		JsonObject header = env.getObject("last_header");
		assertThat(header.has("test_param")).isFalse();
	}
}
