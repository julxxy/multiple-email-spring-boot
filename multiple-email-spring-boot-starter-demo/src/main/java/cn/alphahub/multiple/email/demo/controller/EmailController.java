package cn.alphahub.multiple.email.demo.controller;


import cn.alphahub.multiple.email.EmailTemplate;
import cn.alphahub.multiple.email.annotation.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.alphahub.multiple.email.EmailTemplate.MimeMessageDomain;
import static cn.alphahub.multiple.email.EmailTemplate.SimpleMailMessageDomain;

/**
 * 邮件Controller (测试用)
 *
 * @author lwj
 * @version 1.0
 * @date 2021-09-09 14:01
 */
@Slf4j
@RestController
@RequestMapping("/site/email")
public class EmailController {

    @Autowired
    private EmailTemplate emailTemplate;

    /**
     * 发送简单邮件
     * <p>此方法没有标注 {@code @Email} 注解，将使用默认邮件模板发送。</p>
     *
     * @param message 简单邮件消息对象
     * @return a {@code ResponseEntity} with a success message
     */
    @PostMapping("/simple/send")
    public ResponseEntity<String> sendSimpleEmail(@RequestBody @Validated SimpleMailMessageDomain message) throws Exception {
        log.info("Attempting to send simple email: {}", message);
        emailTemplate.send(message);
        return ResponseEntity.ok("Simple email sent successfully.");
    }

    /**
     * 发送带附件的邮件
     * <p>此方法标注了 {@code @Email(name = "EmailOffice365")}，将使用名为 'EmailOffice365' 的邮件模板发送。</p>
     *
     * @param message Mime邮件消息对象
     * @param file    附件 (可选)
     * @return a {@code ResponseEntity} with a success or error message
     */
    @Email(name = "EmailOffice365")
    @PostMapping("/mime/send")
    public ResponseEntity<String> sendMimeEmail(@ModelAttribute @Validated MimeMessageDomain message,
                                                @RequestPart(required = false) MultipartFile file) {
        log.info("Attempting to send mime email: {}", message);
        try {
            emailTemplate.send(message, file);
            return ResponseEntity.ok("Mime email sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send mime email for domain: {}", message, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send mime email: " + e.getMessage());
        }
    }

    /**
     * 使用指定模板发送简单邮件（用于测试AOP切换）
     * <p>此方法标注了 {@code @Email(name = "EmailOffice365")}，将使用名为 'EmailOffice365' 的邮件模板发送。</p>
     *
     * @return a {@code ResponseEntity} with a success message
     */
    @Email(name = "EmailOffice365")
    @PostMapping("/simple/send/nested")
    public ResponseEntity<String> sendSimpleEmailWithTemplate() throws Exception {
        SimpleMailMessageDomain message = new SimpleMailMessageDomain();
        message.setSubject("Test Subject from Nested Call");
        message.setTo("test@example.com");
        message.setText("This is a test message sent with a specific template.");
        log.info("Attempting to send simple email with 'EmailOffice365' template: {}", message);
        emailTemplate.send(message);
        return ResponseEntity.ok("Simple email with 'EmailOffice365' template sent successfully.");
    }
}
