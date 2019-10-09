package com.mergg.webapp.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mergg.common.persistence.service.AbstractService;
import com.mergg.common.util.DateUtil;
import com.mergg.common.web.RestPreconditions;
import com.mergg.common.web.exception.BadRequestException;
import com.mergg.common.web.exception.ResourceNotFoundException;
import com.mergg.webapp.dto.twitch.TwitchGameDto;
import com.mergg.webapp.dto.twitch.TwitchLiveStream;
import com.mergg.webapp.dto.twitch.TwitchUserDto;
import com.mergg.webapp.dto.OAuthTokenDto;
import com.mergg.webapp.dto.response.LiveStreamInformationResponseDto;
import com.mergg.webapp.persistence.dao.ITwitchAccountJpaDao;
import com.mergg.webapp.persistence.model.Following;
import com.mergg.webapp.persistence.model.Game;
import com.mergg.webapp.persistence.model.LiveStreamInformation;
import com.mergg.webapp.persistence.model.TwitchAccount;
import com.mergg.webapp.persistence.model.User;
import com.mergg.webapp.persistence.model.UserProfile;
import com.mergg.webapp.persistence.model.YoutubeAccount;
import com.mergg.webapp.persistence.model.OAuthToken;

import com.mergg.webapp.service.IFollowingService;
import com.mergg.webapp.service.IGameService;
import com.mergg.webapp.service.ILiveStreamInformationService;
import com.mergg.webapp.service.IOAuthTokenService;
import com.mergg.webapp.service.ITwitchService;
import com.mergg.webapp.service.IUserProfileService;
import com.mergg.webapp.service.IUserService;
import com.mergg.webapp.web.client.ITwitchClient;

@Service
@Transactional
public class TwitchServiceImpl extends AbstractService<TwitchAccount> implements ITwitchService {

	@Autowired
	ITwitchAccountJpaDao dao;

	@Autowired
	private IUserService userService;

	@Autowired
	private IUserProfileService userProfileService;

	@Autowired
	private IFollowingService followingService;

	@Autowired
	private ILiveStreamInformationService liveStreamInformationService;

	@Autowired
	private IOAuthTokenService oauthTokenService;

	@Autowired
	private IGameService gameService;

	@Autowired
	private ITwitchClient twitchClient;


	@Override
	public OAuthTokenDto getOauthToken(Long userId) {
		TwitchAccount twitchAccount = findOneByUserId(userId);

		if (twitchAccount == null) {
			logger.info("Twitch account not linked with Mergg userId " + userId);
			throw new BadRequestException();
		}

		OAuthToken oauthToken = twitchAccount.getOauthToken();

		if (oauthToken == null) {
			logger.info("Twitch account does not contain an OAuthToken for userId " + userId);
			throw new BadRequestException("No oauthToken for Twitch account, must unlink to fix");
		}

		Date expireDate = DateUtil.addSeconds(oauthToken.getCreatedOn(), oauthToken.getExpiresIn());
		if (new Date().after(expireDate)) {
			OAuthTokenDto oauthTokenDto = twitchClient.getOAuthTokenDtoFromRefreshToken(oauthToken.getRefreshToken());
			logger.info("Completed fetching new access_token via refresh token, pushing update to db now");

			oauthTokenService.update(oauthToken, oauthTokenDto);
			return oauthTokenDto;
		} else {
			logger.info("Token not expired, returning existing access_token");
			return new OAuthTokenDto(oauthToken);
		}
	}

	@Override
	public List<LiveStreamInformationResponseDto> findLiveFollowing(Long userId) {

		List<Following> following = followingService.findFollowingByUserId(userId);
		List<LiveStreamInformationResponseDto> liveStreams = new ArrayList<>(); // to be populated and returned

		for (Following f : following) {
			TwitchAccount twitchAccount = f.getFollows().getProfile().getTwitchAccount();

			// only deal with people who linked their twitch
			if (twitchAccount == null) {
				continue;
			}

			// check database if there is a non expired LiveStreamInformation object
			// add it to the list if its non expired, otherwise delete from db
			LiveStreamInformation persisted = twitchAccount.getLiveStreamInformation();
			if (persisted != null) {
				Date expires = DateUtil.addSeconds(persisted.getLastUpdated(), 60);
				Date current = new Date();
				if (current.before(expires)) {
					LiveStreamInformationResponseDto dto = persisted.convertToResponseDto();
					dto.setUser(f.getFollows().convertToResponseDto());
					liveStreams.add(dto);
					continue;
				} else {
					liveStreamInformationService.delete(persisted.getId()); // remove expired
				}
			}

			// get info from twitch
			String accessToken = getOauthToken(f.getFollows().getId()).getAccess_token();
			TwitchLiveStream stream = twitchClient.getLiveStream(twitchAccount.getUser_id(), accessToken);

			// make sure twitch returned data
			if (stream == null) {
				continue;
			}

			LiveStreamInformation info = convertTwitchLiveStreamToLiveStreamInformation(stream,
					twitchAccount.getLogin());

			// add/update to db for future checks
			info = liveStreamInformationService.create(info);
			info.setTwitchAccount(twitchAccount);
			twitchAccount.setLiveStreamInformation(info);
			liveStreamInformationService.update(info); // i can comment this line out and it seems to act the same way
			update(twitchAccount);

			LiveStreamInformationResponseDto dto = info.convertToResponseDto();
			dto.setUser(f.getFollows().convertToResponseDto());
			liveStreams.add(dto);
		}
		return liveStreams;
	}

	private LiveStreamInformation convertTwitchLiveStreamToLiveStreamInformation(TwitchLiveStream stream,
			String twitchLogin) {
		LiveStreamInformation info = new LiveStreamInformation();

		info.setStreamingUsername(twitchLogin);
		info.setStreamingUserId(stream.getUser_id());
		info.setTitle(stream.getTitle());
		info.setThumbnailUrl(stream.getThumbnail_url());
		info.setStartedAt(stream.getStarted_at());
		info.setStreamingPlatform("TWITCH");
		info.setViewCount(stream.getViewer_count());
		info.setLanguage(stream.getLanguage());
		Game game = null;// = gameService.findByTwitchGameId(stream.getGame_id());
		if (game != null) {
			info.setGame(game);
		}
		info.setLastUpdated(new Date());

		return info;
	}

	@Override
	public void update(TwitchAccount resource) {
		TwitchAccount current = findOne(resource.getId());
		RestPreconditions.checkNotNull(current);

		current.setHidden(resource.getHidden());
		current.setLiveStreamInformation(resource.getLiveStreamInformation());
		super.update(current);
	}

	@Override
	public void update(Long id, TwitchAccount resource) {
		resource.setId(id);
		update(resource);
	}

	@Override
	public TwitchGameDto getGameByName(String name, String accessToken) {
		return twitchClient.getGameByName(name, accessToken);
	}

	@Override
	protected PagingAndSortingRepository<TwitchAccount, Long> getDao() {
		return dao;
	}

	@Override
	protected JpaSpecificationExecutor<TwitchAccount> getSpecificationExecutor() {
		return dao;
	}
}
