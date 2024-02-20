package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GitHubApi {

    private final String githubToken;

    public GitHubApi(String githubToken) {
        this.githubToken = githubToken;
    }


    public HttpResponse<String> getResponse(String searchQuery) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI(searchQuery);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "token " + githubToken)
                .build();

        HttpResponse<String> response =  HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 403) {
            handleRateLimitExceeded();
        }
        return response;
    }



    public List<String> getTeamMembers() throws URISyntaxException, IOException, InterruptedException {
        String orgName = "jboss-set";
        String teamName = "set";
        HttpResponse<String> response = getResponse(String.format("https://api.github.com/orgs/%s/teams/%s/members", orgName, teamName));

        if (response.statusCode() != 200) {
            System.out.println("Could not get team_members from GitHub organization, you have probably entered wrong GitHub token.");
            System.exit(0);
        }
        JsonNode teamMembers = new ObjectMapper().readTree(response.body());
        List<String> logins = new ArrayList<>();

        for (JsonNode memberElement : teamMembers) {
            logins.add(memberElement.get("login").asText());
        }

        return logins;
    }

    public boolean hasNextPage(HttpResponse<String> response) {
        return response.headers().firstValue("Link").orElse("").contains("rel=\"next\"");
    }

    private void handleRateLimitExceeded() {
        System.out.println("ERROR:");
        System.out.println("Your API fetching limit has been exceeded, please wait for 1 min");
        System.exit(0);
    }

    
}
