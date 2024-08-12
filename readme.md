# Running the app

Run Postgres

```
docker-compose up &
```

Run the app

```
./gradlew app:bootRun
```

## Executing requests

### Create bookings

```bash
curl -X POST -H "Content-Type: application/json" -d '{
    "date": "'$(date +%Y-%m-%d)'",
    "numberOfPeople": 8,
    "booking": {
        "from": "07:00",
        "to": "07:30"
    }
}' "http://localhost:8080/booking/"
```

### Get available bookings

```bash
curl -X GET -H "Content-Type: application/json" http://localhost:8080/booking/
```
