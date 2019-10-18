package net.openid.conformance.variant;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;

@Component
public class VariantService {

	private static final String SEARCH_PACKAGE = "net.openid";

	private final Map<Class<?>, ParameterHolder<? extends Enum<?>>> variantParametersByClass;
	private final Map<String, ParameterHolder<? extends Enum<?>>> variantParametersByName;
	private final Map<Class<?>, TestModuleHolder> testModulesByClass;
	private final SortedMap<String, TestModuleHolder> testModulesByName;
	private final SortedMap<String, TestPlanHolder> testPlansByName;

	public VariantService() {
		this.variantParametersByClass = inClassesWithAnnotation(VariantParameter.class)
				.collect(toMap(identity(), c -> wrapParameter(c)));

		this.variantParametersByName = variantParametersByClass.values().stream()
				.collect(toMap(p -> p.name, identity()));

		this.testModulesByClass = inClassesWithAnnotation(PublishTestModule.class)
				.collect(toMap(identity(), c -> wrapModule(c)));

		this.testModulesByName = testModulesByClass.values().stream()
				.collect(toSortedMap(m -> m.info.testName(), identity()));

		this.testPlansByName = inClassesWithAnnotation(PublishTestPlan.class)
				.map(c -> wrapPlan(c))
				.collect(toSortedMap(holder -> holder.info.testPlanName(), identity()));
	}

	public TestPlanHolder getTestPlan(String name) {
		return testPlansByName.get(name);
	}

	public Collection<TestPlanHolder> getTestPlans() {
		return testPlansByName.values();
	}

	public TestModuleHolder getTestModule(String name) {
		return testModulesByName.get(name);
	}

	public Collection<TestModuleHolder> getTestModules() {
		return testModulesByName.values();
	}

	private ParameterHolder<? extends Enum<?>> parameter(Class<?> c) {
		ParameterHolder<? extends Enum<?>> p = variantParametersByClass.get(c);
		if (p == null) {
			throw new IllegalArgumentException("Not a variant parameter: " + c.getName());
		}
		return p;
	}

	private ParameterHolder<? extends Enum<?>> parameter(String name) {
		ParameterHolder<? extends Enum<?>> p = variantParametersByName.get(name);
		if (p == null) {
			throw new IllegalArgumentException("Not a variant parameter: " + name);
		}
		return p;
	}

