package com.printmomentum.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public final class S3ObjectStorage implements ObjectStorage {

	private final S3Client s3Client;
	private final String bucket;

	public S3ObjectStorage(S3Client s3Client, String bucket) {
		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	@Override
	public void put(String key, byte[] bytes, String contentType) {
		s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(bucket)
						.key(key)
						.contentType(contentType)
						.build(),
				RequestBody.fromBytes(bytes == null ? new byte[0] : bytes));
	}

	@Override
	public boolean writable() {
		return true;
	}
}
