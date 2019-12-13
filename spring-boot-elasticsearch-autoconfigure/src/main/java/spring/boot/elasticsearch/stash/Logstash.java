package spring.boot.elasticsearch.stash;

import spring.boot.elasticsearch.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.joda.time.Instant;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Intercept send servlet request stats summary payload millis.
 *
 * @author OAK
 * @since 2019/06/25 13:07:00 PM.
 * @version 1.0
 *
 */
@Component
@Slf4j
@Aspect
@EnableAspectJAutoProxy(exposeProxy = true)
public class Logstash {

    /**
     * payload summary stats.
     */
    private AtomicLong total = new AtomicLong(0);

    /**
     * Current Service for Aspect point cut method to stat payload.
     */
    @Pointcut("execution(* spring.boot.elasticsearch.rest.service.*.*(..))")
    private void payload(){

    }

    /**
     * Around advice.
     * @param pjp Current Service for Aspect proceeding join point.
     * @return proceed object.
     * @throws Throwable Throwable object.
     */
    @Around("payload()")
    public  Object doAround(ProceedingJoinPoint pjp) throws Throwable{
        Instant instant = Instant.now();
        Object obj = pjp.proceed();
        Long millis = Instant.now().minus(instant.getMillis()).getMillis();
        Long sumMillis = total.addAndGet(millis);
        Object target = pjp.getTarget();
        if(target instanceof ElasticsearchService){
            log.info("Pass to method：{}，Arguments：{}，Execution time：{} nanosecond，Time consuming：{} millisecond, Total time consuming：{} millisecond",
                    pjp.getSignature().toString(), ""/*Arrays.toString(pjp.getArgs())*/, millis*1000000, millis, sumMillis);
        }else{
            log.info("Pass to method：{}，Arguments：{}，Execution time：{} nanosecond，Time consuming：{} millisecond",
                    pjp.getSignature().toString(), ""/*Arrays.toString(pjp.getArgs())*/, millis*1000000, millis);
        }
        return obj;
    }
}
