package conveyer.backend.configuration;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import conveyer.backend.DTO.enums.ProviderTypes;
import conveyer.backend.business.repository.UserRepository;
import conveyer.backend.persistance.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;
  private final DefaultOAuth2UserService defaultOAuth2UserService;

  public CustomOAuth2UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
    this.defaultOAuth2UserService = new DefaultOAuth2UserService();
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(userRequest);

    String email = oAuth2User.getAttribute("email");
    String name = oAuth2User.getAttribute("name");
    if (name == null) {
      name = oAuth2User.getAttribute("login");
    }

    if (email == null) {
      throw new OAuth2AuthenticationException(
          "Email not available from GitHub. Ensure your GitHub account has a public email or the user:email scope is granted.");
    }

    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    User user;
    var existing = userRepository.findByEmail(email);
    if (existing.isPresent()) {
      user = existing.get();
      if (ProviderTypes.LOCAL.equals(user.getProvider())) {
        user.setProvider(ProviderTypes.GITHUB);
        user.setPassword(null);
        user.setUsername(name);
        user = userRepository.save(user);
      }
      if (!name.equals(user.getUsername())) {
        user.setUsername(name);
        user = userRepository.save(user);
      }
    } else {
      user = new User();
      user.setEmail(email);
      user.setUsername(name);
      user.setProvider(ProviderTypes.valueOf(registrationId.toUpperCase()));
      user.setPassword(null);
      user = userRepository.save(user);
    }

    Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
    Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

    return new DefaultOAuth2User(authorities, attributes, "id");
  }
}
