package com.webank.weevent.broker.fisco;

import java.util.HashMap;
import java.util.Map;

import com.webank.weevent.JUnitTestBase;
import com.webank.weevent.broker.plugin.IProducer;
import com.webank.weevent.sdk.BrokerException;
import com.webank.weevent.sdk.ErrorCode;
import com.webank.weevent.sdk.SendResult;
import com.webank.weevent.sdk.WeEvent;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * FiscoBcosBroker4Producer Tester.
 *
 * @author websterchen
 * @author matthewliu
 * @version 1.0
 * @since 11/08/2018
 */
@Slf4j
public class FiscoBcosBroker4ProducerTest extends JUnitTestBase {
    private IProducer iProducer;

    @Before
    public void before() throws Exception {
        log.info("=============================={}.{}==============================",
                this.getClass().getSimpleName(),
                this.testName.getMethodName());

        this.iProducer = IProducer.build();
        Assert.assertNotNull(this.iProducer);
        this.iProducer.startProducer();
        Assert.assertTrue(this.iProducer.open(this.topicName, this.groupId));
    }

    /**
     * Method: startProducer(String topic)
     */
    @Test
    public void testStartProducer() throws Exception {
        Assert.assertTrue(this.iProducer.startProducer());
    }

    /**
     * Method: shutdownProducer()
     */
    @Test
    public void testShutdownProducer() {
        Assert.assertTrue(this.iProducer.shutdownProducer());
    }

    /**
     * Method: publish(WeEvent event)
     */
    @Test
    public void testPublishEvent() throws Exception {
        SendResult sendResult = this.iProducer.publish(new WeEvent(this.topicName, "hello world.".getBytes()), this.groupId);
        Assert.assertEquals(SendResult.SendResultStatus.SUCCESS, sendResult.getStatus());
    }

    /**
     * topic is exist and content is Chinese
     */
    @Test
    public void testPublishTopicExists() throws Exception {
        SendResult dto = this.iProducer.publish(new WeEvent(this.topicName, "中文消息.".getBytes()), this.groupId);
        Assert.assertEquals(SendResult.SendResultStatus.SUCCESS, dto.getStatus());
    }

