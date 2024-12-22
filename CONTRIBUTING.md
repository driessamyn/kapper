# Contributing to Kapper

Thank you for considering contributing to Kapper!
Contributions, whether big or small, are always welcome.
This guide will help you get started and ensure a smooth contribution process.

## How Can You Contribute?

- **Report bugs**: If you encounter any issues or strange behavior, please [submit a bug report](#submitting-bug-reports).
- **Suggest new features**: Share your ideas for improving Kapper.
- **Improve documentation**: Help us enhance the README or add examples, troubleshooting tips, or integration tutorials.
- **Fix bugs or implement features**: Jump into the code by fixing an issue or working on a feature from our [roadmap](./README.md#roadmap).

---

## Submitting Bug Reports

If you’ve found an issue or unexpected behavior in Kapper, please create a bug report with the following details:

1. A brief description of the problem.
2. Steps to reproduce the issue.
3. Example code (if applicable).
4. Details about your environment:
    - Operating system
    - Kotlin version
    - Database and driver version
    - JVM version

Adding a PR with a test that covers the bug would be even better!

You can [open a GitHub issue](https://github.com/driessamyn/kapper/issues) to report any bugs.
Please check if the issue already exists before creating one.

---

## Suggesting Features

Got a great idea?
Open a "Feature Request" issue in our [GitHub issue tracker](https://github.com/driessamyn/kapper/issues) and include:

1. A clear description of the feature and its intended purpose.
2. Example scenarios where this feature would be useful.
3. Why the feature would benefit the project or its users.

---

## Development Guidelines

If you would like to contribute code to Kapper, follow the steps below.

### Prerequisites

1. **Fork the Repository**
    - Fork the project repository to your GitHub account by clicking the "Fork" button at the top of the [project page](https://github.com/driessamyn/kapper).

2. **Clone Your Fork**
    - Clone the repository locally:
      ```bash
      git clone https://github.com/<your-username>/kapper.git
      cd kapper
      ```

3. **Set Up the Environment**
    - Ensure you have the following installed on your system:
        - Kotlin (version 2.0 or newer)
        - Java JDK 17 or later
        - Gradle (bundled wrapper is recommended)

    - Run the following command to build the project and ensure everything works:
      ```bash
      ./gradlew build
      ```

4. **Run Tests**
    - Validate that the tests pass before proceeding to make any changes:
      ```bash
      ./gradlew check
      ```

---

### Making Changes

1. **Create a Branch**
    - Always create a new branch for your changes:
      ```bash
      git checkout -b feature/your-feature-name
      ```

2. **Coding Standards**
    - Follow the style and patterns already used in the project.
    - Write clear, concise, and maintainable code.
    - Write unit tests or integration tests for any new functionality.

3. **Commit Guidelines**
    - Make meaningful and small commits.
    - Use descriptive commit messages using the [conventional commits specification](https://www.conventionalcommits.org/en/v1.0.0/):
      ```text
      <type>(scope): description
      
      Example:
      feat(logging): added SLF4J-based logging support
      fix(query-parsing): resolved issue with parsing multiple placeholders
      ```

4. **Add Tests**
    - Ensure all new code is accompanied by tests. If your changes include new features, add test cases that demonstrate their usage.

5. **Run Tests Again**
    - After making changes, run all tests again to ensure nothing is broken:
      ```bash
      ./gradlew check
      ```

---

### Submitting a Pull Request

1. **Push Your Branch**
    - Push your branch to your forked repository:
      ```bash
      git push origin feature/your-feature-name
      ```

2. **Create a Pull Request**
    - Open a Pull Request (PR) from your branch to the `main` branch of the Kapper repository:
        - Go to your fork on GitHub and click "Compare & Pull Request."
        - Write a clear description of your changes, including the problem your code solves or the feature it introduces.

3. **Respond to Feedback**
    - Be responsive to any feedback or requested changes during the PR review process.

---

## Code of Conduct

By contributing, you agree to uphold our [Code of Conduct](./CODE_OF_CONDUCT.md).
Please be respectful and collaborative when interacting with others.

---

## Need Help?

If you run into any trouble or have questions, don’t hesitate to:

- Open a [GitHub discussion](https://github.com/driessamyn/kapper/discussions).
- Reach out via the project’s issue tracker.

Thank you for helping improve Kapper—we’re excited to have you contribute!