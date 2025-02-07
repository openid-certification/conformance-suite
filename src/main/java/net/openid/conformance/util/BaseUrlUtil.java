package net.openid.conformance.util;

import com.google.common.base.Strings;
import net.openid.conformance.testmodule.Environment;

public class BaseUrlUtil {

	public static String resolveEffectiveBaseUrl(Environment env) {

		String effectiveBaseUrl = env.getString("base_url");
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			effectiveBaseUrl = externalUrlOverride;
		}

		return effectiveBaseUrl;
	}
}
