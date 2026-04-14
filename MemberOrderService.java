package com.kingpivot.service;

import com.kingpivot.api.dto.memberOrder.*;
import com.kingpivot.api.dto.memberOrder.ApiMemberOrderDetail;
import com.kingpivot.model.MemberOrder;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface MemberOrderService {
    String wechatRefundCallback(HttpServletRequest request, HttpServletResponse response);
    MemberOrder findById(String id);

    void insertOrderReturn(Map<String, Object> map);

    void update(Map<String, Object> param);

    void updateMemberOrderByMemberPaymentID(Map<String, Object> param);

    List<MemberOrder> getMemberOrderByMemberPaymentID(String memberPaymentID);

    List<MemberOrder> getMemberOrderListByMap(Map<String, Object> map);

    Timestamp getPayTime(String id);

    void insertMemberOrder(Map<String, Object> map);

    void insertMemberIncome(Map<String, Object> map);

    boolean checkIsValidId(String id);

    MemberOrderDto getMemberOrderById(String id);

    List<MemberOrderListDto> getListByMap(Map<String, Object> map);

    void insertMemberExpenditure(Map<String,Object> map);

    void save(MemberOrder memberOrder);

    String getMemberOrderByCode(String orderCode);

    Integer getOutMemberDetail(String outMemberOrderID);

    Double getRealPayByOutMemberID(String outMemberOrderID);

    List outSystemGetMemberOrderStatistics(Map<String, Object> param);

    List outSystemGetMemberOrderGoodsStatistics(Map<String, Object> param);

    ApiMemberOrderOut selectMemberOrderByOut(String tianmaoCode);

    Integer getShopOrderTimes(Map<String, Object> map);

    Double getShopPaymentTotal(Map<String, Object> map);

    Double getShopPaymentSumTotal(Map<String, Object> map);

    List<ApiMemberOrderDto> getMemberOrderListByMember(Map<String, Object> map);

    Double getMemberConsumptionAmount(Map<String, Object> map);

    void updateMemberOrderAddress(ModelMap map);

    MemberOrderStatistics getMemberOrderStatistics(String id);

    List<MemberOrderRecommendDto> getMemberOrderRecommendList(Map<String, Object> map);

    Integer getMemberPaidCount(Map<String, Object> map);//会员待发货数量统计 商品是实体商品的

    Integer getMemberDeliveredCount(Map<String, Object> map);

    Integer getMemberNewMemberOrderCount(Map<String, Object> map);

    Integer outSystemUpdateMemberOrder(Map<String, Object> map);

    List<MultiMemberOrderDto> getMultiMemberOrderDtoListByMap(Map<String, Object> map);

    MemberOrderDto getMemberOrderByMap(Map<String, Object> map);

    List<OutSystemMemberOrderList> outSystemGetMemberOrderList(Map<String, Object> map);

    /**
     * 获取会员订单和会员订单商品列表
     * @param map
     * @return
     */
    List<OutSystemMemberOrderAndGoodsList> outSystemGetMemberOrderAndGoodsList(ModelMap map);

    List<ApiMemberOrderList> getListByAppID(String applicationID);

    void insertByMap(Map<String,Object> map);

    void updateByMap(Map<String,Object> map);

    ApiMemberOrderDetail getOneMemberOrderDetailByID(String memberOrderID);

    ApiMemberOrderDetail getMemberOrderByOrderCode(String orderCode);
}
