package io.twentysixty.dts.conversational.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;



/**
 * The persistent class for the session database table.
 *
 */
@Entity
@Table(name="session")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	//@NamedQuery(name="Session.findWithToken", query="SELECT s FROM Session s where s.token=:token"),

})
public class Session implements Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	private UUID connectionId;


	@Column(columnDefinition="text")
	private String avatarName;

	private UUID avatarPic;

	@Column(columnDefinition="timestamptz")
	private Instant createdTs;



	@Column(columnDefinition="timestamptz")
	private Instant authTs;


	@Column(columnDefinition="timestamptz")
	private Instant lastBcTs;

	@Column(columnDefinition="timestamptz")
	private Instant nextBcTs;


	private Integer sentBcasts;



	public UUID getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}





	public Instant getAuthTs() {
		return authTs;
	}

	public void setAuthTs(Instant authTs) {
		this.authTs = authTs;
	}

	public String getAvatarName() {
		return avatarName;
	}

	public void setAvatarName(String avatarName) {
		this.avatarName = avatarName;
	}

	public UUID getAvatarPic() {
		return avatarPic;
	}

	public void setAvatarPic(UUID avatarPic) {
		this.avatarPic = avatarPic;
	}

	public Instant getLastBcTs() {
		return lastBcTs;
	}

	public void setLastBcTs(Instant lastBcTs) {
		this.lastBcTs = lastBcTs;
	}

	public Instant getNextBcTs() {
		return nextBcTs;
	}

	public void setNextBcTs(Instant nextBcTs) {
		this.nextBcTs = nextBcTs;
	}

	public Instant getCreatedTs() {
		return createdTs;
	}

	public void setCreatedTs(Instant createdTs) {
		this.createdTs = createdTs;
	}

	public Integer getSentBcasts() {
		return sentBcasts;
	}

	public void setSentBcasts(Integer sentBcasts) {
		this.sentBcasts = sentBcasts;
	}





}