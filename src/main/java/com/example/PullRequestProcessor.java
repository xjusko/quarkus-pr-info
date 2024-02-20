package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.DateParser.BASIC_DATE_FORMAT;
import static com.example.DateParser.PR_DATE_FORMAT;

public class PullRequestProcessor {

    private Date startDate = new Date(Long.MIN_VALUE);
    private Date endDate = new Date(Long.MAX_VALUE);
    private final String repoOwner = "quarkusio";
    private final String repoName = "quarkus";
    private final DateParser dateParser = new DateParser();
    private final GitHubApi gitHubApi;
    private final List<String> logins;

    public PullRequestProcessor(String token) throws URISyntaxException, IOException, InterruptedException {
        this.gitHubApi = new GitHubApi(token);
        this.logins = gitHubApi.getTeamMembers();
    }
    public PullRequestProcessor(String token, String startDate) throws URISyntaxException, IOException, InterruptedException {
        this(token);
        this.startDate = dateParser.parseDate(BASIC_DATE_FORMAT, startDate);
    }

    public PullRequestProcessor(String token, String startDate, String endDate) throws URISyntaxException, IOException, InterruptedException {
        this(token, startDate);
        this.endDate = dateParser.parseDate(BASIC_DATE_FORMAT, endDate);
    }

    public void process() throws URISyntaxException, IOException, InterruptedException {
        StringBuilder allPullRequestsInfo = new StringBuilder();

        for (String login : logins) {
            String searchQuery = String.format("is:pr+repo:%s/%s+state:closed+author:%s", repoOwner, repoName, login);
            List<JsonNode> userPRs = new ArrayList<>();
            int page = 1;

            while (true) {
                HttpResponse<String> response = gitHubApi.getResponse(String.format("https://api.github.com/search/issues?q=%s&per_page=100&page=%d", searchQuery, page));
                JsonNode pullRequests = getPullRequests(response);

                List<JsonNode> filteredPullRequests = filterPullRequests(pullRequests);
                userPRs.addAll(filteredPullRequests);

                // Check if there is another page and if it needs to be loaded
                if (!gitHubApi.hasNextPage(response) || isClosedBeforeDate(pullRequests)) {
                    break;
                }

                page++;
            }

            if (!userPRs.isEmpty()) {
                appendPullRequestsToOutput(login, allPullRequestsInfo, userPRs);
            }
        }

        printOutput(allPullRequestsInfo);
    }

    private void printOutput(StringBuilder allPullRequestsInfo) {
        if (allPullRequestsInfo.isEmpty()) {
            System.out.println("No pull requests found for the specified date.");
        } else {
            System.out.println(allPullRequestsInfo);
        }
    }

    private void appendPullRequestsToOutput(String login, StringBuilder allPullRequestsInfo, List<JsonNode> userPRs) {
        allPullRequestsInfo.append(login).append(":\n");
        userPRs.forEach(pr -> allPullRequestsInfo.append(getSinglePullRequestInfo(pr)));
        allPullRequestsInfo.append("\n");
    }

    private static JsonNode getPullRequests(HttpResponse<String> response) throws JsonProcessingException {
        return new ObjectMapper().readTree(response.body()).get("items");
    }

    public List<JsonNode> filterPullRequests(JsonNode pullRequests) {
        List<JsonNode> filteredPullRequests = new ArrayList<>();

        for (JsonNode pr : pullRequests) {
            JsonNode user = pr.get("user");

            if (!"dependabot[bot]".equals(user.get("login").asText())
                    && "closed".equals(pr.get("state").asText())
                    && !pr.get("pull_request").get("merged_at").isNull()) {

                Date mergedDate = dateParser.parseDate(PR_DATE_FORMAT, pr.get("pull_request").get("merged_at").asText());
                if (isWithinDateRange(mergedDate)) {
                    filteredPullRequests.add(pr);
                }
            }
        }

        return filteredPullRequests;
    }


    private String getSinglePullRequestInfo(JsonNode pullRequest) {
        String pullRequestLink = getPullRequestLink(pullRequest.get("number").asInt());
        String body = pullRequest.get("body").asText();
        String issueNumber = extractIssueNumberFromBody(body);

        Date mergedDate = dateParser.parseDate(PR_DATE_FORMAT, pullRequest.get("pull_request").get("merged_at").asText());
        String formattedMergedDate = BASIC_DATE_FORMAT.format(mergedDate);

        return String.format("    - Pull Request: %s%n      Merged at: %s%n      Linked Issue ID: %s%n%n",
                pullRequestLink, formattedMergedDate, issueNumber);
    }

    public String extractIssueNumberFromBody(String body) {
        if (body == null) {
            return "No linked issue";
        }

        // Look for both the "#number" pattern and GitHub issue URL
        Pattern issuePattern = Pattern.compile("#([0-9]+)");
        Pattern urlPattern = Pattern.compile("https://github.com/\\S+/issues/([0-9]+)");

        Matcher matchIssue = issuePattern.matcher(body);
        Matcher matchUrl = urlPattern.matcher(body);

        return matchIssue.find() ? matchIssue.group(1) : matchUrl.find() ? matchUrl.group(1) : "No linked issue";
    }

    private boolean isWithinDateRange(Date date) {
        return startDate.compareTo(date) <= 0 && endDate.compareTo(date) > 0;
    }

    private String getPullRequestLink(int prNumber) {
        return String.format("https://github.com/%s/%s/pull/%d", repoOwner, repoName, prNumber);
    }

    // Look at the date of the last Pull Request from the retrieved page
    private boolean isClosedBeforeDate(JsonNode pullRequests) {
        if (!pullRequests.isEmpty()) {
            JsonNode lastPullRequest = pullRequests.get(pullRequests.size() - 1);
            Date closedAtDate = dateParser.parseDate(PR_DATE_FORMAT, lastPullRequest.get("closed_at").asText(""));
            return closedAtDate.before(startDate);
        }
        return false;
    }
}
