package net.openid.conformance.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class ScopeAddingConditionTests {

	@Test
	public void singleScopeIsAddedIfNonePresent() {

		JsonObject client = new JsonObject();

		Environment environment = new Environment();
		environment.putObject("client", client);

		AbstractScopeAddingCondition condition = new AbstractScopeAddingCondition() {

			@Override
			protected String newScope() {
				return "foo";
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		JsonElement scopeElement = client.get("scope");

		assertNotNull(scopeElement);
		assertEquals("foo", OIDFJSON.getString(scopeElement));

	}

	@Test
	public void singleScopeIsAddedToExisting() {

		JsonObject client = new JsonObject();
		client.addProperty("scope", "foo");

		Environment environment = new Environment();
		environment.putObject("client", client);

		AbstractScopeAddingCondition condition = new AbstractScopeAddingCondition() {

			@Override
			protected String newScope() {
				return "bar";
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		JsonElement scopeElement = client.get("scope");

		assertNotNull(scopeElement);
		assertEquals("foo bar", OIDFJSON.getString(scopeElement));

	}

	@Test
	public void multipleScopesAreAdded() {

		JsonObject client = new JsonObject();
		client.addProperty("scope", "foo");

		Environment environment = new Environment();
		environment.putObject("client", client);

		AbstractScopeAddingCondition condition = new AbstractScopeAddingCondition() {

			@Override
			protected String newScope() {
				return "bar";
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		condition = new AbstractScopeAddingCondition() {

			@Override
			protected String newScope() {
				return "baz";
			}

		};
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		JsonElement scopeElement = client.get("scope");

		assertNotNull(scopeElement);
		assertEquals("foo bar baz", OIDFJSON.getString(scopeElement));

	}

}
