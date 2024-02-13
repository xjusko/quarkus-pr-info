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

        List<String> logins = gitHubApi.getTeamMembers();
        gitHubApi.run(logins);

    }
}
