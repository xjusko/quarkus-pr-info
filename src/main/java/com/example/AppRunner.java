package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public class AppRunner {
    public PullRequestProcessor initializePullRequestProcessor(String[] args) throws URISyntaxException, IOException, InterruptedException {
        PullRequestProcessor pullRequestProcessor;

        if (args.length == 1) {
            pullRequestProcessor = new PullRequestProcessor(args[0]);
        } else if (args.length == 2) {
            pullRequestProcessor = new PullRequestProcessor(args[0], args[1]);
        } else if (args.length == 3) {
            pullRequestProcessor = new PullRequestProcessor(args[0], args[1], args[2]);
        } else {
            System.out.println("Invalid arguments provided. Please use:");
            System.out.println("REQUIRED:");
            System.out.println("1 argument: GitHub token (get all PRs)");
            System.out.println("OPTIONAL:");
            System.out.println("2 arguments: GitHub token, startDate (get PRs after startDate)");
            System.out.println("3 arguments: GitHub token, startDate, endDate (get PRs between startDate and endDate)");
            System.exit(1);
            return null; // This line is unreachable, but added to satisfy the compiler
        }

        return pullRequestProcessor;
    }

    public void execute(String[] args) throws URISyntaxException, IOException, InterruptedException {
        PullRequestProcessor pullRequestProcessor = initializePullRequestProcessor(args);

        try {
            pullRequestProcessor.process();
        } catch (URISyntaxException | IOException | InterruptedException | ParseException e) {
            System.out.println("An error occurred: " + e.getMessage());
            System.exit(1);
        }
    }

}
