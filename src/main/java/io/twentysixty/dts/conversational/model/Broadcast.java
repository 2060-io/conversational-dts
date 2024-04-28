package io.twentysixty.dts.conversational.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
	@NamedQuery(name="Broadcast.findForConnectionCampaign", query="SELECT s FROM Broadcast s WHERE s.connectionId=:connectionId and s.campaignId=:campaignId"),
	
})
public class Broadcast implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private UUID id;

	@Column(columnDefinition="timestamptz")
	private Instant ts;
	private UUID connectionId;
	private UUID campaignId;
	
		
	@Column(columnDefinition="timestamptz")
	private Instant lastSuccessTs;
	
	@Column(columnDefinition="timestamptz")
	private Instant lastSentTs;
	
	private UUID campaignScheduleId;
	
	private Integer submittedCount;
	private Integer receivedCount;
	private Integer viewedCount;
	
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

	public UUID getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
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

	public Integer getSubmittedCount() {
		return submittedCount;
	}

	public void setSubmittedCount(Integer submittedCount) {
		this.submittedCount = submittedCount;
	}

	public Integer getReceivedCount() {
		return receivedCount;
	}

	public void setReceivedCount(Integer receivedCount) {
		this.receivedCount = receivedCount;
	}

	public Integer getViewedCount() {
		return viewedCount;
	}

	public void setViewedCount(Integer viewedCount) {
		this.viewedCount = viewedCount;
	}

	
	
	


}