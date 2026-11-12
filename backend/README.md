# WasteLess backend

Etapa implementată: conturi CLIENT/BUSINESS cu JWT, publicare manuală de oferte,
listare și rezervare. Java 17, Spring Boot, Spring Security, JPA, Bean Validation,
PostgreSQL și Flyway. Maven Wrapper și versiunile existente (Boot 4.1.2, Maven
3.10.0) sunt păstrate; validarea Maven a confirmat că se rezolvă corect.

## Configurare și pornire

Necesită JDK 17+, PostgreSQL (recomandat 17) și acces Maven la prima rulare.
Toate comenzile pornesc din directorul backend/.

1. Copiază .env.example în .env și înlocuiește valorile demonstrative.
   Fișierul .env este ignorat de Git.
2. Opțional, pornește PostgreSQL local: docker compose up -d.
   Portul este publicat doar pe localhost. Datele rămân în volumul postgres-data.
   Alternativ, creează o bază goală wasteless și un utilizator dedicat în
   PostgreSQL instalat separat.
3. Exportă variabilele în procesul aplicației. Spring Boot nu încarcă automat
   fișierul .env; Compose îl folosește doar pentru serviciul PostgreSQL.

| Variabilă | Semnificație |
|---|---|
| DB_URL | Obligatoriu, ex. jdbc:postgresql://localhost:5432/wasteless |
| DB_USERNAME | Obligatoriu, utilizatorul bazei |
| DB_PASSWORD | Obligatoriu, parola bazei |
| JWT_SECRET | Obligatoriu, secret aleator de minimum 32 bytes UTF-8, pentru HS256 |
| JWT_TTL_SECONDS | Opțional, implicit 3600; întreg pozitiv |
| CORS_ALLOWED_ORIGINS | Opțional, origini exacte separate prin virgulă; implicit niciuna |
| SERVER_PORT | Opțional, implicit 8080 |

Exemplu PowerShell, cu valori locale alese de tine:

    $env:DB_URL = 'jdbc:postgresql://localhost:5432/wasteless'
    $env:DB_USERNAME = 'wasteless'
    $env:DB_PASSWORD = '<parola-locala>'
    $env:JWT_SECRET = '<secret-aleator-minimum-32-bytes>'
    $env:CORS_ALLOWED_ORIGINS = 'http://localhost:3000'
    .\mvnw.cmd spring-boot:run

Linux/macOS: ./mvnw spring-boot:run, cu aceleași variabile exportate.
Flyway aplică V1__initial_schema.sql; Hibernate doar validează schema.
Nu există credențiale implicite pentru aplicație. Schimbarea secretului invalidează
tokenurile existente. Folosește HTTPS în afara dezvoltării locale.
Starter-ul resource-server furnizează verificarea JWT; nu există flux OAuth.

## API și acces

Toate request-urile sunt JSON. În afara signup/login este obligatoriu
Authorization: Bearer <accessToken>. Identitatea vine din subiectul JWT, iar
rolul și proprietatea sunt reverificate în serviciu folosind baza de date.
Proprietățile necunoscute în JSON sunt respinse cu 400, inclusiv clientId,
businessId, total sau unitPrice trimise nepermis.

| Metodă | Endpoint sub /api/v1 | Acces / rezultat |
|---|---|---|
| POST | /auth/register | Public; 201, token și utilizator |
| POST | /auth/login | Public; 200, token și utilizator |
| POST | /offers | BUSINESS; 201, oferta propriei afaceri |
| GET | /offers | CLIENT; 200, doar stoc > 0 și pickupEnd în viitor |
| GET | /offers/{id} | CLIENT sau BUSINESS proprietar; 200 |
| GET | /business/offers | BUSINESS; 200, numai ofertele sale, inclusiv epuizate/expirate |
| POST | /reservations | CLIENT; 201, rezervare și scădere imediată stoc |
| GET | /reservations/me | CLIENT; 200, numai rezervările sale |
| GET | /reservations/{id} | CLIENT proprietar sau BUSINESS al ofertei; 200 |
| GET | /business/reservations | BUSINESS; 200, numai rezervările ofertelor sale |

