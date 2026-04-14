package com.kingpivot.service.impl;

import cn.hutool.http.HtmlUtil;
import com.kingpivot.api.dto.memberOrder.*;
import com.kingpivot.mapper.MemberOrderMapper;
import com.kingpivot.model.MemberOrder;
import com.kingpivot.service.MemberOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
@Slf4j
@Service(value = "memberOrderService")
public class MemberOrderServiceImpl implements MemberOrderService {
    @Resource
    private MemberOrderMapper memberOrderMapper;

    /** 微信退款回调 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String wechatRefundCallback(HttpServletRequest request, HttpServletResponse response) {

        log.info("log"+"=====================>微信回调开始啦!!!!");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream) request.getInputStream()));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            //sb为微信返回的xml
//            Map resultMap = XStreamUtil.deSerizalizeFromXml(sb.toString(),);
//            if ("SUCCESS".equals(resultMap.get("return_code"))){//通信成功
//                String req_info = String.valueOf(resultMap.get("req_info"));
//                String resultXml = AESUtil.decryptData(req_info,weChatConfig.key);
//                Map<String,Object> reqInfoMap = PayUtil.doXMLParse(resultXml);
//                //code....
//                String resultStr = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
//                return resultStr;
//            }
        }catch (Exception e){
            log.error("log",e);
        }
        throw new RuntimeException("微信回调失败");
    }

    @Override
    public MemberOrder findById(String id) {
        return memberOrderMapper.findById(id);
    }

//    @Override
//    public void update(Map<String, Object> param) {
//        memberOrderMapper.update(param);
//    }

    @Override
    public void update(Map<String, Object> param) {
        if (param != null && param.containsKey("id")) {
            String orderId = (String) param.get("id");
            if (checkIsValidId(orderId)) {
                memberOrderMapper.update(param);
            }
        }
    }

    @Override
    public void updateMemberOrderByMemberPaymentID(Map<String, Object> param) {
        memberOrderMapper.updateMemberOrderByMemberPaymentID(param);
    }

    @Override
    public List<MemberOrder> getMemberOrderByMemberPaymentID(String memberPaymentID) {
        return memberOrderMapper.getMemberOrderByMemberPaymentID(memberPaymentID);
    }

    @Override
    public List<MemberOrder> getMemberOrderListByMap(Map<String, Object> map) {
        return memberOrderMapper.getMemberOrderListByMap(map);
    }

    @Override
    public Timestamp getPayTime(String id) {
        return memberOrderMapper.getPayTime(id);
    }

    @Override
    public void insertMemberOrder(Map<String, Object> map) {
        memberOrderMapper.insertMemberOrder(map);
    }

    @Override
    public void insertMemberIncome(Map<String, Object> map) {
        memberOrderMapper.insertMemberIncome(map);
    }

    @Override
    public boolean checkIsValidId(String id) {
        if (StringUtils.isNotBlank(memberOrderMapper.getIsValidIdById(id))) {
            return true;
        }
        return false;
    }

    @Override
    public MemberOrderDto getMemberOrderByMap(Map<String, Object> map) {
        return memberOrderMapper.getMemberOrderByMap(map);
    }

    @Override
    public MemberOrderDto getMemberOrderById(String id) {
        return memberOrderMapper.getMemberOrderById(id);
    }

    @Override
    public List<MemberOrderListDto> getListByMap(Map<String, Object> map) {
        return memberOrderMapper.getListByMap(map);
    }

    @Override
    public List<MemberOrderRecommendDto> getMemberOrderRecommendList(Map<String, Object> map) {
        return memberOrderMapper.getMemberOrderRecommendList(map);
    }

    @Override
    public void insertMemberExpenditure(Map<String, Object> map) {
        memberOrderMapper.insertMemberExpenditure(map);
    }

    @Override
    public void save(MemberOrder memberOrder) {
        memberOrderMapper.save(memberOrder);
    }

    @Override
    public String getMemberOrderByCode(String orderCode) {
        return memberOrderMapper.getMemberOrderByCode(orderCode);
    }

    @Override
    public Integer getOutMemberDetail(String outMemberOrderID) {
        return memberOrderMapper.getOutMemberDetail(outMemberOrderID);
    }

    @Override
    public Double getRealPayByOutMemberID(String outMemberOrderID) {
        return memberOrderMapper.getRealPayByOutMemberID(outMemberOrderID);
    }

    @Override
    public List outSystemGetMemberOrderStatistics(Map<String, Object> param) {
//        String returnFields = (String) param.get("returnFields");
//        returnFields = HtmlUtil.filter(returnFields);
//        param.put("returnFields",returnFields);
//        return memberOrderMapper.outSystemGetMemberOrderStatistics(param);
        return null;
    }

    @Override
    public List outSystemGetMemberOrderGoodsStatistics(Map<String, Object> param) {
        String groupFieldName = (String) param.get("groupFieldName");
        String returnFields = (String) param.get("returnFields");
        returnFields = HtmlUtil.filter(returnFields);
        groupFieldName = HtmlUtil.filter(groupFieldName);
        param.put("returnFields",returnFields);
        param.put("groupFieldName",groupFieldName);
//        return memberOrderMapper.outSystemGetMemberOrderGoodsStatistics(param);
        return null;
    }

    @Override
    public ApiMemberOrderOut selectMemberOrderByOut(String tianmaoCode) {
        return memberOrderMapper.selectMemberOrderByOut(tianmaoCode);
    }

    @Override
    public Integer getShopOrderTimes(Map<String, Object> map) {
        return memberOrderMapper.getShopOrderTimes(map);
    }

    @Override
    public Double getShopPaymentTotal(Map<String, Object> map) {
        return memberOrderMapper.getShopPaymentTotal(map);
    }

    @Override
    public Double getShopPaymentSumTotal(Map<String, Object> map) {
        return memberOrderMapper.getShopPaymentSumTotal(map);
    }

    @Override
    public List<ApiMemberOrderDto> getMemberOrderListByMember(Map<String, Object> map) {
        return memberOrderMapper.getMemberOrderListByMember(map);
    }

    @Override
    public Double getMemberConsumptionAmount(Map<String, Object> map) {
        return memberOrderMapper.getMemberConsumptionAmount(map);
    }

    @Override
    public void updateMemberOrderAddress(ModelMap map) {
        memberOrderMapper.updateMemberOrderAddress(map);
    }

    @Override
    public MemberOrderStatistics getMemberOrderStatistics(String id) {
        return memberOrderMapper.getMemberOrderStatistics(id);
    }

    @Override
    public Integer getMemberPaidCount(Map<String, Object> map) {
        return memberOrderMapper.getMemberPaidCount(map);
    }

    @Override
    public Integer getMemberDeliveredCount(Map<String, Object> map) {
        return memberOrderMapper.getMemberDeliveredCount(map);
    }

    @Override
    public Integer getMemberNewMemberOrderCount(Map<String, Object> map) {
        return memberOrderMapper.getMemberNewMemberOrderCount(map);
    }

    @Override
    public Integer outSystemUpdateMemberOrder(Map<String, Object> map) {
        return memberOrderMapper.outSystemUpdateMemberOrder(map);
    }

    @Override
    public List<MultiMemberOrderDto> getMultiMemberOrderDtoListByMap(Map<String, Object> map) {
        return memberOrderMapper.getMultiMemberOrderDtoListByMap(map);
    }

    @Override
    public List<OutSystemMemberOrderList> outSystemGetMemberOrderList(Map<String, Object> map) {
        return memberOrderMapper.outSystemGetMemberOrderList(map);
    }

    @Override
    public List<OutSystemMemberOrderAndGoodsList> outSystemGetMemberOrderAndGoodsList(ModelMap map) {
        return memberOrderMapper.outSystemGetMemberOrderAndGoodsList(map);
    }

    @Override
    public List<ApiMemberOrderList> getListByAppID(String applicationID) {
        return memberOrderMapper.getListByAppID(applicationID);
    }

    @Override
    public void insertByMap(Map<String,Object> map) {
        memberOrderMapper.inserts(map);
    }

    @Override
    public void updateByMap(Map<String,Object> map) {
        memberOrderMapper.updates(map);
    }

    @Override
    public ApiMemberOrderDetail getOneMemberOrderDetailByID(String memberOrderID) {
        return memberOrderMapper.getOneMemberOrderDetailByID(memberOrderID);
    }

    @Override
    public ApiMemberOrderDetail getMemberOrderByOrderCode(String orderCode) {
        return memberOrderMapper.getMemberOrderByOrderCode(orderCode);
    }
    @Override
    public void insertOrderReturn(Map<String, Object> map) {
        memberOrderMapper.insertOrderReturn(map);
    }

}
