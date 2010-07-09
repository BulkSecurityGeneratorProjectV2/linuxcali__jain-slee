package org.mobicents.slee.resource.diameter.cxdx;

import java.io.IOException;

import javax.slee.resource.SleeEndpoint;

import net.java.slee.resource.diameter.base.events.avp.AvpNotAllowedException;
import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;
import net.java.slee.resource.diameter.cxdx.CxDxAVPFactory;
import net.java.slee.resource.diameter.cxdx.CxDxClientSessionActivity;
import net.java.slee.resource.diameter.cxdx.CxDxMessageFactory;
import net.java.slee.resource.diameter.cxdx.events.LocationInfoRequest;
import net.java.slee.resource.diameter.cxdx.events.MultimediaAuthenticationRequest;
import net.java.slee.resource.diameter.cxdx.events.PushProfileAnswer;
import net.java.slee.resource.diameter.cxdx.events.PushProfileRequest;
import net.java.slee.resource.diameter.cxdx.events.RegistrationTerminationAnswer;
import net.java.slee.resource.diameter.cxdx.events.RegistrationTerminationRequest;
import net.java.slee.resource.diameter.cxdx.events.ServerAssignmentRequest;
import net.java.slee.resource.diameter.cxdx.events.UserAuthorizationRequest;

import org.jdiameter.api.Answer;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Request;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.cxdx.ClientCxDxSession;
import org.jdiameter.common.api.app.cxdx.CxDxSessionState;
import org.jdiameter.common.impl.app.cxdx.JLocationInfoRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JMultimediaAuthRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JPushProfileAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JRegistrationTerminationAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JServerAssignmentRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JUserAuthorizationRequestImpl;
import org.jdiameter.common.impl.validation.JAvpNotAllowedException;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;

