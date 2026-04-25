# casciian-spring

This Gradle multi-project hosts the Casciian Spring Boot integration:

| Subproject | Purpose |
| ---------- | ------- |
| [`casciian-spring-boot-starter`](./casciian-spring-boot-starter) | Spring Boot 3.x auto-configuration that exposes a Casciian TUI over SSH. Published to Maven Central. |
| [`demo-shop`](./demo-shop) | Runnable Spring Boot demo: a customer-facing web shop **and** an admin TUI that operate on the same H2-backed product catalogue. |

```sh
# Build everything (starter + demo)
./gradlew build

# Run the demo
./gradlew :demo-shop:bootRun
```

Once the demo is running:

* Customers see the product catalogue at <http://localhost:8080/>.
* Operators run CRUD over the same database from a terminal:
  `ssh admin@localhost -p 2222` (password `admin`).

See the per-subproject READMEs for details — most consumers of the starter only
need to read [`casciian-spring-boot-starter/README.md`](./casciian-spring-boot-starter/README.md).

