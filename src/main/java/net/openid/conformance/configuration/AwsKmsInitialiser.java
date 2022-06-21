package net.openid.conformance.configuration;

import com.raidiam.conformance.kms.jce.provider.KmsProvider;
import net.openid.conformance.condition.util.AbstractMtlsStrategy;
import net.openid.conformance.condition.util.AwsKmsMtlsStrategy;
import net.openid.conformance.extensions.KmsJWSSignerFactory;
import net.openid.conformance.extensions.SmartJWSSignerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

import java.security.Security;

@Configuration
public class AwsKmsInitialiser {

	private static final Logger LOG = LoggerFactory.getLogger(AwsKmsInitialiser.class);

	@Value("${fintechlabs.use.aws.kms:false}") boolean useAwsKms;
	@Value("${fintechlabs.use.aws.kms.lazyeval:true}") boolean lazilyEvaluate;

	@EventListener(ApplicationReadyEvent.class)
	void setup() {
		if(useAwsKms) {
			String region = System.getenv("AWS_REGION");
			LOG.info("Initialising KMS client in region {}", region);
			KmsClient kmsClient = KmsClient.builder()
				.region(Region.of(region))
				.build();
			if(!lazilyEvaluate) {
				// fail early if client not configured properly
				kmsClient.listAliases();
			}

			Security.insertProviderAt(new KmsProvider(kmsClient), 1);
			LOG.info("KMS security provider registered - provider name awsKms");
			AbstractMtlsStrategy.register("awsKms", new AwsKmsMtlsStrategy());
			SmartJWSSignerFactory signerFactory = SmartJWSSignerFactory.getInstance();
			signerFactory.register(new KmsJWSSignerFactory(kmsClient));
			LOG.info("KMS JWS signer registered");

		}
	}

}
