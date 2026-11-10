package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
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
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class CallJwtVcIssuerMetadataEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CallJwtVcIssuerMetadataEndpoint cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("issuer.example.com")
				.get("/.well-known/jwt-vc-issuer/tenant")
				.willReturn(success("{\"issuer\":\"https://issuer.example.com/tenant\"}", "application/json")),
			service("unpublished.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(notFound()),
			service("broken.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(serverError())));
		hoverfly.resetJournal();

		cond = new CallJwtVcIssuerMetadataEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_storesResponse(Hoverfly hoverfly) {
		putJwtVcIssuer("https://issuer.example.com/tenant", "https://issuer.example.com/.well-known/jwt-vc-issuer/tenant");

		cond.execute(env);

		hoverfly.verify(service("issuer.example.com").get("/.well-known/jwt-vc-issuer/tenant"));
		assertThat(env.getInteger(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY, "status")).isEqualTo(200);
		assertThat(env.getString(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY, "body_json.issuer"))
			.isEqualTo("https://issuer.example.com/tenant");
	}

	@Test
	public void testEvaluate_storesNoResponseWhenMetadataIsNotPublished() {
		putJwtVcIssuer("https://unpublished.example.com", "https://unpublished.example.com/.well-known/jwt-vc-issuer");

		cond.execute(env);

		assertFalse(env.containsObject(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY));
	}

	@Test
	public void testEvaluate_storesErrorResponseForTheStatusCheck() {
		putJwtVcIssuer("https://broken.example.com", "https://broken.example.com/.well-known/jwt-vc-issuer");

		cond.execute(env);

		assertThat(env.getInteger(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY, "status")).isEqualTo(500);
	}

	private void putJwtVcIssuer(String iss, String metadataUrl) {
		JsonObject jwtVcIssuer = new JsonObject();
		jwtVcIssuer.addProperty("iss", iss);
		jwtVcIssuer.addProperty("iss_source", "status list token");
		jwtVcIssuer.addProperty("metadata_url", metadataUrl);
		env.putObject("status_list_token_jwt_vc_issuer", jwtVcIssuer);
	}
}
