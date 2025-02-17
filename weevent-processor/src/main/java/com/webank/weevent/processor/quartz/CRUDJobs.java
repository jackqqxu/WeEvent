package com.webank.weevent.processor.quartz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.webank.weevent.processor.cache.CEPRuleCache;
import com.webank.weevent.processor.model.CEPRule;
import com.webank.weevent.sdk.BrokerException;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CRUDJobs implements Job {

    public void execute(JobExecutionContext context) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String jobName = context.getJobDetail().getKey().getName();
        String type = context.getJobDetail().getJobDataMap().get("type").toString();
        log.info("{},{} Job execute {}    executing...", this.toString(), jobName, f.format(new Date()));

        switch (type) {
            case "startCEPRule":
                startCEPRule(context, jobName);
                break;

            default:
                log.info("the job name type:{}", type);
                break;
        }
    }


    private static void startCEPRule(JobExecutionContext context, String jobName) {
        Object obj = context.getJobDetail().getJobDataMap().get("rule");
        // ruleMap
        Map<String,CEPRule> ruleMap = (HashMap)context.getJobDetail().getJobDataMap().get("ruleMap");
        try {
            if (obj instanceof CEPRule) {
                log.info("{}", (CEPRule) obj);
                CEPRule rule = (CEPRule) obj;
                // check the status,when the status equal 1,then update
                if (1 == rule.getStatus()||0 == rule.getStatus()||2 == rule.getStatus()) {
                    CEPRuleCache.updateCEPRule(rule, ruleMap);
                }
                log.info("startCEPRule in job: {},rule:{}", jobName, JSONObject.toJSON(obj));
            }
        }catch (BrokerException e){
            log.info("BrokerException:{}",e.toString());
        }

    }




}
