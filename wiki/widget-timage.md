TImage
======

TImage renders a piece of a bitmap image on screen.  Keyboard bindings
are:

* Alt-Left, Alt-Right, '+', and '-' will zoom the image
  larger/smaller.

* Alt-Up and Alt-Down will rotate the image.

* Shift-Left and Shift-Right will select between scaling/stretch
  options.


Screenshots
-----------

An image loaded in TImage is shown below.  This is with no
scaling/stretching.  The pixels in the text cells that are not covered
by the image are black (the border at right and bottom).

![image_1](uploads/aed360cbce2c324aa72530b770cf661c/image_1.png)

The same image, but now with TImage.Scale.STRETCH scaling is shown
below.  All of the text cells are covered, but the image is distorted.

![image_2](uploads/d9a7290f00f49a099068f33c00c79575/image_2.png)

The same image, but now with TImage.Scale.SCALE scaling is shown
below.  The image is made as large as possible to fit the text area
maintaining its aspect ratio and centered.

![image_3](uploads/c532f4eb2f6e87e1f80ef782d4a18c4b/image_3.png)

Examples
--------

```Java
BufferedImage image = ImageIO.read(file);
TImage imageField = new TImage(this, 0, 0, getWidth() - 2, getHeight() - 2,
    image, 0, 0, null);
```

API
---

[TImage API](/TImage.html)
