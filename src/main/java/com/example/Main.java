package com.example;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

public class Main {



    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, ParseException {

        GitHubApi gitHubApi;

        if (args.length == 1) {
            gitHubApi = new GitHubApi(args[0]);
        } else if (args.length == 2) {
            gitHubApi = new GitHubApi(args[0], args[1]);
        } else if (args.length == 3) {
            gitHubApi = new GitHubApi(args[0], args[1], args[2]);
        } else {
            System.out.println("Invalid arguments provided. Please use:");
            System.out.println("REQUIRED:");
            System.out.println("1. 1 argument: GitHub token (get all PRs)");
            System.out.println("OPTIONAL:");
            System.out.println("2. 2 arguments: GitHub token, startDate (get PRs after startDate)");
            System.out.println("3. 3 arguments: GitHub token, startDate, endDate (get PRs between startDate and endDate)");
            return;
        }

        // Get team members' logins

        List<String> logins = gitHubApi.getTeamMembers();

        // Fetch all pull requests (excluding those created by "dependabot" and with state "closed")
        List<JsonNode> pullRequests = gitHubApi.getPullRequests(logins);

        // Check if there are any pull requests
        if (pullRequests.isEmpty()) {
            System.out.println("No pull requests found for the specified users.");
            System.exit(0);
        }

        // Print information for each pull request
        for (JsonNode pullRequest : pullRequests) {
            gitHubApi.printPullRequestInfo(pullRequest);
        }
    }
}
