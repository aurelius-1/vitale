# SignatureValidationFilter — teorie, exemple, testare

Document de referință pentru fix-ul aplicat în:
- `vitale_app/src/main/java/com/marius/ptr/app/config/SignatureValidationFilter.java`
- `common-api/src/main/java/com/marius/ptr/app/SignatureValidationFilter.java`

Scop: să înțelegi *de ce* body-ul unui `HttpServletRequest` poate fi citit o singură dată,
*ce pattern* am folosit ca să-l "reîncărcăm", și *cum testezi* corect acest tip de cod —
nu doar "ce am scris", ci mecanismul din spate, cu exemple independente de proiect.

---

## 1. Teoria de bază: de ce body-ul unui request se citește o singură dată

### 1.1 Ce e de fapt `request.getInputStream()`

Când Tomcat (sau orice servlet container) primește un request HTTP, body-ul nu stă
"gata parsat" undeva în memorie. E pur și simplu ce vine pe socket-ul TCP, byte cu byte,
pe măsură ce sosește de la client. `HttpServletRequest.getInputStream()` îți dă un
`ServletInputStream` — un `InputStream` legat direct de acel socket.

```java
public abstract class ServletInputStream extends InputStream {
    // + isFinished(), isReady(), setReadListener(ReadListener) — I/O async, Servlet 3.1+
}
```

Proprietatea esențială a oricărui `InputStream` (fie el de fișier, de rețea, de array):
**e forward-only**. N-are `reset()` garantat (doar `mark()/reset()` dacă implementarea
suportă explicit, ceea ce un stream de socket NU face). Odată ce ai citit N bytes, aceia
au "trecut" — poziția de citire a avansat ireversibil, exact ca un cap de bandă magnetică
ce nu se poate derula.

```java
InputStream in = request.getInputStream();
byte[] all = in.readAllBytes();   // citește TOT până la EOF
in.read();                        // -1 (EOF) — nu mai e nimic de citit, pentru totdeauna
```

### 1.2 De ce contează asta pentru un `Filter`

Un `jakarta.servlet.Filter` stă *înaintea* servlet-ului țintă (la noi: `DispatcherServlet`
→ `BookController`). Filter chain-ul e o secvență:

```
request brut → Filter1 → Filter2 → ... → DispatcherServlet → Controller → HttpMessageConverter (Jackson)
```

Jackson (via `MappingJackson2HttpMessageConverter`) citește body-ul **o singură dată**,
la finalul lanțului, ca să deserializeze JSON-ul în obiectul Java (`Book`, în cazul nostru).

Dacă orice filtru din lanț citește deja body-ul înaintea lui Jackson — și nu face nimic
special — Jackson găsește streamul gol. Exact asta era bug-ul.

### 1.3 `OncePerRequestFilter`

