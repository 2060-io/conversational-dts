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
import io.twentysixty.dts.conversational.res.c.MediaResource;
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
import io.twentysixty.sa.res.c.CredentialTypeResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;



@ApplicationScoped
public class Service {

	private static Logger logger = Logger.getLogger(Service.class);

	@Inject EntityManager em;

	@RestClient
	@Inject MediaResource mediaResource;


	@Inject MtProducer mtProducer;

	
	@Inject Controller controller;
	
	@RestClient
	@Inject CredentialTypeResource credentialTypeResource;

	
	@ConfigProperty(name = "io.twentysixty.dts.conversational.credential_issuer")
	String credentialIssuer;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.credential_issuer.avatar")
	String invitationImageUrl;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.credential_issuer.label")
	String invitationLabel;



	@ConfigProperty(name = "io.twentysixty.dts.conversational.id_credential_def")
	String credDef;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.welcome")
	String WELCOME;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.welcome2")
	Optional<String> WELCOME2;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.welcome3")
	Optional<String> WELCOME3;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.auth_success")
	Optional<String> AUTH_SUCCESS;


	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.nocred")
	String NO_CRED_MSG;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.request.citizenid")
	Boolean requestCitizenId;


	@ConfigProperty(name = "io.twentysixty.dts.conversational.request.firstname")
	Boolean requestFirstname;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.request.lastname")
	Boolean requestLastname;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.request.photo")
	Boolean requestPhoto;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.request.avatarname")
	Boolean requestAvatarname;


	@ConfigProperty(name = "io.twentysixty.dts.conversational.language")
	Optional<String> language;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.vision.face.verification.url")
	String faceVerificationUrl;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.vision.redirdomain")
	Optional<String> redirDomain;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.vision.redirdomain.q")
	Optional<String> qRedirDomain;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.vision.redirdomain.d")
	Optional<String> dRedirDomain;

	private static String CMD_ROOT_MENU_AUTHENTICATE = "/auth";
	private static String CMD_ROOT_MENU_NO_CRED = "/nocred";
	private static String CMD_ROOT_MENU_OPTION1 = "/option1";
	private static String CMD_ROOT_MENU_LOGOUT = "/logout";

	private static String CMD_ROOT_MENU_BCAST_OPTIN = "/optin";

	private static String CMD_ROOT_MENU_BCAST_OPTOUT = "/optout";




	@ConfigProperty(name = "io.twentysixty.dts.conversational.bcast.interval.days")
	Integer bcastIntervalDays;



	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.root.menu.title")
	String ROOT_MENU_TITLE;


	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.root.menu.option1")
	String ROOT_MENU_OPTION1;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.root.menu.no_cred")
	Optional<String> ROOT_MENU_NO_CRED;



	@ConfigProperty(name = "io.twentysixty.dts.conversational.messages.option1")
	String OPTION1_MSG;

	


	@Inject StatProducer statProducer;


	//private static HashMap<UUID, SessionData> sessions = new HashMap<UUID, SessionData>();
	private static CredentialType type = null;
	private static Object lockObj = new Object();



