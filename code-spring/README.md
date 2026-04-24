# casciian-spring-boot-starter

Spring Boot 3.x auto-configuration that embeds a [Casciian][casciian] TUI
into your Spring application and exposes it over SSH. Drop the jar on the
classpath, point an SSH client at the configured port, and you get a
text-mode admin/management view running in the same JVM as your web app —
with full access to your Spring beans.

## Why

Spring gives you a web view for your users. Sometimes you also want a
terminal view for operators: a richer alternative to a Spring Shell prompt,
reachable over SSH so you can log in from anywhere. That's what this starter
provides. It is modelled on Casciian's telnet demo (`demo.Demo2`) but uses
SSH via [Apache MINA SSHD][sshd], with per-connection `TApplication`
instances so every operator has isolated UI state.

## Getting started

### 1. Add the dependency

```groovy
dependencies {
    implementation 'io.github.crramirez:casciian-spring-boot-starter:0.1.0-SNAPSHOT'
}
```

Requires Java 21 and Spring Boot 3.x.

### 2. Provide a `CasciianTApplicationFactory` bean

The factory is a **singleton** that produces a **fresh `TApplication` per
SSH session**. It can freely inject Spring beans — your repositories,
services, security context — and hand them to each new TUI instance.

```java
@Configuration
public class AdminTuiConfig {

    @Bean
    CasciianTApplicationFactory adminTuiFactory(MyRepository repo,
                                                AuditService audit) {
        return (in, out, session) -> {
            audit.logOpen(session.username(), session.remoteAddress());
            return new AdminApplication(in, out, repo);
        };
    }
}
```

Your `AdminApplication` is any subclass of `casciian.TApplication` whose
constructor takes `(InputStream, OutputStream, …)` and adds whatever
windows, menus, and widgets you want.

### 3. Configure credentials

```yaml
casciian:
  ssh:
    enabled: true        # default
    host: 0.0.0.0        # default
    port: 2222           # default
    username: admin
    password: change-me
    host-key-path: ~/.casciian/ssh_host_key  # default; auto-generated if missing
    banner: "Welcome to Acme Admin TUI"
```

If `username` or `password` is blank, the built-in authenticator rejects
every attempt — declare your own `PasswordAuthenticator` bean (e.g. backed
by Spring Security) to override.

### 4. Connect

```sh
ssh admin@localhost -p 2222
```

## Configuration properties

| Property                      | Default                      | Description                                       |
| ----------------------------- | ---------------------------- | ------------------------------------------------- |
| `casciian.ssh.enabled`        | `true`                       | Turn the whole auto-configuration off.            |
| `casciian.ssh.host`           | `0.0.0.0`                    | Interface to bind.                                |
| `casciian.ssh.port`           | `2222`                       | TCP port.                                         |
| `casciian.ssh.username`       | —                            | Username for the default password auth.           |
| `casciian.ssh.password`       | —                            | Password for the default password auth.           |
| `casciian.ssh.host-key-path`  | `~/.casciian/ssh_host_key`   | Persistent host key (auto-generated on first run).|
| `casciian.ssh.banner`         | —                            | Optional SSH user-auth banner.                    |

## Extension points

Every auto-configured bean is declared with
`@ConditionalOnMissingBean`, so you can override any piece:

| Your bean type                     | Replaces                                     |
| ---------------------------------- | -------------------------------------------- |
| `PasswordAuthenticator`            | Default username/password from properties.   |
| `ShellFactory`                     | The per-connection Casciian shell factory.   |
| `CasciianSshServer`                | The whole `SmartLifecycle`-managed server.   |

## License

Apache License 2.0.

[casciian]: https://github.com/crramirez/casciian
[sshd]: https://mina.apache.org/sshd-project/
