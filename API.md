# Linkr Admin Security — Full Implementation Reference

## What We're Building
TOTP (Google Authenticator) + Rate Limiting + HTTPS for the `/admin` page.
No passwords. No IP restrictions. Works on any device, any network.

---

## How It Works (End to End)

```
You visit /admin
→ Spring Security intercepts — are you authenticated?
→ No → redirect to /admin/login
→ Open Google Authenticator on your phone
→ App shows a 6-digit code (changes every 30 seconds)
→ You type it in
→ Spring verifies using the same secret key + current time
→ Correct → access granted
→ Wrong 5 times → locked out for 30 minutes
```

The code is generated mathematically from a **shared secret key + current time**.
Both your app and Google Authenticator use the same formula. No internet needed.

---

## Tools Needed

| Tool | Purpose | Cost |
|---|---|---|
| Spring Security | Intercepts requests, handles authentication | Free |
| `java-otp` | Generates and verifies TOTP codes | Free |
| Google Authenticator | Your phone app to get codes | Free |
| Bucket4j | Rate limiting (5 attempts, 30min lockout) | Free |
| Let's Encrypt | HTTPS certificate (for deployment only) | Free |

---

## Dependencies to Add to pom.xml

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- TOTP / Google Authenticator -->
<dependency>
    <groupId>com.github.bastiaanjansen</groupId>
    <artifactId>otp-java</artifactId>
    <version>2.0.3</version>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

---

## Files to Create

```
src/main/java/com/thomas/guessthelink/
├── security/
│   ├── SecurityConfig.java        — Spring Security rules (what needs auth, what doesn't)
│   ├── TotpService.java           — generates/verifies 6-digit TOTP codes
│   └── RateLimitService.java      — tracks login attempts, enforces lockout

├── controller/
│   └── AdminController.java       — handles /admin, /admin/login, /admin/setup routes

src/main/resources/templates/
├── admin-setup.html               — one-time QR code scan page (disabled after first use)
├── admin-login.html               — TOTP code input page
└── admin.html                     — the actual admin panel (question generation)
```

**Total: 4 Java files + 3 HTML files**

---

## One-Time Setup Flow (First Time Only)

```
1. You visit /admin/setup
2. TotpService generates a random secret key
3. Page displays a QR code
4. You scan QR code with Google Authenticator app
5. Secret key saved to DB or application.properties
6. /admin/setup is permanently disabled
```

After this, every login is just: open app → type 6-digit code.

---

## File Breakdown

### SecurityConfig.java
**What it does:** Tells Spring Security which routes need authentication.
```
Rules:
- /admin/** → requires TOTP authentication
- /admin/setup → only accessible if no secret exists yet
- Everything else (/, /login, /game, /home) → public
```

### TotpService.java
**What it does:** Generates the secret key, creates QR codes, verifies codes.
```
Methods:
- generateSecret() → creates a random secret key (one time)
- getQRCode(secret) → returns QR code URL for Google Authenticator
- verify(code, secret) → checks if the 6-digit code is valid right now
```

### RateLimitService.java
**What it does:** Tracks failed login attempts per IP, locks out after 5 fails.
```
Rules:
- Max 5 failed attempts
- After 5 fails → locked out for 30 minutes
- Successful login → resets counter
```

### AdminController.java
**What it does:** Handles all /admin routes.
```
Routes:
GET  /admin/setup  → shows QR code (first time only)
POST /admin/setup  → saves secret key
GET  /admin/login  → shows code input page
POST /admin/login  → verifies code, grants session
GET  /admin        → shows admin panel (protected)
POST /admin/generate → calls Gemini, returns questions for review
POST /admin/approve  → saves approved question to DB
```

---

## Admin Panel Features (admin.html)
```
- Button: "Generate Questions" → calls Gemini API
- Shows generated questions one by one:
  - 3 image keywords (used to fetch from Unsplash/Pexels)
  - Answer
  - Clue 1, Clue 2, Clue 3
- Per question: "Approve" button → saves to DB
- Per question: "Reject" button → discards
- Per question: "Regenerate" button → asks Gemini for a new one
```

---

## Image Strategy
Gemini generates **image search keywords** (e.g. "sole of shoe").
Unsplash API or Pexels API fetches real photo URLs from those keywords automatically.

```
Gemini output:
{
  "answer": "capital cities",
  "imageKeywords": ["sole of shoe", "brussels sprouts", "deli meat shop"],
  "clue1": "Think: places",
  "clue2": "These all sound like city names",
  "clue3": "Seoul, Brussels, Delhi"
}

→ Each keyword hits Unsplash API → returns a real image URL
→ URL stored in questions table
```

**Unsplash API:** Free tier — 50 requests/hour. Sign up at unsplash.com/developers.

---

## Build Order When You're Ready

```
1. Add dependencies to pom.xml
2. Build TotpService.java (TOTP logic)
3. Build RateLimitService.java (lockout logic)
4. Build SecurityConfig.java (route rules)
5. Build AdminController.java (routes)
6. Build admin-setup.html (QR code page)
7. Build admin-login.html (code input page)
8. Build admin.html (question generation panel)
9. Integrate Gemini API in AdminController
10. Integrate Unsplash API for image fetching
11. Test end to end
12. Deploy with HTTPS (Let's Encrypt)
```

---

## Resume Talking Points
- TOTP (RFC 6238) implementation from scratch
- Brute force protection via token bucket rate limiting
- Spring Security custom authentication flow
- AI-powered content generation pipeline (Gemini)
- Automated image sourcing via Unsplash API
- Full stack Java deployment with HTTPS

---

## Preparation Checklist (Do These Before Starting)

- [ ] Download Google Authenticator on your phone (iOS or Android — free)
- [ ] Sign up for Unsplash Developer account → get API key (free, unsplash.com/developers)
- [ ] Sign up for Gemini API → get API key (free tier, aistudio.google.com)
- [ ] Make sure your Spring Boot app is running clean with no errors
- [check] Add the 3 dependencies above to pom.xml before writing any code