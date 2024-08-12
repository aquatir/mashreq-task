create table BOOKINGS(
    id UUID             PRIMARY KEY NOT NULL,
    room_name           TEXT NOT NULL,
    booking_size        INT NULL,
    booking_day         TEXT NOT NULL,
    from_time           TEXT NOT NULL,
    to_time             TEXT NOT NULL
);
