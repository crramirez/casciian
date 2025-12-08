TButton
=======

TButton is a clickable button that has animations for both the down
and up position, and an associated TAction to execute.  The action can
be executed by any of the following:

  * Clicking the button.

  * Pressing kbEnter or kbSpace while the button is activated.

  * Calling TButton.dispatch() .

  * Pressing Alt-{letter}, where {letter} is the mnemonic action
    letter in the button label.


Screenshots
-----------

![tbutton_1](uploads/8262d55f9b252daab8c4458590587239/tbutton_1.png)

In the button above, the label passed in the constructor was
"&MessageBoxes".  The "&" in front of the "M" associates the button
with Alt-M.

Examples
--------

```Java
TButton button = addButton("&MessageBoxes", column, row,
        new TAction() {
            public void DO() {
                new DemoMsgBoxWindow(getApplication());
            }
        }
    );
```

API
---

[TButton API](/TButton.html)
