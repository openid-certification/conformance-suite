package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.concurrent.TimeUnit;

public class OIDSSFWaitForStreamReadiness extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement readyElement = null;

		for (int i = 0; i < 12; i++) {
			readyElement = env.getElementFromObject("ssf", "stream.ready");
			if (readyElement != null) {
				break;
			}

			log("Waiting for stream readiness");
			// waitSeconds(10);

			env.doWithLock(() -> {
				log("Waiting for polling request");
				if (env.awaitLockCondition("pollingRequestProcessed", 10, TimeUnit.SECONDS)) {
					log("Detected polling request");
				} else {
					log("Woke up after timeout waiting for polling request");
				}
			});
		}

		if (readyElement == null) {
			throw error("Stream failed to become ready.");
		}

		logSuccess("Stream is ready");
		return env;
	}
}
