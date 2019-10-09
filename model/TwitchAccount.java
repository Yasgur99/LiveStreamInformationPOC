package com.mergg.webapp.persistence.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.xml.bind.annotation.XmlRootElement;

import com.mergg.common.interfaces.IDto;
import com.mergg.common.persistence.model.IEntity;
import com.mergg.webapp.dto.response.LiveStreamInformationResponseDto;
import com.mergg.webapp.dto.response.TwitchAccountResponseDto;

@SuppressWarnings("serial")
@Entity
@XmlRootElement
public class TwitchAccount implements IEntity, IDto {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "twitchAccountId")
	private Long id;

	@OneToOne(mappedBy = "twitchAccount", cascade = CascadeType.ALL)
	private LiveStreamInformation liveStreamInformation;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public LiveStreamInformation getLiveStreamInformation() {
		return liveStreamInformation;
	}

	public void setLiveStreamInformation(LiveStreamInformation liveStreamInformation) {
		this.liveStreamInformation = liveStreamInformation;
	}
}
