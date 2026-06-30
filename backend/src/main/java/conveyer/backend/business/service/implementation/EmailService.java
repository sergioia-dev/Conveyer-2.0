package conveyer.backend.business.service.implementation;

import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import conveyer.backend.business.service.NotifyRouter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements NotifyRouter {

  private final SpringTemplateEngine templateEngine;
  private final JavaMailSender mailSender;

  public EmailService(SpringTemplateEngine templateEngine, JavaMailSender mailSender) {
    this.templateEngine = templateEngine;
    this.mailSender = mailSender;
  }

  @Override
  public String renderTemplate(String templateName, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process(templateName, context);
  }

  @Override
  public boolean sendVerificationCode(String to, String code, String projectName) {
    String html = renderTemplate("email/verification-code", Map.of(
        "code", code,
        "projectName", projectName
    ));
    return sendHtml(to, "Your verification code for " + projectName, html);
  }

  private boolean sendHtml(String to, String subject, String html) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
      return true;
    } catch (MessagingException e) {
      return false;
    }
  }
}
