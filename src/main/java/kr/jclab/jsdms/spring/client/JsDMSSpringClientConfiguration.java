/*
 * Copyright 2018 JC-Lab. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.jclab.jsdms.spring.client;

import kr.jclab.jsdms.spring.client.internal.ServiceImpl;
import kr.jclab.jsdms.spring.client.service.JsDMSSpringClientService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
@EnableConfigurationProperties(JsDMSSpringClientProperties.class)
public class JsDMSSpringClientConfiguration implements BeanPostProcessor {
    @Autowired
    private JsDMSSpringClientProperties properties;
    private ServiceImpl service;

    @PostConstruct
    protected void init() {
        service = new ServiceImpl(properties);
    }

    @ConditionalOnMissingBean
    @Bean
    public JsDMSSpringClientService service() {
        return this.service;
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        if(this.service != null) {
            this.service.stop();
            this.service = null;
        }
    }
}
