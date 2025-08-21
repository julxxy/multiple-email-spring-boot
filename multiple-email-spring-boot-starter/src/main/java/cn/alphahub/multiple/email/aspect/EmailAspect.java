package cn.alphahub.multiple.email.aspect;

import cn.alphahub.multiple.email.annotation.Email;
import cn.alphahub.multiple.email.config.MailWrapper;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件模板切面
 * <p>
 * 关于注解{@code @Email}作用在类和方法的优先级问题
 * <ul>
 *     <li>1. 当注解{@code @Email}作用类上时，该类所有邮件模板方法发送邮件的客户端都以注解{@code @Email}指定为准客户端</li>
 *     <li>2. 当注解{@code @Email}作用方法上时，该方法邮件客户端的为注解{@code @Email}指定的邮件客户端</li>
 *     <li>3. 当注解{@code @Email}同时作用类和方法上时，方法上{@code @Email}注解的优先级高于类上注解{@code @Email}的优先级</li>
 * </ul>
 *
 * @author lwj
 * @version 1.2.0
 * @date 2021-09-07 10:51
 */
@Slf4j
@Aspect
@Component
public class EmailAspect {
    /**
     * Mail sender thread local
     */
    public static final ThreadLocal<JavaMailSender> MAIL_SENDER_TL = new ThreadLocal<>();
    /**
     * Mail properties thread local
     */
    public static final ThreadLocal<MailProperties> MAIL_PROPERTIES_TL = new ThreadLocal<>();

    /**
     * 邮件发送器
     */
    private final MailWrapper mailWrapper;

    public EmailAspect(MailWrapper mailWrapper) {
        this.mailWrapper = mailWrapper;
    }

    /**
     * 定义切入点：匹配任何使用 @Email 注解的方法或在使用了 @Email 注解的类中的任何公共方法
     */
    @Pointcut("@annotation(cn.alphahub.multiple.email.annotation.Email) || @within(cn.alphahub.multiple.email.annotation.Email)")
    public void emailPointcut() {
        // a void method for aspect pointcut.
    }

    /**
     * 环绕通知
     * <p>在方法执行前后进行织入，并控制目标方法的执行</p>
     *
     * @param point ProceedingJoinPoint
     * @return 目标方法的返回值
     * @throws Throwable 目标方法执行过程中抛出的异常
     */
    @Around("emailPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Email email = getEmailAnnotation(point);
        if (email == null) {
            // 理论上不会发生，因为切点已经限制了必须有注解
            return point.proceed();
        }

        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getSimpleName();

        try {
            // 设置邮件配置
            MAIL_SENDER_TL.set(mailWrapper.getMailSender(email.name()));
            MAIL_PROPERTIES_TL.set(mailWrapper.getMailProperties(email.name()));
            log.debug("Switched to email client [{}].", email.name());

            // 记录开始时间
            long startTime = System.currentTimeMillis();
            log.debug("开始执行邮件方法: {}.{}", className, methodName);

            // 执行目标方法
            Object result = point.proceed();

            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            log.info("邮件方法执行完成: {}.{}, 耗时: {}ms", className, methodName, duration);

            return result;

        } catch (Exception e) {
            log.error("邮件方法执行异常: {}.{}, 错误信息: {}", className, methodName, e.getMessage(), e);
            throw e;
        } finally {
            // 清理ThreadLocal，避免内存泄漏
            MAIL_SENDER_TL.remove();
            MAIL_PROPERTIES_TL.remove();
            log.debug("Cleaned up email client ThreadLocal.");
        }
    }

    /**
     * 获取Email注解实例
     * <p>优先级：方法 > 类</p>
     *
     * @param point ProceedingJoinPoint
     * @return Email注解实例，可能为null
     */
    private Email getEmailAnnotation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 优先从方法上获取注解
        Email email = AnnotationUtils.findAnnotation(method, Email.class);
        if (email != null) {
            return email;
        }

        // 如果方法上没有，则从类上获取注解
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), Email.class);
    }
}
