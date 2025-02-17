package main
import(
	"fmt"
	"strconv"
	"encoding/json"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

type TopicController struct {
    fmt.Println("<< ====[TopicController] declare struct ====== >>")
}

type TopicInfo struct {
	CreatedTimestamp string `json:"createdTimestamp"`
	Version  string `json:"version"`
	Topic string `json:"topicName"`
}

type ListTopicName struct {
	Total int `json:"total"`
	Size  string `json:"pageSize"`
	TopicList []string `json:"pageData"`
}

var topicContractName string
var topicContractVersion string
var topicMap = make(map[string]TopicInfo)
var topicIndex = make([]string,0)
const TOPIC_ALREADY_EXIST string = "100100"

func (t *TopicController) Init(stub shim.ChaincodeStubInterface) pb.Response{
    fmt.Println("<< ====[TopicController Init] success init it is view in docker ====== >>")
    return shim.Success([]byte("success init"))
}

func (t *TopicController) Invoke(stub shim.ChaincodeStubInterface) pb.Response{
    fn, args := stub.GetFunctionAndParameters()
	switch fn {
		case "addTopicContractName":
			return t.addTopicContractName(stub, args)
		case "updateTopicContractName":
			return t.updateTopicContractName(stub, args)
		case "getTopicContractName":
			return t.getTopicContractName(stub, args)
		case "getTopicContractVersion":
		    return t.getTopicContractVersion(stub,args)
		case "addTopicInfo":
			return t.addTopicInfo(stub, args)
		case "getTopicInfo":
			return t.getTopicInfo(stub, args)
		case "isTopicExist":
        	return t.isTopicExist(stub, args)
        case "listTopicName":
            return t.listTopicName(stub, args)
	}

	return shim.Error("invoke func error")
}

func (t *TopicController) addTopicContractName(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
	if(topicContractName == "" && topicContractVersion == ""){
		topicContractName=args[0]
		topicContractVersion=args[1]
		return shim.Success([]byte("setTopicName success"))
	}
	return shim.Error("topicContractName exist")
}

func (t *TopicController) updateTopicContractName(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
	topicContractName=args[0]
	topicContractVersion=args[1]
	return shim.Success([]byte("updateTopicContractName success"))
}

func (t *TopicController) getTopicContractName(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
	return shim.Success([]byte(topicContractName))
}

func (t *TopicController) getTopicContractVersion(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
	return shim.Success([]byte(topicContractVersion))
}

func (t *TopicController) addTopicInfo(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
    fmt.Println("<< ====[TopicController] addTopicInfo topic: ====== >>", args[0])
	if _, ok := topicMap[args[0]]; ok {
        return shim.Error(TOPIC_ALREADY_EXIST)
    }
	var topicInfo TopicInfo
	topicInfo.Topic = args[0]
	topicInfo.CreatedTimestamp = args[1]
	topicInfo.Version = args[2]
	topicIndex = append(topicIndex,args[0])
	topicMap[args[0]] = topicInfo;//args[0]:topicName args[1]:timestamp args[2]:
    return shim.Success([]byte("addTopicInfo success"))
}

func (t *TopicController) getTopicInfo(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
	jsonTopicInfo, err := json.Marshal(topicMap[args[0]])
	if err != nil{
		return shim.Error("getTopicInfo err")
	}
	return shim.Success([]byte(jsonTopicInfo))
}

func (t *TopicController) isTopicExist(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
	if _, ok := topicMap[args[0]]; ok {
        return shim.Success([]byte("topic exist"))
    } else {
        return shim.Error("topic not exist")
    }
}

// page index start from 0, pageSize default 10
func (t *TopicController) listTopicName(stub shim.ChaincodeStubInterface,args[] string) pb.Response{
    pageIndex,err := strconv.Atoi(args[0])
    if err !=nil {
        return shim.Error("listTopicName err")
    }

    pageSize,err := strconv.Atoi(args[1])
    if err !=nil {
        return shim.Error("listTopicName err")
    }

    var topicList = make([]string, 0)
    var size int
    if (pageSize <= 0 || pageSize > 100) {
        pageSize = 10
    }
    var total = len(topicIndex)
    var idx = pageIndex * pageSize

    if (len(topicIndex) <= idx) {
        size = 0
    }else{
        if (len(topicIndex) >= idx + pageSize){
            size = pageSize
        } else {
            size = len(topicIndex) - idx
        }
        for i := 0; i < pageSize; i++ {
            if (idx >= len(topicIndex)) {
                break
            }
            topicList = append(topicList,topicIndex[i])
            idx++
        }
    }
    var listTopicName ListTopicName
    listTopicName.Total = total
    listTopicName.Size = strconv.Itoa(size)
    listTopicName.TopicList = topicList

    listTopicNameJson, err := json.Marshal(listTopicName)
    if err != nil{
        return shim.Error("listTopicName err")
    }
    return shim.Success([]byte(listTopicNameJson))
}

func main(){
    err := shim.Start(new(TopicController))
    if err != nil{
        fmt.Println("Error starting Simple chaincode : %s",err)
    }
}