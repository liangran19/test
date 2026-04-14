package com.kingpivot.model;

import java.sql.Timestamp;

public class MemberOrder {
    private String id;//主键
    private String siteID;//站点ID
    private String applicationID;
    private String goldenGetID;
    private String sceneID;
    private String currencyDefineID;
    private String name;
    private Double priceAfterDiscount = 0.0d;//优惠后总价格
    private String memberPaymentID;//会员支付id
    private String paywayID;//支付机构ID
    private Double payTotal = 0.0d;//实际付款金额
    private Double goldenTotal = 0.0d;//实际付款金额
    private Double subPrePay = 0.0d;
    private Timestamp payTime;//付款时间
    private String paySequence;//付款流水号
    private Integer payFrom=1;//付款方式 默认第三方支付
    private Integer returnType=1;//退款类型 默认申请退款
    private Integer sendStatus=0;//发货状态  默认未发货
    private Integer outPayType=1;//外部支付方式
    private Integer payStatus;
    private String cardDefineID;
    private String memberPrivilegeID;
    private String actionID;
    private String memberActionID;
    private String shopID;//销售店铺ID
    private String CompanyID;//公司ID
    private Double priceTotal = 0.0d;//商品总价格
    private Double bonusAmount = 0.0d;
    private String memberID;//会员id
    private String orderCode;//订单编号
    private int status = 1;//订单状态
    private Integer goodsQTY = 0;//商品数量
    private Integer buyType;
    private String goodsSecondDefineID;
    private String goodsSecondID;
    private String distributorCompanyID;//渠道公司ID
    private String recommendMemberID;
    private String memberBuyReadID;
    private Double pointSubAmount=0.0d;
    private Double discountAmount=0.0d;
    private Double tax=0.0d;
    private Double sendPrice=0.0d;
    private Double serviceFee=0.0d;
    private Double weighTotal=0d;
    private Double weightTotalFee=0d;
    private String memberMajorID;
    private String memberTrainID;
    private String buyCompanyID;
    private String memberAddressID;
    private String buyShopID;
    private String useMemberID;
    private String invoiceID;
    private String contactName;
    private String phone;
    private String tianmaoCode;
    private String trainBusinessID;
    private String trainCourseID;
    private Timestamp applyTime;
    private Integer goodsNumbers = 0;//商品种类数
    private Integer sendType;//发货方式
    private Integer orderType = 1;
    private Integer isMemberHide;
    private Double weighPrice=0d;
    private Double rebateSubAmount=0d;
    private Double adjustAmount=0d;
    private Double taxPromotion=0d;
//    结算总价格
    private Double closedPriceTotal;

    public String getCurrencyDefineID() {
        return currencyDefineID;
    }

    public void setCurrencyDefineID(String currencyDefineID) {
        this.currencyDefineID = currencyDefineID;
    }

    public Double getAdjustAmount() {
        return adjustAmount;
    }

    public void setAdjustAmount(Double adjustAmount) {
        this.adjustAmount = adjustAmount;
    }

    public Double getTaxPromotion() {
        return taxPromotion;
    }

    public void setTaxPromotion(Double taxPromotion) {
        this.taxPromotion = taxPromotion;
    }

    public Double getWeighPrice() {
        return weighPrice;
    }

    public void setWeighPrice(Double weighPrice) {
        this.weighPrice = weighPrice;
    }

    public Double getRebateSubAmount() {
        return rebateSubAmount;
    }

    public void setRebateSubAmount(Double rebateSubAmount) {
        this.rebateSubAmount = rebateSubAmount;
    }

    public Double getClosedPriceTotal() {
        return closedPriceTotal;
    }

    public Integer getBuyType() {
        return buyType;
    }

    public void setBuyType(Integer buyType) {
        this.buyType = buyType;
    }

    public Integer getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(Integer payStatus) {
        this.payStatus = payStatus;
    }

    public String getSceneID() {
        return sceneID;
    }

