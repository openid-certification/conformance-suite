package net.openid.conformance.variant;

import com.google.common.collect.Sets;
import net.openid.conformance.info.Plan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Component
public class VariantService {
	private static final String SEARCH_PACKAGE = "net.openid";

	private final Map<Class<?>, ParameterHolder<? extends Enum<?>>> variantParametersByClass;
	private final Map<Class<?>, TestModuleHolder> testModulesByClass;
	private final SortedMap<String, TestModuleHolder> testModulesByName;
	private final SortedMap<String, TestPlanHolder> testPlansByName;

	@Autowired
	public VariantService(@Value("${fintechlabs.profiles.visible:#{null}}") List<String> profilesToSurface) {
		this(testPlanHolder -> profilesToSurface == null || profilesToSurface.isEmpty() || profilesToSurface.contains(testPlanHolder.info.profile()));
	}

	public VariantService(Predicate<? super TestPlanHolder> byProfile) {

		this.variantParametersByClass = inClassesWithAnnotation(VariantParameter.class)
				.collect(toMap(identity(), c -> wrapParameter(c)));

		this.testModulesByClass = inClassesWithAnnotation(PublishTestModule.class)
				.collect(toMap(identity(), c -> wrapModule(c)));

		this.testModulesByName = testModulesByClass.values().stream()
				.collect(toSortedMap("test module", m -> m.info.testName(), identity()));

		this.testPlansByName = inClassesWithAnnotation(PublishTestPlan.class)
				.map(c -> wrapPlan(c))
				.filter(byProfile)
				.collect(toSortedMap("test plan", holder -> holder.info.testPlanName(), identity()));
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

	private static Map<ParameterHolder<? extends Enum<?>>, Enum<?>> typedVariant(VariantSelection variant,
			Map<String, ParameterHolder<? extends Enum<?>>> variantParametersByName) {
		// Ignore any unknown parameters
		return variant.getVariant().entrySet().stream()
				.filter(e -> variantParametersByName.containsKey(e.getKey()))
				.map(e -> {
					ParameterHolder<?> p = variantParametersByName.get(e.getKey());
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

	@SuppressWarnings("JdkObsolete") // errorprone doesn't like 'LinkedList' but ArrayList doesn't have an 'addFirst'
	private static <A extends Annotation> Stream<A> inCombinedAnnotations(
			Class<? extends TestModule> testClass,
			Class<A> annotationClass) {

		// Walk the class hierarchy and collect annotations - we do this because
		// combining @Repeatable with @Inherited doesn't give all annotations (in general).

		LinkedList<Class<?>> classes = new LinkedList<>();
		for (Class<?> c = testClass; TestModule.class.isAssignableFrom(c); c = c.getSuperclass()) {
			classes.addFirst(c);
		}

		return classes.stream()
				.flatMap(c -> Arrays.stream(c.getDeclaredAnnotationsByType(annotationClass)));
	}

	@SuppressWarnings("NonApiType")
	private static <T, K, U> Collector<T, ?, LinkedHashMap<K, U>> toOrderedMap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {

		return Collector.of(LinkedHashMap::new,
				(m, t) -> m.put(keyMapper.apply(t), valueMapper.apply(t)),
				(m, r) -> { m.putAll(r); return m; });
	}

	private static <T extends ParameterHolder<? extends Enum<?>>> Collector<T, ?, T> toSingleParameter() {
		return collectingAndThen(
				reducing((p1, p2) -> {
					if (p1 == p2) {
						return p1;
					} else {
						throw new RuntimeException("Variant parameter declaration includes multiple parameters with name: " + p1.variantParameter.name());
					}
				}),
				p -> p.get());
	}

	private static <T, K, U> Collector<T, ?, SortedMap<K, U>> toSortedMap(
			String itemType,
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {

		return Collector.of(TreeMap::new,
			(m, t) -> {
				K key = keyMapper.apply(t);
				U value = valueMapper.apply(t);
				if (m.containsKey(key)) {
					throw new RuntimeException("More than one %s with the name '%s'".formatted(itemType, key));
				}
				m.put(key, value);
			},
			(m, r) -> {
				for (K t : r.keySet()) {
					if (m.containsKey(t)) {
						throw new RuntimeException("More than one %s with the name '%s'".formatted(itemType, t));
					}
				}
				m.putAll(r);
				return m;
			});
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

		public final VariantParameter variantParameter;

		final Class<T> parameterClass;
		final Map<String, T> valuesByString;

		ParameterHolder(Class<T> parameterClass) {
			this.parameterClass = parameterClass;
			this.variantParameter = parameterClass.getAnnotation(VariantParameter.class);
			this.valuesByString = values().stream().collect(toMap(T::toString, identity()));
			String defaultValue = parameterClass.getAnnotation(VariantParameter.class).defaultValue();
			if (!defaultValue.equals("")) {
				this.valuesByString.put("default", this.valuesByString.get(defaultValue));
			}
		}

		// We compare against the toString() value of each constant, so that variant values can include spaces etc.
		T valueOf(String s) {
			T v = valuesByString.get(s);
			if (v == null) {
				if (valuesByString.containsKey("default")){
					return valuesByString.get("default");
				}
				throw new IllegalArgumentException("Illegal value for variant parameter %s: \"%s\"".formatted(variantParameter.name(), s));
			}
			return v;
		}

		List<T> values() {
			return List.of(parameterClass.getEnumConstants());
		}

		public boolean hasDefault() {
			return valuesByString.containsKey("default");
		}

		public T defaultValue() {
			return valuesByString.get("default");
		}
	}

	/**
	 * Wraps a test module with the variants specific to it in a particular test plan
	 */
	public class TestPlanModuleWithVariant {
		final Class<? extends TestPlan> planClass;
		final TestModuleHolder module;
		// the variants that are defined statically by the plan; i.e. those the user can't select
		// null if no variants are pre-defined
		private final Map<Class<? extends Enum<?>>, ? extends Enum<?>> variant;
		/** configuration fields for any test modules with fixed variants */
		final List<String> fixedVariantConfigurationFields;
		/** "hide" configuration fields for any test modules with fixed variants */
		final List<String> fixedVariantHidesConfigurationFields;

		TestPlanModuleWithVariant(Class<? extends TestPlan> planClass, TestModuleHolder module, Map<Class<? extends Enum<?>>, ? extends Enum<?>> variant) {
			this.planClass = planClass;
			this.module = module;
			this.variant = variant;

			List<String> configurationFields = new ArrayList<>();
			List<String> hidesConfigurationFields = new ArrayList<>();
			// check the test module supports all the variants specified for it in the test plan
			if (variant != null) {
				this.variant.forEach((variantName, variantValue) -> {
					if (!this.module.declaredParametersByClass.containsKey(variantName)) {
						throw new RuntimeException("Test plan '" + this.planClass.getSimpleName() + "' module '" + this.module.moduleClass.getSimpleName() + "' does not have the variant '" + variantName.getSimpleName() + "' but the test plan set a value for this variant");
					}
				});

				this.module.parameters.forEach((param) -> {
					// is the variant's value supported (not marked as NotApplicable) by test module
					final Class<? extends Enum<?>> variantClass = param.parameter.parameterClass;
					if (this.variant.containsKey(variantClass)) {
						final Enum<?> value = this.variant.get(variantClass);
						if (!param.allowedValues.contains(value)) {
							throw new RuntimeException("Test plan '" + this.planClass.getSimpleName() + "' module '" + this.module.moduleClass.getSimpleName() + "' requests variant '" + variantClass.getSimpleName() + "' for a value ('" + value + "') test module has an @VariantNotApplicable for");

						}
						final List<String> hiddenFields = param.hidesConfigurationFields.get(value);
						if (hiddenFields != null) {
							// this is oversimplified but works for our current test plans; I believe the correct logic
							// would be that a value should only be hidden if all the test modules that list this
							// configuration field hide it for their fixed variants - see comment in
							// AbstractOIDCCServerTest's VariantHidesConfigurationFields for ResponseType="id_token"
							hidesConfigurationFields.addAll(hiddenFields);
						}
						final List<String> fields = param.configurationFields.get(value);
						if (fields != null) {
							configurationFields.addAll(fields);
						}
					}
				});
			}
			this.fixedVariantConfigurationFields = configurationFields;
			this.fixedVariantHidesConfigurationFields = hidesConfigurationFields;
		}

		/**
		 * @return map of variants set by the plan definition and the value they are set to
		 */
		Map<String,String> variantAsStrings() {
			Map<String,String> stringMap = new HashMap<>();
			if (variant == null) {
				return null;
			}
			variant.forEach((key, value) -> {
				ParameterHolder<?> p = parameter(key);
				stringMap.put(p.variantParameter.name(), value.toString());
			});

			return stringMap;
		}
	}

	/**
	 * Holder for all the statically known information about a test module class
	 *
	 * Caches all information when instantiated.
	 */
	public class TestPlanHolder {

		public final PublishTestPlan info;

		final List<TestPlanModuleWithVariant> modulesWithVariant;

		final Class<? extends TestPlan> planClass;
		final Map<String, ParameterHolder<? extends Enum<?>>> parametersByName;

		private List<TestPlanModuleWithVariant> convertModuleListEntry(String testPlanName, List<TestPlan.ModuleListEntry> list) {
			return list.stream().flatMap(moduleListEntry -> {
				Map<Class<? extends Enum<?>>, ? extends Enum<?>> variants = moduleListEntry.variant.stream()
					.map(variant -> {
						ParameterHolder<?> p = parameter(variant.key); // used to convert specific enum val into a wildcard one
						return Map.entry(variant.key, p.valueOf(variant.value));
					})
					.collect(toOrderedMap(Map.Entry::getKey, Map.Entry::getValue));

				// moduleListEntry may have multiple modules for each variant set, we're collapsing it into a flat list of one module & applicable variants
				return moduleListEntry.testModules.stream().map(testModuleClass -> {
					TestModuleHolder testModuleHolder = testModulesByClass.get(testModuleClass);
					if (testModuleHolder == null) {
						throw new RuntimeException("Processing testModulesWithVariants for %s: not a published test module: %s".formatted(
							testPlanName,
							testModuleClass.getName()));
					}

					return new TestPlanModuleWithVariant(planClass, testModuleHolder, variants);
				});

			}).collect(toList());
		}

		@SuppressWarnings({ "unchecked" })
		TestPlanHolder(Class<? extends TestPlan> planClass) {
			this.planClass = planClass;
			this.info = planClass.getDeclaredAnnotation(PublishTestPlan.class);

			List<TestPlan.ModuleListEntry> list = null;
			try {
				// Test plans can implement a static method to list modules with variants to run them with; as
				// java doesn't allow interfaces to define static methods (unless they define the implementation too)
				// we have to call this via reflection:
				Method m = planClass.getDeclaredMethod("testModulesWithVariants");
				Object untypedList = m.invoke(null);
				list = (List<TestPlan.ModuleListEntry>) untypedList;
			} catch (NoSuchMethodException e) {
				// class doesn't implement this; below we'll read the modules from the annotation instead
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Reflection issue calling testModulesWithVariants() for "+planClass.getSimpleName(), e);
			}

			if (list != null) {
				// module list is defined by the result of the testModulesWithVariants() method
				this.modulesWithVariant = convertModuleListEntry(planClass.getSimpleName(), list);
			} else {
				// module list comes from annotation
				this.modulesWithVariant = Arrays.stream(info.testModules())
					.map(c -> {
						TestModuleHolder m = testModulesByClass.get(c);
						if (m == null) {
							throw new RuntimeException("In annotation for %s: not a published test module: %s".formatted(
								planClass.getSimpleName(),
								c.getName()));
						}
						return new TestPlanModuleWithVariant(planClass, m, null);
					})
					.collect(toList());
			}
			this.parametersByName = modulesWithVariant.stream()
					.map(p->p.module)
					.flatMap(m -> m.parameters.stream())
					.map(p -> p.parameter)
					.collect(groupingBy(p -> p.variantParameter.name(), toSingleParameter()));
		}

		public List<Plan.Module> getTestModules() {
			List<Plan.Module> testModules = new ArrayList<>();
			modulesWithVariant.forEach((testPlanModuleWithVariant) -> {
				testModules.add(new Plan.Module(testPlanModuleWithVariant.module.info.testName(), testPlanModuleWithVariant.variantAsStrings()));
			});
			return testModules;
		}

		public List<String> configurationFields() {
			Set<String> fields = modulesWithVariant.stream()
				.flatMap(testPlanModuleWithVariant -> testPlanModuleWithVariant.fixedVariantConfigurationFields.stream())
				.collect(toSet());
			fields.addAll(Arrays.asList(info.configurationFields()));
			return new ArrayList<>(fields);
		}

		public List<String> hidesConfigurationFields() {
			Set<String> fields = modulesWithVariant.stream()
				.flatMap(testPlanModuleWithVariant -> testPlanModuleWithVariant.fixedVariantHidesConfigurationFields.stream())
				.collect(toSet());
			return new ArrayList<>(fields);
		}

		public String certificationProfileForVariant(VariantSelection variantSelection) {
			String certProfile = null;

			try {
				// Test plans can implement a static method to list modules with variants to run them with; as
				// java doesn't allow interfaces to define static methods (unless they define the implementation too)
				// we have to call this via reflection:
				Method m = planClass.getDeclaredMethod("certificationProfileName", VariantSelection.class);
				Object result = m.invoke(null, variantSelection);
				certProfile = (String) result;
			} catch (NoSuchMethodException e) {
				// class doesn't implement this so doesn't have any certification profiles
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Reflection issue calling certificationProfileName() for "+planClass.getSimpleName(), e);
			} catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if (target instanceof RuntimeException exception) {
					throw exception;
				}
				throw new RuntimeException("Reflection issue calling certificationProfileName() for "+planClass.getSimpleName(), e);
			}

			return certProfile;
		}

		public List<Plan.Module> getTestModulesForVariant(VariantSelection userSelectedVariant) {
			List<Plan.Module> testModules = new ArrayList<>();
			modulesWithVariant.forEach((testPlanModuleWithVariant) -> {
				// merge user's variant selection with pre-selected variants
				Map<String, String> preselectedVariants = testPlanModuleWithVariant.variantAsStrings();
				Map<String, String> selectedStringVariants = new HashMap<>(userSelectedVariant.getVariant());
				if (preselectedVariants != null) {
					preselectedVariants.forEach((variantName, value) -> {
						if (selectedStringVariants.containsKey(variantName)) {
							// there may be future cases where this is valid, if we want to allow the user to generally select a variant but ensure a specific one is used for a particular test
							throw new RuntimeException("Variant '%s' has been set by user, but test plan already sets this variant for module '%s'".formatted(
								variantName, testPlanModuleWithVariant.module.info.testName()));
						}
					});
					selectedStringVariants.putAll(preselectedVariants);
				}

				Map<ParameterHolder<? extends Enum<?>>, Enum<?>> selectedVariant = typedVariant(new VariantSelection(selectedStringVariants), parametersByName);
				if (!testPlanModuleWithVariant.module.isApplicableForVariant(selectedVariant)) {
					return;
				}

				testModules.add(new Plan.Module(testPlanModuleWithVariant.module.info.testName(), testPlanModuleWithVariant.variantAsStrings()));
			});
			return testModules;
		}

		public Object getVariantSummary() {

			// for each available variant, the set of permitted choices for that variant
			// e.g. ResponseType : ["code", "code id_token"]
			Map<ParameterHolder<?>, Set<String>> values = modulesWithVariant.stream()
					.flatMap(m -> {
						Set<TestModuleVariantInfo<? extends Enum<?>>> parameters = m.module.parameters;
						if (m.variant == null) {
							return parameters.stream();
						}
						// remove any variants preset by the plan definition
						Set<TestModuleVariantInfo<? extends Enum<?>>> selectableParameters = new HashSet<>();
						parameters.forEach((parameter) -> {
							if (!m.variant.containsKey(parameter.parameter.parameterClass)) {
								selectableParameters.add(parameter);
							}
						});
						return selectableParameters.stream();
					})
					.collect(groupingBy(p -> p.parameter,
							flatMapping(p -> p.allowedValues.stream(),
									mapping(v -> v.toString(),
											toSet()))));

			// for each available variant, the names of configuration fields needed for each value
			// e.g. ClientRegistration : { "static_client": [ "client_id" ] }
			Map<ParameterHolder<?>, Map<String, Set<String>>> fields = modulesWithVariant.stream()
					.flatMap(m -> m.module.parameters.stream())
					.collect(groupingBy(p -> p.parameter,
							flatMapping(p -> p.configurationFields.entrySet().stream(),
									groupingBy(e -> e.getKey().toString(),
											flatMapping(e -> e.getValue().stream(),
													mapping(v -> v.toString(),
															toSet()))))));

			// for each available variant, the names of configuration fields that can be hidden for each value
			// e.g. ClientRegistration : { "dynamic_client": [ "client.client_secret" ] }
			Map<ParameterHolder<?>, Map<String, Set<String>>> hideFields = modulesWithVariant.stream()
					.flatMap(m -> m.module.parameters.stream())
					.collect(groupingBy(p -> p.parameter,
							flatMapping(p -> p.hidesConfigurationFields.entrySet().stream(),
									groupingBy(e -> e.getKey().toString(),
											flatMapping(e -> e.getValue().stream(),
													mapping(v -> v.toString(),
															toSet()))))));

			return values.entrySet().stream()
					.collect(toMap(e -> e.getKey().variantParameter.name(),
							e -> {
								ParameterHolder<?> p = e.getKey();
								Set<String> allowed = e.getValue();
								Map<String, Set<String>> pf = fields.getOrDefault(e.getKey(), Map.of());
								Map<String, Set<String>> phf = hideFields.getOrDefault(e.getKey(), Map.of());
								return Map.of(
									"variantInfo", Map.of("displayName", p.variantParameter.displayName(),
															"description", p.variantParameter.description()),
									"variantValues", p.values().stream()
														.map(v -> v.toString())
														.filter(v -> allowed.contains(v))
														.collect(toOrderedMap(identity(),
															v -> Map.of("configurationFields", pf.getOrDefault(v, Set.of()),
																"hidesConfigurationFields", phf.getOrDefault(v, Set.of()))))
								);
							}));
		}

	}

	/**
	 * Holder for all the statically known information about a test module class
	 *
	 * Caches all information when instantiated and provides the way to create an instance of a test module
	 */
	public class TestModuleHolder {

		public final PublishTestModule info;

		final Class<? extends TestModule> moduleClass;
		final Set<TestModuleVariantInfo<? extends Enum<?>>> parameters;
		final Map<String, ParameterHolder<? extends Enum<?>>> parametersByName;
		final Map<Class<?>, ParameterHolder<?>> declaredParametersByClass;

		TestModuleHolder(Class<? extends TestModule> moduleClass) {
			this.moduleClass = moduleClass;
			this.info = moduleClass.getDeclaredAnnotation(PublishTestModule.class);

			Set<ParameterHolder<?>> declaredParameters = inCombinedAnnotations(moduleClass, VariantParameters.class)
					.flatMap(a -> Arrays.stream(a.value()))
					.map(c -> parameter(c))
					.collect(toSet());

			declaredParametersByClass = declaredParameters.stream()
					.collect(toMap(c -> c.parameterClass, identity()));

			Function<Class<?>, ParameterHolder<?>> moduleParameter = c -> {
				ParameterHolder<?> p = declaredParametersByClass.get(c);
				if (p == null) {
					throw new IllegalArgumentException("In annotation for %s: not a declared variant parameter: %s".formatted(
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

			Map<ParameterHolder<?>, Map<String, List<String>>> allHidesConfigurationFields =
					inCombinedAnnotations(moduleClass, VariantHidesConfigurationFields.class)
					.collect(groupingBy(a -> moduleParameter.apply(a.parameter()),
							groupingBy(VariantHidesConfigurationFields::value,
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
							allHidesConfigurationFields.getOrDefault(p, Map.of()),
							allSetupMethods.getOrDefault(p, Map.of())))
					.collect(toSet());

			this.parametersByName = parameters.stream()
					.map(p -> p.parameter)
					.collect(groupingBy(p -> p.variantParameter.name(), toSingleParameter()));
		}

		public boolean isApplicableForVariant(VariantSelection variant) {
			return isApplicableForVariant(typedVariant(variant, parametersByName));
		}

		boolean isApplicableForVariant(Map<ParameterHolder<? extends Enum<?>>, Enum<?>> variant) {
			return parameters.stream()
					.allMatch(p -> {
						Object v = variant.get(p.parameter);
						if (v == null) {
							if (p.parameter.hasDefault()){
								v = p.parameter.defaultValue();
							} else {
								throw new RuntimeException("TestModule '%s' requires a value for variant '%s'".formatted(
										this.info.testName(),
										p.parameter.variantParameter.name()));
							}
						}
						return p.allowedValues.contains(v);
					});
		}

		public Object getVariantSummary() {
			return parameters.stream()
					.collect(toMap(p -> p.parameter.variantParameter.name(),
							p -> {
								return Map.of(
									"variantInfo", Map.of("displayName", p.parameter.variantParameter.displayName(),
															"description", p.parameter.variantParameter.description()),
									"variantValues", p.allowedValues.stream()
															.collect(toOrderedMap(v -> v.toString(),
																v -> Map.of("configurationFields", p.configurationFields.getOrDefault(v, List.of()),
																	"hidesConfigurationFields", p.hidesConfigurationFields.getOrDefault(v, List.of()))))
								);
							}));
		}

		public TestModule newInstance(VariantSelection variant) {
			Map<ParameterHolder<? extends Enum<?>>, Enum<?>> typedVariant = typedVariant(variant, parametersByName);

			// Validate the supplied parameters

			Set<ParameterHolder<?>> declaredParameters = parameters.stream().map(p -> p.parameter).collect(toSet());

			Set<ParameterHolder<?>> missingParameters = Sets.difference(declaredParameters, typedVariant.keySet());
			if (!missingParameters.isEmpty()) {
				//checking the missing parameters has default value
				if(missingParameters.stream().filter(t -> !t.hasDefault()).findFirst().isPresent()) {
					throw new IllegalArgumentException("Missing values for required variant parameters: " +
							missingParameters.stream().map(p -> p.variantParameter.name()).collect(joining(", ")));
				} else {
					//every missing parameter has a default value
					missingParameters.stream().forEach(t-> {
						typedVariant.put(t, t.defaultValue());
					});
				}
			}

			// Note: supplying extra variant parameters is not an error - some modules in a test plan may require the
			// user to pick more variants than other modules do.

			parameters.forEach(p -> {
				Object v = typedVariant.get(p.parameter);
				if (!p.allowedValues.contains(v)) {
					throw new RuntimeException("Not an allowed value for variant parameter %s: %s".formatted(
						p.parameter.variantParameter.name(),
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

	/**
	 * Holds the variant specific information for a particular test module
	 *
	 * @param <T> the variant
	 */
	static class TestModuleVariantInfo<T extends Enum<T>> {

		final ParameterHolder<T> parameter;
		final Set<T> allowedValues;
		final Map<T, List<String>> configurationFields;
		final Map<T, List<String>> hidesConfigurationFields;
		final Map<T, List<Method>> setupMethods;

		TestModuleVariantInfo(
				ParameterHolder<T> parameter,
				Set<String> valuesNotApplicable,
				Map<String, List<String>> configurationFields,
				Map<String, List<String>> hidesConfigurationFields,
				Map<String, List<Method>> setupMethods) {

			this.parameter = parameter;

			this.allowedValues = EnumSet.allOf(parameter.parameterClass);
			valuesNotApplicable.forEach(s -> this.allowedValues.remove(parameter.valueOf(s)));

			this.configurationFields = configurationFields.entrySet().stream()
					.collect(toMap(e -> parameter.valueOf(e.getKey()), e -> e.getValue()));

			this.hidesConfigurationFields = hidesConfigurationFields.entrySet().stream()
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
