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
public class Thread implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private UUID id;

	@Column(columnDefinition="timestamptz")
	private Instant ts;
	
	private UUID connectionId;
	private UUID broadcastId;
	
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
	public UUID getBroadcastId() {
		return broadcastId;
	}
	public void setBroadcastId(UUID broadcastId) {
		this.broadcastId = broadcastId;
	}
	
	
	


}