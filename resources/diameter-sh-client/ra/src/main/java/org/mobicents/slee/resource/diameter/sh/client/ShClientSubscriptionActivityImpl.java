/*
 * Mobicents, Communications Middleware
 * 
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party
 * contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 *
 * Boston, MA  02110-1301  USA
 */
package org.mobicents.slee.resource.diameter.sh.client;

import java.io.IOException;
import java.util.ArrayList;

import javax.slee.resource.SleeEndpoint;

import net.java.slee.resource.diameter.base.events.DiameterMessage;
import net.java.slee.resource.diameter.base.events.avp.AuthSessionStateType;
import net.java.slee.resource.diameter.base.events.avp.AvpNotAllowedException;
import net.java.slee.resource.diameter.base.events.avp.AvpUtilities;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;
import net.java.slee.resource.diameter.sh.DiameterShAvpFactory;
import net.java.slee.resource.diameter.sh.ShSessionState;
import net.java.slee.resource.diameter.sh.client.ShClientMessageFactory;
import net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity;
import net.java.slee.resource.diameter.sh.events.PushNotificationAnswer;
import net.java.slee.resource.diameter.sh.events.PushNotificationRequest;
import net.java.slee.resource.diameter.sh.events.SubscribeNotificationsRequest;
import net.java.slee.resource.diameter.sh.events.avp.DataReferenceType;
import net.java.slee.resource.diameter.sh.events.avp.DiameterShAvpCodes;
import net.java.slee.resource.diameter.sh.events.avp.SubsReqType;
import net.java.slee.resource.diameter.sh.events.avp.UserIdentityAvp;

import org.jdiameter.api.Answer;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.sh.ClientShSession;
import org.jdiameter.common.impl.app.sh.PushNotificationAnswerImpl;
import org.jdiameter.common.impl.app.sh.SubscribeNotificationsRequestImpl;
import org.jdiameter.common.impl.validation.JAvpNotAllowedException;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.diameter.dictionary.AvpRepresentation;
import org.mobicents.slee.resource.diameter.base.DiameterActivityImpl;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;
import org.mobicents.slee.resource.diameter.sh.events.DiameterShMessageImpl;
import org.mobicents.slee.resource.diameter.sh.events.avp.UserIdentityAvpImpl;
import org.mobicents.slee.resource.diameter.sh.client.handlers.ShClientSessionListener;

/**
 * 
 * Sh Client activity created for subscription cases
 *
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @see ShClientSubscriptionActivity
 */
public class ShClientSubscriptionActivityImpl extends DiameterActivityImpl implements ShClientSubscriptionActivity, StateChangeListener<AppSession> {

  protected ClientShSession clientSession = null;
  protected ShSessionState state = ShSessionState.NOTSUBSCRIBED;
  protected ShClientSessionListener listener = null;
  protected DiameterShAvpFactory shAvpFactory = null;
  protected ShClientMessageFactory messageFactory = null;

  //FIXME: add more
  protected UserIdentityAvp userIdentity;
  protected DataReferenceType[] dataReferenceType;
  protected AuthSessionStateType authSessionState;
  protected DiameterIdentity remoteRealm;

  // Last received message(s)
  protected ArrayList<DiameterMessageImpl> stateMessages = new ArrayList<DiameterMessageImpl>();

