package com.valar.basestrategy.utils;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.valar.basestrategy.application.PropertiesReader.properties;

public class S3FolderDownloader {

    private static final int THREAD_COUNT = 5;  // Adjust for parallel downloads

    public static void downloadFolder(boolean isIndex) {
        String bucketName = properties.getProperty("awsBucketName");
        String folderKey;
        String localDirectory;
        if (isIndex) {
            folderKey = properties.getProperty("folderNameForBn");
            localDirectory = properties.getProperty("instanceTargetFolderForBn");
        } else {
            folderKey = properties.getProperty("folderNameForStocks");
            localDirectory = properties.getProperty("instanceTargetFolderForStocks");
        }

        if (bucketName == null || folderKey == null || localDirectory == null) {
            System.err.println("Missing required properties: awsBucketName, folderName, or instanceTargetFolder.");
            return;
        }

        // Initialize S3 client
        S3Client s3Client = S3Client.builder()
                .region(Region.AP_SOUTH_1)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            String continuationToken = null;

            do {
                // List objects with pagination handling
                ListObjectsV2Request.Builder listRequestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(folderKey)
                        .maxKeys(1000); // AWS S3 returns max 1000 items per request

                if (continuationToken != null) {
                    listRequestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequestBuilder.build());
                continuationToken = listResponse.nextContinuationToken();

                if (listResponse.contents().isEmpty()) {
                    System.out.println("No files found in the specified S3 folder: " + folderKey);
                    return;
                }

                for (S3Object s3Object : listResponse.contents()) {
                    executor.submit(() -> downloadFile(s3Client, bucketName, folderKey, localDirectory, s3Object));
                }

            } while (continuationToken != null); // Continue until all files are fetched

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS); // Wait for all downloads to complete

            System.out.println("Download complete: " + localDirectory);

        } catch (Exception e) {
            System.err.println("Error accessing S3 bucket or folder. Please verify your permissions and settings.");
            e.printStackTrace();
        } finally {
            s3Client.close();
        }
    }

    private static void downloadFile(S3Client s3Client, String bucketName, String folderKey, String localDirectory, S3Object s3Object) {
        String key = s3Object.key();
        System.out.println("Checking: " + key);

        // Skip the folder itself
        if (key.endsWith("/")) return;

        // Local file path
        String localFilePath = Paths.get(localDirectory, key.replace(folderKey, "")).toString();
        File localFile = new File(localFilePath);

        // Skip downloading if the file already exists with the same size
        if (localFile.exists() && localFile.length() == s3Object.size()) {
            System.out.println("File already exists, skipping: " + localFilePath);
            return;
        }

        // Ensure directories exist
        if (!localFile.getParentFile().exists() && !localFile.getParentFile().mkdirs()) {
            System.err.println("Failed to create directory: " + localFile.getParentFile().getPath());
            return;
        }

        // Download the object
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(Paths.get(localFilePath)));
            System.out.println("Downloaded: " + localFilePath);
        } catch (Exception e) {
            System.err.println("Error downloading file: " + key);
            e.printStackTrace();
        }
    }
}
