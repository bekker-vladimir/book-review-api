# Book Review API
> 🔗 **Looking for the Frontend?** This is the backend API. For the React user interface, please refer to the [Client Application](https://github.com/bekker-vladimir/book-review-client/tree/main).

A robust backend service for a Book Review application, built with Java  Spring Boot. It provides a comprehensive RESTful API for managing books, reviews, users, and authentication.

## Project Overview

This project serves as the backend API for the Book Review application. It handles core business logic, user authentication, data persistence, and background tasks. The system allows users to view books, post reviews and reports, while administrators can manage the book catalog and oversee user-submitted content.

## Tech Stack

The application is built on a modern, scalable technology stack:

*   **Java 21**: The core programming language.
*   **Spring Boot 3.3.5**: The foundational framework, utilizing modules like Data JPA, Web, Security, Validation, and AMQP.
*   **PostgreSQL**: The primary relational database for persistent data storage.
*   **Redis**: Used as an in-memory data store, specifically for caching and managing token blacklists for authentication.
*   **RabbitMQ**: A message broker used for handling asynchronous background tasks (e.g., email notifications).
*   **Docker & Docker Compose**: Containerization for simple and consistent environment setup and deployment.
*   **JWT (JSON Web Tokens)**: Used for secure, stateless user authentication.
*   **Maven**: Dependency management and build tool.

## Prerequisites

To run this application, ensure you have the following installed on your system:

*   **Docker Desktop** (or Docker Engine + Docker Compose)
*   **Java 21** (Required if running outside of Docker or running tests)
*   **Python 3.x** (Optional, only required if you want to use the seed script)

## Environment Variables

Before running the application, you need to set up your environment variables. 
Create a `.env` file in the root directory and configure the following properties:

```env
# Database Settings
DB_URL=jdbc:postgresql://localhost:5433/BookReviewDB
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Security
JWT_SECRET=your_super_secret_jwt_key_here_must_be_long_enough

# RabbitMQ Credentials (default is usually guest/guest)
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Email / SMTP Settings
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

## Instructions

The easiest and recommended way to run the application is using Docker Compose. It will automatically start the database, Redis, and RabbitMQ.

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd <repository-directory>
    ```

2.  **Start the services using Docker Compose:**<br>
    Ensure Docker is running, then execute the following command in the root directory:
    ```bash
    docker-compose up -d
    ```
    *   The `-d` flag runs the containers in the background (detached mode).

3.  **Run the Spring Boot application:**<br>
    You can run the application directly from your IDE or use Maven:
    ```bash
    mvn spring-boot:run
    ```

4.  **Access the API:**
    *   The main API will be available at: `http://localhost:8080`
    *   Swagger UI (API Documentation) is accessible at: `http://localhost:8080/swagger-ui.html`
    *   RabbitMQ Management Interface: `http://localhost:15672` (Default credentials often `guest`/`guest` unless overridden by environment variables)
    *   RedisInsight: `http://localhost:5540`

5.  **Stopping the services:**<br>
    To stop the application and its dependencies, run:
    ```bash
    docker-compose down
    ```
    If you want to clear the database volumes as well, you can run `docker-compose down -v`.

---

## Database Seeding (`seed.py`)

To help you get started quickly with test data, the project includes a Python script (`seed.py`) that populates the database with initial users, books, and reviews. It fetches realistic book data from OpenLibrary and uses Faker to generate mock user details and reviews.

### Purpose
The seed script automates the creation of entities so you can immediately test the UI or API endpoints without manually inserting data.

### How to Run

1.  Ensure you have Python 3 installed.
2.  Install the required Python dependencies:
    ```bash
    pip install -r requirements-seeder.txt
    ```
3.  Run the seed script. The `--all` flag will generate users, fetch books, approve them (as admin), and create reviews:
    ```bash
    python seed.py --all
    ```
