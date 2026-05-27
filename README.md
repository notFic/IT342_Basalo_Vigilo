# Vigilo

Vigilo is a visitor log system for small residential and rental properties such as dormitories, mini-studios, and bed-space facilities. It replaces paper-based guard logbooks with a digital workflow for visitor check-in, active monitoring, check-out, historical lookup, and administrative oversight.

The project currently includes:

- A Spring Boot backend connected to Supabase PostgreSQL
- A React + Vite web dashboard for staff and admin users
- A native Android mobile app
- Project documentation in the `documents/` folder, including the SDD

## Core Features

- Staff login with email/password
- Google sign-in support
- Visitor check-in with ID image upload
- Active visitor dashboard
- Visitor check-out flow
- Historical visitor records
- Admin-only staff management
- Admin-only location management
- Admin-only audit log view
- Admin voiding flow for historical records
- Scheduled and manually triggerable auto-close process for non-extended visits
- Optional SMTP notifications for welcome emails and void notifications

## Repository Structure

```text
IT342_Basalo_Vigilo/
|- backend/
|  \- vigilo/        # Spring Boot API
|- web/              # React + Vite web dashboard
|- mobile/           # Android app
|- documents/        # SDD and related project documents
\- automated_regression_tests.py
```

## Tech Stack

- Backend: Java 17, Spring Boot, Spring Data JPA, Spring Security, Spring Mail
- Database: Supabase PostgreSQL
- Web: React 19, TypeScript, Vite
- Mobile: Kotlin, Jetpack Compose, Retrofit

## Prerequisites

Install these before running the project:

- Java 17
- Node.js 18+ and npm
- Android Studio with Android SDK 34 if you want to run the mobile app
- A working Supabase PostgreSQL database

## Backend Setup

Backend path:

```powershell
cd "backend\vigilo"
```

Run the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Default backend URL:

```text
http://localhost:8080
```

The API base path is:

```text
http://localhost:8080/api/v1
```

Uploaded visitor ID images are stored under:

```text
backend/vigilo/uploads/visitor-ids
```

## Web Setup

Web path:

```powershell
cd "web"
```

Install dependencies:

```powershell
npm install
```

Run the development server:

```powershell
npm run dev
```

Build the web app:

```powershell
npm run build
```

Default dev URL:

```text
http://localhost:5173
```

## Mobile Setup

Mobile path:

```powershell
cd "mobile"
```

Open the `mobile` folder in Android Studio and let Gradle sync.

By default, the Android app uses:

```text
http://192.168.1.14:8080/api/v1/
```

You can override the API base URL at build time using the Gradle property `VIGILO_API_BASE_URL`.

Example:

```powershell
.\gradlew.bat assembleDebug -PVIGILO_API_BASE_URL=http://10.0.2.2:8080/api/v1/
```

Notes:

- Use `10.0.2.2` for the Android emulator when the backend is running on your local machine.
- Use your computer's LAN IP if testing on a physical Android device.

## Suggested Run Order

1. Start the backend
2. Start the web app or mobile app
3. Log in using an existing staff/admin account in the database

## Current Environment Notes

- Supabase is used for the PostgreSQL database connection.
- The system supports optional SMTP email sending for:
  - welcome emails when admin creates staff accounts
  - facility manager notifications when an admin voids a record
- The nightly auto-close process is scheduled for `11:59 PM` by default and only affects non-extended active visits.

## Documentation

Project documents are in:

```text
documents/
```

Notable files:

- `SDD_Vigilo_Basalo.pdf`
- `SDD_Vigilo_Basalo (changes made).docx`

## Troubleshooting

### Backend will not start

- Confirm the Supabase JDBC URL, username, and password are correct
- Confirm Java 17 is installed
- Check whether port `8080` is already in use

### Web cannot reach the backend

- Confirm the backend is running on `http://localhost:8080`
- Confirm `VITE_API_BASE_URL` points to the correct API base path
- Restart Vite after changing `.env`

### Mobile cannot connect

- Do not use `localhost` directly from Android
- Use `10.0.2.2` for the emulator
- Use your machine's LAN IP for a physical device

## Status

This repository is an active school project and still being aligned with the SDD across backend, web, and mobile surfaces.
