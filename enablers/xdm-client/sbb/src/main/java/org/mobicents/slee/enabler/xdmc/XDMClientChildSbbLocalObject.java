package org.mobicents.slee.enabler.xdmc;

import org.mobicents.slee.enabler.sip.SubscriptionClientParentSbbLocalObject;

/**
 * Client interface to interact with an XDM Server. IF used by an sbb in a child
 * relation, then that sbb's local object must implement
 * {@link XDMClientParentSbbLocalObject}.
 * 
 * @author martins
 * 
 */
public interface XDMClientChildSbbLocalObject extends SubscriptionClientParentSbbLocalObject, XDMClientChild {

}
