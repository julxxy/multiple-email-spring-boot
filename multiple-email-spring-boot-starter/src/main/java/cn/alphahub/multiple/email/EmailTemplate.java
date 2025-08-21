package cn.alphahub.multiple.email;

import cn.alphahub.multiple.email.aspect.EmailAspect;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 邮件模板方法默认实现
 *
 * @author lwj
 * @version 1.0
 * @date 2021-09-07 10:44
 */
@Slf4j
@Component
@Validated
@AutoConfigureBefore({MailProperties.class, JavaMailSender.class})
public class EmailTemplate {
    /**
     * default mail properties
     */
    @Autowired(required = false)
    private MailProperties defaultMailProperties;
    /**
     * default java mail sender
     */
    @Autowired(required = false)
    private JavaMailSender defaultJavaMailSender;
    /**
     * thread pool executor
     */
    @Autowired
    @Qualifier("emailThreadPoolExecutor")
    private ThreadPoolExecutor emailThreadPoolExecutor;

    /**
     * 获取邮件是发送实例
     *
     * @return JavaMailSender
     */
    private JavaMailSender getMailSender() {
        JavaMailSender mailSender = EmailAspect.MAIL_SENDER_TL.get();
        if (Objects.isNull(mailSender)) {
            return this.defaultJavaMailSender;
        }
        return mailSender;
    }

    /**
     * 获取电子邮件支持的配置属性
     *
     * @return MailProperties
     */
    private MailProperties getMailProperties() {
        MailProperties properties = EmailAspect.MAIL_PROPERTIES_TL.get();
        if (Objects.isNull(properties)) {
            return this.defaultMailProperties;
        }
        return properties;
    }

    /**
     * 发送给定的简单邮件消息
     *
     * @param data the message to send
     * @throws MailException Base class for all mail exceptions
     */
    public void send(@Valid SimpleMailMessageDomain data) throws Exception {
        SimpleMailMessage simpleMessage = new SimpleMailMessage();
        simpleMessage.setFrom(this.getMailProperties().getUsername());
        simpleMessage.setTo(data.getTo());
        if (ObjectUtils.isNotEmpty(data.getCc())) {
            simpleMessage.setCc(data.getCc());
        }
        simpleMessage.setSentDate(Objects.nonNull(data.getSentDate()) ? Date.from(data.getSentDate().atZone(ZoneId.systemDefault()).toInstant()) : new Date());
        simpleMessage.setSubject(data.getSubject());
        simpleMessage.setText(data.getText());
        JavaMailSender mailSender = this.getMailSender();

        RequestAttributes mainThreadRequestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> sendResponseFuture = CompletableFuture.runAsync(() -> {
            log.info("Current send simple message thread info: '{}' '{}' '{}'", Thread.currentThread().getId(), Thread.currentThread().getThreadGroup().getName(), Thread.currentThread().getName());
            RequestContextHolder.setRequestAttributes(mainThreadRequestAttributes);
            mailSender.send(simpleMessage);
        }, emailThreadPoolExecutor);

        try {
            CompletableFuture.allOf(sendResponseFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("发送给定的简单邮件消息失败: {}", data, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 发送带附件的邮件
     *
     * @param data metadata of message to send
     * @param file Nullable, support for spring MVC upload file received in the request, can be null.
     * @throws MessagingException messaging exception
     */
    public void send(@Valid MimeMessageDomain data, @Nullable MultipartFile file) throws Exception {
        JavaMailSender mailSender = this.getMailSender();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(this.getMailProperties().getUsername());
        helper.setTo(data.getTo());
        helper.setCc(data.getCc());
        helper.setSentDate(Objects.nonNull(data.getSentDate()) ? Date.from(data.getSentDate().atZone(ZoneId.systemDefault()).toInstant()) : new Date());
        helper.setSubject(data.getSubject());
        helper.setText(data.getText(), true);
        if (Objects.nonNull(file) && !file.isEmpty()) {
            helper.addAttachment(Objects.requireNonNull(file.getOriginalFilename(), "The attachment file name (including file suffix) cannot be empty"), file);
        }
        if (StringUtils.isNoneBlank(data.getFilepath())) {
            File newFile = new File(data.getFilepath());
            helper.addAttachment(newFile.getName(), newFile);
        }
        RequestAttributes mainThreadRequestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> sendResponseFuture = CompletableFuture.runAsync(() -> {
            log.info("Current send mime mime message thread info: '{}' '{}' '{}'", Thread.currentThread().getId(), Thread.currentThread().getThreadGroup().getName(), Thread.currentThread().getName());
            RequestContextHolder.setRequestAttributes(mainThreadRequestAttributes);
            mailSender.send(mimeMessage);
        }, emailThreadPoolExecutor);

        try {
            CompletableFuture.allOf(sendResponseFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("发送带附件的邮件失败: {}", data, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 简单邮件消息对象
     * <p>支持文本消息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleMailMessageDomain {
        /**
         * 收件人的邮箱
         */
        @Email(regexp = "^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$", message = "收件人邮箱格式不正确")
        @NotBlank(message = "收件人邮箱不能为空")
        private String to;
        /**
         * 抄送邮箱（非必填）
         */
        private String[] cc;
        /**
         * 邮件发送日期, 默认当前时刻: {@code new Date()} 提交格式: yyyy-MM-dd HH:mm:ss
         */
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime sentDate;
        /**
         * 邮件主题、邮件标题
         */
        @NotBlank(message = "邮件主题不能为空")
        private String subject;
        /**
         * 邮件正文、邮件内容
         */
        @NotBlank(message = "邮件正文不能为空")
        private String text;
    }

    /**
     * 带附件邮件消息对象
     * <p>支持附件：图片、文件等资源
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MimeMessageDomain {
        /**
         * 收件人的邮箱
         */
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(regexp = "^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$", message = "收件人邮箱格式不正确")
        private String to;
        /**
         * 抄送邮箱（非必填）
         */
        private String[] cc;
        /**
         * 邮件发送日期, 默认当前时刻: {@code new Date()} 提交格式: yyyy-MM-dd HH:mm:ss
         */
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime sentDate;
        /**
         * 邮件主题、邮件标题
         */
        @NotBlank(message = "邮件主题不能为空")
        private String subject;
        /**
         * 邮件正文、邮件内容 （可以是html字符串）
         */
        @NotBlank(message = "邮件正文不能为空")
        private String text;
        /**
         * 附件文件的路径 （没有附件文件不用还传）
         */
        private String filepath;
    }
}
