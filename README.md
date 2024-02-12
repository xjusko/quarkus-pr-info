# GitHub API Project

This project is a Java application that interacts with the GitHub API to retrieve information about pull requests.

## Building the Project

To build the project, make sure you have [Maven](https://maven.apache.org/) installed. Then, run the following command:

```bash
mvn clean install
```

This will compile the code, run tests, and create an executable JAR file in the `target` directory.

## Prerequisites

Before running the application, you need to obtain a GitHub token with the "read:org" permission. Follow the steps below to create a token:

1. **Login to GitHub**

2. **Navigate to Settings:**
   Click on your profile picture in the top right corner of GitHub and select "Settings" from the dropdown menu.

3. **Access Developer Settings:**
   In the left sidebar, click on "Developer settings."

4. **Generate a New Token:**
   - Click on "Personal access tokens" under "Access tokens."
   - Under "Select scopes," ensure that only the "read:org" permission is selected.
   - Click "Generate token."

5. **Copy the Token**

## Running the Application

After building the project, you can run the application using the generated JAR file. Use the following command:

```bash
java -jar target/quarkus-pr-info.jar <GitHub-token> [<startDate> <endDate>]
```

### Arguments:

- `<GitHub-token>`: Your GitHub personal access token.
- `<startDate>`: Optional. Start date in the format dd-MM-yyyy (e.g., 25-02-2022).
- `<endDate>`: Optional. End date in the format dd-MM-yyyy (e.g., 25-02-2022).

### Examples:

1. **Get all pull requests:**

    ```bash
    java -jar target/quarkus-pr-info.jar <GitHub-token>
    ```

2. **Get pull requests after a specific start date:**

    ```bash
    java -jar target/quarkus-pr-info.jar <GitHub-token> 20-02-2022
    ```

3. **Get pull requests between a start and end date:**

    ```bash
    java -jar target/quarkus-pr-info.jar <GitHub-token> 01-01-2022 01-01-2023
    ```