	private Map<ParameterHolder<? extends Enum<?>>, ? extends Enum<?>> typedVariant(VariantSelection variant) {
		return variant.getVariant().entrySet().stream()
				.map(e -> {
					ParameterHolder<?> p = parameter(e.getKey());
					return Map.entry(p, p.valueOf(e.getValue()));
				})
				.collect(toOrderedMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static Stream<Class<?>> inClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
		Stream.Builder<Class<?>> builder = Stream.builder();
		try {
			for (BeanDefinition bd : scanner.findCandidateComponents(SEARCH_PACKAGE)) {
				builder.accept(Class.forName(bd.getBeanClassName()));
			}
		} catch (ClassNotFoundException e) {
			// Not expected to happen
			throw new RuntimeException("Error loading class", e);
		}
		return builder.build();
	}

	private static <A extends Annotation> Stream<A> inCombinedAnnotations(
			Class<? extends TestModule> testClass,
			Class<A> annotationClass) {

		// Walk the class hierarchy and collect annotations - we do this because
		// combining @Repeatable with @Inherited doesn't give all annotations (in general).

		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		for (Class<?> c = testClass; TestModule.class.isAssignableFrom(c); c = c.getSuperclass()) {
			classes.addFirst(c);
		}

		return classes.stream()
				.flatMap(c -> Arrays.stream(c.getDeclaredAnnotationsByType(annotationClass)));
	}

	private static <T, K, U> Collector<T, ?, LinkedHashMap<K, U>> toOrderedMap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {

		return Collector.of(LinkedHashMap::new,
				(m, t) -> m.put(keyMapper.apply(t), valueMapper.apply(t)),
				(m, r) -> { m.putAll(r); return m; });
	}

	private static <T, K, U> Collector<T, ?, SortedMap<K, U>> toSortedMap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {

		return Collector.of(TreeMap::new,
				(m, t) -> m.put(keyMapper.apply(t), valueMapper.apply(t)),
				(m, r) -> { m.putAll(r); return m; });
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ParameterHolder<?> wrapParameter(Class<?> c) {
		if (!c.isEnum()) {
			throw new IllegalArgumentException("Variant parameters must be enums: " + c.getName());
		}
		return new ParameterHolder(c);
	}

	private TestModuleHolder wrapModule(Class<?> c) {
		if (!TestModule.class.isAssignableFrom(c)) {
			throw new RuntimeException("PublishTestModule annotation applied to a class which is not a test module: " + c.getName());
		}
		return new TestModuleHolder(c.asSubclass(TestModule.class));
	}

	private TestPlanHolder wrapPlan(Class<?> c) {
		if (!TestPlan.class.isAssignableFrom(c)) {
			throw new RuntimeException("PublishTestPlan annotation applied to a class which is not a test plan: " + c.getName());
		}
		return new TestPlanHolder(c.asSubclass(TestPlan.class));
	}

	public static class ParameterHolder<T extends Enum<T>> {

		public final String name;

		final Class<T> parameterClass;
		final Map<String, T> valuesByString;

		ParameterHolder(Class<T> parameterClass) {
			this.parameterClass = parameterClass;
			this.name = parameterClass.getAnnotation(VariantParameter.class).value();
			this.valuesByString = values().stream().collect(toMap(T::toString, identity()));
		}

		// We compare against the toString() value of each constant, so that variant values can include spaces etc.
		T valueOf(String s) {
			T v = valuesByString.get(s);
			if (v == null) {
				throw new IllegalArgumentException(String.format("Illegal value for variant parameter %s: \"%s\"", name, s));
			}
			return v;
		}

		List<T> values() {
			return List.of(parameterClass.getEnumConstants());
		}

	}

	public class TestPlanHolder {

		public final PublishTestPlan info;

		final List<TestModuleHolder> modules;
		final Class<? extends TestPlan> planClass;

		TestPlanHolder(Class<? extends TestPlan> planClass) {
			this.planClass = planClass;
			this.info = planClass.getDeclaredAnnotation(PublishTestPlan.class);
			this.modules = Arrays.stream(info.testModules())
					.map(c -> {
						TestModuleHolder m = testModulesByClass.get(c);
						if (m == null) {
							throw new RuntimeException(String.format("In annotation for %s: not a published test module: %s",
									planClass.getSimpleName(),
									c.getName()));
						}
						return m;
					})
					.collect(toList());
		}

		public List<String> getTestModules() {
			return modules.stream().map(m -> m.info.testName()).collect(toList());
		}

		public List<String> getTestModulesForVariant(VariantSelection variant) {
			Map<ParameterHolder<? extends Enum<?>>, ? extends Enum<?>> v = typedVariant(variant);
			return modules.stream()
					.filter(m -> m.isApplicableForVariant(v))
					.map(m -> m.info.testName())
					.collect(toList());
		}

		public Object getVariantSummary() {
			Map<ParameterHolder<?>, Set<String>> values = modules.stream()
					.flatMap(m -> m.parameters.stream())
					.collect(groupingBy(p -> p.parameter,
							flatMapping(p -> p.allowedValues.stream(),
									mapping(v -> v.toString(),
											toSet()))));

			Map<ParameterHolder<?>, Map<String, Set<String>>> fields = modules.stream()
					.flatMap(m -> m.parameters.stream())
					.collect(groupingBy(p -> p.parameter,
							flatMapping(p -> p.configurationFields.entrySet().stream(),
									groupingBy(e -> e.getKey().toString(),
											flatMapping(e -> e.getValue().stream(),
													mapping(v -> v.toString(),
															toSet()))))));

			return values.entrySet().stream()
					.collect(toMap(e -> e.getKey().name,
							e -> {
								ParameterHolder<?> p = e.getKey();
								Set<String> allowed = e.getValue();
								Map<String, Set<String>> pf = fields.getOrDefault(e.getKey(), Map.of());
								return p.values().stream()
										.map(v -> v.toString())
										.filter(v -> allowed.contains(v))
										.collect(toOrderedMap(identity(),
												v -> Map.of("configurationFields", pf.getOrDefault(v, Set.of()))));
							}));
		}

	}

	public class TestModuleHolder {

		public final PublishTestModule info;

		final Class<? extends TestModule> moduleClass;
		final Set<TestModuleVariantInfo<? extends Enum<?>>> parameters;

		TestModuleHolder(Class<? extends TestModule> moduleClass) {
			this.moduleClass = moduleClass;
			this.info = moduleClass.getDeclaredAnnotation(PublishTestModule.class);

			Set<ParameterHolder<?>> declaredParameters = inCombinedAnnotations(moduleClass, VariantParameters.class)
					.flatMap(a -> Arrays.stream(a.value()))
					.map(c -> parameter(c))
					.collect(toSet());

			Map<Class<?>, ParameterHolder<?>> declaredParametersByClass = declaredParameters.stream()
					.collect(toMap(c -> c.parameterClass, identity()));

			Function<Class<?>, ParameterHolder<?>> moduleParameter = c -> {
				ParameterHolder<?> p = declaredParametersByClass.get(c);
				if (p == null) {
					throw new IllegalArgumentException(String.format("In annotation for %s: not a declared variant parameter: %s",
							moduleClass.getSimpleName(),
							c.getName()));
				}
				return p;
			};

			Map<ParameterHolder<?>, Set<String>> allValuesNotApplicable =
					inCombinedAnnotations(moduleClass, VariantNotApplicable.class)
					.collect(groupingBy(a -> moduleParameter.apply(a.parameter()),
							flatMapping(a -> Arrays.stream(a.values()), toSet())));

			Map<ParameterHolder<?>, Map<String, List<String>>> allConfigurationFields =
					inCombinedAnnotations(moduleClass, VariantConfigurationFields.class)
					.collect(groupingBy(a -> moduleParameter.apply(a.parameter()),
							groupingBy(VariantConfigurationFields::value,
									flatMapping(a -> Arrays.stream(a.configurationFields()),
											toList()))));

			Map<ParameterHolder<?>, Map<String, List<Method>>> allSetupMethods =
					Arrays.stream(moduleClass.getMethods())
					.filter(m -> m.isAnnotationPresent(VariantSetup.class))
					.map(m -> Map.entry(m.getAnnotation(VariantSetup.class), m))
					.collect(groupingBy(e -> moduleParameter.apply(e.getKey().parameter()),
							groupingBy(e -> e.getKey().value(),
									mapping(e -> e.getValue(), toList()))));

			this.parameters = declaredParameters.stream()
					.map(p -> new TestModuleVariantInfo<>(
							p,
							allValuesNotApplicable.getOrDefault(p, Set.of()),
							allConfigurationFields.getOrDefault(p, Map.of()),
							allSetupMethods.getOrDefault(p, Map.of())))
					.collect(toSet());
		}

		public boolean isApplicableForVariant(VariantSelection variant) {
			return isApplicableForVariant(typedVariant(variant));
		}

		boolean isApplicableForVariant(Map<ParameterHolder<? extends Enum<?>>, ? extends Enum<?>> variant) {
			return parameters.stream()
					.allMatch(p -> {
						Object v = variant.get(p.parameter);
						return v != null && p.allowedValues.contains(v);
					});
		}

		public Object getVariantSummary() {
			return parameters.stream()
					.collect(toMap(p -> p.parameter.name,
							p -> p.allowedValues.stream()
									.collect(toOrderedMap(v -> v.toString(),
											v -> Map.of("configurationFields", p.configurationFields.getOrDefault(v, List.of()))))));
		}

		public TestModule newInstance(VariantSelection variant) {
			Map<ParameterHolder<? extends Enum<?>>, ? extends Enum<?>> typedVariant = typedVariant(variant);

			// Validate the supplied parameters

			Set<ParameterHolder<?>> declaredParameters = parameters.stream().map(p -> p.parameter).collect(toSet());

			Set<ParameterHolder<?>> missingParameters = Sets.difference(declaredParameters, typedVariant.keySet());
			if (!missingParameters.isEmpty()) {
				throw new IllegalArgumentException("Missing values for required variant parameters: " +
						missingParameters.stream().map(p -> p.name).collect(joining(", ")));
			}

			// Note: supplying extra variant parameters is not an error

			parameters.forEach(p -> {
				Object v = typedVariant.get(p.parameter);
				if (!p.allowedValues.contains(v)) {
					throw new RuntimeException(String.format("Not an allowed value for variant parameter %s: %s",
							p.parameter.name,
							v));
				}
			});

			// Create the module
			TestModule module;
			try {
				module = moduleClass.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Couldn't create test module", e);
			}

			module.setVariant(typedVariant.entrySet().stream()
					.collect(toMap(e -> e.getKey().parameterClass, e -> e.getValue())));

			// Invoke any setup methods for the configured variant
			try {
				for (TestModuleVariantInfo<?> p : parameters) {
					for (Method setup : p.setupMethods.getOrDefault(typedVariant.get(p.parameter), List.of())) {
						setup.invoke(module);
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Failed to initialize test module: " + info.testName(), e);
			}

			return module;
		}

	}

	class TestModuleVariantInfo<T extends Enum<T>> {

		final ParameterHolder<T> parameter;
		final Set<T> allowedValues;
		final Map<T, List<String>> configurationFields;
		final Map<T, List<Method>> setupMethods;

		TestModuleVariantInfo(
				ParameterHolder<T> parameter,
				Set<String> valuesNotApplicable,
				Map<String, List<String>> configurationFields,
				Map<String, List<Method>> setupMethods) {

			this.parameter = parameter;

			this.allowedValues = EnumSet.allOf(parameter.parameterClass);
			valuesNotApplicable.forEach(s -> this.allowedValues.remove(parameter.valueOf(s)));

			this.configurationFields = configurationFields.entrySet().stream()
					.collect(toMap(e -> parameter.valueOf(e.getKey()), e -> e.getValue()));

			this.setupMethods = setupMethods.entrySet().stream()
					.collect(toMap(e -> parameter.valueOf(e.getKey()), e -> e.getValue()));

			// Sanity-check the setup methods
			setupMethods.values().stream()
					.flatMap(List::stream)
					.forEach(m -> {
							if (!Modifier.isPublic(m.getModifiers())) {
								throw new RuntimeException("Variant setup methods must be public: " + m);
							}

							if (m.getParameterCount() != 0) {
								throw new RuntimeException("Variant setup methods cannot take parameters: " + m);
							}
					});
		}

	}

}