	@Transactional
	public void newConnection(UUID connectionId) throws Exception {
		UUID threadId = UUID.randomUUID();

		// create session
		Session session = this.getSession(connectionId);

		mtProducer.sendMessage(TextMessage.build(connectionId,threadId , WELCOME));
		if (WELCOME2.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME2.get()));
		}
		if (WELCOME3.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME3.get()));
		}


		mtProducer.sendMessage(this.getRootMenu(connectionId, session));

		//mtProducer.sendMessage(this.getIdentityCredentialRequest(connectionId, null));

		ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
		lenum.add(DtsStat.ESTABLISHED_CONNECTION);
		statProducer.spool(OrchestratorStatClass.DTS.toString(), Controller.getDtsConfig().getId(), lenum, Instant.now(), 1);
		

	}



	private BaseMessage getIdentityCredentialRequest(UUID connectionId, UUID threadId) {
		IdentityProofRequestMessage ip = new IdentityProofRequestMessage();
		ip.setConnectionId(connectionId);
		ip.setThreadId(threadId);

		RequestedProofItem id = new RequestedProofItem();
		id.setCredentialDefinitionId(credDef);
		id.setType("verifiable-credential");
		List<String> attributes = new ArrayList<>();
		if (requestCitizenId) {
			attributes.add("citizenId");
		}
		if (requestFirstname) {
			attributes.add("firstname");
		}
		if (requestLastname) {
			attributes.add("lastname");
		}
		if (requestPhoto) {
			attributes.add("photo");
		}
		if (requestAvatarname) {
			attributes.add("avatarName");
		}
		attributes.add("issued");
		id.setAttributes(attributes);

		List<RequestedProofItem> rpi = new ArrayList<>();
		rpi.add(id);

		ip.setRequestedProofItems(rpi);


		try {
			logger.info("getCredentialRequest: claim: " + JsonUtil.serialize(ip, false));
		} catch (Exception e) {

		}
		return ip;
	}


	private Session optOutConnectionForBroadcast(Session session) {
		session.setNextBcTs(null);
		session = em.merge(session);
		return session;
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



	Pair<String, byte[]> getImage(String image) {
		String mimeType = null;
		byte[] imageBytes = null;

		String[] separated =  image.split(";");
		if (separated.length>1) {
			String[] mimeTypeData = separated[0].split(":");
			String[] imageData = separated[1].split(",");

			if (mimeTypeData.length>1) {
				mimeType = mimeTypeData[1];
			}
			if (imageData.length>1) {
				String base64Image = imageData[1];
				if (base64Image != null) {
					try {
						imageBytes = Base64.decode(base64Image);
					} catch (IOException e) {
						logger.error("", e);
					}
				}
			}

		}

		if ((mimeType == null) || (imageBytes == null)) {
			return null;
		}

		return Pair.create(mimeType, imageBytes);

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

	private String buildVisionUrl(String url) {

		if(redirDomain.isPresent()) {
			url = url + "&rd=" +  redirDomain.get();
		}
		if(qRedirDomain.isPresent()) {
			url = url + "&q=" +  qRedirDomain.get();
		}
		if(dRedirDomain.isPresent()) {
			url = url + "&d=" +  dRedirDomain.get();
		}
		if (language.isPresent()) {
			url = url + "&lang=" +  language.get();
		}

		return url;
	}
	private BaseMessage generateFaceVerificationMediaMessage(UUID connectionId, UUID threadId, String token) {
		String url = faceVerificationUrl.replaceFirst("TOKEN", token);
		url = this.buildVisionUrl(url);

		MediaItem mi = new MediaItem();
		mi.setMimeType("text/html");
		mi.setUri(url);
		mi.setTitle(getMessage("FACE_VERIFICATION_HEADER"));
		mi.setDescription(getMessage("FACE_VERIFICATION_DESC"));
		mi.setOpeningMode("normal");
		List<MediaItem> mis = new ArrayList<>();
		mis.add(mi);
		MediaMessage mm = new MediaMessage();
		mm.setConnectionId(connectionId);
		mm.setThreadId(threadId);
		mm.setDescription(getMessage("FACE_VERIFICATION_DESC"));
		mm.setItems(mis);
		return mm;
	}


	@Transactional
	public void userInput(BaseMessage message) throws Exception {

		Session session = this.getSession(message.getConnectionId());


		String content = null;

		MediaMessage mm = null;

		if (message instanceof TextMessage) {

			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();

		} else if ((message instanceof ContextualMenuSelect) ) {

			ContextualMenuSelect menuSelect = (ContextualMenuSelect) message;
			content = menuSelect.getSelectionId();

		} else if ((message instanceof MenuSelectMessage)) {

			MenuSelectMessage menuSelect = (MenuSelectMessage) message;
			content = menuSelect.getMenuItems().iterator().next().getId();
		} else if ((message instanceof MediaMessage)) {
			mm = (MediaMessage) message;
			content = "media";
		} /*else if ((message instanceof IdentityProofSubmitMessage)) {
			if (session.getAuthTs() == null) {
				try {
					logger.info("userInput: claim: " + JsonUtil.serialize(message, false));
				} catch (JsonProcessingException e) {

				}
				boolean sentVerifLink = false;
				IdentityProofSubmitMessage ipm = (IdentityProofSubmitMessage) message;

				if (ipm.getSubmittedProofItems().size()>0) {

					SubmitProofItem sp = ipm.getSubmittedProofItems().iterator().next();

					if ((sp.getVerified() != null) && (sp.getVerified())) {
						if (sp.getClaims().size()>0) {

							String citizenId = null;
							String firstname = null;
							String lastname = null;
							String photo = null;
							String avatarName = null;

							for (Claim c: sp.getClaims()) {
								if (c.getName().equals("citizenId")) {
									citizenId = c.getValue();
								} else if (c.getName().equals("firstname")) {
									firstname = c.getValue();
								} else if (c.getName().equals("lastname")) {
									lastname = c.getValue();
								} else if (c.getName().equals("photo")) {
									photo = c.getValue();
									logger.info("userInput: photo: " + photo);
								} else if (c.getName().equals("avatarName")) {
									avatarName = c.getValue();
								}
							}
							session.setCitizenId(citizenId);
							session.setFirstname(firstname);
							session.setLastname(lastname);
							session.setAvatarName(avatarName);

							if (photo != null) {
								Pair<String, byte[]> imageData = getImage(photo);
								if (imageData != null) {
									UUID mediaUUID = UUID.randomUUID();
									mediaResource.createOrUpdate(mediaUUID, 1, mediaUUID.toString());


									File file = new File(System.getProperty("java.io.tmpdir") + "/" + mediaUUID);

									FileOutputStream fos = new FileOutputStream(file);
									fos.write(imageData.getRight());
									fos.flush();
									fos.close();

									Resource r = new Resource();
									r.chunk = new FileInputStream(file);
									mediaResource.uploadChunk(mediaUUID, 0, mediaUUID.toString(), r);

									file.delete();
									session.setPhoto(mediaUUID);
									session.setToken(UUID.randomUUID());
									em.merge(session);

									mtProducer.sendMessage(generateFaceVerificationMediaMessage(message.getConnectionId(), message.getThreadId(), session.getToken().toString()));

									sentVerifLink = true;
								}
							}


						}
					}
					} else {
						// user do not have the required credential, send invitation link

						mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , NO_CRED_MSG));
						mtProducer.sendMessage(this.getInvitationMessage(message.getConnectionId(), message.getThreadId()));

					}


				if (!sentVerifLink) {

					notifySuccess(session.getConnectionId());

					//mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("CREDENTIAL_ERROR")));

				}
			}

		}*/
		if (content != null) {
			if (content.equals(CMD_ROOT_MENU_AUTHENTICATE.toString())) {
				mtProducer.sendMessage(this.getIdentityCredentialRequest(message.getConnectionId(), message.getThreadId()));
			} else if (content.equals(CMD_ROOT_MENU_OPTION1.toString())) {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , OPTION1_MSG));

			} else if (content.equals(CMD_ROOT_MENU_NO_CRED.toString())) {

				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , NO_CRED_MSG));
				mtProducer.sendMessage(this.getInvitationMessage(message.getConnectionId(), message.getThreadId()));

			} else if (content.equals(CMD_ROOT_MENU_LOGOUT.toString())) {
				if (session != null) {
					session.setAuthTs(null);
					session = em.merge(session);
				}
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("UNAUTHENTICATED")));

			} else if (content.equals(CMD_ROOT_MENU_BCAST_OPTIN.toString())) {
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



	private BaseMessage getInvitationMessage(UUID connectionId, UUID threadId) {
		InvitationMessage invitation = new InvitationMessage();
		invitation.setConnectionId(connectionId);
		invitation.setThreadId(threadId);
		invitation.setImageUrl(invitationImageUrl);
		invitation.setDid(credentialIssuer);
		invitation.setLabel(invitationLabel);
		return invitation;
	}




	public BaseMessage getRootMenu(UUID connectionId, Session session) {

		ContextualMenuUpdate menu = new ContextualMenuUpdate();
		menu.setTitle(ROOT_MENU_TITLE);
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


	private static  String broadcastSelectQuery =
			"UPDATE session AS s1 SET nextBcTs=:nextBcTs WHERE s1.connectionId IN "
					+ "( SELECT connectionId FROM session AS s2 where s2.nextBcTs<:now AND s2.nextBcTs>:minNextBcTs "
					+ " LIMIT QTY FOR UPDATE SKIP LOCKED ) RETURNING s1.connectionId, s1.avatarName ";

	@Transactional
	public List<List<Object>> lockAndGetConnections(Integer qty) {



		String query = broadcastSelectQuery.replaceFirst("QTY",  qty.toString());


		Query q = em.createNativeQuery(query);
		Instant now = Instant.now();



		Instant lessDiscoIntervalTs = null;

		// set +1h just in case sending generates exception, will be adjusted later
		q.setParameter("nextBcTs", Instant.now().plusSeconds(3600l));
		q.setParameter("now", now);
		q.setParameter("minNextBcTs", Instant.now().minusSeconds(86400l * 365));

		List<List<Object>>res = q.getResultList();
		if (controller.isDebugEnabled()) {
			if (res.size() >0) {
				logger.info("getForDiscovery: " + query + " " + res);
			} else {
				logger.info("getForDiscovery: " + query + " no result" );
			}
		}


		return res;
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
