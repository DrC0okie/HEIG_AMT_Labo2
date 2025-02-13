# Report

## Exercise 1 - Transaction, Renting a DVD

**Question:** Describe an alternative strategy that could achieve a similar result to the one implemented.

Une autre stratégie consiste à utiliser une colonne supplémentaire dans la table d'inventaire pour indiquer directement si un inventaire est loué. Par exemple, une colonne is_rented (bool) pourrait être utilisée.

Cette méthode fonctionnerait de la manière suivante : vérifier et mettre à jour l'état de l'inventaire atomiquement via une transaction SQL unique.

```sql
Copier le code
UPDATE inventory
SET is_rented = TRUE
WHERE inventory_id = :inventoryId
  AND is_rented = FALSE;
```

Si le nombre de lignes affectées est 0, cela signifie que l'inventaire est déjà loué.

On insère ensuite l'enregistrement dans la table rental.

Cette stratégie réduit le nombre de requêtes, mais nécessite une modification du schéma de la base de données.

**Question:** Explain why managing the concurrency using [@Lock](https://quarkus.io/guides/cdi-reference#container-managed-concurrency) or Java `synchronized` is not a solution.

Ces approches ne sont pas adaptées pour des applications distribuées ou multi-threadées car :

- **Scope local :** @Lock et synchronized fonctionnent uniquement au niveau de l'instance de l'application. Si plusieurs instances de l'application accèdent à la même base de données (par exemple dans un cluster), ces mécanismes ne garantissent pas l'intégrité des données.

- **Pas de synchronisation avec la base de données :** Ces mécanismes ne verrouillent pas les ressources dans la base de données elle-même, ce qui permet à des requêtes concurrentes provenant d'autres sources (par exemple, un autre client SQL) de contourner les verrouillages.

- **Pas scalable :** Ces solutions peuvent rapidement devenir des goulets d'étranglement si de nombreuses requêtes sont exécutées en parallèle.

La gestion de la concurrence doit être effectuée directement au niveau de la base de données, en utilisant des transactions et des niveaux d'isolation appropriés.

## Exercise 3 - Implement authentication for staff

**Question: Why is the password storage in the Sakila `Staff` table insecure?**

- SHA-1 is an outdated hashing algorithm that is considered cryptographically insecure due to collision vulnerabilities. Attackers can generate the same hash for different inputs, making it susceptible to attacks.

- Passwords are hashed directly without any additional random data (salt). This allows attackers to use precomputed hash databases (rainbow tables) to quickly crack passwords.

- Without iteration or key-stretching mechanisms (e.g., bcrypt, PBKDF2, or Argon2), the hashes can be brute-forced more efficiently.

**Proposed Solutions**

1. Use a modern password hashing algorithm like bcrypt or Argon2. These algorithms automatically handle salting and are computationally expensive, making brute-force attacks infeasible.

2. Store hashed passwords in a field capable of handling variable-length hashed strings (e.g., 60-characters for bcrypt).
3. Enforce minimum length of at least 12 characters for passwords

**HTTP flow description :**

**1. Request: Login Form Submission**

- Request URL: `http://localhost:8080/j_security_check`

- Request Method: `POST`

- Request Payload: `{"j_username": "Mike",  "j_password": "12345"}`

The Quarkus form authentication mechanism intercepts this request. The provided username (`j_username`) and password (`j_password`) are validated against the `Staff` table using the Quarkus Security JPA identity provider.

**2. Response to Login Request**

- Response code: `302 Found` (redirect to `/hello/me` on successful authentication) or `302 Found` (redirect to `/error.html` if credentials are invalid).

- Response Headers: Contains a session cookie encrypted using the configured key (`quarkus.http.auth.session.encryption-key`).

On success, the session cookie allows the user to stay authenticated for subsequent requests. The user is redirected to the landing page (`/hello/me`).

**3. Request: Accessing Protected Resource**

- Request URL: `http://localhost:8080/hello/me`

- Request Method: `GET`

The session cookie is sent with this request to authenticate the user. The `HelloResource` endpoint checks if the user is authenticated and has the `user` role. If authorized, the endpoint returns a personalized greeting.

**4. Response to Protected Resource**

- Response Code: `200 OK`

- Response Body: `Hello Mike!`

**Question: What is sent to authenticate to the application and how is it transmitted?**

The login form sends the following credentials via an HTTP `POST` request to `/j_security_check`:

- `j_username`: The username entered by the user.
- `j_password`: The plaintext password entered by the user.

These credentials are transmitted as part of the HTTP request body. While transmitted over an unencrypted HTTP connection.

**Question: What is the content of the response specific to the authentication flow?**

- Successful authentication: A `302 Found` response with a `Set-Cookie` header containing the encrypted session cookie. The browser automatically follows the redirect to the landing page (`/hello/me`).
- Failed authentication: A `302 Found` response redirecting the user to `/error.html`.

The session cookie contains encrypted authentication information, which Quarkus uses to verify the user's identity for subsequent requests.

**Question: Why is the test authentication flow insecure in a production environment?**

- User credentials (`j_username` and `j_password`) are sent over the network in plaintext if HTTPS is not enforced. This makes them vulnerable to interception by attackers.

- The `CustomPasswordProvider` uses SHA-1, which is outdated and insecure for password hashing.

- Without proper configurations (e.g., secure, HttpOnly, SameSite cookies), the session cookie may be vulnerable to interception or misuse.

**Question: What is required to make the authentication flow secure?**

- Encrypt all HTTP traffic using HTTPS to prevent interception of sensitive data and configure Quarkus to redirect HTTP requests to HTTPS automatically.

- Replace the SHA-1-based password hashing with bcrypt or Argon2 and use a dedicated password management library for secure password handling ([see this article](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)).

- Add secure flags to cookies to prevent theft or misuse:

```
quarkus.http.cookie.secure=true
quarkus.http.cookie.http-only=true
quarkus.http.cookie.same-site=strict
```

- Add rate-limiting mechanisms to prevent brute-force attacks.

- Implement MFA for sensitive applications.

## Exercise 5 - Implement a frontend for rentals

### Discuss the pros and cons of using an approach such as the one promoted by htmx.

htmx is a modern library that emphasizes minimalism by allowing developers to build dynamic, server-driven web applications with a declarative approach. It enables HTML elements to directly communicate with the server using attributes like hx-get, hx-post, and others, simplifying AJAX-like behavior without requiring extensive JavaScript.
We get rid of JSON communication between front-end and back-end and replace it by plain html.

#### Pros

- htmx relies on HTML attributes, making it easy to add dynamic behavior directly to the markup without writing JavaScript
- No need for complex client-side frameworks to handle interactivity; the server does most of the heavy lifting
-  Allows developers to keep logic on the server, leveraging server-side frameworks and reducing duplication of logic between the frontend and backend

#### Cons

- The server processes most of the requests and delivers updated content, which can increase server load compared to SPAs that offload some processing to the client.
- Applications relying heavily on server responses may feel sluggish for users with high-latency connections, especially if the server is far from the user.
-  For highly interactive applications like drag-and-drop UIs or real-time collaborative tools, htmx may not be sufficient without supplemental JavaScript