  public ShClientSubscriptionActivityImpl(ShClientMessageFactory shClientMessageFactory, DiameterShAvpFactory shAvpFactory, ClientShSession session, DiameterIdentity destinationHost, DiameterIdentity destinationRealm, SleeEndpoint endpoint)
  {
    super(shClientMessageFactory.getBaseMessageFactory(), shAvpFactory.getBaseFactory(), null, (EventListener<Request, Answer>) session, destinationHost, destinationRealm, endpoint);

    this.clientSession = session;
    this.clientSession.addStateChangeNotification(this);
    super.setCurrentWorkingSession(this.clientSession.getSessions().get(0));
    this.shAvpFactory = shAvpFactory;
    this.messageFactory = shClientMessageFactory;
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity#getClientMessageFactory()
   */
  public ShClientMessageFactory getClientMessageFactory() {
    return this.messageFactory;
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#getSubscribedUserIdentity()
   */
  public UserIdentityAvp getSubscribedUserIdentity() {
    return this.userIdentity;
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#createPushNotificationAnswer()
   */
  public PushNotificationAnswer createPushNotificationAnswer() {
    PushNotificationAnswer pna = null;

    for (int index = 0; index < stateMessages.size(); index++) {
      if (stateMessages.get(index).getCommand().getCode() == PushNotificationAnswer.commandCode) {
        PushNotificationRequest msg = (PushNotificationRequest) stateMessages.get(index);

        pna = this.messageFactory.createPushNotificationAnswer(msg);
        if (pna.getAuthSessionState() == null && this.authSessionState != null) {
          pna.setAuthSessionState(this.authSessionState);
        }

        ((DiameterShMessageImpl) pna).setData(msg);
        break;
      }
    }

    return pna;
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#createPushNotificationAnswer(long, boolean)
   */
  public PushNotificationAnswer createPushNotificationAnswer(long resultCode, boolean isExperimental) {
    PushNotificationAnswer pna = createPushNotificationAnswer();

    if(isExperimental) {
      pna.setExperimentalResult(shAvpFactory.getBaseFactory().createExperimentalResult(ShClientMessageFactory._SH_APP_ID, resultCode));
    }
    else {
      pna.setResultCode(resultCode);
    }

    return pna;
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#sendPushNotificationAnswer(net.java.slee.resource.diameter.sh.server.events.PushNotificationAnswer)
   */
  public void sendPushNotificationAnswer(PushNotificationAnswer answer) throws IOException {
    try {
      DiameterMessageImpl msg = (DiameterMessageImpl) answer;

      this.clientSession.sendPushNotificationAnswer(new PushNotificationAnswerImpl((Answer) msg.getGenericData()));

      clean((DiameterShMessageImpl)answer);
      fetchSessionData(answer, false);
      
    }
    catch (JAvpNotAllowedException e) {
      throw new AvpNotAllowedException("Message validation failed.", e, e.getAvpCode(), e.getVendorId());
    }
    catch (Exception e) {
      //FIXME: extent this exception to add trace....
      throw new IOException("Failed to send message, due to: " + e.getLocalizedMessage());
    }
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#sendPushNotificationAnswer(long, boolean)
   */
  public void sendPushNotificationAnswer(long resultCode, boolean isExperimentalResultCode) throws IOException {
    PushNotificationAnswer answer= this.createPushNotificationAnswer(resultCode, isExperimentalResultCode);

    if(answer != null) {
      this.sendPushNotificationAnswer(answer);
    }
    else {
      throw new IOException("Could not create PNA, there is no PNR?");
    }
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#sendSubscribeNotificationsRequest(net.java.slee.resource.diameter.sh.server.events.SubscribeNotificationsRequest)
   */
  public void sendSubscribeNotificationsRequest(SubscribeNotificationsRequest request) throws IOException {
    try {
      DiameterMessageImpl msg = (DiameterMessageImpl) request;

      this.clientSession.sendSubscribeNotificationsRequest(new SubscribeNotificationsRequestImpl((Request) msg.getGenericData()));
      fetchSessionData(msg, false);
    }
    catch (JAvpNotAllowedException e) {
      throw new AvpNotAllowedException("Message validation failed.", e, e.getAvpCode(), e.getVendorId());
    }
    catch (Exception e) {
      throw new IOException("Failed to send message, due to: " + e.getLocalizedMessage());
    }
  }

  /*
   * (non-Javadoc)
   * @see net.java.slee.resource.diameter.sh.ShClientSubscriptionActivity#sendUnsubscribeRequest()
   */
  public void sendUnsubscribeRequest() throws IOException {
    try {
      SubscribeNotificationsRequest snr = this.messageFactory.createSubscribeNotificationsRequest(getSubscribedUserIdentity(), this.dataReferenceType[0], SubsReqType.UNSUBSCRIBE);

      snr.setDataReferences(this.dataReferenceType);
      snr.setDestinationRealm(remoteRealm);
      snr.setAuthSessionState(authSessionState);
      DiameterMessageImpl msg = (DiameterMessageImpl) snr;

      this.clientSession.sendSubscribeNotificationsRequest(new SubscribeNotificationsRequestImpl((Request) msg.getGenericData()));
    }
    catch (JAvpNotAllowedException e) {
      throw new AvpNotAllowedException("Message validation failed.", e, e.getAvpCode(), e.getVendorId());
    }
    catch (Exception e) {
      throw new IOException("Failed to send message, due to: " + e.getLocalizedMessage());
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
  
  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.app.StateChangeListener#stateChanged(java.lang.Enum, java.lang.Enum)
   */
  public void stateChanged(Enum oldState, Enum newState) {
    org.jdiameter.common.api.app.sh.ShSessionState shNewState = (org.jdiameter.common.api.app.sh.ShSessionState) newState;

    switch (shNewState) {
      case NOTSUBSCRIBED:
        break;
      case SUBSCRIBED:
        state = ShSessionState.SUBSCRIBED;
        // FIXME: error?
        break;
      case TERMINATED:
        state = ShSessionState.TERMINATED;
        listener.sessionDestroyed(getSessionId(), clientSession);
        this.clientSession.removeStateChangeNotification(this);
        break;
    }
  }

  /*
   * (non-Javadoc)
   * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#getSessionListener()
   */
  public Object getSessionListener() {
    return this.listener;
  }

  /*
   * (non-Javadoc)
   * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#setSessionListener(java.lang.Object)
   */
  public void setSessionListener(Object ra) {
    this.listener = (ShClientSessionListener) ra;
  }

  /*
   * (non-Javadoc)
   * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#endActivity()
   */
  public void endActivity() {
    this.clientSession.release();
  }

  //  FIXME: Find a good way to deal with this.
  //  /*
  //   * (non-Javadoc)
  //   * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#getDiameterAvpFactory()
  //   */
  //  public Object getDiameterAvpFactory()
  //  {
  //    return this.shAvpFactory;
  //  }
  //
  //  /*
  //   * (non-Javadoc)
  //   * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#getDiameterMessageFactory()
  //   */
  //  public Object getDiameterMessageFactory()
  //  {
  //    return this.messageFactory;
  //  }

  /**
   * 
   * @return
   */
  ClientShSession getClientSession() {
    return this.clientSession;
  }

  /**
   * 
   * @param request
   */
  public void fetchSessionData(DiameterMessage msg, boolean incoming) {
    if(msg.getHeader().isRequest())
    {
      //Well it should always be getting this on request and only once ?
      if(incoming) {
        if(this.userIdentity == null) {
          //FIXME: make this diff.
          AvpRepresentation rep = AvpDictionary.INSTANCE.getAvp(DiameterShAvpCodes.USER_IDENTITY, DiameterShAvpCodes.SH_VENDOR_ID);
          this.userIdentity = new UserIdentityAvpImpl(DiameterShAvpCodes.USER_IDENTITY, DiameterShAvpCodes.SH_VENDOR_ID,rep.getRuleMandatoryAsInt(),rep.getRuleProtectedAsInt(),AvpUtilities.getAvpAsGrouped(DiameterShAvpCodes.USER_IDENTITY, DiameterShAvpCodes.SH_VENDOR_ID, ((DiameterMessageImpl)msg).getGenericData().getAvps()));
        }

        if(this.authSessionState == null) {
          this.authSessionState = AuthSessionStateType.fromInt(AvpUtilities.getAvpAsInteger32(277, ((DiameterMessageImpl)msg).getGenericData().getAvps()));
        }

        stateMessages.add((DiameterMessageImpl) msg);
      }
      else {
        // FIXME: this should be up there... in incoming?
        if(destinationRealm == null) {
          this.remoteRealm = msg.getDestinationRealm();
        }

        if(msg instanceof SubscribeNotificationsRequest) {
          SubscribeNotificationsRequest snr = (SubscribeNotificationsRequest) msg;
          if(dataReferenceType == null || dataReferenceType.length == 0) {
            dataReferenceType = snr.getDataReferences();
          }
          if(authSessionState == null && snr.hasAuthSessionState()) {
            authSessionState = snr.getAuthSessionState();
          }
          if(userIdentity == null && snr.hasUserIdentity()) {
            userIdentity  = snr.getUserIdentity();
          }
        }
      }
    }
  }

  /**
   * 
   * @param msg
   */
  private void clean(DiameterShMessageImpl msg) {
    if(msg.getData() != null) {
      this.stateMessages.remove(msg.removeData());
    }
  }

}