Clasa Spring `OncePerRequestFilter` (extinsă de `SignatureValidationFilter`) garantează
că `doFilterInternal(...)` rulează o singură dată per request (util dacă request-ul
trece prin `RequestDispatcher.forward()`/`include()`, unde filtrele s-ar putea re-executa).
Contractul e simplu — implementezi:

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {
    // ... logica ta ...
    chain.doFilter(request, response); // OBLIGATORIU, altfel request-ul moare aici
}
```

`chain.doFilter(request, response)` e apelul care "dă drumul mai departe" — pe orice
obiect `request` alegi tu să pasezi. **Asta e portița pe care o exploatăm în soluție.**

---

## 2. Problema, reprodusă minimal (fără Spring Boot, doar concept)

Ca să izolăm bug-ul de tot restul aplicației, iată-l redus la esență:

```java
class BuggyFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        // citim tot ca să calculăm un hash / o semnătură
        byte[] body = request.getInputStream().readAllBytes();
        boolean valid = verify(body);

        if (!valid) {
            ((HttpServletResponse) res).sendError(401);
            return;
        }

        // GREȘEALA: pasăm mai departe ACELAȘI request,
        // al cărui InputStream e deja la EOF
        chain.doFilter(request, res);
    }
}
```

Controller-ul de la capătul lanțului face, conceptual, ce face Jackson:

```java
byte[] bodyInController = request.getInputStream().readAllBytes();
// bodyInController.length == 0   <-- SURPRIZĂ
```

Rezultatul concret la noi: `HttpMessageNotReadableException: Required request body is
missing`, tradus de Spring în `400 Bad Request`. L-am confirmat direct în logul
serverului când am făcut primul test de inserare (`POST /books`).

**Punctul cheie:** problema nu are legătură cu JSON, cu Jackson, sau cu `Book` — e pur
mecanică de `InputStream`. Orice filtru care citește body-ul și nu-l "repune la loc"
strică orice consumator de după el.

---

## 3. Pattern-ul folosit: Decorator

### 3.1 Definiție (Gang of Four)

> *Attach additional responsibilities to an object dynamically. Decorators provide a
> flexible alternative to subclassing for extending functionality.*

Practic: iei un obiect care implementează o interfață, construiești un alt obiect care
implementează **aceeași interfață**, îl "înfășori" pe primul, și suprascrii doar
metodele care te interesează — restul le delegi mecanic la obiectul original.

### 3.2 Exemplul canonic — chiar `java.io`

Ai folosit asta de zeci de ori fără să te gândești la "pattern":

```java
Reader r = new BufferedReader(new InputStreamReader(new FileInputStream("x.txt"), UTF_8));
```

- `FileInputStream` — sursa brută de bytes
- `InputStreamReader` — decorator: adaugă decodare bytes→char
- `BufferedReader` — decorator: adaugă buffering + `readLine()`

Fiecare strat *este-a* un `Reader`/`InputStream` (implementează aceeași interfață de
bază) și *conține* stratul anterior. Poți oricând trece un `BufferedReader` acolo unde
se așteaptă un `Reader` simplu — polimorfism.

### 3.3 `HttpServletRequestWrapper` — decorator gata făcut de Servlet API

Jakarta Servlet API vine deja cu un decorator standard pentru exact acest scenariu:

```java
public class HttpServletRequestWrapper extends ServletRequestWrapper
        implements HttpServletRequest {

    public HttpServletRequestWrapper(HttpServletRequest request) { super(request); }

    // Fiecare metodă, by default, delegă la request-ul original:
    @Override public String getMethod() { return this.getRequest().getMethod(); }
    @Override public String getHeader(String name) { return ((HttpServletRequest) getRequest()).getHeader(name); }
    // ... zeci de altele, toate delegate automat
}
```

Tu extinzi clasa asta și suprascrii **doar** ce ai nevoie. Tot restul (headere, URI,
metodă HTTP, parametri) rămâne identic cu request-ul original, pentru că vine prin
delegare automată din clasa părinte.

---

## 4. Clase anonime și closures — recapitulare rapidă

Poate ai 6 ani de experiență dar lucrezi mai mult cu Spring/adnotații decât cu Java
"de bază" — e foarte comun, nu-i nimic în neregulă. Hai să trecem prin mecanism cu
exemple mici, fără servleți.

### 4.1 Clasă anonimă = instanțiere + definiție, într-un singur pas

Varianta normală (cu clasă numită):

```java
class MyComparator implements Comparator<String> {
    @Override public int compare(String a, String b) { return a.length() - b.length(); }
}
List<String> list = ...;
list.sort(new MyComparator());
```

Varianta anonimă — definești clasa *inline*, fără nume, exact unde o folosești o
singură dată:

```java
list.sort(new Comparator<String>() {
    @Override public int compare(String a, String b) { return a.length() - b.length(); }
});
```

E exact aceeași mecanică pe care o folosim pentru `HttpServletRequestWrapper` și
`ServletInputStream` — clase mici, folosite o singură dată, fără să polueze codebase-ul
cu fișiere `.java` separate pentru fiecare.

### 4.2 Closure — clasa anonimă "ține minte" variabile din context

```java
static Runnable greeterFor(String name) {
    return new Runnable() {
        @Override public void run() {
            System.out.println("Salut, " + name); // 'name' vine din afara clasei anonime
        }
    };
}

