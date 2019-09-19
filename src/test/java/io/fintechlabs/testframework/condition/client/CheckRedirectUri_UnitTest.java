package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckRedirectUri_UnitTest {


	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	// Since Mockito doesnt' seem to be able to mock InetAddress.getHostByName, we'll have to use real addresses.
	private static final String goodHttpUri = "http://127.0.0.1/";  // HTTP has to end up being a loopback address.
	private static final String badHttpUri =  "http://this.cant.possibley.be.a.real.thing/";
	private static final String goodHttpsUri = "http://127.0.0.1/";
	private static final String goodCustomUri = "customapp://foo.bar.baz/";
	private static final String badUri = "ht this can't be a URI://foo.bar/";

	private CheckRedirectUri cond;

	@Before
	public void setUp(){
		cond = new CheckRedirectUri();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrors(){
		env.putString("redirect_uri", goodHttpUri);
		cond.execute(env);

		env.putString("redirect_uri", goodHttpsUri);
		cond.execute(env);

		env.putString("redirect_uri", goodCustomUri);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_nonLoopBackHttp(){
		env.putString("redirect_uri", badHttpUri);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badUriFormat(){
		env.putString("redirect_uri", badUri);
		cond.execute(env);
	}
}
