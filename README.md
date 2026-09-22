# Instant Messaging App

![Java 21](https://img.shields.io/badge/Java-21-blue.svg)
![Vert.x](https://img.shields.io/badge/Eclipse%20Vert.x-5.0.0-purple)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED?logo=docker&logoColor=white)

A secure web messaging platform emphasizing backend architecture robustness and application security.

This project implements a reactive backend using **Java 21** and **Eclipse Vert.x**. The goal is to combine software engineering and systems security through concrete implementations (mitigating user enumeration vulnerabilities, BCrypt password hashing, and least-privilege rootless execution). The application is containerized via a **multi-stage Dockerfile** to optimize the runtime footprint. All security events are centralized and monitored in real-time through the deployment of the **ELK stack** (Elasticsearch, Logstash, Kibana).

## Technical Stack
* **Backend:** Java 21, Eclipse Vert.x, Maven
* **Frontend:** FreeMarker, Bootstrap, Internationalization (i18n)
* **Infrastructure & Deployment:** Docker, Docker Compose (Multi-stage build)
* **Observability & Quality:** Elastic Stack (ELK), SonarQube, JaCoCo

## Prerequisites
* [Docker](https://docs.docker.com/get-docker/) and Docker Compose installed on your host machine.

## Installation & Deployment

1. **Clone the repository**
   ```bash
   git clone [https://github.com/Chaima-Souidi/instant-messaging-app.git](https://github.com/Chaima-Souidi/instant-messaging-app.git)
   cd instant-messaging-app
   ```

2. **Configure the environment**
   Copy the example configuration file to create your local configuration:
   ```bash
   cp conf/config.example.json conf/config.json
   ```

3. **Launch the infrastructure (App + ELK Stack)**
   Navigate to the observability directory and start the containerized deployment:
   ```bash
   cd observability-stack
   docker compose up --build -d
   ```

4. **Access the interfaces**
   * **Web Application:** `http://localhost:8080`
   * **Kibana (Security Monitoring):** `http://localhost:5601`

## Stopping the Infrastructure
To gracefully stop the containers without losing data:
```bash
docker compose stop
```
To destroy the containers and virtual networks (add `-v` to remove persistent volumes):
```bash
docker compose down
```
