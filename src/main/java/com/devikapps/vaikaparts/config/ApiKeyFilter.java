package com.devikapps.vaikaparts.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private static final String API_KEY_HEADER = "X-Api-Key";

  @Value("${app.security.api-key}")
  private String apiKey;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    log.info("API key validation processing");

    String requestedApiKey = request.getHeader(API_KEY_HEADER);

    if (apiKey.equals(requestedApiKey)) {
      var auth = new PreAuthenticatedAuthenticationToken(requestedApiKey, null, List.of());
      SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
  }
}
