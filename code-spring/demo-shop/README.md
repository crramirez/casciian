# demo-shop

A small Spring Boot application that shows what the
[casciian-spring-boot-starter](../casciian-spring-boot-starter) is for.

* **Customers** see a product catalogue at <http://localhost:8080/>.
* **Operators** SSH into the same JVM (`ssh admin@localhost -p 2222`,
  password `admin`) and CRUD the catalogue from a Casciian TUI.

Both views share the same `ProductRepository`, so anything an admin
creates, edits, or deletes from the terminal shows up the next time a
customer reloads the page.

## How it's wired

```
+-------------------------------------------------------+
|  Spring Boot JVM (DemoShopApplication)                |
|                                                       |
|  +------------+    +-------------------+    +------+  |
|  |  Tomcat    |--> | ProductRepository |<-- | TUI  |  |
|  |  / Thymeleaf|   |  (Spring Data JPA)|    |  app  | |
|  +------------+    +---------+---------+    +------+  |
|       |                      |                  |     |
|     :8080                    v               :2222    |
|                            H2 in-mem            (SSH) |
+-------------------------------------------------------+
```

* `Product` / `ProductRepository` &mdash; JPA entity + Spring Data repo.
* `ProductFakerSeeder` &mdash; uses [DataFaker][datafaker] to create
  ~25 random products on first startup (only when the table is empty).
* `ShopController` &mdash; renders the customer-facing Thymeleaf page.
* `admin/AdminTuiConfig` &mdash; declares the
  `CasciianTApplicationFactory` bean expected by the starter.
* `admin/AdminTApplication` &mdash; Casciian `TApplication` with a
  `Products` menu (Refresh / New / Edit selected / Delete selected) over
  the same `ProductRepository`.

## Run it

```sh
# from the repository root
cd code-spring
./gradlew :demo-shop:bootRun
```

Then:

```sh
# customer view
open http://localhost:8080/

# admin view
ssh admin@localhost -p 2222    # password: admin
```

The admin TUI is keyboard-driven; press `F2` (or click) to open the
**Products** menu and use **New / Edit selected / Delete selected /
Refresh**. Use `F10` (or `File > Exit`) to log out.

## Configuration

See [`src/main/resources/application.yml`](src/main/resources/application.yml).
The H2 datasource is in-memory; SSH credentials default to `admin/admin`
for the demo and **must** be overridden for any real deployment, e.g.:

```sh
SPRING_APPLICATION_JSON='{"casciian":{"ssh":{"username":"ops","password":"…"}}}' \
  java -jar demo-shop-*.jar
```

[datafaker]: https://www.datafaker.net/
