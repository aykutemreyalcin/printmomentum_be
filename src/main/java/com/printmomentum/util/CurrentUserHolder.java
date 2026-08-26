package com.printmomentum.util;

import com.printmomentum.domain.User;
import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserHolder {

	private static final ThreadLocal<Principal> currentPrincipal = new ThreadLocal<>();

	public Principal getCurrentPrincipal() {
		return currentPrincipal.get();
	}

	public User getCurrentUser() {
		Object principal = getCurrentPrincipal();
		User fromRequest = unwrap(principal);
		if (fromRequest != null) {
			return fromRequest;
		}
		return unwrap(SecurityContextHolder.getContext().getAuthentication());
	}

	private static User unwrap(Object principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken token) {
			Object innerPrincipal = token.getPrincipal();
			if (innerPrincipal instanceof User user) {
				return user;
			}
		}
		if (principal instanceof User user) {
			return user;
		}
		return null;
	}

	public void setCurrentPrincipal(Principal principal) {
		currentPrincipal.set(principal);
	}

	public void clear() {
		currentPrincipal.remove();
	}
}
