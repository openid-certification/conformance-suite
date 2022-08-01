package net.openid.conformance.apis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.JsonLoadingJUnitRunner;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.http.HttpHeaders;

import java.text.ParseException;
import java.util.List;

import static net.openid.conformance.util.JsonUtils.configureJsonPathForGson;
import static org.mockito.Mockito.mock;

@RunWith(JsonLoadingJUnitRunner.class)
public abstract class AbstractJsonResponseConditionUnitTest implements DataUtils {

	protected JsonObject jsonObject;
	protected Environment environment = new Environment();
	private HttpHeaders responseHeaders = new HttpHeaders();

	private boolean isJwt = false;

	public AbstractJsonResponseConditionUnitTest() {
		environment.putObject("resource_endpoint_response_full", new JsonObject());
		environment.putObject("consent_endpoint_response_full", new JsonObject());
	}

	protected void setHeaders(String headerName, String...values) {
		responseHeaders.addAll(headerName, List.of(values));
	}

	public void setJwt(boolean jwt) {
		isJwt = jwt;
	}

	protected void setStatus(int status) {
		JsonObject responseCode = new JsonObject();
		responseCode.addProperty("code", status);
		environment.putObject("resource_endpoint_response_code", responseCode);
		environment.getObject("resource_endpoint_response_full").addProperty("status", status);
		environment.getObject("consent_endpoint_response_full").addProperty("status", status);
	}

	protected void run(final AbstractCondition condition) {
		enrichCondition(condition);
		try {
			condition.execute(environment);
		}  catch(ConditionError error) {
			throw new AssertionError("Condition failed", error);
		}
	}

	protected ConditionError runAndFail(final AbstractCondition condition) {
		enrichCondition(condition);
		try {
			condition.execute(environment);
		} catch(ConditionError error) {
			return error;
		}
		throw new AssertionError("The condition passed but we expected it to fail");
	}

	private void enrichCondition(AbstractCondition condition) {
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		Gson gson = new GsonBuilder()
			.serializeNulls()
			.create();
		String bodyString = gson.toJson(ifJsonWrapped(jsonObject));

		if(isJwt){
			try {
				JWTClaimsSet claimSet = JWTClaimsSet.parse(bodyString);
				bodyString = new PlainJWT(claimSet).serialize();
			} catch (ParseException e) {
				throw new AssertionError("Could not parse JSON to JWT Claim Set");
			}
		}
		environment.putString("resource_endpoint_response", bodyString);
		environment.putObject("consent_endpoint_response", jsonObject);
		environment.putString("resource_endpoint_response_full", "body", bodyString);
		environment.putString("consent_endpoint_response_full", "body", bodyString);

		JsonObject headersObject = mapToJsonObject(responseHeaders, false);
		environment.putObject("resource_endpoint_response_headers", headersObject);
		environment.putObject("resource_endpoint_response_full", "headers", headersObject);
		environment.putObject("consent_endpoint_response_full", "headers", headersObject);
	}

	private JsonElement ifJsonWrapped(JsonObject jsonObject) {
		if (jsonObject.has(JsonLoadingJUnitRunner.WRAPPED)) {
			return jsonObject.get(JsonLoadingJUnitRunner.WRAPPED);
		}
		return jsonObject;
	}

	@BeforeClass
	public static void setup() {
		configureJsonPathForGson();
	}
}
