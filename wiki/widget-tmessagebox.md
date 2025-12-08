TMessageBox
===========

TMessageBox is a system-modal dialog with buttons for OK, Cancel, Yes, or No.

Screenshots
-----------

![messagebox_1](uploads/d869bda995331fd9c814c70fdc43203f/messagebox_1.png)

![messagebox_2](uploads/6ba07a232dc62648b2f2d9f15888c237/messagebox_2.png)

Examples
--------

```Java
TMessageBox box = messageBox(title, caption,
    TMessageBox.Type.OK | TMessageBox.Type.CANCEL);
if (box.getResult() == TMessageBox.OK) {
    // ... the user pressed OK, do stuff ...
}
```

The above example is equivalent to:

```Java
if (messageBox(title, caption, TMessageBox.Type.OK | TMessageBox.Type.CANCEL).isOk()) {
    // ... the user pressed OK, do stuff ...
}
```

API
---

[TMessageBox API](/TMessageBox.html)
