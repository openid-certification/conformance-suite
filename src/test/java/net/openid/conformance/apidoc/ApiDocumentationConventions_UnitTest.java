package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conventions for the springdoc-generated API documentation. No Spring context is started; the
 * controllers are located by classpath scanning and inspected by reflection.
 *
 * <p>Every controller with mapped handler methods must either be {@code @Hidden} (internal
 * endpoints such as the test dispatcher and error/home controllers) or document every handler
 * with an {@code @Operation} carrying a non-empty, globally unique operationId — so a new
 * controller cannot silently ship undocumented.
 */
public class ApiDocumentationConventions_UnitTest {

	private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = List.of(
		RequestMapping.class, GetMapping.class, PostMapping.class,
		PutMapping.class, DeleteMapping.class, PatchMapping.class);

	@Test
	public void everyDocumentedControllerHandlerHasAUniqueOperationId() throws Exception {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

		Map<String, String> operationIdToMethod = new HashMap<>();
		List<String> problems = new ArrayList<>();
		int documentedControllers = 0;

		for (BeanDefinition bd : scanner.findCandidateComponents("net.openid.conformance")) {
			Class<?> controller = Class.forName(bd.getBeanClassName());
			if (controller.isAnnotationPresent(Hidden.class)) {
				continue;
			}

			List<Method> handlers = new ArrayList<>();
			for (Method method : controller.getDeclaredMethods()) {
				if (MAPPING_ANNOTATIONS.stream().anyMatch(method::isAnnotationPresent)
						&& !method.isAnnotationPresent(Hidden.class)) {
					handlers.add(method);
				}
			}
			if (handlers.isEmpty()) {
				continue;
			}
			documentedControllers++;

			for (Method handler : handlers) {
				String where = controller.getSimpleName() + "." + handler.getName();
				Operation operation = handler.getAnnotation(Operation.class);
				if (operation == null) {
					problems.add(where + " has no @Operation (document it, or mark the controller/method @Hidden if internal)");
					continue;
				}
				String operationId = operation.operationId();
				if (operationId.isEmpty()) {
					problems.add(where + " has no operationId (springdoc would derive one, risking _1 suffixes)");
					continue;
				}
				String previous = operationIdToMethod.put(operationId, where);
				if (previous != null) {
					problems.add("duplicate operationId '" + operationId + "' on " + previous + " and " + where);
				}
			}
		}

		assertTrue(documentedControllers >= 12, "expected to find the API controllers, found " + documentedControllers);
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}
}
