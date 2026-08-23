package com.printmomentum.storage;

public interface ObjectStorage {

	void put(String key, byte[] bytes, String contentType);

	boolean writable();

	static String objectKey(long listingId, int n) {
		return "listings/" + listingId + "/" + n + ".jpg";
	}
}
