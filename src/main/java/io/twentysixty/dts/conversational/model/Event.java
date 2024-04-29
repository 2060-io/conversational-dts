package io.twentysixty.dts.conversational.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import io.twentysixty.orchestrator.api.enums.CampaignManagement;



/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	
})
public class Event implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private UUID id;

	private UUID threadId;
	
	
	@Column(columnDefinition="timestamptz")
	private Instant ts;
	
	@ManyToOne
	@JoinColumn(name="connectionid")
	private Connection connection;
	
	
	@ManyToOne
	@JoinColumn(name="broadcastId")
	private Broadcast broadcast;
	
	private Boolean submitted;
	private Boolean submittedReceived;
	private Boolean submittedViewed;
	
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public Instant getTs() {
		return ts;
	}
	public void setTs(Instant ts) {
		this.ts = ts;
	}
	
	public UUID getThreadId() {
		return threadId;
	}
	public void setThreadId(UUID threadId) {
		this.threadId = threadId;
	}
	public Broadcast getBroadcast() {
		return broadcast;
	}
	public void setBroadcast(Broadcast broadcast) {
		this.broadcast = broadcast;
	}
	public Connection getConnection() {
		return connection;
	}
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	public Boolean getSubmitted() {
		return submitted;
	}
	public void setSubmitted(Boolean submitted) {
		this.submitted = submitted;
	}
	public Boolean getSubmittedReceived() {
		return submittedReceived;
	}
	public void setSubmittedReceived(Boolean submittedReceived) {
		this.submittedReceived = submittedReceived;
	}
	public Boolean getSubmittedViewed() {
		return submittedViewed;
	}
	public void setSubmittedViewed(Boolean submittedViewed) {
		this.submittedViewed = submittedViewed;
	}
	


}