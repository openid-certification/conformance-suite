package net.openid.conformance.security;

import java.beans.Expression;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.util.UrlPathHelper;

public class AdditiveUrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private final Map<String, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	@Nullable
	private String lookupPathAttributeName;

	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	public void setLookupPathAttributeName(@Nullable String lookupPathAttributeName) {
		this.lookupPathAttributeName = lookupPathAttributeName;
	}

	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
		throw new RuntimeException("setCorsConfigurations is not allowed, use registerCorsConfiguration instead");
	}

	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return Collections.unmodifiableMap(this.corsConfigurations);
	}

	public void registerCorsConfiguration(String path, CorsConfiguration config) {
		this.corsConfigurations.put(path, config);
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		// The method for obtaining 'lookupPath' differs between Spring Framework versions 5.2 and 5.3.
		// To allow for this, Expressions are used. At such time that we move to Spring Boot 2.4.x the
		// Expressions can be removed and 'resolveAndCacheLookupPath' used directly.

		Class<?> c = this.urlPathHelper.getClass();
		ArrayList<String> methodNames = new ArrayList<>();

		for (Method m : c.getDeclaredMethods()) {
			if (! methodNames.contains(m.getName())) {
				methodNames.add(m.getName());
			}
		}

		String lookupPath = "";

		if (methodNames.contains("resolveAndCacheLookupPath")) {
			// Spring Boot 2.4.x (Spring Framework 5.3)
			Expression expr = new Expression(this.urlPathHelper, "resolveAndCacheLookupPath", new Object[] {request});

			try {
				expr.execute();
				lookupPath = (String)expr.getValue();
			}
			catch (Exception x) {
			}
		}
		else if (methodNames.contains("getLookupPathForRequest")) {
			// Spring Boot 2.3.x (Spring Framework 5.2)
			Expression expr = new Expression(this.urlPathHelper, "getLookupPathForRequest", new Object[] {request, this.lookupPathAttributeName});

			try {
				expr.execute();
				lookupPath = (String)expr.getValue();
			}
			catch (Exception x) {
			}
		}

		for (Map.Entry<String, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (this.pathMatcher.match(entry.getKey(), lookupPath)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
