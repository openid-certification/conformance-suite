package net.openid.conformance.testmodule.endpoint;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The endpoints declared with {@link Endpoint}, {@link Get}, {@link Post} and {@link WellKnown} on a set
 * of objects. Scanning fails fast on an invalid handler signature or on two handlers for the same
 * channel, path and method, so a misdeclared endpoint shows up when the module is set up rather than
 * as a request answered by the wrong handler.
 */
public final class EndpointRegistry {

	private static final String WELL_KNOWN_PREFIX = "/.well-known/";

	private final List<EndpointRoute> routes;

	private EndpointRegistry(List<EndpointRoute> routes) {
		this.routes = List.copyOf(routes);
	}

	public static EndpointRegistry scan(Object... targets) {
		return scan(Arrays.asList(targets));
	}

	public static EndpointRegistry scan(List<?> targets) {
		List<EndpointRoute> routes = new ArrayList<>();
		for (Object target : targets) {
			for (Method handler : findHandlers(target.getClass())) {
				for (EndpointRoute route : routesFor(target, handler)) {
					for (EndpointRoute existing : routes) {
						if (existing.overlaps(route)) {
							throw new IllegalStateException("Endpoints " + existing.name() + " and " + route.name()
								+ " both handle " + route.methods() + " " + route.path() + " on " + route.channel());
						}
					}
					routes.add(route);
				}
			}
		}
		return new EndpointRegistry(routes);
	}

	public List<EndpointRoute> routes() {
		return routes;
	}

	/**
	 * The route for a request, or why there is none.
	 */
	public Resolution resolve(Channel channel, String path, String method) {
		String normalized = normalizeRequestPath(channel, path);
		List<EndpointRoute> pathMatches = routes.stream()
			.filter(r -> r.channel() == channel && r.matchesPath(normalized))
			// the most specific well-known path wins over a shorter prefix
			.sorted(Comparator.comparingInt((EndpointRoute r) -> r.path().length()).reversed())
			.toList();
		if (pathMatches.isEmpty()) {
			boolean onOtherChannel = routes.stream()
				.anyMatch(r -> r.channel() != channel && r.matchesPath(normalizeRequestPath(r.channel(), path)));
			return new Resolution(null, onOtherChannel ? Unmatched.CHANNEL_NOT_ALLOWED : Unmatched.NO_ROUTE);
		}
		return pathMatches.stream()
			.filter(r -> r.acceptsMethod(method))
			.findFirst()
			.map(r -> new Resolution(r, null))
			.orElseGet(() -> new Resolution(null, Unmatched.METHOD_NOT_ALLOWED));
	}

	/**
	 * Exactly one of the two is set.
	 */
	public record Resolution(EndpointRoute route, Unmatched unmatched) {
	}

	/**
	 * Annotated methods of the class and its superclasses. Where a subclass overrides an annotated method,
	 * the most-derived annotated declaration is kept; {@link Method#invoke} dispatches virtually, so an
	 * override is called even when only the superclass declaration carries the annotation.
	 */
	private static List<Method> findHandlers(Class<?> type) {
		Map<String, Method> handlers = new LinkedHashMap<>();
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Method method : c.getDeclaredMethods()) {
				if (!isAnnotated(method) || method.isBridge() || method.isSynthetic()) {
					continue;
				}
				String key = (Modifier.isPrivate(method.getModifiers()) ? c.getName() + "#" : "")
					+ method.getName() + Arrays.toString(method.getParameterTypes());
				handlers.putIfAbsent(key, method);
			}
		}
		handlers.values().forEach(EndpointRegistry::validateSignature);
		return List.copyOf(handlers.values());
	}

	private static boolean isAnnotated(Method method) {
		return method.isAnnotationPresent(Endpoint.class)
			|| method.isAnnotationPresent(Get.class)
			|| method.isAnnotationPresent(Post.class)
			|| method.isAnnotationPresent(WellKnown.class);
	}

	private static void validateSignature(Method method) {
		String name = method.getDeclaringClass().getSimpleName() + "." + method.getName();
		if (Modifier.isStatic(method.getModifiers())) {
			throw new IllegalStateException("Endpoint handler " + name + " must not be static");
		}
		if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
			throw new IllegalStateException("Endpoint handler " + name + " must be a public method of a public class");
		}
		Class<?>[] parameters = method.getParameterTypes();
		if (parameters.length > 1 || (parameters.length == 1 && parameters[0] != EndpointRequest.class)) {
			throw new IllegalStateException("Endpoint handler " + name
				+ " must take no parameters or a single EndpointRequest");
		}
		if (method.getReturnType() == void.class) {
			throw new IllegalStateException("Endpoint handler " + name + " must return the response");
		}
	}

	private static List<EndpointRoute> routesFor(Object target, Method handler) {
		List<EndpointRoute> routes = new ArrayList<>();
		Endpoint endpoint = handler.getAnnotation(Endpoint.class);
		if (endpoint != null) {
			addRoutes(routes, target, handler, endpoint.path(), endpoint.methods(), endpoint.channels(), endpoint.consumes());
		}
		Get get = handler.getAnnotation(Get.class);
		if (get != null) {
			addRoutes(routes, target, handler, get.value(), new String[] { "GET" }, get.channels(), new String[0]);
		}
		Post post = handler.getAnnotation(Post.class);
		if (post != null) {
			addRoutes(routes, target, handler, post.value(), new String[] { "POST" }, post.channels(), post.consumes());
		}
		WellKnown wellKnown = handler.getAnnotation(WellKnown.class);
		if (wellKnown != null) {
			String suffix = stripSlashes(wellKnown.value());
			if (suffix.isEmpty()) {
				throw new IllegalStateException("Endpoint handler " + handler.getName() + " has an empty @WellKnown suffix");
			}
			routes.add(new EndpointRoute(Channel.WELL_KNOWN, WELL_KNOWN_PREFIX + suffix, true,
				upperCase(wellKnown.methods()), List.of(), target, handler));
		}
		return routes;
	}

	private static void addRoutes(List<EndpointRoute> routes, Object target, Method handler, String path,
			String[] methods, Channel[] channels, String... consumes) {
		if (channels.length == 0) {
			throw new IllegalStateException("Endpoint handler " + handler.getName() + " declares no channels");
		}
		for (Channel channel : channels) {
			if (channel == Channel.WELL_KNOWN) {
				throw new IllegalStateException("Endpoint handler " + handler.getName()
					+ " must use @WellKnown for the well-known channel");
			}
			routes.add(new EndpointRoute(channel, stripLeadingSlash(path), false, upperCase(methods),
				List.of(consumes), target, handler));
		}
	}

	private static String normalizeRequestPath(Channel channel, String path) {
		if (path == null) {
			return "";
		}
		return channel == Channel.WELL_KNOWN ? path : stripLeadingSlash(path);
	}

	private static String stripLeadingSlash(String path) {
		return path.startsWith("/") ? path.substring(1) : path;
	}

	private static String stripSlashes(String value) {
		String stripped = stripLeadingSlash(value);
		return stripped.endsWith("/") ? stripped.substring(0, stripped.length() - 1) : stripped;
	}

	private static Set<String> upperCase(String... methods) {
		Set<String> result = new LinkedHashSet<>();
		for (String method : methods) {
			result.add(method.toUpperCase(Locale.ROOT));
		}
		return Set.copyOf(result);
	}
}
