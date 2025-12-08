TFileOpenBox
============

TFileOpenBox is a system-modal dialog for selecting a file to open or save.

Screenshots
-----------

![fileopenbox_1](uploads/ef2b70e337afc797236b3c24b07a6e44/fileopenbox_1.png)

Examples
--------

```Java
filename = fileOpenBox("/path/to/file.ext", TFileOpenBox.Type.OPEN);
if (filename != null) {
    // ... the user selected a file, go open it ...
}
```

API
---

[TFileOpenBox API](/TFileOpenBox.html)
