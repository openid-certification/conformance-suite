package net.openid.conformance.apis;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonLoadingJUnitRunner;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static net.openid.conformance.util.JsonPathUtil.configureJsonPathForGson;
import static org.mockito.Mockito.mock;

@RunWith(JsonLoadingJUnitRunner.class)
public abstract class AbstractJsonResponseConditionUnitTest implements DataUtils {

	protected JsonObject jsonObject;
	protected Environment environment = new Environment();
	private HttpHeaders responseHeaders = new HttpHeaders();

	protected void setHeaders(String headerName, String...values) {
		responseHeaders.addAll(headerName, List.of(values));
	}

	protected void setStatus(int status) {
		JsonObject responseCode = new JsonObject();
		responseCode.addProperty("code", status);
		environment.putObject("resource_endpoint_response_code", responseCode);
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
		environment.putObject("resource_endpoint_response", jsonObject);
		environment.putObject("resource_endpoint_response_headers", mapToJsonObject(responseHeaders, false));
	}

	@BeforeClass
	public static void setup() {
		configureJsonPathForGson();
	}

}
