TComboBox
=========

TComboBox is a combobox that has a drop-down list and an edit field.
A TAction can also be executed when the field is updated.  Alt-Down
can be used to show the drop-down; kbEsc will close the drop-down.

Screenshots
-----------

![combobox_1](uploads/f5a2799aab4ddfc67fab41e5772122e9/combobox_1.png)

![combobox_2](uploads/2aa83c98d7b4eeee5c823d1b71677e7b/combobox_2.png)

Examples
--------

```Java
TComboBox comboBox = addComboBox(x, y, 12, comboValues, 2, 6,
    new TAction() {
        public void DO() {
            messageBox("ComboBox", "You chose: " + comboBox.getText(),
                TMessageBox.Type.OK);
        }
    }
);
```

API
---

[TComboBox API](/TComboBox.html)
