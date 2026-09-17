package net.openid.conformance.info;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.logging.MongoKeyWrapper;
import net.openid.conformance.variant.VariantSelection;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Document(collection = DBTestPlanService.COLLECTION)
public class Plan {

	@Id
	private String _id;
	@Indexed
	private String planName;
	private VariantSelection variant;
	private org.bson.Document config;
	@Indexed
	private String started;
	@Indexed
	private Map<String, String> owner;
	@Indexed
	private String description;
	private List<String> certificationProfileName;
	private List<Module> modules;
	private String version;
	private String summary;
	@Indexed
	private String publish;

	private Boolean immutable;

	public static class Module {

		private String testModule;
		private Map<String,String> variant;
		private List<String> instances;
		/**
		 * The status and result of the module's latest run, attached to a listing row from that
		 * run's test document; never stored with the plan.
		 */
		@Transient
		private String status;
		@Transient
		private String result;

		Module() {
			// Load constructor
		}

		public Module(String module, Map<String,String> variant) {
			this(module, variant, Collections.emptyList());
		}

		/**
		 * A module that has already run. Stored modules are read back through the no-arg
		 * constructor, and a new plan's modules have no runs yet, so this is for building the
		 * shape a listing decorates outside the database.
		 */
		Module(String module, Map<String,String> variant, List<String> instances) {
			this.testModule = module;
			this.variant = variant;
			this.instances = instances;
		}

		public String getTestModule() {
			return testModule;
		}

		public List<String> getInstances() {
			return instances;
		}

		public Map<String,String> getVariant() { return variant; }

		public String getStatus() {
			return status;
		}

		public String getResult() {
			return result;
		}

		void setLatestRun(String status, String result) {
			this.status = status;
			this.result = result;
		}
	}

	Plan() {
		// Load constructor
	}

	public Plan(String id,
			String planName,
			VariantSelection variant,
			JsonObject config,
			Instant started,
			Map<String, String> owner,
			String description,
			List<String> certificationProfileName,
			List<Module> testModules,
			String version,
			String summary,
			String publish) {
		this._id = id;
		this.planName = planName;
		this.variant = variant;
		this.config = org.bson.Document.parse(new GsonBuilder().serializeNulls().create().toJson(
				MongoKeyWrapper.wrap(config)));
		this.started = started.toString();
		this.owner = owner;
		this.description = description;
		this.certificationProfileName = certificationProfileName;
		this.modules = testModules;
		this.version = version;
		this.summary = summary;
		this.publish = publish;
	}

	public String getId() {
		return _id;
	}

	public String getPlanName() {
		return planName;
	}

	public VariantSelection getVariant() {
		return variant;
	}

	public org.bson.Document getConfig() {
		return config;
	}

	public String getStarted() {
		return started;
	}

	public Map<String, String> getOwner() {
		return owner;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getCertificationProfileName() {
		return certificationProfileName;
	}

	public List<Module> getModules() {
		return modules;
	}

	public String getVersion() {
		return version;
	}

	public String getSummary() {
		return summary;
	}

	public String getPublish() {
		return publish;
	}

	public Boolean getImmutable() {
		return immutable;
	}
}
