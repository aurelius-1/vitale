package com.marius.ptr.app.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

@Component
public class S3FileCheckRoute extends RouteBuilder {

    private final S3Client s3Client;

    @Value("${s3.bucket.name}")
    private String bucketName;

    @Value("${s3.bucket.processed-name}")
    private String processedBucketName;

    public S3FileCheckRoute(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void configure() {
        from("timer:s3-poll?period=10000")
                .routeId("s3-file-check-route")
                .process(exchange -> {
                    List<S3Object> objects = s3Client.listObjectsV2(
                            ListObjectsV2Request.builder()
                                    .bucket(bucketName)
                                    .build()
                    ).contents();

                    exchange.getIn().setHeader("fileCount", objects.size());
                    exchange.getIn().setBody(objects);
                })
                .choice()
                .when(simple("${header.fileCount} == 0"))
                    .log("No files found in bucket, skipping")
                .otherwise()
                    .log("Found ${header.fileCount} file(s) in bucket — processing")
                    .split(body())
                        .process(exchange -> {
                            S3Object s3Object = exchange.getIn().getBody(S3Object.class);
                            var response = s3Client.getObjectAsBytes(
                                    GetObjectRequest.builder()
                                            .bucket(bucketName)
                                            .key(s3Object.key())
                                            .build());
                            exchange.getIn().setBody(response.asByteArray());
                            exchange.getIn().setHeader("fileName", s3Object.key());
                            exchange.getIn().setHeader("fileSize", s3Object.size());
                        })
                        .choice()
                            .when(simple("${header.fileSize} == 0"))
                                .log("File '${header.fileName}' is EMPTY")
                                .setHeader("fileIsEmpty", constant(true))
                            .otherwise()
                                .log("File '${header.fileName}': ${header.fileSize} bytes — processing")
                                .setHeader("fileIsEmpty", constant(false))
                        .end()
                        .process(exchange -> {
                            String key = exchange.getIn().getHeader("fileName", String.class);
                            s3Client.copyObject(CopyObjectRequest.builder()
                                    .sourceBucket(bucketName)
                                    .sourceKey(key)
                                    .destinationBucket(processedBucketName)
                                    .destinationKey(key)
                                    .build());
                            s3Client.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build());
                            log.info("Moved '{}' to bucket '{}'", key, processedBucketName);
                        })
                    .end()
                .end();
    }
}
