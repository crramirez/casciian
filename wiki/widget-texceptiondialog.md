TExceptionDialog
================

TExceptionDialog displays an exception and its stack trace to the
user, and provides a means to save a troubleshooting report for
support.

Screenshots
-----------

![exceptiondialog_1](uploads/99bfb6138eaa079c51a5234ff93ef572/exceptiondialog_1.png)

Examples
--------

```Java
try {
    // Do something that might generate IOException...
} catch (IOException e) {
    // Show this exception to the user.
    new TExceptionDialog(this, e);
}
```

API
---

[TExceptionDialog API](/TExceptionDialog.html)