Listele acceptă ?page=0&size=20 (page >= 0, size 1..100).
Răspuns stabil:

    {"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}

Sortare după createdAt descrescător, apoi id.
Detaliile unei oferte cunoscute rămân accesibile clientului și după epuizare sau
expirare, pentru consultare; rezervarea este refuzată dacă nu mai este disponibilă.

### Autentificare

Signup BUSINESS:

    {
      "email": "bakery@example.com",
      "password": "LocalDemo123!",
      "role": "BUSINESS",
      "businessName": "Local Bakery",
      "pickupLocation": "Cluj-Napoca, Strada Exemplu 1"
    }

Pentru CLIENT se trimit numai email, password și role CLIENT.
Emailul este salvat cu litere mici și fără spații la extremități. Parola are
minimum 8 caractere și maximum 72 bytes UTF-8 (limita BCrypt).
BusinessName și pickupLocation sunt obligatorii doar pentru BUSINESS.
Login:

    {"email":"bakery@example.com","password":"LocalDemo123!"}

Răspuns signup/login:

    {
      "accessToken": "<JWT>",
      "tokenType": "Bearer",
      "expiresAt": "2027-01-01T13:00:00Z",
      "user": {
        "id": "<UUID>",
        "email": "bakery@example.com",
        "role": "BUSINESS",
        "businessName": "Local Bakery",
        "pickupLocation": "Cluj-Napoca, Strada Exemplu 1"
      }
    }

JWT conține numai sub (UUID), role, iss, iat și exp. Semnătura HS256, issuer și
expirarea sunt validate; nu sunt acceptate tokenuri fără exp. Parolele și hash-urile
nu apar în DTO-uri.

### Oferte

    {
      "productName": "Croissant rescue bag",
      "category": "Bakery",
      "description": "Selecție de croissante proaspete.",
      "originalPrice": 8.00,
      "wasteLessPrice": 3.20,
      "currency": "EUR",
      "quantity": 3,
      "pickupLocation": "Cluj-Napoca, Strada Exemplu 1",
      "pickupStart": "2027-01-01T16:00:00+02:00",
      "pickupEnd": "2027-01-01T19:00:00+02:00",
      "imageUrl": "https://example.com/croissant.jpg"
    }

Folosește date viitoare la rulare. Currency omis/null devine EUR; se acceptă
coduri ISO 4217 din trei litere mari. Prețurile sunt pozitive, cu cel mult 10
cifre întregi și 2 zecimale, WasteLess <= original. În această etapă toate monedele
folosesc maximum două zecimale și nu există conversie valutară.
Quantity este un întreg pozitiv. PickupLocation omis/null/gol folosește profilul
business și este salvat pe ofertă. ImageUrl este opțional; dacă este prezent,
trebuie să fie un URL HTTPS, maximum 2048 caractere.
Categoriile sunt exact Bakery, Cafe, Restaurant, Grocery, Other.

Intervalul este ISO 8601 cu dată și offset obligatoriu, start < end și end în
viitor la publicare. Stocarea este TIMESTAMPTZ, iar răspunsurile folosesc UTC (Z);
offset-ul original nu este păstrat separat. Clientul afișează ora în zona dorită.
O ofertă se poate rezerva înainte de pickupStart, dar numai înainte de pickupEnd.
Răspunsul conține câmpurile de mai sus și id, businessId, businessName,
createdAt. Quantity reprezintă stocul rămas.

### Rezervări și concurență

Request:

    {"offerId":"<UUID>","quantity":2}

Răspunsul include id, offerId, clientId, quantity, createdAt, unitPrice,
currency, total, status. Prețul și moneda sunt snapshot din ofertă;
totalul este calculat exclusiv pe server ca unitPrice * quantity.
RESERVED înseamnă doar rezervare, fără plată și fără ridicare confirmată.
Nu există expirare automată sau restituire automată a stocului.

Serviciul tranzacțional ia un lock PostgreSQL SELECT ... FOR UPDATE pe oferta
cerută, verifică termenul și stocul după obținerea lock-ului, scade stocul și
inserează rezervarea în aceeași tranzacție. Un rollback anulează ambele operații.
Două cereri pentru ultima unitate sunt serializate: una primește 201, cealaltă 409.
Schema impune și quantity >= 0. Nu există idempotency keys în această etapă:
repetarea unui POST reușit poate crea încă o rezervare dacă mai există stoc.

### Erori

Format comun:

    {"timestamp":"2027-01-01T12:00:00Z","status":400,"message":"Invalid request","details":["quantity: must be greater than 0"]}

400 date invalide (inclusiv JSON, UUID sau paginare); 401 autentificare absentă,
login incorect sau JWT invalid/expirat; 403 rol ori proprietar nepermis; 404 resursă
inexistentă; 409 email duplicat, stoc insuficient, interval de ridicare încheiat sau
conflict de persistență. Nu sunt expuse detalii SQL ori stack traces.

## Flux demonstrabil

Pornește aplicația și rulează ./demo.ps1 în PowerShell.
Scriptul generează adrese de email unice și date de ridicare viitoare, execută:
signup business → login → publicare → signup client → login → listare →
rezervare → verificare stoc (3 → 1). Creează date persistente în baza configurată.

Echivalent cURL pentru request-urile principale (shell POSIX, tokens/id obținute
din răspunsurile anterioare):

    curl -sS -X POST http://localhost:8080/api/v1/auth/register \
      -H 'Content-Type: application/json' \
      -d '{"email":"client@example.com","password":"LocalDemo123!","role":"CLIENT"}'
    curl -sS -X POST http://localhost:8080/api/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"client@example.com","password":"LocalDemo123!"}'
    curl -sS http://localhost:8080/api/v1/offers \
      -H "Authorization: Bearer $CLIENT_TOKEN"
    curl -sS -X POST http://localhost:8080/api/v1/reservations \
      -H "Authorization: Bearer $CLIENT_TOKEN" -H 'Content-Type: application/json' \
      -d "{\"offerId\":\"$OFFER_ID\",\"quantity\":2}"
    curl -sS "http://localhost:8080/api/v1/offers/$OFFER_ID" \
      -H "Authorization: Bearer $CLIENT_TOKEN"

Pentru publicare, salvează JSON-ul ofertei de mai sus cu interval actualizat în
offer.json, apoi:

    curl -sS -X POST http://localhost:8080/api/v1/offers \
      -H "Authorization: Bearer $BUSINESS_TOKEN" \
      -H 'Content-Type: application/json' --data-binary @offer.json

## Testare

    .\mvnw.cmd verify

Linux/macOS: ./mvnw verify. Testele pornesc un server HTTP și un **PostgreSQL real**
temporar prin io.zonky.test:embedded-postgres, fără Docker. Binarele sunt aduse
prin Maven; nu este o bază în memorie. Pe Linux, initdb necesită utilizator non-root.
Alternativ se poate folosi o bază PostgreSQL **dedicată testelor**, goală la prima
rulare: setează TEST_DB_URL, TEST_DB_USERNAME, TEST_DB_PASSWORD.
Testele creează și modifică date; nu indica baza de producție.
Secretul testelor este fix, exclusiv pentru teste. Niciun test nu este omis
automat dacă PostgreSQL nu poate porni: suita eșuează explicit.

Acoperire: signup/login ambele roluri, BCrypt, duplicate email normalizat,
validări cont, login invalid, JWT absent/alterat/expirat/issuer greșit,
restricții pe roluri, refuz câmpuri nepermise, validări oferte,
snapshot preț/monedă/total și stoc, stoc insuficient, ofertă expirată, paginare,
izolare între clienți și business-uri, două request-uri HTTP simultane pentru
ultima unitate (exact un 201, un 409, stoc final 0, o singură rezervare în DB).
Suita aplică migrările Flyway și validarea Hibernate pe PostgreSQL.

## Corespondența Flutter și limitele etapei

Context inspectat read-only cu git show/diff: main, welcome-page,
business-dashboard-page, business-AI-insights-page și business-create-offer-page.
BusinessOffersRepository din main conține mock-uri în memorie; nu a fost modificat.

| Flutter | Backend / adaptare viitoare |
|---|---|
| OfferDraft.productName | productName |
| category | Aceleași valori, case-sensitive |
| description | description, obligatoriu |
| originalPrice / wasteLessPrice (String în draft) | Numere JSON decimale, BigDecimal/NUMERIC |
| quantity (String în draft) | Întreg JSON pozitiv; răspunsul este stocul rămas |
| pickupWindow (text orar) | UI trebuie să ceară data și zona și să trimită pickupStart/pickupEnd cu offset |
| photoAsset / BusinessOffer.imageAsset | Nu sunt URL-uri publice; viitorul UI va folosi imageUrl sau placeholder |
| BusinessOffer.id (mock string) | UUID transmis ca String |
| title | productName |
| available | quantity |
| price (double EUR) | wasteLessPrice + currency explicită, EUR implicit |
| stockLevel | Indicator de prezentare derivat în UI, nu stare persistentă |
| isPaused | Amânat; API-ul nu expune pauză |

În plus față de mock-uri sunt persistate proprietarul autentificat, profilul minim
business, locația ofertei, timestamp-uri, preț inițial, categorie și descriere.
Nu este integrat frontend-ul. Prezentarea nu a fost disponibilă ca fișier atașat
în această sesiune; scopul implementat este exclusiv cel din cerere.

Amânate: editare/ștergere/pauză oferte, dashboard financiar, căutare geografică
avansată, upload/object storage, ADMIN, OAuth, refresh tokens, resetare parole,
verificare email, plăți/webhook-uri/comisioane, QR/ridicare, anulare/expirare
rezervări, AI, notificări, livrare și infrastructură cloud.

## Verificare efectuată în această implementare

Pe Windows, cu JDK 17.0.17 și PostgreSQL real 14.20:
mvnw.cmd verify a trecut cu 11 teste, 0 eșecuri, 0 erori, 0 omise.
Sunt verificate și rollback-ul la refuzul inserării de către DB, cantitățile
fracționare și refuzul categoriilor transmise ca index numeric.
JAR-ul împachetat a fost pornit separat cu variabile de mediu, pe o bază temporară,
iar demo.ps1 a trecut: stoc 3 → 1, total 6.40 EUR, status RESERVED.
Procesele temporare au fost oprite după verificare.
Docker nu este instalat în mediul de lucru: compose.yaml și rularea cu imaginea
PostgreSQL 17 nu au fost verificate prin Docker. Nu s-a testat integrarea Flutter.