Runnable r = greeterFor("Marius");
r.run(); // "Salut, Marius" — deși metoda greeterFor() s-a terminat de mult
```

`name` e o variabilă locală a metodei `greeterFor`, dar clasa anonimă o "capturează" —
compilatorul îi generează intern un field privat + constructor care o copiază, ca ea să
rămână vie chiar și după ce metoda originală a ieșit din stivă. Condiția: `name` trebuie
să fie **effectively final** (nu e reasignată după inițializare) — altfel nu compilează.

Exact asta se întâmplă în:

```java
private static HttpServletRequest wrapWithBody(HttpServletRequest request, byte[] body) {
    return new HttpServletRequestWrapper(request) {
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(body); // 'body' capturat din closure
            return new ServletInputStream() { ... };
        }
    };
}
```

`body` (parametrul metodei) e capturat de clasa anonimă `HttpServletRequestWrapper`.
De fiecare dată când cineva apelează `getInputStream()` pe wrapper, se creează un
`ByteArrayInputStream` **nou**, dar din **același** array `body` — de-asta poți citi
body-ul de N ori, fiecare citire pornind curat de la byte 0.

---

## 5. Soluția, construită incremental

### Pasul 1 — citim body-ul o singură dată, la sursă, ca `byte[]`

```java
byte[] rawBody = request.getInputStream().readAllBytes();
```

Un `byte[]` (spre deosebire de un `InputStream`) e date pasive în memorie — poți să-l
citești de câte ori vrei, din câte puncte vrei, fără să-l "consumi".

### Pasul 2 — validăm hash-ul + semnătura pe acel array (neschimbat)

```java
if (!validateBodyHash(rawBody, bodyHash)) { ... 401 ... }
if (!validateSignature(publicKey, rawBody, signature)) { ... 401 ... }
```

### Pasul 3 — construim un request "cu memorie", care poate reda `rawBody` la cerere

```java
private static HttpServletRequest wrapWithBody(HttpServletRequest request, byte[] body) {
    return new HttpServletRequestWrapper(request) {

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return buffer.available() == 0; }
                @Override public boolean isReady()    { return true; }
                @Override public void setReadListener(ReadListener rl) { /* no-op, vezi 5.1 */ }
                @Override public int read()           { return buffer.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    };
}
```

### Pasul 4 — dăm mai departe wrapper-ul, nu request-ul original

```java
chain.doFilter(wrapWithBody(request, rawBody), response);
```

Din acest punct, orice cod din aval (alt filtru, `DispatcherServlet`, Jackson) care
apelează `.getInputStream()` pe request primește un `ByteArrayInputStream` proaspăt,
peste `rawBody` — indiferent de câte ori e apelat.

### 5.1 De ce `setReadListener` poate rămâne gol

E parte din API-ul de I/O **asincron** (Servlet 3.1+), pentru cazul în care body-ul
vine "pe bucăți" de la client și vrei să fii notificat non-blocant când mai sunt date
disponibile (`ReadListener.onDataAvailable()`). `ByteArrayInputStream` e 100% sincron,
in-memory — nu așteaptă niciodată date, deci nu are ce să notifice. Las metoda goală
intenționat, nu din neglijență.

---

## 6. Alternative — și de ce NU le-am folosit

### 6.1 `ContentCachingRequestWrapper` (varianta veche, greșit folosită)

```java
ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
wrapped.getInputStream().readAllBytes();     // (A) citim ca să cache-uim
byte[] body = wrapped.getContentAsByteArray();
chain.doFilter(wrapped, response);           // (B) trimitem ACELAȘI wrapper mai departe
```

`ContentCachingRequestWrapper` (Spring) *cache-uiește* bytes pe măsură ce sunt citiți —
dar nu oferă un al doilea "cap de citire". E gândit pentru alt scenariu: citești body-ul
**normal, o singură dată, în aval** (ex: în controller), iar cache-ul intern devine
util *după aceea* — de exemplu într-un `Filter` care rulează **după** controller și
vrea să logheze body-ul pentru audit (`getContentAsByteArray()` la finalul lanțului).
Dacă tu citești manual în filtru (A) ÎNAINTE ca body-ul să fie citit "organic" de
Jackson, ai epuizat singurul stream disponibil — (B) trimite mai departe un cadavru.

**Regulă practică:** `ContentCachingRequestWrapper` e pentru "citește o dată, în aval,
loghează după" — nu pentru "citește de două ori, oriunde".

### 6.2 Wrapper custom cu body citit direct în constructor (variantă echivalentă, foarte răspândită)

O variantă des întâlnită în blogposturi/proiecte reale, echivalentă ca idee cu ce am
făcut, dar structurată ca o clasă numită în loc de closure inline:

```java
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes(); // citit o dată, aici
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
```

Diferența față de soluția noastră: aici citirea body-ului se întâmplă *în constructor*,
nu separat în filtru. E o alegere de stil — dacă ai nevoie de acest wrapper în mai
multe filtre din lanț (nu doar în cel de semnătură), o clasă numită, reutilizabilă,
e mai curată decât o closure privată într-un singur filtru. La noi, fiindcă avem un
singur loc unde se întâmplă asta (`SignatureValidationFilter`), am ales varianta
inline — mai puțin cod, fără fișier nou.

### 6.3 De ce nu am ținut body-ul ca `String`

Am fi putut face `new String(rawBody, UTF_8)` și lucra cu `String` peste tot. Problema:
hash-ul SHA-256 și verificarea semnăturii RSA trebuie calculate pe **exact aceiași
bytes** pe care i-a semnat clientul. Orice conversie `bytes → String → bytes` riscă
normalizări de encoding (mai ales dacă cineva greșește charset-ul), ceea ce ar rupe
validarea semnăturii. Lucrul direct cu `byte[]` elimină acest risc — de-asta și
`sign-request.ps1` scrie explicit fișierul temporar ca UTF-8 fără BOM, cu comentariu
dedicat despre asta.

---

## 7. Cum testezi acest tip de cod

Testarea unui `Filter` are trei niveluri, de la izolat la end-to-end. Pentru un caz
ca acesta, toate trei sunt utile.

### 7.1 Nivel 1 — testezi doar wrapper-ul, fără Spring, fără HTTP real

Cel mai ieftin și mai rapid test: verifici *exact* proprietatea pe care vrei s-o
garantezi — că `getInputStream()` poate fi apelat de mai multe ori și dă mereu
conținutul complet.

```java
class WrapWithBodyTest {

