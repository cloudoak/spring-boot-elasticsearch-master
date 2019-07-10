package spring.boot.elasticsearch.actuator;

import net.bull.javamelody.MonitoringFilter;
import net.bull.javamelody.SessionListener;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator Configuration for Boot Elasticsearch REST clients.
 *
 * @author OAK
 * @since 2019/06/24 19:22:00 PM.
 * @version  1.0
 */
@Configuration
public class ActuatorConfiguration {
    @Bean
    public FilterRegistrationBean monitorFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new MonitoringFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }

    @Bean
    public ServletListenerRegistrationBean sessionListener() {
        ServletListenerRegistrationBean servletListenerRegistrationBean = new ServletListenerRegistrationBean();
        servletListenerRegistrationBean.setListener(new SessionListener());
        return servletListenerRegistrationBean;
    }

}
