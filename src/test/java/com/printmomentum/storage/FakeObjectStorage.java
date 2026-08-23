package com.printmomentum.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeObjectStorage implements ObjectStorage {

	private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

	@Override
	public void put(String key, byte[] bytes, String contentType) {
		objects.put(key, bytes == null ? new byte[0] : bytes);
	}

	@Override
	public boolean writable() {
		return true;
	}

	public byte[] get(String key) {
		return objects.get(key);
	}

	public Map<String, byte[]> objects() {
		return Map.copyOf(objects);
	}
}
