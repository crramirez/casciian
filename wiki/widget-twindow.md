TWindow
=======

TWindow is the top-level container and drawing surface for other widgets.

TWindow is a very special class, serving as the fundamental glue
between TWidget and TApplication:

* Events are only routed to widgets that have windows.

* Widgets can only access the TApplication and Screen through their
  parent window.

* Windows can be system-modal: receiving all events and drawn on top
  of all other windows.




Window Lifecycle
----------------

```mermaid
graph RL;
  I(Constructor) --> A(setActive true)
  A --> F(onFocus)
  F --> WA[Window Active On Screen]
  WA --> UA(setActive false)
  UA --> UF(onUnfocus)
  UF --> WI[Window Inactive On Screen]
  WI --> A
  WA --> H(onHide)
  H --> HW[Window Hidden]
  HW --> S(onShow)
  S --> F
  WA --> PC(onPreClose)
  PC --> UA2(setActive false)
  UA2 --> UF2(onUnFocus)
  UF2 --> G1(Window Removed From List)
  G1 --> C(onClose)
  C --> G2[Window Ready for Garbage Collection]
```

Examples
--------

```Java
application.addWindow("Window title", x, y, width, height);
```

API
---

[TWindow API](/TWindow.html)
