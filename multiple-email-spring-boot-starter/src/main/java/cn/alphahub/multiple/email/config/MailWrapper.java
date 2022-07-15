package cn.alphahub.multiple.email.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

/**
 * Client Wrapper
 *
 * @author weasley
 * @version 1.0
 * @date 2022/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailWrapper {
    /**
     * email properties map
     */
    private Map<String, MailProperties> emailPropertiesMap;
    /**
     * java mail sender map
     */
    private Map<String, JavaMailSender> javaMailSenderMap;

    /**
     * get email properties
     *
     * @param name name
     * @return MailProperties
     */
    public MailProperties getMailProperties(String name) {
        return this.emailPropertiesMap.get(name);
    }

    /**
     * get java mail sender
     *
     * @param name name
     * @return JavaMailSender
     */
    public JavaMailSender getMailSender(String name) {
        return this.javaMailSenderMap.get(name);
    }
}
