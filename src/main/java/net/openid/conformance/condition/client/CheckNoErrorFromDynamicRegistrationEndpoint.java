package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckNoErrorFromDynamicRegistrationEndpoint extends AbstractCondition {

    @Override
    @PreEnvironment(required = "dynamic_registration_endpoint_response")
    public Environment evaluate(Environment env) {

        JsonElement error = env.getElementFromObject("dynamic_registration_endpoint_response", "body_json.error");

        if (error != null) {
            throw error("'error' field found in response from dynamic registration endpoint.",
				args("error", error));
        }

        logSuccess("Dynamic registration endpoint did not return an error.");

        return env;
    }
}
