package com.template.quarkus.service;

import com.template.quarkus.client.JsonPlaceholderClient;
import com.template.quarkus.dto.ExternalPostDto;
import com.template.quarkus.dto.ExternalPostSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ExternalApiService {

    private static final Logger LOG = Logger.getLogger(ExternalApiService.class);

    @RestClient
    @Inject
    JsonPlaceholderClient jsonPlaceholderClient;

    public List<ExternalPostSummary> getAllPostSummaries() {
        LOG.debug("Fetching all posts from JSONPlaceholder");
        try {
            List<ExternalPostDto> posts = jsonPlaceholderClient.getAllPosts();
            return posts.stream()
                    .map(ExternalPostSummary::from)
                    .toList();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch posts from external API");
            throw new WebApplicationException("External API unavailable. Please try again later.", 502);
        }
    }

    public ExternalPostSummary getPostSummaryById(Integer id) {
        LOG.debugf("Fetching post id=%d from JSONPlaceholder", id);
        try {
            ExternalPostDto post = jsonPlaceholderClient.getPostById(id);
            return ExternalPostSummary.from(post);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new WebApplicationException("External post not found with ID: " + id, 404);
            }
            LOG.errorf(e, "Failed to fetch post id=%d from external API", id);
            throw new WebApplicationException("External API unavailable. Please try again later.", 502);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch post id=%d from external API", id);
            throw new WebApplicationException("External API unavailable. Please try again later.", 502);
        }
    }

    public List<ExternalPostSummary> getPostSummariesByUser(Integer userId) {
        LOG.debugf("Fetching posts for userId=%d from JSONPlaceholder", userId);
        try {
            List<ExternalPostDto> posts = jsonPlaceholderClient.getPostsByUser(userId);
            return posts.stream()
                    .map(ExternalPostSummary::from)
                    .toList();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch posts for userId=%d from external API", userId);
            throw new WebApplicationException("External API unavailable. Please try again later.", 502);
        }
    }
}
