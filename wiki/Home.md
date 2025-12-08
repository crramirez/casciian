Casciian - Java Text User Interface Library
===========================================

Casciian is a text-based windowing system loosely reminiscent of
Borland's [Turbo Vision](http://en.wikipedia.org/wiki/Turbo_Vision)
system.  Casciian works on both command-line [Xterm-like
terminals](terminals) and on X11/Mac/Windows GUI systems using Swing.



Documentation
-------------

* Writing Casciian applications:
  - Casciian's [high-level design.](high-level-design)
  - Casciian [java.lang.System properties.](System-Properties)
* Xterm backend:
  - [Supported terminals.](terminals)
  - [Other Casciian-specific escape sequences.](casciian-sequences)
* [Known limitations.](limitations)



Examples
--------

* [Hello World.](hello-world)
* The [official demo.](demo-application)



Widget Gallery
--------------

* [TButton](widget-tbutton)
* [TCalendar](widget-tcalendar)
* [TCheckBox](widget-tcheckbox)
* [TComboBox](widget-tcombobox)
* [TDirectoryList](widget-tdirectorylist)
* [TEditColorThemeWindow](widget-teditcolorthemewindow)
* [TEditDesktopStyleWindow](widget-teditdesktopstylewindow)
* [TEditor](widget-teditorwidget)
* [TEditorWindow](widget-teditorwindow)
* [TExceptionDialog](widget-texceptiondialog)
* [TField](widget-tfield)
* [TFileOpenBox](widget-tfileopenbox)
* [THelpWindow](widget-thelpwindow)
* [THScroller](widget-thscroller)
* [TImage](widget-timage)
* [TImageWindow](widget-timagewindow)
* [TInputBox](widget-tinputbox)
* [TLabel](widget-tlabel)
* [TList](widget-tlist)
* [TMessageBox](widget-tmessagebox)
* [TPanel](widget-tpanel)
* [TPasswordField](widget-tpasswordfield)
* [TProgressBar](widget-tprogressbar)
* [TRadioGroup](widget-tradiogroup)
* [TScreenOptionsWindow](widget-tscreenoptionswindow)
* [TScrollableWidget](widget-tscrollablewidget)
* [TScrollableWindow](widget-tscrollablewindow)
* [TSpinner](widget-tspinner)
* [TSplitPane](widget-tsplitpane)
* [TStatusBar](widget-tstatusbar)
* [TTable](widget-ttablewidget)
* [TTableWindow](widget-ttablewindow)
* [TTerminalInformationWindow](widget-tterminalinformationwindow)
* [TTerminal](widget-tterminalwidget)
* [TTerminalWindow](widget-tterminalwindow)
* [TText](widget-ttext)
* [TTextPicture](widget-ttextpicture)
* [TTextPictureWindow](widget-ttextpicturewindow)
* [TTreeView](widget-ttreeview)
* [TVScroller](widget-tvscroller)
* [TWindow](widget-twindow)


Optional Layout Managers
------------------------

Casciian defaults to absolute positioning of elements within a window.
However, additional layout managers that will respond correctly to
window resizing events are available:

* [BoxLayoutManager](layout-box)
* [StretchLayoutManager](layout-stretch)
