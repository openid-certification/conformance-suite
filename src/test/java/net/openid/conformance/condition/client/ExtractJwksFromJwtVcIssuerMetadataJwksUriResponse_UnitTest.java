package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.notFound;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link CallJwtVcIssuerMetadataJwksUri} and {@link ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse}
 * together, since the extraction only ever sees what the call stored.
 */
@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse_UnitTest {

	private static final String JWKS = "{\"keys\":[{\"kty\":\"EC\",\"crv\":\"P-256\",\"kid\":\"k\","
		+ "\"x\":\"b28d4MwZMjw8-00CG4xfnn9SLMVMM19SlqZpVb_uNtQ\",\"y\":\"Xv5zWwuoaTgdS6hV43yI6gBwTnjukmFQQnJ_kCxzqk8\"}]}";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CallJwtVcIssuerMetadataJwksUri call;

	private ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("issuer.example.com")
				.get("/jwks")
				.willReturn(success(JWKS, "application/jwk-set+json"))
				.get("/array")
				.willReturn(success("[]", "application/json"))
				.get("/missing")
				.willReturn(notFound())));
		hoverfly.resetJournal();

		call = new CallJwtVcIssuerMetadataJwksUri();
		call.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		cond = new ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_extractsJwks(Hoverfly hoverfly) {
		env.putString("jwt_vc_issuer_metadata_jwks_uri", "https://issuer.example.com/jwks");

		call.execute(env);
		cond.execute(env);

		hoverfly.verify(service("issuer.example.com").get("/jwks"));
		JsonObject jwks = env.getObject("jwt_vc_issuer_metadata_jwks");
		assertThat(jwks).isEqualTo(JsonParser.parseString(JWKS));
	}

	@Test
	public void testEvaluate_ignoresNotFound() {
		env.putString("jwt_vc_issuer_metadata_jwks_uri", "https://issuer.example.com/missing");

		call.execute(env);
		assertThat(env.getInteger(CallJwtVcIssuerMetadataJwksUri.RESPONSE_KEY, "status")).isEqualTo(404);
		cond.execute(env);

		assertFalse(env.containsObject("jwt_vc_issuer_metadata_jwks"));
	}

	@Test
	public void testEvaluate_rejectsBodyThatIsNotAJsonObject() {
		env.putString("jwt_vc_issuer_metadata_jwks_uri", "https://issuer.example.com/array");

		call.execute(env);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("not a JSON object");
	}
}
