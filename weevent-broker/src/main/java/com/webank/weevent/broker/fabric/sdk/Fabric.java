package com.webank.weevent.broker.fabric.sdk;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.webank.weevent.BrokerApplication;
import com.webank.weevent.broker.fabric.config.FabricConfig;
import com.webank.weevent.broker.fabric.dto.TransactionInfo;
import com.webank.weevent.broker.fisco.constant.WeEventConstants;
import com.webank.weevent.broker.fisco.dto.ListPage;
import com.webank.weevent.broker.fisco.util.DataTypeUtils;
import com.webank.weevent.broker.fisco.util.ParamCheckUtils;
import com.webank.weevent.protocol.rest.entity.GroupGeneral;
import com.webank.weevent.protocol.rest.entity.TbBlock;
import com.webank.weevent.protocol.rest.entity.TbNode;
import com.webank.weevent.protocol.rest.entity.TbTransHash;
import com.webank.weevent.sdk.BrokerException;
import com.webank.weevent.sdk.ErrorCode;
import com.webank.weevent.sdk.SendResult;
import com.webank.weevent.sdk.TopicInfo;
import com.webank.weevent.sdk.TopicPage;
import com.webank.weevent.sdk.WeEvent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

/**
 * @author websterchen
 * @version v1.1
 * @since 2019/8/20
 */
@Slf4j
public class Fabric {
    //config
    private FabricConfig fabricConfig;
    private static HFClient hfClient = null;
    private static Channel channel = null;
    // topic info list in local memory
    private Map<String, TopicInfo> topicInfo = new ConcurrentHashMap<>();

    public Fabric(FabricConfig fabricConfig) {
        this.fabricConfig = fabricConfig;
    }

    public void init(String channelName) {
        try {
            this.hfClient = FabricSDKWrapper.initializeClient(this.fabricConfig);
            this.channel = FabricSDKWrapper.initializeChannel(hfClient, channelName, this.fabricConfig);
        } catch (Exception e) {
            log.error("init fabric failed", e);
            BrokerApplication.exit();
        }
    }

