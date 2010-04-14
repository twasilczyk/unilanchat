#include "tools_systemintegration_X11StartupNotification.h"
#include "jni_helper.h"

//#include <iostream>
#include <sstream>
#include <string>
#include <X11/Xlib.h> //xorg-x11-libX11-devel

using namespace std;

static void broadcast_xmessage(const string& message);
static string escape_for_xmessage(const string& s);

JNIEXPORT void JNICALL Java_tools_systemintegration_X11StartupNotification_notifyStartupCompleteNative
	(JNIEnv *env, jclass, jstring jStartupID)
{
	string startupID = JNI_jstring2string(env, jStartupID);

//	clog << "(debug) X11StartupNotification.notifyStartupCompleteNative: " << startupID << endl;

	broadcast_xmessage("remove: ID=" + escape_for_xmessage(startupID));
}

class XDisplay
{
	public:
	XDisplay()
	{
		display = XOpenDisplay(0);
	}

	~XDisplay()
	{
		XFlush(display);
		XCloseDisplay(display);
	}

	Atom getAtomByName(const char* atom_name)
	{
		return XInternAtom(display, atom_name, False);
	}

	Display* display;
};

static string escape_for_xmessage(const string& s)
{
	ostringstream oss;
	for (string::const_iterator it = s.begin(); it != s.end(); ++it)
	{
	if (*it == ' ' || *it == '"' || *it == '\\')
		oss << '\\';
	oss << *it;
	}
	return oss.str();
}

static void broadcast_xmessage(const string& message)
{
	XDisplay xdisplay;
	Window xroot_window = DefaultRootWindow(xdisplay.display);

	XSetWindowAttributes attrs;
	attrs.override_redirect = True;
	attrs.event_mask = PropertyChangeMask | StructureNotifyMask;
	Window xwindow = XCreateWindow(xdisplay.display, xroot_window, -100, -100, 1, 1, 0,
		CopyFromParent, CopyFromParent, CopyFromParent, CWOverrideRedirect | CWEventMask, &attrs);

	Atom type_atom = xdisplay.getAtomByName("_NET_STARTUP_INFO");
	Atom type_atom_begin = xdisplay.getAtomByName("_NET_STARTUP_INFO_BEGIN");

	XEvent xevent;
	xevent.xclient.type = ClientMessage;
	xevent.xclient.message_type = type_atom_begin;
	xevent.xclient.display = xdisplay.display;
	xevent.xclient.window = xwindow;
	xevent.xclient.format = 8;

	const char* src = message.c_str();
	const char* src_end = src + message.length() + 1; // Include trailing NUL

	while (src != src_end)
	{
		char* dest = &xevent.xclient.data.b[0];
		char* dest_end = dest + 20;
		while (dest != dest_end && src != src_end)
			*dest++ = *src++;
		while (dest != dest_end)
			*dest++ = 0;
		XSendEvent(xdisplay.display, xroot_window, False, PropertyChangeMask, &xevent);
		xevent.xclient.message_type = type_atom;
	}

	XDestroyWindow(xdisplay.display, xwindow);
}
