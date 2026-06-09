# Copilot Instructions

## License Headers

Every new Java source file must use the Apache 2.0 license header with
Copyright 2025 Carlos Rafael Ramirez:

```java
/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
```

When **modifying** a Java source file that was created in the first commit of
the repository (originally written by Autumn Lamonte and dedicated to the
public domain via CC0), replace the existing CC0 header with:

```java
/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
```

## Unit Testing Guidelines

When creating unit tests for this project, follow these guidelines:

### Testing Approach
- **Black-box testing only**: Test the API, not the implementation details
- **Use AssertJ and Mockito**: These are the preferred testing libraries
- **Avoid 1:1 test-to-class mapping**: One unit test can test many classes if they are chained and part of one API call
- **Test business logic only**: Don't test constant values, external libraries, or JDK functionality (like `Files`)

### Code Structure for Testability
- **Extract business logic into separate testable classes**: When functionality is tightly coupled to UI or frameworks, extract it into standalone classes (like `FileTypeDetector`) to enable unit testing
- **Keep tests focused**: Don't create obvious tests that don't add value

### When to Create Tests
- Always try to create tests for changes if they are valuable or can be used for regression testing
- Prioritize tests that verify important business logic and edge cases
