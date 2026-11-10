package net.openid.conformance.testmodule.endpoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One endpoint on one channel, as found by {@link EndpointRegistry}.
 *
 * @param channel the channel the endpoint is reachable on
 * @param path the normalized path: without a leading slash on {@link Channel#HTTP} and {@link Channel#MTLS},
 *             starting with {@code /.well-known/} on {@link Channel#WELL_KNOWN}
 * @param prefixMatch whether paths below {@code path} also match (well-known documents)
 * @param methods accepted upper-case HTTP methods; empty accepts any
 * @param consumes media types the endpoint expects in the request body
 * @param target the object the handler is invoked on
 * @param handler the handler method
 */
public record EndpointRoute(Channel channel, String path, boolean prefixMatch, Set<String> methods,
		List<String> consumes, Object target, Method handler) {

	/**
	 * The handler's name for logs and error messages, such as {@code FAPI2AuthorizationServerEndpoints.token}.
	 */
	public String name() {
		return target.getClass().getSimpleName() + "." + handler.getName();
	}

	boolean matchesPath(String normalizedPath) {
		if (prefixMatch) {
			return normalizedPath.equals(path) || normalizedPath.startsWith(path + "/");
		}
		return normalizedPath.equals(path);
	}

	boolean acceptsMethod(String method) {
		return methods.isEmpty() || (method != null && methods.contains(method.toUpperCase(Locale.ROOT)));
	}

	boolean overlaps(EndpointRoute other) {
		if (channel != other.channel || !path.equals(other.path) || prefixMatch != other.prefixMatch) {
			return false;
		}
		if (methods.isEmpty() || other.methods.isEmpty()) {
			return true;
		}
		return methods.stream().anyMatch(other.methods::contains);
	}

	Object invoke(EndpointRequest request) {
		try {
			if (handler.getParameterCount() == 0) {
				return handler.invoke(target);
			}
			return handler.invoke(target, request);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Endpoint handler " + name() + " threw a checked exception", cause);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Endpoint handler " + name() + " is not accessible", e);
		}
	}
}
