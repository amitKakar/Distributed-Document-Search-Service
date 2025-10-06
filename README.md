# Multi-Tenant Document Search Demo

A Spring Boot demo application for multi-tenant document search with per-tenant rate limiting, in-memory caching, and PostgreSQL full-text search. Designed for easy local testing and extensibility.

## Technology Stack
- Java 11
- Spring Boot (Web, Data JPA, Actuator)
- PostgreSQL
- Caffeine (in-memory cache)
- Lombok
- Maven

## How to Run
1. **Clone the repository** and navigate to the project directory.
2. **Set up PostgreSQL**:
   - Create a database named `documents_db`.
   - Create a user `postgres` with password `postgres` (or update `src/main/resources/application.properties` accordingly).
3. **Build and run the application**:
   ```sh
   mvn spring-boot:run
   ```
   The app will start on `http://localhost:8080`.

## REST Endpoints

### Create Document
```
POST /documents
Headers: X-Tenant-Id: tenant1
Body: { "title": "Doc Title", "content": "Some text" }
```
**Response:**
```
201 Created
{
  "id": 1,
  "tenantId": "tenant1",
  "title": "Doc Title",
  "content": "Some text",
  "createdAt": "2025-10-06T12:34:56"
}
```

### Search Documents
```
GET /search?q=foo&page=0&size=10
Headers: X-Tenant-Id: tenant1
```
**Response:**
```
200 OK
{
  "content": [ ... ],
  "totalElements": 1,
  "totalPages": 1,
  ...
}
```

### Get Document by ID
```
GET /documents/1
Headers: X-Tenant-Id: tenant1
```
**Response:**
```
200 OK
{ ...document... }
```

### Delete Document
```
DELETE /documents/1
Headers: X-Tenant-Id: tenant1
```
**Response:**
```
204 No Content
```

### Health Check
```
GET /actuator/health
```
**Response:**
```
200 OK
{
  "status": "UP",
  "components": { "db": { "status": "UP" }, ... }
}
```

## Multi-Tenancy Approach
- Every request must include the `X-Tenant-Id` header.
- The application enforces tenant scoping at the API and database query level.
- All data access and cache keys are isolated per tenant.

## Caching
- Search results are cached in-memory (Caffeine) per tenant/query/page/size.
- Cache is invalidated automatically on document creation or deletion for a tenant.

## Rate Limiting
- Each tenant is limited to 30 requests per minute (sliding window).
- Exceeding the limit returns HTTP 429 with a JSON error.

## Health Check
- Use `/actuator/health` to verify application and database status.

## Extensibility & Limitations
- **Demo only:** Uses PostgreSQL ILIKE for text search (not Elasticsearch).
- **In-memory cache and rate limiting:** Not suitable for distributed/multi-instance deployments.
- **No authentication:** Only tenant header is enforced.
- Easily extensible for production use with distributed cache/rate limiting and real authentication.

## License
MIT License

## Author
Amit Kakar

