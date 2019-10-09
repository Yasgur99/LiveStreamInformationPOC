package com.mergg.webapp.persistence.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.xml.bind.annotation.XmlRootElement;

import com.mergg.common.interfaces.IDto;
import com.mergg.common.persistence.model.IEntity;
import com.mergg.webapp.dto.response.GameResponseDto;
import com.mergg.webapp.dto.response.LiveStreamInformationResponseDto;

@SuppressWarnings("serial")
@Entity
@XmlRootElement
public class LiveStreamInformation implements IEntity, IDto {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "liveStreamInformationId")
	private Long id;

	@Column(unique = false, nullable = true)
	private Date lastUpdated;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "liveStreamInformationId")
	private TwitchAccount twitchAccount;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "liveStreamInformationId", referencedColumnName = "liveStreamInformationId")
	private MixerAccount mixerAccount;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "liveStreamInformationId", referencedColumnName = "liveStreamInformationId")
	private YoutubeAccount youtubeAccount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public TwitchAccount getTwitchAccount() {
		return twitchAccount;
	}

	public void setTwitchAccount(TwitchAccount twitchAccount) {
		this.twitchAccount = twitchAccount;
	}

	public MixerAccount getMixerAccount() {
		return mixerAccount;
	}

	public void setMixerAccount(MixerAccount mixerAccount) {
		this.mixerAccount = mixerAccount;
	}

	public YoutubeAccount getYoutubeAccount() {
		return youtubeAccount;
	}

	public void setYoutubeAccount(YoutubeAccount youtubeAccount) {
		this.youtubeAccount = youtubeAccount;
	}
}
