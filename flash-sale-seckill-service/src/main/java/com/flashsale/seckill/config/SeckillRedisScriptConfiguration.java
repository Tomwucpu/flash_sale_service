package com.flashsale.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class SeckillRedisScriptConfiguration {

    @Bean
    RedisScript<Long> seckillAttemptRedisScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill_attempt.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
