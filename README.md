## 🧠 Deep-Dive: Complete Internal Execution Flow (Step-by-Step)

This describes exactly what happens inside the service when a user registers, logs in, and accesses a secured API.

---

### 🔹 A. Registration Flow — `/api/auth/register`

1. **HTTP Request Arrives**

   ```
   POST /api/auth/register
   Content-Type: application/json
   ```

   Body contains user details.

2. **Security Filter Chain**

   * `SecurityConfig` identifies `/api/auth/**` as `permitAll`
   * No authentication required → request allowed to enter controller.

3. **Controller Layer**

   * `AuthController.register()` receives `RegisterRequest`.

4. **Validation**

   * `@Valid` triggers Bean Validation (email, not blank, etc.)
   * If invalid → 400 returned automatically.

5. **Service Layer**

   * `AuthService.register()` is called.

6. **Business Logic**

   * `UserRepository.existsByEmail()` → check duplicates
   * `UserRepository.existsByEmployeeId()` → check duplicates
   * Assign default role if none provided
   * Encrypt password using `BCryptPasswordEncoder`

7. **Database**

   * `userRepository.save(user)` → inserts:

     * user row → `users` table
     * roles → `users_roles` table (ElementCollection)

8. **Response**

   * Returns success message → 200/201

📌 Result → The user exists in DB with hashed password & roles.

---

### 🔹 B. Login Flow — `/api/auth/login` (JWT creation)

1. **HTTP Request Arrives**

   ```
   POST /api/auth/login
   ```

   Body contains email + password.

2. **Security Filter Chain**

   * `/api/auth/**` → permitted → no token required.

3. **Controller Layer**

   * `AuthController.login()` receives `LoginRequest`.

4. **Service Layer**

   * `AuthService.login()` runs:

     ```
     authenticationManager.authenticate(
         new UsernamePasswordAuthenticationToken(email, password)
     )
     ```

5. **Spring Security Authentication Lifecycle**
   a. `DaoAuthenticationProvider` is selected
   b. Calls `CustomUserDetailsService.loadUserByUsername(email)`
   c. `UserRepository.findByEmail(email)` → fetch user + roles from DB
   d. Builds `UserDetails` object — containing:

   * email
   * encrypted password from DB
   * roles

6. **Password Verification**

   * `BCryptPasswordEncoder.matches(rawPassword, encryptedPassword)`
   * If mismatch → `BadCredentialsException`
   * If match → authentication successful

7. **JWT Creation**

   * `jwtService.generateToken(subject, claims)`
   * Subject (`sub`) = email
   * Claims = roles + timestamps
   * Signs using HS256 and secret key

8. **Response**

   * `AuthResponse` returned → includes token & expiry

📌 Result → Client receives a **signed JWT token**.

---

### 🔹 C. Secured API Access — `Authorization: Bearer <token>`

Example protected endpoint: `/api/asset/list` (future microservice).

1. **HTTP Request Arrives**

   ```
   GET /api/asset/list
   Authorization: Bearer <token>
   ```

2. **Security Filter Chain**

   * Matches endpoint → authentication REQUIRED
   * `JwtAuthenticationFilter` executes

3. **JWT Filter Logic**

   * Extracts token from header
   * Calls `jwtService.validateAndParse(token)`

     * Validate signature (HS256)
     * Validate expiry
     * Extract `sub` (email)
   * Loads user using:

     ```
     customUserDetailsService.loadUserByUsername(email)
     ```
   * Builds `UsernamePasswordAuthenticationToken` containing:

     * principal = user details
     * authorities = roles
   * Stores this in: `SecurityContextHolder.getContext().setAuthentication()`

4. **Authorization Check**

   * If request has `@PreAuthorize("hasRole('ROLE_IT_ADMIN')")`
   * Spring checks if `SecurityContext.authentication.authorities` contains required role
   * If not → 403 Forbidden

5. **Controller Execution**

   * Now user is authenticated
   * Controller receives:

     ```
     Authentication authentication
     ```
   * Business logic executed
   * Response returned

📌 Result → Request succeeds only if valid JWT + required roles.

---

### 🔹 D. Lifecycle Summary Diagram (Text Format)

```
REGISTER   → Validation → Save User → 200 OK
LOGIN      → Authenticate (DB + BCrypt) → Generate Token → Return JWT
PROTECTED  → Extract Token → Validate → Load User → Set SecurityContext → Allow/Reject
```

---

### 🔥 Key Guarantees of This JWT Solution

| Concern             | How the system handles it                       |
| ------------------- | ----------------------------------------------- |
| Password protection | BCrypt hashing, never stored in plain text      |
| Token security      | Signed JWT using strong secret key              |
| Statelessness       | No server-side session; token contains identity |
| Role enforcement    | Checked on every request using JWT claims       |
| Performance         | No DB hit if token trusted by gateway (future)  |

---

### 📌 What Will Happen Next (When Microservices Come In)

* JWT generated here will be sent from API Gateway to Asset-Service / Upgrade-Service
* Those services will not store passwords or login logic
* They will **only validate JWT** and get:

  * Email
  * Roles
  * Expiration time
* No cross-service DB calls needed for authentication

This preserves both **security + microservice isolation**.
## 🧠 Amarendra sahoo
