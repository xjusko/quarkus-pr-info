# GitHub API Project

This project is a Java application that interacts with the GitHub API to retrieve information about pull requests.

## Building the Project

To build the project, make sure you have [Maven](https://maven.apache.org/) installed. Then, run the following command:

```bash
mvn clean install
```

This will compile the code, run tests, and create an executable JAR file in the `target` directory.

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
