StretchLayoutManager
====================

StrechLayoutManager repositions child widgets based on their
coordinates when added and the current widget size.

Screenshots
-----------

Below is the normal demo main window.  The widgets are all initially
added using fixed coordinates.

![stretchlayout_1](uploads/65bbcea57b8323469e1af03e80c525d4/stretchlayout_1.png)

When the window is made larger, the widgets' X, Y, width, and height
values are changed to be similar to the relative positions as they
were orginally were.

![stretchlayout_2](uploads/a0e16686e9118b798de488a9f02fbb92/stretchlayout_2.png)

If the window is further shrunk, widgets can cover each other as seen
below.

![stretchlayout_3](uploads/33f2e8c04557983420a86759a3f7f304/stretchlayout_3.png)

Examples
--------

```Java
setLayoutManager(new StretchLayoutManager(width, height);
```

API
---

[StretchLayoutManager API](/layout/StretchLayoutManager.html)