    public void setSceneID(String sceneID) {
        this.sceneID = sceneID;
    }

    public String getMemberPrivilegeID() {
        return memberPrivilegeID;
    }

    public void setMemberPrivilegeID(String memberPrivilegeID) {
        this.memberPrivilegeID = memberPrivilegeID;
    }

    public Double getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(Double bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public String getInvoiceID() {
        return invoiceID;
    }

    public void setInvoiceID(String invoiceID) {
        this.invoiceID = invoiceID;
    }

    public Double getSubPrePay() {
        return subPrePay;
    }

    public void setSubPrePay(Double subPrePay) {
        this.subPrePay = subPrePay;
    }

    public String getMemberAddressID() {
        return memberAddressID;
    }

    public void setMemberAddressID(String memberAddressID) {
        this.memberAddressID = memberAddressID;
    }

    public Double getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(Double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setClosedPriceTotal(Double closedPriceTotal) {
        this.closedPriceTotal = closedPriceTotal;
    }

    public Double getSendPrice() {
        return sendPrice;
    }

    public void setSendPrice(Double sendPrice) {
        this.sendPrice = sendPrice;
    }

    public Double getWeighTotal() {
        if(weighTotal==null){
            return 0d;
        }
        return weighTotal;
    }

    public void setWeighTotal(Double weighTotal) {
        this.weighTotal = weighTotal;
    }

    public Double getWeightTotalFee() {
        if(weightTotalFee==null){
            return 0d;
        }
        return weightTotalFee;
    }

    public void setWeightTotalFee(Double weightTotalFee) {
        this.weightTotalFee = weightTotalFee;
    }

    public Double getPointSubAmount() {
        return pointSubAmount;
    }

    public void setPointSubAmount(Double pointSubAmount) {
        this.pointSubAmount = pointSubAmount;
    }

    public String getMemberMajorID() {
        return memberMajorID;
    }

    public void setMemberMajorID(String memberMajorID) {
        this.memberMajorID = memberMajorID;
    }

    public String getMemberTrainID() {
        return memberTrainID;
    }

    public void setMemberTrainID(String memberTrainID) {
        this.memberTrainID = memberTrainID;
    }

    public String getMemberBuyReadID() {
        return memberBuyReadID;
    }

    public void setMemberBuyReadID(String memberBuyReadID) {
        this.memberBuyReadID = memberBuyReadID;
    }

    public String getGoldenGetID() {
        return goldenGetID;
    }

    public void setGoldenGetID(String goldenGetID) {
        this.goldenGetID = goldenGetID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSiteID() {
        return siteID;
    }

    public void setSiteID(String siteID) {
        this.siteID = siteID;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPriceAfterDiscount() {
        return priceAfterDiscount;
    }

    public void setPriceAfterDiscount(Double priceAfterDiscount) {
        this.priceAfterDiscount = priceAfterDiscount;
    }

    public String getMemberPaymentID() {
        return memberPaymentID;
    }

    public void setMemberPaymentID(String memberPaymentID) {
        this.memberPaymentID = memberPaymentID;
    }

    public String getGoodsSecondDefineID() {
        return goodsSecondDefineID;
    }

    public void setGoodsSecondDefineID(String goodsSecondDefineID) {
        this.goodsSecondDefineID = goodsSecondDefineID;
    }

    public String getGoodsSecondID() {
        return goodsSecondID;
    }

    public void setGoodsSecondID(String goodsSecondID) {
        this.goodsSecondID = goodsSecondID;
    }

    public String getPaywayID() {
        return paywayID;
    }

    public void setPaywayID(String paywayID) {
        this.paywayID = paywayID;
    }

    public Double getPayTotal() {
        return payTotal;
    }

    public void setPayTotal(Double payTotal) {
        this.payTotal = payTotal;
    }

    public Timestamp getPayTime() {
        return payTime;
    }

    public void setPayTime(Timestamp payTime) {
        this.payTime = payTime;
    }

    public String getPaySequence() {
        return paySequence;
    }

    public void setPaySequence(String paySequence) {
        this.paySequence = paySequence;
    }

    public Integer getPayFrom() {
        return payFrom;
    }

    public void setPayFrom(Integer payFrom) {
        this.payFrom = payFrom;
    }

    public String getCardDefineID() {
        return cardDefineID;
    }

    public void setCardDefineID(String cardDefineID) {
        this.cardDefineID = cardDefineID;
    }

    public String getActionID() {
        return actionID;
    }

    public void setActionID(String actionID) {
        this.actionID = actionID;
    }

    public String getShopID() {
        return shopID;
    }

    public void setShopID(String shopID) {
        this.shopID = shopID;
    }

    public String getCompanyID() {
        return CompanyID;
    }

    public void setCompanyID(String companyID) {
        CompanyID = companyID;
    }

    public String getBuyShopID() {
        return buyShopID;
    }

    public void setBuyShopID(String buyShopID) {
        this.buyShopID = buyShopID;
    }

    public Double getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(Double priceTotal) {
        this.priceTotal = priceTotal;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMemberID() {
        return memberID;
    }

    public void setMemberID(String memberID) {
        this.memberID = memberID;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getGoodsQTY() {
        return goodsQTY;
    }

    public void setGoodsQTY(Integer goodsQTY) {
        this.goodsQTY = goodsQTY;
    }

    public String getDistributorCompanyID() {
        return distributorCompanyID;
    }

    public void setDistributorCompanyID(String distributorCompanyID) {
        this.distributorCompanyID = distributorCompanyID;
    }

    public String getRecommendMemberID() {
        return recommendMemberID;
    }

    public void setRecommendMemberID(String recommendMemberID) {
        this.recommendMemberID = recommendMemberID;
    }

    public String getBuyCompanyID() {
        return buyCompanyID;
    }

    public void setBuyCompanyID(String buyCompanyID) {
        this.buyCompanyID = buyCompanyID;
    }

    public String getUseMemberID() {
        return useMemberID;
    }

    public void setUseMemberID(String useMemberID) {
        this.useMemberID = useMemberID;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTianmaoCode() {
        return tianmaoCode;
    }

    public void setTianmaoCode(String tianmaoCode) {
        this.tianmaoCode = tianmaoCode;
    }

    public String getTrainBusinessID() {
        return trainBusinessID;
    }

    public void setTrainBusinessID(String trainBusinessID) {
        this.trainBusinessID = trainBusinessID;
    }

    public String getTrainCourseID() {
        return trainCourseID;
    }

    public void setTrainCourseID(String trainCourseID) {
        this.trainCourseID = trainCourseID;
    }

    public Timestamp getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Timestamp applyTime) {
        this.applyTime = applyTime;
    }

    public Integer getGoodsNumbers() {
        return goodsNumbers;
    }

    public void setGoodsNumbers(Integer goodsNumbers) {
        this.goodsNumbers = goodsNumbers;
    }

    public Integer getSendType() {
        return sendType;
    }

    public void setSendType(Integer sendType) {
        this.sendType = sendType;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public Double getGoldenTotal() {
        return goldenTotal;
    }

    public void setGoldenTotal(Double goldenTotal) {
        this.goldenTotal = goldenTotal;
    }

    public Integer getIsMemberHide() {
        return isMemberHide;
    }

    public void setIsMemberHide(Integer isMemberHide) {
        this.isMemberHide = isMemberHide;
    }

    public String getMemberActionID() {
        return memberActionID;
    }

    public void setMemberActionID(String memberActionID) {
        this.memberActionID = memberActionID;
    }

    public Integer getOutPayType() {
        return outPayType;
    }

    public void setOutPayType(Integer outPayType) {
        this.outPayType = outPayType;
    }


    public Integer getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(Integer sendStatus) {
        this.sendStatus = sendStatus;
    }

    public Integer getReturnType() {
        return returnType;
    }

    public void setReturnType(Integer returnType) {
        this.returnType = returnType;
    }

}