    /**
     * test extensions is null
     */
    @Test
    public void testPublishExtIsNull() {
        try {
            SendResult sendResult = this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes(), null), this.groupId);
            Assert.assertEquals(SendResult.SendResultStatus.SUCCESS, sendResult.getStatus());
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_EXTENSIONS_IS_NUll.getCode(), e.getCode());
        }
    }

    /**
     * extensions contain multiple key and value ,key start with 'weevent-'
     */
    @Test
    public void testPublishExtContainMulKey() throws BrokerException {
        Map<String, String> ext = new HashMap<>();
        ext.put("weevent-test", "test value");
        ext.put("weevent-test2", "test value2");
        ext.put("weevent-test3", "test value3");
        SendResult sendResult = this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes(), ext), this.groupId);
        Assert.assertEquals(sendResult.getStatus(), SendResult.SendResultStatus.SUCCESS);
    }

    /**
     * extensions contain multiple key and value ,one key not start with 'weevent-'
     */
    @Test
    public void testPublishExtContainMulKeyContainNotInvalidKey() {
        try {
            Map<String, String> ext = new HashMap<>();
            ext.put("weevent-test", "test value");
            ext.put("weevent-test2", "test value2");
            ext.put("test3", "test value3");
            this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes(), ext), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_EXTENSIONS_KEY_INVALID.getCode(), e.getCode());
        }
    }

    /**
     * extensions contain one key and value, key not start with 'weevent-'
     */
    @Test
    public void testPublishExtContainNotInvalidKey() {
        try {
            Map<String, String> ext = new HashMap<>();
            ext.put("test1", "test value");
            this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes(), ext), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_EXTENSIONS_KEY_INVALID.getCode(), e.getCode());
        }
    }

    /**
     * topic is not exists
     */
    @Test
    public void testPublishTopicNotExist() {
        try {
            String topicNotExists = "fsgdsggdgerer";
            this.iProducer.publish(new WeEvent(topicNotExists, "中文消息.".getBytes()), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.TOPIC_NOT_EXIST.getCode(), e.getCode());
        }
    }

    /**
     * topic is blank
     */
    @Test
    public void testPublishTopicIsBlank() {
        try {
            this.iProducer.publish(new WeEvent(" ", "中文消息.".getBytes()), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.TOPIC_IS_BLANK.getCode(), e.getCode());
        }
    }

    /**
     * topic is null
     */
    @Test
    public void testPublishTopicIsNull() {
        try {
            this.iProducer.publish(new WeEvent(null, "中文消息.".getBytes()), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.TOPIC_IS_BLANK.getCode(), e.getCode());
        }
    }

    /**
     * topic length > 64
     */
    @Test
    public void testPublishTopicOverMaxLen() {
        try {
            String topicNotExists = "fsgdsggdgererqwertyuioplkjhgfdsazxqazwsxedcrfvtgbyhnujmikolppoiuyt";
            this.iProducer.publish(new WeEvent(topicNotExists, "中文消息.".getBytes()), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.TOPIC_EXCEED_MAX_LENGTH.getCode(), e.getCode());
        }
    }

    /**
     * topic is exits and content is null
     */
    @Test
    public void testPublishContentIsNull() {
        try {
            this.iProducer.publish(new WeEvent(this.topicName, null), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_CONTENT_IS_BLANK.getCode(), e.getCode());
        }
    }

    /**
     * topic is exits and content is blank
     */
    @Test
    public void testPublishContentIsBlank() {
        try {
            byte[] bytes = "".getBytes();
            this.iProducer.publish(new WeEvent(this.topicName, bytes), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_CONTENT_IS_BLANK.getCode(), e.getCode());
        }
    }

    /**
     * groupId is null
     */
    @Test
    public void testPublishGroupIdIsNull() {
        try {
            this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes()), null);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_GROUP_ID_INVALID.getCode(), e.getCode());
        }
    }

    /**
     * groupId is not exist
     */
    @Test
    public void testPublishGroupIdIsNotExist() {
        try {
            this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes()), "4");
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.WEB3SDK_UNKNOWN_GROUP.getCode(), e.getCode());
        }
    }

    /**
     * groupId is not number
     */
    @Test
    public void testPublishGroupIdIsNotNum() {
        try {
            this.iProducer.publish(new WeEvent(this.topicName, "this is only test message".getBytes()), "sfsdf");
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.EVENT_GROUP_ID_INVALID.getCode(), e.getCode());
        }
    }

    /**
     * topic contain special character without in [32,128]
     */
    @Test
    public void testPublishTopicContainSpecialChar() {
        try {
            char[] charStr = {69, 72, 31};
            String illegalTopic = new String(charStr);
            byte[] bytes = "hello world".getBytes();
            this.iProducer.publish(new WeEvent(illegalTopic, bytes), this.groupId);
            Assert.fail();
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.TOPIC_CONTAIN_INVALID_CHAR.getCode(), e.getCode());
        }
    }

    /**
     * topic is Chinese character
     */
    @Test
    public void testPublishTopicContainChineseChar() {
        try {
            byte[] bytes = "".getBytes();
            this.iProducer.publish(new WeEvent("中国", bytes), this.groupId);
        } catch (BrokerException e) {
            Assert.assertEquals(ErrorCode.TOPIC_CONTAIN_INVALID_CHAR.getCode(), e.getCode());
        }
    }

    /**
     * publish with custom header
     */
    @Test
    public void testPublishTag() throws Exception {
        Map<String, String> ext = new HashMap<>();
        ext.put(WeEvent.WeEvent_TAG, "create");
        SendResult sendResult = this.iProducer.publish(new WeEvent(this.topicName, "hello tag: create".getBytes(), ext), this.groupId);
        Assert.assertEquals(sendResult.getStatus(), SendResult.SendResultStatus.SUCCESS);
    }
}