    public TopicInfo getTopicInfo(String topicName) throws BrokerException {
        if (this.topicInfo.containsKey(topicName)) {
            return this.topicInfo.get(topicName);
        }

        if (!isTopicExist(topicName)) {
            throw new BrokerException(ErrorCode.TOPIC_NOT_EXIST);
        }

        try {
            ChaincodeID chaincodeID = FabricSDKWrapper.getChainCodeID(fabricConfig.getTopicControllerName(), fabricConfig.getTopicControllerVersion());
            TransactionInfo transactionInfo = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, false,
                    "getTopicInfo", fabricConfig.getTransactionTimeout(), topicName);
            if (ErrorCode.SUCCESS.getCode() != transactionInfo.getCode()){
                throw new BrokerException(transactionInfo.getCode(), transactionInfo.getMessage());
            }
            TopicInfo topicInfo = JSONObject.parseObject(transactionInfo.getPayLoad(), TopicInfo.class);

            this.topicInfo.put(topicName, topicInfo);
            return topicInfo;
        } catch (InterruptedException | ProposalException | ExecutionException | InvalidArgumentException exception) {
            log.error("publish event failed due to transaction execution error.", exception);
            throw new BrokerException(ErrorCode.TRANSACTION_EXECUTE_ERROR);
        } catch (TimeoutException timeout) {
            log.error("publish event failed due to transaction execution timeout.", timeout);
            throw new BrokerException(ErrorCode.TRANSACTION_TIMEOUT);
        }
    }

    public boolean createTopic(String topicName) throws BrokerException {
        try {
            ChaincodeID chaincodeID = FabricSDKWrapper.getChainCodeID(fabricConfig.getTopicControllerName(),
                    fabricConfig.getTopicControllerVersion());
            TransactionInfo transactionInfo = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, true,"addTopicInfo",
                    fabricConfig.getTransactionTimeout(), topicName, getTimestamp(System.currentTimeMillis()),fabricConfig.getTopicVerison());
            if (ErrorCode.SUCCESS.getCode() != transactionInfo.getCode()){
                if (WeEventConstants.TOPIC_ALREADY_EXIST.equals(transactionInfo.getMessage())){
                    throw new BrokerException(ErrorCode.TOPIC_ALREADY_EXIST);
                }
                throw new BrokerException(transactionInfo.getCode(), transactionInfo.getMessage());
            }
            return true;
        } catch (InterruptedException | ProposalException | ExecutionException | InvalidArgumentException exception) {
            log.error("create topic :{} failed due to transaction execution error.{}", topicName, exception);
            throw new BrokerException(ErrorCode.TRANSACTION_EXECUTE_ERROR);
        } catch (TimeoutException timeout) {
            log.error("create topic :{} failed due to transaction execution timeout. {}", topicName, timeout);
            throw new BrokerException(ErrorCode.TRANSACTION_TIMEOUT);
        }
    }

    public boolean isTopicExist(String topicName) throws BrokerException {
        try {
            ChaincodeID chaincodeID = FabricSDKWrapper.getChainCodeID(fabricConfig.getTopicControllerName(),
                    fabricConfig.getTopicControllerVersion());
            TransactionInfo transactionInfo = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, false,
                    "isTopicExist", fabricConfig.getTransactionTimeout(),topicName);

            return ErrorCode.SUCCESS.getCode() == transactionInfo.getCode();
        } catch (InterruptedException | ProposalException | ExecutionException | InvalidArgumentException exception) {
            log.error("create topic :{} failed due to transaction execution error.{}", topicName, exception);
            throw new BrokerException(ErrorCode.TRANSACTION_EXECUTE_ERROR);
        } catch (TimeoutException timeout) {
            log.error("create topic :{} failed due to transaction execution timeout. {}", topicName, timeout);
            throw new BrokerException(ErrorCode.TRANSACTION_TIMEOUT);
        }
    }

    public TopicPage listTopicName(Integer pageIndex, Integer pageSize) throws BrokerException {
        TopicPage topicPage = new TopicPage();
        try {
            ChaincodeID chaincodeID = FabricSDKWrapper.getChainCodeID(fabricConfig.getTopicControllerName(),
                    fabricConfig.getTopicControllerVersion());
            TransactionInfo transactionInfo = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, false,
                    "listTopicName", fabricConfig.getTransactionTimeout(), String.valueOf(pageIndex), String.valueOf(pageSize));

            if (ErrorCode.SUCCESS.getCode() != transactionInfo.getCode()){
                throw new BrokerException(transactionInfo.getCode(), transactionInfo.getMessage());
            }

            ListPage<String> listPage = JSON.parseObject(transactionInfo.getPayLoad(), new TypeReference<ListPage<String>>(){});
            topicPage.setPageIndex(pageIndex);
            topicPage.setPageSize(listPage.getPageSize());
            topicPage.setTotal(listPage.getTotal());
            for (String topic : listPage.getPageData()) {
                topicPage.getTopicInfoList().add(getTopicInfo(topic));
            }

            return topicPage;
        } catch (InterruptedException | ProposalException | ExecutionException | InvalidArgumentException exception) {
            log.error("list topicName failed due to transaction execution error.{}", exception);
            throw new BrokerException(ErrorCode.TRANSACTION_EXECUTE_ERROR);
        } catch (TimeoutException timeout) {
            log.error("list topicName failed due to transaction execution timeout. {}", timeout);
            throw new BrokerException(ErrorCode.TRANSACTION_TIMEOUT);
        }
    }

    public WeEvent getEvent(String eventId) throws BrokerException {
        ParamCheckUtils.validateEventId("", eventId, getBlockHeight());

        Long blockNum = DataTypeUtils.decodeBlockNumber(eventId);
        List<WeEvent> events = this.loop(blockNum);
        for (WeEvent event : events) {
            if (eventId.equals(event.getEventId())) {
                log.info("event:{}", event);
                return event;
            }
        }

        throw new BrokerException(ErrorCode.EVENT_ID_NOT_EXIST);

    }

    public SendResult publishEvent(String topicName, String eventContent, String extensions) throws BrokerException {
        if (!isTopicExist(topicName)) {
            throw new BrokerException(ErrorCode.TOPIC_NOT_EXIST);
        }

        SendResult sendResult = new SendResult();
        try {
            ChaincodeID chaincodeID = getChaincodeID(fabricConfig);
            TransactionInfo transactionInfo = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, true, "publish", fabricConfig.getTransactionTimeout(), topicName, eventContent, extensions);
            sendResult.setStatus(SendResult.SendResultStatus.SUCCESS);
            sendResult.setEventId(DataTypeUtils.encodeEventId(topicName, transactionInfo.getBlockNumber().intValue(), Integer.parseInt(transactionInfo.getPayLoad())));
            sendResult.setTopic(topicName);
            return sendResult;
        } catch (InterruptedException | ProposalException | ExecutionException | InvalidArgumentException exception) {
            log.error("publish event failed due to transaction execution error.", exception);
            throw new BrokerException(ErrorCode.TRANSACTION_EXECUTE_ERROR);
        } catch (TimeoutException timeout) {
            log.error("publish event failed due to transaction execution timeout.", timeout);
            sendResult.setStatus(SendResult.SendResultStatus.TIMEOUT);
            return sendResult;
        }
    }

    private static ChaincodeID getChaincodeID(FabricConfig fabricConfig) throws InvalidArgumentException, ProposalException, InterruptedException, ExecutionException, TimeoutException {
        ChaincodeID chaincodeID = FabricSDKWrapper.getChainCodeID(fabricConfig.getTopicControllerName(), fabricConfig.getTopicControllerVersion());
        String topicContractName = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, false, "getTopicContractName", fabricConfig.getTransactionTimeout()).getPayLoad();
        String topicContractVersion = FabricSDKWrapper.executeTransaction(hfClient, channel, chaincodeID, false, "getTopicContractVersion", fabricConfig.getTransactionTimeout()).getPayLoad();

        return FabricSDKWrapper.getChainCodeID(topicContractName, topicContractVersion);
    }

    public Long getBlockHeight() throws BrokerException {
        try {
            return channel.queryBlockchainInfo().getHeight() - 1;
        } catch (Exception e) {
            log.error("get block height error:{}", e);
            throw new BrokerException(ErrorCode.GET_BLOCK_HEIGHT_ERROR);
        }
    }

    /**
     * Fetch all event in target block.
     *
     * @param blockNum the blockNum
     * @return java.lang.Integer null if net error
     */
    public List<WeEvent> loop(Long blockNum) throws BrokerException {
        List<WeEvent> weEventList = new ArrayList<>();
        try {
            weEventList = FabricSDKWrapper.getBlockChainInfo(channel, blockNum);
        } catch (Exception e) {
            log.error("getEvent error:{}", e);
        }
        return weEventList;
    }

    /**
     * Gets the ISO 8601 timestamp.
     *
     * @param date the date
     * @return the ISO 8601 timestamp
     */
    private static String getTimestamp(long date) {
        // TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // df.setTimeZone(tz);
        return df.format(date);
    }

    public GroupGeneral getGroupGeneral() throws BrokerException {

        try {
            return FabricSDKWrapper.getGroupGeneral(channel);
        } catch (ProposalException | InvalidArgumentException e) {
            log.error("get group general error:{}", e);
            throw new BrokerException(ErrorCode.FABRICSDK_GETBLOCKINFO_ERROR);
        }
    }

    public List<TbTransHash> queryTransList(BigInteger blockNumber) throws BrokerException {

        try {
            return FabricSDKWrapper.queryTransList(fabricConfig, channel, blockNumber);
        } catch (InvalidArgumentException | ProposalException  e) {
            log.error("query trans list by transHash and blockNum error:{}", e);
            throw new BrokerException(ErrorCode.FABRICSDK_GETBLOCKINFO_ERROR);
        }
    }

    public List<TbBlock> queryBlockList(BigInteger blockNumber) throws BrokerException {

        try {
            return FabricSDKWrapper.queryBlockList(fabricConfig, channel, blockNumber);
        } catch (InvalidArgumentException | ProposalException e) {
            log.error("query block list by transHash and blockNum error:{}", e);
            throw new BrokerException(ErrorCode.FABRICSDK_GETBLOCKINFO_ERROR);
        }
    }

    public List<TbNode> queryNodeList() throws BrokerException {

        try {
            return FabricSDKWrapper.queryNodeList(fabricConfig, channel);
        } catch (InvalidArgumentException | ProposalException e) {
            log.error("query node list by transHash and blockNum error:{}", e);
            throw new BrokerException(ErrorCode.FABRICSDK_GETBLOCKINFO_ERROR);
        }
    }
}
