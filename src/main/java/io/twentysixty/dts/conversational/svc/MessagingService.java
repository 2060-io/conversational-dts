package io.twentysixty.dts.conversational.svc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;
import org.jgroups.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mobiera.ms.commons.stats.api.StatEnum;


import io.twentysixty.dts.conversational.ex.NonexistentConnectionException;
import io.twentysixty.dts.conversational.jms.MtProducer;
import io.twentysixty.dts.conversational.jms.StatProducer;
import io.twentysixty.dts.conversational.model.Session;
import io.twentysixty.orchestrator.stats.DtsStat;
import io.twentysixty.orchestrator.stats.OrchestratorStatClass;
import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuItem;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.IdentityProofRequestMessage;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaItem;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.RequestedProofItem;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.v1.CredentialTypeResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;



@ApplicationScoped
public class MessagingService {

	private static Logger logger = Logger.getLogger(MessagingService.class);

	@Inject EntityManager em;
	@Inject MtProducer mtProducer;
	@Inject Controller controller;
	

	
	@ConfigProperty(name = "io.twentysixty.dts.conversational.bcast.interval.days")
	Integer bcastIntervalDays;

	
	
	@ConfigProperty(name = "io.twentysixty.dts.conversational.language")
	Optional<String> language;


	private static String CMD_ROOT_MENU_BCAST_OPTIN = "/optin";
	private static String CMD_ROOT_MENU_BCAST_OPTOUT = "/optout";




	@Inject StatProducer statProducer;


	

	@Transactional
	public void newConnection(UUID connectionId) throws Exception {
		UUID threadId = UUID.randomUUID();

		Session session = this.getSession(connectionId);

		mtProducer.sendMessage(TextMessage.build(connectionId,threadId , this.getMessage("WELCOME")));
		mtProducer.sendMessage(this.getRootMenu(connectionId, session));

		ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
		lenum.add(DtsStat.ESTABLISHED_CONNECTION);
		statProducer.spool(OrchestratorStatClass.DTS.toString(), Controller.getDtsConfig().getId(), lenum, Instant.now(), 1);
		

	}


	private Session getSession(UUID connectionId) {
		Session session = em.find(Session.class, connectionId);
		if (session == null) {
			session = new Session();
			session.setConnectionId(connectionId);
			Instant now = Instant.now();
			session.setNextBcTs(now.plusSeconds(60l));
			session.setCreatedTs(now);
			em.persist(session);

		}

		return session;
	}


	ResourceBundle bundle = null;

	private String getMessage(String messageName) {
		String retval = messageName;
		if (bundle == null) {
			if (language.isPresent()) {
				try {
					bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale(language.get()));
				} catch (Exception e) {
					bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale("en"));
				}
			} else {
				bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale("en"));
			}

		}
		try {
			retval = bundle.getString(messageName);
		} catch (Exception e) {

		}


		return retval;
	}


	@Transactional
	public void userInput(BaseMessage message) throws Exception {

		Session session = this.getSession(message.getConnectionId());
		String content = null;

		if (message instanceof TextMessage) {

			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();

		} else if ((message instanceof ContextualMenuSelect) ) {

			ContextualMenuSelect menuSelect = (ContextualMenuSelect) message;
			content = menuSelect.getSelectionId();

		} else if ((message instanceof MenuSelectMessage)) {

			MenuSelectMessage menuSelect = (MenuSelectMessage) message;
			content = menuSelect.getMenuItems().iterator().next().getId();
		} if (content != null) {
			if (content.equals(CMD_ROOT_MENU_BCAST_OPTIN.toString())) {
				session = optin(session);
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("BCAST_OPTED_IN")));

			} else if (content.equals(CMD_ROOT_MENU_BCAST_OPTOUT.toString())) {
				session = optout(session);
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("BCAST_OPTED_OUT")));

			}

			else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("ERROR")));
			}
		}
		mtProducer.sendMessage(this.getRootMenu(message.getConnectionId(), session));
	}


	private Session optout(Session session) {
		session.setNextBcTs(null);
		return em.merge(session);
	}



	private Session optin(Session session) {
		session.setNextBcTs(Instant.now().plusSeconds(60));
		return em.merge(session);
		
	}



	public BaseMessage getRootMenu(UUID connectionId, Session session) {

		ContextualMenuUpdate menu = new ContextualMenuUpdate();
		menu.setTitle(getMessage("ROOT_MENU_DEFAULT_TITLE"));
		menu.setDescription(getMessage("ROOT_MENU_DEFAULT_DESCRIPTION"));

		
		List<ContextualMenuItem> options = new ArrayList<>();


		if (session != null) {

			if (session.getNextBcTs() == null) {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_BCAST_OPTIN, getMessage("ROOT_MENU_BCAST_OPTIN"), null));

			} else {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_BCAST_OPTOUT, getMessage("ROOT_MENU_BCAST_OPTOUT"), null));

			}


		}

		menu.setOptions(options);



		if (controller.isDebugEnabled()) {
			try {
				logger.info("getRootMenu: " + JsonUtil.serialize(menu, false));
			} catch (JsonProcessingException e) {
			}
		}
		menu.setConnectionId(connectionId);
		menu.setId(UUID.randomUUID());
		menu.setTimestamp(Instant.now());

		return menu;


	}





	public void sendBaseMessage(UUID connectionId, BaseMessage message) throws Exception {

		Session session = em.find(Session.class, connectionId);
		if (session == null) {
			throw new NonexistentConnectionException();
		}
		message.setConnectionId(connectionId);
		mtProducer.sendMessage(message);

		updateConnectionBcastTs(connectionId);
	}


	@Transactional
	public void updateConnectionBcastTs(UUID connectionId) {
		Session session = this.getSession(connectionId);
		Instant now = Instant.now();
		session.setLastBcTs(now);
		session.setNextBcTs(now.plusSeconds(86400l * bcastIntervalDays));
		if (session.getSentBcasts() == null) {
			session.setSentBcasts(1);
		} else {
			session.setSentBcasts(session.getSentBcasts() + 1);
		}
		session = em.merge(session);
	}

}
