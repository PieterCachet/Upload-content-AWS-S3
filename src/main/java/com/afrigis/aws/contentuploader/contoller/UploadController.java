package com.afrigis.aws.contentuploader.contoller;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.afrigis.aws.contentuploader.config.AmazonClient;
import com.afrigis.aws.contentuploader.model.ContentUpload;
import com.afrigis.aws.contentuploader.model.UploadedFile;

@Controller
public class UploadController {

    private AmazonClient amazonClient;

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String landing(Model m) {
        m.addAttribute("folders", amazonClient.folders());
        m.addAttribute("files", new UploadedFile());
        return "upload";
    }

    @PostMapping(path = "/upload")
    public Callable<String> post(ContentUpload input, Model m) {
        return () -> {
            m.addAttribute("folders", amazonClient.folders());
            m.addAttribute("files", amazonClient.uploadFiles(input));
            return "upload";
        };
    }

    @Autowired
    public void setAmazonClient(AmazonClient amazonClient) {
        this.amazonClient = amazonClient;
    }

}
