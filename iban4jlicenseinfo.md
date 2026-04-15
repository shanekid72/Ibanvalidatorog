Confirmed directly from the library's POM and embedded copyright header. Here's the verdict:

## ✅ iban4j is free for everyone — Apache License 2.0

**Source of truth (from `iban4j-3.2.11-RELEASE.pom` we have on disk):**

```
Copyright 2013 Artur Mkrtchyan
Licensed under the Apache License, Version 2.0
```

```xml
<licenses>
  <license>
    <name>Apache License 2.0</name>
    <url>https://www.apache.org/licenses/LICENSE-2.0</url>
  </license>
</licenses>
```

GitHub project: https://github.com/arturmkrtchyan/iban4j

---

### What Apache 2.0 means in plain English

You **can**, royalty-free, for personal or commercial use:
- ✅ Use, copy, modify, distribute, sublicense
- ✅ Bundle it into proprietary/closed-source products
- ✅ Use it in SaaS / hosted services
- ✅ Use it in commercial banking, fintech, enterprise apps
- ✅ Distribute compiled code without sharing your source

You **must**:
- 📄 Include a copy of the Apache 2.0 license text with any redistribution that contains iban4j (binary or source)
- 📝 Preserve the copyright notice (`Copyright 2013 Artur Mkrtchyan`) and any existing NOTICE file
- ⚠️ State significant changes if you modified the library itself
- 🚫 Not use the iban4j name/marks for endorsement

You **don't have to**:
- ❌ Open-source your own code (it's permissive, not copyleft like GPL)
- ❌ Pay anything
- ❌ Give attribution in your UI (only in source/binary distributions)

---

### Practical implications for *your* project

Since you're building a **server-side API** (not redistributing iban4j as a binary to end users):

- For internal/SaaS use → **no extra paperwork needed**. You're just running it on your server.
- If you ship a Docker image, on-prem installer, or downloadable JAR to customers → include the Apache 2.0 license text and a NOTICE-style attribution somewhere in the artifact (e.g. a `THIRD-PARTY-LICENSES.txt` file, or in your About page).

A simple way to handle attribution automatically is the Maven **license plugin**:

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>license-maven-plugin</artifactId>
  <version>2.4.0</version>
  <executions>
    <execution>
      </goals>
    </execution>
  </executions>
</plugin>
```

Run `mvn license:add-third-party` and it generates `target/generated-sources/license/THIRD-PARTY.txt` listing all transitive dependencies and their licenses — drop that into your release artifact and you're covered.

---

### Bottom line
**Yes, iban4j is 100% free to use** — for hobby, internal tools, commercial SaaS, fintech, banking — anywhere. The only obligation is the standard Apache 2.0 attribution if/when you redistribute the library itself.