TTreeView
=========

TTreeView is a simple tree view, provided as either a widget
(TTreeView) or a window (TTreeViewWindow).

Screenshots
-----------

![treeview_1](uploads/e3c796eaa74a42752911ccd4d07ce569/treeview_1.png)

![treeview_2](uploads/8c0d0a641842237a7a95699fddb6a72e/treeview_2.png)

Examples
--------

```Java
treeView = addTreeViewWidget(x, y, width, height);
new TDirectoryTreeItem(treeView, ".", true);
```

```Java
public class ProjectWindow extends TTreeViewWindow {

    private ProjectWindowTreeItem root;

    public ProjectWindow(final TApplication parent, final Project project) {

        super(parent, i18n.getString("windowTitle"),
            project.getWindowLeft(), project.getWindowTop() - 1,
            project.getWindowWidth(), project.getWindowHeight(),
            RESIZABLE | ABSOLUTEXY, null);

        // Load the treeview
        root = new ProjectWindowTreeItem(this, project);
        for (Target target: project.getTargets()) {
            root.addChildTarget(this, target);
        }
        reflowData();
}
```

API
---

[TTreeViewWidget API](/TTreeViewWidget.html)

[TTreeViewWindow API](/TTreeViewWindow.html)
