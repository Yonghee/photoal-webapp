package com.photoal.webapp.web;

import com.photoal.webapp.service.storage.StorageFileNotFoundException;
import com.photoal.webapp.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/photos")
    public String listUploadedFiles(Model model) throws IOException {

        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", path.getFileName().toString()).build().toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    @GetMapping("/photos/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/photos")
    public String handleFileUpload(@RequestParam("file") MultipartFile[] files,
            RedirectAttributes redirectAttributes) {

        List<String> fileNames = new ArrayList<>();
        for (MultipartFile file : files) {
            storageService.store(file);
            fileNames.add(file.getOriginalFilename());
}

        if (fileNames.size() <= 0) {
            throw new IllegalArgumentException();
        }

        log.info("#### Uploaded File => {}", fileNames.toString());


        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded [" + fileNames.toString() + "]!");

        return "redirect:/photos";
    }

    @ExceptionHandler({StorageFileNotFoundException.class,IllegalArgumentException.class})
    public ResponseEntity<?> handleStorageFileNotFound(Throwable exc) {
        if (exc instanceof StorageFileNotFoundException) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().build();
        }

    }



}
