package com.printmomentum.config;

import com.printmomentum.storage.NoopObjectStorage;
import com.printmomentum.storage.ObjectStorage;
import com.printmomentum.storage.S3ObjectStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

	@Bean
	@ConditionalOnProperty(prefix = "printmomentum.storage.s3", name = "enabled", havingValue = "true")
	S3Client s3Client(StorageProperties properties) {
		return S3Client.builder().region(Region.of(properties.region())).build();
	}

	@Bean
	ObjectStorage objectStorage(StorageProperties properties, ObjectProvider<S3Client> s3Client) {
		S3Client client = s3Client.getIfAvailable();
		if (properties.enabled() && client != null && properties.bucket() != null && !properties.bucket().isBlank()) {
			return new S3ObjectStorage(client, properties.bucket());
		}
		return new NoopObjectStorage();
	}
}
