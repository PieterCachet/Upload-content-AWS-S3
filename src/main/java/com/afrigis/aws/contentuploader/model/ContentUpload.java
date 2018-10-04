package com.afrigis.aws.contentuploader.model;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ContentUpload {
    
    private String folderName;
    private List<String> fileNames;
    private List<MultipartFile> files;

}
