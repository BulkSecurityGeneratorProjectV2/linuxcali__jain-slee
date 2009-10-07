package org.mobicents.slee.resource.diameter.base;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.naming.OperationNotSupportedException;
import javax.slee.resource.SleeEndpoint;

import net.java.slee.resource.diameter.base.DiameterActivity;
import net.java.slee.resource.diameter.base.DiameterAvpFactory;
import net.java.slee.resource.diameter.base.DiameterMessageFactory;
import net.java.slee.resource.diameter.base.events.DiameterMessage;
import net.java.slee.resource.diameter.base.events.avp.AvpNotAllowedException;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;

import org.apache.log4j.Logger;
import org.jdiameter.api.Answer;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Message;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.jdiameter.common.impl.validation.JAvpNotAllowedException;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;
import org.mobicents.slee.resource.diameter.base.handlers.BaseSessionCreationListener;

/**
 * 
 * Implementation of {@link DiameterActivity}.
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class DiameterActivityImpl implements DiameterActivity {

  protected static Logger logger = Logger.getLogger(DiameterActivityImpl.class);

  protected Session session = null;
  protected String sessionId = null;
  
  protected DiameterActivityHandle handle = null;
  
  protected DiameterMessageFactory messageFactory = null;
  protected DiameterAvpFactory avpFactory = null;
  
  protected EventListener<Request, Answer> raEventListener = null;
  protected DiameterIdentity destinationHost = null;
  protected DiameterIdentity destinationRealm = null;
  protected SleeEndpoint endpoint = null;

  protected BaseSessionCreationListener baseListener = null;

  protected boolean terminateAfterProcessing = false;

  public DiameterActivityImpl(DiameterMessageFactory messageFactory, DiameterAvpFactory avpFactory, Session session, EventListener<Request, Answer> raEventListener, DiameterIdentity destinationHost, DiameterIdentity destinationRealm, SleeEndpoint endpoint)
  {
    super();

    this.messageFactory = messageFactory;
    this.avpFactory = avpFactory;
    this.session = session;
    this.raEventListener = raEventListener;

    this.destinationHost = destinationHost;
    this.destinationRealm = destinationRealm;

    if(session != null) {
      this.setCurrentWorkingSession(session);
      this.sessionId = session.getSessionId();
    }

    this.endpoint = endpoint;
  }

  protected void setCurrentWorkingSession(Session session) {
    this.sessionId = session.getSessionId();
    this.session = session;

    if (this.handle == null) {
      this.handle = new DiameterActivityHandle(this.sessionId);
    }
  }

  public void endActivity()
  {
    if(session != null) {
      session.release();
    }

    try {
      endpoint.endActivity(this.getActivityHandle());
    }
    catch (Exception e) {
      logger.error("Failed to end activity [" + this + "].", e);
    }
  }

  public DiameterAvpFactory getDiameterAvpFactory() {
    return this.avpFactory;
  }

  public DiameterMessageFactory getDiameterMessageFactory() {
    return this.messageFactory;
  }

  public String getSessionId() {
    return this.sessionId;
  }

  public void sendMessage(DiameterMessage message) throws IOException {
    // FIXME: baranowb - this is async send?
    try {
      if (message instanceof DiameterMessageImpl) {
        DiameterMessageImpl msg = (DiameterMessageImpl) message;
        this.session.send(msg.getGenericData(), this.raEventListener);
        // FIXME: baranowb; get dest host and realm :], possibly some other avps
      }
      else {
        throw new OperationNotSupportedException("Trying to send wrong type of message? [" + message.getClass() + "] \n" + message);
      }
    }
    catch (JAvpNotAllowedException e) {
      throw new AvpNotAllowedException("Message validation failed.", e, e.getAvpCode(), e.getVendorId());
    }
    catch (Exception e) {
      throw new IOException("Failed to send message, due to: " + e);
    }
  }

  // ============= IMPL methods

  public DiameterActivityHandle getActivityHandle() {
    return this.handle;
  }

  public DiameterMessage sendSyncMessage(DiameterMessage message) {
    try {
      if (message instanceof DiameterMessageImpl) {
        DiameterMessageImpl msg = (DiameterMessageImpl) message;
        Future<Message> response = this.session.send( msg.getGenericData());
        // FIXME: alexandre: get dest host and realm, possibly some other avps

        return (DiameterMessage) response.get();
      }
      else {
        throw new OperationNotSupportedException("Trying to send wrong type of message? [" + message.getClass() + "] \n" + message);
      }
    }
    catch (JAvpNotAllowedException e) {
      throw new AvpNotAllowedException("Message validation failed.", e, e.getAvpCode(), e.getVendorId());
    }
    catch (Exception e) {
      logger.error("Failure sending sync request.", e);
    }

    return null;
  }

  public boolean isValid() {
    return this.session.isValid();
  }

  public Object getSessionListener() {
    return baseListener;
  }

  public void setSessionListener(Object ra) {
    this.baseListener = (BaseSessionCreationListener) ra;
  }

  protected void clean() {
    this.session = null;
    this.handle = null;
    this.avpFactory = null;
    this.raEventListener = null;
    this.avpFactory = null;
    this.handle = null;
  }

  public void setTerminateAfterProcessing(boolean terminateAfterProcessing) {
    this.terminateAfterProcessing = terminateAfterProcessing;
  }
  
  public boolean isTerminateAfterProcessing() {
    return terminateAfterProcessing;
  }
}
