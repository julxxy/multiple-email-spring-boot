package cn.alphahub.multiple.email.aspect;

import cn.alphahub.multiple.email.annotation.Email;
import cn.alphahub.multiple.email.config.ClientWrapper;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * 邮件模板切面
 * <p>
 * 关于注解{@code @Email}作用在类和方法的优先级问题
 * <ul>
 *     <li>1. 当注解{@code @Email}作用类上时，该类所有邮件模板方法发送邮件的客户端都以注解{@code @Email}指定为准客户端</li>
 *     <li>2. 当注解{@code @Email}作用方法上时，该方法邮件客户端的为注解{@code @Email}指定的邮件客户端</li>
 *     <li>3. 当注解{@code @Email}同时作用类，和方法上时，类上{@code @Email}注解的优先级高于方法上注解{@code @Email}的优先级</li>
 * </ul>
 *
 * @author lwj
 * @version 1.0
 * @date 2021-09-07 10:51
 */
@Slf4j
@Aspect
@Component
public class EmailAspect {
    /**
     * mail sender thread local
     */
    public static final ThreadLocal<JavaMailSender> MAIL_SENDER_THREAD_LOCAL = new ThreadLocal<>();
    /**
     * mail properties thread local
     */
    public static final ThreadLocal<MailProperties> MAIL_PROPERTIES_THREAD_LOCAL = new ThreadLocal<>();

    /////////////////////////////////////////////////////////////
    //                  AOP Proxy On Class
    /////////////////////////////////////////////////////////////

    /**
     * 定义切入点方法
     */
    @Pointcut(value = "@within(cn.alphahub.multiple.email.annotation.Email)")
    public void pointcutProxyOnClass() {
        // a void method for proxy on class aspect pointcut.
    }


    /**
     * 目标方法执行之前执行
     *
     * @param point join point
     * @param email email annotation
     */
    @Before("pointcutProxyOnClass() && @within(email)")
    public void beforeProxyOnClass(JoinPoint point, Email email) {
        ClientWrapper wrapper = SpringUtil.getBean(ClientWrapper.class);
        MAIL_SENDER_THREAD_LOCAL.set(wrapper.getMailSender(email.name()));
        MAIL_PROPERTIES_THREAD_LOCAL.set(wrapper.getMailProperties(email.name()));
    }

    /**
     * 目标方法执行之后必定执行(无论是否报错)
     * <p>
     * 目标方法同时需要{@code @Email}注解的修饰，并且这里（通知）的形参名要与上面注解中的一致
     *
     * @param email email annotation
     */
    @After("pointcutProxyOnClass() && @within(email)")
    public void afterProxyOnClass(Email email) {
        if (MAIL_SENDER_THREAD_LOCAL.get() != null)
            MAIL_SENDER_THREAD_LOCAL.remove();
        if (MAIL_PROPERTIES_THREAD_LOCAL.get() != null)
            MAIL_PROPERTIES_THREAD_LOCAL.remove();
    }

    /**
     * 目标方法抛出异常后执行
     * <p>
     * 目标方法同时需要{@code @Email}注解的修饰，并且这里（通知）的形参名要与上面注解中的一致,可以声明来获取目标方法抛出的异常
     *
     * @param throwable some exception
     * @param email     email annotation
     */
    @AfterThrowing(pointcut = "pointcutProxyOnClass() && @within(email)", throwing = "throwable")
    public void afterThrowingProxyOnClass(Email email, Throwable throwable) {
        log.error("{}", throwable.getLocalizedMessage());
    }

    /////////////////////////////////////////////////////////////
    //                  AOP Proxy On Method
    /////////////////////////////////////////////////////////////

    /**
     * 定义切入点方法
     */
    @Pointcut(value = "@annotation(cn.alphahub.multiple.email.annotation.Email)")
    public void pointcut() {
        // a void method for proxy on method aspect pointcut.
    }

    /**
     * 目标方法执行之前执行
     *
     * @param point join point
     * @param email email annotation
     */
    @Before("pointcut() && @annotation(email)")
    public void before(JoinPoint point, Email email) {
        log.info("1. before");
        ClientWrapper wrapper = SpringUtil.getBean(ClientWrapper.class);
        MAIL_SENDER_THREAD_LOCAL.set(wrapper.getMailSender(email.name()));
        MAIL_PROPERTIES_THREAD_LOCAL.set(wrapper.getMailProperties(email.name()));
    }

    /**
     * 环绕通知
     *
     * @param point point
     * @return proceed
     * @throws Throwable throwable
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        log.info("2. around");
        long beginTime = System.currentTimeMillis();
        Object proceed = point.proceed();
        long endTime = System.currentTimeMillis() - beginTime;
        log.warn("2. around耗时：{}（ms），开始时间：{}，结束时间：{}", endTime, DateUtil.formatDateTime(new Date(beginTime)), DateUtil.formatDateTime(new Date(endTime)));
        return proceed;
    }

    /**
     * 目标方法执行之后必定执行(无论是否报错)
     * <p>
     * 目标方法同时需要{@code @Email}注解的修饰，并且这里（通知）的形参名要与上面注解中的一致
     *
     * @param email email annotation
     */
    @After("pointcut() && @annotation(email)")
    public void after(Email email) {
        log.info("3. after");
        if (MAIL_SENDER_THREAD_LOCAL.get() != null)
            MAIL_SENDER_THREAD_LOCAL.remove();
        if (MAIL_PROPERTIES_THREAD_LOCAL.get() != null)
            MAIL_PROPERTIES_THREAD_LOCAL.remove();
    }

    /**
     * 目标方法有返回值且正常返回后执行
     * <p>
     * 这里切入点方法的形参名{@code pointcut()}要与上面注解中的一致
     *
     * @param point        join point
     * @param responseData response data
     */
    @AfterReturning(pointcut = "pointcut()", returning = "responseData")
    public void afterReturning(JoinPoint point, Object responseData) {
        log.info("4. afterReturning, responseData: {}", Objects.isNull(responseData) ? "response data is null." : responseData.toString());
    }

    /**
     * 目标方法抛出异常后执行
     * <p>
     * 目标方法同时需要{@code @Email}注解的修饰，并且这里（通知）的形参名要与上面注解中的一致,可以声明来获取目标方法抛出的异常
     *
     * @param throwable some throwable
     * @param email     email annotation
     */
    @AfterThrowing(pointcut = "pointcut() && @annotation(email)", throwing = "throwable")
    public void afterThrowing(Email email, Throwable throwable) {
        log.error("5. afterThrowing, throwable: {}", throwable.getLocalizedMessage());
    }

    /////////////////////////////////////////////////////////////
    //                  END AOP CONSTRUCTION
    /////////////////////////////////////////////////////////////
}
