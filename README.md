# UniPlace: Internship & Placement Portal

UniPlace is the internship and placement system delivered by Halcyon Campus Software to the university. This
package is the vendor delivery under acceptance testing. The specification it was built against is the
Requirements document published on D2L.

## Contents

| Path | What it is |
| --- | --- |
| `portal/` | UniPlace Portal: Java 17, Spring Boot 3, Thymeleaf, Spring Data JPA, H2, Spring Security. Full source code and its test suite. |
| `bin/uniplace-portal.jar` | The same Portal, prebuilt, for running without downloading anything. Not kept in Git (see `.gitignore`): a fresh clone of your team repository does not have it, copy it from this package. |
| `engine/uniplace-engine.jar` | Matching & Compliance Engine, the REST service the Portal calls. Runs on Windows, macOS and Linux with Java. |
| `engine/openapi.yaml` | The Engine API contract (OpenAPI 3). |
| `start.sh`, `start.cmd` | Start both parts. |
| `ai-eval/` | Fictional motivation letters with reference score bands, for evaluating the AI Assistant. |
| `docs/`, `tests/` | Starting points for the team's documents and tests. |
| `.github/` | Issue template, label list, and the workflows *Tests* (runs the Portal tests on every push) and *Create labels*. |

## Requirements

- A **JDK 17 or later** (Java 21 also works). A JRE is not enough to build and test the Portal. Without
  administrator rights, unzip a portable JDK (for example the Eclipse Temurin 17 JDK `.zip`) and put its `bin`
  folder on the PATH.
- **Git**, to work with your team repository.

Unzip the project to a local folder that is not synchronised by OneDrive or similar (for example
`C:\Users\<you>\uniplace`): synchronisation locks the database and build files. On macOS / Linux, if the scripts
lost their execute permission when unzipping, run `chmod +x start.sh portal/mvnw`.

`start.cmd` finds Java on its own. To run `mvnw.cmd` yourself, `JAVA_HOME` must point to the JDK folder, for
example in PowerShell: `$env:JAVA_HOME = "C:\Users\<you>\jdk-17"` (for the current window only).

## Running

Windows:

```powershell
.\start.cmd             # start with the existing data
.\start.cmd --reset     # delete the Portal data and load the demonstration data again (stop the Portal first)
.\start.cmd --jar       # run the prebuilt Portal: nothing is downloaded (can be combined with --reset)
```

macOS / Linux:

```bash
./start.sh
./start.sh --reset
./start.sh --jar
```

Then open http://127.0.0.1:8080.

- Without `--jar`, the Portal is built from `portal/` with the Maven wrapper; the first start downloads Maven and
  the dependencies (internet access needed) and takes a few minutes. With `--jar`, it starts in seconds.
- When started by the scripts, the Engine writes its log to `logs/engine.log`; its health endpoint is `GET /api/v1/health`.
- Both parts listen only on this computer (127.0.0.1): the Portal on port 8080, the Engine on port 8081. Nothing is
  reachable from other computers, and the firewall does not ask for permission.
- The Engine can also be started alone: `java -jar engine/uniplace-engine.jar`. An optional
  first argument, or the `ENGINE_PORT` environment variable, sets the port.
- The Portal keeps its data in `portal/data/` (H2 database and uploaded files). Data survives restarts; use
  `--reset` to go back to the demonstration data.
- Stop with Ctrl+C. On Windows, also close the minimised Engine window.

## Lab computers

Only a JDK 17 (or later) and Git are needed. No administrator rights or firewall rules are needed: both parts only use
127.0.0.1. If the network blocks the Maven downloads, use `--jar` to run the Portal and see the next section.

## Maven behind a proxy

If the first start (without `--jar`) stops with errors about downloading dependencies on a network that needs a
proxy, create the file `.m2/settings.xml` in your home folder (Windows: `C:\Users\<you>\.m2\settings.xml`) with
the proxy that your browser uses:

```xml
<settings>
  <proxies>
    <proxy>
      <id>campus</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>proxy.example.edu</host>
      <port>8080</port>
      <!-- <username>you</username> <password>secret</password> only if the proxy asks for a login -->
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

Add a second `<proxy>` entry with `<protocol>http</protocol>` if the proxy is also used for plain HTTP. The very first
start also downloads Maven itself, which does not read this file. For that download, set the proxy in the
`MAVEN_OPTS` environment variable as well (same host and port as above):

- macOS / Linux: `export MAVEN_OPTS="-Dhttps.proxyHost=proxy.example.edu -Dhttps.proxyPort=8080"`
- Windows PowerShell: `$env:MAVEN_OPTS="-Dhttps.proxyHost=proxy.example.edu -Dhttps.proxyPort=8080"`

These lines last for the current terminal window only. Until the proxy is sorted out, `--jar` runs the prebuilt
Portal; the tests (`mvnw test`) still need the downloads.

## Demonstration accounts

Password for every account: `Uniplace!2026`

| Role | Username |
| --- | --- |
| Coordinator | `coordinator` |
| Recruiters (QTech Solutions) | `recruiter.qtech`, `recruiter2.qtech` |
| Recruiter (Gulf Bank) | `recruiter.gulfbank` |
| Recruiter (Doha Energy Partners) | `recruiter.dohaenergy` |
| Recruiter (Lusail Health Group) | `recruiter.lusail` |
| Recruiter (Pearl Logistics) | `recruiter.pearl` |
| Recruiter (Desert Bloom Media, pending approval) | `recruiter.desertbloom` |
| Recruiter without a company | `recruiter.newco` |
| Students | `student.amal`, `student.omar`, `student.khalid`, `student.noor`, `student.yousef`, `student.amna`, and the other students: see Coordinator → Users (`student.` + first name in lower case; when a first name repeats, a number is added, for example `student.fatima2`) |

The demonstration data is generated relative to the date of the first start: deadlines, interviews and
offers are placed around that day.

## AI Assistant

Coordinator → Settings: choose the **AI provider**, paste the API key and the model name, press **Save settings**,
then **Test connection**. Choosing a provider fills in its base URL and an example model.

| Provider | Base URL | Model |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1` | e.g. `gpt-4o-mini` |
| Azure OpenAI (v1 endpoint) | `https://<your-resource>.openai.azure.com/openai/v1` | your deployment name |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` | e.g. `gemini-2.0-flash` |
| OpenRouter (recommended) | `https://openrouter.ai/api/v1` | any model ending in `:free`, e.g. `nvidia/nemotron-3-ultra-550b-a55b:free` (about 50 requests a day per account; if a free model is busy, pick another one) |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| Ollama (local, no key) | `http://localhost:11434/v1` | a model you have pulled, e.g. `llama3.2` |
| Other OpenAI-compatible | its base URL | its model |

Without a key (except Ollama) the built-in simulated assistant answers. The key is stored only in your local
installation and is shown on screen only by its last four characters. All demonstration data is fictional; never type real personal
data into the system.

## Volume data

Coordinator → Tools → Generate test data adds synthetic students, postings, applications and notifications
(up to 10,000 applications per run) for performance testing.

## Tests

```bash
cd portal
./mvnw test          # Windows: .\mvnw.cmd test
```

When some tests fail, Maven stops before writing the coverage report. To get the report anyway:
`./mvnw test -Dmaven.test.failure.ignore=true` (Windows PowerShell: `.\mvnw.cmd test "-Dmaven.test.failure.ignore=true"`).

Reports: `portal/target/surefire-reports`; line coverage (JaCoCo): `portal/target/site/jacoco/index.html`.
