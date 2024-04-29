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
	@NamedQuery(name="Broadcast.findForConnectionCampaign", query="SELECT s FROM Broadcast s WHERE s.connection=:connection and s.campaignId=:campaignId"),
	
})
public class Broadcast implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private UUID id;

	@Column(columnDefinition="timestamptz")
	private Instant ts;
	
	@ManyToOne
	@JoinColumn(name="connectionid")
	private Connection connection;
	
	private UUID campaignId;
	
		
	@Column(columnDefinition="timestamptz")
	private Instant lastSuccessTs;
	
	@Column(columnDefinition="timestamptz")
	private Instant lastSentTs;
	
	private UUID campaignScheduleId;
	
	private Integer messageCount;
	private Integer submitted;
	private Integer submittedReceived;
	private Integer submittedViewed;
	
	
	private CampaignManagement management;
	
	@Column(columnDefinition="timestamptz")
	private Instant runWhenTs;

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


	public UUID getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(UUID campaignId) {
		this.campaignId = campaignId;
	}

	public Instant getLastSuccessTs() {
		return lastSuccessTs;
	}

	public void setLastSuccessTs(Instant lastSuccessTs) {
		this.lastSuccessTs = lastSuccessTs;
	}

	public Instant getLastSentTs() {
		return lastSentTs;
	}

	public void setLastSentTs(Instant lastSentTs) {
		this.lastSentTs = lastSentTs;
	}

	public UUID getCampaignScheduleId() {
		return campaignScheduleId;
	}

	public void setCampaignScheduleId(UUID campaignScheduleId) {
		this.campaignScheduleId = campaignScheduleId;
	}

	

	public CampaignManagement getManagement() {
		return management;
	}

	public void setManagement(CampaignManagement management) {
		this.management = management;
	}

	public Instant getRunWhenTs() {
		return runWhenTs;
	}

	public void setRunWhenTs(Instant runWhenTs) {
		this.runWhenTs = runWhenTs;
	}

	public Integer getSubmitted() {
		return submitted;
	}

	public void setSubmitted(Integer submitted) {
		this.submitted = submitted;
	}

	public Integer getSubmittedReceived() {
		return submittedReceived;
	}

	public void setSubmittedReceived(Integer submittedReceived) {
		this.submittedReceived = submittedReceived;
	}

	public Integer getSubmittedViewed() {
		return submittedViewed;
	}

	public void setSubmittedViewed(Integer submittedViewed) {
		this.submittedViewed = submittedViewed;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public Integer getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(Integer messageCount) {
		this.messageCount = messageCount;
	}

	
	
	
	


}