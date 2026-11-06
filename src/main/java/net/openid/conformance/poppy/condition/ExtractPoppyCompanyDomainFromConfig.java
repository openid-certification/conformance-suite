package net.openid.conformance.poppy.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class ExtractPoppyCompanyDomainFromConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "poppy_domain")
	public Environment evaluate(Environment env) {
		String configured = env.getString("config", "poppy.company_domain");
		if (Strings.isNullOrEmpty(configured) || configured.isBlank()) {
			throw error("'Company domain' field is missing from the 'Poppy' section in the test configuration");
		}
		String value = configured.trim();
		String withScheme = value.contains("://") ? value : "https://" + value;
		URI uri;
		try {
			uri = new URI(withScheme);
		} catch (URISyntaxException e) {
			throw error("'Company domain' in the 'Poppy' section of the test configuration is not a valid host name or URL",
				e, args("company_domain", configured));
		}
		if (!"https".equals(uri.getScheme())) {
			throw error("'Company domain' in the 'Poppy' section of the test configuration must be a host name or an https:// URL; Poppy discovery documents are only served over HTTPS",
				args("company_domain", configured));
		}
		if (uri.getHost() == null) {
			throw error("'Company domain' in the 'Poppy' section of the test configuration does not contain a host name",
				args("company_domain", configured));
		}
		if (uri.getPort() != -1) {
			throw error("'Company domain' in the 'Poppy' section of the test configuration includes a port, but Poppy requires the Company's well-known documents on the default HTTPS port",
				args("company_domain", configured));
		}
		String path = uri.getRawPath();
		if (uri.getRawQuery() != null || uri.getRawFragment() != null || (path != null && !path.isEmpty() && !"/".equals(path))) {
			throw error("'Company domain' in the 'Poppy' section of the test configuration must be only a host name; remove any path, query or fragment",
				args("company_domain", configured));
		}
		String domain = uri.getHost().toLowerCase(Locale.ROOT);
		env.putString("poppy_domain", domain);
		logSuccess("Using the Company domain from the test configuration", args("poppy_domain", domain));
		return env;
	}
}
