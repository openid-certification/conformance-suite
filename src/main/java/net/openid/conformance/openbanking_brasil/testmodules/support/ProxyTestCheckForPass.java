package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class ProxyTestCheckForPass extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger("resource_endpoint_response_full", "status");
		if(status == HttpStatus.CREATED.value()) {
			env.putBoolean("payments_proxy_run_poll", true);
			logSuccess("Status was 201, will begin polling");
		} else {
			env.putBoolean("payments_proxy_run_poll", false);
			logSuccess("422 response status, poll will not run");
		}
		return env;
	}
}
