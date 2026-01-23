# Copilot Instructions

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