    @Test
    void inputStreamCanBeReadMultipleTimes() throws IOException {
        byte[] body = "{\"isbn\":\"1234567890\"}".getBytes(StandardCharsets.UTF_8);

        HttpServletRequest original = mock(HttpServletRequest.class); // Mockito
        HttpServletRequest wrapped = SignatureValidationFilter.wrapWithBody(original, body);
        // notă: fă metoda package-private sau expune-o via un test-helper dacă vrei
        // s-o testezi izolat fără reflection

        byte[] firstRead  = wrapped.getInputStream().readAllBytes();
        byte[] secondRead = wrapped.getInputStream().readAllBytes();

        assertArrayEquals(body, firstRead);
        assertArrayEquals(body, secondRead);   // <-- exact garanția pe care o vrei
    }

    @Test
    void readerReturnsSameContentAsUtf8() throws IOException {
        byte[] body = "București".getBytes(StandardCharsets.UTF_8); // diacritice, intenționat
        HttpServletRequest original = mock(HttpServletRequest.class);
        HttpServletRequest wrapped = SignatureValidationFilter.wrapWithBody(original, body);

        String read = wrapped.getReader().readLine();

        assertEquals("București", read);
    }
}
```

> Notă: `wrapWithBody` e `private static` acum. Pentru testul de mai sus fie îl faci
> package-private (fără modificator), fie îl testezi indirect prin `doFilterInternal`
> (nivel 2, mai jos) — ambele sunt abordări valide; eu aș recomanda package-private,
> e mai ieftin de testat izolat.

### 7.2 Nivel 2 — testezi filtrul complet, cu `MockHttpServletRequest`/`MockFilterChain` (Spring Test)

Aici verifici comportamentul *întregului* `doFilterInternal`, inclusiv validarea
semnăturii, fără să pornești un server HTTP real.

```java
class SignatureValidationFilterTest {

