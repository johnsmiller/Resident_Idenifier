package com.sjsurha.resident_identifier;

//REWRITING PROGRAM TO NOT BE RELIANT UPON THIS SOLUTION. A PLATFORM DEPENDANT SOLUTION & IS UGLY APPROACH

//Retrieved from: http://www.camick.com/java/source/RequestFocusListener.java
//Used for element to grab focus in JDialogs while maintaining default return (enter) key behavior
//THIS WORK IS NOT MY OWN. PROPER AUTHOR INFORMATION WILL BE ADDED ONCE FOUND

import javax.swing.*;
import javax.swing.event.*;

/**
 *  Convenience class to request focus on a component.
 *
 *  When the component is added to a realized Window then component will
 *  request focus immediately, since the ancestorAdded event is fired
 *  immediately.
 *
 *  When the component is added to a non realized Window, then the focus
 *  request will be made once the window is realized, since the
 *  ancestorAdded event will not be fired until then.
 *
 *  Using the default constructor will cause the listener to be removed
 *  from the component once the AncestorEvent is generated. A second constructor
 *  allows you to specify a boolean value of false to prevent the
 *  AncestorListener from being removed when the event is generated. This will
 *  allow you to reuse the listener each time the event is generated.
 */
public class RequestFocusListener implements AncestorListener
{
	private boolean removeListener;

	/*
	 *  Convenience constructor. The listener is only used once and then it is
	 *  removed from the component.
	 */
	public RequestFocusListener()
	{
		this(true);
	}

	/*
	 *  Constructor that controls whether this listen can be used once or
	 *  multiple times.
	 *
	 *  @param removeListener when true this listener is only invoked once
	 *                        otherwise it can be invoked multiple times.
	 */
	public RequestFocusListener(boolean removeListener)
	{
		this.removeListener = removeListener;
	}

	@Override
	public void ancestorAdded(AncestorEvent e)
	{
		JComponent component = e.getComponent();
		component.requestFocusInWindow();

		if (removeListener)
			component.removeAncestorListener( this );
	}

	@Override
	public void ancestorMoved(AncestorEvent e) {}

	@Override
	public void ancestorRemoved(AncestorEvent e) {}
}