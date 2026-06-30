package conveyer.backend.business.service;

import java.util.Map;

public interface NotifyRouter {

  String renderTemplate(String templateName, Map<String, Object> variables);

  boolean sendVerificationCode(String to, String code, String projectName);
}
