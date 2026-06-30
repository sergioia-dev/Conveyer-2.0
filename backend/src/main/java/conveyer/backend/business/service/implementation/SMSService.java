package conveyer.backend.business.service.implementation;

import java.util.Map;

import org.springframework.stereotype.Service;

import conveyer.backend.business.service.NotifyRouter;

@Service
public class SMSService implements NotifyRouter {

  @Override
  public boolean sendVerificationCode(String to, String code, String projectName) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String renderTemplate(String templateName, Map<String, Object> variables) {
    // TODO Auto-generated method stub
    return null;
  }

}
