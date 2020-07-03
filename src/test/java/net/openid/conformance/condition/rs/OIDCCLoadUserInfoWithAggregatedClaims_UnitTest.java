package net.openid.conformance.condition.rs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
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
public class OIDCCLoadUserInfoWithAggregatedClaims_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCLoadUserInfoWithAggregatedClaims cond;

    @Before
    public void setUp() throws Exception {
		cond = new OIDCCLoadUserInfoWithAggregatedClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
    }

    @Test
    public void evaluate() {
    	cond.evaluate(env);
    	JsonObject user = env.getObject("user_info");
		assertNotNull(user);
		JsonElement claimSources = user.get("_claim_sources");
		assertNotNull(claimSources);
		JsonObject claimSourcesObject = claimSources.getAsJsonObject();
		assertTrue(claimSourcesObject.has("src1"));
		assertTrue(claimSourcesObject.get("src1").getAsJsonObject().has("JWT"));
    }
}
