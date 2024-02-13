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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubApi {

    public final SimpleDateFormat PR_DATE_FORMAT = createDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private String github_token;
    private Date startDate = new Date(Long.MIN_VALUE);
    private Date endDate = new Date(Long.MAX_VALUE);
    private final SimpleDateFormat BASIC_DATE_FORMAT = createDateFormat("dd-MM-yyyy");
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
        List<JsonNode> filteredPullRequests = new ArrayList<>();

        for (JsonNode pr : pullRequests) {
            JsonNode user = pr.get("user");

            if (!"dependabot[bot]".equals(user.get("login").asText())
                    && "closed".equals(pr.get("state").asText())
                    && !pr.get("pull_request").get("merged_at").isNull()) {

                Date mergedDate;
                try {
                    mergedDate = PR_DATE_FORMAT.parse(pr.get("pull_request").get("merged_at").asText());
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

    public void run(List<String> logins) throws URISyntaxException, IOException, InterruptedException, ParseException {
        String searchQuery;

        int page = 1;
        boolean matchedPR = false; // check if any pull request was matched and written out
        for (String login : logins) {
             searchQuery = String.format("is:pr+repo:%s/%s+state:closed+author:%s", repoOwner, repoName, login);
            List<JsonNode> userPRs = new ArrayList<>();


            while (true) {
                HttpResponse<String> response = getApiResponse(String.format("https://api.github.com/search/issues?q=%s&per_page=100&page=%d", searchQuery, page));

                if (response.statusCode() == 403) {

                    System.out.println("%n%nYour API fetching limit has been exceeded, please wait for 1 min");
                    return;
                }
                // Check if the response is valid JSON
                JsonNode pullRequests = new ObjectMapper().readTree(response.body()).get("items");

                List<JsonNode> filteredPullRequests = filterPullRequests(pullRequests, logins, startDate, endDate);
                userPRs.addAll(filteredPullRequests);

                if (!response.headers().firstValue("Link").orElse("").contains("rel=\"next\"") || isClosedBeforeDate(pullRequests, startDate)) {
                    break; // No more pages, exit the loop
                }
                page++;
            }
            if (!userPRs.isEmpty()) {
                printPullRequestInfo(userPRs, login);
                matchedPR = true;
            }
        }
        if (!matchedPR) {
            System.out.println("No pull requests found for the specified date.");
        }

    }

    private HttpResponse<String> getApiResponse(String searchQuery) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI(searchQuery);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "token " + github_token)
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void printPullRequestInfo(List<JsonNode> pullRequests, String login) {

        System.out.println(login + ":");
        for (JsonNode pullRequest : pullRequests) {
            printSinglePullRequestInfo(pullRequest);
        }
    }

    private void printSinglePullRequestInfo(JsonNode pullRequest) {
        String pullRequestLink = getPullRequestLink(pullRequest.get("number").asInt());
        String body = pullRequest.get("body").asText();
        String issueNumber = extractIssueNumberFromBody(body);

        Date mergedDate;
        try {
            mergedDate = PR_DATE_FORMAT.parse(pullRequest.get("pull_request").get("merged_at").asText());
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing date", e);
        }
        String formattedMergedDate = BASIC_DATE_FORMAT.format(mergedDate);

        System.out.printf("    - Pull Request: %s%n", pullRequestLink);
        System.out.printf("      Merged at: %s%n", formattedMergedDate);
        System.out.printf("      Linked Issue ID: %s%n%n", issueNumber);

    }



    public List<String> getTeamMembers() throws URISyntaxException, IOException, InterruptedException {
        String orgName = "jboss-set";
        String teamName = "set";
        HttpResponse<String> response = getApiResponse(String.format("https://api.github.com/orgs/%s/teams/%s/members", orgName, teamName));

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
        if (!pullRequests.isEmpty()) {
            // Get the last entry in the array
            JsonNode lastPullRequest = pullRequests.get(pullRequests.size() - 1);
            String closedAtString = lastPullRequest.get("closed_at").asText("");
            Date closedAtDate = PR_DATE_FORMAT.parse(closedAtString);

            return closedAtDate.before(specifiedDate);

        }
        return false;
    }

    private SimpleDateFormat createDateFormat(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setLenient(false);
        return dateFormat;
    }
}
