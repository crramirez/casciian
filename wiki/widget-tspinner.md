TSpinner
========

TSpinner implements a simple up/down spinner.  "up" and "down"
TActions can be specified to execute with each click on the up or down
arrow, respectively.

Screenshots
-----------

![spinner_1](uploads/284a5bdfd09458f6f6f90a0bb6a53769/spinner_1.png)

Examples
--------

```Java
addSpinner(35 + dayOfWeekLabel.getWidth(), row - 1,
    new TAction() {
        public void DO() {
            // Action to perform when the user clicks on the up arrow.
        }
    },
    new TAction() {
        public void DO() {
            // Action to perform when the user clicks on the down arrow.
        }
    }
);
```

API
---

[TSpinner API](/TSpinner.html)
