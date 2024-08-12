# Running the app

Run Postgres

```
docker-compose up 
```

In a separate terminal (or after running above with `&`) run the app.

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

# Assumptions

- Handling rooms as enum is okay, see comment on `RoomName` class.
- Handling maintenance intervals as hardcoded values is okay, see comment in `BookingService`
- No serious load is expected => total pessimistic locking is fast/good enough.
    - Assumes postgres and uses transaction-level advisory locks to implement locking due to simplicity
- 
