package com.printmomentum.config;

import com.printmomentum.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration}")
	private long jwtExpiration;

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public String generateToken(User user) {
		HashMap<String, Object> claims = new HashMap<>();
		claims.put("userId", user.getId());
		claims.put("role", user.getRole().getValue());
		claims.put("nonce", UUID.randomUUID().toString());
		return generateToken(claims, user);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return buildToken(extraClaims, userDetails, jwtExpiration);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
		return buildToken(extraClaims, userDetails, expirationMs);
	}

	public String generateV2AccessToken(User user, Long sessionId, long expirationMs) {
		HashMap<String, Object> claims = new HashMap<>();
		claims.put("userId", user.getId());
		claims.put("role", user.getRole().getValue());
		claims.put("nonce", UUID.randomUUID().toString());
		claims.put("authVersion", "v2");
		claims.put("tokenType", "access");
		claims.put("sessionId", sessionId);
		return generateToken(claims, user, expirationMs);
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		return Jwts.builder()
				.setClaims(extraClaims)
				.setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSignInKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSignInKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public boolean isV2AccessToken(String token) {
		Claims claims = extractAllClaims(token);
		String authVersion = claims.get("authVersion", String.class);
		String tokenType = claims.get("tokenType", String.class);
		return "v2".equals(authVersion) && "access".equals(tokenType);
	}

	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
