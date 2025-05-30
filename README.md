# Pricing Microservice

A Spring Boot microservice that provides a RESTful API for retrieving pricing information for retail products.

## Features

- **RESTful API** for retrieving pricing information with pagination support
- **Pricing business rules implementation**:
  - Marking prices with overlapping validity ranges and different values as "overlapped"
  - Merging prices with overlapping validity ranges and equal values
- **In-memory H2 database** for data storage
- **In-memory cache** for improved performance
- **Comprehensive test coverage** for price validity range logic
- **Swagger/OpenAPI documentation**
- **Error handling** with appropriate HTTP status codes

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Quick Start

### Building the Application

To build the application, run:

```bash
mvn clean package
```

### Running the Application

To run the application, execute:

```bash
java -jar target/pricing-service-0.0.1-SNAPSHOT.jar
```

Alternatively, you can use the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

## API Endpoints

### Get Prices

```
GET /pricing/v1/prices/{storeID}/{articleID}?page=1&pageSize=3
```

Example request:

```
GET http://localhost:8083/pricing/v1/prices/7001/1000102674?page=1&pageSize=3
```

Example response:

```json
{
  "generated_date": "2025-05-14T14:15:10Z",
  "article": "1000102674",
  "store": "7001",
  "meta": {
    "page": 1,
    "size": 3
  },
  "properties": {
    "uom": "EA",
    "description": "WH Halifax Passage Lever in Satin Nickel",
    "brand": "Weiser",
    "model": "9GLA1010"
  },
  "prices": [
    {
      "type": "retail",
      "subtype": "regular",
      "currency": "CAD",
      "amount": 30.0,
      "valid_from": "2023-12-31T23:59:59Z",
      "valid_to": "9999-12-31T23:59:59Z",
      "overlapped": false
    },
    {
      "type": "retail",
      "subtype": "discounted",
      "currency": "CAD",
      "amount": 27.0,
      "valid_from": "2023-12-21T23:59:59Z",
      "valid_to": "2025-12-31T23:59:58Z",
      "overlapped": true
    },
    {
      "type": "retail",
      "subtype": "discounted",
      "currency": "CAD",
      "amount": 26.5,
      "valid_from": "2023-12-21T23:59:59Z",
      "valid_to": "2025-12-25T23:59:58Z",
      "overlapped": true
    }
  ]
}
```

### Clear Cache (Admin)

```
POST /pricing/v1/prices/admin/clear-cache
```

## Business Rules Implementation

The API implements two key business rules regarding price validity ranges:

1. **Marking Overlapping Prices**:
   - Prices with overlapping validity ranges AND different price values are marked with `"overlapped": true`
   - Example: Two discounted prices with the same validity range but different amounts will be marked as overlapped

2. **Merging Prices**:
   - Prices with overlapping validity ranges AND equal price values are merged into a single price
   - The merged price has the largest combined validity range (earliest start date to latest end date)
   - Example: Two special prices with the same amount and overlapping validity ranges will appear as one price

## Test Data

The application is pre-loaded with several test scenarios to demonstrate the business rules:

1. **Article 1000102674 (Store 7001)**:
   - Contains overlapping discounted prices with different amounts
   - These prices should be marked as `"overlapped": true`

2. **Article 1000203345 (Store 7001)**:
   - Contains non-overlapping prices
   - None of these prices should be marked as overlapped

3. **Article 2000000001 (Store 8001)**:
   - Contains various test scenarios demonstrating different overlap cases
   - Access this article to see examples of both overlapped prices and merged prices

## API Documentation

OpenAPI/Swagger documentation is available at:

- Swagger UI: http://localhost:8083/pricing/swagger-ui.html
- OpenAPI Specification: http://localhost:8083/pricing/v3/api-docs

## H2 Database Console

The H2 database console is available at:

```
http://localhost:8083/pricing/h2-console
```

Connection details:
- JDBC URL: `jdbc:h2:mem:pricingdb`
- Username: `sa`
- Password: `password`

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── pricingservice/
│   │               ├── config/
│   │               │   ├── DataLoader.java
│   │               │   └── OpenApiConfig.java
│   │               ├── controller/
│   │               │   └── PriceController.java
│   │               ├── dto/
│   │               │   ├── ErrorResponseDTO.java
│   │               │   ├── MetaDTO.java
│   │               │   ├── PriceDTO.java
│   │               │   ├── PriceResponseDTO.java
│   │               │   └── PropertiesDTO.java
│   │               ├── exception/
│   │               │   ├── GlobalExceptionHandler.java
│   │               │   └── PriceNotFoundException.java
│   │               ├── model/
│   │               │   ├── Article.java
│   │               │   └── Price.java
│   │               ├── repository/
│   │               │   ├── ArticleRepository.java
│   │               │   └── PriceRepository.java
│   │               ├── service/
│   │               │   └── PriceService.java
│   │               └── PricingServiceApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/
            └── example/
                └── pricingservice/
                    ├── controller/
                    │   └── PriceControllerIntegrationTest.java
                    └── service/
                        └── PriceServiceTest.java
```

## Error Handling

When prices are not found, the API returns a 404 status code with the following response format:

```json
{
  "type": "Not_Found",
  "title": "Unavailable prices",
  "status": 404,
  "detail": "No prices were found for a given request"
}
```

## Caching

The service uses an in-memory cache based on the Java `ConcurrentHashMap` implementation. Responses are cached based on the store ID, article ID, page, and page size. The cache can be cleared using the admin endpoint.

## Testing

Run the tests with:

```bash
mvn test
```

The test suite includes:

- **Unit tests** for the pricing business logic
- **Integration tests** for the REST API endpoints

## Troubleshooting

If you encounter issues:

1. Check the application logs for detailed error messages
2. Verify the H2 database connection settings
3. Ensure you're using the correct URL format for API requests
4. Clear the cache if you suspect stale data is being returned

For further assistance, please contact the development team.
