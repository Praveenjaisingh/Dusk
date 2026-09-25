package com.example.chat.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.exception.ValidationException;
import com.example.chat.service.FileStorageService;
import com.example.chat.util.CurrentUserResolver;

import jakarta.servlet.http.Part;

@Component
public class MediaController {

    private final FileStorageService fileStorageService;
    private final CurrentUserResolver currentUserResolver;

    public MediaController(
            FileStorageService fileStorageService,
            CurrentUserResolver currentUserResolver) {
        this.fileStorageService = fileStorageService;
        this.currentUserResolver = currentUserResolver;
    }

    public ServerResponse upload(ServerRequest request) throws Exception {
        try {
            // Authentication is already enforced by SecurityConfig.
            // This also verifies that the current user can be resolved.
            currentUserResolver.resolve();

            MultiValueMap<String, Part> multipartData =
                    request.multipartData();

            Part file = multipartData.getFirst("file");

            if (file == null || file.getSize() == 0) {
                throw new ValidationException(
                        Map.of("file", "File is required")
                );
            }

            Map<String, Object> stored =
                    fileStorageService.store(file);

            return ServerResponse.status(HttpStatus.CREATED).body(Map.of(
                    "status", true,
                    "message", "File uploaded successfully",
                    "data", stored
            ));

        } catch (ValidationException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Validation failed",
                    "errors", ex.getErrors()
            ));

        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }
}
