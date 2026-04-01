package com.template.quarkus.resource;

import com.template.quarkus.config.AppConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * FEATURE: Multipart File Upload (quarkus-rest).
 *
 * Quarkus REST (RESTEasy Reactive) handles multipart form data natively
 * using @RestForm annotations on method parameters or a form data class.
 *
 * Two styles for reading multipart data:
 *
 *   Style 1 — Individual @RestForm parameters (used here):
 *     public Response upload(
 *         @RestForm("file") FileUpload file,
 *         @RestForm("name") String name) { ... }
 *
 *   Style 2 — Form data class with @MultipartForm:
 *     public class UploadForm {
 *         @RestForm FileUpload file;
 *         @RestForm String description;
 *     }
 *     public Response upload(@MultipartForm UploadForm form) { ... }
 *
 * FileUpload provides:
 *   fileUpload.filePath()       — the uploaded file as a Path (temp file)
 *   fileUpload.fileName()       — original filename from the client
 *   fileUpload.contentType()    — MIME type (e.g., image/jpeg, text/csv)
 *   fileUpload.size()           — file size in bytes
 *
 * Max upload size is configured via:
 *   quarkus.http.limits.max-body-size=10M  (application.properties)
 *
 * Calling this endpoint with curl:
 *   curl -X POST http://localhost:8080/api/files/upload \
 *     -F "file=@/path/to/products.csv;type=text/csv" \
 *     -F "description=Product import batch 1"
 *
 * Or with JavaScript (browser):
 *   const form = new FormData();
 *   form.append('file', fileInput.files[0]);
 *   form.append('description', 'batch import');
 *   fetch('/api/files/upload', { method: 'POST', body: form });
 */
@Path("/api/files")
@Tag(name = "File Upload", description = "Multipart file upload demonstration")
public class FileUploadResource {

    private static final Logger LOG = Logger.getLogger(FileUploadResource.class);

    // Storage directory for uploaded files — in production use object storage (S3, GCS, etc.)
    private static final Path UPLOAD_DIR = Path.of(System.getProperty("java.io.tmpdir"), "quarkus-uploads");

    @Inject
    AppConfig appConfig;

    // -------------------------------------------------------------------------
    // POST /api/files/upload — single file upload
    // -------------------------------------------------------------------------
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Upload a file (multipart/form-data)",
        description = """
            Accepts a multipart/form-data request with:
              - file: the binary file content
              - description: optional text description

            Returns metadata about the stored file.

            Max file size: controlled by quarkus.http.limits.max-body-size (default 10M).
            """
    )
    public Response uploadFile(
            @RestForm("file") FileUpload file,
            @RestForm("description") @DefaultValue("") String description) {

        if (!appConfig.features().fileUploadEnabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("message", "File upload feature is disabled"))
                    .build();
        }

        if (file == null || file.size() == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "No file provided or file is empty"))
                    .build();
        }

        LOG.infof("[FILE-UPLOAD] Received: name=%s, type=%s, size=%d bytes",
                file.fileName(), file.contentType(), file.size());

        try {
            // Create upload directory if it doesn't exist
            Files.createDirectories(UPLOAD_DIR);

            // Generate a unique filename to avoid collisions
            String extension  = extractExtension(file.fileName());
            String storedName = UUID.randomUUID() + extension;
            Path destination  = UPLOAD_DIR.resolve(storedName);

            // Copy the temp file to the upload directory
            Files.copy(file.filePath(), destination, StandardCopyOption.REPLACE_EXISTING);

            LOG.infof("[FILE-UPLOAD] Stored as: %s", destination);

            return Response.ok(Map.of(
                "originalName", file.fileName(),
                "storedName",   storedName,
                "contentType",  file.contentType(),
                "sizeBytes",    file.size(),
                "description",  description,
                "message",      "File uploaded successfully"
            )).build();

        } catch (IOException e) {
            LOG.errorf(e, "[FILE-UPLOAD] Failed to store file: %s", file.fileName());
            return Response.serverError()
                    .entity(Map.of("message", "Failed to store file: " + e.getMessage()))
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/files/upload/multiple — multiple files in one request
    // -------------------------------------------------------------------------
    @POST
    @Path("/upload/multiple")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Upload multiple files at once",
        description = """
            Accepts multiple files in a single multipart request.

            curl example:
              curl -X POST http://localhost:8080/api/files/upload/multiple \\
                -F "files=@file1.txt" -F "files=@file2.txt"
            """
    )
    public Response uploadMultiple(
            @RestForm("files") java.util.List<FileUpload> files) {

        if (files == null || files.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "No files provided"))
                    .build();
        }

        var results = files.stream().map(f -> Map.of(
            "name",        f.fileName(),
            "contentType", f.contentType(),
            "sizeBytes",   f.size()
        )).toList();

        LOG.infof("[FILE-UPLOAD] Received %d files", files.size());

        return Response.ok(Map.of(
            "uploadedCount", files.size(),
            "files", results,
            "note", "In this demo, files are listed but not persisted to disk"
        )).build();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
