Hello World For Casciian
========================

The following code below is all that is needed to create a basic
application:

```Java
import casciian.TApplication;

public class MyApplication extends TApplication {

    public MyApplication() throws Exception {
        super(BackendType.XTERM);

        // Create standard menus for Tool, File, and Window.
        addToolMenu();
        addFileMenu();
        addWindowMenu();
    }

    public static void main(String [] args) throws Exception {
        MyApplication app = new MyApplication();
        app.run();
    }
}
```

Compile it with 'javac -cp path/to/casciian.jar MyApplication.java', and run it with 'java -cp path/to/casciian.jar:. MyApplication' .  (You can use your IDE to pull casciian from Maven, and just drop the code above into a new class called MyApplication.)

This will create an application with three menus:

![hello_world_1](uploads/27d9bc85bed2467ef16fe791ed33639c/hello_world_1.png)

The tools menu (with the hamburger icon on left) has items for repainting the desktop, opening an image file, and changing the font:

![hello_world_2](uploads/7e9a3256a3d12e7fe2d2dfa6ff09a1c9/hello_world_2.png)

The file menu has options for spawning a terminal window and exiting.  It is important to have some means of exiting your application, especially for the Xterm backend.  (The Swing backend will automatically exit if you close the frame.)

You can use File-->OS Shell to spawn a terminal.  The terminal itself is a good one: it can pass vttest, handles keyboard and mouse, and can even run Casciian inside.  Here is shot of the vttest SGR colors screen:

![hello_world_5](uploads/18388519fab6fbbe6df8567c264fc57f/hello_world_5.png)

Here is another shot where the host xterm window was made large enough for two terminal windows side-by-side, Midnight Commander running on the left terminal and the Casciian demo application running on the right:

![hello_world_6](uploads/92364f5e4f95828cdc99dd0e722579f6/hello_world_6.png)

When you are ready to exit, select File --> Exit, or press Alt-X.  This brings up a system-modal message box for confirmation:

![hello_world_7](uploads/e26e201333c14a8f85016a618788141e/hello_world_7.png)

Commentary
----------

This example has MyApplication subclass TApplication, and call some functions to add menus in its constructor.  One could also just create TApplication directly, and call the functions on it:

```Java
import casciian.TApplication;

public class HelloWorld {

    public static void main(String [] args) throws Exception {
        TApplication app = new TApplication(TApplication.BackendType.XTERM);
        app.addToolMenu();
        app.addFileMenu();
        app.addWindowMenu();
        app.run();
    }
}
```