    private final SignatureValidationFilter filter = new SignatureValidationFilter();

    @Test
    void validSignature_passesBodyThroughIntact() throws Exception {
        byte[] body = "{\"isbn\":\"1234567890\"}".getBytes(StandardCharsets.UTF_8);
        String bodyHash = base64Sha256(body);
        String signature = signWithTestKey(body); // helper cu cheia din test_key_signature.pem

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/books");
        request.setContent(body);
        request.addHeader("x-signature-body-hash", bodyHash);
        request.addHeader("x-signature", signature);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // FilterChain "spion" care verifică ce a primit efectiv mai departe
        AtomicReference<byte[]> seenByNextInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            seenByNextInChain.set(((HttpServletRequest) req).getInputStream().readAllBytes());
        };

        filter.doFilter(request, response, chain);

        assertArrayEquals(body, seenByNextInChain.get()); // <-- asta e regresia pe care o previi
        assertEquals(200, response.getStatus());          // nimic n-a fost blocat de filtru
    }

    @Test
    void tamperedBody_isRejectedWith401() throws Exception {
        byte[] signedBody   = "{\"isbn\":\"1234567890\"}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBody = "{\"isbn\":\"9999999999\"}".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/books");
        request.setContent(tamperedBody); // body diferit de ce s-a semnat
        request.addHeader("x-signature-body-hash", base64Sha256(signedBody));
        request.addHeader("x-signature", signWithTestKey(signedBody));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain); // request-ul nu trebuie să ajungă mai departe
    }
}
```

Acest nivel e cel mai valoros pentru regresii: dacă cineva rescrie filtrul peste un an
și reintroduce bug-ul vechi (`chain.doFilter(wrappedOriginal, ...)` în loc de
`wrapWithBody(...)`), `validSignature_passesBodyThroughIntact` pică imediat, clar,
fără să fie nevoie de un server Spring Boot pornit.

### 7.3 Nivel 3 — test de integrare end-to-end (`@SpringBootTest` + `MockMvc` sau `TestRestTemplate`)

Verifică tot lanțul real: filtru → `DispatcherServlet` → `BookController` → Jackson →
`BookService` → baza de date. E ce am făcut manual cu `curl`/`sign-request.ps1` în
sesiunea asta — merită automatizat:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BookControllerSignedRequestIT {

    @Autowired MockMvc mockMvc;

    @Test
    void postSignedBook_isPersisted() throws Exception {
        String json = """
            {"isbn":"1234567890","title":"Test","author":"Ana","price":19.99}
            """;
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-signature-body-hash", base64Sha256(body))
                .header("x-signature", signWithTestKey(body))
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isbn").value("1234567890"));
    }
}
```

Acest test ar fi picat imediat cu implementarea veche (400, "Required request body is
missing") și confirmă fix-ul exact în condițiile reale de rulare (Tomcat embedded,
Jackson real, Spring Security/filter chain reale) — nu doar mecanica low-level.

### 7.4 Checklist de cazuri limită de acoperit

