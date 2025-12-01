# FocusFortress

A comprehensive productivity and habit-tracking application built with Spring Boot and React. FocusFortress helps users build better habits, track their progress, and discover personalized content based on their interests.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)
![React](https://img.shields.io/badge/React-18.2.0-blue.svg)

## 📸 Screenshots

### Authentication
<!-- Add screenshot of login/register page here -->
![Login Page](screenshots/login.png)
*Secure authentication with "Remember Me" functionality*

### Dashboard
<!-- Add screenshot of main dashboard here -->
![Dashboard](screenshots/dashboard.png)
*Main dashboard overview with quick access to all features*

### Habit Tracking
<!-- Add screenshot of habits page here -->
![Habits](screenshots/habits.png)
*Track your habits and sync with Google Calendar*

### Counter Management
<!-- Add screenshot of counters page here -->
![Counters](screenshots/counters.png)
*Manage multiple counters for various activities*

### Media Library
<!-- Add screenshot of media library here -->
![Media Library](screenshots/media-library.png)
*Personalized YouTube content based on your interests*

### User Profile & Settings
<!-- Add screenshot of profile/settings page here -->
![Settings](screenshots/settings.png)
*Customize your profile and manage application settings*

## ✨ Features


### 🎯 Habit Management
- Create and track daily habits
- Set habit duration (e.g., 21-day challenges)
- Visual progress tracking
- **Google Calendar integration** for automatic scheduling

### 📊 Counter System
- Track multiple activities with customizable counters
- Increment/decrement functionality
- Real-time counter updates

### 📺 Personalized Media Library
- **Interest-based content curation**
- Automatic YouTube video recommendations
- Category and subcategory organization
- Save and manage your favorite content

## 🏗️ Architecture

### Backend (Spring Boot)
- **RESTful API** design
- **Spring Security** for authentication and authorization
- **JPA/Hibernate** for database operations
- **H2 Database** (development) / **MySQL** (production ready)
- **JWT** token-based authentication
- **YouTube Data API v3** integration

### Frontend (React)
- **React 18.2** with functional components and hooks
- **React Router** for navigation
- **Axios** for API communication
- **Context API** for state management
- **Responsive design** with custom styling

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.4.2**
  - Spring Web
  - Spring Data JPA
  - Spring Security 6.4.3
  - Spring Validation
- **JWT (JJWT) 0.11.5**
- **Lombok 1.18.26**
- **H2 Database 2.3.232**
- **MySQL Connector 9.2.0**
- **Maven** (Build tool)

### Frontend
- **React 18.2.0**
- **React Router DOM 6.20.0**
- **Axios 1.6.0**
- **Lucide React** (Icons)
- **Emoji Picker React**

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Node.js 16+ and npm
- Maven 3.6+
- (Optional) MySQL 8.0+ for production

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/FocusFortress.git
   cd FocusFortress
   ```

2. **Configure application properties**
   
   Edit `backend/src/main/resources/application.properties`:
   ```properties
   # Update JWT secret for production
   jwt.secret=your-secret-key-here
   
   # Configure YouTube API key
   youtube.api.key=your-youtube-api-key
   
   # For MySQL (optional)
   spring.datasource.url=jdbc:mysql://localhost:3306/focusfortress
   spring.datasource.username=your-username
   spring.datasource.password=your-password
   ```

3. **Build and run the backend**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

   The backend will start at `http://localhost:8080`

### Frontend Setup

1. **Install dependencies**
   ```bash
   cd frontend
   npm install
   ```

2. **Configure API endpoint**
   
   Edit `frontend/src/services/api.js` if needed (default: `http://localhost:8080`)

3. **Start the development server**
   ```bash
   npm start
   ```

   The frontend will start at `http://localhost:3000`

## 📁 Project Structure

```
FocusFortress/
├── backend/
│   └── src/
│       └── main/
│           ├── java/com/focusfortress/
│           │   ├── config/          # Security, CORS, etc.
│           │   ├── controller/      # REST endpoints
│           │   ├── dto/             # Data Transfer Objects
│           │   ├── exception/       # Custom exceptions
│           │   ├── mapper/          # Entity-DTO mappers
│           │   ├── model/           # JPA entities
│           │   ├── repository/      # Data access layer
│           │   ├── security/        # JWT, authentication
│           │   ├── service/         # Business logic
│           │   └── youtube/         # YouTube API integration
│           └── resources/
│               └── application.properties
├── frontend/
│   └── src/
│       ├── components/
│       │   ├── auth/         # Login, Register
│       │   ├── counters/     # Counter management
│       │   ├── dashboard/    # Main dashboard
│       │   ├── habits/       # Habit tracking
│       │   ├── layout/       # App layout
│       │   ├── media/        # Media library
│       │   └── settings/     # User settings
│       ├── context/          # React Context
│       └── services/         # API services
├── architecture_diagrams/    # C4 model diagrams
└── pom.xml
```

## 🔑 API Endpoints

### Authentication
- `POST /api/users/register` - Register new user
- `POST /api/users/login` - User login
- `GET /api/users/me` - Get current user

### Habits
- `GET /api/habits` - Get all user habits
- `POST /api/habits` - Create new habit
- `PUT /api/habits/{id}` - Update habit
- `DELETE /api/habits/{id}` - Delete habit

### Counters
- `GET /api/counters` - Get all user counters
- `POST /api/counters` - Create new counter
- `PUT /api/counters/{id}` - Update counter
- `DELETE /api/counters/{id}` - Delete counter

### Media Library
- `GET /api/media-library` - Get user's media items
- `POST /api/media-library` - Add media item
- `DELETE /api/media-library/{id}` - Remove media item

### Interests & Categories
- `GET /api/interests` - Get all available interests
- `POST /api/user-interests` - Set user interests
- `GET /api/categories` - Get category tree


---

**Happy Habit Building :)**

