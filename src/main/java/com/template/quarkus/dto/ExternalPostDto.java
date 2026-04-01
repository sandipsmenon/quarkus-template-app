package com.template.quarkus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Post data fetched from JSONPlaceholder external API")
public class ExternalPostDto {

    @Schema(description = "ID of the user who authored the post", example = "1")
    @JsonProperty("userId")
    public Integer userId;

    @Schema(description = "Unique post identifier", example = "1")
    public Integer id;

    @Schema(description = "Post title", example = "sunt aut facere repellat provident occaecati")
    public String title;

    @Schema(description = "Post body content", example = "quia et suscipit...")
    public String body;
}
