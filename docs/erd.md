# ERD

```dbml
Table roles {
  role_id uuid [pk]
  code varchar [unique]
  description varchar
  created_at timestamptz
}

Table users {
  user_id uuid [pk]
  full_name varchar
  email varchar [unique]
  phone varchar
  password_hash varchar
  status varchar
  email_verified boolean
  created_at timestamptz
  updated_at timestamptz
}

Table user_roles {
  user_id uuid [ref: > users.user_id]
  role_id uuid [ref: > roles.role_id]
}

Table hotels {
  hotel_id uuid [pk]
  name varchar
  city varchar
  province varchar
  address varchar
  is_deleted boolean
}

Table rooms {
  room_id uuid [pk]
  hotel_id uuid [ref: > hotels.hotel_id]
  name varchar
  room_type varchar
  capacity int
  price_per_night numeric
  status varchar
  average_rating numeric
  review_count int
  is_deleted boolean
}

Table room_images {
  image_id uuid [pk]
  room_id uuid [ref: > rooms.room_id]
  image_url varchar
  is_primary boolean
}

Table amenities {
  amenity_id uuid [pk]
  name varchar [unique]
}

Table room_amenities {
  room_id uuid [ref: > rooms.room_id]
  amenity_id uuid [ref: > amenities.amenity_id]
}

Table bookings {
  booking_id uuid [pk]
  booking_code varchar [unique]
  user_id uuid [ref: > users.user_id]
  room_id uuid [ref: > rooms.room_id]
  check_in date
  check_out date
  guest_count int
  status varchar
  price_per_night_snapshot numeric
  total_amount numeric
  expires_at timestamptz
}

Table payments {
  payment_id uuid [pk]
  booking_id uuid [ref: > bookings.booking_id]
  provider varchar
  order_id varchar [unique]
  amount numeric
  status varchar
  idempotency_key varchar [unique]
}

Table refund_requests {
  refund_id uuid [pk]
  booking_id uuid [ref: > bookings.booking_id]
  payment_id uuid [ref: > payments.payment_id]
  amount numeric
  percentage int
  status varchar
  idempotency_key varchar [unique]
}

Table reviews {
  review_id uuid [pk]
  booking_id uuid [unique, ref: > bookings.booking_id]
  user_id uuid [ref: > users.user_id]
  room_id uuid [ref: > rooms.room_id]
  rating int
  content varchar
}

Table review_images {
  review_image_id uuid [pk]
  review_id uuid [ref: > reviews.review_id]
  image_url varchar
}

Table email_jobs {
  job_id uuid [pk]
  booking_id uuid [ref: > bookings.booking_id]
  user_id uuid [ref: > users.user_id]
  event_type varchar
  recipient varchar
  status varchar
}

Table email_logs {
  log_id uuid [pk]
  job_id uuid [ref: > email_jobs.job_id]
  booking_id uuid [ref: > bookings.booking_id]
  event_type varchar
  recipient varchar
  status varchar
}

Table audit_logs {
  audit_id uuid [pk]
  actor_user_id uuid [ref: > users.user_id]
  action varchar
  entity_type varchar
  entity_id uuid
}

Table login_logs {
  login_log_id uuid [pk]
  user_id uuid [ref: > users.user_id]
  email varchar
  success boolean
}

Table jwt_token_blacklist {
  token_id uuid [pk]
  user_id uuid [ref: > users.user_id]
  token_hash varchar [unique]
  expires_at timestamptz
}
```
