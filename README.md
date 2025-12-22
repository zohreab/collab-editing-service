#  Collaborative Editing System  
## A Microservices-Based Real-Time Document Collaboration Platform

---

##  Overview

This project implements a **collaborative document editing system** inspired by platforms such as **Google Docs** and **Overleaf**.  
It is designed using a **microservice-based architecture** and demonstrates real-time collaboration, secure authentication, and version control in a distributed system.

The system enables multiple users to collaboratively edit documents in real time while maintaining full version history and fine-grained access control.

---

## System Architecture

The system is composed of independent microservices communicating via **REST APIs** and **WebSockets**, coordinated through an **API Gateway**.

```
Frontend (React)
        |
        v
API Gateway (Spring Cloud Gateway)
        |
        +------------------+
        |                  |
        v                  v
User Service        Document Service
                           |
                           v
                    Version Service
```

---

##  Microservices Overview

###  User Service

**Responsibilities**
- User registration
- User authentication using JWT
- Password update

**Technologies**
- Spring Boot
- Spring Security
- JPA
- PostgreSQL (production)
- H2 (testing)

---

###  Document Service

**Responsibilities**
- Create and manage documents
- Real-time collaborative editing
- Share documents with other users
- Revoke collaborator access
- Delete documents

**Technologies**
- Spring Boot
- REST APIs
- WebSocket (STOMP)
- JPA

---

### Version Service

**Responsibilities**
- Maintain document version history
- Store full document snapshots
- Restore previous versions
- Track user contributions

**Technologies**
- Spring Boot
- REST APIs
- JPA

---

###  API Gateway

**Responsibilities**
- Centralized request routing
- JWT validation
- User identity propagation via headers
- WebSocket routing support

**Technologies**
- Spring Cloud Gateway

---

##  Frontend

The frontend is implemented using **React** and provides a modern, user-friendly interface.

**Features**
- User authentication pages
- Document dashboard
- Real-time collaborative editor
- Active user indicators
- Version history sidebar


---

##  Testing

Each microservice includes **automated unit and integration tests**.

**Testing Stack**
- JUnit 5
- Mockito
- Spring MockMvc
- H2 in-memory database (test profile)

**Test Coverage**
- User Service
- Document Service
- Version Service
- API Gateway

All services successfully pass tests using:
- `mvn test`
- IntelliJ IDEA “Run with Coverage”

---

## Configuration

### Environment Variables

Sensitive configuration values are provided via environment variables and are **not hard-coded**.

```bash
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

---

### Spring Profiles

- **default**: PostgreSQL database
- **test**: H2 in-memory database

Each service includes an `application-test.yml` for safe and isolated testing.

---

##  Running the Project

### Backend Services

Run each service from its directory:

```bash
mvn spring-boot:run
```

| Service | Port |
|--------|------|
| API Gateway | 8080 |
| User Service | 8081 |
| Document Service | 8082 |
| Version Service | 8083 |

---

### Frontend

```bash
cd collab-frontend
npm install
npm run dev
```

Frontend will be available at:

```
http://localhost:5173
```

---

## User Interface 



### Register Page

<img width="1889" height="829" alt="Register Page" src="https://github.com/user-attachments/assets/2eeaadb1-44f2-4bed-966a-0e2de0ca77a4" />

---

### Login Page

<img width="1874" height="807" alt="Login Page" src="https://github.com/user-attachments/assets/af3857cd-695f-4e07-8b6d-978d0a547c7f" />

---

### Dashboard Page

<img width="1803" height="707" alt="Dashboard Page" src="https://github.com/user-attachments/assets/7e1e8cf8-0eb8-4feb-93c6-a5f8487b212d" />

---

### Change Password

<img width="1814" height="802" alt="Change Password" src="https://github.com/user-attachments/assets/daae0f55-d05e-47d0-8b13-39da388b7f00" />

---

### Create Document

<img width="1887" height="783" alt="Create Document" src="https://github.com/user-attachments/assets/f92708e8-96ab-47ed-a6ea-cbcf30a7941c" />

---

### Share Document

<img width="1885" height="782" alt="Share Document" src="https://github.com/user-attachments/assets/420a75be-2375-4f46-9754-70dc6acc0b83" />

<img width="1888" height="792" alt="Share Document 2" src="https://github.com/user-attachments/assets/3c17bbe4-ce9f-4f16-a343-26d13fe6d225" />

---

### Document Shared With Another User

<img width="1845" height="764" alt="Shared Document" src="https://github.com/user-attachments/assets/dd54c30d-d996-47d0-8acb-4beba02358a2" />

---

### Collaborative Editing

<img width="1863" height="802" alt="Collab Editing 1" src="https://github.com/user-attachments/assets/03ee3e8b-46b8-42de-b2a4-88e012f38ca2" />

<img width="1862" height="790" alt="Collab Editing 2" src="https://github.com/user-attachments/assets/af7fc641-fd75-471d-8bf7-185e6f4949fd" />

---

### Version History

<img width="1840" height="791" alt="Version History" src="https://github.com/user-attachments/assets/ff32cc7c-e8a4-4c8b-af64-eb31a507c53d" />

---

### Deleting the Document

<img width="1845" height="781" alt="Delete Document" src="https://github.com/user-attachments/assets/50d82623-772e-44c9-aa40-1201a113ef88" />













---


