package net.openid.conformance.info;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.logging.GsonObjectToBsonDocumentConverter;
import net.openid.conformance.variant.VariantSelection;
import org.springframework.data.annotation.Id;
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
	private String certificationProfileName;
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

		Module() {
			// Load constructor
		}

		public Module(String module, Map<String,String> variant) {
			this.testModule = module;
			this.variant = variant;
			this.instances = Collections.emptyList();
		}

		public String getTestModule() {
			return testModule;
		}

		public List<String> getInstances() {
			return instances;
		}

		public Map<String,String> getVariant() { return variant; }
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
			String certificationProfileName,
			List<Module> testModules,
			String version,
			String summary,
			String publish) {
		this._id = id;
		this.planName = planName;
		this.variant = variant;
		this.config = org.bson.Document.parse(new GsonBuilder().serializeNulls().create().toJson(
				GsonObjectToBsonDocumentConverter.convertFieldsToStructure(config)));
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

	public String getCertificationProfileName() {
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
