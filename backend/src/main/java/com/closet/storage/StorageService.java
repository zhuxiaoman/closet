package com.closet.storage;
import java.io.InputStream;

public interface StorageService {
    String upload(String prefix, String filename, InputStream content, long size, String contentType);
    InputStream download(String key);
    void delete(String key);
}
