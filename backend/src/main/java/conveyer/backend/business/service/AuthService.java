package conveyer.backend.business.service;

import com.nimbusds.jose.JOSEException;

import conveyer.backend.DTO.SignInRequestDTO;
import conveyer.backend.DTO.SignInResponseDTO;
import conveyer.backend.DTO.SignUpDTO;

public interface AuthService {

  public boolean createLocalAccount(SignUpDTO dto);

  public SignInResponseDTO signIn(SignInRequestDTO dto) throws JOSEException;

  public SignInResponseDTO refreshAccessToken(String refreshToken);

  public String checkStatus(String accessToken);

  public SignInResponseDTO handleOAuth2Login(String email) throws JOSEException;

  public void logout(String accessToken);

}
