package net.openid.conformance.apis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.CallHttpResource;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static net.openid.conformance.util.JsonMatchers.isString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class CallHttpResourceConditionTests {

	@Mock
	private TestInstanceEventLog eventLog;

	private CallHttpResource condition = new CallHttpResource();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.get("/api/v1/simple")
			.willReturn(success(write(Map.of(
				"message", "Hello"
			)), "application/json")),
		service("good.example.com")
			.post("/api/v1/simple")
			.anyBody()
			.willReturn(success(write(Map.of(
				"message", "Hello"
			)), "application/json")),
		service("good.example.com")
			.patch("/api/v1/simple")
			.anyBody()
			.willReturn(success(write(Map.of(
				"message", "Hello"
			)), "application/json")),
		service("good.example.com")
			.put("/api/v1/simple")
			.anyBody()
			.willReturn(success(write(Map.of(
				"message", "Hello"
			)), "application/json")),
		service("good.example.com")
			.delete("/api/v1/simple")
			.willReturn(success())

		));

	@Test
	public void executesGetByDefault() {

		Environment environment = new Environment();
		environment.putString("resource_url", "https://good.example.com/api/v1/simple");

		condition.evaluate(environment);

		hoverfly.verify(service("good.example.com")
			.get("/api/v1/simple"));

		assertThat(environment.containsObject("resource_endpoint_response"), is(true));
		JsonObject responseObject = environment.getObject("resource_endpoint_response");

		assertThat(responseObject.has("message"), is(true));
		assertThat(responseObject.get("message"), isString("Hello"));

	}

	@Test
	public void executesPost() {

		JsonObject body = new JsonObject();
		body.addProperty("key", "value");
		String entity = new Gson().toJson(body);

		execute("POST", "/api/v1/simple", entity);

		hoverfly.verify(service("good.example.com")
			.post("/api/v1/simple").body(entity));

	}

	@Test
	public void executesPut() {

		JsonObject body = new JsonObject();
		body.addProperty("key", "value");
		String entity = new Gson().toJson(body);

		execute("PUT", "/api/v1/simple", entity);

		hoverfly.verify(service("good.example.com")
			.put("/api/v1/simple").body(entity));

	}

	@Test
	public void executesPatch() {

		JsonObject body = new JsonObject();
		body.addProperty("key", "value");
		String entity = new Gson().toJson(body);

		execute("PATCH", "/api/v1/simple", entity);

		hoverfly.verify(service("good.example.com")
			.patch("/api/v1/simple").body(entity));

	}

	@Test
	public void executesDelete() {


		execute("DELETE", "/api/v1/simple");

		hoverfly.verify(service("good.example.com")
			.delete("/api/v1/simple"));

	}

	@Test
	public void headersAreUsedIfProvided() {

		String bearerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJtZXNzYWdlIjoiV2h5IG9uIGVhcnRoIGRpZCB5b3UgYm90aGVyIGxvb2tpbmcgYXQgdGhpcz8ifQ.TtgEW-yJoHDhJd7W8c7D97B3hw26wEqLRbpfoh2TVAM";
		Environment environment = new Environment();
		environment.putString("resource_url", "https://good.example.com/api/v1/simple");
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + bearerToken);
		environment.putObject("request_headers", condition.mapToJsonObject(headers, false));

		condition.evaluate(environment);

		hoverfly.verify(service("good.example.com")
			.get("/api/v1/simple").header("Authorization", "Bearer " + bearerToken));

	}

	@Test(expected = ConditionError.class)
	public void errorsOnHttpFailure() {

		execute("GET", "/api/v1/notthere");

	}

	void execute(String method, String path) {
		execute(method, path, null);
	}

	void execute(String method, String path, String body) {

		Environment environment = new Environment();
		if(body != null) {
			environment.putString("request_body", body);
		}

		environment.putString("resource_method", method);
		environment.putString("resource_url", "https://good.example.com" + path);

		condition.evaluate(environment);
	}



	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	static String write(Object payload) {
		try {
			return new ObjectMapper().writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new AssertionError("Couldn't write the json object");
		}
	}

}
