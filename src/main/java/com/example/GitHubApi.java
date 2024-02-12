package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubApi {

    public final SimpleDateFormat PR_DATE_FORMAT = createDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private String github_token;
    private Date startDate = new Date(Long.MIN_VALUE);
    private Date endDate = new Date(Long.MAX_VALUE);
    private final SimpleDateFormat BASIC_DATE_FORMAT = createDateFormat("dd-MM-yyyy");

    // Specify the repository owner and name
    private final String repoOwner = "quarkusio";
    private final String repoName = "quarkus";

    public GitHubApi(String github_token) {
        this.github_token = github_token;
    }

    public GitHubApi(String github_token, String startDate) {
        this.github_token = github_token;
        try {
            this.startDate = BASIC_DATE_FORMAT.parse(startDate);
        } catch (ParseException e) {
            throw new RuntimeException("The provided date format is incorrect. Please use the format dd-MM-yyyy, e.g. 25-02-2022");
        }
    }

    public GitHubApi(String github_token, String startDate, String endDate) {
        this.github_token = github_token;
        try {
            this.startDate = BASIC_DATE_FORMAT.parse(endDate);
            this.startDate = BASIC_DATE_FORMAT.parse(startDate);
        } catch (ParseException e) {
            throw new RuntimeException("The provided date format is incorrect. Please use the format dd-MM-yyyy, e.g. 25-02-2022");
        }
    }

    public String extractIssueNumberFromBody(String body) {
        // Check if body is null
        if (body == null) {
            return "No linked issue";
        }

        // Extract issue number from the pull request body
        // Look for both the "#number" pattern and GitHub issue URL
        Pattern issuePattern = Pattern.compile("#([0-9]+)");
        Pattern urlPattern = Pattern.compile("https://github.com/\\S+/issues/([0-9]+)");

        Matcher matchIssue = issuePattern.matcher(body);
        Matcher matchUrl = urlPattern.matcher(body);

        if (matchIssue.find()) {
            return matchIssue.group(1);
        } else if (matchUrl.find()) {
            return matchUrl.group(1);
        } else {
            return "No linked issue";
        }
    }

    public List<JsonNode> filterPullRequests(JsonNode pullRequests, List<String> logins, Date startDate, Date endDate) {
        // Filter pull requests based on specified logins and merged date
        List<JsonNode> filteredPullRequests = new ArrayList<>();

        for (JsonNode pr : pullRequests) {
            JsonNode user = pr.get("user");

            if (!"dependabot[bot]".equals(user.get("login").asText())
                    && "closed".equals(pr.get("state").asText())
                    && logins.contains(user.get("login").asText())
                    && !pr.get("merged_at").isNull()) {

                Date mergedDate;
                try {
                    mergedDate = PR_DATE_FORMAT.parse(pr.get("merged_at").asText());
                } catch (ParseException e) {
                    throw new RuntimeException("Error parsing date", e);
                }

                if (startDate.compareTo(mergedDate) <= 0 && endDate.compareTo(mergedDate) > 0) {
                    filteredPullRequests.add(pr);
                }

            }
        }

        return filteredPullRequests;
    }

    public List<JsonNode> getPullRequests(List<String> logins) throws URISyntaxException, IOException, InterruptedException, ParseException {
        // Initialize an empty list to store all pull requests
        List<JsonNode> allPullRequests = new ArrayList<>();

        // Set the start and end dates for the range of interest

        int page = 1; // Initialize page counter

        while (true) {
            // Get the list of pull requests from the specified repository
            URI uri = new URI(String.format("https://api.github.com/repos/%s/%s/pulls?state=closed&per_page=100&page=%d", repoOwner, repoName, page));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "token " + github_token)
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the response is valid JSON
            JsonNode pullRequests = new ObjectMapper().readTree(response.body());

            // Filter and add pull requests to the list
            List<JsonNode> filteredPullRequests = filterPullRequests(pullRequests, logins, startDate, endDate);
            allPullRequests.addAll(filteredPullRequests);

            // Check if there is another page

            if (!response.headers().firstValue("Link").orElse("").contains("rel=\"next\"") || isClosedBeforeDate(pullRequests, startDate)) {
                break; // No more pages, exit the loop
            }

            page++; // Move to the next page
        }
        return allPullRequests;
    }

    public void printPullRequestInfo(JsonNode pullRequest) {
        // Print information about the pull request
        JsonNode user = pullRequest.get("user");
        String pullRequestLink = getPullRequestLink(pullRequest.get("number").asInt());
        String body = pullRequest.get("body").asText();
        String issueNumber = extractIssueNumberFromBody(body);

        Date mergedDate;
        try {
            mergedDate = PR_DATE_FORMAT.parse(pullRequest.get("merged_at").asText());
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing date", e);
        }
        String formattedMergedDate = BASIC_DATE_FORMAT.format(mergedDate);

        System.out.printf("Pull Request: %s, User: %s, Merged at: %s, Linked Issue ID: %s%n", pullRequestLink, user.get("login").asText(), formattedMergedDate, issueNumber);
    }

    public List<String> getTeamMembers() throws URISyntaxException, IOException, InterruptedException {
        // GitHub API endpoint for team members
        String orgName = "jboss-set";
        String teamName = "set";
        URI uri = new URI(String.format("https://api.github.com/orgs/%s/teams/%s/members", orgName, teamName));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "token " + github_token)
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Make the API request using the HttpClient and extract logins
        JsonNode teamMembers = new ObjectMapper().readTree(response.body());
        List<String> logins = new ArrayList<>();

        for (JsonNode memberElement : teamMembers) {
            logins.add(memberElement.get("login").asText());
        }

        return logins;
    }

    private String getPullRequestLink(int prNumber) {
        String prLinkTemplate = "https://github.com/%s/%s/pull/%d";
        return String.format(prLinkTemplate, repoOwner, repoName, prNumber);
    }

    private boolean isClosedBeforeDate(JsonNode pullRequests, Date specifiedDate) throws ParseException {
        // Check if the pullRequests node is an array and not empty
        if (!pullRequests.isEmpty()) {
            // Get the last entry in the array
            JsonNode lastPullRequest = pullRequests.get(pullRequests.size() - 1);

            // Get the "closed_at" value (returns null if field is absent)
            String closedAtString = lastPullRequest.get("closed_at").asText("");

            // Parse the "closed_at" value as a Date
            Date closedAtDate = PR_DATE_FORMAT.parse(closedAtString);

            return closedAtDate.before(specifiedDate);

        }

        // Return false if any condition fails
        return false;
    }

    private SimpleDateFormat createDateFormat(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setLenient(false);
        return dateFormat;
    }
}