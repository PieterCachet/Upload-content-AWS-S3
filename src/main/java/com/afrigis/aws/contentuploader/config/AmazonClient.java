package com.afrigis.aws.contentuploader.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.afrigis.aws.contentuploader.model.ContentUpload;
import com.afrigis.aws.contentuploader.model.Folder;
import com.afrigis.aws.contentuploader.model.UploadedFile;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

@Service
public class AmazonClient {

    private AmazonS3 awsS3Client;

    @Value("${aws.endpointUrl}")
    private String endpointUrl;
    @Value("${aws.accessKey}")
    private String accessKey;
    @Value("${aws.secretKey}")
    private String secretKey;
    @Value("${aws.bucketName}")
    private String bucketName;

    @PostConstruct
    private void initializeAmazon() {
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.awsS3Client = new AmazonS3Client(credentials);
    }

    public List<UploadedFile> uploadFiles(ContentUpload input) throws Exception {
        Map<String, MultipartFile> fileMap = buildParamMap(input);
        List<UploadedFile> fileLocation = new ArrayList<>();
        for (Map.Entry<String, MultipartFile> file : fileMap.entrySet()) {
            fileLocation.add(uploadFile(file.getKey(), file.getValue(), input.getFolderName()));
        }
        return fileLocation;
    }

    private Map<String, MultipartFile> buildParamMap(ContentUpload input) {
        Map<String, MultipartFile> params = new LinkedHashMap<>();
        for (int i = 0; i < input.getFiles().size(); i++) {
            String n = input.getFileNames().get(i);
            MultipartFile f = input.getFiles().get(i);
            params.put(n, f);
        }
        return params;
    }

    public UploadedFile uploadFile(String filename, MultipartFile multipartFile, String folder) throws Exception {
        UploadedFile uploadedFile = new UploadedFile();
        try {
            File file = convertMultiPartToFile(multipartFile);
            StringBuilder fileName = buildupFileName(filename, file);
            uploadFileTos3bucket(fileName.toString(), file, folder);
            uploadedFile.setName(buildupFileEndPoint(endpointUrl, bucketName, folder, fileName.toString()));
            uploadedFile.setSize(file.length());
            file.delete();
        } catch (Exception e) {
            throw e;
        }
        return uploadedFile;
    }

    private StringBuilder buildupFileName(String filename, File file) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(filename);
        fileName.append(".");
        fileName.append(FilenameUtils.getExtension(file.getName()));
        return fileName;
    }

    private String buildupFileEndPoint(String endpointUrl, String bucketName, String folder, String fileName) {
        StringBuilder fileLoaction = new StringBuilder();
        fileLoaction.append(endpointUrl);
        fileLoaction.append("/");
        fileLoaction.append(bucketName);
        fileLoaction.append("/");
        fileLoaction.append(folder);
        fileLoaction.append("/");
        fileLoaction.append(fileName);
        return fileLoaction.toString();
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    // Upload file to AWS.
    private PutObjectResult uploadFileTos3bucket(String fileName, File file, String folder) {
        try {
            StringBuilder location = new StringBuilder();
            location.append(bucketName);
            if (!StringUtils.isEmpty(folder)) {
                location.append("/");
                location.append(folder);
            }

            return awsS3Client.putObject(new PutObjectRequest(location.toString(), fileName, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch (Exception e) {
            throw e;
        }
    }

    // List folder(s) within bucket.
    public List<Folder> folders() {
        List<Folder> folders = new ArrayList<>();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withDelimiter("/");
        ListObjectsV2Result listing = awsS3Client.listObjectsV2(req);
        for (String commonPrefix : listing.getCommonPrefixes()) {
            Folder folder = new Folder();
            folder.setName(removeLastCharOptional(commonPrefix));
            folders.add(folder);
        }
        return folders;
    }

    // Remove the last char of a given string.
    public static String removeLastCharOptional(String s) {
        return Optional.ofNullable(s)
                .filter(str -> str.length() != 0)
                .map(str -> str.substring(0, str.length() - 1))
                .orElse(s);
    }

    // WIP - Delete file
    public String deleteFileFromS3Bucket(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            awsS3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
            return "Item was deleted.";
        } catch (Exception e) {
            return "Exception occured whilst deleting file";
        }
    }

}
