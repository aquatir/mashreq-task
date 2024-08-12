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

# Running for development

- Run `./gradlew app:generateJooqClasses` to generate jooq classes. Make sure `app/build/generated-jooq` is available as
  source set (should be by default with gradle)
- Run tests `./gradlew app:test`

# Assumptions

- Didn't implement `As a user, I should be able to see meeting rooms available by giving the time range.` part of
  assignment, currently the implementation return all available intervals instead.
- Handling rooms as enum is okay (provided no extra rooms are expected) see comment on `RoomName` class.
- Handling maintenance intervals as hardcoded values is okay, see comment in `BookingService`
- No serious load is expected => pessimistic locking on DB is fast/good enough.
    - Assumes postgres and uses transaction-level advisory locks to implement locking due to simplicity of
      implementation
- Handling very last interval as `23:59` is okay, due to a requirement of handling just 1 day.

# Stack

- Kotlin 2.0
- jooq for DB layer
- Spring 3
