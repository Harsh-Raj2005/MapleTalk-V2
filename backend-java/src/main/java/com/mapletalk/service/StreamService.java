package com.mapletalk.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Generates Stream Chat/Video user tokens and registers users with Stream.
 * There is no official Stream server SDK for Java; both operations are
 * documented, minimal HTTP/JWT mechanisms — exactly the same approach a
 * server SDK would use internally — so no additional library is needed.
 * The API secret never leaves this class; only a signed token or an
 * outbound HTTP call ever uses it.
 */
@Service
public class StreamService {

	private static final Logger log = LoggerFactory.getLogger(StreamService.class);
	private static final String STREAM_CHAT_BASE_URL = "https://chat.stream-io-api.com";

	private final SecretKey signingKey;
	private final String apiKey;
	private final RestClient restClient;

	public StreamService(
			@Value("${stream.api.secret}") String apiSecret,
			@Value("${stream.api.key}") String apiKey) {
		this.signingKey = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
		this.apiKey = apiKey;

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		requestFactory.setReadTimeout(3000);
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	/**
	 * @param streamUserId the stable Stream user identity — see
	 *                      {@code ChatController}, which derives this from
	 *                      the authenticated V2 user's numeric id, never
	 *                      from client input.
	 */
	public String generateToken(String streamUserId) {
		// Stream's servers only accept HS256 — jjwt's algorithm auto-detection
		// (signWith(Key) with no explicit algorithm) upgrades to HS384/HS512
		// for a long-enough key, which Stream then rejects. HS256 must be
		// forced explicitly regardless of the configured secret's length.
		return Jwts.builder()
				.claim("user_id", streamUserId)
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();
	}

	/**
	 * Registers (or updates) a user's Stream profile. Stream requires every
	 * channel member to already exist server-side — a user who has never
	 * been upserted cannot be added to a channel even if the requester has
	 * connected. Called once at signup so any future friend can always open
	 * a chat with them, regardless of whether that user has opened the chat
	 * page yet themselves.
	 *
	 * Best-effort: a failure here must never break signup. Stream being
	 * briefly unreachable is not a reason to refuse creating the account.
	 */
	public void upsertUser(String streamUserId, String name, String image) {
		try {
			// Stream's REST API only accepts this as a trusted server-level
			// request when the token explicitly claims server:true — an
			// otherwise-valid signed JWT without it is rejected as 401.
			String serverToken = Jwts.builder()
					.claim("server", true)
					.signWith(signingKey, Jwts.SIG.HS256)
					.compact();

			restClient.post()
					.uri(STREAM_CHAT_BASE_URL + "/users?api_key={key}", apiKey)
					.header("Authorization", serverToken)
					.header("stream-auth-type", "jwt")
					.body(Map.of("users", Map.of(streamUserId, Map.of(
							"id", streamUserId,
							"name", name,
							"image", image == null ? "" : image))))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			// The response body from Stream never contains our secret — only
			// their own error description — so it's safe to log in full.
			log.warn("Could not upsert Stream user [streamUserId={}]: {} {}",
					streamUserId, ex.getStatusCode(), ex.getResponseBodyAsString());
		} catch (Exception ex) {
			log.warn("Could not upsert Stream user [streamUserId={}]: {}", streamUserId, ex.getMessage());
		}
	}

}
