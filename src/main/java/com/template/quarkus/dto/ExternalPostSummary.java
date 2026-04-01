package com.template.quarkus.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Transformed summary of an external post")
public class ExternalPostSummary {

    @Schema(description = "Post ID", example = "1")
    public Integer id;

    @Schema(description = "Author user ID", example = "1")
    public Integer authorId;

    @Schema(description = "Post title in title case", example = "Sunt Aut Facere Repellat Provident Occaecati")
    public String title;

    @Schema(description = "Truncated body (first 100 chars)", example = "quia et suscipit...")
    public String excerpt;

    @Schema(description = "Estimated read time in seconds based on word count", example = "12")
    public int estimatedReadSeconds;

    public static ExternalPostSummary from(ExternalPostDto post) {
        ExternalPostSummary summary = new ExternalPostSummary();
        summary.id = post.id;
        summary.authorId = post.userId;
        summary.title = toTitleCase(post.title);
        summary.excerpt = post.body != null && post.body.length() > 100
                ? post.body.substring(0, 100) + "..."
                : post.body;
        int wordCount = post.body != null ? post.body.split("\\s+").length : 0;
        summary.estimatedReadSeconds = (int) Math.ceil(wordCount / 3.0); // ~180 wpm reading speed
        return summary;
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
