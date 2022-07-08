package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class SetSpecifiedUrlParameterToSpecifiedValue extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"extracted_link", "value", "parameter"})
	@PostEnvironment(strings = "extracted_link")
	public Environment evaluate(Environment env) {
		String link  = env.getString("extracted_link");
		String value  = env.getString("value");
		String parameter  = env.getString("parameter");
		try {
			link = UriComponentsBuilder.fromUri(new URI(link)).replaceQueryParam(parameter, value).build().toString();
			env.putString("extracted_link", link);
			logSuccess("Added / updated parameter and value",
				Map.of("Value", value, "Parameter", parameter, "Link", link));
		} catch (URISyntaxException e) {
			throw error("Specified link is incorrect", Map.of("Link", link));
		}
		return env;
	}
}