/**
 *
 * CxDxClientSessionImpl.java
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class CxDxClientSessionImpl extends CxDxSessionImpl implements CxDxClientSessionActivity {

  protected ClientCxDxSession appSession;

  /**
   * @param messageFactory
   * @param avpFactory
   * @param session
   * @param raEventListener
   * @param timeout
   * @param destinationHost
   * @param destinationRealm
   * @param endpoint
   */
  public CxDxClientSessionImpl(CxDxMessageFactory messageFactory, CxDxAVPFactory avpFactory, ClientCxDxSession session, EventListener<Request, Answer> raEventListener, DiameterIdentity destinationHost, DiameterIdentity destinationRealm, SleeEndpoint endpoint) {
    super(messageFactory, avpFactory, session.getSessions().get(0), raEventListener, destinationHost, destinationRealm, endpoint);
    this.appSession = session;
    this.appSession.addStateChangeNotification(this);
  }



  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#createLocationInfoRequest()
   */
  public LocationInfoRequest createLocationInfoRequest() {
    // Create the request
    LocationInfoRequest lir = cxdxMessageFactory.createLocationInfoRequest(super.getSessionId());

    // Fill session related AVPs, if present
    fillSessionAVPs(lir);

    return lir;
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#createMultimediaAuthenticationRequest()
   */
  public MultimediaAuthenticationRequest createMultimediaAuthenticationRequest() {
    // Create the request
    MultimediaAuthenticationRequest mar = cxdxMessageFactory.createMultimediaAuthenticationRequest(super.getSessionId());

    // Fill session related AVPs, if present
    fillSessionAVPs(mar);

    return mar;
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#createPushProfileAnswer()
   */
  public PushProfileAnswer createPushProfileAnswer() {
    // Make sure we have the correct type of Request
    if (!(lastRequest instanceof PushProfileRequest)) {
      logger.warn("Invalid type of answer for this activity.");
      return null;
    }

    try {
      // Create the answer
      PushProfileAnswer ppa = (PushProfileAnswer) this.cxdxMessageFactory.createCxDxMessage(lastRequest.getHeader(), new DiameterAvp[]{}, PushProfileAnswer.COMMAND_CODE, CxDxMessageFactoryImpl.cxdxAppId);

      // Fill session related AVPs, if present
      fillSessionAVPs(ppa);

      return ppa;
    }
    catch (InternalException e) {
      logger.error("Failed to create Push-Profile-Answer.", e);
    }

    return null;
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#createRegistrationTerminationRequest()
   */
  public RegistrationTerminationAnswer createRegistrationTerminationAnswer() {
    // Make sure we have the correct type of Request
    if (!(lastRequest instanceof RegistrationTerminationRequest)) {
      logger.warn("Invalid type of answer for this activity.");
      return null;
    }

    try {
      // Create the answer
      RegistrationTerminationAnswer rta = (RegistrationTerminationAnswer) this.cxdxMessageFactory.createCxDxMessage(lastRequest.getHeader(), new DiameterAvp[]{}, RegistrationTerminationAnswer.COMMAND_CODE, CxDxMessageFactoryImpl.cxdxAppId);

      // Fill session related AVPs, if present
      fillSessionAVPs(rta);

      return rta;
    }
    catch (InternalException e) {
      logger.error("Failed to create Registration-Termination-Answer.", e);
    }

    return null;
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#createServerAssignmentRequest()
   */
  public ServerAssignmentRequest createServerAssignmentRequest() {
    // Create the request
    ServerAssignmentRequest sar = cxdxMessageFactory.createServerAssignmentRequest(super.getSessionId());

    // Fill session related AVPs, if present
    fillSessionAVPs(sar);

    return sar;
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#createUserAuthorizationRequest()
   */
  public UserAuthorizationRequest createUserAuthorizationRequest() {
    // Create the request
    UserAuthorizationRequest uar = cxdxMessageFactory.createUserAuthorizationRequest(super.getSessionId());

    // Fill session related AVPs, if present
    fillSessionAVPs(uar);

    return uar;
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#sendLocationInfoRequest(net.java.slee.resource.diameter.cxdx.events.LocationInfoRequest)
   */
  public void sendLocationInfoRequest(LocationInfoRequest locationInfoRequest) throws IOException {
    try{
      DiameterMessageImpl msg = (DiameterMessageImpl) locationInfoRequest;

      appSession.sendLocationInformationRequest(new JLocationInfoRequestImpl(msg.getGenericData()));
    }
    catch(JAvpNotAllowedException anae) {
      throw new AvpNotAllowedException(anae.getMessage(),anae.getAvpCode(),anae.getVendorId());
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#sendMultimediaAuthenticationRequest(net.java.slee.resource.diameter.cxdx.events.MultimediaAuthenticationRequest)
   */
  public void sendMultimediaAuthenticationRequest(MultimediaAuthenticationRequest multimediaAuthenticationRequest) throws IOException {
    try{
      DiameterMessageImpl msg = (DiameterMessageImpl) multimediaAuthenticationRequest;

      appSession.sendMultimediaAuthRequest(new JMultimediaAuthRequestImpl(msg.getGenericData()));
    }
    catch(JAvpNotAllowedException anae) {
      throw new AvpNotAllowedException(anae.getMessage(),anae.getAvpCode(),anae.getVendorId());
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#sendPushProfileAnswer(net.java.slee.resource.diameter.cxdx.events.PushProfileAnswer)
   */
  public void sendPushProfileAnswer(PushProfileAnswer pushProfileAnswer) throws IOException {
    try{
      DiameterMessageImpl msg = (DiameterMessageImpl) pushProfileAnswer;

      appSession.sendPushProfileAnswer(new JPushProfileAnswerImpl(msg.getGenericData()));
    }
    catch(JAvpNotAllowedException anae) {
      throw new AvpNotAllowedException(anae.getMessage(),anae.getAvpCode(),anae.getVendorId());
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#sendRegistrationTerminationRequest(net.java.slee.resource.diameter.cxdx.events.RegistrationTerminationRequest)
   */
  public void sendRegistrationTerminationAnswer(RegistrationTerminationAnswer registrationTerminationAnswer) throws IOException {
    try{
      DiameterMessageImpl msg = (DiameterMessageImpl) registrationTerminationAnswer;

      appSession.sendRegistrationTerminationAnswer(new JRegistrationTerminationAnswerImpl(msg.getGenericData()));
    }
    catch(JAvpNotAllowedException anae) {
      throw new AvpNotAllowedException(anae.getMessage(),anae.getAvpCode(),anae.getVendorId());
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#sendServerAssignmentRequest(net.java.slee.resource.diameter.cxdx.events.ServerAssignmentRequest)
   */
  public void sendServerAssignmentRequest(ServerAssignmentRequest serverAssignmentRequest) throws IOException {
    try{
      DiameterMessageImpl msg = (DiameterMessageImpl) serverAssignmentRequest;

      appSession.sendServerAssignmentRequest(new JServerAssignmentRequestImpl(msg.getGenericData()));
    }
    catch(JAvpNotAllowedException anae) {
      throw new AvpNotAllowedException(anae.getMessage(),anae.getAvpCode(),anae.getVendorId());
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
  }

  /* (non-Javadoc)
   * @see net.java.slee.resource.diameter.cxdx.CxDxClientSession#sendUserAuthorizationRequest(net.java.slee.resource.diameter.cxdx.events.UserAuthorizationRequest)
   */
  public void sendUserAuthorizationRequest(UserAuthorizationRequest userAuthorizationRequest) throws IOException {
    try {
      DiameterMessageImpl msg = (DiameterMessageImpl) userAuthorizationRequest;

      appSession.sendUserAuthorizationRequest(new JUserAuthorizationRequestImpl(msg.getGenericData()));
    }
    catch (JAvpNotAllowedException anae) {
      throw new AvpNotAllowedException(anae.getMessage(), anae.getAvpCode(), anae.getVendorId());
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
  }

  public void stateChanged(Enum oldState, Enum newState) {
    if (!terminated) {
      if (newState == CxDxSessionState.TERMINATED || newState == CxDxSessionState.TIMEDOUT) {
        terminated = true;
        super.cxdxSessionListener.sessionDestroyed(sessionId, this.appSession);
      }
    }
  }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdiameter.api.app.StateChangeListener#stateChanged(java.lang.Object,
	 * java.lang.Enum, java.lang.Enum)
	 */
	public void stateChanged(AppSession arg0, Enum oldState, Enum newState) {
		this.stateChanged(oldState, newState);

	}
}
