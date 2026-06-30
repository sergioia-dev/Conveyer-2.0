package conveyer.backend.configuration.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import conveyer.backend.DTO.HTTPResponseDTO;
import conveyer.backend.configuration.service.RateLimitingService;
import tools.jackson.databind.ObjectMapper;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final RateLimitingService rateLimitingService;

  public RateLimitingFilter(RateLimitingService rateLimitingService) {
    this.rateLimitingService = rateLimitingService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String clientIp = getClientIp(request);

    Bucket tokenBucket = rateLimitingService.resolveBucket(clientIp);

    var probe = tokenBucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaning", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      Long waitForRefil = probe.getNanosToWaitForRefill() / 1_000_000_000;
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefil));
      response.setContentType("application/json");

      HTTPResponseDTO jsonResponse = new HTTPResponseDTO(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests",
          "You have exhausted your API Request Quota, Retry again in " + waitForRefil.toString() + " seconds");
      ObjectMapper mapper = new ObjectMapper();

      String jsonString = mapper.writeValueAsString(jsonResponse);

      response.getWriter().write(jsonString);
      ;

    }

  }

  private String getClientIp(HttpServletRequest request) {
    // Check for X-Forwarded-For, This is use when using a Load balancer or a Proxy
    String xfHeader = request.getHeader("X-Forwarded-For");

    if (xfHeader == null || xfHeader.isEmpty()) {
      return request.getRemoteAddr();
    }

    return xfHeader.split(",")[0].trim();

  }
}