| Caz | De ce contează |
|---|---|
| Body gol (`GET`, sau `POST` fără conținut) | `shouldNotFilter` exclude doar `/actuator/**` — un `GET` normal trece prin filtru cu body gol; verifică hash-ul unui array gol |
| Body cu diacritice / UTF-8 multi-byte | Confirmă că encoding-ul din `getReader()` (`StandardCharsets.UTF_8`) corespunde cu ce a semnat clientul |
| Body mare (ex: > 1MB) | `readAllBytes()` trebuie să citească tot, nu doar un buffer inițial; testează că nu se trunchiază |
| Semnătură/hash invalide | Trebuie 401, și `chain` NU trebuie apelat deloc (`verifyNoInteractions`) |
| Citire dublă a `getInputStream()` în aval | Exact regresia pe care o previi — vezi 7.2 |
| Requesturi concurente (thread-safety) | `wrapWithBody` e `static`, fără stare pe `this` — sigur de rulat din mai multe thread-uri Tomcat simultan; un test cu `ExecutorService` + N requesturi paralele, verificând că fiecare thread vede body-ul lui corect, e un test bun de adăugat dacă ai dubii |

---

## 8. Ghid rapid: cum implementezi corect acest pattern data viitoare

Aplicabil oricărui `Filter`/`Interceptor` care trebuie să **citească body-ul și să-l
lase citibil și pentru codul de după el** (semnături, logging de audit, rate limiting
pe conținut, etc.):

1. **Nu presupune niciodată** că poți citi `request.getInputStream()` de două ori pe
   același obiect `request` — nu se poate, indiferent de wrapper folosit, dacă nu ai
   grijă explicit.
2. Citește body-ul **o singură dată**, undeva devreme, direct în `byte[]`
   (`readAllBytes()`).
3. Fă orice validare/logică ai nevoie pe acel `byte[]` — e sigur, poți să-l citești
   de câte ori vrei, e memorie pasivă.
4. Înainte de `chain.doFilter(...)`, **înlocuiește** request-ul cu un wrapper
   (`HttpServletRequestWrapper`) care suprascrie `getInputStream()`/`getReader()` să
   servească acel `byte[]`, printr-un stream nou de fiecare dată când e cerut.
5. **Niciodată** nu trimite mai departe un wrapper al cărui stream intern a fost deja
   citit (asta a fost bug-ul original cu `ContentCachingRequestWrapper`).
6. Dacă ai nevoie de acest comportament în mai multe filtre, extrage-l într-o clasă
   numită, reutilizabilă (vezi 6.2), în loc să duplici closure-ul.
7. Scrie măcar un test de nivel 2 (7.2) care verifică explicit că body-ul ajunge
   intact la `FilterChain` — el te protejează de regresii viitoare, chiar dacă
   altcineva rescrie filtrul fără context.

---

## 9. Capcane comune (ce să eviți)

- **`ContentCachingRequestWrapper` "pentru citire dublă"** — nu e ce face; e pentru
  citire simplă + inspecție ulterioară a cache-ului, după ce body-ul a fost consumat
  organic de altcineva.
- **Body citit ca `String` pentru validare de semnătură** — riscă mismatch de encoding
  între ce s-a semnat (bytes) și ce se validează (bytes derivați dintr-un `String`
  reconstruit). Lucrează direct pe `byte[]`.
- **`setReadListener` lăsat gol "din neatenție"** — la noi e corect gol pentru că
  `ByteArrayInputStream` e sincron; dacă vreodată body-ul ar veni dintr-o sursă
  asincronă reală, ai nevoie de implementare completă acolo.
- **Uitarea `chain.doFilter(...)`** — dacă nu-l apelezi deloc (ex: ai un `return` din
  greșeală înainte), request-ul moare tăcut, fără eroare clară — verifică mereu că
  fiecare cale de cod din filtru fie apelează `chain.doFilter`, fie trimite explicit
  un răspuns de eroare (`sendError`).
- **Testare doar "manuală, cu Postman/curl"** — utilă pentru verificare rapidă (cum am
  făcut și noi), dar nu înlocuiește un test automat de nivel 2, care prinde regresia
  instant, fără server pornit, fără Docker, fără bază de date.
