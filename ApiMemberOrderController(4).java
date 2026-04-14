package com.kingpivot.api.controller.ApiMemberOrderController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.kingpivot.api.controller.weixin.RefundInfo;
import com.kingpivot.api.controller.weixin.RefundReturnInfo;
import com.kingpivot.api.controller.weixin.TransferUtils;
import com.kingpivot.api.controller.weixin.WechatPayUtils;
import com.kingpivot.api.dto.appointment.Appointment;
import com.kingpivot.api.dto.badgeDefine.BadgeDefineDto;
import com.kingpivot.api.dto.bonus.BonusDto;
import com.kingpivot.api.dto.cartGoods.ApiCartGoodsListDto;
import com.kingpivot.api.dto.cartGoods.CartGoods;
import com.kingpivot.api.dto.companyBank.ApiCompanyBankDetailDto;
import com.kingpivot.api.dto.computerUnit.OutSystemComputerUnitList;
import com.kingpivot.api.dto.goodsCompany.GoodsCompanyDto;
import com.kingpivot.api.dto.goodsPrice.GoodsPriceDto;
import com.kingpivot.api.dto.goodsSecond.GoodsSecondDto;
import com.kingpivot.api.dto.memberBonus.MemberBonusList;
import com.kingpivot.api.dto.memberBuyRead.MemberBuyRead;
import com.kingpivot.api.dto.memberBuyRead.MemberBuyReadDetailDto;
import com.kingpivot.api.dto.memberMajor.MyMemberMajorListDto;
import com.kingpivot.api.dto.memberOrder.*;
import com.kingpivot.api.dto.memberOrderGoods.MemberOrderGoodsDtoList;
import com.kingpivot.api.dto.memberOrderGoods.MemberOrderGoodsList;
import com.kingpivot.api.dto.memberOrderGoods.NewMemberOrderGoodsList;
import com.kingpivot.api.dto.memberPayment.OutMemberPaymentDto;
import com.kingpivot.api.dto.memberPrivilege.ApiMemberPrivilegeDto;
import com.kingpivot.api.dto.memberStatistics.MemberStatisticsDto;
import com.kingpivot.api.dto.model.ApiModelListDto;
import com.kingpivot.api.dto.orderTemplateGoods.OrderTemplateGoodsDto;
import com.kingpivot.api.dto.presentDefine.ApiPresentDefineDetailDto;
import com.kingpivot.api.dto.presentRecord.ApiPresentRecordDetailDto;
import com.kingpivot.api.dto.shop.ApiShopDetailDto;
import com.kingpivot.api.dto.shop.OutShopDto;
import com.kingpivot.api.protocol.MessageHeader;
import com.kingpivot.api.protocol.MessagePacket;
import com.kingpivot.api.protocol.MessagePage;
import com.kingpivot.base.config.Config;
import com.kingpivot.base.config.RedisKey;
import com.kingpivot.base.member.model.Member;
import com.kingpivot.base.memberlog.model.Memberlog;
import com.kingpivot.base.support.MemberLogDTO;
import com.kingpivot.common.KingBase;
import com.kingpivot.common.jms.SendMessageService;
import com.kingpivot.common.jms.dto.memberBalance.MemberBalanceRequestBase;
import com.kingpivot.common.model.BaseParameter;
import com.kingpivot.common.service.BaseService;
import com.kingpivot.common.utils.*;
import com.kingpivot.model.*;
import com.kingpivot.service.*;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.kingpivot.base.memberlog.model.NewMemberlog;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping(value = "/api")
public class ApiMemberOrderController {
    public String apiV3key = "";

    @ApiOperation(value = "submitOneMemberOrder", notes = "创建一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/submitOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket submitOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//                Member member = memberService.selectById(sessionID);

        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = memberService.selectById(member.getId());
        map.put("applicationID", member.getApplicationID());
        map.put("memberID", member.getId());
        map.put("creator", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("siteID", memberLogDTO.getSiteId());
        String companyID = request.getParameter("companyID");
        if (StringUtils.isEmpty(companyID)) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不能为空");
        }
        if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
            return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "companyID不正确");
        }
        map.put("companyID", companyID);
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            if (!cityService.checkIdIsValid(cityID)) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }
        String buyCompanyID = request.getParameter("buyCompanyID");
        if (StringUtils.isBlank(buyCompanyID)) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "buyCompanyID不能为空");
        }
        if (StringUtils.isBlank(companyService.getIsValidIdBy(buyCompanyID))) {
            return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "buyCompanyID不正确");
        }
        map.put("buyCompanyID", buyCompanyID);
        Company company = companyService.selectById(buyCompanyID);
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(distributorCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        } else if (company != null && StringUtils.isNotBlank(company.getDistributorCompanyID())) {
            map.put("distributorCompanyID", company.getDistributorCompanyID());
        }

        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        } else if (company != null && StringUtils.isNotBlank(company.getRecommendMemberID())) {
            map.put("recommendMemberID", company.getRecommendMemberID());
        }
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String salesEmployeeID = request.getParameter("salesEmployeeID");
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!employeeService.checkEmployeeID(salesEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        String orderTemplateID = request.getParameter("orderTemplateID");
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!orderTemplateGoodsService.checkIsValidId(orderTemplateID)) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIsNull, "orderTemplateID不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        String buyShopID = request.getParameter("buyShopID");
        if (StringUtils.isNotBlank(buyShopID)) {
            if (!shopService.checkIdIsValid(buyShopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "buyShopID不正确");
            }
            map.put("buyShopID", buyShopID);
        }
        String buyEmployeeID = request.getParameter("buyEmployeeID");
        if (StringUtils.isNotBlank(buyEmployeeID)) {
            if (!employeeService.checkEmployeeID(buyEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "buyEmployeeID不正确");
            }
            map.put("buyEmployeeID", buyEmployeeID);
        }
        String useEmployeeID = request.getParameter("useEmployeeID");
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!employeeService.checkEmployeeID(useEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "useEmployeeID不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        String useMemberID = request.getParameter("useMemberID");
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!memberService.checkMemberId(useMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "useMemberID不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        String name = request.getParameter("name");
        if (StringUtils.isEmpty(name)) name = member.getName() + TimeTest.getTimeStr();
        map.put("name", name);
        String orderType = request.getParameter("orderType");
        if (StringUtils.isBlank(orderType)) {
            return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "orderType不能为空");
        }
        if (!NumberUtils.isNumeric(orderType)) {
            return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "orderType请输入数字");
        }
        map.put("orderType", Integer.valueOf(orderType));
        String priceTotal = request.getParameter("priceTotal");
        if (StringUtils.isBlank(priceTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "priceTotal不能为空");
        }
        if (!NumberUtils.isNumeric(priceTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "priceTotal请输入数字");
        }
        Double a = Double.valueOf(priceTotal);
        map.put("priceTotal", a);
        Double b = 0d;
        String sendPrice = request.getParameter("sendPrice");
        if (StringUtils.isNotBlank(sendPrice)) {
            if (!NumberUtils.isNumeric(sendPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "sendPrice请输入数字");
            }
            b = Double.valueOf(sendPrice);
        }
        map.put("sendPrice", b);
        Double priceAfterDiscount = NumberUtils.add(a, b);
        b = 0d;
        String subPrePay = request.getParameter("subPrePay");
        if (StringUtils.isNotBlank(subPrePay)) {
            if (!NumberUtils.isNumeric(subPrePay)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "subPrePay请输入数字");
            }
            b = Double.valueOf(subPrePay);
        }
        map.put("subPrePay", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        String pointUsed = request.getParameter("pointUsed");
        if (StringUtils.isNotBlank(pointUsed)) {
            if (!NumberUtils.isNumeric(pointUsed)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "pointUsed请输入数字");
            }
            map.put("pointUsed", Integer.valueOf(pointUsed));
        } else {
            map.put("pointUsed", 0);
        }
        String isMemberHide = request.getParameter("isMemberHide");
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide请输入数字");
            }
            map.put("isMemberHide", Integer.valueOf(isMemberHide));
        } else {
            map.put("isMemberHide", 0);
        }
        Double c = 0d;
        String pointSubAmount = request.getParameter("pointSubAmount");
        if (StringUtils.isNotBlank(pointSubAmount)) {
            if (!NumberUtils.isNumeric(pointSubAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "pointSubAmount请输入数字");
            }
            c = Double.valueOf(pointSubAmount);
        }
        map.put("pointSubAmount", c);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, c);
        b = 0d;
        String rebateSubAmount = request.getParameter("rebateSubAmount");
        if (StringUtils.isNotBlank(rebateSubAmount)) {
            if (!NumberUtils.isNumeric(rebateSubAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "rebateSubAmount请输入数字");
            }
            b = Double.valueOf(rebateSubAmount);
        }
        map.put("rebateSubAmount", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        b = 0d;
        String bonusAmount = request.getParameter("bonusAmount");
        if (StringUtils.isNotBlank(bonusAmount)) {
            if (!NumberUtils.isNumeric(bonusAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "bonusAmount请输入数字");
            }
            b = Double.valueOf(bonusAmount);
        }
        map.put("bonusAmount", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        String memberBonusIDs = request.getParameter("memberBonusIDs");
        b = 0d;
        String discountAmount = request.getParameter("discountAmount");
        if (StringUtils.isNotBlank(discountAmount)) {
            if (!NumberUtils.isNumeric(discountAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "discountAmount请输入数字");
            }
            b = Double.valueOf(discountAmount);
        }
        map.put("discountAmount", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        map.put("priceAfterDiscount", priceAfterDiscount);
        String tax = request.getParameter("tax");
        if (StringUtils.isBlank(tax)) tax = "0";
        map.put("tax", Double.valueOf(tax));
        String serviceFee = request.getParameter("serviceFee");
        if (StringUtils.isBlank(serviceFee)) serviceFee = "0";
        map.put("serviceFee", Double.valueOf(serviceFee));
        String payTotal = request.getParameter("payTotal");
        if (StringUtils.isBlank(payTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "payTotal不能为空");
        }
        if (!NumberUtils.isNumeric(payTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "payTotal请输入数字");
        }
        map.put("payTotal", Double.valueOf(payTotal));
        String closedPriceTotal = request.getParameter("closedPriceTotal");
        if (StringUtils.isNotBlank(closedPriceTotal)) {
            if (!NumberUtils.isNumeric(closedPriceTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "closedPriceTotal请输入数字");
            }
            map.put("closedPriceTotal", Double.valueOf(closedPriceTotal));
        }
        String incomeTotal = request.getParameter("incomeTotal");
        if (StringUtils.isNotBlank(incomeTotal)) {
            if (!NumberUtils.isNumeric(incomeTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "incomeTotal请输入数字");
            }
            map.put("incomeTotal", Double.valueOf(incomeTotal));
        }
        String pointTotal = request.getParameter("pointTotal");
        if (StringUtils.isNotBlank(pointTotal)) {
            if (!NumberUtils.isNumeric(pointTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "pointTotal请输入数字");
            }
            map.put("pointTotal", Integer.valueOf(pointTotal));
        } else {
            map.put("pointTotal", 0);
        }
        String rebateTotal = request.getParameter("rebateTotal");
        if (StringUtils.isNotBlank(rebateTotal)) {
            if (!NumberUtils.isNumeric(rebateTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "rebateTotal请输入数字");
            }
            map.put("rebateTotal", Double.valueOf(rebateTotal));
        } else {
            map.put("rebateTotal", 0d);
        }
        String goldenTotal = request.getParameter("goldenTotal");
        if (StringUtils.isNotBlank(goldenTotal)) {
            if (!NumberUtils.isNumeric(goldenTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "goldenTotal请输入数字");
            }
            map.put("goldenTotal", Integer.valueOf(goldenTotal));
        } else {
            map.put("goldenTotal", 0);
        }
        String beanTotal = request.getParameter("beanTotal");
        if (StringUtils.isNotBlank(beanTotal)) {
            if (!NumberUtils.isNumeric(beanTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "beanTotal请输入数字");
            }
            map.put("beanTotal", Integer.valueOf(beanTotal));
        } else {
            map.put("beanTotal", 0);
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isBlank(payFrom)) {
            return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom不能为空");
        }
        if (!NumberUtils.isNumeric(payFrom)) {
            return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom请输入数字");
        }
        map.put("payFrom", Integer.valueOf(payFrom));
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDIsError, "payWayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isBlank(sendType)) {
            return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendType不能为空");
        }
        if (!NumberUtils.isNumeric(sendType)) {
            return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendType请输入数字");
        }
        map.put("sendType", Integer.valueOf(sendType));
        String sendUrgencyType = request.getParameter("sendUrgencyType");
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendUrgencyType请输入数字");
            }
            map.put("sendUrgencyType", Integer.valueOf(sendUrgencyType));
        }
        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
        }

        String memberAddressID = request.getParameter("memberAddressID");
        String contactName = request.getParameter("contactName");
        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(memberAddressID)) {
            MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
            if (memberAddress == null || StringUtils.isBlank(memberAddress.getId())) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
            if (StringUtils.isBlank(contactName)) contactName = memberAddress.getContactName();
            if (StringUtils.isBlank(phone)) phone = memberAddress.getPhone();
        }
        if (StringUtils.isNotBlank(contactName)) map.put("contactName", contactName);
        if (StringUtils.isNotBlank(phone)) map.put("phone", phone);
        String reservedDate = request.getParameter("reservedDate");
        if (StringUtils.isNotBlank(reservedDate)) {
            if (!TimeTest.dateIsPass(reservedDate)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "reservedDate请输入yyyy-MM-dd的格式");
            }
            map.put("reservedDate", TimeTest.strToTimes(reservedDate));
        }
        String memberMemo = request.getParameter("memberMemo");
        if (StringUtils.isNotBlank(memberMemo)) map.put("memberMemo", memberMemo);
        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!memberInvoiceDefineService.checkIsValidId(memberInvoiceDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }
        String buyType = request.getParameter("buyType");
        if (StringUtils.isNotBlank(buyType)) {
            map.put("buyType", buyType);
        }
        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            map.put("outPayType", outPayType);
        }
        String actionGoodsType = request.getParameter("actionGoodsType");
        if (StringUtils.isNotBlank(actionGoodsType)) {
            map.put("actionGoodsType", actionGoodsType);
        }
        String financeType = request.getParameter("financeType");
        if (StringUtils.isNotBlank(financeType)) {
            map.put("financeType", financeType);
        }

        String memberTrainID = request.getParameter("memberTrainID");
        if (StringUtils.isNotBlank(memberTrainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberTrain", memberTrainID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberTrainID不正确");
            }
            map.put("memberTrainID", memberTrainID);
        }

        String cartID = request.getParameter("cartID");
        String id = IdGenerator.uuid32();
        Timestamp time = TimeTest.getTime();
        map.put("id", id);
        map.put("orderCode", sequenceDefineService.genCode("orderSeq", id));
        map.put("orderSequnce", "DD" + TimeTest.toString(time));
        map.put("applyTime", time);
        map.put("status", 1);
        map.put("payStatus", 0);
        map.put("sendStatus", 0);
        map.put("isMemberHide", 0);
        memberOrderService.insertMemberOrder(map);
        map.clear();
        map.put("id", id);
        Map<String, Object> pMap = new HashMap<>();
        Double weighTotal = 0d;
        Integer goodsQTY = 0;
        Integer goodsNumbers = 0;
        if (StringUtils.isNotBlank(orderTemplateID)) {
            pMap.put("orderTemplateID", orderTemplateID);
            pMap.put("sortTypeOrder", "1");
            List<OrderTemplateGoodsDto> list = orderTemplateGoodsService.getListByMap(pMap);
            if (list != null && list.size() > 0) {
                a = 0d;
                Integer q = 0;
                for (OrderTemplateGoodsDto o : list) {
                    pMap.clear();
                    pMap.put("id", IdGenerator.uuid32());
                    pMap.put("memberOrderID", id);
                    pMap.put("companyID", companyID);
                    if (StringUtils.isNotBlank(o.getGoodsID())) pMap.put("goodsID", o.getGoodsID());
                    if (StringUtils.isNotBlank(o.getGoodsCompanyID()))
                        pMap.put("goodsCompanyID", o.getGoodsCompanyID());
                    if (StringUtils.isNotBlank(o.getGoodsShopID())) pMap.put("goodsShopID", o.getGoodsShopID());
                    if (StringUtils.isNotBlank(shopID)) pMap.put("shopID", shopID);
                    pMap.put("name", o.getName());
                    pMap.put("priceStand", o.getPriceStand());
                    pMap.put("priceNow", o.getPriceNow());//1
                    q = o.getBuyNumber();
                    goodsQTY = +q;
                    pMap.put("QTY", q);
                    pMap.put("QTYWeighed", q);
                    pMap.put("salesUnitQTY", q);
                    a = NumberUtils.mul(Double.valueOf(q), o.getStandardWeigh());
                    pMap.put("weighTotal", a);
                    pMap.put("priceTotal", NumberUtils.mul(Double.valueOf(q), o.getStandardPrice()));
                    weighTotal += NumberUtils.add(weighTotal, a);
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID1()))
                        pMap.put("objectFeatureItemID1", o.getObjectFeatureItemID1());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID2()))
                        pMap.put("objectFeatureItemID2", o.getObjectFeatureItemID2());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID3()))
                        pMap.put("objectFeatureItemID3", o.getObjectFeatureItemID3());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID4()))
                        pMap.put("objectFeatureItemID4", o.getObjectFeatureItemID4());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID5()))
                        pMap.put("objectFeatureItemID5", o.getObjectFeatureItemID5());
                    pMap.put("status", 1);
                    pMap.put("creator", member.getId());
                    memberOrderGoodsService.insertGoodsByMap(pMap);
                    goodsNumbers++;
                }
            }
            pMap.clear();
        }
        //判断是否有使用积分
        if (StringUtils.isNotBlank(pointUsed)) {
            boolean fals = kingBase.addPointGet(member, "订单支付使用积分抵扣", -Integer.valueOf(pointUsed), Config.MEMBERORDER_OBJECTDEFINEID,
                    id, name, null, 2);
            if (!fals) {
                priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, c);
                map.put("pointSubAmount", 0d);
            }
        }
        //返利处理
        if (StringUtils.isNotBlank(rebateSubAmount)) {
            boolean fals = kingBase.addRebate(member, "订单支付使用返利抵扣", -Double.valueOf(rebateSubAmount), Config.MEMBERORDER_OBJECTDEFINEID,
                    id, name, 99);
            if (!fals) {
                priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, Double.valueOf(rebateSubAmount));
                map.put("rebateSubAmount", 0d);
            }
        }
        map.put("priceAfterDiscount", priceAfterDiscount);
        //判断有没有使用红包
        if (StringUtils.isNotBlank(bonusAmount) && StringUtils.isNotBlank(memberBonusIDs)) {
            if (memberBonusIDs.indexOf(',') != -1) {
                String[] strs = memberBonusIDs.split(",");
                if (strs != null && strs.length > 0) {
                    for (int i = 0; i < strs.length; i++) {
                        pMap.clear();
                        pMap.put("id", strs[i]);
                        MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(pMap);
                        if (memberBonus == null || StringUtils.isBlank(memberBonus.getId())) continue;
                        memberBonus.setMemberOrderID(id);
                        memberBonus.setUseTime(time);
                        memberBonusService.update(memberBonus);
                    }
                }
            } else {
                pMap.clear();
                pMap.put("id", memberBonusIDs);
                MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(pMap);
                if (memberBonus != null && StringUtils.isNotBlank(memberBonus.getId())) {
                    memberBonus.setMemberOrderID(id);
                    memberBonus.setUseTime(time);
                    memberBonusService.update(memberBonus);
                }
            }
            pMap.clear();
        }
        if (StringUtils.isNotBlank(cartID)) {
            pMap.clear();
            pMap.put("cartID", cartID);
            List<CartGoods> list = cartGoodsService.getListByCartID(cartID);
            if (list != null && list.size() > 0) {
                a = 0d;
                Integer q = 0;
                for (CartGoods o : list) {
                    pMap.clear();
                    pMap.put("id", IdGenerator.uuid32());
                    pMap.put("memberOrderID", id);
                    pMap.put("companyID", companyID);
                    if (StringUtils.isNotBlank(o.getGoodsID())) pMap.put("goodsID", o.getGoodsID());
                    if (StringUtils.isNotBlank(o.getGoodsCompanyID()))
                        pMap.put("goodsCompanyID", o.getGoodsCompanyID());
                    if (StringUtils.isNotBlank(o.getGoodsShopID())) pMap.put("goodsShopID", o.getGoodsShopID());
                    if (StringUtils.isNotBlank(shopID)) pMap.put("shopID", shopID);
                    if (StringUtils.isNotBlank(o.getCartID())) pMap.put("cartID", o.getCartID());
                    if (StringUtils.isNotBlank(o.getIDCode())) pMap.put("IDCode", o.getIDCode());
                    pMap.put("name", o.getName());
                    pMap.put("priceStand", o.getPriceStand());
                    pMap.put("priceNow", o.getPriceNow());//2
                    q = o.getQty();
//                    goodsQTY = +q;
                    goodsQTY = BigDecimal.valueOf(goodsQTY)
                            .add(BigDecimal.valueOf(q)).intValue();
                    pMap.put("QTY", q);
                    pMap.put("QTYWeighed", q);
                    pMap.put("salesUnitQTY", o.getSalesUnitQTY());
                    a = NumberUtils.mul(Double.valueOf(q), o.getStandardWeigh());
                    pMap.put("weighTotal", a);
                    pMap.put("priceTotal", o.getPriceTotal());
                    weighTotal += NumberUtils.add(weighTotal, a);
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID1()))
                        pMap.put("objectFeatureItemID1", o.getObjectFeatureItemID1());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID2()))
                        pMap.put("objectFeatureItemID2", o.getObjectFeatureItemID2());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID3()))
                        pMap.put("objectFeatureItemID3", o.getObjectFeatureItemID3());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID4()))
                        pMap.put("objectFeatureItemID4", o.getObjectFeatureItemID4());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID5()))
                        pMap.put("objectFeatureItemID5", o.getObjectFeatureItemID5());
                    if (o.getDiscountRate() != null) pMap.put("discountRate", o.getDiscountRate());
                    pMap.put("status", 1);
                    pMap.put("creator", member.getId());
                    memberOrderGoodsService.insertGoodsByMap(pMap);
                    goodsNumbers++;
                }
            }
            pMap.clear();
        }
        map.put("weighTotal", weighTotal);
        map.put("weightTotalFee", weighTotal);
        map.put("goodsNumbers", goodsNumbers);
        map.put("goodsQTY", goodsQTY);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("创建一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.submitOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberOrderID", id);
        return MessagePacket.newSuccess(rsMap, "submitOneMemberOrder success");
    }

    @ApiOperation(value = "updateMemberOrderAddress", notes = "修改一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String salesEmployeeID = request.getParameter("salesEmployeeID");
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!employeeService.checkEmployeeID(salesEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        String useEmployeeID = request.getParameter("useEmployeeID");
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!employeeService.checkEmployeeID(useEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "useEmployeeID不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        String useMemberID = request.getParameter("useMemberID");
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!memberService.checkMemberId(useMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "useMemberID不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        String memberAddressName = request.getParameter("memberAddressName");
        if (StringUtils.isNotBlank(memberAddressName)) {
            map.put("memberAddressName", memberAddressName);
        }

        String name = request.getParameter("name");
        if (StringUtils.isEmpty(name)) name = member.getName() + TimeTest.getTimeStr();
        map.put("name", name);

        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "orderType请输入数字");
            }
            map.put("orderType", Integer.valueOf(orderType));
        }

        Double a = 0.0d;
        String priceTotal = request.getParameter("priceTotal");
        if (StringUtils.isNotBlank(priceTotal)) {
            if (!NumberUtils.isNumeric(priceTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "priceTotal请输入数字");
            }
            a = Double.valueOf(priceTotal);
            map.put("priceTotal", a);
        }
        String isMemberHide = request.getParameter("isMemberHide");
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide请输入数字");
            }
            map.put("isMemberHide", Integer.valueOf(isMemberHide));
        }
        Double b = 0d;
        String sendPrice = request.getParameter("sendPrice");
        if (StringUtils.isNotBlank(sendPrice)) {
            if (!NumberUtils.isNumeric(sendPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "sendPrice请输入数字");
            }
            b = Double.valueOf(sendPrice);
        }
        map.put("sendPrice", b);
        Double priceAfterDiscount = NumberUtils.add(a, b);
        b = 0d;
        String subPrePay = request.getParameter("subPrePay");
        if (StringUtils.isNotBlank(subPrePay)) {
            if (!NumberUtils.isNumeric(subPrePay)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "subPrePay请输入数字");
            }
            b = Double.valueOf(subPrePay);
        }
        map.put("subPrePay", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        String pointUsed = request.getParameter("pointUsed");
        if (StringUtils.isNotBlank(pointUsed)) {
            if (!NumberUtils.isNumeric(pointUsed)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "pointUsed请输入数字");
            }
            map.put("pointUsed", Integer.valueOf(pointUsed));
        } else {
            map.put("pointUsed", 0);
        }
        Double c = 0d;
        String pointSubAmount = request.getParameter("pointSubAmount");
        if (StringUtils.isNotBlank(pointSubAmount)) {
            if (!NumberUtils.isNumeric(pointSubAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "pointSubAmount请输入数字");
            }
            c = Double.valueOf(pointSubAmount);
        }
        map.put("pointSubAmount", c);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, c);
        b = 0d;
        String rebateSubAmount = request.getParameter("rebateSubAmount");
        if (StringUtils.isNotBlank(rebateSubAmount)) {
            if (!NumberUtils.isNumeric(rebateSubAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "rebateSubAmount请输入数字");
            }
            b = Double.valueOf(rebateSubAmount);
        }
        map.put("rebateSubAmount", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);

        b = 0d;
        String bonusAmount = request.getParameter("bonusAmount");
        if (StringUtils.isNotBlank(bonusAmount)) {
            if (!NumberUtils.isNumeric(bonusAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "bonusAmount请输入数字");
            }
            b = Double.valueOf(bonusAmount);
        }
        map.put("bonusAmount", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        String memberBonusIDs = request.getParameter("memberBonusIDs");
        b = 0d;
        String discountAmount = request.getParameter("discountAmount");
        if (StringUtils.isNotBlank(discountAmount)) {
            if (!NumberUtils.isNumeric(discountAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "discountAmount请输入数字");
            }
            b = Double.valueOf(discountAmount);
        }
        map.put("discountAmount", b);
        priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, b);
        map.put("priceAfterDiscount", priceAfterDiscount);
        String tax = request.getParameter("tax");
        if (StringUtils.isBlank(tax)) tax = "0";
        map.put("tax", Double.valueOf(tax));
        String serviceFee = request.getParameter("serviceFee");
        if (StringUtils.isBlank(serviceFee)) serviceFee = "0";
        map.put("serviceFee", Double.valueOf(serviceFee));
        String payTotal = request.getParameter("payTotal");
        if (StringUtils.isBlank(payTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "payTotal请输入数字");
        }
        if (!NumberUtils.isNumeric(payTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "payTotal请输入数字");
        }
        map.put("payTotal", Double.valueOf(payTotal));
        String closedPriceTotal = request.getParameter("closedPriceTotal");
        if (StringUtils.isNotBlank(closedPriceTotal)) {
            if (!NumberUtils.isNumeric(closedPriceTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "closedPriceTotal请输入数字");
            }
            map.put("closedPriceTotal", Double.valueOf(closedPriceTotal));
        }
        String incomeTotal = request.getParameter("incomeTotal");
        if (StringUtils.isNotBlank(incomeTotal)) {
            if (!NumberUtils.isNumeric(incomeTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "incomeTotal请输入数字");
            }
            map.put("incomeTotal", Double.valueOf(incomeTotal));
        }
        String pointTotal = request.getParameter("pointTotal");
        if (StringUtils.isNotBlank(pointTotal)) {
            if (!NumberUtils.isNumeric(pointTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "pointTotal请输入数字");
            }
            map.put("pointTotal", Integer.valueOf(pointTotal));
        } else {
            map.put("pointTotal", 0);
        }
        String rebateTotal = request.getParameter("rebateTotal");
        if (StringUtils.isNotBlank(rebateTotal)) {
            if (!NumberUtils.isNumeric(rebateTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "rebateTotal请输入数字");
            }
            map.put("rebateTotal", Double.valueOf(rebateTotal));
        } else {
            map.put("rebateTotal", 0d);
        }
        String goldenTotal = request.getParameter("goldenTotal");
        if (StringUtils.isNotBlank(goldenTotal)) {
            if (!NumberUtils.isNumeric(goldenTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "goldenTotal请输入数字");
            }
            map.put("goldenTotal", Double.valueOf(goldenTotal));
        } else {
            map.put("goldenTotal", 0);
        }
        String beanTotal = request.getParameter("beanTotal");
        if (StringUtils.isNotBlank(beanTotal)) {
            if (!NumberUtils.isNumeric(beanTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "beanTotal请输入数字");
            }
            map.put("beanTotal", Integer.valueOf(beanTotal));
        } else {
            map.put("beanTotal", 0);
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isBlank(orderType)) {
            return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom请输入数字");
        }
        if (!NumberUtils.isNumeric(orderType)) {
            return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom请输入数字");
        }
        map.put("payFrom", Integer.valueOf(payFrom));
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDIsError, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isBlank(sendType)) {
            return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendType请输入数字");
        }
        if (!NumberUtils.isNumeric(sendType)) {
            return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendType请输入数字");
        }
        map.put("sendType", Integer.valueOf(sendType));
        String sendUrgencyType = request.getParameter("sendUrgencyType");
        if (StringUtils.isBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendUrgencyType请输入数字");
            }
            map.put("sendUrgencyType", Integer.valueOf(sendUrgencyType));
        }
        String memberAddressID = request.getParameter("memberAddressID");
        String contactName = request.getParameter("contactName");
        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(memberAddressID)) {
            MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
            if (memberAddress == null || StringUtils.isBlank(memberAddress.getId())) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
            if (StringUtils.isBlank(contactName)) contactName = memberAddress.getContactName();
            if (StringUtils.isBlank(phone)) phone = memberAddress.getPhone();
        }
        if (StringUtils.isNotBlank(contactName)) map.put("contactName", contactName);
        if (StringUtils.isNotBlank(phone)) map.put("phone", phone);
        String reservedDate = request.getParameter("reservedDate");
        if (StringUtils.isNotBlank(reservedDate)) {
            if (!TimeTest.dateIsPass(reservedDate)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "reservedDate请输入yyyy-MM-dd的格式");
            }
            map.put("reservedDate", TimeTest.strToTimes(reservedDate));
        }
        String memberMemo = request.getParameter("memberMemo");
        if (StringUtils.isNotBlank(memberMemo)) map.put("memberMemo", memberMemo);
        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!memberInvoiceDefineService.checkIsValidId(memberInvoiceDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }
        String cartID = request.getParameter("cartID");
        Timestamp time = TimeTest.getTime();
        map.clear();
        map.put("id", memberOrderID);
        Map<String, Object> pMap = new HashMap<>();
        Double weighTotal = 0d;
        Integer goodsQTY = 0;
        Integer goodsNumbers = 0;
        //判断是否有使用积分
        if (StringUtils.isNotBlank(pointUsed)) {
            pointService.deletePoint(memberOrderID);
            boolean fals = kingBase.addPointGet(member, "订单支付使用积分抵扣", -Integer.valueOf(pointUsed), Config.MEMBERORDER_OBJECTDEFINEID, memberOrderID, name, null, 2);
            if (!fals) {
                priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, c);
                map.put("pointSubAmount", 0d);
            }
        }
        //返利处理
        if (StringUtils.isNotBlank(rebateSubAmount)) {
            memberRebateService.deleteRebateByObjectID(memberOrderID);
            boolean fals = kingBase.addRebate(member, "订单支付使用返利抵扣", -Double.valueOf(rebateSubAmount), Config.MEMBERORDER_OBJECTDEFINEID, memberOrderID, name, 99);
            if (!fals) {
                priceAfterDiscount = NumberUtils.subtract(priceAfterDiscount, Double.valueOf(rebateSubAmount));
                map.put("rebateSubAmount", 0d);
            }
        }
        map.put("priceAfterDiscount", priceAfterDiscount);
        //判断有没有使用红包
        if (StringUtils.isNotBlank(bonusAmount) && StringUtils.isNotBlank(memberBonusIDs)) {
            if (memberBonusIDs.indexOf(',') != -1) {
                String[] strs = memberBonusIDs.split(",");
                if (strs != null && strs.length > 0) {
                    for (int i = 0; i < strs.length; i++) {
                        pMap.clear();
                        pMap.put("id", strs[i]);
                        MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(pMap);
                        if (memberBonus == null || StringUtils.isBlank(memberBonus.getId())) continue;
                        if (memberBonus.getUseTime() != null) continue;
                        memberBonus.setMemberOrderID(memberOrderID);
                        memberBonus.setUseTime(time);
                        memberBonusService.update(memberBonus);
                    }
                }
            } else {
                pMap.clear();
                pMap.put("id", memberBonusIDs);
                MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(pMap);
                if (memberBonus != null && StringUtils.isNotBlank(memberBonus.getId()) && memberBonus.getUseTime() == null) {
                    memberBonus.setMemberOrderID(memberOrderID);
                    memberBonus.setUseTime(time);
                    memberBonusService.update(memberBonus);
                }
            }
            pMap.clear();
        }
        if (StringUtils.isNotBlank(cartID)) {
            memberOrderGoodsService.deleteGoods(memberOrderID);
            pMap.clear();
            pMap.put("cartID", cartID);
            List<CartGoods> list = cartGoodsService.getListByCartID(cartID);
            if (list != null && list.size() > 0) {
                a = 0d;
                Integer q = 0;
                for (CartGoods o : list) {
                    pMap.clear();
                    pMap.put("id", IdGenerator.uuid32());
                    pMap.put("memberOrderID", memberOrderID);
                    pMap.put("companyID", member.getCompanyID());
                    if (StringUtils.isNotBlank(o.getGoodsID())) pMap.put("goodsID", o.getGoodsID());
                    if (StringUtils.isNotBlank(o.getGoodsCompanyID()))
                        pMap.put("goodsCompanyID", o.getGoodsCompanyID());
                    if (StringUtils.isNotBlank(o.getGoodsShopID())) pMap.put("goodsShopID", o.getGoodsShopID());
                    if (StringUtils.isNotBlank(o.getCartID())) pMap.put("cartID", o.getCartID());
                    if (StringUtils.isNotBlank(o.getIDCode())) pMap.put("IDCode", o.getIDCode());
                    pMap.put("name", o.getName());
                    pMap.put("priceStand", o.getPriceStand());
                    pMap.put("priceNow", o.getPriceNow());//3
                    q = o.getQty();
                    goodsQTY = +q;
                    pMap.put("QTY", q);
                    pMap.put("QTYWeighed", q);
                    pMap.put("salesUnitQTY", o.getSalesUnitQTY());
                    a = NumberUtils.mul(Double.valueOf(q), o.getStandardWeigh());
                    pMap.put("weighTotal", a);
                    pMap.put("priceTotal", o.getPriceTotal());
                    weighTotal += NumberUtils.add(weighTotal, a);
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID1()))
                        pMap.put("objectFeatureItemID1", o.getObjectFeatureItemID1());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID2()))
                        pMap.put("objectFeatureItemID2", o.getObjectFeatureItemID2());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID3()))
                        pMap.put("objectFeatureItemID3", o.getObjectFeatureItemID3());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID4()))
                        pMap.put("objectFeatureItemID4", o.getObjectFeatureItemID4());
                    if (StringUtils.isNotBlank(o.getObjectFeatureItemID5()))
                        pMap.put("objectFeatureItemID5", o.getObjectFeatureItemID5());
                    if (o.getDiscountRate() != null) pMap.put("discountRate", o.getDiscountRate());
                    pMap.put("status", 1);
                    pMap.put("creator", member.getId());
                    memberOrderGoodsService.insertGoodsByMap(pMap);
                    goodsNumbers++;
                }
            }
            pMap.clear();
        }
        map.put("weighTotal", weighTotal);
        map.put("weightTotalFee", weighTotal);
        map.put("goodsNumbers", goodsNumbers);
        map.put("goodsQTY", goodsQTY);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("修改一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.updateOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateOneMemberOrder success");
    }

    @ApiOperation(value = "applyReturnOneMemberOrder", notes = "申请退货一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/applyReturnOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket applyReturnOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }

        map.put("id", memberOrderID);
        String returnCode = request.getParameter("returnCode");
        if (StringUtils.isNotBlank(returnCode)) map.put("returnCode", returnCode);
        map.put("applyReturnTime", TimeTest.getTime());
        map.put("status", 70);
        memberOrderService.update(map);

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder != null) {
            Map<String, Object> returnMap = Maps.newHashMap();

            String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
            returnMap.put("id", uuid);
            returnMap.put("creator", member.getId());

            returnMap.put("memberOrderID", memberOrder.getId());
            returnMap.put("applicationID", memberOrder.getApplicationID());
            returnMap.put("siteID", memberOrder.getSiteID());
            returnMap.put("companyID", memberOrder.getCompanyID());
            returnMap.put("shopID", memberOrder.getShopID());
            returnMap.put("memberID", memberOrder.getMemberID());
            returnMap.put("orderCode", memberOrder.getOrderCode());
            returnMap.put("payFrom", memberOrder.getPayFrom());
            returnMap.put("goodsQTY", memberOrder.getGoodsQTY());
            returnMap.put("goodsNumbers", memberOrder.getGoodsNumbers());

            String memberName = member.getName() != null ? member.getName() : "未知";
            returnMap.put("name", memberName + "申请退货");
            returnMap.put("status", 1);

            memberOrderService.insertOrderReturn(returnMap);
        }


        if (memberLogDTO != null) {
            String descriptions = String.format("申请退货一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.applyReturnOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("applyReturnTime", TimeTest.getTimeStr());

        return MessagePacket.newSuccess(rsMap, "applyReturnOneMemberOrder success");
    }

    @ApiOperation(value = "returnGoodsOneMemberOrder", notes = "申请退货一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/returnGoodsOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket returnGoodsOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);
        map.put("returnGoodsTime", TimeTest.getTime());
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("审批退货申请一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.returnGoodsOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("returnGoodsTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "returnGoodsOneMemberOrder success");
    }

    @ApiOperation(value = "cancelConfirmOneMemberOrder", notes = "审批取消一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/cancelConfirmOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket cancelConfirmOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("cancelConformMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        String cancelConformEmployeeID = request.getParameter("cancelConformEmployeeID");
        if (StringUtils.isNotBlank(cancelConformEmployeeID)) {
            if (!employeeService.checkEmployeeID(cancelConformEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "cancelConformEmployeeID不正确");
            }
            map.put("cancelConformEmployeeID", cancelConformEmployeeID);
        }
        map.put("id", memberOrderID);
        map.put("cancelConfirmTime", TimeTest.getTime());
        map.put("status", 13);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("审批取消一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.cancelConfirmOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("cancelConfirmTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "cancelConfirmOneMemberOrder success");
    }

    @ApiOperation(value = "cancelOneMemberOrder", notes = "撤销一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/cancelOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket cancelOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }

        String cancelType = request.getParameter("cancelType");
        if (StringUtils.isNotBlank(cancelType) && "1".equals(cancelType)) {
            map.put("isValid", 0);
        }

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null || memberOrder.getStatus() != 1) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "订单状态不对，撤销不了");
        }

        map.put("id", memberOrderID);
        map.put("cancelTime", TimeTest.getTime());
        map.put("status", 2);
        map.put("payStatus", 1);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("撤销一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.cancelOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("cancelTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "cancelOneMemberOrder success");
    }

    @ApiOperation(value = "renewOneMemberOrder", notes = "独立接口:恢复撤销一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/renewOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket renewOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        if (memberOrder.getStatus() == 1) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "订单状态已经是新的，无法修改");
        }
        map.put("id", memberOrderID);
        map.put("status", 1);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("恢复撤销一个会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.renewOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("modifiedTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "renewOneMemberOrder success");
    }

    @ApiOperation(value = "getGoodsOneMemberOrder", notes = "签收一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getGoodsOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getGoodsOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("getGoodsMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        String getGoodsEmployeeID = request.getParameter("getGoodsEmployeeID");
        if (StringUtils.isNotBlank(getGoodsEmployeeID)) {
            if (!employeeService.checkEmployeeID(getGoodsEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "getGoodsEmployeeID不正确");
            }
            map.put("getGoodsEmployeeID", getGoodsEmployeeID);
        }
        map.put("id", memberOrderID);
        map.put("getGoodsTime", TimeTest.getTime());
        map.put("status", 8);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("签收一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.getGoodsOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("getGoodsTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "getGoodsOneMemberOrder success");
    }

    @ApiOperation(value = "sendOneMemberOrder", notes = "签收一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/sendOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket sendOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("sendoutMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        String sendEmployeeID = request.getParameter("sendEmployeeID");
        if (StringUtils.isNotBlank(sendEmployeeID)) {
            if (!employeeService.checkEmployeeID(sendEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "sendEmployeeID不正确");
            }
            map.put("sendEmployeeID", sendEmployeeID);
        }
        String expressID = request.getParameter("expressID");
        if (StringUtils.isNotBlank(expressID)) {
            if (!expressService.checkIsValid(expressID)) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDFound, "expressID不正确");
            }
            map.put("expressID", expressID);
        }
        String sendCode = request.getParameter("sendCode");
        if (StringUtils.isNotBlank(sendCode)) map.put("sendCode", sendCode);
        map.put("id", memberOrderID);
        map.put("sendTime", TimeTest.getTime());
        map.put("status", 6);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("签收一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.sendOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sendTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "sendOneMemberOrder success");
    }

    @ApiOperation(value = "processOneMemberOrder", notes = "签收一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/processOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket processOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("processMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null || memberOrder.getStatus() != 4) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "订单状态不是付款订单，不能接单");
        }
        String processEmployeeID = request.getParameter("processEmployeeID");
        if (StringUtils.isNotBlank(processEmployeeID)) {
            if (!employeeService.checkEmployeeID(processEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "processEmployeeID不正确");
            }
            map.put("processEmployeeID", processEmployeeID);
        }
        map.put("id", memberOrderID);
        map.put("processTime", TimeTest.getTime());
        map.put("status", 10);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("签收一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.processOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("processTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "processOneMemberOrder success");
    }

    @ApiOperation(value = "companyPaymentOneMemberOrder", notes = "线下公司付款一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/companyPaymentOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket companyPaymentOneMemberOrder(HttpServletRequest request, ModelMap map, ModelMap pMap) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("payMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null || memberOrder.getStatus() != 1) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "订单状态不是新，不能付款");
        }
        pMap.put("applicatioID", memberOrder.getApplicationID());
        pMap.put("companyID", memberOrder.getBuyCompanyID());
        pMap.put("name", memberOrder.getName());
        pMap.put("amount", memberOrder.getPayTotal());
        pMap.put("fee", 0d);
        pMap.put("payStatus", 1);
        pMap.put("objectDefineID", Config.MEMBERORDER_OBJECTDEFINEID);
        pMap.put("objectID", memberOrderID);
        pMap.put("objectName", memberOrder.getName());
        pMap.put("objectCode", memberOrder.getOrderCode());
        pMap.put("memberID", member.getId());
        pMap.put("payMemberID", member.getId());
        pMap.put("status", 4);
        if (StringUtils.isNotBlank(memberOrder.getBuyShopID())) pMap.put("shopID", memberOrder.getBuyShopID());
        if (StringUtils.isNotBlank(memberOrder.getPaywayID())) pMap.put("paywayID", memberOrder.getPaywayID());
        pMap.put("workDate", memberOrder.getApplyTime());
        String companyPaywayID = request.getParameter("companyPaywayID");
        if (StringUtils.isNotBlank(companyPaywayID)) {
            if (!companyPaywayService.checkIsValid(companyPaywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyPaywayIDFound, "companyPaywayID不正确");
            }
            pMap.put("companyPaywayID", companyPaywayID);
        }
        String companyBankID = request.getParameter("companyBankID");
        if (StringUtils.isNotBlank(companyBankID)) {
            ApiCompanyBankDetailDto api = companyBankService.getCompanyBankDetail(companyBankID);
            if (api == null || StringUtils.isBlank(api.getCompanyBankID())) {
                return MessagePacket.newFail(MessageHeader.Code.companyBankIDNotFound, "companyBankID不正确");
            }
            pMap.put("companyBankID", companyBankID);
            pMap.put("bankID", api.getChildBankID());
            pMap.put("bankName", api.getChildBankName());
            pMap.put("accountName", api.getName());
            pMap.put("account", api.getAccount());
        }
        String otherCompanyPaywayID = request.getParameter("otherCompanyPaywayID");
        if (StringUtils.isNotBlank(otherCompanyPaywayID)) {
            if (!companyPaywayService.checkIsValid(otherCompanyPaywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyPaywayIDFound, "otherCompanyPaywayID不正确");
            }
            pMap.put("otherCompanyPaywayID", otherCompanyPaywayID);
        }
        String otherCompanyBankID = request.getParameter("otherCompanyBankID");
        if (StringUtils.isNotBlank(otherCompanyBankID)) {
            ApiCompanyBankDetailDto api = companyBankService.getCompanyBankDetail(otherCompanyBankID);
            if (api == null || StringUtils.isBlank(api.getCompanyBankID())) {
                return MessagePacket.newFail(MessageHeader.Code.companyBankIDNotFound, "otherCompanyBankID不正确");
            }
            pMap.put("otherCompanyBankID", otherCompanyBankID);
            pMap.put("otherBankID", api.getChildBankID());
            pMap.put("otherBankName", api.getChildBankName());
            pMap.put("otherAccountName", api.getName());
            pMap.put("otherAccount", api.getAccount());
        }
        String payEmployeeID = request.getParameter("payEmployeeID");
        if (StringUtils.isNotBlank(payEmployeeID)) {
            if (!employeeService.checkEmployeeID(payEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "payEmployeeID不正确");
            }
            map.put("payEmployeeID", payEmployeeID);
            pMap.put("employeeID", payEmployeeID);
            pMap.put("payEmployeeID", payEmployeeID);
        }
        String paySequence = request.getParameter("paySequence");
        if (StringUtils.isNotBlank(paySequence)) {
            map.put("paySequence", paySequence);
            pMap.put("paySeq", paySequence);
        }
        String id = IdGenerator.uuid32();
        map.put("id", memberOrderID);
        pMap.put("payDoneTime", TimeTest.getTime());
        String payTime = request.getParameter("payTime");
        if (StringUtils.isNotBlank(payTime)) {
            map.put("payTime", payTime);
        }
        if (StringUtils.isBlank(payTime)) {
            map.put("payTime", TimeTest.getTime());
        }
        map.put("companyPaymentID", id);
        map.put("payFrom", 21);
        map.put("status", 40);
        memberOrderService.update(map);
        companyPaymentService.submitOneCompanypayment(pMap);
        if (memberLogDTO != null) {
            String descriptions = String.format("线下公司付款一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.companyPaymentOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("companyPaymentID", id);
        return MessagePacket.newSuccess(rsMap, "companyPaymentOneMemberOrder success");
    }

    @ApiOperation(value = "confirmPayOneMemberOrder", notes = "线下公司付款一个企业会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/confirmPayOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket confirmPayOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("confirmPayMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        String confirmPayEmployeeID = request.getParameter("confirmPayEmployeeID");
        if (StringUtils.isNotBlank(confirmPayEmployeeID)) {
            if (!employeeService.checkEmployeeID(confirmPayEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "confirmPayEmployeeID不正确");
            }
            map.put("confirmPayEmployeeID", confirmPayEmployeeID);
        }
        map.put("id", memberOrderID);
        map.put("confirmPayTime", TimeTest.getTime());
        map.put("status", 4);
        memberOrderService.update(map);
        if (memberLogDTO != null) {
            String descriptions = String.format("确认收款一个企业会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.confirmPayOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("confirmPayTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "confirmPayOneMemberOrder success");
    }

    @ApiOperation(value = "getOneMemberOrderDetail", notes = "获取一个企业会员订单详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getOneMemberOrderDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOneMemberOrderDetail(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("confirmPayMemberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        MemberOrderDto memberOrderDto = memberOrderService.getMemberOrderById(memberOrderID);
        if (memberOrderDto == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (memberLogDTO != null) {
            String descriptions = String.format("获取一个企业会员订单详细信息");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.getOneMemberOrderDetail);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", memberOrderDto);
        return MessagePacket.newSuccess(rsMap, "getOneMemberOrderDetail success");
    }

    @ApiOperation(value = "getMemberOrderList", notes = "获取企业会员订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberOrderList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberOrderList(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//         Member member = memberService.selectById(sessionID);

        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        if (StringUtils.isNotBlank(member.getApplicationID())) map.put("applicationID", member.getApplicationID());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            if (!cityService.checkIdIsValid(cityID)) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(distributorCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.customerCompanyIDIsError, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            Member member1 = memberService.selectByRecommendCode(recommendCode);
            if (member1 != null) {
                map.put("recommendMemberID", member1.getId());
            }
        }
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String salesEmployeeID = request.getParameter("salesEmployeeID");
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!employeeService.checkEmployeeID(salesEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null && channel.getId() == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotNull, "channelID不正确");
            }
            map.put("channelID", channelID);
        }
        String orderTemplateID = request.getParameter("orderTemplateID");
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!orderTemplateGoodsService.checkIsValidId(orderTemplateID)) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIsNull, "orderTemplateID不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        String buyCompanyID = request.getParameter("buyCompanyID");
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(buyCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "buyCompanyID不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }
        String buyShopID = request.getParameter("buyShopID");
        if (StringUtils.isNotBlank(buyShopID)) {
            if (!shopService.checkIdIsValid(buyShopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "buyShopID不正确");
            }
            map.put("buyShopID", buyShopID);
        }
        String buyEmployeeID = request.getParameter("buyEmployeeID");
        if (StringUtils.isNotBlank(buyEmployeeID)) {
            if (!employeeService.checkEmployeeID(buyEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "buyEmployeeID不正确");
            }
            map.put("buyEmployeeID", buyEmployeeID);
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
            }
            map.put("memberID", memberID);
        }
        String useEmployeeID = request.getParameter("useEmployeeID");
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!employeeService.checkEmployeeID(useEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "useEmployeeID不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        String useMemberID = request.getParameter("useMemberID");
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!memberService.checkMemberId(useMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "useMemberID不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "orderType请输入数字");
            }
            map.put("orderType", Integer.valueOf(orderType));
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom请输入数字");
            }
            map.put("payFrom", Integer.valueOf(payFrom));
        }
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDIsError, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            if (!NumberUtils.isNumeric(sendType)) {
                return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendType请输入数字");
            }
            map.put("sendType", Integer.valueOf(sendType));
        }
        String sendUrgencyType = request.getParameter("sendUrgencyType");
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendUrgencyType请输入数字");
            }
            map.put("sendUrgencyType", Integer.valueOf(sendUrgencyType));
        }
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
        }
        String status = request.getParameter("status");
        if (StringUtils.isNotBlank(status)) {
            if (!NumberUtils.isNumeric(status)) {
                return MessagePacket.newFail(MessageHeader.Code.statusIsError, "status请填写数字");
            }
            map.put("status", Integer.valueOf(status));
        }
        String isMemberHide = request.getParameter("isMemberHide");
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide请填写数字");
            }
            map.put("isMemberHide", Integer.valueOf(isMemberHide));
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) map.put("sortTypeName", sortTypeName);
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) map.put("sortTypeTime", sortTypeTime);
        String sortTypePrice = request.getParameter("sortTypePrice");
        if (StringUtils.isNotBlank(sortTypePrice)) map.put("sortTypePrice", sortTypePrice);
        String sortTypeStatus = request.getParameter("sortTypeStatus");
        if (StringUtils.isNotBlank(sortTypeStatus)) map.put("sortTypeStatus", sortTypeStatus);
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<MemberOrderListDto> list = memberOrderService.getListByMap(map);
        if (list != null && !list.isEmpty()) {
            PageInfo<MemberOrderListDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (StringUtils.isNotBlank(recommendCode)) {
            if (!list.isEmpty() && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    String recommendMemberID1 = list.get(i).getRecommendMemberID();
                    Double price = list.get(i).getPriceAfterDiscount();
                    // 找到商品
                    List<MemberOrderGoods> memberOrderGoodsList = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(list.get(i).getMemberOrderID());
                    if (!memberOrderGoodsList.isEmpty() && memberOrderGoodsList.size() > 0) {
                        String goodsShopID = memberOrderGoodsList.get(0).getGoodsShopID();
                        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                        list.get(i).setGoodsShopID(goodsShopID);
                        list.get(i).setGoodsShopName(goodsShop.getName());
                    }
                    if (StringUtils.isNotBlank(recommendMemberID1)) {
                        map.put("memberID", recommendMemberID1);
                        map.put("majorID", "8a2f462a78ecd2fa0178eedb3ed4057b");
                        List<MyMemberMajorListDto> memberMajorListDtos = memberMajorService.getMemberMajorList(map);
                        if (!memberMajorListDtos.isEmpty() && memberMajorListDtos.size() > 0) {

                            BigDecimal recommendPrice = BigDecimal.valueOf(price)
                                    .multiply(new BigDecimal("0.3"));
                            list.get(i).setRecommendPrice(recommendPrice.doubleValue());
                            //list.get(i).setRecommendPrice(price * 0.3);
                        } else {

                            BigDecimal recommendPrice = BigDecimal.valueOf(price)
                                    .multiply(new BigDecimal("0.1"));
                            list.get(i).setRecommendPrice(recommendPrice.doubleValue());
                           //list.get(i).setRecommendPrice(price * 0.1);
                        }
                    }
                }
            }
        }
        if (memberLogDTO != null) {
            String description = String.format("获取企业会员订单列表");
            sendMessageService.sendMemberLogMessage(sessionID, description, request,
                    Memberlog.MemberOperateType.getMemberOrderList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberOrderList success");
    }

    @ApiOperation(value = "getMemberOrderByCode", notes = "获取一个企业会员订单详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberOrderByCode", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberOrderByCode(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String orderCode = request.getParameter("orderCode");
        if (StringUtils.isEmpty(orderCode)) {
            return MessagePacket.newFail(MessageHeader.Code.orderCodeIsNotNull, "orderCode不能为空");
        }
        if (!StringUtils.isNumeric(orderCode)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "orderCode请输入数字");
        }
        map.put("orderCode", orderCode);
        String memberOrderID = memberOrderService.getMemberOrderByCode(orderCode);
        Map<String, Object> rsMap = Maps.newHashMap();
        if (memberOrderID != null) {
            String descriptions = String.format("根据订单编号查询24小时内订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.getMemberOrderByCode);
            rsMap.put("data", memberOrderID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderNotFound, "未查询到会员订单");
        }
        return MessagePacket.newSuccess(rsMap, "getMemberOrderByCode success");
    }

    @ApiOperation(value = "getOutMemberDetail", notes = "根据外部订单编号获取订单状态")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getOutMemberDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOutMemberDetail(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String outMemberOrderID = request.getParameter("outMemberOrderID");
        if (StringUtils.isEmpty(outMemberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.outMemberOrderIDNotNull, "outMemberOrderID不能为空");
        }
        Integer status = memberOrderService.getOutMemberDetail(outMemberOrderID);
        Map<String, Object> rsMap = Maps.newHashMap();
        if (status != null) {
            String descriptions = String.format("根据外部订单编号获取订单状态");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.getOutMemberDetail);
            rsMap.put("data", status);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderNotFound, "未查询到会员订单");
        }
        return MessagePacket.newSuccess(rsMap, "getOutMemberDetail success");
    }

    @ApiOperation(value = "getMemberOrderListByMember", notes = "独立接口：获取某某会员购买了某某商品")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberOrderListByMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberOrderListByMember(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isEmpty(sessionID) && StringUtils.isEmpty(applicationID)) {
            return MessagePacket.newFail(MessageHeader.Code.sessionIDAndApplicationIDNotNull, "sessionID和applicationID不能同时为空！");
        }
        MemberLogDTO memberLogDTO = null;
        Member member = null;
        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
            // member =memberService.selectById(sessionID);
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            map.put("applicationID", member.getApplicationID());
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        }
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            map.put("applicationID", applicationID);
        }
        String sceneID = request.getParameter("sceneID");
        if (StringUtils.isNotBlank(sceneID)) {
            if (!sceneService.checkIsValidId(sceneID)) {
                return MessagePacket.newFail(MessageHeader.Code.sceneIDNotFound, "sceneID不正确");
            }
            map.put("sceneID", sceneID);
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        List<ApiMemberOrderDto> list = memberOrderService.getMemberOrderListByMember(map);
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                String memberName = list.get(i).getMemberName();
                String goodsShopName = list.get(i).getGoodsShopName();
                if (StringUtils.isNotBlank(memberName) && StringUtils.isNotBlank(goodsShopName)) {
                    String name = memberName.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
                    list.get(i).setName(name + "刚刚购买了" + goodsShopName);
                }
            }
            PageInfo<ApiMemberOrderDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (memberLogDTO != null) {
            String description = String.format("获取某某会员购买了某某商品");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberOrderList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        return MessagePacket.newSuccess(rsMap, "getMemberOrderListByMember success");
    }

    @ApiOperation(value = "getMemberConsumptionAmount", notes = "独立接口：获取会员总消费金额")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberConsumptionAmount", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberConsumptionAmount(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        MemberLogDTO memberLogDTO = null;
        Member member = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("applicationID", member.getApplicationID());
        map.put("memberID", member.getId());
        memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) {
            if (!TimeTest.isTimeType(beginTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeIsError, "beginTime请输入yyyy-MM-dd HH:mm:ss格式");
            }
            map.put("beginTime", beginTime);
        }

        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) {
            if (!TimeTest.isTimeType(endTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeIsError, "endTime请输入yyyy-MM-dd HH:mm:ss格式");
            }
            map.put("endTime", endTime);
        }
        Double consumptionAmount = memberOrderService.getMemberConsumptionAmount(map);
        if (memberLogDTO != null) {
            String description = String.format("获取会员消费金额");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberConsumptionAmount);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", consumptionAmount);
        return MessagePacket.newSuccess(rsMap, "getMemberConsumptionAmount success");
    }

    @ApiOperation(value = "updateMemberOrderAddress", notes = "独立接口：编辑一个会员订单的地址")
    @RequestMapping(value = "/updateMemberOrderAddress", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberOrderAddress(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderNotFound, "memberOrderID不正确");
        }
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isEmpty(memberAddressID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsNull, "memberAddressID不能为空");
        }
        MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
        if (memberAddress == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
        }
        map.put("id", memberOrderID);
        map.put("memberAddressID", memberAddressID);
        memberOrderService.updateMemberOrderAddress(map);
        String description1 = String.format("编辑一个会员订单的地址");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.updateMemberOrderAddress);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberOrderAddress success");
    }

    @ApiOperation(value = "getMemberOrderStatistics", notes = "独立接口：获取会员订单各种状态订单数")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberOrderStatistics", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberOrderStatistics(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("memberID", member.getId());
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            map.put("orderType", orderType);
        }
        //获取 待付款和
        MemberOrderStatistics dto = new MemberOrderStatistics();
        Integer newMemberOrderCount = memberOrderService.getMemberNewMemberOrderCount(map);
        if (newMemberOrderCount != null) {
            dto.setNewMemberOrderCount(newMemberOrderCount);
        } else {
            dto.setNewMemberOrderCount(0);
        }
        //待发货
        Integer paidCount = memberOrderService.getMemberPaidCount(map);

        if (paidCount != null) {
            dto.setPaidCount(paidCount);
        } else {
            dto.setPaidCount(0);
        }
        //待收货
        Integer deliveredCount = memberOrderService.getMemberDeliveredCount(map);
        if (deliveredCount != null) {
            dto.setDeliveredCount(deliveredCount);
        } else {
            dto.setDeliveredCount(0);
        }
        String description1 = String.format("获取会员订单各种状态订单数");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.getMemberOrderStatistics);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", dto);
        return MessagePacket.newSuccess(rsMap, "getMemberOrderStatistics success");
    }

    @ApiOperation(value = "SJBuyGolden", notes = "独立接口：设计得到购买设计贝（金币）")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/SJBuyGolden", produces = {"application/json;charset=UTF-8"})
    public MessagePacket SJBuyGolden(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String isApplePay = request.getParameter("isApplePay");
        String goodsShopID = request.getParameter("goodsShopID");
        if (StringUtils.isBlank(goodsShopID)) {
            return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotNull, "goodsShopID不能为空");
        }
        if (!goodsShopService.checkIsValidId(goodsShopID)) {
            return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (!siteService.checkIdIsValid(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isBlank(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.paywayIDNotNull, "payWayID不能为空");
        }
        if (!paywayService.checkIsValidId(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
        }
        // 购买设计贝数量
        String goodsQTY = request.getParameter("goodsQTY");
        if (StringUtils.isBlank(goodsQTY)) {
            return MessagePacket.newFail(MessageHeader.Code.goodsQTYNotNull, "goodsQTY不能为空");
        }
        // 购买金额
        String priceTotal = request.getParameter("priceTotal");
        if (StringUtils.isBlank(priceTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.priceTotalNotNull, "priceTotal不能为空");
        }
        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            map.put("outPayType", outPayType);
        }
        // 创建会员订单
        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
        String applicationID = goodsShop.getApplicationID();
        String companyID = goodsShop.getCompanyID();
        String shopID = goodsShop.getShopID();
        String memberOrderID = IdGenerator.uuid32();
        map.put("id", memberOrderID);
        map.put("applicationID", applicationID);
        map.put("buyType", 12);
        map.put("companyID", companyID);
        map.put("financeType", 1);
        map.put("siteID", siteID);
        map.put("shopID", shopID);
        map.put("memberID", member.getId());
        map.put("name", member.getName() + "购买" + goodsQTY + "个设计贝");
        map.put("orderType", 81);
        map.put("applyTime", TimeTest.getTime());
        map.put("goodsQTY", goodsQTY);
        map.put("priceTotal", Double.parseDouble(priceTotal));
        map.put("closedPriceTotal", Double.parseDouble(priceTotal));
        map.put("priceAfterDiscount", Double.parseDouble(priceTotal));
        map.put("payFrom", 2);
        map.put("contactName", member.getName());
        map.put("phone", member.getPhone());
        map.put("status", 1);
        map.put("creator", member.getId());
        String orderCode = sequenceDefineService.genCode("orderSeq", memberOrderID);
        map.put("orderCode", orderCode);
        map.put("payWayID", payWayID);
        map.put("goodsNumbers", 0);
        map.put("weighTotal", 0);
        map.put("weightTotalFee", 0);
        map.put("sendPrice", 0);
        map.put("pointUsed", 0);
        map.put("pointTotal", 0);
        map.put("rebateTotal", 0);
        map.put("goldenTotal", 0);
        map.put("beanTotal", 0);
        map.put("sendStatus", 0);
        map.put("payStatus", 0);
        map.put("subPrePay", 0);
        map.put("pointSubAmount", 0);
        map.put("rebateSubAmount", 0);
        map.put("bonusAmount", 0);
        map.put("weighPrice", 0);
        map.put("discountAmount", 0);
        map.put("tax", 0);
        map.put("presentPayAmount", 0);
        map.put("sendType", 1);
        map.put("sendFee", 0);
        map.put("returnAmount", 0);
        map.put("isMemberHide", 0);
        memberOrderService.insertMemberOrder(map);
        // 创建goldenGet记录
        GoldenGet goldenGet = new GoldenGet();
        goldenGet.setApplicationID(applicationID);
        goldenGet.setId(IdGenerator.uuid32());
        goldenGet.setCompanyID(companyID);
        goldenGet.setShopID(shopID);
        goldenGet.setMemberID(member.getId());
        goldenGet.setName(member.getName() + "购买" + goodsQTY + "个设计贝");
        goldenGet.setNumber(Double.parseDouble(goodsQTY));
        goldenGet.setTokenNumber(Double.parseDouble(goodsQTY));
        goldenGet.setMemberOrderID(memberOrderID);
        goldenGet.setStatus(1);
        goldenGet.setGetType(1);
        goldenGet.setOrderCode(orderCode);
        goldenGet.setPayWayID(payWayID);
        goldenGetService.insert(goldenGet);
        // 如果是苹果支付的话需要创建memberPayment
        String memberPaymentID = "";
        if (StringUtils.isNotBlank(isApplePay) && "1".equals(isApplePay)) {
            MemberPayment memberPayment = new MemberPayment();
            memberPaymentID = IdGenerator.uuid32();
            memberPayment.setId(memberPaymentID);
            memberPayment.setApplicationID(applicationID);
            memberPayment.setMemberID(member.getId());
            memberPayment.setName(member.getName() + "购买" + goodsQTY + "个设计贝");
            memberPayment.setObjectName(member.getName() + "购买" + goodsQTY + "个设计贝");
            memberPayment.setOrderCode(orderCode);
            memberPayment.setAmount(Double.parseDouble(priceTotal));
            memberPayment.setObjectDefineID(Config.MEMBERORDER_OBJECTDEFINEID);
            memberPayment.setObjectID(memberOrderID);
            memberPayment.setPayFrom(2);
            memberPayment.setApplyTime(TimeTest.getTime());
            memberPayment.setStatus(1);
            memberPaymentService.insert(memberPayment);
            // 保存id到memberOrder
            map.clear();
            map.put("id", memberOrderID);
            map.put("memberPaymentID", memberPaymentID);
            memberOrderService.update(map);
        }
        // 创建memberOrderGoods
        map.clear();
        map.put("id", IdGenerator.uuid32());
        map.put("applicationID", applicationID);
        map.put("companyID", companyID);
        map.put("shopID", shopID);
        map.put("memberID", member.getId());
        map.put("memberOrderID", memberOrderID);
        map.put("goodsShopID", goodsShopID);
        map.put("priceTotalReturn", 0);
        map.put("priceReturn", 0);
        map.put("QTYWeighed", 0);
        map.put("giveQTY", 0);
        map.put("weighTotal", 0);
        map.put("weightTotalFee", 0);
        map.put("weighPrice", 0);
        map.put("tax", 0);
        map.put("priceTotalReturn", 0);
        map.put("discountRate", 0);
        if (goodsShop.getStandPrice() != null) {
            map.put("priceStand", goodsShop.getStandPrice());
        }
        if (goodsShop.getRealPrice() != null) {
            map.put("priceNow", goodsShop.getRealPrice());//4
        }
        if (goodsShop.getUseDescription() != null) {
            map.put("description", goodsShop.getDescription());
        }
        if (goodsShop.getUnitDescription() != null) {
            map.put("useDescription", goodsShop.getUnitDescription());
        }
        if (goodsShop.getGoodsID() != null) {
            map.put("goodsID", goodsShop.getGoodsID());
        }
        if (goodsShop.getGoodsCompanyID() != null) {
            map.put("goodsCompanyID", goodsShop.getGoodsCompanyID());
        }
        if (goodsShop.getTermCode() != null) {
            map.put("IDCode", goodsShop.getTermCode());
        }
        if (goodsShop.getName() != null) {
            map.put("name", goodsShop.getName());
        }
        if (goodsShop.getShowName() != null) {
            map.put("showName", goodsShop.getShowName());
        }
        if (goodsShop.getLittleImage() != null) {
            map.put("listImage", goodsShop.getLittleImage());
        }
        if (goodsShop.getTermCode() != null) {
            map.put("IDCode", goodsShop.getTermCode());
        }
        map.put("QTY", 1);
        map.put("status", 1);
        if (priceTotal != null) {
            map.put("priceTotal", priceTotal);
        }
        map.put("name", member.getName() + "购买" + goodsShop.getName());
        memberOrderGoodsService.insertGoodsByMap(map);
        String description1 = String.format("设计得到购买设计贝（金币）");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request,
                Memberlog.MemberOperateType.getMemberOrderStatistics);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("memberOrderID", memberOrderID);
        if (StringUtils.isNotBlank(isApplePay) && "1".equals(isApplePay)) {
            rsMap.put("memberPaymentID", memberPaymentID);
        }
        return MessagePacket.newSuccess(rsMap, "SJBuyGolden success");
    }

    @ApiOperation(value = "JYCreateMemberOrder", notes = "独立接口：甲悦创建订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/JYCreateMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket JYCreateMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String goodsShopID = request.getParameter("goodsShopID");
        if (StringUtils.isBlank(goodsShopID)) {
            return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotNull, "goodsShopID不能为空");
        }
        if (!goodsShopService.checkIsValidId(goodsShopID)) {
            return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID不正确");
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isBlank(shopID)) {
            return MessagePacket.newFail(MessageHeader.Code.shopIDNotNull, "shopID不能为空");
        }
        if (!shopService.checkIdIsValid(shopID)) {
            return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (!siteService.checkIdIsValid(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isBlank(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.paywayIDNotNull, "payWayID不能为空");
        }
        if (!paywayService.checkIsValidId(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
        }
        // 购买金额
        String priceTotal = request.getParameter("priceTotal");
        if (StringUtils.isBlank(priceTotal)) {
            return MessagePacket.newFail(MessageHeader.Code.priceTotalNotNull, "priceTotal不能为空");
        }
        // 创建会员订单
        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
        String applicationID = goodsShop.getApplicationID();
        String companyID = goodsShop.getCompanyID();
        String memberOrderID = IdGenerator.uuid32();
        map.put("id", memberOrderID);
        map.put("applicationID", applicationID);
        map.put("companyID", companyID);
        map.put("siteID", siteID);
        map.put("shopID", shopID);
        map.put("memberID", member.getId());
        map.put("name", member.getName() + "报修");
        map.put("orderType", 1);
        map.put("applyTime", TimeTest.getTime());
        map.put("goodsQTY", 1);
        map.put("priceTotal", Double.parseDouble(priceTotal));
        map.put("priceAfterDiscount", Double.parseDouble(priceTotal));
        map.put("payFrom", 2);
        map.put("contactName", member.getName());
        map.put("phone", member.getPhone());
        map.put("status", 1);
        map.put("isMemberHide", 0);
        map.put("creator", member.getId());
        String orderCode = sequenceDefineService.genCode("orderSeq", memberOrderID);
        map.put("orderCode", orderCode);
        map.put("payWayID", payWayID);
        memberOrderService.insertMemberOrder(map);
        // 创建memberOrderGoods
        map.clear();
        map.put("id", IdGenerator.uuid32());
        map.put("applicationID", applicationID);
        map.put("companyID", companyID);
        map.put("shopID", shopID);
        map.put("memberID", member.getId());
        map.put("memberOrderID", memberOrderID);
        map.put("QTY", 1);
        map.put("status", 1);
        map.put("priceTotal", priceTotal);
        map.put("name", member.getName() + "报修");
        memberOrderGoodsService.insertGoodsByMap(map);
        String description1 = String.format("甲悦创建订单");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.getMemberOrderStatistics);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("memberOrderID", memberOrderID);
        return MessagePacket.newSuccess(rsMap, "JYCreateMemberOrder success");
    }

    @ApiOperation(value = "SJApplePayMemberOrderNotify", notes = "独立接口：applePay充值设计贝IOS回调")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/SJApplePayMemberOrderNotify", produces = {"application/json;charset=UTF-8"})
    public MessagePacket SJApplePayMemberOrderNotify(HttpServletRequest request, ModelMap map) {
        String resXml = "";
        // 流水号
        String transaction_id = request.getParameter("transaction_id");
        if (StringUtils.isBlank(transaction_id)) {
            return MessagePacket.newFail(MessageHeader.Code.transaction_idNotNull, "流水号不能为空");
        }

        String memberPaymentID = request.getParameter("memberPaymentID");
        if (StringUtils.isBlank(memberPaymentID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberPaymentIDNotNull, "会员支付ID不能为空");
        }
        MemberPayment memberPayment = memberPaymentService.findById(memberPaymentID);
        if (memberPayment == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberPaymentIDNotFound, "会员支付ID不正确");
        }

        sendMessageService.sendPayReturn(memberPayment.getAmount(), transaction_id,
                memberPayment.getId(), 4);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("data", "支付成功");
        return MessagePacket.newSuccess(rsMap, "success");
    }

    @ApiOperation(value = "useGoldenBuyMemberOrder", notes = "独立接口：金币支付")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/useGoldenBuyMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket useGoldenBuyMemberOrder(HttpServletRequest request, ModelMap map) throws InterruptedException {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }

        Member member1 = memberService.selectById(member.getId());
        if (member1 == null) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "请重新登录");
        }
        String tradePassword = request.getParameter("tradePassword");
        if (StringUtils.isEmpty(tradePassword)) {
            return MessagePacket.newFail(MessageHeader.Code.tradePasswordIsNull, "交易密码不能为空");
        }
        if (StringUtils.isBlank(member1.getTradePassword())) {
            return MessagePacket.newFail(MessageHeader.Code.tradePasswordIsNull, "请设置交易密码");
        }
        if (!tradePassword.equals(member1.getTradePassword())) {
            return MessagePacket.newFail(MessageHeader.Code.tradePasswordIsError, "交易密码不正确");
        }

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if ("2".equals(memberOrder.getPayStatus().toString())) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "请勿重复支付");
        }
        Double closedPriceTotal = memberOrder.getClosedPriceTotal();
        // 找到会员统计表ID
        MemberStatisticsDto memberStatisticsDto = memberStatisticsService.getByMemberID(member.getId());
        // 判断金币数量是否足够
        Double golden = memberStatisticsDto.getGolden();
        if (golden < closedPriceTotal) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "金币不足");
        }

        sendMessageService.sendPayReturn(closedPriceTotal, null,
                memberOrderID, 3);

        // 休眠一秒后查询订单状态 成功则走分销队列
        Thread.sleep(1000);
        // 分销
        if (memberOrder.getStatus() == 9) {
            sendMessageService.sendMemberIncome(member.getId(), memberOrder.getClosedPriceTotal(), memberOrder.getId());
        }
        String description1 = String.format("金币支付");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request,
                Memberlog.MemberOperateType.useGoldenBuy);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("buyTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "useGoldenBuyMemberOrder success");
    }

    @ApiOperation(value = "queryOneMemberOrderStatus", notes = "独立接口：查询订单支付状态")
    @RequestMapping(value = "/queryOneMemberOrderStatus", produces = {"application/json;charset=UTF-8"})
    public MessagePacket queryOneMemberOrderStatus(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        String description1 = String.format("查询订单支付状态");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request,
                Memberlog.MemberOperateType.queryOneMemberOrderStatus);
        Map<String, Object> rsMap = new HashMap<>();
        if (memberOrder.getPayStatus() != null) {
            rsMap.put("payStatus", memberOrder.getPayStatus());
        } else {
            rsMap.put("payStatus", null);
        }
        rsMap.put("status", memberOrder.getStatus());
        if (memberOrder.getPayTotal() != null) {
            rsMap.put("payTotal", memberOrder.getPayTotal());
        } else {
            rsMap.put("payTotal", null);
        }
        if (memberOrder.getGoldenTotal() != null) {
            rsMap.put("goldenTotal", memberOrder.getGoldenTotal());
        } else {
            rsMap.put("goldenTotal", null);
        }
        if (memberOrder.getPayFrom() != null) {
            rsMap.put("payFrom", memberOrder.getPayFrom());
        } else {
            rsMap.put("payFrom", null);
        }
        if (memberOrder.getPayTime() != null) {
            rsMap.put("payTime", TimeTest.toDateTimeFormat(memberOrder.getPayTime()));
        } else {
            rsMap.put("payTime", null);
        }
        if (memberOrder.getPaySequence() != null) {
            rsMap.put("paySequence", memberOrder.getPaySequence());
        } else {
            rsMap.put("paySequence", null);
        }
        return MessagePacket.newSuccess(rsMap, "queryOneMemberOrderStatus success");
    }

    @Autowired
    private BonusService bonusService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ObjectDefineService objectDefineService;
    @Autowired
    private MemberStatisticsService memberStatisticsService;
    @Autowired
    private MemberOrderService memberOrderService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private SendMessageService sendMessageService;
    @Autowired
    private MemberAddressService memberAddressService;
    @Autowired
    private SequenceDefineService sequenceDefineService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private SceneService sceneService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private OrderTemplateGoodsService orderTemplateGoodsService;
    @Autowired
    private MemberOrderGoodsService memberOrderGoodsService;
    @Autowired
    private KingBase kingBase;
    @Autowired
    private PaywayService paywayService;
    @Autowired
    private MemberInvoiceDefineService memberInvoiceDefineService;
    @Autowired
    private MemberBonusService memberBonusService;
    @Resource
    private ComputerRecordService computerRecordService;

    @ApiOperation(value = "SJCreateMemberOrder", notes = "独立接口：设计得到创建订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/SJCreateMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket SJCreateMemberOrder(HttpServletRequest request, ModelMap map, ModelMap maps) {
        String sessionID = request.getParameter("sessionID");
        String memberOrderID = IdGenerator.uuid32();
        Member member = null;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
            Member member1 = memberService.getMemberByRecommendCode(map);
            if (member1 != null) {
                map.clear();
                map.put("recommendMemberID", member1.getId());
            }
        }

        String goodsShopID = request.getParameter("goodsShopID");
        if (StringUtils.isNotBlank(goodsShopID)) {
            if (!goodsShopService.checkIsValidId(goodsShopID)) {
                return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID不正确");
            }
        }

        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        } else {
            siteID = member.getSiteID();
            map.put("siteID", siteID);
        }

        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isNotBlank(payWayID)) {
            if (!paywayService.checkIsValidId(payWayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
            }
            map.put("payWayID", payWayID);
        }

        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
            MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
            if (memberAddress != null) {
                if (StringUtils.isNotBlank(memberAddress.getContactName())) {
                    map.put("contactName", memberAddress.getContactName());
                }
                if (StringUtils.isNotBlank(memberAddress.getName())) {
                    map.put("memberAddressName", memberAddress.getName());
                }
                if (StringUtils.isNotBlank(memberAddress.getPhone())) {
                    map.put("phone", memberAddress.getPhone());
                }
            }
        }

        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!memberInvoiceDefineService.checkIsValidId(memberInvoiceDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }

        String memberMemo = request.getParameter("memberMemo");
        if (StringUtils.isNotBlank(memberMemo)) {
            map.put("memberMemo", memberMemo);
        }

        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }

        String actionGoodsType = request.getParameter("actionGoodsType");
        if (StringUtils.isNotBlank(actionGoodsType)) {
            map.put("actionGoodsType", actionGoodsType);
        }

        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            map.put("outPayType", outPayType);
        }

        String buyType = request.getParameter("buyType");
        if (StringUtils.isNotBlank(buyType)) {
            map.put("buyType", buyType);
        }

        String financeType = request.getParameter("financeType");
        if (StringUtils.isNotBlank(financeType)) {
            map.put("financeType", financeType);
        }

        // 付款方式
        String payFrom = request.getParameter("payFrom");
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            map.put("orderType", orderType);
        } else {
            orderType = "1";
        }

        // 购买金额--金币
        String goldenTotal = request.getParameter("goldenTotal");
        // 原价
        String priceTotalStr = request.getParameter("priceTotal");
        Double priceTotal = Double.valueOf(priceTotalStr);
        // 实际价格初始化
        Double firstPayTotal = priceTotal;
        // 判断红包是否使用
        Boolean is = false;
        String memberBonusID = request.getParameter("memberBonusID");
        HashMap memberMap = new HashMap();
        if (StringUtils.isNotBlank(memberBonusID)) {
            memberMap.put("id", memberBonusID);
            // 查询会员红包信息
            MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(memberMap);
            // 判断是否可用
            if (memberBonus.getStatus() == 10) {
                is = true;
                // 红包金额
                Double amount = Double.valueOf(memberBonus.getAmount());
                // 红包折扣
                Double offRate = memberBonus.getOffRate();
                // 获取红包起点金额
                String bonusID = memberBonus.getBonusID();
                Integer startPrice = 0;
                if (StringUtils.isNotBlank(bonusID)) {
                    BonusDto bonusDto = bonusService.getByID(bonusID);
                    // 起点金额
                    startPrice = Integer.valueOf(bonusDto.getStartPrice());
                }
                if (priceTotal >= startPrice) {
                    if (amount == null || amount == 0.0) {
                        // 该分支为使用红包折扣
//                        offRate = offRate / 100;
                        offRate = BigDecimal.valueOf(offRate).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).doubleValue();
//                        firstPayTotal = Double.valueOf(priceTotal - priceTotal * offRate);
                        firstPayTotal = BigDecimal.valueOf(priceTotal)
                                .subtract(BigDecimal.valueOf(priceTotal)
                                        .multiply(BigDecimal.valueOf(offRate))
                                        .setScale(2, RoundingMode.HALF_UP))
                                .setScale(2, RoundingMode.HALF_UP).doubleValue();
                    } else {
                        // 该分支为红包金额
//                        firstPayTotal = Double.valueOf(priceTotal - amount);
                        firstPayTotal = BigDecimal.valueOf(priceTotal)
                                .subtract(new BigDecimal(amount))
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();

                    }
                } else {
                    return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "未满足该红包的起点金额");
                }
            }
        }
        // 创建会员订单
        String applicationID = member.getApplicationID();
        String companyID = member.getCompanyID();
        Double payTotal = firstPayTotal;
        map.put("id", memberOrderID);
        map.put("applicationID", applicationID);
        map.put("companyID", companyID);
        map.put("memberID", member.getId());
        map.put("name", member.getName() + "购买商品");
        map.put("orderType", orderType);
        map.put("applyTime", TimeTest.getTime());
        map.put("goodsQTY", 1);
        map.put("priceTotal", payTotal);
        map.put("goodsNumbers", 1);
        map.put("weighTotal", 0);
        map.put("weightTotalFee", 0);
        map.put("sendPrice", 0);
        map.put("pointUsed", 0);
        map.put("pointTotal", 0);
        map.put("rebateTotal", 0);
        map.put("goldenTotal", 0);
        map.put("beanTotal", 0);
        map.put("subPrePay", 0);
        map.put("pointSubAmount", 0);
        map.put("rebateSubAmount", 0);
        map.put("bonusAmount", 0);
        map.put("sendStatus", 0);
        map.put("weighPrice", 0);
        map.put("discountAmount", 0);
        map.put("tax", 0);
        map.put("payStatus", 0);
        map.put("presentPayAmount", 0);
        map.put("sendType", 1);
        map.put("sendFee", 0);
        map.put("returnAmount", 0);
        map.put("isMemberHide", 0);
        //  优惠后价格
        map.put("priceAfterDiscount", payTotal);
        //  付款方式
        if (Integer.valueOf(payFrom) == 2) {
            // 第三方支付
            map.put("payFrom", 2);
            if (is) {
                map.put("closedPriceTotal", firstPayTotal);
            } else {
                map.put("closedPriceTotal", goldenTotal);
            }
        } else if (Integer.valueOf(payFrom) == 12) {
            // 金币支付
            map.put("payFrom", 12);
            if (is) {
                map.put("closedPriceTotal", firstPayTotal);
            } else {
                map.put("closedPriceTotal", priceTotal);
            }
        }
        map.put("priceAfterDiscount", firstPayTotal);
        if (StringUtils.isNotBlank(goldenTotal)) {
            map.put("goldenTotal", Double.parseDouble(goldenTotal));
        }
        map.put("contactName", member.getName());
        map.put("phone", member.getPhone());
        map.put("status", 1);
        map.put("creator", member.getId());
        String orderCode = sequenceDefineService.genCode("orderSeq", memberOrderID);
        map.put("orderCode", orderCode);
        Double bonusAmount = 0.0d;
        String memberBonusIDchain = request.getParameter("memberBonusIDchain");
        if (StringUtils.isNotBlank(memberBonusIDchain)) {
            String[] memberBonusIDArray = memberBonusIDchain.split(",");
            List<MemberBonus> memberBonusList = new ArrayList<>();
            for (String memberBonusID1 : memberBonusIDArray) {
                // 检查会员红包是否存在和可用
                maps.clear();
                maps.put("id", memberBonusID1);
                MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(maps);
                if (memberBonus != null) {
                    long cancelTime = memberBonus.getCancelTime().getTime();
                    String nowTime = TimeTest.getTimeStr();
                    long newDate = TimeTest.strToDate(nowTime).getTime();
                    if (cancelTime >= newDate && memberBonus.getMemberOrderID() == null) {
                        memberBonusList.add(memberBonus);
                        // 修改红包状态
                        Double amount = memberBonus.getAmount();
                        maps.clear();
                        maps.put("id", memberBonus.getId());
                        maps.put("offAmount", amount);
                        maps.put("useTime", TimeTest.getTime());
                        maps.put("memberOrderID", memberOrderID);
                        memberBonusService.updateByMap(maps);
//                        bonusAmount += amount;
                        bonusAmount = BigDecimal.valueOf(bonusAmount)
                                .add(BigDecimal.valueOf(amount)).doubleValue();
                    }
                }
            }
            // 修改订单最后的成交金额
            if (!memberBonusList.isEmpty() && memberBonusList.size() > 0) {
                for (MemberBonus memberBonus : memberBonusList) {
                    maps.clear();
                    maps.put("id", memberBonus.getId());
//                    maps.put("orderTotal", firstPayTotal - bonusAmount);
                    maps.put("orderTotal", BigDecimal.valueOf(firstPayTotal)
                            .subtract(BigDecimal.valueOf(bonusAmount)).doubleValue());
                    memberBonusService.updateByMap(maps);
                }
            }
        }
        // 保存订单的最后成交价格
//        map.put("payTotal", firstPayTotal - bonusAmount);
        map.put("payTotal", BigDecimal.valueOf(firstPayTotal)
                .subtract(BigDecimal.valueOf(bonusAmount)).doubleValue());
        memberOrderService.insertMemberOrder(map);
        String memberBuyReadID = request.getParameter("memberBuyReadID");
        if (StringUtils.isNotBlank(memberBuyReadID)) {
            if (!memberBuyReadService.checkIdIsValid(memberBuyReadID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberBuyReadIDNotFound, "memberBuyReadID不正确");
            }
            // 修改
            memberBuyReadService.update(new HashMap<String, Object>() {
                {
                    put("id", memberBuyReadID);
                    put("memberOrderID", memberOrderID);
                    put("payFrom", payFrom);
                    put("payWayID", payWayID);
                    if ("12".equals(payFrom)) {
                        put("cashPrice", 0);
                    } else {
                        put("goldenPrice", 0);
                    }
                }
            });
        }
        if (StringUtils.isNotBlank(memberBonusID)) {
            MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
            map.clear();
            map.put("status", 20);
            map.put("memberOrderID", memberOrderID);
            map.put("id", memberBonusID);
            map.put("orderTotal", memberOrder.getPriceAfterDiscount());
            map.put("useTime", TimeTest.getTimeStr());
            memberBonusService.updateByMap(map);
        }

        // 创建memberOrderGoods
        String byCart = request.getParameter("byCart");
        if (StringUtils.isNotBlank(byCart)) {
            if ("1".equals(byCart)) {
                map.clear();
                map.put("memberID", member.getId());
                map.put("isSelect", 1);
                String cartID = "";
                List<ApiCartGoodsListDto> cartGoodsList = cartGoodsService.getCartGoodsList(map);
                List<ApiCartGoodsListDto> newCartGoodsList = new ArrayList<>();
                if (!cartGoodsList.isEmpty() && cartGoodsList.size() > 0) {
                    cartID = cartGoodsList.get(0).getCartID();
                    if (StringUtils.isNotBlank(shopID)) {
                        for (ApiCartGoodsListDto dto : cartGoodsList) {
                            GoodsShop goodsShops = goodsShopService.getGoodsShopByID(dto.getGoodsShopID());
                            if (goodsShops != null) {
                                String shopID1 = goodsShops.getShopID();
                                if (StringUtils.isNotBlank(shopID1) && shopID1.equals(shopID)) {
                                    newCartGoodsList.add(dto);
                                }
                            }
                        }
                    } else {
                        newCartGoodsList.addAll(cartGoodsList);
                    }
                    for (ApiCartGoodsListDto cartGoodsListDto : newCartGoodsList) {
                        GoodsShop goodsShop1 = goodsShopService.getGoodsShopByID(cartGoodsListDto.getGoodsShopID());
                        map.clear();
                        map.put("id", IdGenerator.uuid32());
                        map.put("applicationID", applicationID);
                        map.put("companyID", companyID);
                        map.put("shopID", shopID);
                        map.put("memberID", member.getId());
                        map.put("memberOrderID", memberOrderID);
                        map.put("goodsShopID", goodsShop1.getId());
                        map.put("priceTotalReturn", 0);
                        map.put("priceReturn", 0);
                        map.put("QTYWeighed", 0);
                        map.put("giveQTY", 0);
                        map.put("weighTotal", 0);
                        map.put("weightTotalFee", 0);
                        map.put("weighPrice", 0);
                        map.put("tax", 0);
                        map.put("priceTotalReturn", 0);
                        map.put("discountRate", 0);
                        if (goodsShop1.getStandPrice() != null) {
                            map.put("priceStand", goodsShop1.getStandPrice());
                        }
                        if (goodsShop1.getRealPrice() != null) {
                            map.put("priceNow", goodsShop1.getRealPrice());//5
                            map.put("priceTotal", goodsShop1.getRealPrice() * cartGoodsListDto.getQTY());
                        }
                        if (goodsShop1.getUseDescription() != null) {
                            map.put("description", goodsShop1.getDescription());
                        }
                        if (goodsShop1.getUnitDescription() != null) {
                            map.put("useDescription", goodsShop1.getUnitDescription());
                        }
                        if (goodsShop1.getGoodsID() != null) {
                            map.put("goodsID", goodsShop1.getGoodsID());
                        }
                        if (goodsShop1.getGoodsCompanyID() != null) {
                            map.put("goodsCompanyID", goodsShop1.getGoodsCompanyID());
                        }
                        if (goodsShop1.getTermCode() != null) {
                            map.put("IDCode", goodsShop1.getTermCode());
                        }
                        if (goodsShop1.getName() != null) {
                            map.put("name", goodsShop1.getName());
                        }
                        if (goodsShop1.getShowName() != null) {
                            map.put("showName", goodsShop1.getShowName());
                        }
                        if (goodsShop1.getLittleImage() != null) {
                            map.put("listImage", goodsShop1.getLittleImage());
                        }
                        if (goodsShop1.getTermCode() != null) {
                            map.put("IDCode", goodsShop1.getTermCode());
                        }
                        map.put("QTY", 1);
                        map.put("status", cartGoodsListDto.getQTY());
                        if (StringUtils.isNotBlank(goldenTotal)) {
                            map.put("objectFeatureItemID1", goldenTotal);
                        }
                        memberOrderGoodsService.insertGoodsByMap(map);
                        // 删除购物车商品
                        cartGoodsService.deleteGoodsFromCart(new HashMap<String, Object>() {
                            {
                                put("cartGoodsID", cartGoodsListDto.getCartGoodsID());
                            }
                        });
                    }
                }
                // 修改购物车当前商品数量
                if (StringUtils.isNotBlank(cartID)) {
                    map.clear();
                    map.put("memberID", member.getId());
                    map.put("isSelect", 1);
                    List<ApiCartGoodsListDto> cartNumList = cartGoodsService.getCartGoodsList(map);
                    if (!cartNumList.isEmpty() && cartNumList.size() > 0) {
                        map.clear();
                        map.put("id", cartID);
                        map.put("QTY", cartNumList.size());
                        cartService.updateOneCart(map);
                    }
                }
            }
        } else {
            GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
            map.clear();
            map.put("id", IdGenerator.uuid32());
            map.put("applicationID", applicationID);
            map.put("companyID", companyID);
            map.put("shopID", shopID);
            map.put("memberID", member.getId());
            map.put("memberOrderID", memberOrderID);
            map.put("goodsShopID", goodsShopID);
            map.put("priceTotalReturn", 0);
            map.put("priceReturn", 0);
            map.put("QTYWeighed", 0);
            map.put("giveQTY", 0);
            map.put("weighTotal", 0);
            map.put("weightTotalFee", 0);
            map.put("weighPrice", 0);
            map.put("tax", 0);
            map.put("priceTotalReturn", 0);
            map.put("discountRate", 0);
            if (goodsShop.getStandPrice() != null) {
                map.put("priceStand", goodsShop.getStandPrice());
            }
            if (goodsShop.getRealPrice() != null) {
                map.put("priceNow", goodsShop.getRealPrice());//6
                map.put("priceTotal", goodsShop.getRealPrice());
            }
            if (goodsShop.getUseDescription() != null) {
                map.put("description", goodsShop.getDescription());
            }
            if (goodsShop.getUnitDescription() != null) {
                map.put("useDescription", goodsShop.getUnitDescription());
            }
            if (goodsShop.getGoodsID() != null) {
                map.put("goodsID", goodsShop.getGoodsID());
            }
            if (goodsShop.getGoodsCompanyID() != null) {
                map.put("goodsCompanyID", goodsShop.getGoodsCompanyID());
            }
            if (goodsShop.getTermCode() != null) {
                map.put("IDCode", goodsShop.getTermCode());
            }
            if (goodsShop.getName() != null) {
                map.put("name", goodsShop.getName());
            }
            if (goodsShop.getShowName() != null) {
                map.put("showName", goodsShop.getShowName());
            }
            if (goodsShop.getLittleImage() != null) {
                map.put("listImage", goodsShop.getLittleImage());
            }
            if (goodsShop.getTermCode() != null) {
                map.put("IDCode", goodsShop.getTermCode());
            }
            map.put("QTY", 1);
            map.put("status", 1);
            if (StringUtils.isNotBlank(goldenTotal)) {
                map.put("objectFeatureItemID1", goldenTotal);
            }
            memberOrderGoodsService.insertGoodsByMap(map);
        }
        String description1 = String.format("设计得到创建订单");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request,
                Memberlog.MemberOperateType.SJCreateMemberOrder);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("memberOrderID", memberOrderID);
        return MessagePacket.newSuccess(rsMap, "SJCreateMemberOrder success");
    }

    @ApiOperation(value = "getMemberOrderRecommendList", notes = "获取会员分销订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberOrderRecommendList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberOrderRecommendList(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("memberID", member.getId());
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) map.put("sortTypeTime", sortTypeTime);
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<MemberOrderRecommendDto> list = memberOrderService.getMemberOrderRecommendList(map);
        if (!list.isEmpty() && list.size() > 0) {
            PageInfo<ApiModelListDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        List<MemberOrderRecommendDto> list1 = new ArrayList<>();
        if (!list.isEmpty() && list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getPayTimeStr() != null) {
                    Double price = list.get(i).getPriceAfterDiscount();
                    // 找到商品
                    List<MemberOrderGoods> memberOrderGoodsList = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(list.get(i).getMemberOrderID());
                    if (!memberOrderGoodsList.isEmpty() && memberOrderGoodsList.size() > 0) {
                        String goodsShopID = memberOrderGoodsList.get(0).getGoodsShopID();
                        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                        list.get(i).setGoodsShopID(goodsShopID);
                        list.get(i).setGoodsShopName(goodsShop.getName());
                    }
                    if (StringUtils.isNotBlank(list.get(i).getRecommendMemberID())) {
                        map.put("memberID", list.get(i).getRecommendMemberID());
                        map.put("majorID", "8a2f462a78ecd2fa0178eedb3ed4057b");
                        List<MyMemberMajorListDto> memberMajorListDtos = memberMajorService.getMemberMajorList(map);
                        if (!memberMajorListDtos.isEmpty() && memberMajorListDtos.size() > 0) {
                            BigDecimal result = BigDecimal.valueOf(price)
                                    .multiply(BigDecimal.valueOf(0.3))
                                    .setScale(2, RoundingMode.HALF_UP);
                            list.get(i).setRecommendPrice(result.doubleValue());
                        } else {
                            BigDecimal result = BigDecimal.valueOf(price)
                                    .multiply(BigDecimal.valueOf(0.1))
                                    .setScale(2, RoundingMode.HALF_UP);
                            list.get(i).setRecommendPrice(result.doubleValue());
                        }
                    }
                }
            }
        }
        if (memberLogDTO != null) {
            String description = String.format("获取会员分销订单列表");
            sendMessageService.sendMemberLogMessage(sessionID, description, request,
                    Memberlog.MemberOperateType.getMemberOrderRecommendList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberOrderRecommendList success");
    }

    @ApiOperation(value = "outSystemGetMemberOrderStatistics", notes = "独立接口:外部系统会员订单统计列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetMemberOrderStatistics", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberOrderStatistics(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//      User user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (!companyService.checkIdIsValid(companyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String orderTemplateID = request.getParameter("orderTemplateID");
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!orderTemplateService.checkIsValidId(orderTemplateID)) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIDIsNull, "orderTemplateID不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        String actionID = request.getParameter("actionID");
        if (StringUtils.isNotBlank(actionID)) {
            if (!actionService.checkIdIsValid(actionID)) {
                return MessagePacket.newFail(MessageHeader.Code.actionIDNotFound, "actionID不正确");
            }
            map.put("actionID", actionID);
        }
        String goodsSecondDefineID = request.getParameter("goodsSecondDefineID");
        if (StringUtils.isNotBlank(goodsSecondDefineID)) {
            GoodsSecondDto goodsSecondDto = goodsSecondService.getGoodsSecondDetail(goodsSecondDefineID);
            if (goodsSecondDto == null) {
                return MessagePacket.newFail(MessageHeader.Code.goodsSecondDefineIDNotFound, "goodsSecondDefineID不正确");
            }
            map.put("goodsSecondDefineID", goodsSecondDefineID);
        }
        String trainBusinessID = request.getParameter("trainBusinessID");
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!trainBusinessService.checkIdIsValid(trainBusinessID)) {
                return MessagePacket.newFail(MessageHeader.Code.trainBusinessIDNotFound, "trainBusinessID不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
            }
            map.put("memberID", memberID);
        }
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            map.put("orderType", orderType);
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            map.put("payFrom", payFrom);
        }
        String returnType = request.getParameter("returnType");
        if (StringUtils.isNotBlank(returnType)) {
            map.put("returnType", returnType);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            map.put("sendType", sendType);
        }
        String status = request.getParameter("status");
        if (StringUtils.isNotBlank(status)) {
            map.put("status", status);
        }
        StringBuilder returnFieldsBuffer = new StringBuilder();
        String returnFields = request.getParameter("returnFields");
        if (StringUtils.isBlank(returnFields)) {
            return MessagePacket.newFail(MessageHeader.Code.returnFieldsNotNull, "returnFields不能为空");
        }
        returnFieldsBuffer.append(returnFields);
        String groupFieldName = request.getParameter("groupFieldName");
        if (StringUtils.isNotBlank(groupFieldName)) {
            map.put("groupFieldName", groupFieldName);
        }
        String groupAmountArray = request.getParameter("groupAmountArray");
        if (StringUtils.isNotBlank(groupAmountArray) && StringUtils.isNotBlank(groupFieldName)) {
            map.put("groupBy", "price,date");
        } else if (StringUtils.isNotBlank(groupAmountArray)) {
            map.put("groupBy", "price");
        } else {
            map.put("groupBy", "date");
        }
        String sortType = request.getParameter("sortType");
        if (StringUtils.isNotBlank(sortType)) {
            map.put("sortType",sortType);
        } else {
            map.put("sortType", 1);
        }
        String groupTimeType = request.getParameter("groupTimeType");
        if (StringUtils.isNotBlank(groupTimeType)) {
            if ("1".equals(groupTimeType)) {
                returnFieldsBuffer.append(",DATE_FORMAT(" + groupFieldName + ",\"%Y年\") date");
            } else if ("2".equals(groupTimeType)) {
                returnFieldsBuffer.append(",CONCAT(YEAR(" + groupFieldName + "),'年第',quarter(" + groupFieldName + "),'季度') date");
            } else if ("3".equals(groupTimeType)) {
                returnFieldsBuffer.append(",DATE_FORMAT(" + groupFieldName + ",\"%Y年%m月\") date");
            } else if ("4".equals(groupTimeType)) {
                returnFieldsBuffer.append(",CONCAT(YEAR(" + groupFieldName + "),'年第',DATE_FORMAT(" + groupFieldName + ",'%U'),'周') date");
            } else if ("5".equals(groupTimeType)) {
                returnFieldsBuffer.append(",DATE_FORMAT(" + groupFieldName + ",\"%Y年%m月%d日\") date");
            } else if ("6".equals(groupTimeType)) {
                returnFieldsBuffer.append(",DATE_FORMAT(" + groupFieldName + ",\"%Y年%m月%d日 %H时\") date");
            } else if ("7".equals(groupTimeType)) {
                returnFieldsBuffer.append(",DATE_FORMAT(" + groupFieldName + ",\"%Y年%m月%d日 %H时%i分\") date");
            } else if ("8".equals(groupTimeType)) {
                returnFieldsBuffer.append(",DATE_FORMAT(" + groupFieldName + ",\"%Y年%m月%d日 %H时%i分%s秒\") date");
            }
        }
        if (StringUtils.isNotBlank(groupAmountArray)) {
            StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append(",elt(interval(payTotal");
            String[] amountArray = groupAmountArray.split(",");
            for (String s : amountArray) {
                stringBuffer.append("," + s);
            }
            stringBuffer.append(")");
            for (int i = 1; i < amountArray.length; i++) {
                if (i == 1) {
                    stringBuffer.append(",'less" + amountArray[i] + "'");
                } else if (i == amountArray.length - 1) {
                    stringBuffer.append(",'" + amountArray[i - 1] + "-" + amountArray[i] + "'" + ",'more" + amountArray[i] + "'");
                } else {
                    stringBuffer.append(",'" + amountArray[i - 1] + "-" + amountArray[i] + "'");
                }
            }
            stringBuffer.append(") price");
            returnFieldsBuffer.append(stringBuffer.toString());
        }
        map.put("returnFields", returnFieldsBuffer.toString());
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isBlank(beginTime)) {
            return MessagePacket.newFail(MessageHeader.Code.beginTimeIsNull, "beginTime不能为空");
        }
        map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isBlank(endTime)) {
            return MessagePacket.newFail(MessageHeader.Code.endTimeIsNull, "endTime不能为空");
        }
        map.put("endTime", endTime);
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            map.put("keyWords", "%" + keyWords + "%");
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List list = memberOrderService.outSystemGetMemberOrderStatistics(map);
        if (!list.isEmpty()) {
            PageInfo<BadgeDefineDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (user != null) {
            String descriptions = String.format("外部系统会员订单统计列表");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberOrderStatistics",
                    DataLog.objectDefineID.memberOrder.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberOrderStatistics success");
    }

    @ApiOperation(value = "outSystemGetMemberOrderGoodsStatistics", notes = "独立接口:外部系统会员订单统计列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetMemberOrderGoodsStatistics", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberOrderGoodsStatistics(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//      User user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (!companyService.checkIdIsValid(companyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String orderTemplateID = request.getParameter("orderTemplateID");
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!orderTemplateService.checkIsValidId(orderTemplateID)) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIDIsNull, "orderTemplateID不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        String actionID = request.getParameter("actionID");
        if (StringUtils.isNotBlank(actionID)) {
            if (!actionService.checkIdIsValid(actionID)) {
                return MessagePacket.newFail(MessageHeader.Code.actionIDNotFound, "actionID不正确");
            }
            map.put("actionID", actionID);
        }
        String goodsSecondDefineID = request.getParameter("goodsSecondDefineID");
        if (StringUtils.isNotBlank(goodsSecondDefineID)) {
            GoodsSecondDto goodsSecondDto = goodsSecondService.getGoodsSecondDetail(goodsSecondDefineID);
            if (goodsSecondDto == null) {
                return MessagePacket.newFail(MessageHeader.Code.goodsSecondDefineIDNotFound, "goodsSecondDefineID不正确");
            }
            map.put("goodsSecondDefineID", goodsSecondDefineID);
        }
        String trainBusinessID = request.getParameter("trainBusinessID");
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!trainBusinessService.checkIdIsValid(trainBusinessID)) {
                return MessagePacket.newFail(MessageHeader.Code.trainBusinessIDNotFound, "trainBusinessID不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
            }
            map.put("memberID", memberID);
        }
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String goodsID = request.getParameter("goodsID");
        if (StringUtils.isNotBlank(goodsID)) {
            if (!goodsService.checkIsValidId(goodsID)) {
                return MessagePacket.newFail(MessageHeader.Code.goodsIDNotFound, "goodsID不正确");
            }
            map.put("goodsID", goodsID);
        }
        String goodsCompanyID = request.getParameter("goodsCompanyID");
        if (StringUtils.isNotBlank(goodsCompanyID)) {
            if (!goodsCompanyService.checkIsValidId(goodsCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.goodsCompanyIDNotFound, "goodsCompanyID不正确");
            }
            map.put("goodsCompanyID", goodsCompanyID);
        }
        String goodsShopID = request.getParameter("goodsShopID");
        if (StringUtils.isNotBlank(goodsShopID)) {
            if (!goodsShopService.checkIsValidId(goodsShopID)) {
                return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID不正确");
            }
            map.put("goodsShopID", goodsShopID);
        }
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            map.put("orderType", orderType);
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            map.put("payFrom", payFrom);
        }
        String returnType = request.getParameter("returnType");
        if (StringUtils.isNotBlank(returnType)) {
            map.put("returnType", returnType);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            map.put("sendType", sendType);
        }
        String status = request.getParameter("status");
        if (StringUtils.isNotBlank(status)) {
            map.put("status", status);
        }
        StringBuilder returnFieldsBuffer = new StringBuilder();
        String returnFields = request.getParameter("returnFields");
        if (StringUtils.isBlank(returnFields)) {
            return MessagePacket.newFail(MessageHeader.Code.returnFieldsNotNull, "returnFields不能为空");
        }
        returnFieldsBuffer.append(returnFields);
        String groupFieldName = request.getParameter("groupFieldName");
        if (StringUtils.isBlank(groupFieldName)) {
            return MessagePacket.newFail(MessageHeader.Code.groupFieldNameNotNull, "groupFieldName不能为空");
        }
        map.put("groupFieldName", groupFieldName);
        String groupAmountArray = request.getParameter("groupAmountArray");
        if (StringUtils.isNotBlank(groupAmountArray)) {
            map.put("groupBy", "price,date");
        } else {
            map.put("groupBy", "date");
        }
        String sortType = request.getParameter("sortType");
        if (StringUtils.isNotBlank(sortType)) {
            map.put("sortType",sortType);
        } else {
            map.put("sortType", 1);
        }
        String groupTimeType = request.getParameter("groupTimeType");
        if (StringUtils.isBlank(groupTimeType)) {
            return MessagePacket.newFail(MessageHeader.Code.groupTimeTypeNotNull, "groupTimeType不能为空");
        }
        if ("1".equals(groupTimeType)) {
            returnFieldsBuffer.append(",DATE_FORMAT(m." + groupFieldName + ",\"%Y年\") date");
        } else if ("2".equals(groupTimeType)) {
            returnFieldsBuffer.append(",CONCAT(YEAR(m." + groupFieldName + "),'年第',quarter(" + groupFieldName + "),'季度') date");
        } else if ("3".equals(groupTimeType)) {
            returnFieldsBuffer.append(",DATE_FORMAT(m." + groupFieldName + ",\"%Y年%m月\") date");
        } else if ("4".equals(groupTimeType)) {
            returnFieldsBuffer.append(",CONCAT(YEAR(m." + groupFieldName + "),'年第',DATE_FORMAT(" + groupFieldName + ",'%U'),'周') date");
        } else if ("5".equals(groupTimeType)) {
            returnFieldsBuffer.append(",DATE_FORMAT(m." + groupFieldName + ",\"%Y年%m月%d日\") date");
        } else if ("6".equals(groupTimeType)) {
            returnFieldsBuffer.append(",DATE_FORMAT(m." + groupFieldName + ",\"%Y年%m月%d日 %H时\") date");
        } else if ("7".equals(groupTimeType)) {
            returnFieldsBuffer.append(",DATE_FORMAT(m." + groupFieldName + ",\"%Y年%m月%d日 %H时%i分\") date");
        } else if ("8".equals(groupTimeType)) {
            returnFieldsBuffer.append(",DATE_FORMAT(m." + groupFieldName + ",\"%Y年%m月%d日 %H时%i分%s秒\") date");
        }
        if (StringUtils.isNotBlank(groupAmountArray)) {
            StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append(",elt(interval(m.priceTotal");
            String[] amountArray = groupAmountArray.split(",");
            for (String s : amountArray) {
                stringBuffer.append("," + s);
            }
            stringBuffer.append(")");
            for (int i = 1; i < amountArray.length; i++) {
                if (i == 1) {
                    stringBuffer.append(",'less" + amountArray[i] + "'");
                } else if (i == amountArray.length - 1) {
                    stringBuffer.append(",'" + amountArray[i - 1] + "-" + amountArray[i] + "'" + ",'more" + amountArray[i] + "'");
                } else {
                    stringBuffer.append(",'" + amountArray[i - 1] + "-" + amountArray[i] + "'");
                }
            }
            stringBuffer.append(") price");
            returnFieldsBuffer.append(stringBuffer.toString());
        }
        map.put("returnFields", returnFieldsBuffer.toString());
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isBlank(beginTime)) {
            return MessagePacket.newFail(MessageHeader.Code.beginTimeIsNull, "beginTime不能为空");
        }
        map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isBlank(endTime)) {
            return MessagePacket.newFail(MessageHeader.Code.endTimeIsNull, "endTime不能为空");
        }
        map.put("endTime", endTime);
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            map.put("keyWords", "%" + keyWords + "%");
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List list = memberOrderService.outSystemGetMemberOrderGoodsStatistics(map);
        if (!list.isEmpty()) {
            PageInfo<BadgeDefineDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (user != null) {
            String descriptions = String.format("外部系统会员订单统计列表");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberOrderGoodsStatistics",
                    DataLog.objectDefineID.memberOrder.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberOrderGoodsStatistics success");
    }

    //外部系统获取会员订单列表
    @ApiOperation(value = "outSystemGetMemberOrderList", notes = "独立接口:外部系统获取会员订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetMemberOrderList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberOrderList(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID.不正确");
            }
            map.put("applicationID", applicationID);
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        }
        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            if (!channelService.checkIsValidByid(channelID)) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
        }
        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            if (!cityService.checkIdIsValid(cityID)) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }

        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String propertyID = request.getParameter("propertyID");
        if (StringUtils.isNotBlank(propertyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("property", propertyID))) {
                return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "propertyID不正确");
            }
            map.put("propertyID", propertyID);
        }
        String departmentID = request.getParameter("departmentID");
        if (StringUtils.isNotBlank(departmentID)) {
            Department department = departmentService.selectById(departmentID);
            if (department == null) {
                return MessagePacket.newFail(MessageHeader.Code.departmentIDNotFound, "departmentID不正确");
            }
            map.put("departmentID", departmentID);
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        String recommendMemberID = request.getParameter("recommendMemberID");//  推荐人会员ID 可以为空
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String salesEmployeeID = request.getParameter("salesEmployeeID");
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!employeeService.checkEmployeeID(salesEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        String houseID = request.getParameter("houseID");
        if (StringUtils.isNotBlank(houseID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("house", houseID))) {
                return MessagePacket.newFail(MessageHeader.Code.houseIDNotFound, "houseID不正确");
            }
            map.put("houseID", houseID);
        }
        String officeID = request.getParameter("officeID");
        if (StringUtils.isNotBlank(officeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("office", officeID))) {
                return MessagePacket.newFail(MessageHeader.Code.officeIDNotFound, "officeID不正确");
            }
            map.put("officeID", officeID);
        }
        String machineID = request.getParameter("machineID");
        if (StringUtils.isNotBlank(machineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("machine", machineID))) {
                return MessagePacket.newFail(MessageHeader.Code.machineIDNotFound, "machineID不正确");
            }
            map.put("machineID", machineID);
        }
        String orderTemplateID = request.getParameter("orderTemplateID");
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("orderTemplate", orderTemplateID))) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIDNotFound, "orderTemplateID不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        String cardDefineID = request.getParameter("cardDefineID");
        if (StringUtils.isNotBlank(cardDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("cardDefine", cardDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.cardDefineIDNotFound, "cardDefineID不正确");
            }
            map.put("cardDefineID", cardDefineID);
        }
        String actionID = request.getParameter("actionID");
        if (StringUtils.isNotBlank(actionID)) {
            if (!actionService.checkIdIsValid(actionID)) {
                return MessagePacket.newFail(MessageHeader.Code.actionIDNotFound, "actionID不正确");
            }
            map.put("actionID", actionID);
        }
        String constructionProjectID = request.getParameter("constructionProjectID");
        if (StringUtils.isNotBlank(constructionProjectID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("constructionProject", constructionProjectID))) {
                return MessagePacket.newFail(MessageHeader.Code.constructionProjectIDNotFound, "constructionProjectID不正确");
            }
            map.put("constructionProjectID", constructionProjectID);
        }
        String constructionID = request.getParameter("constructionID");
        if (StringUtils.isNotBlank(constructionID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("construction", constructionID))) {
                return MessagePacket.newFail(MessageHeader.Code.constructionIDNotFound, "constructionID不正确");
            }
            map.put("constructionID", constructionID);
        }

        String sceneID = request.getParameter("sceneID");
        if (StringUtils.isNotBlank(sceneID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("scene", sceneID))) {
                return MessagePacket.newFail(MessageHeader.Code.sceneIDNotFound, "sceneID不正确");
            }
            map.put("sceneID", sceneID);
        }
        String tenderID = request.getParameter("tenderID");
        if (StringUtils.isNotBlank(tenderID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("tender", tenderID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "tenderID不正确");
            }
            map.put("tenderID", tenderID);
        }
        String goodsTogetherDefineID = request.getParameter("goodsTogetherDefineID");
        if (StringUtils.isNotBlank(goodsTogetherDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogetherDefine", goodsTogetherDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "goodsTogetherDefineID不正确");
            }
            map.put("goodsTogetherDefineID", goodsTogetherDefineID);
        }
        String goodsTogetherID = request.getParameter("goodsTogetherID");
        if (StringUtils.isNotBlank(goodsTogetherID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogether", goodsTogetherID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "goodsTogetherID不正确");
            }
            map.put("goodsTogetherID", goodsTogetherID);
        }
        String goodsTogetherJoinID = request.getParameter("goodsTogetherJoinID");
        if (StringUtils.isNotBlank(goodsTogetherJoinID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("property", goodsTogetherJoinID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "goodsTogetherJoinID不正确");
            }
            map.put("goodsTogetherJoinID", goodsTogetherJoinID);
        }

        String goodsSecondDefineID = request.getParameter("goodsSecondDefineID");
        if (StringUtils.isNotBlank(goodsSecondDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsSecondDefine", goodsSecondDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "goodsSecondDefineID不正确");
            }
            map.put("goodsSecondDefineID", goodsSecondDefineID);
        }
        String goodsSecondID = request.getParameter("goodsSecondID");
        if (StringUtils.isNotBlank(goodsSecondID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsSecond", goodsSecondID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "goodsSecondID不正确");
            }
            map.put("goodsSecondID", goodsSecondID);
        }
        String goodsPriceDefineID = request.getParameter("goodsPriceDefineID"); // 特价定义ID
        if (StringUtils.isNotBlank(goodsPriceDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsPriceDefine", goodsPriceDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceDefineID 不正确");
            }
            map.put("goodsPriceDefineID", goodsPriceDefineID);
        }
        String goodsPriceID = request.getParameter("goodsPriceID"); // 商品特价ID
        if (StringUtils.isNotBlank(goodsPriceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsPrice", goodsPriceID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceID 不正确");
            }
            map.put("goodsPriceID", goodsPriceID);
        }
        String trainBusinessID = request.getParameter("trainBusinessID");
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainBusiness", trainBusinessID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "trainBusinessID不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        String trainCourseID = request.getParameter("trainCourseID");
        if (StringUtils.isNotBlank(trainCourseID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainCourse", trainCourseID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "trainCourseID不正确");
            }
            map.put("trainCourseID", trainCourseID);
        }
        String trainHourID = request.getParameter("trainHourID");
        if (StringUtils.isNotBlank(trainHourID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainHour", trainHourID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "trainHourID不正确");
            }
            map.put("trainHourID", trainHourID);
        }
        String memberTrainID = request.getParameter("memberTrainID");
        if (StringUtils.isNotBlank(memberTrainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberTrain", memberTrainID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberTrainID不正确");
            }
            map.put("memberTrainID", memberTrainID);
        }
        String memberBuyReadID = request.getParameter("memberBuyReadID");
        if (StringUtils.isNotBlank(memberBuyReadID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberBuyRead", memberBuyReadID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberBuyReadID不正确");
            }
            map.put("memberBuyReadID", memberBuyReadID);
        }
        String memberMajor = request.getParameter("memberMajor");
        if (StringUtils.isNotBlank(memberMajor)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberMajor", memberMajor))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberMajor不正确");
            }
            map.put("memberMajor", memberMajor);
        }
        String goldenGetID = request.getParameter("goldenGetID");
        if (StringUtils.isNotBlank(goldenGetID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goldenGet", goldenGetID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "goldenGetID不正确");
            }
            map.put("goldenGetID", goldenGetID);
        }
        String memberPrivilegeID = request.getParameter("memberPrivilegeID");
        if (StringUtils.isNotBlank(memberPrivilegeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("property", memberPrivilegeID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberPrivilegeID不正确");
            }
            map.put("memberPrivilegeID", memberPrivilegeID);
        }
        String hospitalDiagnosisID = request.getParameter("hospitalDiagnosisID");
        if (StringUtils.isNotBlank(hospitalDiagnosisID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("property", hospitalDiagnosisID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "hospitalDiagnosisID不正确");
            }
            map.put("hospitalDiagnosisID", hospitalDiagnosisID);
        }
        String buyCompanyID = request.getParameter("buyCompanyID");
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", buyCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "buyCompanyID不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }
        String buyShopID = request.getParameter("buyShopID");
        if (StringUtils.isNotBlank(buyShopID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("shop", buyShopID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "buyShopID不正确");
            }
            map.put("buyShopID", buyShopID);
        }
        String buyEmployeeID = request.getParameter("buyEmployeeID");
        if (StringUtils.isNotBlank(buyEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", buyEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "buyEmployeeID不正确");
            }
            map.put("buyEmployeeID", buyEmployeeID);
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberID不正确");
            }
            map.put("memberID", memberID);
        }
        String useEmployeeID = request.getParameter("useEmployeeID");
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", useEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "useEmployeeID不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        String useMemberID = request.getParameter("useMemberID");
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", useMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "useMemberID不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        String financeType = request.getParameter("financeType");
        if (StringUtils.isNotBlank(financeType)) {
            map.put("financeType", financeType);
        }

        String actionGoodsType = request.getParameter("actionGoodsType");
        if (StringUtils.isNotBlank(actionGoodsType)) {
            if (!NumberUtils.isNumeric(actionGoodsType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "actionGoodsType请输入数字");
            }
            map.put("actionGoodsType", Integer.valueOf(actionGoodsType));
        }
        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "outPayType请输入数字");
            }
            map.put("outPayType", Integer.valueOf(outPayType));
        }
        String buyType = request.getParameter("buyType");
        if (StringUtils.isNotBlank(buyType)) {
            if (!NumberUtils.isNumeric(buyType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "buyType请输入数字");
            }
            map.put("buyType", Integer.valueOf(buyType));
        }
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "orderType请输入数字");
            }
            map.put("orderType", Integer.valueOf(orderType));
        }

        String cancelConfirmUserID = request.getParameter("cancelConfirmUserID");
        if (StringUtils.isNotBlank(cancelConfirmUserID)) {
            if (!userService.checkUserId(cancelConfirmUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "cancelConfirmUserID不正确");
            }
            map.put("cancelConfirmUserID", cancelConfirmUserID);
        }
        String cancelConformMemberID = request.getParameter("cancelConformMemberID");
        if (StringUtils.isNotBlank(cancelConformMemberID)) {
            if (!memberService.checkMemberId(cancelConformMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "cancelConformMemberID不正确");
            }
            map.put("cancelConformMemberID", cancelConformMemberID);
        }
        String cancelConformEmployeeID = request.getParameter("cancelConformEmployeeID");
        if (StringUtils.isNotBlank(cancelConformEmployeeID)) {
            if (!employeeService.checkEmployeeID(cancelConformEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "cancelConformEmployeeID不正确");
            }
            map.put("cancelConformEmployeeID", cancelConformEmployeeID);
        }
        String presentRecordID = request.getParameter("presentRecordID");
        if (StringUtils.isNotBlank(presentRecordID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", presentRecordID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "presentRecordID不正确");
            }
            map.put("presentRecordID", presentRecordID);
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom请输入数字");
            }
            map.put("payFrom", Integer.valueOf(payFrom));
        }
        String payUserID = request.getParameter("payUserID");
        if (StringUtils.isNotBlank(payUserID)) {
            if (!userService.checkUserId(payUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "payUserID不正确");
            }
            map.put("payUserID", payUserID);
        }
        String payMemberID = request.getParameter("payMemberID");
        if (StringUtils.isNotBlank(payMemberID)) {
            if (!memberService.checkMemberId(payMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "payMemberID不正确");
            }
            map.put("payMemberID", payMemberID);
        }
        String payEmployeeID = request.getParameter("payEmployeeID");
        if (StringUtils.isNotBlank(payEmployeeID)) {
            if (!employeeService.checkEmployeeID(payEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "payEmployeeID不正确");
            }
            map.put("payEmployeeID", payEmployeeID);
        }
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDIsError, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String memberPaymentID = request.getParameter("memberPaymentID");
        if (StringUtils.isNotBlank(memberPaymentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPayment", memberPaymentID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberPaymentID不正确");
            }
            map.put("memberPaymentID", memberPaymentID);
        }
        String companyPaymentID = request.getParameter("companyPaymentID");
        if (StringUtils.isNotBlank(companyPaymentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("companyPayment", companyPaymentID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "companyPaymentID不正确");
            }
            map.put("companyPaymentID", companyPaymentID);
        }
        String payStatus = request.getParameter("payStatus");
        if (StringUtils.isNotBlank(payStatus)) {
            if (!NumberUtils.isNumeric(payStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payStatus请输入数字");
            }
            map.put("payStatus", Integer.valueOf(payStatus));
        }
        String returnType = request.getParameter("returnType");
        if (StringUtils.isNotBlank(returnType)) {
            if (!NumberUtils.isNumeric(returnType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "returnType请输入数字");
            }
            map.put("returnType", Integer.valueOf(returnType));
        }
        String returnPaywayID = request.getParameter("returnPaywayID");
        if (StringUtils.isNotBlank(returnPaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", returnPaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "returnPaywayID不正确");
            }
            map.put("returnPaywayID", returnPaywayID);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            if (!NumberUtils.isNumeric(sendType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendType请输入数字");
            }
            map.put("sendType", Integer.valueOf(sendType));
        }
        String sendUrgencyType = request.getParameter("sendUrgencyType");
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendUrgencyType请输入数字");
            }
            map.put("sendUrgencyType", Integer.valueOf(sendUrgencyType));
        }
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
        }

        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberInvoiceDefine", memberInvoiceDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }
        String invoiceID = request.getParameter("invoiceID");
        if (StringUtils.isNotBlank(invoiceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("invoice", invoiceID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "invoiceID不正确");
            }
            map.put("invoiceID", invoiceID);
        }
        String invoiceStatus = request.getParameter("invoiceStatus");
        if (StringUtils.isNotBlank(invoiceStatus)) {
            if (!NumberUtils.isNumeric(invoiceStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "invoiceStatus请填写数字");
            }
            map.put("invoiceStatus", Integer.valueOf(invoiceStatus));
        }
        String processUserID = request.getParameter("processUserID");
        if (StringUtils.isNotBlank(processUserID)) {
            if (!userService.checkUserId(processUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "processUserID不正确");
            }
            map.put("processUserID", processUserID);
        }
        String processMemberID = request.getParameter("processMemberID");
        if (StringUtils.isNotBlank(processMemberID)) {
            if (!memberService.checkMemberId(processMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "processMemberID不正确");
            }
            map.put("processMemberID", processMemberID);
        }
        String processEmployeeID = request.getParameter("processEmployeeID");
        if (StringUtils.isNotBlank(processEmployeeID)) {
            if (!employeeService.checkEmployeeID(processEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "processEmployeeID不正确");
            }
            map.put("processEmployeeID", processEmployeeID);
        }
        String weighedUserID = request.getParameter("weighedUserID");
        if (StringUtils.isNotBlank(weighedUserID)) {
            if (!userService.checkUserId(weighedUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "weighedUserID不正确");
            }
            map.put("weighedUserID", weighedUserID);
        }
        String outUserID = request.getParameter("outUserID");
        if (StringUtils.isNotBlank(outUserID)) {
            if (!userService.checkUserId(outUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "outUserID不正确");
            }
            map.put("outUserID", outUserID);
        }
        String outMemberID = request.getParameter("outMemberID");
        if (StringUtils.isNotBlank(outMemberID)) {
            if (!memberService.checkMemberId(outMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "outMemberID不正确");
            }
            map.put("outMemberID", outMemberID);
        }
        String outEmployeeID = request.getParameter("outEmployeeID");
        if (StringUtils.isNotBlank(outEmployeeID)) {
            if (!employeeService.checkEmployeeID(outEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "outEmployeeID不正确");
            }
            map.put("outEmployeeID", outEmployeeID);
        }
        String expressID = request.getParameter("expressID");
        if (StringUtils.isNotBlank(expressID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("express", expressID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "expressID不正确");
            }
            map.put("expressID", expressID);
        }
        String sendUserID = request.getParameter("sendUserID");
        if (StringUtils.isNotBlank(sendUserID)) {
            if (!userService.checkUserId(sendUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "sendUserID不正确");
            }
            map.put("sendUserID", sendUserID);
        }
        String sendoutMemberID = request.getParameter("sendoutMemberID");
        if (StringUtils.isNotBlank(sendoutMemberID)) {
            if (!memberService.checkMemberId(sendoutMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "sendoutMemberID不正确");
            }
            map.put("sendoutMemberID", sendoutMemberID);
        }
        String sendEmployeeID = request.getParameter("sendEmployeeID");
        if (StringUtils.isNotBlank(sendEmployeeID)) {
            if (!employeeService.checkEmployeeID(sendEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "sendEmployeeID不正确");
            }
            map.put("sendEmployeeID", sendEmployeeID);
        }
        String sendPayType = request.getParameter("sendPayType");
        if (StringUtils.isNotBlank(sendPayType)) {
            if (!NumberUtils.isNumeric(sendPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendPayType请填写数字");
            }
            map.put("sendPayType", Integer.valueOf(sendPayType));
        }
        String sendStatus = request.getParameter("sendStatus");
        if (StringUtils.isNotBlank(sendStatus)) {
            if (!NumberUtils.isNumeric(sendStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendStatus请填写数字");
            }
            map.put("sendStatus", Integer.valueOf(sendStatus));
        }
        String sendCompanyID = request.getParameter("sendCompanyID");
        if (StringUtils.isNotBlank(sendCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", sendCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "sendCompanyID不正确");
            }
            map.put("sendCompanyID", sendCompanyID);
        }
        String sendMemberID = request.getParameter("sendMemberID");
        if (StringUtils.isNotBlank(sendMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", sendMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "sendMemberID不正确");
            }
            map.put("sendMemberID", sendMemberID);
        }
        String payINType = request.getParameter("payINType");
        if (StringUtils.isNotBlank(payINType)) {
            if (!NumberUtils.isNumeric(payINType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payINType请填写数字");
            }
            map.put("payINType", Integer.valueOf(payINType));
        }

        String payINuserID = request.getParameter("payINuserID");
        if (StringUtils.isNotBlank(payINuserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", payINuserID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "payINuserID不正确");
            }
            map.put("payINuserID", payINuserID);
        }

        String confirmPayUserID = request.getParameter("confirmPayUserID");
        if (StringUtils.isNotBlank(confirmPayUserID)) {
            if (!userService.checkUserId(confirmPayUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "confirmPayUserID不正确");
            }
            map.put("confirmPayUserID", confirmPayUserID);
        }

        String confirmPayMemberID = request.getParameter("confirmPayMemberID");
        if (StringUtils.isNotBlank(confirmPayMemberID)) {
            if (!memberService.checkMemberId(confirmPayMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "confirmPayMemberID不正确");
            }
            map.put("confirmPayMemberID", confirmPayMemberID);
        }

        String confirmPayEmployeeID = request.getParameter("confirmPayEmployeeID");
        if (StringUtils.isNotBlank(confirmPayEmployeeID)) {
            if (!employeeService.checkEmployeeID(confirmPayEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "confirmPayEmployeeID不正确");
            }
            map.put("confirmPayEmployeeID", confirmPayEmployeeID);
        }
        String getGoodsUserID = request.getParameter("getGoodsUserID");
        if (StringUtils.isNotBlank(getGoodsUserID)) {
            if (!userService.checkUserId(getGoodsUserID)) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "getGoodsUserID不正确");
            }
            map.put("getGoodsUserID", getGoodsUserID);
        }

        String getGoodsMemberID = request.getParameter("getGoodsMemberID");
        if (StringUtils.isNotBlank(getGoodsMemberID)) {
            if (!memberService.checkMemberId(getGoodsMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "getGoodsMemberID不正确");
            }
            map.put("getGoodsMemberID", getGoodsMemberID);
        }

        String getGoodsEmployeeID = request.getParameter("getGoodsEmployeeID");
        if (StringUtils.isNotBlank(getGoodsEmployeeID)) {
            if (!employeeService.checkEmployeeID(getGoodsEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "getGoodsEmployeeID不正确");
            }
            map.put("getGoodsEmployeeID", getGoodsEmployeeID);
        }

        String status = request.getParameter("status");
        if (StringUtils.isNotBlank(status)) {
            if (!NumberUtils.isNumeric(status)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "status请填写数字");
            }
            map.put("status", Integer.valueOf(status));
        }
        String isLock = request.getParameter("isLock");
        if (StringUtils.isNotBlank(isLock)) {
            if (!NumberUtils.isNumeric(isLock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isLock请填写数字");
            }
            map.put("isLock", Integer.valueOf(isLock));
        }
        String isValid = request.getParameter("isValid");
        if (StringUtils.isNotBlank(isValid)) {
            if (!NumberUtils.isNumeric(isValid)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isValid请填写数字");
            }
            map.put("isValid", Integer.valueOf(isValid));
        } else {
            map.put("isValid", 1);
        }
        String isMemberHide = request.getParameter("isMemberHide");
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide请填写数字");
            }
            map.put("isMemberHide", Integer.valueOf(isMemberHide));
        }
        String beginPrice = request.getParameter("beginPrice");
        if (StringUtils.isNotBlank(beginPrice)) {
            if (!NumberUtils.isNumeric(beginPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "beginPrice请填写数字");
            }
            map.put("beginPrice", Double.valueOf(beginPrice));
        }
        String endPrice = request.getParameter("endPrice");
        if (StringUtils.isNotBlank(endPrice)) {
            if (!NumberUtils.isNumeric(endPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "endPrice请填写数字");
            }
            map.put("endPrice", Double.valueOf(endPrice));
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) map.put("sortTypeName", sortTypeName);
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) map.put("sortTypeTime", sortTypeTime);
        String sortTypePrice = request.getParameter("sortTypePrice");
        if (StringUtils.isNotBlank(sortTypePrice)) map.put("sortTypePrice", sortTypePrice);
        String sortTypeStatus = request.getParameter("sortTypeStatus");
        if (StringUtils.isNotBlank(sortTypeStatus)) map.put("sortTypeStatus", sortTypeStatus);
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            map.put("keyWords", "%" + keyWords + "%");
        }
        Page page = HttpServletHelper.assembyPage(request);        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<OutSystemMemberOrderList> list = memberOrderService.outSystemGetMemberOrderList(map);
        if (list != null && !list.isEmpty()) {
            PageInfo<OutSystemMemberOrderList> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
            for (OutSystemMemberOrderList obj : list) {
                map.clear();
                map.put("memberOrderID", obj.getMemberOrderID());
                map.put("bodyType", 1);
                obj.setHasRealGoods(goodsShopService.getHasGoodsByMap(map));
                map.put("bodyType", 2);
                obj.setHasVirtualGoods(goodsShopService.getHasGoodsByMap(map));
            }
        }
//        if (StringUtils.isNotBlank(recommendCode)) {
//            if (!list.isEmpty() && list.size() > 0) {
//                for (int i = 0; i < list.size(); i++) {
//                    // 找到商品
//                    List<MemberOrderGoods> memberOrderGoodsList = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(list.get(i).getMemberOrderID());
//                    if (!memberOrderGoodsList.isEmpty() && memberOrderGoodsList.size() > 0) {
//                        String goodsShopID = memberOrderGoodsList.get(0).getGoodsShopID();
//                        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
//                        list.get(i).setGoodsShopID(goodsShopID);
//                        list.get(i).setGoodsShopName(goodsShop.getName());
//                    }
//                    String recommendMemberID1=list.get(i).getRecommendMemberID();
//                    Double price=list.get(i).getPriceAfterDiscount();
//                    if (StringUtils.isNotBlank(recommendMemberID1)){
//                        map.put("memberID",recommendMemberID1);
//                        map.put("majorID","8a2f462a78ecd2fa0178eedb3ed4057b");
//                        List<MyMemberMajorListDto> memberMajorListDtos=memberMajorService.getMemberMajorList(map);
//                        if (!memberMajorListDtos.isEmpty()&&memberMajorListDtos.size()>0){
//                            list.get(i).setRecommendPrice(price*0.3);
//                        }else {
//                            list.get(i).setRecommendPrice(price*0.1);
//                        }
//                    }
//                }
//            }
//        }
        if (user != null) {
            String descriptions = String.format("外部系统获取会员订单列表");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberOrderList",
                    DataLog.objectDefineID.memberOrder.getOname(), null, null, DataLog.OperateType.LIST, request);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberOrderList success");
    }

    //外部系统获取会员订单详细信息
    @ApiOperation(value = "outSystemGetOneMemberOrderDetail", notes = "独立接口:外部系统获取一个会员订单详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetOneMemberOrderDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetOneMemberOrderDetail(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        MemberOrderDto memberOrderDto = memberOrderService.getMemberOrderById(memberOrderID);
        if (user != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统获取一个会员订单详细信息");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetOneMemberOrderDetail",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.DETAIL, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", memberOrderDto);
        return MessagePacket.newSuccess(rsMap, "outSystemGetOneMemberOrderDetail success");
    }

    //外部系统删除一个会员订单
    @ApiOperation(value = "outSystemDeleteOneMemberOrder", notes = "独立接口:外部系统删除一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemDeleteOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemDeleteOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
        map.put("id", memberOrderID);
        map.put("isValid", "0");
        memberOrderService.outSystemUpdateMemberOrder(map);
        if (user != null) {
            String descriptions = String.format("外部系统删除一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemDeleteOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.DELETE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("deleteTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemDeleteOneMemberOrder success");
    }

    //外部系统会员订单处理  接单
    @ApiOperation(value = "outSystemProcessOneMemberOrder", notes = "独立接口：外部系统接单一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemProcessOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemProcessOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        UserLogin userLogin = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
        userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (userLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("processUserID", userLogin.getUserID());
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null || memberOrder.getStatus() != 4) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "订单状态不是付款订单，不能接单");
        }
        String processEmployeeID = request.getParameter("processEmployeeID");
        if (StringUtils.isNotBlank(processEmployeeID)) {
            if (!employeeService.checkEmployeeID(processEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "processEmployeeID不正确");
            }
            map.put("processEmployeeID", processEmployeeID);
        }
        map.put("id", memberOrderID);
        map.put("processUserID", userLogin.getUserID());
        map.put("processEmployeeID", userLogin.getEmployeeID());
        map.put("processMemberID", userLogin.getMemberID());
        map.put("processTime", TimeTest.getTime());
        map.put("status", 10);
        memberOrderService.update(map);
        if (userLogin != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统接单一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemProcessOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("processTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemProcessOneMemberOrder success");
    }

    @ApiOperation(value = "outSystemFinishOneMemberOrder", notes = "独立接口：外部系统完成一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemFinishOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemFinishOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("modifier", user.getId());
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);
        map.put("status", 9);
        memberOrderService.update(map);
        if (user != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统完成一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemFinishOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.CLOSE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("finishTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemFinishOneMemberOrder success");
    }

    //外部系统  会员订单发货
    @ApiOperation(value = "outSystemSendOneMemberOrder", notes = "独立接口:外部系统发货一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemSendOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemSendOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("sendUserID", user.getId());
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        String sendEmployeeID = request.getParameter("sendEmployeeID");
        if (StringUtils.isNotBlank(sendEmployeeID)) {
            if (!employeeService.checkEmployeeID(sendEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "sendEmployeeID不正确");
            }
            map.put("sendEmployeeID", sendEmployeeID);
        }
        String expressID = request.getParameter("expressID");
        if (StringUtils.isNotBlank(expressID)) {
            if (!expressService.checkIsValid(expressID)) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDFound, "expressID不正确");
            }
            map.put("expressID", expressID);
        }
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
            //再从memberAddressID获取name放进memberOrder表里的memberAddressName
            MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
            map.put("memberAddressName", memberAddress.getName());
        }

        String contactName = request.getParameter("contactName");
        if (StringUtils.isNotBlank(contactName)) {
            map.put("contactName", contactName);
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            if (!NumberUtils.isNumeric(phone)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "phone请输入的数字");
            }
            map.put("phone", phone);
        }
        String sendCode = request.getParameter("sendCode");
        if (StringUtils.isNotBlank(sendCode)) map.put("sendCode", sendCode);
        map.put("id", memberOrderID);
        map.put("sendTime", TimeTest.getTime());
        map.put("status", 6);
        memberOrderService.update(map);

        if (user != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统发货一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemSendOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sendTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemSendOneMemberOrder success");
    }


    //外部系统  会员订单确认收款
    @ApiOperation(value = "outSystemConfirmPayOneMemberOrder", notes = "独立接口:外部系统一个会员订单确认收款")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemConfirmPayOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemConfirmPayOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }

        String confirmPayEmployeeID = request.getParameter("confirmPayEmployeeID");
        if (StringUtils.isNotBlank(confirmPayEmployeeID)) {
            if (!employeeService.checkEmployeeID(confirmPayEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "confirmPayEmployeeID不正确");
            }
            map.put("confirmPayEmployeeID", confirmPayEmployeeID);
        }

        String paySequence = request.getParameter("paySequence");
        if (StringUtils.isNotBlank(paySequence)) {
            map.put("paySequence", paySequence);
        }

        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }

        String nowTime = TimeTest.getTimeStr();
        String payTime = request.getParameter("payTime");
        if (StringUtils.isNotBlank(payTime)) {
            map.put("payTime", payTime);
        } else {
            map.put("payTime", nowTime);
        }

        map.put("modifier", user.getId());
        map.put("id", memberOrderID);
        map.put("confirmPayTime", nowTime);
        map.put("confirmPayUserID", user.getId());
        map.put("payINuserID", user.getId());
        map.put("status", 4);
        map.put("payStatus", 2);

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID 不正确");
        }

//        原来的代码保持不变，增加 1个 orderType=33 的判断处理逻辑。
//        在 memberOrderGoods里面 获取到 goodsShopID，获取到 QTY，例如 3个表示他买了 3个周期，获取salesUnitQTY 例如，
//        表示他买了2个 机器。获取里面的周期类型 和周期个数，根据周期时间和个数，以及 periodQTY ，
//        计算结束时间。例如，周期类型=月，周期个数=6 表示 每个商品里面包含6个月，QTY=3 则表示他 买了 18个月。
//        到 computerUnit 里面 查询 goodsShopID 相同，并且currentMemberID为空，状态=1 空闲的记录。
//        按照orderSeq 从小到达排序， 设置前面 salesUnitQTY 个 的 currentMemberID=当前会员，状态=5 正在使用，
//        并且去创建1个 computerRecord 记录，和 computerUnit相同的字段都复制过来，计划和实际，收益开始时间=现在，3个结束时间 =
//        根据 周期个数X 周期时间X QTY 。会员ID=当前会员ID，会员订单ID，会员订单状态，状态=2.
//
        if (memberOrder.getOrderType() != null && memberOrder.getOrderType() == 33) {
            List<MemberOrderGoodsList> mogs = memberOrderGoodsService.selectByMemberOrderID(memberOrderID);
            if (mogs.isEmpty()) {
                return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "该订单下没有商品");
            }
            for (MemberOrderGoodsList mo : mogs) {
                ModelMap computerRecordMap = new ModelMap();
                if (mo.getPeriodType() == null) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "该订单商品没有周期类型");
                }
                if (mo.getQTY() == null || mo.getQTY() == 0 || mo.getPeriodNumber() == null || mo.getPeriodNumber() == 0) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "该订单商品的订购数量或者周期个数不能为空或者为0");
                }
                int number = mo.getQTY() * mo.getPeriodNumber();
                if (mo.getPeriodType() == 1) {
                    computerRecordMap.put("endTime", DateUtil.addToDay(new Date(), number));
                    computerRecordMap.put("gainEndTime", DateUtil.addToDay(new Date(), number));
                } else if (mo.getPeriodType() == 2) {
                    computerRecordMap.put("endTime", DateUtil.addToMonth(new Date(), number));
                    computerRecordMap.put("gainEndTime", DateUtil.addToMonth(new Date(), number));
                } else if (mo.getPeriodType() == 3) {
                    computerRecordMap.put("endTime", DateUtil.addToYear(new Date(), number));
                    computerRecordMap.put("gainEndTime", DateUtil.addToYear(new Date(), number));
                } else if (mo.getPeriodType() == 10) {
                    //次目前不知道怎么算
                }
                if (mo.getSalesUnitQTY() == null || mo.getSalesUnitQTY() == 0) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "该商品的销售数量为空或者0");
                }
                if (mo.getGoodsShopID() == null) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "该订单商品没有goodsShopID");
                }
                if (memberOrder.getMemberID() == null) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "该订单没有购买会员");
                }
                List<OutSystemComputerUnitList> computerUnits = computerUnitService.selectByComputerUnit(mo.getGoodsShopID());
                if (computerUnits.isEmpty() || computerUnits.size() < mo.getSalesUnitQTY()) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "没有空闲的计算机单元");
                }
                memberOrderService.update(map);
                for (int i = 0; i < mo.getSalesUnitQTY(); i++) {
                    OutSystemComputerUnitList computerUnit = computerUnits.get(i);
                    ModelMap computerUnitMap = new ModelMap();
                    if (StringUtils.isNotBlank(memberOrder.getMemberID())) {
                        computerUnitMap.put("currentMemberID", memberOrder.getMemberID());
                    }
                    computerUnitMap.put("status", 5);
                    computerUnitMap.put("id", computerUnit.getComputerUnitID());
                    computerUnitService.updateByMap(computerUnitMap);
                    if (computerUnit.getApplicationID() != null) {
                        computerRecordMap.put("applicationID", computerUnit.getApplicationID());
                    }
                    if (computerUnit.getCompanyID() != null) {
                        computerRecordMap.put("companyID", computerUnit.getCompanyID());
                    }
                    if (computerUnit.getServiceCompanyID() != null) {
                        computerRecordMap.put("serviceCompanyID", computerUnit.getServiceCompanyID());
                    }
                    if (computerUnit.getCustomerCompanyID() != null) {
                        computerRecordMap.put("customerCompanyID", computerUnit.getCustomerCompanyID());
                    }
                    if (computerUnit.getComputerID() != null) {
                        computerRecordMap.put("computerID", computerUnit.getComputerID());
                    }
                    if (computerUnit.getComputerUnitID() != null) {
                        computerRecordMap.put("computerUnitID", computerUnit.getComputerUnitID());
                    }
                    if (computerUnit.getName() != null) {
                        computerRecordMap.put("name", computerUnit.getName());
                    }
                    if (computerUnit.getSourceID() != null) {
                        computerRecordMap.put("sourceID", computerUnit.getSourceID());
                    }
                    if (computerUnit.getSourceAccountID() != null) {
                        computerRecordMap.put("sourceAccountID", computerUnit.getSourceAccountID());
                    }
                    computerRecordMap.put("id", IdGenerator.uuid32());
                    computerRecordMap.put("memberOrderID", memberOrderID);
                    computerRecordMap.put("memberOrderStatus", 2);
                    computerRecordMap.put("status", 1);
                    computerRecordMap.put("planBeginTime", TimeTest.getTime());
                    computerRecordMap.put("beginTime", TimeTest.getTime());
                    if (StringUtils.isNotBlank(memberOrder.getMemberID())) {
                        computerRecordMap.put("memberID", memberOrder.getMemberID());
                    }
                    if (user.getEmployeeID() != null) {
                        computerRecordMap.put("employeeID", user.getEmployeeID());
                    }
                    if (user.getId() != null) {
                        computerRecordMap.put("userID", user.getId());
                    }
                    computerRecordService.insertByMap(computerRecordMap);
                }
            }

        }


        if (user != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统一个会员订单确认收款");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemConfirmPayOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("confirmPayTime", nowTime);
        return MessagePacket.newSuccess(rsMap, "outSystemConfirmPayOneMemberOrder success");
    }

    //外部系统 会员订单 同意退货
    @ApiOperation(value = "outSystemReturnGoodsOneMemberOrder", notes = "独立接口:外部系统同意退货一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemReturnGoodsOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemReturnGoodsOneMemberOrder(HttpServletRequest request, ModelMap map) throws IllegalAccessException {
        String outToken = request.getParameter("outToken");
        User user = null;
        UserLogin userLogin = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
        userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        String returnCode = request.getParameter("returnCode");
        if (StringUtils.isNotBlank(returnCode)) {
            map.put("returnCode", returnCode);
        }
        String memberPaymentID = request.getParameter("memberPaymentID");
        if (!memberPaymentService.checkIdIsValid(memberPaymentID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberPayIDNotFound, "memberPaymentID不正确");
        }

        //需要退款的金额
//        String amount = request.getParameter("amount");
//        log.info("log"+">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>退款！！！！！！！！！！！！！" + amount);
//        ApiMemberOrderOut apiMemberOrderOut = memberOrderService.selectMemberOrderByOut(memberOrderID);
        OutMemberPaymentDto apiMemberOrderOut = memberPaymentService.getOneMemberPaymentDetailByID(memberPaymentID);
        Double amount = apiMemberOrderOut.getAmount();

        if (apiMemberOrderOut == null) {
            log.info("memberOrderID未查询到订单");
            return MessagePacket.newFail(MessageHeader.Code.outMemberOrderIDNotFound, "memberOrderID未查询到订单");
        }
        if (StringUtils.isEmpty(apiMemberOrderOut.getMemberID())) {
            log.info("订单状态异常，无支付会员ID");
            return MessagePacket.newFail(MessageHeader.Code.memberOrderException, "订单状态异常，无支付会员ID");
        }
        Member member = memberService.selectById(apiMemberOrderOut.getMemberID());
        if (member == null) {
            log.info("订单状态异常，无支付会员");
            return MessagePacket.newFail(MessageHeader.Code.memberOrderException, "订单状态异常，无支付会员");
        }
//        if (apiMemberOrderOut.getStatus() != 70) {
//            log.info("log"+"订单状态异常1");
//            return MessagePacket.newFail(MessageHeader.Code.memberOrderException, "订单状态异常");
//        }

//        if (StringUtils.isEmpty(amount)) {
//            log.info("log"+"amount不能为空");
//            return MessagePacket.newFail(MessageHeader.Code.amountNotNull, "amount不能为空");
//        }
//        Integer status = memberOrderService.getOutMemberDetail(memberOrderID);
//        MemberOrderDto memberOrderById = memberOrderService.getMemberOrderById(apiMemberOrderOut.getMemberOrderID());
        MemberOrderDto memberOrderById = memberOrderService.getMemberOrderById(memberOrderID);
        Integer status = memberOrderById.getStatus();
        log.info("status=============" + status);
        if (!status.equals(70)) {
            log.info("订单未完成支付，无法退款");

            return MessagePacket.newFail(MessageHeader.Code.fail, "订单未完成支付，无法退款");
        }
//        Double payTotal = memberOrderService.getRealPayByOutMemberID(memberOrderID);
//        Double payTotal = memberOrderById.getPayTotal();
//        log.info("log"+">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>退款payTotal！！！！！！！！！！！！！" + payTotal);
//        log.info("log"+">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>amount退款！！！！！！！！！！！！！" + amount);
//        if (!amount.equals(payTotal)) {
//            log.info("log"+"退款金额与实际付款金额不匹配");
//
//            return MessagePacket.newFail(MessageHeader.Code.fail, "退款金额与实际付款金额不匹配");
//        }
//        OutMemberPaymentDto outMemberPaymentDto = memberPaymentService.getOneMemberPaymentDetailByID(memberPaymentID);

        //根据支付方式执行退款操作，同时产生资金变动记录（刚哥）
        log.info("apiMemberOrderOut============" + apiMemberOrderOut.toString());
        if (memberOrderService.getMemberOrderById(memberOrderID).getPayFrom() == null) {
            log.info("订单状态异常，无支付方式");

            return MessagePacket.newFail(MessageHeader.Code.memberOrderException, "订单状态异常，无支付方式");
        }
        MemberBalanceRequestBase base = null;
        switch (memberOrderService.getMemberOrderById(memberOrderID).getPayFrom()) {
            //订单为钱包支付，需要走钱包退款
            case 1:
                base = MemberBalanceRequestBase.BALANCE()
                        .amount(new BigDecimal(amount))
                        .applicationId(userLogin.getApplicationID())
                        .operation(String.format("%s,订单退款钱包", userLogin.getMemberName()))
                        .name(String.format("%s,订单退款钱包", userLogin.getMemberName()))
                        .companyId(userLogin.getCompanyID())
                        .balanceType(999)
                        .type(18)
                        .fee(new BigDecimal(0))
                        .siteId(memberService.selectById(userLogin.getMemberID()).getSiteID())
                        .memberId(userLogin.getMemberID())
                        .objectDefineId(Config.MEMBERORDER_OBJECTDEFINEID)
                        .objectId(apiMemberOrderOut.getObjectID())
                        .objectName(apiMemberOrderOut.getName())
                        .paySequence(IdGenerator.uuid32())
                        .build();
                sendMessageService.sendMemberBalanceMessage(JacksonHelper.toJson(base)); break;
                //订单为第三方支付（目前只支持微信支付）
            case 2:
                if (StringUtils.isEmpty(apiMemberOrderOut.getPaywayID())) {
                    return MessagePacket.newFail(MessageHeader.Code.fail, "订单没有支付机构ID");
                }
                PayWay payway = paywayService.findById(apiMemberOrderOut.getPaywayID());
                if (payway == null) {
                    return MessagePacket.newFail(MessageHeader.Code.fail, "支付机构不存在");
                }
                map.put("id", memberOrderID);
                map.put("status", 5);
                if (StringUtils.isEmpty(payway.getReturnURL())) {
                    return MessagePacket.newFail(MessageHeader.Code.fail, "证书地址为空");
                }
                RefundInfo refundInfo = new RefundInfo();
                //appid
                refundInfo.setAppid(payway.getServerPassword());

                //商户号
                refundInfo.setMch_id(payway.getPartnerID());
                refundInfo.setNonce_str(TransferUtils.buildRandom());
                //微信支付成功后的流水号
                refundInfo.setTransaction_id(apiMemberOrderOut.getPaySequence());
                log.info("transaction_id>>>>>>>>>>>>>>>>>" + apiMemberOrderOut.getPaySequence());
                //退款唯一流水号，需要记录，发起退款失败后还是继续使用这个，直到退款成功，每个唯一流水号只能用一次
//                    refundInfo.setOut_refund_no(apiMemberOrderOut.getId());
                refundInfo.setOut_refund_no(apiMemberOrderOut.getMemberPaymentID());   //支付总金额（单位分，需要把元转分）
//                    int total_fee = (int) (apiMemberOrderOut.getPayTotal() * 100);
//                int total_fee = (int) (memberOrderService.getMemberOrderById(memberOrderID).getPayTotal() * 100);
                int total_fee = BigDecimal.valueOf(memberOrderService.getMemberOrderById(memberOrderID).getPayTotal())
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP).intValue();
//                    int total_fee = (int) (memberOrderService.getMemberOrderById(apiMemberOrderOut.getMemberOrderID()).getPayTotal() * 100);
                refundInfo.setTotal_fee(total_fee);
                //本次退款金额，所有退款金额加起来不能大于支付总金额（单位分，需要把元转分）
                refundInfo.setRefund_fee(total_fee);
                //退款回调地址
                String requestURL = String.valueOf(request.getRequestURL());
                log.info("requestURL==============" + requestURL);
                String url = requestURL.substring(0, requestURL.lastIndexOf("/"));
                String notify_url = url + "/notifyUrl.json";
//                                                String url1=requestURL.replace("outSystemReturnGoodsOneMemberOrder.json","");
                String url2 = notify_url.replace("http", "https");
//                    notify_url=notify_url.replace("http","https");
                String orderNotifyURL = payway.getOrderNotifyURL();
                log.info("orderNotifyURL==============" + orderNotifyURL);
                log.info("notify_url==============" + url2);
//
//                    refundInfo.setNotify_url(orderNotifyURL);
//                    refundInfo.setCallback_type(2);
                Container container = new Container();
                container.setPath(url2);
                container.setService("pay");
                refundInfo.setContainer(container);
                refundInfo.setNotify_url(url2);
//                    refundInfo.set(orderNotifyURL);container
                apiV3key = payway.getPrivateKey();
                //签名
                refundInfo.setSign(WechatPayUtils.createSign(MapUtil.beanToMap(refundInfo), payway.getPrivateKey()));
                String result = "";

                //无论微信支付退款如何，都设置 状态= 已退
                //在微信回调里面，如果 退款失败了，再 把状态修改为 申请退款
//                    map.put("id", memberOrderID);
//                    map.put("status", 7);
//                    map.put("returnGoodsTime", TimeTest.getTime());
//                    map.put("returnGoodsUserID", userLogin.getUserID());
//                    map.put("returnGoodsEmployeeID", userLogin.getEmployeeID());
//                    map.put("returnGoodsMemberID", userLogin.getMemberID());
//                    memberOrderService.update(map);

                try {
                    result = TransferUtils.doRefund(XStreamUtil.getXstream(RefundInfo.class).toXML(refundInfo), payway.getPartnerID(), payway.getReturnURL());
                    log.info("result》》》》》》" + result);
                    RefundReturnInfo refundReturnInfo = (RefundReturnInfo) XStreamUtil.getXstream(RefundReturnInfo.class).fromXML(result);
//                        log.info("refundReturnInfo》》》》》》" + result);
                    if (null != refundReturnInfo) {
                        map.put("id", memberOrderID);
                        map.put("status", 5);
                        map.put("returnAmount", apiMemberOrderOut.getReturnAmount());
                        map.put("returnPaywayID", apiMemberOrderOut.getPaywayID());
                        map.put("returnTime", new Timestamp(System.currentTimeMillis()));
                        map.put("returnType", 1);


//                            String requestURL = String.valueOf(request.getRequestURL());
//                            log.info("requestURL=============="+requestURL);
//                            String url1=requestURL.replace("outSystemReturnGoodsOneMemberOrder.json","");
//                            String url2=url1.replace("http","https");
//                            String HTTPURL=url2+"notifyUrl";

//                            HttpPost httpPost = new HttpPost(orderNotifyURL);
////                            HttpPost httpPost = new HttpPost(orderNotifyURL);
//                            log.info("httpPost=============="+httpPost);
//                            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
                        String jsonString = JSON.toJSONString(refundReturnInfo);
                        log.info("jsonString==============" + jsonString);
//                            StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
//                            httpPost.setEntity(requestEntity);
//                            CloseableHttpClient httpClient = HttpClients.createDefault();
//                            CloseableHttpResponse response = httpClient.execute(httpPost);
//                            log.info("response============"+response);
                        memberOrderService.update(map);

                        return MessagePacket.newFail(MessageHeader.Code.success, "退款成功");
                    } else {
                        return MessagePacket.newFail(MessageHeader.Code.fail, refundReturnInfo.getReturn_msg());
                    }
                } catch (Exception e) {
                    //在微信回调里面，如果 退款失败了，再 把状态修改为 申请退款，目前是14退款中
//                        map.put("id", memberOrderID);
//                        map.put("status", 14);
//                        map.put("returnGoodsTime", TimeTest.getTime());
//                        map.put("returnGoodsUserID", userLogin.getUserID());
//                        map.put("returnGoodsEmployeeID", userLogin.getEmployeeID());
//                        map.put("returnGoodsMemberID", userLogin.getMemberID());
//                        memberOrderService.update(map);
                    log.error("log",e);
                } break;
                //订单为礼品卡支付，需要走礼品卡退款
            case 7:
                base = MemberBalanceRequestBase.BALANCE()
                        .amount(new BigDecimal(amount))
                        .memberId(member.getId())
                        .objectId(apiMemberOrderOut.getObjectID())
                        .objectDefineId(Config.MEMBERORDER_OBJECTDEFINEID)
                        .objectName(apiMemberOrderOut.getName())
                        .applicationId(member.getApplicationID())
                        .objectCode(apiMemberOrderOut.getObjectCode())
                        .fee(new BigDecimal(0))
                        .type(101)
                        .balanceType(999)
                        .paySequence(IdGenerator.uuid32())
                        .name(String.format("%s,订单退款礼品卡", member.getName()))
                        .operation(String.format("%s,订单退款礼品卡", member.getName()))
                        .build();
                sendMessageService.sendMemberBalanceMessage(JacksonHelper.toJson(base));
                break;
        }

//        switch (memberOrderService.getMemberOrderById(apiMemberOrderOut.getMemberOrderID()).getOutPayType()) {
//        switch (memberOrderService.getMemberOrderById(memberOrderID).getOutPayType()) {
//
//            //订单微信支付
//            case 1:
//                if (StringUtils.isEmpty(apiMemberOrderOut.getPaywayID())) {
//                    return MessagePacket.newFail(MessageHeader.Code.fail, "订单没有支付机构ID");
//                }
//                PayWay payway = paywayService.findById(apiMemberOrderOut.getPaywayID());
//                if (payway == null) {
//                    return MessagePacket.newFail(MessageHeader.Code.fail, "支付机构不存在");
//                }
//                if (StringUtils.isEmpty(payway.getReturnURL())) {
//                    return MessagePacket.newFail(MessageHeader.Code.fail, "证书地址为空");
//                }
//                RefundInfo refundInfo = new RefundInfo();
//                //appid
//                refundInfo.setAppid(payway.getServer密码());
//                //商户号
//                refundInfo.setMch_id(payway.getPartnerID());
//                refundInfo.setNonce_str(TransferUtils.buildRandom());
//                //微信支付成功后的流水号
//                refundInfo.setTransaction_id(apiMemberOrderOut.getPaySequence());
//                log.info("transaction_id>>>>>>>>>>>>>>>>>" + apiMemberOrderOut.getPaySequence());
//                //退款唯一流水号，需要记录，发起退款失败后还是继续使用这个，直到退款成功，每个唯一流水号只能用一次
//                refundInfo.setOut_refund_no(apiMemberOrderOut.getMemberID());
//                //支付总金额（单位分，需要把元转分）
//                int total_fee = (int) (memberOrderService.getMemberOrderById(memberOrderID).getPayTotal() * 100);
////                int total_fee = (int) (memberOrderService.getMemberOrderById(apiMemberOrderOut.getMemberOrderID()).getPayTotal() * 100);
//                refundInfo.setTotal_fee(total_fee);
//                //本次退款金额，所有退款金额加起来不能大于支付总金额（单位分，需要把元转分）
//                refundInfo.setRefund_fee(total_fee);
//                //签名
//                refundInfo.setSign(WechatPayUtils.createSign(MapUtil.beanToMap(refundInfo), payway.getPrivateKey()));
//                String result = "";
//                try {
//                    result = TransferUtils.doRefund(XStreamUtil.getXstream(RefundInfo.class).toXML(refundInfo), payway.getPartnerID(), payway.getReturnURL());
//                    log.info("result》》》》》》" + result);
//                    RefundReturnInfo refundReturnInfo = (RefundReturnInfo) XStreamUtil.getXstream(RefundReturnInfo.class).fromXML(result);
//                    if (null != refundReturnInfo) {
//                        map.put("id", apiMemberOrderOut.getMemberOrderID());
//                        map.put("status", 5);
//                        map.put("returnAmount", apiMemberOrderOut.getReturnAmount());
//                        map.put("returnPaywayID", apiMemberOrderOut.getPaywayID());
//                        map.put("returnTime", new Timestamp(System.currentTimeMillis()));
//                        map.put("returnType", 1);
//                        memberOrderService.update(map);
//                        return MessagePacket.newFail(MessageHeader.Code.success, "退款成功");
//                    } else {
//                        return MessagePacket.newFail(MessageHeader.Code.fail, refundReturnInfo.getReturn_msg());
//                    }
//                } catch (Exception e) {
//                    log.error("log",e);
//                }
//                break;
//        }
        map.put("id", memberOrderID);
        map.put("status", 7);
        map.put("returnGoodsTime", TimeTest.getTime());
        map.put("returnGoodsUserID", userLogin.getUserID());
        map.put("returnGoodsEmployeeID", userLogin.getEmployeeID());
        map.put("returnGoodsMemberID", userLogin.getMemberID());

        memberOrderService.update(map);
        if (userLogin != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统同意退货一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemReturnGoodsOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("returnGoodsTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemReturnGoodsOneMemberOrder success");
    }

    //微信退款回调
    @ApiOperation(value = "微信退款回调", notes = "微信退款回调")
    @RequestMapping("/notifyUrl")
    public String wechatRefundCallback(HttpServletRequest request, HttpServletResponse response) {
        String result = memberOrderService.wechatRefundCallback(request, response);
        return result;
    }

    ////    @GetMapping("/notifyUrl")
//    @Transactional
//    public Object refundNotifyTesult(@RequestBody String jsonData) throws Exception{
//        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<weixin-notifyUrl-notityXml>>>>>>>>>>>>>>>>>>>>>>>>>：" + jsonData);
//        Map<String,String> jsonMap = com.alibaba.fastjson.JSONObject.parseObject(jsonData, Map.class);
//        String resource = com.alibaba.fastjson.JSON.toJSONString(jsonMap.get("resource"));
//        com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(resource);
//        String ciphertext = String.valueOf(object.get("ciphertext"));
////        String apiV3key = String.valueOf(object.get("apiV3key"));
//        String nonce = String.valueOf(object.get("nonce"));
//        String associated_data = String.valueOf(object.get("associated_data"));
//
//        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<weixin-jsonMap-notityXml>>>>>>>>>>>>>>>>>>>>>>>>>：" + jsonMap);
//        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<weixin-object-notityXml>>>>>>>>>>>>>>>>>>>>>>>>>：" + object);
//        String resultStr = decryptToString(associated_data.getBytes("UTF-8"), nonce.getBytes("UTF-8"), ciphertext);
//        Map<String, String> reqInfo = com.alibaba.fastjson.JSONObject.parseObject(resultStr, Map.class);
//
//        String refund_status = reqInfo.get("refund_status");//退款状态
//        String out_trade_no = reqInfo.get("out_trade_no"); //订单号
//
//        Map<String, Object> parm = new HashMap<>();
//        if (!StringUtils.isEmpty(refund_status) && "SUCCESS".equals(refund_status))  {
//
//            //你自己的业务
//
//            parm.put("code", "SUCCESS");
//            parm.put("message", "成功");
//        } else {
//            parm.put("code", "FAIL");
//            parm.put("message", "失败");
//            throw new RuntimeException("退款失败");
//        }
//        return parm;  //返回给前端的参数
//    }
//    //退款回调  解密数据
//    public String decryptToString(byte[] associatedData, byte[] nonce, String ciphertext) throws GeneralSecurityException, IOException {
//        try {
//            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
////            String apiV3key = "";
//
//            SecretKeySpec key = new SecretKeySpec(apiV3key.getBytes(), "AES");//todo 这里的apiV3key是你的商户APIV3密钥
//            GCMParameterSpec spec = new GCMParameterSpec(128, nonce);//规定为128
//
//            cipher.init(Cipher.DECRYPT_MODE, key, spec);
//            cipher.updateAAD(associatedData);
//
//            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), "utf-8");
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//            throw new IllegalStateException(e);
//        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
//            throw new IllegalArgumentException(e);
//        }
//    }
    @RequestMapping(value = "/outSystemUpdateOneMemberOrderSend", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemUpdateOneMemberOrderSend(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
        UserLogin userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
//        User user = userService.selectUserByOutToken(outToken);
        if (userLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
        }
        String memberAddressName = request.getParameter("memberAddressName");
        if (StringUtils.isNotBlank(memberAddressName)) {
            map.put("memberAddressName", memberAddressName);
        }
        String contactName = request.getParameter("contactName");
        if (StringUtils.isNotBlank(contactName)) {
            map.put("contactName", contactName);
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            if (!NumberUtils.isNumeric(phone)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "phone请输入的数字");
            }
            map.put("phone", phone);
        }
        String expressID = request.getParameter("expressID");
        if (StringUtils.isNotBlank(expressID)) {
            if (!expressService.checkIsValid(expressID)) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDFound, "expressID不正确");
            }
            map.put("expressID", expressID);
        }
        String isMemberHide = request.getParameter("isMemberHide");
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide请填写数字");
            }
            map.put("isMemberHide", Integer.valueOf(isMemberHide));
        }
        String sendCompanyID = request.getParameter("sendCompanyID");
        if (StringUtils.isNotBlank(sendCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", sendCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "sendCompanyID 不正确");
            }
            map.put("sendCompanyID", sendCompanyID);
        }
        String sendMemberID = request.getParameter("sendMemberID");
        if (StringUtils.isNotBlank(sendMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", sendMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "sendMemberID 不正确");
            }
            map.put("sendMemberID", sendMemberID);
        }
        String sendVehicleCode = request.getParameter("sendVehicleCode");
        if (StringUtils.isNotBlank(sendVehicleCode)) {
            map.put("sendVehicleCode", sendVehicleCode);
        }
        String sendName = request.getParameter("sendName");
        if (StringUtils.isNotBlank(sendName)) {
            map.put("sendName", sendName);
        }
        String sendPhone = request.getParameter("sendPhone");
        if (StringUtils.isNotBlank(sendPhone)) {
            map.put("sendPhone", sendPhone);
        }
        String sendMemo = request.getParameter("sendMemo");
        if (StringUtils.isNotBlank(sendMemo)) {
            map.put("sendMemo", sendMemo);
        }
        map.put("modifier", userLogin.getUserID());
        map.put("sendUserID", userLogin.getUserID());
        map.put("sendEmployeeID", userLogin.getEmployeeID());
        map.put("sendMemberID", userLogin.getMemberID());
        map.put("sendTime", TimeTest.getTimeStr());
        map.put("sendStatus", 1);
        map.put("status", 6);
        String sendCode = request.getParameter("sendCode");
        if (StringUtils.isNotBlank(sendCode)) {
            map.put("sendCode", sendCode);
        }
        memberOrderService.outSystemUpdateMemberOrder(map);
        if (userLogin != null) {
            MemberOrderDto data = memberOrderService.getMemberOrderById(memberOrderID);
            String descriptions = String.format("外部系统修改一个会员订单（配送发货信息)");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemUpdateOneMemberOrderSend",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    data.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("modifiedTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemUpdateOneMemberOrderSend success");
    }

    /**
     * 这个接口请不要擅自修改！！！！
     */
    @ApiOperation(value = "standardCreateOneMemberOrder", notes = "独立接口：设计得到创建订单new")
    @RequestMapping(value = "/standardCreateOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket standardCreateOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String memberOrderID = IdGenerator.uuid32();
        String objectID = null;
        boolean flag = false;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//       Member member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        if (StringUtils.isBlank(member.getId())) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请重新登录");
        }
        map.put("memberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String buyWithVip = request.getParameter("buyWithVip");

        String majorID = request.getParameter("majorID");
        /*if(StringUtils.isBlank(majorID)){
            return MessagePacket.newFail(MessageHeader.Code.majorIDIsError, "majorID 不能为空");
        }*/
        if (StringUtils.isNotBlank(majorID)) {
            map.put("majorID", majorID);
        }
        // 判断当前会员是否是vip
        boolean isVip = false;
        List<MyMemberMajorListDto> list = memberMajorService.getMemberMajorList(new HashMap<String, Object>() {
            {
                put("memberID", member.getId());
                put("majorID", majorID);
                put("effEndDate", TimeTest.getTime());
            }
        });
        if (!list.isEmpty()) {
            isVip = true;
        }
        if (StringUtils.isNotBlank(buyWithVip)) {
            isVip = true;
        }

        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
            Member member1 = memberService.getMemberByRecommendCode(map);
            if (member1 != null) {
                map.put("recommendMemberID", member1.getId());
            } else {
                return MessagePacket.newFail(MessageHeader.Code.recommandCodeIsError, "recommendCode 推荐码不正确");
            }
        }

        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }

        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null && channel.getId() == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
        }

        String sendUrgencyType = request.getParameter("sendUrgencyType");
//        log.info("sendUrgencyType::"+sendUrgencyType);
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                map.put("sendUrgencyType", 1);
            }
            map.put("sendUrgencyType", sendUrgencyType);
        }
        if (StringUtils.isBlank(sendUrgencyType)) {
            map.put("sendUrgencyType", 1);
        }

        String reservedDate = request.getParameter("reservedDate");
        if (StringUtils.isNotBlank(reservedDate)) {
            map.put("reservedDate", reservedDate);
        }
        if (StringUtils.isBlank(reservedDate)) {
            map.put("reservedDate", TimeTest.getNowDateFormat());
        }

        String memberMajorID = request.getParameter("memberMajorID");
        if (StringUtils.isNotBlank(memberMajorID)) {
            if (!memberMajorService.checkID(memberMajorID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberMajorIsNull, "memberMajorID不正确");
            }
            map.put("memberMajorID", memberMajorID);
        }

        String goldenGetID = request.getParameter("goldenGetID");
        if (StringUtils.isNotBlank(goldenGetID)) {
            if (!goldenGetService.checkIdGoldenGetIsValid(goldenGetID)) {
                return MessagePacket.newFail(MessageHeader.Code.goldenGetIDNotFound, "goldenGetID不正确");
            }
            map.put("goldenGetID", goldenGetID);
        }

        String memberPrivilegeID = request.getParameter("memberPrivilegeID");
        if (StringUtils.isNotBlank(memberPrivilegeID)) {
            if (!memberPrivilegeService.checkIDIsValid(memberPrivilegeID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberPrivilegeIDNotFound, "memberPrivilegeID不正确");
            }
            map.put("memberPrivilegeID", memberPrivilegeID);
        }

        String presentDefineID = request.getParameter("presentDefineID");
        if (StringUtils.isNotBlank(presentDefineID)) {
            if (!presentDefineService.checkIdIsValid(presentDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberPrivilegeIDNotFound, "presentDefineID 不正确");
            }
        }
        String presentRecordID = request.getParameter("presentRecordID");
        if (StringUtils.isNotBlank(presentRecordID)) {
            if (!presentRecordService.checkIdIsValid(presentRecordID)) {
                return MessagePacket.newFail(MessageHeader.Code.presentRecordIDNotFound, "presentRecordID 不正确");
            }
            ApiPresentRecordDetailDto onePresentRecordDetail = presentRecordService.getOnePresentRecordDetail(presentRecordID);
            if (onePresentRecordDetail.getStatus() != 1
                    && onePresentRecordDetail.getStatus() != 8
                    && onePresentRecordDetail.getStatus() != 20) {
                return MessagePacket.newFail(MessageHeader.Code.presentRecordIDNotFound, "presentRecordID 状态不正确");
            }
            map.put("presentRecordID", presentRecordID);
        }

        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "orderType请输入数字");
            }
            map.put("orderType", orderType);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.orderTypeNotNull, "orderType不能为空");
        }

        String memberTrainID = request.getParameter("memberTrainID");
        String trainBusinessID = request.getParameter("trainBusinessID");
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!trainBusinessService.checkIdIsValid(trainBusinessID)) {
                return MessagePacket.newFail(MessageHeader.Code.trainBusinessIDNotFound, "trainBusinessID不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        String memberActionID = request.getParameter("memberActionID");
        if (StringUtils.isNotBlank(memberActionID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberAction", memberActionID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberActionIDNotFound, "memberActionID 不正确");
            }
            map.put("memberActionID", memberActionID);
        }

//        log.info("memberTrainID："+memberTrainID);
//        log.info("trainBusinessID："+trainBusinessID);
        //课程培训，如果传了memberTrainID就使用，没传通过trainBusinessID和会员ID得到memberTrainID
        if (!StringUtils.isNotBlank(memberTrainID) && "60".equals(orderType)) {
            Map<String, Object> rsMap = Maps.newHashMap();
            rsMap.put("trainBusinessID", trainBusinessID);
            rsMap.put("applyMemberID", memberLogDTO.getMemberId());
            MemberTrain memberTrain = memberTrainService.gettCountMemberTrainTime(rsMap);
            if (memberTrain != null) {
//                log.info("memberTrainID变化了，变化之前：" + memberTrainID);
                memberTrainID = memberTrain.getId();
//                log.info("memberTrainID变化了，变化之后：" + memberTrainID);
            }
        }
        if (StringUtils.isNotBlank(memberTrainID)) {
            MemberTrain memberTrain = memberTrainService.findById(memberTrainID);
            if (memberTrain == null) {
                return MessagePacket.newFail(MessageHeader.Code.memberTrainIDNotFound, "memberTrainID不正确");
            }
            map.put("memberTrainID", memberTrainID);

        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        } else {
            map.put("siteID", member.getSiteID());
        }

        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isNotBlank(payWayID)) {
            if (!paywayService.checkIsValidId(payWayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
            }
            map.put("payWayID", payWayID);
        }

        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!memberInvoiceDefineService.checkIsValidId(memberInvoiceDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }

        String memberPaywayID = request.getParameter("memberPaywayID");
        if (StringUtils.isNotBlank(memberPaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPayway", memberPaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPaywayIDNotFound, "memberPaywayID 不正确");
            }
            map.put("memberPaywayID", memberPaywayID);
        }

        String buyCompanyID = request.getParameter("buyCompanyID");
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (!companyService.checkIdIsValid(buyCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "buyCompanyID 不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }

        String goodsCompanyID = request.getParameter("goodsCompanyID");
        if (StringUtils.isNotBlank(goodsCompanyID)) {
            if (!companyService.checkIdIsValid(goodsCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "goodsCompanyID 不正确");
            }
            map.put("goodsCompanyID", goodsCompanyID);
        }

        String realPrice = request.getParameter("realPrice");
        if (StringUtils.isNotBlank(realPrice)) {
            map.put("realPrice", realPrice);
        }

        String memberBuyReadID = request.getParameter("memberBuyReadID");
        if (StringUtils.isNotBlank(memberBuyReadID)) {
            if (!memberBuyReadService.checkIdIsValid(memberBuyReadID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberBuyReadIDNotFound, "memberBuyReadID不正确");
            }
            map.put("memberBuyReadID", memberBuyReadID);
            MemberBuyReadDetailDto memberBuyRead = memberBuyReadService.getOneMemberBuyReadDetail(new HashMap<String, Object>() {
                {
                    put("id", memberBuyReadID);
                }
            });
            if (memberBuyRead != null) {
                if (StringUtils.isNotBlank(memberBuyRead.getObjectID())) {
                    objectID = memberBuyRead.getObjectID();
                }
            }

            // 判断订单是否重复
            List<MemberOrderGoodsDtoList> memberOrderGoodsDtoLists = memberOrderGoodsService.getMemberOrderGoodsDtoListByMap(new HashMap<String, Object>() {
                {
                    put("objectID", memberBuyRead.getObjectID());
                    put("orderMemberID", member.getId());
                    put("orderStatus", 1);
                }
            });
            if (!memberOrderGoodsDtoLists.isEmpty()) {
                memberOrderID = memberOrderGoodsDtoLists.get(0).getMemberOrderID();
                // 删除订单商品
                for (MemberOrderGoodsDtoList memberOrderGoods : memberOrderGoodsDtoLists) {
                    memberOrderGoodsService.updateByMap(new HashMap<String, Object>() {
                        {
                            put("id", memberOrderGoods.getMemberOrderGoodsID());
                            put("isValid", 0);
                        }
                    });
                }
                flag = true;
            }
        }

        String memberMemo = request.getParameter("memberMemo");
        if (StringUtils.isNotBlank(memberMemo)) {
            map.put("memberMemo", memberMemo);
        }

        String expressID = request.getParameter("expressID");
        if (StringUtils.isNotBlank(expressID)) {
            if (!expressService.checkIsValid(expressID)) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDFound, "expressID不正确");
            }
            map.put("expressID", expressID);
        }

        String sendCompanyID = request.getParameter("sendCompanyID");
        if (StringUtils.isNotBlank(sendCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", sendCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.idIsError, "sendCompanyID不正确");
            }
            map.put("sendCompanyID", sendCompanyID);
        }

        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }

        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }

        String actionGoodsType = request.getParameter("actionGoodsType");
        if (StringUtils.isNotBlank(actionGoodsType)) {
            if (!NumberUtils.isNumeric(actionGoodsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "actionGoodsType请输入数字");
            }
            map.put("actionGoodsType", actionGoodsType);
        }

        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                map.put("outPayType", 1);
            }
            map.put("outPayType", outPayType);
        } else {
            map.put("outPayType", 1);
        }

        String buyType = request.getParameter("buyType");
        if (StringUtils.isNotBlank(buyType)) {
            if (!NumberUtils.isNumeric(buyType)) {
                map.put("buyType", 0);
            }
            map.put("buyType", buyType);
        } else {
            map.put("buyType", 0);
        }

        String financeType = request.getParameter("financeType");
        if (StringUtils.isNotBlank(financeType)) {
            if (!NumberUtils.isNumeric(financeType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "financeType请输入数字");
            }
            map.put("financeType", financeType);
        } else {
            map.put("financeType", 1);
        }

        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
            MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
            if (memberAddress != null) {
                if (StringUtils.isNotBlank(memberAddress.getContactName())) {
                    map.put("contactName", memberAddress.getContactName());
                }
                if (StringUtils.isNotBlank(memberAddress.getName())) {
                    map.put("memberAddressName", memberAddress.getName());
                }
                if (StringUtils.isNotBlank(memberAddress.getPhone())) {
                    map.put("phone", memberAddress.getPhone());
                }
            }
        }

        /**
         *如果goodsPriceID不为空，就根据goodsPriceID查找goodsPrice记录，从中获取goodsShopID，使用该goodsShopID
         *
         */
        String goodsPriceID = request.getParameter("goodsPriceID");
        String goodsShopID = request.getParameter("goodsShopID");
        String byCart = request.getParameter("byCart");
        if (StringUtils.isBlank(goodsShopID) && StringUtils.isBlank(byCart) && StringUtils.isBlank(goodsPriceID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "byCart、goodsShopID和goodsPriceID不能同时为空");
        }
        if (!"1".equals(byCart)) {
            if (StringUtils.isBlank(goodsShopID) && StringUtils.isBlank(goodsPriceID)) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "goodsShopID和goodsPriceID不能同时为空");
            }
        }

        GoodsPriceDto goodsPriceDto = null;
        if (StringUtils.isNotBlank(goodsPriceID)) {
            goodsPriceDto = goodsPriceService.getById(goodsPriceID);
            if (goodsPriceDto != null) {
                String goodsPriceDto_goodsShopID = goodsPriceDto.getGoodsShopID();
                if (StringUtils.isNotBlank(goodsPriceDto_goodsShopID)) {
                    goodsShopID = goodsPriceDto_goodsShopID;
                }
            } else {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceID 不正确");
            }
        }
        // 根据goodsPriceID获取goodsPrice记录不为空，存入goodsPriceID，以及记录中的goodsPriceDefineID
        if (goodsPriceDto != null) {
            map.put("goodsPriceID", goodsPriceID);
            String goodsPriceDefineID = goodsPriceDto.getGoodsPriceDefineID();
            map.put("goodsPriceDefineID", goodsPriceDefineID);
        }


        String goodsQTY = request.getParameter("goodsQTY");
        map.put("goodsQTY", goodsQTY);
        log.info("goodsQTY1::" + goodsQTY);
        String price = request.getParameter("price");


        Double priceAfterDiscount = 0.0d;//LIUPAD1
        Integer goodsNumbers = 1;
        Integer goodsQty = 0;//商品数量
        Double weighTotal = 0.0;
        Double pricetotal = 0.0d;//商品总价格

        if (StringUtils.isNotBlank(goodsShopID)) {
            if (!goodsShopService.checkIsValidId(goodsShopID)) {
                return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID 不正确");
            }
        }

        List<MemberOrder> memberOrderListByMap = null;//课程培训用得到，如果有数据，则不在创建订单
        if (StringUtils.isNotBlank(byCart) && "1".equals(byCart)) {
            priceAfterDiscount = 0.0d;//LIUPAD2
            List<ApiCartGoodsListDto> cartGoodsList = cartGoodsService.getCartGoodsList(new HashMap<String, Object>() {
                {
                    put("memberID", member.getId());
                    put("isSelect", 1);
                }
            });
            if (!cartGoodsList.isEmpty()) {
                goodsNumbers = cartGoodsList.size();
                for (ApiCartGoodsListDto cartGoodsListDto : cartGoodsList) {
                    // 重新计算价格
                    Integer QTY = 0;// LIUQTY1 订购数量
                    Integer salesUnitQTY = 0;//销售单位数量
                    if (cartGoodsListDto.getQTY() != null) {
//                        goodsQty += cartGoodsListDto.getQTY();
                        goodsQty = BigDecimal.valueOf(goodsQty)
                                .add(BigDecimal.valueOf(QTY)).intValue();
                        QTY = cartGoodsListDto.getQTY();//LIUQTY2
                    }
                    if (cartGoodsListDto.getSalesUnitQTY() != null) {
                        salesUnitQTY = cartGoodsListDto.getSalesUnitQTY();
                    }

                    if (StringUtils.isNotBlank(buyWithVip) && StringUtils.isNotBlank(cartGoodsListDto.getObjectDefineID())) {
                        if ("8af5993a512ce9a201512e7d84b60873".equals(cartGoodsListDto.getObjectDefineID())) {
                            Story story = storyService.getStory(cartGoodsListDto.getObjectID());
                            if (story.getCashVIPPrice() != null) {
//                                priceAfterDiscount += (story.getCashVIPPrice() * QTY);//LIUPAD3
                                priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount)
                                        .add(BigDecimal.valueOf(story.getCashVIPPrice())
                                                .multiply(BigDecimal.valueOf(QTY))
                                                .setScale(2, RoundingMode.HALF_UP)).doubleValue();

                            }
                        } else if ("8a2f462a70844be901708556b35e02bb".equals(cartGoodsListDto.getObjectDefineID())) {
                            TrainCourse trainCourse = trainCourseService.findById(cartGoodsListDto.getObjectID());
                            if (trainCourse.getPriceVIPCash() != null) {
//                                priceAfterDiscount += (trainCourse.getPriceVIPCash() * QTY);//LIUPAD4
                                priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount)
                                        .add(BigDecimal.valueOf(trainCourse.getPriceVIPCash())
                                                .multiply(BigDecimal.valueOf(QTY))
                                                .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                            }
                        }
                    } else {
                        String cartGoodsShopID = cartGoodsListDto.getGoodsShopID();
                        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(cartGoodsShopID);
                        if (goodsShop != null) {

                            BigDecimal weighTotalBig = BigDecimal.ZERO;

                            if (goodsShop.getStandardWeigh() != null) {
                                // 将当前重量转换为 BigDecimal 并累加
                                BigDecimal standardWeigh = goodsShop.getStandardWeigh() == null ? BigDecimal.ZERO : BigDecimal.valueOf(goodsShop.getStandardWeigh());
                                BigDecimal goodsQtyBig = goodsQTY == null ? BigDecimal.ZERO : new BigDecimal(goodsQTY);
                                weighTotalBig = weighTotalBig.add(standardWeigh.multiply(goodsQtyBig));
                                // 如需保持原 double 类型的 weighTotal，可同步更新（按需保留）
                                weighTotal = weighTotalBig.doubleValue();
                            }
                            /*
                            if (goodsShop.getStandardWeigh() != null) {
                                weighTotal += goodsShop.getStandardWeigh();
                            }*/

                            if (StringUtils.isNotBlank(orderType) && orderType.equals("33")) {
                                if (isVip) {
                                    if (goodsShop.getRealPriceVIP() != null) {
//                                        priceAfterDiscount += (goodsShop.getRealPriceVIP() * QTY * salesUnitQTY);//LIUPAD5
                                        priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPriceVIP())
                                                        .multiply(BigDecimal.valueOf(QTY))
                                                        .multiply(BigDecimal.valueOf(salesUnitQTY))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                } else {
                                    if (goodsShop.getRealPrice() != null) {
//                                        priceAfterDiscount += (goodsShop.getRealPrice() * QTY * salesUnitQTY);//LIUPAD6
                                        priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPrice())
                                                        .multiply(BigDecimal.valueOf(QTY))
                                                        .multiply(BigDecimal.valueOf(salesUnitQTY))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                }
                            } else {

                                if (isVip) {
                                    if (goodsShop.getRealPriceVIP() != null) {
//                                        priceAfterDiscount += (goodsShop.getRealPriceVIP() * QTY);//LIUPAD6
                                        priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPriceVIP())
                                                        .multiply(BigDecimal.valueOf(QTY))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                } else {
                                    if (goodsShop.getRealPrice() != null) {
//                                        priceAfterDiscount += (goodsShop.getRealPrice() * QTY);//LIUPAD7
                                        priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPrice())
                                                        .multiply(BigDecimal.valueOf(QTY))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                }
                            }
                        }
                    }
                }
                map.put("priceTotal", priceAfterDiscount);
            } else {
                return MessagePacket.newFail(MessageHeader.Code.fail, "购物车中无商品");
            }


        } else {
            if (StringUtils.isNotBlank(goldenGetID)) {
                GoldenGet goldenGet = goldenGetService.getByID(goldenGetID);
                if (goldenGet != null) {
                    pricetotal = goldenGet.getPayAMount();
                }
            } else if (StringUtils.isNotBlank(memberBuyReadID)) {
                MemberBuyRead memberBuyRead = memberBuyReadService.getById(memberBuyReadID);
                if (memberBuyRead != null) {
                    pricetotal = memberBuyRead.getCashPrice();
                }
            } else if (StringUtils.isNotBlank(memberPrivilegeID)) {
                ApiMemberPrivilegeDto memberPrivilege = memberPrivilegeService.selectByID(memberPrivilegeID);
                if (memberPrivilege != null) {
                    pricetotal = memberPrivilege.getApplyAmount();
                }
            } else {
                GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                if (isVip) {
                    pricetotal = goodsShop.getRealPriceVIP();
                } else {
                    pricetotal = goodsShop.getRealPrice();
                }
                Double goodsQtyBig = goodsQty == null ? 0.0 : Double.valueOf(goodsQTY);
                pricetotal = pricetotal * goodsQtyBig;
            }
            if (StringUtils.isNotBlank(memberTrainID) && "60".equals(orderType)) {
                MemberTrain memberTrain = memberTrainService.findById(memberTrainID);
                //memberTrain不为空时获取其中的培训字段
                if (memberTrain != null) {
                    Map<String, Object> memberOrdderMap = new HashMap<>();
                    memberOrdderMap.put("memberID", memberLogDTO.getMemberId());
                    memberOrdderMap.put("memberTrainID", memberTrainID);
                    memberOrderListByMap = memberOrderService.getMemberOrderListByMap(memberOrdderMap);
                    if (memberOrderListByMap != null && memberOrderListByMap.size() > 0) {
//                        log.info("memberID:" + memberLogDTO.getMemberId());
//                        log.info("memberTrainID:" + memberTrainID);
                        log.info("已经有订单数据了");
                        flag = true;
                    }
                    pricetotal = memberTrain.getPriceEnd();
                    map.put("memberTrainID", memberTrainID);
                    String trainBusinessID_liu = memberTrain.getTrainBusinessID();
                    if (StringUtils.isNotBlank(trainBusinessID_liu)) {
                        map.put("trainBusinessID", trainBusinessID_liu);
                    }
                    String trainCourseID_liu = memberTrain.getTrainCourseID();
                    if (StringUtils.isNotBlank(trainCourseID_liu)) {
                        map.put("trainCourseID", trainCourseID_liu);
                    }
                    String trainHourID_liu = memberTrain.getTrainHourID();
                    if (StringUtils.isNotBlank(trainHourID_liu)) {
                        map.put("trainHourID", trainHourID_liu);
                    }
                }
            }
            //我传 price 是有的，理论上，不能 前端传价格
            //因为 后台随时会修改价格
            map.put("priceTotal", pricetotal);
            //map.put("priceTotal", price);
            priceAfterDiscount = pricetotal;
        }

        Double bonusAmount = 0.0d;
        String memberBonusIDChain = request.getParameter("memberBonusIDChain");
        if (StringUtils.isNotBlank(memberBonusIDChain)) {
            String[] memberBonusIDArray = memberBonusIDChain.split(",");
            List<MemberBonus> memberBonusList = new ArrayList<>();
            for (String memberBonusID : memberBonusIDArray) {
                // 检查会员红包是否存在和可用
                MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(new HashMap<String, Object>() {
                    {
                        put("id", memberBonusID);
                    }
                });
                if (memberBonus != null) {
                    // 判断是否可用
                    if (memberBonus.getCancelTime() != null) {
                        long cancelTime = memberBonus.getCancelTime().getTime();
                        String nowTime = TimeTest.getTimeStr();
                        long newDate = TimeTest.strToDate(nowTime).getTime();
                        if (cancelTime < newDate) {
                            return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "红包已经作废");
                        }
                    }
                    if (memberBonus.getMemberOrderID() == null && memberBonus.getStatus() == 10) {
                        Double amount = memberBonus.getAmount();
                        Double offRate = memberBonus.getOffRate();
                        String bonusID = memberBonus.getBonusID();
                        Integer startPrice = 0;
                        if (StringUtils.isNotBlank(bonusID)) {
                            BonusDto bonusDto = bonusService.getByID(bonusID);
                            startPrice = bonusDto.getStartPrice();
                        }
                        if (priceAfterDiscount >= startPrice) {
                            if (amount == null || BigDecimal.valueOf(amount).signum() == 0) {
//                                offRate = offRate / 100;
//                                bonusAmount += priceAfterDiscount * offRate;
//                                memberBonus.setOffAmount(priceAfterDiscount * offRate);

                                offRate = BigDecimal.valueOf(offRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
                                bonusAmount = BigDecimal.valueOf(bonusAmount)
                                        .add(BigDecimal.valueOf(priceAfterDiscount)
                                                .multiply(BigDecimal.valueOf(offRate))
                                                .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                memberBonus.setOffAmount(BigDecimal.valueOf(priceAfterDiscount)
                                        .multiply(BigDecimal.valueOf(offRate)
                                                .setScale(2, RoundingMode.HALF_UP)).doubleValue());
                            } else {
//                                bonusAmount += amount;
                                bonusAmount = BigDecimal.valueOf(bonusAmount).add(BigDecimal.valueOf(amount)).doubleValue();
                                memberBonus.setOffAmount(amount);
                            }
                            memberBonusList.add(memberBonus);
                        } else {
                            return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "未满足该红包的起点金额");
                        }
                    }
                }
            }
            // 修改使用红包的状态和属性（不修改状态，改为支付成功后才修改）
            Map<String, Object> map1 = new HashMap<>();
            if (!memberBonusList.isEmpty() && memberBonusList.size() > 0) {
                for (MemberBonus memberBonus : memberBonusList) {
                    map1.clear();
                    map1.put("id", memberBonus.getId());
                    map1.put("status", 20);
                    map1.put("memberOrderID", memberOrderID);
                    map1.put("useTime", TimeTest.getTime());
                    map1.put("orderTotal", BigDecimal.valueOf(priceAfterDiscount).subtract(BigDecimal.valueOf(bonusAmount)).doubleValue());
                    map1.put("offAmount", memberBonus.getOffAmount());
                    memberBonusService.updateByMap(map1);
                }
            }
//            priceAfterDiscount -= bonusAmount;
            priceAfterDiscount = BigDecimal.valueOf(priceAfterDiscount).subtract(BigDecimal.valueOf(bonusAmount)).doubleValue();
            // 折扣金额
            map.put("bonusAmount", bonusAmount);//LIUPAD9
        }
        map.put("closedPriceTotal", priceAfterDiscount);
        map.put("priceAfterDiscount", priceAfterDiscount);

        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payFrom请输入数字");
            }
            if ("12".equals(payFrom)) {
                map.put("goldenTotal", priceAfterDiscount);
            } else {
                map.put("goldenTotal", 0);
            }
        } else {
            return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom不能为空");
        }
        map.put("payFrom", payFrom);
        String periodQTY = request.getParameter("periodQTY");
        if (orderType.equals("33") && StringUtils.isNotBlank(byCart)) {
//            当 byCart 不是1 的时候，采用 参数传入的 goodsShopID，参数periodQTY 表示要购买的
//            周期个数，参数goodsQTY 表示客户购买商品虚拟机的个数。
//            先去 computerUnit 表查询 goodsShopID对应的status=1 的记录，
//            并且currentMemberID为空的记录，如果查询 记录为0 ，
//            则表示 没有足够的 虚拟机 ，不能创建订单，报错退出。如果有，则 正常创建订单。
            if (!byCart.equals("1")) {
                GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                if (goodsShop == null) {
                    return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID不正确");
                }

                if (StringUtils.isBlank(periodQTY)) {//要购买的周期个数
                    return MessagePacket.newFail(MessageHeader.Code.numberNot, "periodQTY 不能为空");
                }
                if (StringUtils.isBlank(goodsQTY)) {//客户购买商品虚拟机的个数
                    return MessagePacket.newFail(MessageHeader.Code.fail, "goodsQTY 不能为空");
                }
                List<OutSystemComputerUnitList> computerUnits = computerUnitService.selectByComputerUnit(goodsShop.getId());
                if (computerUnits.isEmpty()) {
                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "没有足够的虚拟机不能创建订单");
                }
//                if (goodsShop.getPeriodNumber() == null) {
//                    return MessagePacket.newFail(MessageHeader.Code.numberNot, "goodsShop中的periodNumber不能为空");
//                }
//                if (goodsShop.getPeriodType() == null) {
//                    return MessagePacket.newFail(MessageHeader.Code.numberNot, "goodsShop中的periodType不能为空");
//                }
//                List<OutSystemComputerUnitList> computerUnits = computerUnitService.selectByComputerUnits(goodsShop.getId(), goodsQTY);
//                if (computerUnits.isEmpty() || computerUnits.size() < Integer.parseInt(goodsQTY)) {
//                    return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "没有空闲的计算机单元或者空闲计算机小于需要数量");
//                }
//                for (OutSystemComputerUnitList computerUnit : computerUnits) {
//                    ModelMap computerUnitMap = new ModelMap();
//                    computerUnitMap.put("currentMemberID", member.getId());
//                    computerUnitMap.put("status", 5);
//                    computerUnitMap.put("id", computerUnit.getComputerUnitID());
//                    computerUnitService.updateByMap(computerUnitMap);
//                    ModelMap computerRecordMap = new ModelMap();
//                    computerRecordMap.put("id", IdGenerator.uuid32());
//                    computerRecordMap.put("applicationID", member.getApplicationID());
//                    computerRecordMap.put("companyID", member.getCompanyID());
//                    computerRecordMap.put("beginTime", TimeTest.getTime());
//                    if (goodsShop.getPeriodType() == 1) {
//                        int day = Integer.parseInt(periodQTY) * goodsShop.getPeriodNumber();
//                        computerRecordMap.put("endTime", DateUtil.addToDay(new Date(), day));
//                    } else if (goodsShop.getPeriodType() == 2) {
//                        int month = Integer.parseInt(periodQTY) * goodsShop.getPeriodNumber();
//                        computerRecordMap.put("endTime", DateUtil.addToMonth(new Date(), month));
//                    } else if (goodsShop.getPeriodType() == 3) {
//                        int year = Integer.parseInt(periodQTY) * goodsShop.getPeriodNumber();
//                        computerRecordMap.put("endTime", DateUtil.addToYear(new Date(), year));
//                    }
//                    computerRecordService.insertByMap(computerRecordMap);
//                }
            }
        }

        String depositSaveID = request.getParameter("depositSaveID");
        if (StringUtils.isNotBlank(depositSaveID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("depositSave", depositSaveID))) {
                return MessagePacket.newFail(MessageHeader.Code.depositSaveIDNotFound, "depositSaveID 不正确");
            }
        }

        /**
         * 如果是 礼品卡定义 批量生成空白礼品卡
         */
        if (orderType.equals("6") && StringUtils.isNotBlank(presentDefineID)) {
            ApiPresentDefineDetailDto presentDefineDetail = presentDefineService.getPresentDefineDetail(presentDefineID);
            String presentDefine_shopID = presentDefineDetail.getShopID();
            String presentDefine_companyID = presentDefineDetail.getCompanyID();
            Double presentDefine_cashPrice = presentDefineDetail.getCashPrice();//现金价格
            Double presentDefine_pointPrice = presentDefineDetail.getPointPrice();//积分价格
            Double presentDefine_goldenPrice = presentDefineDetail.getGoldenPrice();//金币价格
            Double presentDefine_beanPrice = presentDefineDetail.getBeanPrice();//豆子价格

            String presentDefine_goodsID = presentDefineDetail.getGoodsID();
            String presentDefine_goodsCompanyID = presentDefineDetail.getGoodsCompanyID();
            String presentDefine_goodsShopID = presentDefineDetail.getGoodsShopID();

            String presentDefine_trainBusinessID = presentDefineDetail.getTrainBusinessID();
            String presentDefine_majorID = presentDefineDetail.getMajorID();
//            Double presentDefine_unitPrice = presentDefineDetail.getU();//销售价格
//            Double presentDefine_priceTotal = presentDefine_cashPrice * Integer.valueOf(goodsQTY);
//            Double pointTotal = presentDefine_pointPrice * Integer.valueOf(goodsQTY);
//            Double goldenTotal = presentDefine_goldenPrice * Integer.valueOf(goodsQTY);
//            Double beanTotal = presentDefine_beanPrice * Integer.valueOf(goodsQTY);
            Double presentDefine_priceTotal = BigDecimal.valueOf(presentDefine_cashPrice)
                    .multiply(BigDecimal.valueOf(Integer.parseInt(goodsQTY)))
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            Double pointTotal = BigDecimal.valueOf(presentDefine_pointPrice)
                    .multiply(BigDecimal.valueOf(Integer.parseInt(goodsQTY)))
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            Double goldenTotal = BigDecimal.valueOf(presentDefine_goldenPrice)
                    .multiply(BigDecimal.valueOf(Integer.parseInt(goodsQTY)))
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            Double beanTotal = BigDecimal.valueOf(presentDefine_beanPrice)
                    .multiply(BigDecimal.valueOf(Integer.parseInt(goodsQTY)))
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();


            goodsQty = Integer.valueOf(goodsQTY);
            map.put("presentDefineID", presentDefineID);

            map.put("goodsQTY", goodsQTY);
            map.put("companyID", presentDefine_companyID);
            map.put("shopID", presentDefine_shopID);
            map.put("distributorCompanyID", presentDefine_goodsCompanyID);
            map.put("trainBusinessID", presentDefine_trainBusinessID);
            map.put("majorID", presentDefine_majorID);
            map.put("priceTotal", presentDefine_priceTotal);
            map.put("priceAfterDiscount", presentDefine_priceTotal);
            map.put("closedPriceTotal", presentDefine_priceTotal);

            map.put("pointTotal", pointTotal);
            map.put("goldenTotal", goldenTotal);
            map.put("beanTotal", beanTotal);
        }

        // 创建会员订单
        String applicationID = member.getApplicationID();
        String companyID = member.getCompanyID();
        String orderCode = sequenceDefineService.genCode("orderSeq", memberOrderID);
        map.put("orderCode", orderCode);
        map.put("id", memberOrderID);
        map.put("applicationID", applicationID);
        //map.put("companyID", companyID);
        if (StringUtils.isNotBlank(goodsShopID)) {
            String getcompanyid = goodsShopService.getcompanyid(goodsShopID);
            map.put("companyID", getcompanyid);
        }
        if (StringUtils.isNotBlank(goodsShopID)) {
            String getshopid = goodsShopService.getshopid(goodsShopID);
            map.put("shopID", getshopid);
        }
        if (StringUtils.isNotBlank(shopID)) {
            ApiShopDetailDto shopDetail = shopService.getShopDetail(shopID);
            if (shopDetail.getCompanyID() != null) {
                map.put("companyID", shopDetail.getCompanyID());
            }
        }


        //map.put("shopID", goodsShopID);
        map.put("name", member.getName() + "购买商品");
        map.put("applyTime", TimeTest.getTime());
        map.put("goodsNumbers", goodsNumbers);
        if (StringUtils.isNotBlank(price)) {
            map.put("priceTotal", Double.parseDouble(price));
        }
        if (StringUtils.isNotBlank(goodsQTY)) {
            map.put("goodsQTY", goodsQTY);
        } else {
            map.put("goodsQTY", goodsQty);
        }
        log.info("goodsQTY2::" + goodsQTY);
        if (member.getRecommendID() != null) {
            if (baseService.checkIdIsValid(new BaseParameter("member", member.getRecommendID()))) {
                map.put("recommendMemberID", member.getRecommendID());
            }
        }
        map.put("weighTotal", Math.round(weighTotal));
        map.put("weightTotalFee", 0);
        map.put("sendPrice", 0);
        map.put("pointUsed", 0);
        map.put("pointTotal", 0);
        map.put("rebateTotal", 0);
        map.put("beanTotal", 0);
        map.put("subPrePay", 0);
        map.put("pointSubAmount", 0);
        map.put("rebateSubAmount", 0);
        map.put("bonusAmount", 0);
        map.put("sendStatus", 0);
        map.put("weighPrice", 0);
        map.put("discountAmount", 0);
        map.put("tax", 0);
        map.put("serviceFee", 0);
        map.put("payStatus", 0);
        map.put("presentPayAmount", 0);
        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            if (!NumberUtils.isNumeric(sendType)) {
                map.put("sendType", 1);
            }
            if (NumberUtils.isNumeric(sendType)) {
                map.put("sendType", sendType);
            }
        }
        if (StringUtils.isBlank(sendType)) {
            map.put("sendType", 1);
        }
        map.put("sendFee", 0);
        map.put("returnAmount", 0);
        map.put("isMemberHide", 0);
        map.put("status", 1);
        map.put("trainBusinessID", trainBusinessID);
        map.put("phone", member.getPhone());
        map.put("creator", member.getId());

        //优惠后总价格 priceAfterDiscount= priceTotal+sendPrice-subPrePay-pointSubAmount-rebateSubAmount-bonusAmount+weighPrice-discountAmount

        Double priceTotal_ = 0.0d;
        String pt = String.valueOf(map.get("priceTotal"));
        if (StringUtils.isNotBlank(pt) && !"null".equals(pt)) {
            priceTotal_ = Double.valueOf(pt);
        }
        Double sendPrice_ = Double.valueOf(String.valueOf(map.get("sendPrice")));
        Double subPrePay_ = Double.valueOf(String.valueOf(map.get("subPrePay")));
        Double pointSubAmount_ = Double.valueOf(String.valueOf(map.get("pointSubAmount")));
        Double rebateSubAmount_ = Double.valueOf(String.valueOf(map.get("rebateSubAmount")));
        Double bonusAmount_ = Double.valueOf(String.valueOf(map.get("bonusAmount")));
        Double weighPrice_ = Double.valueOf(String.valueOf(map.get("weighPrice")));
        Double discountAmount_ = Double.valueOf(String.valueOf(map.get("discountAmount")));
//        Double priceAfterDiscount_ = priceTotal_ + sendPrice_ - subPrePay_ - pointSubAmount_ - rebateSubAmount_ - bonusAmount_ + weighPrice_ - discountAmount_;
        Double priceAfterDiscount_ = BigDecimal.valueOf(priceTotal_).add(BigDecimal.valueOf(sendPrice_))
                .subtract(BigDecimal.valueOf(subPrePay_)).subtract(BigDecimal.valueOf(pointSubAmount_))
                .subtract(BigDecimal.valueOf(rebateSubAmount_)).subtract(BigDecimal.valueOf(bonusAmount_))
                .add(BigDecimal.valueOf(weighPrice_)).subtract(BigDecimal.valueOf(discountAmount_)).doubleValue();


        //log.info("优惠后总价格：："+priceAfterDiscount_);
        map.put("priceAfterDiscount", priceAfterDiscount_);
        //结算总价格 closedPriceTotal= priceAfterDiscount+tax+serviceFee
        Double tax_ = Double.valueOf(String.valueOf(map.get("tax")));
        Double serviceFee_ = Double.valueOf(String.valueOf(map.get("serviceFee")));
//        Double closedPriceTotal_ = priceAfterDiscount_ + tax_ + serviceFee_;
        Double closedPriceTotal_ = BigDecimal.valueOf(priceAfterDiscount_)
                .add(BigDecimal.valueOf(tax_)).add(BigDecimal.valueOf(serviceFee_)).doubleValue();
        map.put("closedPriceTotal", closedPriceTotal_);

        /**
         * 如果是使用 礼品卡，直接更改状态
         */
        if ("6".equals(orderType) && "7".equals(payFrom) && StringUtils.isNotBlank(presentRecordID)) {
            map.put("status", 4);
            map.put("payStatus", 2);
        }


        // 判断是否有重复订单，有就编辑并删除订单中的商品
        if (StringUtils.isNotBlank(goodsShopID)) {
            if (flag) {
                //培训课程，是否有数据，有就不做任何操作，不添加订单信息
                if (memberOrderListByMap != null && memberOrderListByMap.size() > 0) {
//                    log.info(memberOrderListByMap.size());
                    memberOrderID = memberOrderListByMap.get(0).getId();
//                    log.info("memberOrderID："+memberOrderID);
                    log.info("订单数据存在");
                } else {
                    memberOrderService.update(map);
                }
            } else {
                memberOrderService.insertMemberOrder(map);
            }
        } else {
            memberOrderService.insertMemberOrder(map);
        }
        if (StringUtils.isNotBlank(memberActionID)) {
            Map<String, Object> memberActionMap = Maps.newHashMap();
            memberActionMap.put("id", memberActionID);
            memberActionMap.put("memberOrderID", memberOrderID);
            memberActionMap.put("modifier", member.getId());
            memberActionService.updateMyMemberAction(memberActionMap);
        }


        //如果goodsPriceID有值且正确，添加goodsPriceMember记录 ，参数从上表获取
        if (goodsPriceDto != null) {
            Map<String, Object> goodsPriceMemberMap = new HashMap<>();
            goodsPriceMemberMap.put("id", IdGenerator.uuid32());
            if (StringUtils.isNotBlank(goodsPriceDto.getApplicationID())) {
                String goodsPrice_applicationID = goodsPriceDto.getApplicationID();//applicationID
                goodsPriceMemberMap.put("applicationID", goodsPrice_applicationID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getCompanyID())) {
                String goodsPrice_companyID = goodsPriceDto.getCompanyID();
                goodsPriceMemberMap.put("companyID", goodsPrice_companyID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getShopID())) {
                String goodsPrice_shopID = goodsPriceDto.getShopID();//shopID
                goodsPriceMemberMap.put("shopID", goodsPrice_shopID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getSceneID())) {
                String goodsPrice_sceneID = goodsPriceDto.getSceneID();//sceneID
                goodsPriceMemberMap.put("sceneID", goodsPrice_sceneID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getPropertyID())) {
                String goodsPrice_propertyID = goodsPriceDto.getPropertyID();
                goodsPriceMemberMap.put("propertyID", goodsPrice_propertyID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getGoodsPriceDefineID())) {
                String goodsPrice_goodsPriceDefineID = goodsPriceDto.getGoodsPriceDefineID();//goodsPriceDefineID
                goodsPriceMemberMap.put("goodsPriceDefineID", goodsPrice_goodsPriceDefineID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getGoodsPriceID())) {
                String goodsPrice_goodsPriceID = goodsPriceDto.getGoodsPriceID();//goodsPriceID
                goodsPriceMemberMap.put("goodsPriceID", goodsPrice_goodsPriceID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getGoodsID())) {
                String goodsPrice_goodsID = goodsPriceDto.getGoodsID();//goodsID
                goodsPriceMemberMap.put("goodsID", goodsPrice_goodsID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getGoodsCompanyID())) {
                String goodsPrice_goodsCompanyID = goodsPriceDto.getGoodsCompanyID();
                goodsPriceMemberMap.put("goodsCompanyID", goodsPrice_goodsCompanyID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getGoodsShopID())) {
                String goodsPrice_goodsShopID = goodsPriceDto.getGoodsShopID();//goodsShopID
                goodsPriceMemberMap.put("goodsShopID", goodsPrice_goodsShopID);
            }
            if (StringUtils.isNotBlank(String.valueOf(map.get("cityID"))) && !"null".equals(String.valueOf(map.get("cityID")))) {
                String memberOrder_cityID = String.valueOf(map.get("cityID"));//cityID
                goodsPriceMemberMap.put("cityID", memberOrder_cityID);
            }
            if (StringUtils.isNotBlank(goodsPriceDto.getName())) {
                String goodsPrice_name = goodsPriceDto.getName();
                goodsPriceMemberMap.put("name", goodsPrice_name);
            }
            if (StringUtils.isNotBlank(memberLogDTO.getMemberId())) {
                String memberLogDTO_memberID = memberLogDTO.getMemberId();
                goodsPriceMemberMap.put("memberID", memberLogDTO_memberID);
            }
            String memberOrder_memberOrderID = memberOrderID;
            goodsPriceMemberMap.put("memberOrderID", memberOrder_memberOrderID);
            String memberOrderStatus = "1";//新
            goodsPriceMemberMap.put("memberOrderStatus", memberOrderStatus);
            if (StringUtils.isNotBlank(String.valueOf(map.get("goodsQTY")))) {
                String memberOrder_goodsQTY = String.valueOf(map.get("goodsQTY"));
                goodsPriceMemberMap.put("orderNumber", Integer.valueOf(memberOrder_goodsQTY));
                if ("0".equals(memberOrder_goodsQTY)) {
                    goodsPriceMemberMap.put("orderNumber", 1);
                }
            }
            if (StringUtils.isNotBlank(String.valueOf(map.get("priceTotal"))) && !"null".equals(String.valueOf(map.get("priceTotal")))) {
                String memberOrder_priceTotal = String.valueOf(map.get("priceTotal"));
                goodsPriceMemberMap.put("orderPrice", Double.valueOf(memberOrder_priceTotal));
                goodsPriceMemberMap.put("orderAmount", Double.valueOf(memberOrder_priceTotal));
            }
            log.info("要添加了");
//            for (Map.Entry<String, Object> entry : map.entrySet()) {
//                log.info("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//            }
            goodsPriceMemberService.insertByMap(goodsPriceMemberMap);
        } else {
            log.info("goodsPriceDto 没有数据 为空");
        }

        if (orderType.equals("22") && StringUtils.isNotBlank(depositSaveID)) {

            Map<String, Object> deMap = new HashMap<>();
            deMap.put("id", depositSaveID);
            deMap.put("memberOrderID", memberOrderID);
            deMap.put("memberOrderStatus", 1);
            depositSaveService.updateByMap(deMap);
        }

        // 创建memberOrderGoods
        // 计算总重量
        Double weight = 0.0d;
        int num = 0;
        log.info("byCart的值::" + byCart);
        if (StringUtils.isNotBlank(byCart) && "1".equals(byCart)) {//购物车不为空
            log.info("购物车不为空");
            List<ApiCartGoodsListDto> cartGoodsList = cartGoodsService.getCartGoodsList(new HashMap<String, Object>() {
                {
                    put("memberID", member.getId());
                    put("isSelect", 1);
                }
            });
            String cartID = "";
            List<ApiCartGoodsListDto> newCartGoodsList = new ArrayList<>();
            if (!cartGoodsList.isEmpty() && cartGoodsList.size() > 0) {
                cartID = cartGoodsList.get(0).getCartID();
//                    cartID = cartGoodsList.get(0).getCartGoodsID();
                if (StringUtils.isNotBlank(shopID)) {
                    for (ApiCartGoodsListDto dto : cartGoodsList) {
                        GoodsShop goodsShops = goodsShopService.getGoodsShopByID(dto.getGoodsShopID());
                        if (goodsShops != null) {
                            String shopID1 = goodsShops.getShopID();
                            if (StringUtils.isNotBlank(shopID1) && shopID1.equals(shopID)) {
                                newCartGoodsList.add(dto);
                            }
                        }
                    }
                } else {
                    newCartGoodsList.addAll(cartGoodsList);
                }
                for (ApiCartGoodsListDto cartGoodsListDto : newCartGoodsList) {
                    GoodsShop goodsShop1 = goodsShopService.getGoodsShopByID(cartGoodsListDto.getGoodsShopID());
//                        map.clear();
                    map.put("id", IdGenerator.uuid32());
                    map.put("applicationID", applicationID);
                    map.put("companyID", companyID);
                    map.put("shopID", shopID);
                    map.put("memberID", member.getId());
                    map.put("memberOrderID", memberOrderID);
                    map.put("goodsShopID", goodsShop1.getId());
                    map.put("priceTotalReturn", 0);
                    map.put("priceReturn", 0);
                    map.put("QTYWeighed", 0);
                    map.put("giveQTY", 0);
                    map.put("weighTotal", 0);
                    map.put("weightTotalFee", 0);
                    map.put("weighPrice", 0);
                    map.put("tax", 0);
                    map.put("discountRate", 0);
                    if (cartGoodsListDto.getPeriodType() != null) {
                        map.put("periodType", cartGoodsListDto.getPeriodType());
                    }
                    if (cartGoodsListDto.getPeriodNumber() != null) {
                        map.put("periodNumber", cartGoodsListDto.getPeriodNumber());
                    }
                    if (goodsShop1.getUseDescription() != null) {
                        map.put("description", goodsShop1.getUseDescription());
                    }
                    if (goodsShop1.getUnitDescription() != null) {
                        map.put("useDescription", goodsShop1.getUnitDescription());
                    }
                    if (goodsShop1.getGoodsID() != null) {
                        map.put("goodsID", goodsShop1.getGoodsID());
                    }
                    if (goodsShop1.getGoodsCompanyID() != null) {
                        map.put("goodsCompanyID", goodsShop1.getGoodsCompanyID());
                    }
                    if (goodsShop1.getTermCode() != null) {
                        map.put("IDCode", goodsShop1.getTermCode());
                    }
                    if (goodsShop1.getShopID() != null) {
                        map.put("shopID", goodsShop1.getShopID());
                    }
                    if (goodsShop1.getTermCode() != null) {
                        map.put("IDCode", goodsShop1.getTermCode());
                    }
                    if (objectID!=null&&StringUtils.isNotBlank(objectID)) {
                        map.put("objectID", objectID);
                    }
                    if (goodsShop1.getStandardWeigh() != null) {
                        map.put("weighTotal", goodsShop1.getStandardWeigh());
                        map.put("weightTotalFee", goodsShop1.getStandardWeigh());
//                        weight += goodsShop1.getStandardWeigh();
                        weight = BigDecimal.valueOf(weight).add(BigDecimal.valueOf(goodsShop1.getStandardWeigh())).doubleValue();
                    }
                    map.put("QTY", cartGoodsListDto.getQTY());//q8
                    map.put("salesUnitQTY", cartGoodsListDto.getQTY());
                    map.put("status", 1);
                    if (goodsShop1.getShowName() != null) {
                        map.put("showName", goodsShop1.getShowName());
                    }
                    if ("12".equals(payFrom)) {
                        map.put("objectFeatureItemID1", priceAfterDiscount);
                    }

                    Double goodsPrice = 0.0d;
                    if (StringUtils.isNotBlank(cartGoodsListDto.getObjectDefineID())) {
                        if ("8af5993a512ce9a201512e7d84b60873".equals(cartGoodsListDto.getObjectDefineID())) {
                            Story story = storyService.getStory(cartGoodsListDto.getObjectID());
                            if (story != null) {
                                map.put("name", story.getName());
                                map.put("listImage", story.getListImage());
                                if (isVip) {
                                    goodsPrice = story.getCashVIPPrice();
                                } else {
                                    goodsPrice = story.getCashPrice();
                                }
                                map.put("priceStand", goodsPrice);
                                map.put("priceNow", goodsPrice);//7
//                                map.put("priceTotal", goodsPrice * cartGoodsListDto.getQTY());
                                map.put("priceTotal", BigDecimal.valueOf(goodsPrice)
                                        .multiply(BigDecimal.valueOf(cartGoodsListDto.getQTY()))
                                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
                            }
                        } else if ("8a2f462a70844be901708556b35e02bb".equals(cartGoodsListDto.getObjectDefineID())) {
                            TrainCourse trainCourse = trainCourseService.findById(cartGoodsListDto.getObjectID());
                            if (trainCourse != null) {
                                map.put("name", trainCourse.getName());
                                map.put("listImage", trainCourse.getListImage());
                                if (isVip) {
                                    goodsPrice = trainCourse.getPriceVIPCash();
                                } else {
                                    goodsPrice = trainCourse.getPriceCash();
                                }
                                map.put("priceStand", goodsPrice);
                                map.put("priceNow", goodsPrice);//8
//                                map.put("priceTotal", goodsPrice * cartGoodsListDto.getQTY());
                                map.put("priceTotal", BigDecimal.valueOf(goodsPrice)
                                        .multiply(BigDecimal.valueOf(cartGoodsListDto.getQTY()))
                                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
                            }
                        } else if ("8a2f462a6371016e0163717f1e0f057c".equals(cartGoodsListDto.getObjectDefineID())) {
                            TrainBusiness trainBusiness = trainBusinessService.findById(cartGoodsListDto.getObjectID());
                            if (trainBusiness != null) {
                                map.put("name", trainBusiness.getName());
                                map.put("listImage", trainBusiness.getListImage());
                                if (isVip) {
                                    goodsPrice = trainBusiness.getPriceVIPCash();
                                } else {
                                    goodsPrice = trainBusiness.getPriceCash();
                                }
                                map.put("priceStand", goodsPrice);
                                map.put("priceNow", goodsPrice);//9
//                                map.put("priceTotal", goodsPrice * cartGoodsListDto.getQTY());
                                map.put("priceTotal", BigDecimal.valueOf(goodsPrice)
                                        .multiply(BigDecimal.valueOf(cartGoodsListDto.getQTY()))
                                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
                            }
                        }
                    } else {
                        if (goodsShop1.getName() != null) {
                            map.put("name", goodsShop1.getName());
                        }
                        if (goodsShop1.getLittleImage() != null) {
                            map.put("listImage", goodsShop1.getLittleImage());
                        } else if (goodsShop1.getLargerImage() != null) {
                            map.put("listImage", goodsShop1.getLargerImage());
                        }
                        if (goodsShop1.getStandPrice() != null) {
                            map.put("priceStand", goodsShop1.getStandPrice());
                        }
                        if (goodsShop1.getRealPrice() != null) {
                            if ("1".equals(byCart)) {//购物车不为空 使用购物车价格
                                Double cartGoods_priceNow = cartGoodsListDto.getPriceNow();
                                map.put("priceNow", cartGoods_priceNow);//购车价格
//                                map.put("priceTotal", cartGoods_priceNow * cartGoodsListDto.getQTY());
                                map.put("priceTotal", BigDecimal.valueOf(cartGoods_priceNow)
                                        .multiply(BigDecimal.valueOf(cartGoodsListDto.getQTY()))
                                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
                            } else {
                                map.put("priceNow", goodsShop1.getRealPrice());//10
//                                map.put("priceTotal", goodsShop1.getRealPrice() * cartGoodsListDto.getQTY());
                                map.put("priceTotal", BigDecimal.valueOf(goodsShop1.getRealPrice())
                                        .multiply(BigDecimal.valueOf(cartGoodsListDto.getQTY()))
                                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
                            }

                        }
                    }
                    memberOrderGoodsService.insertGoodsByMap(map);
                    // 删除购物车商品
                    cartGoodsService.deleteGoodsFromCart(new HashMap<String, Object>() {
                        {
                            put("cartGoodsID", cartGoodsListDto.getCartGoodsID());
                        }
                    });
                }
                // 修改购物车当前商品数量
                if (StringUtils.isNotBlank(cartID)) {
//                    List<ApiCartGoodsListDto> cartNumList = cartGoodsService.getCartGoodsList(new HashMap<String, Object>() {
//                        {
//                            put("memberID", member.getId());
//                            put("isSelect", 1);
//                        }
//                    });
//                    if (!cartNumList.isEmpty() && cartNumList.size() > 0) {
//                        map.clear();
//                        map.put("id", cartID);
//                        map.put("QTY", cartNumList.size());
//                        cartService.updateOneCart(map);
//                    }else if(!cartNumList.isEmpty()){
//                        map.clear();
//                        map.put("id", cartID);
//                        map.put("QTY", 0);
//                        cartService.updateOneCart(map);
//                    }
                    map.clear();
                    map.put("cartID", cartID);
                    cartService.updateClearCart(map);
                }
            }

        } else {//购物车为空
            log.info("直接购买：byChart");
            GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
            if (goodsShop.getReserveNumber() != null && goodsShop.getReserveNumber() > 0) {
                num = goodsShop.getReserveNumber();
            }
            map.clear();
            map.put("id", IdGenerator.uuid32());
            map.put("applicationID", applicationID);
            map.put("companyID", companyID);
            map.put("shopID", shopID);
            map.put("memberID", member.getId());
            map.put("memberOrderID", memberOrderID);
            map.put("goodsShopID", goodsShopID);
            map.put("priceTotalReturn", 0);
            map.put("priceReturn", 0);
            map.put("QTYWeighed", 0);
            map.put("giveQTY", 0);
            map.put("weighTotal", 0);
            map.put("weightTotalFee", 0);
            map.put("weighPrice", 0);
            map.put("tax", 0);
            map.put("discountRate", 0);
            map.put("majorID", majorID);
            log.info("直接购买map参数初始化完毕");
            log.info("orderType：" + orderType);

            if ("82".equals(orderType)
                    || "83".equals(orderType)
                    || "85".equals(orderType)
                    || "84".equals(orderType)) {
                if (StringUtils.isNotBlank(memberBuyReadID)) {
                    MemberBuyReadDetailDto memberBuyReadDto = memberBuyReadService.getOneMemberBuyReadDetail(new HashMap<String, Object>() {
                        {
                            put("id", memberBuyReadID);
                        }
                    });
                    if (memberBuyReadDto != null) {
                        Story story = storyService.getStory(memberBuyReadDto.getObjectID());
                        if (story != null) {
                            if (story.getListImage() != null) {
                                map.put("listImage", story.getListImage());
                            }
                            if (story.getName() != null) {
                                map.put("name", story.getName());
                            }
                            if (story.getShortDescription() != null) {
                                map.put("description", story.getShortDescription());
                            }
                            if (memberBuyReadDto.getCashPrice() != null) {
                                map.put("priceStand", memberBuyReadDto.getCashPrice());
                                map.put("priceNow", memberBuyReadDto.getCashPrice());//11
                                map.put("priceTotal", memberBuyReadDto.getCashPrice());
                            }
                        }
                    }
                }
                //todo
            } else if (orderType.equals("70")) {
                if (StringUtils.isBlank(goodsQTY)) {
                    return MessagePacket.newFail(MessageHeader.Code.fail, "goodsQTY 不能为空");
                }

                if (StringUtils.isBlank(goodsShopID)) {
                    return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID 不能为空");
                }
                if (goodsShop == null) {
                    return MessagePacket.newFail(MessageHeader.Code.goodsShopIDNotFound, "goodsShopID不正确");
                }
                String appointmentIDs = request.getParameter("appointmentIDs");

                List<String> appointmentList = new ArrayList<>();
                if (StringUtils.isNotBlank(appointmentIDs) && appointmentIDs.indexOf(",") > 0) {
                    appointmentList = Arrays.stream(appointmentIDs.split(",")).collect(Collectors.toList());
                } else {
                    appointmentList.add(appointmentIDs);
                }
                int number = 0;
                int a = 10;
                if (goodsShop.getReserveNumber() != null) {
//                    number = goodsShop.getReserveNumber() * Integer.parseInt(goodsQTY);
                    number = BigDecimal.valueOf(goodsShop.getReserveNumber())
                            .multiply(BigDecimal.valueOf(Integer.parseInt(goodsQTY)))
                            .setScale(2, RoundingMode.HALF_UP)
                            .intValue();
                }
                if (number < appointmentList.size()) {
                    return MessagePacket.newFail(MessageHeader.Code.numberNot, "商品数量不足: number =" + number + " appointmentList.size=" + appointmentList.size()
                            + " goodsQTY=" + goodsQTY + " reserveNumber=" + goodsShop.getReserveNumber());
                }
//                当前 orderType传入 70 表示 要创建1个 预约订单。同时，要传入goodsShopID，去查看 goodsShop里面的 reserveNumber 预约个数=A。
//                去查看 传入的参数 appointmentIDs传入的已经创建的预约ID数组，这里ID的个数= B，去查看传入参数 goodsQTY 选择的商品个数C。
//                先计算 AXC 表示购买的预约个数，如果小于B，则提示商品个数不足，报错退出。如果大于B 则 需要创建（AXC-B）个数的 appointment记录。
//                接着创建memberOrder记录，并同时创建1个memberOrderGoods记录。
//                然后，修改全部 appointment记录中的 memberOrderID，memberOrderStatus 字段。
                String ids = IdGenerator.uuid32();
                if (number > appointmentList.size()) {
                    int sum = number - appointmentList.size();
                    for (int i = 0; i < sum; i++) {
                        Map<String, Object> appointmentMap = new HashMap<>();
                        appointmentMap.put("id", IdGenerator.uuid32());
                        appointmentMap.put("memberOrderID", memberOrderID);
                        appointmentMap.put("status", 1);
                        appointmentMap.put("applicationID", member.getApplicationID());
                        appointmentMap.put("companyID", member.getCompanyID());
                        appointmentMap.put("name", goodsShop.getName());
                        appointmentMap.put("applyMemberID", member.getId());
                        appointmentMap.put("memberOrderStatus", 1);
                        appointmentService.insertByMap(appointmentMap);
                    }
                }

                for (String appointmentID : appointmentList) {
                    Appointment appointment = appointmentService.getById(appointmentID);
                    if (appointment == null) {
                        return MessagePacket.newFail(MessageHeader.Code.appointmentIDNotFound, "appointmentID 不正确");
                    }
                    Map<String, Object> appointmentMap = new HashMap<>();
                    appointmentMap.put("id", appointment.getId());
                    appointmentMap.put("memberOrderID", memberOrderID);
                    appointmentMap.put("memberOrderStatus", 1);
                    appointmentService.updateByMap(appointmentMap);
                }

            } else if ("81".equals(orderType)
                    && StringUtils.isNotBlank(goldenGetID)) {
                GoldenGet goldenGet = goldenGetService.getByID(goldenGetID);
                if (goldenGet != null) {
                    map.put("priceStand", goldenGet.getNumber());
                    map.put("priceNow", goldenGet.getPayAMount());//12
                    map.put("priceTotal", goldenGet.getPayAMount());
                }
                if (goodsShop.getLittleImage() != null) {
                    map.put("listImage", goodsShop.getLittleImage());
                }
                if (goodsShop.getName() != null) {
                    map.put("name", goodsShop.getName());
                }
            } else {
                log.info("orderType啥也不是，走的else");
                if (goodsShop.getStandPrice() != null) {
                    map.put("priceStand", goodsShop.getStandPrice());
                }
                if (goodsShop.getRealPrice() != null) {
                    map.put("priceNow", goodsShop.getRealPrice());//14
                    map.put("priceTotal", goodsShop.getRealPrice());
                }
                if (goodsShop.getLittleImage() != null) {
                    map.put("listImage", goodsShop.getLittleImage());
                }
                if (goodsShop.getName() != null) {
                    map.put("name", goodsShop.getName());
                }
                //直接购买如果goodsPriceID不为空，就使用对应
                if (goodsPriceDto != null) {
                    log.info("priceTotal_::" + priceTotal_);
                    map.put("priceNow", priceTotal_);//15
                    map.put("priceTotal", priceTotal_);
                }
            }

            if ("60".equals(orderType) || StringUtils.isNotBlank(memberTrainID)) {
                //"60".equals(orderType)
                log.info("orderType：60" + "memberTrainID：" + memberTrainID);
                MemberTrain memberTrain = memberTrainService.findById(memberTrainID);
                if (memberTrain != null) {
//                log.info("memberTrain："+memberTrain);
//                log.info("memberTrain："+memberTrain.getPriceEnd());
                    map.put("priceStand", memberTrain.getPriceEnd());
                    map.put("priceNow", memberTrain.getPriceEnd());//13
                    map.put("priceTotal", memberTrain.getPriceEnd());
                    if (StringUtils.isNotBlank(memberTrain.getTrainCourseID())) {
                        log.info("走的：getTrainCourseID");
                        objectID = memberTrain.getTrainCourseID();
                        TrainCourse trainCourse = trainCourseService.findById(memberTrain.getTrainCourseID());
                        if (trainCourse.getListImage() != null) {
                            map.put("listImage", trainCourse.getListImage());
                        }
                        if (trainCourse.getName() != null) {
                            map.put("name", trainCourse.getName());
                        }
                    }
                    if (StringUtils.isNotBlank(memberTrain.getTrainBusinessID())) {
                        log.info("走的：getTrainBusinessID");
                        objectID = memberTrain.getTrainBusinessID();
                        String train_trainCourseID = memberTrain.getTrainCourseID();
                        String train_trainHourseID = memberTrain.getTrainHourID();
                        map.put("trainCourseID", train_trainCourseID);
                        map.put("trainHourID", train_trainHourseID);
                        TrainBusiness trainBusiness = trainBusinessService.findById(memberTrain.getTrainBusinessID());
                        String trainBusiness_goodsShopID = trainBusiness.getGoodsShopID();
                        if (StringUtils.isNotBlank(trainBusiness_goodsShopID)) {
                            goodsShopID = trainBusiness_goodsShopID;
                        }
                        map.put("goodsShopID", goodsShopID);
                        Double trainBusiness_priceCash = trainBusiness.getPriceCash();
                        if (trainBusiness_priceCash != 0d) {
                            map.put("priceNow", trainBusiness_priceCash);
                        }
//                    log.info("trainBusiness："+trainBusiness);
//                    log.info("trainBusiness："+trainBusiness.getListImage());
                        if (trainBusiness.getListImage() != null) {
                            map.put("listImage", trainBusiness.getListImage());
                        }
                        if (trainBusiness.getName() != null) {
                            map.put("name", trainBusiness.getName());
                        }
                        Map<String, Object> map1 = new HashMap<>();
                        map1.put("id", memberTrainID);
                        map1.put("memberOrderID", memberOrderID);
                        memberTrainService.updateByMap(map1);
                    }
                }
            }

            log.info("priceStand：" + map.get("priceStand"));
            log.info("priceNow：" + map.get("priceNow"));
            log.info("priceTotal：" + map.get("priceTotal"));
            if (goodsShop.getUseDescription() != null) {
                map.put("description", goodsShop.getDescription());
            }
            if (goodsShop.getUnitDescription() != null) {
                map.put("useDescription", goodsShop.getUnitDescription());
            }
            if (goodsShop.getGoodsID() != null) {
                map.put("goodsID", goodsShop.getGoodsID());
            }
            if (goodsShop.getGoodsCompanyID() != null) {
                map.put("goodsCompanyID", goodsShop.getGoodsCompanyID());

            }
            if (goodsShop.getTermCode() != null) {
                map.put("IDCode", goodsShop.getTermCode());
            }
            if (goodsShop.getShowName() != null) {
                map.put("showName", goodsShop.getShowName());
            }
            if (goodsShop.getTermCode() != null) {
                map.put("IDCode", goodsShop.getTermCode());
            }
            if (goodsShop.getShopID() != null) {
                map.put("shopID", goodsShop.getShopID());
            }
            if (goodsShop.getStandardWeigh() != null) {
                map.put("weighTotal", goodsShop.getStandardWeigh());
                map.put("weightTotalFee", goodsShop.getStandardWeigh());
                BigDecimal weightBig = BigDecimal.valueOf(weight);

                BigDecimal standardWeigh = BigDecimal.valueOf(goodsShop.getStandardWeigh());
                weightBig = weightBig.add(standardWeigh);

                weightBig = weightBig.setScale(3, RoundingMode.HALF_UP);

                weight = weightBig.doubleValue();
               //weight += goodsShop.getStandardWeigh();
            }
            if (objectID!=null&&StringUtils.isNotBlank(objectID)) {
                map.put("objectID", objectID);
            }
            if (StringUtils.isNotBlank(goodsQTY)) {
                map.put("QTY", Integer.valueOf(goodsQTY));//q9
            } else {
                map.put("QTY", 1);//q10
            }
            map.put("salesUnitQTY", 1);
            map.put("status", 1);
            if ("12".equals(payFrom)) {
                map.put("objectFeatureItemID1", priceAfterDiscount);
            }
            if ("6".equals(orderType) && "7".equals(payFrom) && StringUtils.isNotBlank(presentRecordID)) {
                // 礼品卡商品
                log.info("礼品卡商品");
            }
            log.info("直接购买map参数装填完毕");
            log.info("直接购买商品了，准备添加 memberOrderGoods");
            //如果订单存在（培训课程，根据会员id和trainBusinessid得到已经存在的订单id，），就不添加订单商品
            List<MemberOrderGoods> memberOrderGoodsList = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrderID);
            if (memberOrderGoodsList != null && memberOrderGoodsList.size() > 0) {
                log.info("不执行memberOrderGoods添加");
            } else {
                memberOrderGoodsService.insertGoodsByMap(map);
            }
            log.info("直接购买商品了添加完毕 memberOrderGoods");
        }
        // 修改订单的重量
        map.clear();
        map.put("id", memberOrderID);
        map.put("weighTotal", weight);
        map.put("weightTotalFee", weight);

        //直接购买，会员订单默认数量为1
        if (!"1".equals(byCart)) {
            map.put("goodsQTY", 1);
            if (StringUtils.isNotBlank(presentDefineID) && orderType.equals("6")) {
                map.put("goodsQTY", goodsQTY);
            }
        }
        memberOrderService.update(map);
        /**
         * 如果是礼品卡 创建订单，发送消息 到 队列中
         */
        if (orderType.equals("6") && StringUtils.isNotBlank(presentDefineID)) {
            sendMessageService.mcMemberBuyGiftCardMessages(member.getId(), presentDefineID, memberOrderID);
        }
        /**
         * 使用礼品卡 修改 礼品卡状态 变为已经用完
         */
        if ("6".equals(orderType) && "7".equals(payFrom) && StringUtils.isNotBlank(presentRecordID)) {
            map.clear();
            map.put("id", presentRecordID);
            map.put("usedTime", TimeTest.getTimeStr());
            map.put("status", 100);
            presentRecordService.updateByMap(map);
        }
        String description1 = String.format("标准创建订单new");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request,
                Memberlog.MemberOperateType.standardCreateOneMemberOrder);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("memberOrderID", memberOrderID);
        if (num >= 0) {
            rsMap.put("reserveNumber", num);
        }
        return MessagePacket.newSuccess(rsMap, "standardCreateOneMemberOrder success");
    }

    @ApiOperation(value = "standardCreateOneMemberOrderKZY", notes = "独立接口：科之翼创建订单new")
    @RequestMapping(value = "/standardCreateOneMemberOrderKZY", produces = {"application/json;charset=UTF-8"})
    public MessagePacket standardCreateOneMemberOrderKZY(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String memberOrderID = IdGenerator.uuid32();
        String objectID = null;
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//       Member member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        if (StringUtils.isBlank(member.getId())) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请重新登录");
        }
        map.put("memberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
            Member member1 = memberService.getMemberByRecommendCode(map);
            if (member1 != null) {
                map.put("recommendMemberID", member1.getId());
            }
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        } else {
            map.put("siteID", member.getSiteID());
        }

        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isNotBlank(payWayID)) {
            if (!paywayService.checkIsValidId(payWayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
            }
            map.put("payWayID", payWayID);
        }

        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!memberInvoiceDefineService.checkIsValidId(memberInvoiceDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }

        String memberMemo = request.getParameter("memberMemo");
        if (StringUtils.isNotBlank(memberMemo)) {
            map.put("memberMemo", memberMemo);
        }

        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }

        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }

        String actionGoodsType = request.getParameter("actionGoodsType");
        if (StringUtils.isNotBlank(actionGoodsType)) {
            if (!NumberUtils.isNumeric(actionGoodsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "actionGoodsType请输入数字");
            }
            map.put("actionGoodsType", actionGoodsType);
        }

        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "outPayType请输入数字");
            }
            map.put("outPayType", outPayType);
        }

        String buyType = request.getParameter("buyType");
        if (StringUtils.isNotBlank(buyType)) {
            if (!NumberUtils.isNumeric(buyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "buyType请输入数字");
            }
            map.put("buyType", buyType);
        }

        String financeType = request.getParameter("financeType");
        if (StringUtils.isNotBlank(financeType)) {
            if (!NumberUtils.isNumeric(financeType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "financeType请输入数字");
            }
            map.put("financeType", financeType);
        }

        boolean flag = false;
        String goodsCompanyID = request.getParameter("goodsCompanyID");
        if (StringUtils.isNotBlank(goodsCompanyID)) {
            if (!goodsCompanyService.checkIsValidId(goodsCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.goodsCompanyIDNotFound, "goodsCompanyID不正确");
            }
            map.put("goodsCompanyID", goodsCompanyID);
            // 判断订单是否重复
            List<MemberOrderGoodsDtoList> memberOrderGoodsDtoLists = memberOrderGoodsService.getMemberOrderGoodsDtoListByMap(new HashMap<String, Object>() {
                {
                    put("goodsCompanyID", goodsCompanyID);
                    put("orderMemberID", member.getId());
                    put("orderStatus", 1);
                }
            });
            if (!memberOrderGoodsDtoLists.isEmpty()) {
                memberOrderID = memberOrderGoodsDtoLists.get(0).getMemberOrderID();
                // 删除订单商品
                for (MemberOrderGoodsDtoList memberOrderGoods : memberOrderGoodsDtoLists) {
                    memberOrderGoodsService.updateByMap(new HashMap<String, Object>() {
                        {
                            put("id", memberOrderGoods.getMemberOrderGoodsID());
                            put("isValid", 0);
                        }
                    });
                }
                flag = true;
            }
        }

        String buyCompanyID = request.getParameter("buyCompanyID");
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (!companyService.checkIdIsValid(buyCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "buyCompanyID不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }

        Double priceAfterDiscount = 0.0d;
        Integer goodsNumbers = 1;
        Integer goodsQTY = 0;
        Double weighTotal = 0.0;
        String byCart = request.getParameter("byCart");
        if (StringUtils.isBlank(goodsCompanyID) && StringUtils.isBlank(byCart)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "byCart和goodsCompanyID不能同时为空");
        }
        if (StringUtils.isNotBlank(byCart) && "1".equals(byCart)) {
            List<ApiCartGoodsListDto> cartGoodsList = cartGoodsService.getCartGoodsList(new HashMap<String, Object>() {
                {
                    put("memberID", member.getId());
                    put("isSelect", 1);
                }
            });
            if (!cartGoodsList.isEmpty()) {
                goodsNumbers = cartGoodsList.size();
                for (ApiCartGoodsListDto cartGoodsListDto : cartGoodsList) {
                    Double priceStand = 0.0d;
                    Double priceNow = 0.0d;//15
                    Double platServicePrice = 0.0d;
                    // 重新计算价格
                    Integer QTY = 0;//q11
                    if (cartGoodsListDto.getQTY() != null) {
//                        goodsQTY += cartGoodsListDto.getQTY();
                        QTY = cartGoodsListDto.getQTY();//q12
                        goodsQTY = BigDecimal.valueOf(goodsQTY).add(BigDecimal.valueOf(cartGoodsListDto.getQTY())).intValue();

                    }
                    if (cartGoodsListDto.getPriceStand() != null) {
                        priceStand = cartGoodsListDto.getPriceStand();
                    }
                    if (cartGoodsListDto.getPlatServicePrice() != null) {
                        platServicePrice = cartGoodsListDto.getPlatServicePrice();
                    }
                    if (cartGoodsListDto.getPriceNow() != null) {
                        priceNow = cartGoodsListDto.getPriceNow();//16
                    }
//                    priceAfterDiscount += ((platServicePrice + priceStand + priceNow) * QTY);//q13
                    priceAfterDiscount += (BigDecimal.valueOf(BigDecimal.valueOf(platServicePrice).
                                    add(BigDecimal.valueOf(priceStand))
                                    .add(BigDecimal.valueOf(priceNow)).doubleValue())
                            .multiply(BigDecimal.valueOf(QTY))
                            .setScale(2, RoundingMode.HALF_UP).doubleValue());//q13
                }
                map.put("priceTotal", priceAfterDiscount);
            } else {
                return MessagePacket.newFail(MessageHeader.Code.fail, "购物车中无商品");
            }
        } else {
            GoodsCompanyDto goodsCompany = goodsCompanyService.getGoodsCompanyDtoById(goodsCompanyID);
            if (goodsCompany != null) {
                Double standPrice = 0.0d;
                if (goodsCompany.getStandPrice() != null) {
                    standPrice = goodsCompany.getStandPrice();
                }
                Double realPrice = 0.0d;
                if (goodsCompany.getRealPrice() != null) {
                    realPrice = goodsCompany.getRealPrice();
                }
                Double platServicePrice = 0.0d;
                if (goodsCompany.getPlatServicePrice() != null) {
                    platServicePrice = goodsCompany.getPlatServicePrice();
                }
//                priceAfterDiscount = standPrice + realPrice + platServicePrice;
                priceAfterDiscount = BigDecimal.valueOf(standPrice)
                        .add(BigDecimal.valueOf(realPrice))
                        .add(BigDecimal.valueOf(platServicePrice)).doubleValue();
                map.put("priceTotal", priceAfterDiscount);
            }
        }

        map.put("closedPriceTotal", priceAfterDiscount);
        map.put("priceAfterDiscount", priceAfterDiscount);

        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payFrom请输入数字");
            }
        } else {
            return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom不能为空");
        }
        map.put("payFrom", payFrom);

        // 创建会员订单
        String applicationID = member.getApplicationID();
        String companyID = member.getCompanyID();
        String orderCode = sequenceDefineService.genCode("orderSeq", memberOrderID);
        map.put("orderCode", orderCode);
        map.put("id", memberOrderID);
        map.put("applicationID", applicationID);
        map.put("companyID", companyID);
        map.put("name", member.getName() + "购买公司商品");
        map.put("applyTime", TimeTest.getTime());
        map.put("goodsNumbers", goodsNumbers);
        map.put("goodsQTY", goodsQTY);
        map.put("weighTotal", 0);
        map.put("weightTotalFee", 0);
        map.put("sendPrice", 0);
        map.put("pointUsed", 0);
        map.put("pointTotal", 0);
        map.put("rebateTotal", 0);
        map.put("beanTotal", 0);
        map.put("subPrePay", 0);
        map.put("pointSubAmount", 0);
        map.put("rebateSubAmount", 0);
        map.put("bonusAmount", 0);
        map.put("sendStatus", 0);
        map.put("weighPrice", 0);
        map.put("discountAmount", 0);
        map.put("orderType ", 90);
        map.put("tax", 0);
        map.put("payStatus", 0);
        map.put("presentPayAmount", 0);
        map.put("sendType", 1);
        map.put("sendFee", 0);
        map.put("returnAmount", 0);
        map.put("isMemberHide", 0);
        map.put("status", 1);
        map.put("contactName", member.getName());
        map.put("phone", member.getPhone());
        map.put("creator", member.getId());
        // 判断是否有重复订单，有就编辑并删除订单中的商品
        if (StringUtils.isNotBlank(goodsCompanyID)) {
            if (flag) {
                memberOrderService.update(map);
            } else {
                memberOrderService.insertMemberOrder(map);
            }
        } else {
            memberOrderService.insertMemberOrder(map);
        }

        // 创建memberOrderGoods
        // 计算总重量
        Double weight = 0.0d;
        if (StringUtils.isNotBlank(byCart)) {
            if ("1".equals(byCart)) {
                List<ApiCartGoodsListDto> cartGoodsList = cartGoodsService.getCartGoodsList(new HashMap<String, Object>() {
                    {
                        put("memberID", member.getId());
                        put("isSelect", 1);
                    }
                });
                String cartID = cartGoodsList.get(0).getCartID();
                if (!cartGoodsList.isEmpty() && cartGoodsList.size() > 0) {
                    for (ApiCartGoodsListDto cartGoodsListDto : cartGoodsList) {
                        GoodsCompanyDto goodsCompanyDto = goodsCompanyService.getGoodsCompanyDtoById(cartGoodsListDto.getGoodsCompanyID());
                        map.clear();
                        map.put("id", IdGenerator.uuid32());
                        map.put("applicationID", applicationID);
                        map.put("companyID", companyID);
                        map.put("memberID", member.getId());
                        map.put("memberOrderID", memberOrderID);
                        map.put("goodsCompanyID", cartGoodsListDto.getGoodsCompanyID());
                        map.put("priceTotalReturn", 0);
                        map.put("priceReturn", 0);
                        map.put("QTYWeighed", 0);
                        map.put("giveQTY", 0);
                        map.put("weighTotal", 0);
                        map.put("weightTotalFee", 0);
                        map.put("weighPrice", 0);
                        map.put("tax", 0);
                        map.put("discountRate", 0);
                        if (goodsCompanyDto.getDescription() != null) {
                            map.put("description", goodsCompanyDto.getDescription());
                        }
                        if (goodsCompanyDto.getUnitDescription() != null) {
                            map.put("useDescription", goodsCompanyDto.getUnitDescription());
                        }
                        if (goodsCompanyDto.getGoodsID() != null) {
                            map.put("goodsID", goodsCompanyDto.getGoodsID());
                        }
                        if (goodsCompanyDto.getTermCode() != null) {
                            map.put("IDCode", goodsCompanyDto.getTermCode());
                        }
                        if (goodsCompanyDto.getTermCode() != null) {
                            map.put("IDCode", goodsCompanyDto.getTermCode());
                        }
                        map.put("QTY", cartGoodsListDto.getQTY());//q14
                        map.put("salesUnitQTY", cartGoodsListDto.getQTY());
                        map.put("status", 1);
                        if (goodsCompanyDto.getShowName() != null) {
                            map.put("showName", goodsCompanyDto.getShowName());
                        }

                        if (goodsCompanyDto.getName() != null) {
                            map.put("name", goodsCompanyDto.getName());
                        }
                        if (goodsCompanyDto.getLittleImage() != null) {
                            map.put("listImage", goodsCompanyDto.getLittleImage());
                        } else if (goodsCompanyDto.getLargerImage() != null) {
                            map.put("listImage", goodsCompanyDto.getLargerImage());
                        }
                        Double priceStand = 0.0d;
                        Double priceNow = 0.0d;//17
                        Double platServicePrice = 0.0d;
                        if (cartGoodsListDto.getPriceStand() != null) {
                            priceStand = cartGoodsListDto.getPriceStand();
                            map.put("priceStand", cartGoodsListDto.getPriceStand());
                        }
                        if (cartGoodsListDto.getPriceNow() != null) {
                            priceNow = cartGoodsListDto.getPriceNow();//18
                            map.put("priceNow", cartGoodsListDto.getPriceNow());//19
                        }
                        if (cartGoodsListDto.getPlatServicePrice() != null) {
                            platServicePrice = cartGoodsListDto.getPlatServicePrice();
                            map.put("platServicePrice", cartGoodsListDto.getPlatServicePrice());
                        }
//                        map.put("priceTotal", (platServicePrice + priceStand + priceNow) * cartGoodsListDto.getQTY());
                        map.put("priceTotal", BigDecimal.valueOf(
                                        BigDecimal.valueOf(platServicePrice)
                                                .add(BigDecimal.valueOf(priceStand))
                                                .add(BigDecimal.valueOf(priceNow)).doubleValue()
                                ).multiply(BigDecimal.valueOf(cartGoodsListDto.getQTY()))
                                .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        memberOrderGoodsService.insertGoodsByMap(map);
                        // 删除购物车商品
                        cartGoodsService.deleteGoodsFromCart(new HashMap<String, Object>() {
                            {
                                put("cartGoodsID", cartGoodsListDto.getCartGoodsID());
                            }
                        });
                    }
                }
                // 修改购物车当前商品数量
                if (StringUtils.isNotBlank(cartID)) {
                    List<ApiCartGoodsListDto> cartNumList = cartGoodsService.getCartGoodsList(new HashMap<String, Object>() {
                        {
                            put("memberID", member.getId());
                            put("isSelect", 1);
                        }
                    });
                    if (!cartNumList.isEmpty() && cartNumList.size() > 0) {
                        map.clear();
                        map.put("id", cartID);
                        map.put("QTY", cartNumList.size());//q15
                        cartService.updateOneCart(map);
                    }
                }
            }
        } else {
            GoodsCompanyDto goodsCompanyDto = goodsCompanyService.getGoodsCompanyDtoById(goodsCompanyID);
            map.clear();
            map.put("id", IdGenerator.uuid32());
            map.put("applicationID", applicationID);
            map.put("companyID", companyID);
            map.put("memberID", member.getId());
            map.put("memberOrderID", memberOrderID);
            map.put("goodsCompanyID", goodsCompanyDto.getId());
            map.put("priceTotalReturn", 0);
            map.put("priceReturn", 0);
            map.put("QTYWeighed", 0);
            map.put("giveQTY", 0);
            map.put("weighTotal", 0);
            map.put("weightTotalFee", 0);
            map.put("weighPrice", 0);
            map.put("tax", 0);
            map.put("discountRate", 0);
            Double priceStand = 0.0d;
            Double priceNow = 0.0d;//20
            Double platServicePrice = 0.0d;
            if (goodsCompanyDto.getStandPrice() != null) {
                priceStand = goodsCompanyDto.getStandPrice();
                map.put("priceStand", goodsCompanyDto.getStandPrice());
            }
            if (goodsCompanyDto.getRealPrice() != null) {
                priceNow = goodsCompanyDto.getRealPrice();//21
                map.put("priceNow", goodsCompanyDto.getRealPrice());//22
            }
            if (goodsCompanyDto.getPlatServicePrice() != null) {
                platServicePrice = goodsCompanyDto.getPlatServicePrice();
                map.put("platServicePrice", goodsCompanyDto.getPlatServicePrice());
            }
//            map.put("priceTotal", platServicePrice + priceStand + priceNow);
            map.put("priceTotal", BigDecimal.valueOf(platServicePrice)
                    .add(BigDecimal.valueOf(priceStand))
                    .add(BigDecimal.valueOf(priceNow)).doubleValue());
            if (goodsCompanyDto.getLittleImage() != null) {
                map.put("listImage", goodsCompanyDto.getLittleImage());
            }
            if (goodsCompanyDto.getName() != null) {
                map.put("name", goodsCompanyDto.getName());
            }
            if (goodsCompanyDto.getDescription() != null) {
                map.put("description", goodsCompanyDto.getDescription());
            }
            if (goodsCompanyDto.getUnitDescription() != null) {
                map.put("useDescription", goodsCompanyDto.getUnitDescription());
            }
            if (goodsCompanyDto.getGoodsID() != null) {
                map.put("goodsID", goodsCompanyDto.getGoodsID());
            }
            if (goodsCompanyDto.getTermCode() != null) {
                map.put("IDCode", goodsCompanyDto.getTermCode());
            }
            if (goodsCompanyDto.getShowName() != null) {
                map.put("showName", goodsCompanyDto.getShowName());
            }
            map.put("QTY", 1);//16
            map.put("salesUnitQTY", 1);
            map.put("status", 1);
            memberOrderGoodsService.insertGoodsByMap(map);
        }
        String description1 = String.format("科之翼创建订单new");
        sendMessageService.sendMemberLogMessage(sessionID, description1, request,
                Memberlog.MemberOperateType.standardCreateOneMemberOrderKZY);
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("memberOrderID", memberOrderID);
        return MessagePacket.newSuccess(rsMap, "standardCreateOneMemberOrderKZY success");
    }

    @RequestMapping(value = "/refreshOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket refreshOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member=memberService.selectById("e8f9dcd3013b4e10bff0b14b4ff20599");
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        MemberLogin memberLogin = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
        if (memberLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder==null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);

        String expressID = request.getParameter("expressID");
        if (StringUtils.isNotBlank(expressID)) {
            if (!expressService.checkIsValid(expressID)) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDFound, "expressID不正确");
            }
            map.put("expressID", expressID);
        }


        //加这个参数，1是 保存到 memberOrder对应的字段，2是获取 里面的 名称name，
        // 联系人contactName姓名 ，和电话phone，填写到memberOrder
        // 里面的memberAddressName ，contactName, phone 等3个字段；
        //并检查 memberAddress 里面 的 mapX 和 mapY，如果不为空，再 检查 memberOrder里面的 shop的 mapX 和mapY，如果都不为空，则计算 这2个的 距离，并返回 distance（距离，单位是米）。这是计算 运费的依据，运费暂时不计算。
        Double distance = null;
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            MemberAddress memberaddress = memberAddressService.findById(memberAddressID);
            if (memberaddress==null) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
            map.put("memberAddressName", memberaddress.getName());
            map.put("contactName", memberaddress.getContactName());
            map.put("phone", memberaddress.getPhone());
            if (StringUtils.isNotBlank(memberOrder.getShopID())) {
                OutShopDto shopDto = shopService.getOutSystemDtoByIdOrOutId(memberOrder.getShopID());
                if (shopDto != null) {
                    String shopDtoMAPX = shopDto.getMapX();
                    String shopDtoMAPY = shopDto.getMapY();
                    String memberaddressmapX = memberaddress.getMapX();
                    String memberaddressmapY = memberaddress.getMapY();

                    if (StringUtils.isNoneBlank(shopDtoMAPX, shopDtoMAPY, memberaddressmapX, memberaddressmapY)) {
                        try {
                            double shopLon = Math.toRadians(Double.parseDouble(shopDtoMAPX)); // 店铺经度
                            double shopLat = Math.toRadians(Double.parseDouble(shopDtoMAPY)); // 店铺纬度
                            double memberLon = Math.toRadians(Double.parseDouble(memberaddressmapX)); // 会员地址经度
                            double memberLat = Math.toRadians(Double.parseDouble(memberaddressmapY)); // 会员地址纬度

                            double deltaLat = memberLat - shopLat;
                            double deltaLon = memberLon - shopLon;
                            double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                                    + Math.cos(shopLat) * Math.cos(memberLat)
                                    * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
                            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                            double earthRadius = 6371000;
                            distance = earthRadius * c; // 最终距离（单位：米）
                        } catch (NumberFormatException e) {
                            log.error("经纬度格式错误，无法计算距离：" + e.getMessage());
                        }
                    } else {
                        log.error("经纬度为空，无法计算距离");
                    }
                }
            }

        }

        String memberMemo = request.getParameter("memberMemo");
        if (StringUtils.isNotBlank(memberMemo)) {
            map.put("memberMemo", memberMemo);
        }

        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            if ("6".equals(sendType)&&StringUtils.isNotBlank(memberAddressID)){
                return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "sendType为6时memberOrderID不能为空");
            }
            map.put("sendType", sendType);
        }

        String pointUsed = request.getParameter("pointUsed");
        if (StringUtils.isNotBlank(pointUsed)) {
            map.put("pointUsed", pointUsed);
        }

        String pointSubAmount = request.getParameter("pointSubAmount");
        if (StringUtils.isNotBlank(pointSubAmount)) {
            map.put("pointSubAmount", pointSubAmount);
        }


        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID");
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!memberInvoiceDefineService.checkIsValidId(memberInvoiceDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }

        String reservedDate = request.getParameter("reservedDate");
        if (StringUtils.isNotBlank(reservedDate)) {
            map.put("reservedDate", reservedDate);
        }

        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            map.put("payFrom", payFrom);
        }

        String buyWithVip = request.getParameter("buyWithVip");

        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "outPayType请输入数字");
            } else {
                map.put("outPayType", outPayType);
            }
        }

        // 处理价格
        Double priceTotal = 0.0d;
        Double bonusAmount = 0.0d;
        Boolean isVip = false;
        // 判断当前会员的类型
        String memberBonusArray = request.getParameter("memberBonusArray");
        List<MyMemberMajorListDto> memberMajorList = memberMajorService.getMemberMajorList(new HashMap<String, Object>() {
            {
                put("memberID", member.getId());
                put("majorID", "ff808081798df2a201798e518e7e008b");
                put("effEndDate", TimeTest.getTime());
            }
        });
        if (!memberMajorList.isEmpty()) {
            isVip = true;
        }
        //MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder != null) {
            Integer orderType = memberOrder.getOrderType();
            switch (orderType) {
                case 82:
                case 83:
                case 84:
                    String memberBuyReadID = memberOrder.getMemberBuyReadID();
                    if (StringUtils.isNotBlank(memberBuyReadID)) {
                        MemberBuyReadDetailDto memberBuyRead =
                                memberBuyReadService.getOneMemberBuyReadDetail(new HashMap<String, Object>() {
                                    {
                                        put("id", memberBuyReadID);
                                    }
                                });
                        if (memberBuyRead != null) {
                            Story story = storyService.getStory(memberBuyRead.getObjectID());
                            if (story != null) {
                                if (isVip) {
                                    priceTotal = story.getCashVIPPrice();
                                } else {
                                    priceTotal = story.getCashPrice();
                                }
                            }
                        }
                    }
                    break;
                case 60:
                    String memberTrainID = memberOrder.getMemberTrainID();
                    if (StringUtils.isNotBlank(memberTrainID)) {
                        MemberTrain memberTrain = memberTrainService.findById(memberTrainID);
                        if (memberTrain != null) {
                            TrainBusiness trainBusiness = trainBusinessService.findById(memberTrain.getTrainBusinessID());
                            if (trainBusiness != null) {
                                if (isVip) {
                                    priceTotal = trainBusiness.getPriceVIPCash();
                                } else {
                                    priceTotal = trainBusiness.getPriceCash();
                                }
                            }
                        }
                    }
                    break;
                case 62:
                    if (StringUtils.isNotBlank(memberOrder.getMemberTrainID())) {
                        MemberTrain memberTrain = memberTrainService.findById(memberOrder.getMemberTrainID());
                        if (memberTrain != null) {
                            TrainCourse trainCourse = trainCourseService.findById(memberTrain.getTrainCourseID());
                            if (trainCourse != null) {
                                if (isVip) {
                                    priceTotal = trainCourse.getPriceVIPCash();
                                } else {
                                    priceTotal = trainCourse.getPriceCash();
                                }
                            }
                        }
                    }
                    break;
                case 81:
                    priceTotal = memberOrder.getPriceTotal();
                    break;
                case 31:
                    List<MemberOrderGoods> memberOrderGoodsList = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrder.getId());
                    if (!memberOrderGoodsList.isEmpty()) {
                        // MemberOrderGoods memberOrderGoods = memberOrderGoodsList.get(0);
                        for (MemberOrderGoods memberOrderGoods : memberOrderGoodsList) {
                            String goodsShopID = "";
                            if (StringUtils.isNotBlank(memberOrderGoods.getObjectID())) {
                                goodsShopID = memberOrderGoods.getObjectID();
                            } else {
                                goodsShopID = memberOrderGoods.getGoodsShopID();
                            }
                            if (StringUtils.isNotBlank(goodsShopID)) {
                                GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                                if (isVip) {
                                    priceTotal = goodsShop.getRealPriceVIP();
                                } else {
                                    priceTotal = goodsShop.getRealPrice();
                                }
                            }
                        }
                    }
                    break;
                case 70:
                    List<MemberOrderGoods> memberOrderGoods1 = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrder.getId());
                    if (!memberOrderGoods1.isEmpty()) {
                        for (MemberOrderGoods memberOrderGoods : memberOrderGoods1) {
                            Integer qty = memberOrderGoods.getQTY();//q17
                            if (StringUtils.isNotBlank(buyWithVip)) {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();

                                if (StringUtils.isNotBlank(goodsShopID)) {
//                                    priceTotal += memberOrderGoods.getPriceTotal();
                                    priceTotal = BigDecimal.valueOf(priceTotal).add(BigDecimal.valueOf(memberOrderGoods.getPriceTotal())).doubleValue();

                                }
                            } else {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();
                                if (StringUtils.isNotBlank(goodsShopID)) {
                                    GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                                    if (isVip) {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPriceVIP())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();

                                    } else {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPrice())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                }
                            }
                        }
                    }
                    break;
                case 33:
                    List<MemberOrderGoods> memberOrderGoods2 = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrder.getId());
                    if (!memberOrderGoods2.isEmpty()) {
                        for (MemberOrderGoods memberOrderGoods : memberOrderGoods2) {
                            Integer qty = memberOrderGoods.getQTY();
                            if (StringUtils.isNotBlank(buyWithVip)) {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();

                                if (StringUtils.isNotBlank(goodsShopID)) {
//                                    priceTotal += memberOrderGoods.getPriceTotal();
                                    priceTotal = BigDecimal.valueOf(priceTotal)
                                            .add(BigDecimal.valueOf(memberOrderGoods.getPriceTotal())).doubleValue();
                                }
                            } else {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();
                                if (StringUtils.isNotBlank(goodsShopID)) {
                                    GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                                    if (isVip) {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPriceVIP())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    } else {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPrice())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                }
                            }
                        }
                    }
                    break;
                case 22:
                    List<MemberOrderGoods> memberOrderGoods3 = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrder.getId());
                    if (!memberOrderGoods3.isEmpty()) {
                        for (MemberOrderGoods memberOrderGoods : memberOrderGoods3) {
                            Integer qty = memberOrderGoods.getQTY();
                            if (StringUtils.isNotBlank(buyWithVip)) {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();

                                if (StringUtils.isNotBlank(goodsShopID)) {
                                    priceTotal += memberOrderGoods.getPriceTotal();
                                }
                            } else {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();
                                if (StringUtils.isNotBlank(goodsShopID)) {
                                    GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                                    if (isVip) {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPriceVIP())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    } else {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPrice())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                }
                            }
                        }
                    }
                    break;
                case 1:
                    List<MemberOrderGoods> memberOrderGoodsList1 = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrder.getId());
                    if (!memberOrderGoodsList1.isEmpty()) {
                        for (MemberOrderGoods memberOrderGoods : memberOrderGoodsList1) {
                            Integer qty = memberOrderGoods.getQTY();
                            if (StringUtils.isNotBlank(buyWithVip)) {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();

                                if (StringUtils.isNotBlank(goodsShopID)) {
//                                    priceTotal += memberOrderGoods.getPriceTotal();
                                    priceTotal = BigDecimal.valueOf(priceTotal)
                                            .add(BigDecimal.valueOf(memberOrderGoods.getPriceTotal())).doubleValue();
                                }
                            } else {
                                String goodsShopID = memberOrderGoods.getGoodsShopID();
                                if (StringUtils.isNotBlank(goodsShopID)) {
                                    GoodsShop goodsShop = goodsShopService.getGoodsShopByID(goodsShopID);
                                    if (isVip) {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPriceVIP())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();

                                    } else {
                                        priceTotal = BigDecimal.valueOf(priceTotal)
                                                .add(BigDecimal.valueOf(goodsShop.getRealPrice())
                                                        .multiply(BigDecimal.valueOf(qty))
                                                        .setScale(2, RoundingMode.HALF_UP)).doubleValue();
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            // 从新计算红包价格
            if (StringUtils.isNotBlank(memberBonusArray)) {
                // 讲原先的红包设置为空
                List<MemberBonusList> list = memberBonusService.outSystemGetMemberBonusList(new HashMap<String, Object>() {
                    {
                        put("memberOrderID", memberOrderID);
                    }
                });
                if (!list.isEmpty()) {
                    for (MemberBonusList memberBonus : list) {
                        String memberBonusID = memberBonus.getId();
                        memberBonusService.updateByMap(new HashMap<String, Object>() {
                            {
                                put("memberOrderNull", 1);
                                put("id", memberBonusID);
                            }
                        });
                    }
                }
                String[] memberBonusIDArray = memberBonusArray.split(",");
                List<MemberBonus> memberBonusList = new ArrayList<>();
                Integer isMultiple = 1;
                for (String memberBonusID : memberBonusIDArray) {
                    // 检查会员红包是否存在和可用
                    MemberBonus memberBonus = memberBonusService.getMemberBonusByIdOrCode(new HashMap<String, Object>() {
                        {
                            put("id", memberBonusID);
                        }
                    });
                    if (memberBonus != null) {
                        // 判断状态是否可用
                        if (memberBonus.getStatus() != null && memberBonus.getStatus() != 10) {
                            return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "红包状态不正确无法使用");
                        }
                        // 判断叠加是否可用
                        if (memberBonus.getIsMultiple() != null && memberBonus.getIsMultiple() == 0) {
                            if (isMultiple == 0) {
                                return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "部分红包无法叠加");
                            } else {
                                isMultiple = 0;
                            }
                        }
                        // 判断指定商品是否可用
                        if (StringUtils.isNotBlank(memberBonus.getGoodsShopID())) {
                            List<MemberOrderGoodsDtoList> memberOrderGoodsDtoLists = memberOrderGoodsService.getMemberOrderGoodsDtoListByMap(new HashMap<String, Object>() {
                                {
                                    put("memberOrderID", memberOrderID);
                                    put("goodsShopID", memberBonus.getGoodsShopID());
                                }
                            });
                            if (memberOrderGoodsDtoLists.isEmpty()) {
                                return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "订单不存在指定商品无法使用");
                            }
                        }
                        if (StringUtils.isNotBlank(memberBonus.getStoryID())) {
                            List<MemberOrderGoodsDtoList> memberOrderGoodsDtoLists = memberOrderGoodsService.getMemberOrderGoodsDtoListByMap(new HashMap<String, Object>() {
                                {
                                    put("memberOrderID", memberOrderID);
                                    put("objectID", memberBonus.getStoryID());
                                }
                            });
                            if (memberOrderGoodsDtoLists.isEmpty()) {
                                return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "订单不存在指定故事商品无法使用");
                            }
                        }
                        if (StringUtils.isNotBlank(memberBonus.getTrainBusinessID())) {
                            List<MemberOrderGoodsDtoList> memberOrderGoodsDtoLists = memberOrderGoodsService.getMemberOrderGoodsDtoListByMap(new HashMap<String, Object>() {
                                {
                                    put("memberOrderID", memberOrderID);
                                    put("objectID", memberBonus.getTrainBusinessID());
                                }
                            });
                            if (memberOrderGoodsDtoLists.isEmpty()) {
                                return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "订单不存在指定训练营商品无法使用");
                            }
                        }


                        // 判断价格是否可用
                        if (memberBonus.getMemberOrderID() == null || memberOrderService.getMemberOrderById(memberBonus.getMemberOrderID()) == null) {
                            Double amount = memberBonus.getAmount();
                            Double offRate = memberBonus.getOffRate();
                            String bonusID = memberBonus.getBonusID();
                            Integer startPrice = 0;
                            if (StringUtils.isNotBlank(bonusID)) {
                                BonusDto bonusDto = bonusService.getByID(bonusID);
                                startPrice = bonusDto.getStartPrice();
                            }
                            if (priceTotal >= startPrice) {
                                if (amount == null || amount == 0.0) {
//                                    offRate = offRate / 100;
//                                    bonusAmount += priceTotal * offRate;
//                                    memberBonus.setOffAmount(priceTotal * offRate);
                                    offRate = BigDecimal.valueOf(offRate)
                                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
                                    bonusAmount += BigDecimal.valueOf(priceTotal)
                                            .multiply(BigDecimal.valueOf(offRate))
                                            .setScale(2, RoundingMode.HALF_UP).doubleValue();

                                    memberBonus.setOffAmount(BigDecimal.valueOf(priceTotal)
                                            .multiply(BigDecimal.valueOf(offRate))
                                            .setScale(2, RoundingMode.HALF_UP).doubleValue());
                                } else {
//                                    bonusAmount += amount;
                                    bonusAmount = BigDecimal.valueOf(bonusAmount).add(BigDecimal.valueOf(amount)).doubleValue();
                                    memberBonus.setOffAmount(amount);
                                }
                                memberBonusList.add(memberBonus);
                            } else {
                                return MessagePacket.newFail(MessageHeader.Code.moneyVeryLittle, "未满足该红包的起点金额");
                            }
                        }
                    }
                }
                // 修改使用红包的状态和属性
                Map<String, Object> map1 = new HashMap<>();
                if (!memberBonusList.isEmpty() && memberBonusList.size() > 0) {
                    for (MemberBonus memberBonus : memberBonusList) {
                        map1.clear();
                        map1.put("id", memberBonus.getId());
                        map1.put("memberOrderID", memberOrderID);
                        map1.put("orderTotal", BigDecimal.valueOf(priceTotal)
                                .subtract(BigDecimal.valueOf(bonusAmount)).intValue());
                        map1.put("offAmount", bonusAmount);
                        memberBonusService.updateByMap(map1);
                    }
                }
                priceTotal -= bonusAmount;
                // 折扣金额
                map.put("bonusAmount", bonusAmount);
            }
//            map.put("priceTotal", priceTotal);
            //map.put("priceAfterDiscount", priceTotal);
            Double priceAfterDiscount =
                    (priceTotal == null ? 0.0 : priceTotal)
                            + (memberOrder.getSendPrice() == null ? 0.0 : memberOrder.getSendPrice())
                            - (memberOrder.getSubPrePay() == null ? 0.0 : memberOrder.getSubPrePay())
                            - (pointSubAmount == null ?
                            (memberOrder.getPointSubAmount() == null ? 0.0 : memberOrder.getPointSubAmount()) :
                            (StringUtils.isNumeric(pointSubAmount.toString()) ? Double.parseDouble(pointSubAmount.toString()) : 0.0))
                            - (memberOrder.getRebateSubAmount() == null ? 0.0 : memberOrder.getRebateSubAmount())
                            - (bonusAmount == null ?
                            (memberOrder.getBonusAmount() == null ? 0.0 : memberOrder.getBonusAmount()) :
                            (StringUtils.isNumeric(bonusAmount.toString()) ? Double.parseDouble(bonusAmount.toString()) : 0.0))
                            + (memberOrder.getWeighPrice() == null ? 0.0 : memberOrder.getWeighPrice())
                            - (memberOrder.getDiscountAmount() == null ? 0.0 : memberOrder.getDiscountAmount());
            map.put("priceAfterDiscount", priceAfterDiscount);

            Double closedPriceTotal =
                    (priceAfterDiscount == null ? 0.0 : priceAfterDiscount)
                            + (memberOrder.getAdjustAmount() == null ? 0.0 : memberOrder.getAdjustAmount())
                            + (memberOrder.getTax() == null ? 0.0 : memberOrder.getTax())
                            - (memberOrder.getTaxPromotion() == null ? 0.0 : memberOrder.getTaxPromotion())
                            + (memberOrder.getServiceFee() == null ? 0.0 : memberOrder.getServiceFee());
            map.put("closedPriceTotal", closedPriceTotal);
            //map.put("closedPriceTotal", priceTotal);

            if ("1".equals(payFrom)){
                Map<String,Object> memberPaymentMap = new HashMap<>();
                memberPaymentMap.put("memberOrderID",memberOrderID);
                MemberPayment memberPayment = memberPaymentService.selectIsCreated(memberPaymentMap);

                String clientIp = request.getHeader("X-Forwarded-For");
                if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
                    clientIp = request.getHeader("Proxy-Client-IP");
                }
                if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
                    clientIp = request.getHeader("WL-Proxy-Client-IP");
                }
                if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
                    clientIp = request.getRemoteAddr();
                }
                if (clientIp != null && clientIp.contains(",")) {
                    clientIp = clientIp.split(",")[0].trim();
                }
                if (memberPayment!=null){
                    memberPayment.setApplicationID(memberOrder.getApplicationID());
                    memberPayment.setCompanyID(memberOrder.getCompanyID());
                    memberPayment.setSiteID(memberLogin.getSiteID());
                    memberPayment.setMemberID(memberLogin.getMemberID());
                    memberPayment.setDeviceID(memberLogin.getDeviceID());
                    memberPayment.setMemberOrderID(memberOrderID);
                    memberPayment.setAccountToken(memberLogin.getMemberName());
                    memberPayment.setName(memberLogin.getMemberName()+"钱包");
                    memberPayment.setOrderCode(memberOrder.getOrderCode());
                    memberPayment.setReceiveCompanyID(memberOrder.getCompanyID());
                    memberPayment.setReceiveShopID(memberOrder.getShopID());
                    memberPayment.setAmount(memberOrder.getClosedPriceTotal());
                    memberPayment.setPayFrom(1);
                    memberPayment.setPaywayID(paywayID);
                    memberPayment.setApplyTime(new Timestamp(System.currentTimeMillis()));
                    memberPayment.setPayCurrencyDefineID(memberOrder.getCurrencyDefineID());
                    memberPayment.setPaidAmount(0.0);
                    memberPayment.setIpaddr(clientIp);
                    memberPayment.setStatus(1);
                    memberPaymentService.update(memberPayment);
                    map.put("memberPaymentID", memberPayment.getId());
                }else {
                    memberPayment =  new MemberPayment();
                    String memberPaymentID = IdGenerator.uuid32();
                    memberPayment.setId(memberPaymentID);
                    memberPayment.setApplicationID(memberOrder.getApplicationID());
                    memberPayment.setCompanyID(memberOrder.getCompanyID());
                    memberPayment.setSiteID(memberLogin.getSiteID());
                    memberPayment.setMemberID(memberLogin.getMemberID());
                    memberPayment.setDeviceID(memberLogin.getDeviceID());
                    memberPayment.setMemberOrderID(memberOrderID);
                    memberPayment.setAccountToken(memberLogin.getMemberName());
                    memberPayment.setName(memberLogin.getMemberName()+"钱包");
                    memberPayment.setOrderCode(memberOrder.getOrderCode());
                    memberPayment.setReceiveCompanyID(memberOrder.getCompanyID());
                    memberPayment.setReceiveShopID(memberOrder.getShopID());
                    memberPayment.setAmount(memberOrder.getClosedPriceTotal());
                    memberPayment.setPayFrom(1);
                    memberPayment.setPaywayID(paywayID);
                    memberPayment.setApplyTime(new Timestamp(System.currentTimeMillis()));
                    memberPayment.setPayCurrencyDefineID(memberOrder.getCurrencyDefineID());
                    memberPayment.setPaidAmount(0.0);
                    memberPayment.setIpaddr(clientIp);
                    memberPayment.setStatus(1);
                    memberPaymentService.insert(memberPayment);
                    map.put("memberPaymentID", memberPaymentID);
                }
            }
            memberOrderService.update(map);
        }

        Integer goodsNumbers = 0;
        Integer goodsQTY = 0;
        List<MemberOrderGoods> list = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrderID);
        List<NewMemberOrderGoodsList> memberOrderGoodsLists = new ArrayList<>();
        if (!list.isEmpty()) {
            goodsNumbers = list.size();
            for (MemberOrderGoods memberOrderGoods : list) {
                Integer qty = memberOrderGoods.getQTY();
//                goodsQTY += qty;
                goodsQTY = BigDecimal.valueOf(goodsQTY)
                        .add(BigDecimal.valueOf(qty)).intValue();
                NewMemberOrderGoodsList dto = new NewMemberOrderGoodsList();
                dto.setMemberOrderGoodsID(memberOrderGoods.getId());
                dto.setGoodsShopID(memberOrderGoods.getGoodsShopID());
                GoodsShop goodsShop = goodsShopService.getGoodsShopByID(memberOrderGoods.getGoodsShopID());
                if (goodsShop != null) {
                    dto.setGoodsShopName(goodsShop.getName());
                }
                dto.setName(memberOrderGoods.getName());
                dto.setShowName(memberOrderGoods.getShowName());
                dto.setListImage(memberOrderGoods.getListImage());
                dto.setObjectID(memberOrderGoods.getObjectID());
                dto.setPriceStand(memberOrderGoods.getPriceStand());
                dto.setPriceNow(memberOrderGoods.getPriceNow());//23
                dto.setQTY(memberOrderGoods.getQTY());
                dto.setPriceTotal(memberOrderGoods.getPriceTotal());
                memberOrderGoodsLists.add(dto);
            }
        }

        RefreshOneMemberOrderDto refreshOneMemberOrderDto = new RefreshOneMemberOrderDto();
        refreshOneMemberOrderDto.setGoodsNumbers(goodsNumbers);

        if (memberOrder != null && memberOrder.getGoodsQTY() != null) {
            refreshOneMemberOrderDto.setGoodsQTY(memberOrder.getGoodsQTY());
        } else {
            refreshOneMemberOrderDto.setGoodsQTY(goodsQTY);
        }


        refreshOneMemberOrderDto.setPriceTotal(memberOrder.getPriceTotal());
        refreshOneMemberOrderDto.setWeighTotal(memberOrder.getWeighTotal());
        refreshOneMemberOrderDto.setSendPrice(memberOrder.getSendPrice());
        refreshOneMemberOrderDto.setPointSubAmount(memberOrder.getPointSubAmount());
        refreshOneMemberOrderDto.setBonusAmount(bonusAmount);
        refreshOneMemberOrderDto.setDiscountAmount(memberOrder.getDiscountAmount());
        refreshOneMemberOrderDto.setPriceAfterDiscount(priceTotal);
        refreshOneMemberOrderDto.setTax(memberOrder.getTax());
        refreshOneMemberOrderDto.setServiceFee(memberOrder.getServiceFee());
        refreshOneMemberOrderDto.setClosedPriceTotal(priceTotal);
        refreshOneMemberOrderDto.setMemberPaymentID(memberOrder.getMemberPaymentID());
//       refreshOneMemberOrderDto.setPayTotal(priceTotal);
        //20221022龙公子修改
        refreshOneMemberOrderDto.setPhone(member.getPhone());
        if (StringUtils.isNotBlank(memberAddressID)) {
            MemberAddress memberAddress = memberAddressService.findById(memberAddressID);
            refreshOneMemberOrderDto.setMemberAddressName(memberAddress.getName());
            refreshOneMemberOrderDto.setPhone(memberAddress.getPhone());
            refreshOneMemberOrderDto.setContactName(memberAddress.getContactName());
        } else if (StringUtils.isNotBlank(memberOrder.getMemberAddressID())) {
            MemberAddress memberAddress = memberAddressService.findById(memberOrder.getMemberAddressID());
            refreshOneMemberOrderDto.setMemberAddressName(memberAddress.getName());
            refreshOneMemberOrderDto.setPhone(memberAddress.getPhone());
            refreshOneMemberOrderDto.setContactName(memberAddress.getContactName());
        }
        refreshOneMemberOrderDto.setInvoiceID(memberOrder.getInvoiceID());
        refreshOneMemberOrderDto.setMemberBonusArray(memberBonusArray);
        refreshOneMemberOrderDto.setMemberOrderGoodsList(memberOrderGoodsLists);
        refreshOneMemberOrderDto.setOrderType(memberOrder.getOrderType());
        refreshOneMemberOrderDto.setStatus(memberOrder.getStatus());
        refreshOneMemberOrderDto.setName(memberOrder.getName());
        refreshOneMemberOrderDto.setOrderCode(memberOrder.getOrderCode());

        if (memberLogDTO != null) {
            String description = String.format("刷新一个会员订单（优惠券变化）");
            sendMessageService.sendMemberLogMessage(sessionID, description, request,
                    Memberlog.MemberOperateType.refreshOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newTreeMap();
        rsMap.put("refreshTime", TimeTest.getTimeStr());
        rsMap.put("data", refreshOneMemberOrderDto);
        if (distance!=null){
            rsMap.put("distance", distance);
        }
        return MessagePacket.newSuccess(rsMap, "refreshOneMemberOrder success");
    }

    @ApiOperation(value = "outSystemUpdateOneMemberOrderPrice", notes = "独立接口：外部系统修改会员订单的价格")
    @RequestMapping(value = "/outSystemUpdateOneMemberOrderPrice", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemUpdateOneMemberOrderPrice(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        User user = userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("modifier", user.getId());
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDNotFound, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);

        String sendPrice = request.getParameter("sendPrice");
        if (StringUtils.isNotBlank(sendPrice)) {
            if (!NumberUtils.isNumeric(sendPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendPrice请输入数字");
            }
            map.put("sendPrice", sendPrice);
        } else {
            sendPrice = "0.0";
        }

        String discountAmount = request.getParameter("discountAmount");
        if (StringUtils.isNotBlank(discountAmount)) {
            if (!NumberUtils.isNumeric(discountAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "discountAmount请输入数字");
            }
            map.put("discountAmount", discountAmount);
        } else {
            discountAmount = "0.0";
        }

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        Double priceTotal = memberOrder.getPriceTotal();
        Double bonusAmount = memberOrder.getBonusAmount();
        Double subPrePay = memberOrder.getSubPrePay();
        Double pointSubAmount = memberOrder.getPointSubAmount();
        Double tax = memberOrder.getTax();
        Double serviceFee = memberOrder.getServiceFee();

        // 计算价格priceAfterDiscount
//        Double priceAfterDiscount = priceTotal + Double.parseDouble(sendPrice) - subPrePay
//                - pointSubAmount - bonusAmount - Double.parseDouble(discountAmount);
        Double priceAfterDiscount = BigDecimal.valueOf(priceTotal)
                .add(BigDecimal.valueOf(Double.parseDouble(sendPrice)))
                .subtract(BigDecimal.valueOf(subPrePay))
                .subtract(BigDecimal.valueOf(pointSubAmount))
                .subtract(BigDecimal.valueOf(bonusAmount))
                .subtract(BigDecimal.valueOf(Double.parseDouble(discountAmount))).doubleValue();
        map.put("priceAfterDiscount", priceAfterDiscount);
        // 计算价格closedPriceTotal
//        Double closedPriceTotal = priceAfterDiscount + tax + serviceFee;
        Double closedPriceTotal = BigDecimal.valueOf(priceAfterDiscount)
                .add(BigDecimal.valueOf(tax))
                .add(BigDecimal.valueOf(serviceFee)).doubleValue();
        map.put("closedPriceTotal", closedPriceTotal);

        memberOrderService.update(map);

        if (opLog != null) {
            String description = String.format("外部系统修改会员订单的价格");
            sendMessageService.sendDataLogMessage(user, description, "outSystemUpdateOneMemberOrderPrice",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrderID,
                    memberOrder.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("priceAfterDiscount", priceAfterDiscount);
        rsMap.put("closedPriceTotal", closedPriceTotal);
        return MessagePacket.newSuccess(rsMap, "outSystemUpdateOneMemberOrderPrice success");
    }

    @ApiOperation(value = "hsjccxjk", notes = "独立接口：核酸检测查询")
    @RequestMapping(value = "/hsjccxjk", produces = {"application/json;charset=UTF-8"})
    public MessagePacket hsjccxjk(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        String appKey = "98625141753466e62";
        String DSC_APP_CODE = "vbr4R6SDQT6NriGDJR0kq9ysCdYeom56";
        String siteCode = "TQ34CQk8";
        String serviceCode = "CWY8DPeJ1CY6lTUt";

        String name = request.getParameter("name");
        if (StringUtils.isBlank(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }

        String sfzh = request.getParameter("sfzh");
        if (StringUtils.isBlank(sfzh)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "sfzh不能为空");
        }

        try {
            long time = TimeTest.getTime().getTime();
            HttpClient httpclient = new DefaultHttpClient();
            String url = "http://10.246.5.90:8080/prod-api/hsjccxjk/2?serviceCode=" + serviceCode
                    + "&appKey=" + appKey
                    + "&timestamp=" + time
                    + "&name=" + name
                    + "&sfzh=" + sfzh
                    + "&signature=" + DigestUtils.sha1Hex(appKey + siteCode + time);
            HttpPost httppost = new HttpPost(url);
            //添加http头信息
            httppost.addHeader("Content-Type", "application/json");
            httppost.addHeader("DSC_APP_CODE", DSC_APP_CODE);
            net.sf.json.JSONObject obj = new net.sf.json.JSONObject();
            HttpResponse response = httpclient.execute(httppost);
            //检验状态码，如果成功接收数据
            int code = response.getStatusLine().getStatusCode();
            if (code == 200) {
                //返回json格式
                String rev = EntityUtils.toString(response.getEntity());
                obj = net.sf.json.JSONObject.fromObject(rev);
                // 将返回的json格式转换成map
                map = (Map) obj;
            }
        } catch (ClientProtocolException e) {
            log.error("log",e);
        } catch (IOException e) {
            log.error("log",e);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", map);
        return MessagePacket.newSuccess(rsMap, "hsjccxjk success");
    }

    @ApiOperation(value = "thirdDataGetOneHesuanKangyuan", notes = "独立接口：核酸抗原检测查询")
    @RequestMapping(value = "/thirdDataGetOneHesuanKangyuan", produces = {"application/json;charset=UTF-8"})
    public MessagePacket thirdDataGetOneHesuanKangyuan(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        String appKey = "98625141753466e62";
        String DSC_APP_CODE = "vbr4R6SDQT6NriGDJR0kq9ysCdYeom56";
        String siteCode = "TQ34CQk8";
        String serviceCode = "CWY8DPeJ1CY6lTUt";
//        String serviceCode = "ODYGQkAfd6bhRJwz";

        String name = request.getParameter("name");

        String idCard = request.getParameter("idCard");

        String kingID = request.getParameter("kingID");
        if (StringUtils.isNotBlank(kingID)) {
            People people = peopleService.getByKingID(kingID);
            if (people == null) {
                return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "kingID不正确");
            }
            name = people.getName();
            idCard = people.getIdNumber();
        }

        if (StringUtils.isBlank(kingID) && StringUtils.isBlank(name) && StringUtils.isBlank(idCard)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "kingID为空时name和idCard不能为空");
        }
        net.sf.json.JSONObject obj = new net.sf.json.JSONObject();
        StringBuilder stringBuilder = new StringBuilder();
        try {
            long time = TimeTest.getTime().getTime();
            HttpClient httpclient = new DefaultHttpClient();
            String url = "http://10.246.5.90:8080/prod-api/hsjccxjk/2?serviceCode=" + serviceCode
                    + "&appKey=" + appKey
                    + "&timestamp=" + time
                    + "&name=" + name
                    + "&sfzh=" + idCard
                    + "&signature=" + DigestUtils.sha1Hex(appKey + siteCode + time);
            log.info("url=======" + url);
            HttpPost httppost = new HttpPost(url);
            //添加http头信息
            httppost.addHeader("Content-Type", "application/json");
            httppost.addHeader("DSC_APP_CODE", DSC_APP_CODE);
            HttpResponse response = httpclient.execute(httppost);
            //检验状态码，如果成功接收数据
            int code = response.getStatusLine().getStatusCode();
            if (code == 200) {
                //返回json格式
                stringBuilder.append(EntityUtils.toString(response.getEntity()));
                obj = net.sf.json.JSONObject.fromObject(stringBuilder.toString().replaceAll("null", "\"null\""));
                // 将返回的json格式转换成map
                map = (Map) obj;
            }
        } catch (ClientProtocolException e) {
            log.error("log",e);
        } catch (IOException e) {
            log.error("log",e);
        }
//        try {
//            long time = TimeTest.getTime().getTime();
//            HttpClient httpclient = new DefaultHttpClient();
//            String url = "http://10.246.5.90:8080/prod-api/hskyjcjgcx/1?serviceCode=" + serviceCode
//                    + "&appKey=" + appKey
//                    + "&timestamp=" + time
//                    + "&username=" + name
//                    + "&id_card=" + idCard
//                    + "&signature=" + DigestUtils.sha1Hex(appKey + siteCode + time);
//            HttpPost httppost = new HttpPost(url);
//            //添加http头信息
//            httppost.addHeader("Content-Type", "application/json");
//            httppost.addHeader("DSC_APP_CODE", DSC_APP_CODE);
//            net.sf.json.JSONObject obj = new net.sf.json.JSONObject();
//            HttpResponse response = httpclient.execute(httppost);
//            //检验状态码，如果成功接收数据
//            int code = response.getStatusLine().getStatusCode();
//            if (code == 200) {
//                //返回json格式
//                String rev = EntityUtils.toString(response.getEntity());
//                obj = net.sf.json.JSONObject.fromObject(rev);
//                // 将返回的json格式转换成map
//                map = (Map) obj;
//            }
//        } catch (ClientProtocolException e) {
//            log.error("log",e);
//        } catch (IOException e) {
//            log.error("log",e);
//        } catch (Exception e) {
//            log.error("log",e);
//        }
        if (map.isEmpty()) {
            map.put("id_card", "test-id_card-000000");
            map.put("username", "test-name");
            map.put("pick_date", TimeTest.getTimeStr());
            map.put("result", "test-name");
        }
        return MessagePacket.newSuccess(map, "thirdDataGetOneHesuanKangyuan success");
    }

    @ApiOperation(value = "offPaymentOneMemberOrder", notes = "独立接口:线下付款给1个会员订单")
    @RequestMapping(value = "/offPaymentOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket offPaymentOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);

        String paySequence = request.getParameter("paySequence");
        if (StringUtils.isNotBlank(paySequence)) {
            map.put("paySequence", paySequence);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.paySequenceNotNull, "paySequence不能为空");
        }

        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDIsError, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.paywayIDNotNull, "paywayID不能为空");
        }
        String currentTime = TimeTest.getTimeStr();
        String currentMemberID = member.getId();
        map.put("payTime", currentTime);
        map.put("payMemberID", currentMemberID);
        map.put("payFrom", 21);
        map.put("payStatus", 0);
        map.put("status", 40);
        map.put("modifier", currentMemberID);
        memberOrderService.update(map);

        //上传附件
        String attachmentList = request.getParameter("attachmentList");
        if (StringUtils.isNotBlank(attachmentList)) {
            String[] attachments = attachmentList.split(",");
            if (attachments != null && attachments.length > 0) {
                for (String attachment : attachments) {
                    Attachment attachmentNew = new Attachment();
                    attachmentNew.setId(IdGenerator.uuid32());
                    attachmentNew.setOrderSeq(attachmentService.getMaxOrderSeq(memberOrderID));
                    attachmentNew.setUrl(attachment);
                    attachmentNew.setFileType(1);
                    attachmentNew.setShowType(100);
                    attachmentNew.setMemberID(member == null ? null : member.getId());
                    attachmentNew.setName(member == null ? "附件图" : member.getName() + "上传附件图");
                    attachmentNew.setObjectID(memberOrderID);
                    attachmentNew.setObjectName(memberOrder.getName());
                    attachmentNew.setObjectDefineID("8a2f462a5a6026cd015a64fe3e920cef");//会员订单
                    attachmentNew.setCreator(member.getId());
                    attachmentService.insert(attachmentNew,sessionID);
                }
            }
        }
        if (memberLogDTO != null) {
            String descriptions = String.format("线下付款给1个会员订单");
            sendMessageService.sendMemberLogMessage(sessionID, descriptions, request, Memberlog.MemberOperateType.offPaymentOneMemberOrder);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("modifiedTime", currentTime);
        rsMap.put("status", "正在付款中...");
        return MessagePacket.newSuccess(rsMap, "offPaymentOneMemberOrder success");
    }

    @ApiOperation(value = "outSystemDoneOneMemberOrder", notes = "独立接口：外部系统完成一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemDoneOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemDoneOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        User user = userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }

        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDNotFound, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);
        map.put("modifier", user.getId());
        map.put("status", 9);
        memberOrderService.outSystemUpdateMemberOrder(map);
        if (user != null) {
            String descriptions = String.format("外部系统完成一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemDoneOneMemberOrder",
                    DataLog.objectDefineID.memberOrder.getOname(), memberOrder.getId(),
                    memberOrder.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("modifiedTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemDoneOneMemberOrder success");
    }

    @ApiOperation(value = "outSystemGetMemberOrderAndGoodsList", notes = "独立接口:外部系统获取会员订单和会员订单商品列表")
    @RequestMapping(value = "/outSystemGetMemberOrderAndGoodsList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberOrderAndGoodsList(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        }

        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            if (!cityService.checkIdIsValid(cityID)) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }

        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String actionID = request.getParameter("actionID");
        if (StringUtils.isNotBlank(actionID)) {
            if (!actionService.checkIdIsValid(actionID)) {
                return MessagePacket.newFail(MessageHeader.Code.actionIDNotFound, "actionID不正确");
            }
            map.put("actionID", actionID);
        }
        String sceneID = request.getParameter("sceneID");
        if (StringUtils.isNotBlank(sceneID)) {
            if (!sceneService.checkIsValidId(sceneID)) {
                return MessagePacket.newFail(MessageHeader.Code.sceneIDNotFound, "sceneID不正确");
            }
            map.put("sceneID", sceneID);
        }
        String trainBusinessID = request.getParameter("trainBusinessID");
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!trainBusinessService.checkIdIsValid(trainBusinessID)) {
                return MessagePacket.newFail(MessageHeader.Code.trainBusinessIDNotFound, "trainBusinessID不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        String trainCourseID = request.getParameter("trainCourseID");
        if (StringUtils.isNotBlank(trainCourseID)) {
            if (!trainCourseService.checkIDIsValid(trainCourseID)) {
                return MessagePacket.newFail(MessageHeader.Code.trainCourseIDNotFound, "trainCourseID不正确");
            }
            map.put("trainCourseID", trainCourseID);
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "recommendMemberID不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            Member member1 = memberService.selectByRecommendCode(recommendCode);
            if (member1 != null) {
                map.put("recommendMemberID", member1.getId());
            }
        }
        String salesMemberID = request.getParameter("salesMemberID");
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!memberService.checkMemberId(salesMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "salesMemberID不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        String salesEmployeeID = request.getParameter("salesEmployeeID");
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!employeeService.checkEmployeeID(salesEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null && channel.getId() == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
        }
        String orderTemplateID = request.getParameter("orderTemplateID");
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!orderTemplateGoodsService.checkIsValidId(orderTemplateID)) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIsNull, "orderTemplateID不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        String buyCompanyID = request.getParameter("buyCompanyID");
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(buyCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "buyCompanyID不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }
        String buyShopID = request.getParameter("buyShopID");
        if (StringUtils.isNotBlank(buyShopID)) {
            if (!shopService.checkIdIsValid(buyShopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "buyShopID不正确");
            }
            map.put("buyShopID", buyShopID);
        }
        String buyEmployeeID = request.getParameter("buyEmployeeID");
        if (StringUtils.isNotBlank(buyEmployeeID)) {
            if (!employeeService.checkEmployeeID(buyEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "buyEmployeeID不正确");
            }
            map.put("buyEmployeeID", buyEmployeeID);
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
            }
            map.put("memberID", memberID);
        }
        String useEmployeeID = request.getParameter("useEmployeeID");
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!employeeService.checkEmployeeID(useEmployeeID)) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "useEmployeeID不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        String useMemberID = request.getParameter("useMemberID");
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!memberService.checkMemberId(useMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "useMemberID不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        String orderType = request.getParameter("orderType");
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "orderType请输入数字");
            }
            map.put("orderType", Integer.valueOf(orderType));
        }
        String financeType = request.getParameter("financeType");
        if (StringUtils.isNotBlank(financeType)) {
            if (!NumberUtils.isNumeric(financeType)) {
                return MessagePacket.newFail(MessageHeader.Code.financeTypeIsError, "financeType请填写数字");
            }
            map.put("financeType", Integer.valueOf(financeType));
        }
        String payFrom = request.getParameter("payFrom");
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.payFromNotNull, "payFrom请输入数字");
            }
            map.put("payFrom", Integer.valueOf(payFrom));
        }
        String paywayID = request.getParameter("paywayID");
        if (StringUtils.isNotBlank(paywayID)) {
            if (!paywayService.checkIsValidId(paywayID)) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDIsError, "paywayID不正确");
            }
            map.put("paywayID", paywayID);
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isNotBlank(sendType)) {
            if (!NumberUtils.isNumeric(sendType)) {
                return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendType请输入数字");
            }
            map.put("sendType", Integer.valueOf(sendType));
        }
        String sendUrgencyType = request.getParameter("sendUrgencyType");
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.sendTypeNotNull, "sendUrgencyType请输入数字");
            }
            map.put("sendUrgencyType", Integer.valueOf(sendUrgencyType));
        }
        String memberAddressID = request.getParameter("memberAddressID");
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!memberAddressService.checkIsValidId(memberAddressID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDIsError, "memberAddressID不正确");
            }
            map.put("memberAddressID", memberAddressID);
        }

        String paySequence = request.getParameter("paySequence");
        if (StringUtils.isNotBlank(paySequence)) {
            map.put("paySequence", "%" + paySequence + "%");
        }

        String status = request.getParameter("status");
        if (StringUtils.isNotBlank(status)) {
            if (!NumberUtils.isNumeric(status)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "status请填写数字");
            }
            map.put("status", Integer.valueOf(status));
        }
        String isLock = request.getParameter("isLock");
        if (StringUtils.isNotBlank(isLock)) {
            if (!NumberUtils.isNumeric(isLock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isLock请填写数字");
            }
            map.put("isLock", Integer.valueOf(isLock));
        }
        String isValid = request.getParameter("isValid");
        if (StringUtils.isNotBlank(isValid)) {
            if (!NumberUtils.isNumeric(isValid)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isValid请填写数字");
            }
            map.put("isValid", Integer.valueOf(isValid));
        }
        String actionGoodsType = request.getParameter("actionGoodsType");
        if (StringUtils.isNotBlank(actionGoodsType)) {
            if (!NumberUtils.isNumeric(actionGoodsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "actionGoodsType请填写数字");
            }
            map.put("actionGoodsType", Integer.valueOf(actionGoodsType));
        }

        String outPayType = request.getParameter("outPayType");
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "outPayType请填写数字");
            }
            map.put("outPayType", Integer.valueOf(outPayType));
        }
        String buyType = request.getParameter("buyType");
        if (StringUtils.isNotBlank(buyType)) {
            if (!NumberUtils.isNumeric(buyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "buyType请填写数字");
            }
            map.put("buyType", Integer.valueOf(buyType));
        }
        String payStatus = request.getParameter("payStatus");
        if (StringUtils.isNotBlank(payStatus)) {
            if (!NumberUtils.isNumeric(payStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payStatus请填写数字");
            }
            map.put("payStatus", Integer.valueOf(payStatus));
        }
        String sendStatus = request.getParameter("sendStatus");
        if (StringUtils.isNotBlank(sendStatus)) {
            if (!NumberUtils.isNumeric(sendStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendStatus请填写数字");
            }
            map.put("sendStatus", Integer.valueOf(sendStatus));
        }
        String isMemberHide = request.getParameter("isMemberHide");
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide请填写数字");
            }
            map.put("isMemberHide", Integer.valueOf(isMemberHide));
        }
        String beginPrice = request.getParameter("beginPrice");
        if (StringUtils.isNotBlank(beginPrice)) {
            if (!NumberUtils.isNumeric(beginPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "beginPrice请填写数字");
            }
            map.put("beginPrice", Double.valueOf(beginPrice));
        }
        String endPrice = request.getParameter("endPrice");
        if (StringUtils.isNotBlank(endPrice)) {
            if (!NumberUtils.isNumeric(endPrice)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "endPrice请填写数字");
            }
            map.put("endPrice", Double.valueOf(endPrice));
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);

        String beginPayTime = request.getParameter("beginPayTime");
        if (StringUtils.isNotBlank(beginPayTime)) map.put("beginPayTime", beginPayTime);
        String endPayTime = request.getParameter("endPayTime");
        if (StringUtils.isNotBlank(endPayTime)) map.put("endPayTime", endPayTime);
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) map.put("sortTypeName", sortTypeName);
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) map.put("sortTypeTime", sortTypeTime);
        String sortTypePrice = request.getParameter("sortTypePrice");
        if (StringUtils.isNotBlank(sortTypePrice)) map.put("sortTypePrice", sortTypePrice);
        String sortTypeStatus = request.getParameter("sortTypeStatus");
        if (StringUtils.isNotBlank(sortTypeStatus)) map.put("sortTypeStatus", sortTypeStatus);
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            map.put("keyWords", "%" + keyWords + "%");
        }
        String memberOrderGoodsName = request.getParameter("memberOrderGoodsName");//模糊查询memberOrderGoods表的Name,showName
        if (StringUtils.isNotBlank(memberOrderGoodsName)) {
            map.put("memberOrderGoodsName", "%" + memberOrderGoodsName + "%");
        }
        Page page = HttpServletHelper.assembyPage(request);        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<OutSystemMemberOrderAndGoodsList> list = memberOrderService.outSystemGetMemberOrderAndGoodsList(map);
        if (list != null && !list.isEmpty()) {
            PageInfo<OutSystemMemberOrderAndGoodsList> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
            for (OutSystemMemberOrderAndGoodsList obj : list) {
                if (StringUtils.isNotBlank(obj.getMemberOrderID())) {
                    map.clear();
                    map.put("memberOrderID", obj.getMemberOrderID());
                    map.put("bodyType", 1);
                    obj.setHasRealGoods(goodsShopService.getHasGoodsByMap(map));
                    map.put("bodyType", 2);
                    obj.setHasVirtualGoods(goodsShopService.getHasGoodsByMap(map));
                }
            }
        }
        if (user != null) {
            String descriptions = String.format("外部系统获取会员订单和会员订单商品列表");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberOrderAndGoodsList",
                    DataLog.objectDefineID.memberOrder.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberOrderAndGoodsList success");
    }

    @ApiOperation(value = "getWeiXinPayStatus", notes = "独立接口：查询微信支付状态")
    @RequestMapping(value = "/getWeiXinPayStatus", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getWeiXinPayStatus(HttpServletRequest request, ModelMap map) {
//        String sessionID = request.getParameter("sessionID");
//        if (StringUtils.isEmpty(sessionID)) {
//            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//        }
//        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
////        Member member = memberService.selectById(sessionID);
//        if (member == null) {
//            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//        }
//        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
//        if (memberLogDTO == null) {
//            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//        }

        String mchid = request.getParameter("mchid");
        if (StringUtils.isBlank(mchid)) {
            return MessagePacket.newFail(MessageHeader.Code.mchidNotNull, "mchid不能为空");
        }

        String out_trade_no = request.getParameter("out_trade_no");
        if (StringUtils.isBlank(out_trade_no)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "out_trade_no不能为空");
        }

        // 调用微信接口
        Map<String, Object> returnMap = new HashMap<>();
        String errorMsg = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            String uri = "https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/"
                    + out_trade_no + "?mchid=" + mchid;
            HttpGet httpGet = new HttpGet(uri);
            //添加http头信息
            httpGet.addHeader("Accept", "application/json;charset=UTF-8");
            net.sf.json.JSONObject obj = new net.sf.json.JSONObject();
            HttpResponse response;
            response = httpclient.execute(httpGet);
            //检验状态码，如果成功接收数据
            int code = response.getStatusLine().getStatusCode();
            if (code == 200) {
                //返回json格式
                String rev = EntityUtils.toString(response.getEntity());
                obj = net.sf.json.JSONObject.fromObject(rev);
                // 将返回的json格式转换成map
                returnMap = (Map) obj;
                errorMsg = obj.toString();
            }
        } catch (ClientProtocolException e) {
            log.error("log",e);
        } catch (IOException e) {
            log.error("log",e);
        } catch (Exception e) {
            log.error("log",e);
        }
        // 检查返回值中的数据，查看请求是否成功
        String errmsg = (String) returnMap.get("errmsg");
        if (StringUtils.isNotBlank(errmsg)) {
            // 请求失败，打印失败的信息
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "接口调用失败：" + errorMsg);
        }

//        if (memberLogDTO != null) {
//            String description = String.format("查询微信支付状态");
//            sendMessageService.sendMemberLogMessage(sessionID, description, request,
//                    Memberlog.MemberOperateType.getWeiXinPayStatus);
//        }
        return MessagePacket.newSuccess(returnMap, "getWeiXinPayStatus success");
    }

    @ApiOperation(value = "getZFBPayStatus", notes = "独立接口：查询支付宝支付状态")
    @RequestMapping(value = "/getZFBPayStatus", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getZFBPayStatus(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String out_trade_no = request.getParameter("out_trade_no");

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isNotBlank(memberOrderID)) {
            MemberOrderDto memberOrder = memberOrderService.getMemberOrderById(memberOrderID);
            out_trade_no = memberOrder.getMemberPaymentID();
            if (StringUtils.isBlank(out_trade_no)) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "没有支付信息");
            }
        }

        if (StringUtils.isBlank(out_trade_no)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "out_trade_no不能为空");
        }

        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isBlank(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.paywayIDNotNull, "payWayID不能为空");
        }
        if (!paywayService.checkIsValidId(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
        }
        PayWay payWay = paywayService.findById(payWayID);

        // 调用支付宝接口
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", payWay.getServerPassword(),
                payWay.getPrivateKey(), "json", "GBK", payWay.getCommonKey(), "RSA2");
        AlipayTradeQueryRequest alipayTradeQueryRequest = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", out_trade_no);
        alipayTradeQueryRequest.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(alipayTradeQueryRequest);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (!response.isSuccess()) {
            log.info("response", response);
            return MessagePacket.newFail(MessageHeader.Code.fail, "调用失败");
        }

        Double amount = Double.parseDouble(response.getTotalAmount());
        String trade_no = response.getTradeNo();

        MemberPayment memberPayment = memberPaymentService.findById(out_trade_no);
        if (memberPayment != null) {
            if (memberPayment.getStatus() != null && memberPayment.getStatus() != 10) {
                sendMessageService.sendPayReturn(amount, trade_no,
                        memberPayment.getId(), 1);
            }
        }

        if (memberLogDTO != null) {
            String description = String.format("查询支付宝支付状态");
            sendMessageService.sendMemberLogMessage(sessionID, description, request,
                    Memberlog.MemberOperateType.getZFBPayStatus);
        }
        return MessagePacket.newSuccess(response.getBody(), "getZFBPayStatus success");
    }

    @ApiOperation(value = "getWeChartPayStatus", notes = "独立接口：查询微信支付状态")
    @RequestMapping(value = "/getWeChartPayStatus", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getWeChartPayStatus(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String out_trade_no = request.getParameter("out_trade_no");

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isNotBlank(memberOrderID)) {
            MemberOrderDto memberOrder = memberOrderService.getMemberOrderById(memberOrderID);
            out_trade_no = memberOrder.getMemberPaymentID();
            if (StringUtils.isBlank(out_trade_no)) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "没有支付信息");
            }
        }

        if (StringUtils.isBlank(out_trade_no)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "out_trade_no不能为空");
        }

        String payWayID = request.getParameter("payWayID");
        if (StringUtils.isBlank(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.paywayIDNotNull, "payWayID不能为空");
        }
        if (!paywayService.checkIsValidId(payWayID)) {
            return MessagePacket.newFail(MessageHeader.Code.payWayIDNotFound, "payWayID不正确");
        }
        PayWay payWay = paywayService.findById(payWayID);

        // 调用微信接口
        Transaction result = null;
        com.wechat.pay.java.core.Config config =
                new RSAAutoCertificateConfig.Builder()
                        .merchantId(payWay.getPartnerID())
                        .privateKeyFromPath("/opt/app/interface-server/apiclient_key.pem")
                        .merchantSerialNumber("3037D9FA91BB58E122C43F49C908ED0A1D842DA3")
                        .apiV3Key(payWay.getPrivateKey())
                        .build();
        JsapiService service = new JsapiService.Builder().config(config).build();
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(payWay.getPartnerID());
        queryRequest.setOutTradeNo(out_trade_no);
        try {
            result = service.queryOrderByOutTradeNo(queryRequest);
            log.info("getWeChartPayStatus(payerTotal)[" + result.getAmount().getPayerTotal() + "]");
            log.info("getWeChartPayStatus(transaction_id)[" + result.getTransactionId() + "]");
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
//            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
//            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
        }

        MemberPayment memberPayment = memberPaymentService.findById(out_trade_no);
        if (memberPayment != null) {
            if (memberPayment.getStatus() != null && memberPayment.getStatus() != 10) {
                Double amount = NumberUtils.keepPrecision(NumberUtils.div(result.getAmount().getPayerTotal().doubleValue(), 100d), 2);
                sendMessageService.sendPayReturn(amount, result.getTransactionId(),
                        memberPayment.getId(), 1);
            }
        }

        if (memberLogDTO != null) {
            String description = String.format("查询微信支付状态");
            sendMessageService.sendMemberLogMessage(sessionID, description, request,
                    Memberlog.MemberOperateType.getWeChartPayStatus);
        }
        return MessagePacket.newSuccess(result, "getWeChartPayStatus success");
    }



    private static final java.util.concurrent.ConcurrentHashMap<String, com.wechat.pay.java.core.Config> WECHAT_CONFIG_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @ApiOperation(value = "autoRefundOneMemberOrderForWX", notes = "微信支付自动退款订单退款")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
            @ApiImplicitParam(paramType = "query", name = "memberOrderID", value = "会员订单ID", dataType = "String")
    })
    @RequestMapping(value = "/autoRefundOneMemberOrderForWX", produces = {"application/json;charset=UTF-8"})
    public MessagePacket autoRefundOneMemberOrderForWX(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member cacheMember = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (cacheMember == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = memberService.selectById(cacheMember.getId());
        if (member == null || member.getIsValid() == 0) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "该会员已被删除，无法操作");
        }
        if (member.getIsLock() == 1) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "该会员已被锁定，无法操作");
        }
        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isEmpty(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
        if (memberOrder == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsError, "未查询到该订单");
        }
        if (StringUtils.isBlank(memberOrder.getMemberID()) || !memberOrder.getMemberID().equals(member.getId())) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "该订单不属于当前登录用户");
        }
        if (memberOrder.getStatus() != 4) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "订单状态不符合退款条件 (非待发货状态)");
        }
        if (memberOrder.getPayStatus() == null || memberOrder.getPayStatus() != 2) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "订单支付状态不符合退款条件 (非已付款)");
        }
        if (memberOrder.getPayFrom() == null || memberOrder.getPayFrom() != 2) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "非第三方支付订单，无法自动退款");
        }
        if (memberOrder.getOutPayType() == null || memberOrder.getOutPayType() != 1) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "非微信支付订单，无法调用微信退款");
        }
        if (memberOrder.getSendStatus() == null || memberOrder.getSendStatus() != 0) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "订单已发货，无法直接退款");
        }
        String memberPaymentID = memberOrder.getMemberPaymentID();
        if (StringUtils.isBlank(memberPaymentID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "订单缺少支付记录信息，无法退款");
        }
        MemberPayment memberPayment = memberPaymentService.findById(memberPaymentID);
        if (memberPayment == null) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "未查询到对应的支付记录");
        }
        if (memberPayment.getStatus() == null || memberPayment.getStatus() != 10) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "支付记录状态非付款完成，无法自动退款");
        }

        map.clear();
        map.put("id", memberOrderID);
        map.put("returnType", 1);
        map.put("status", 14);
        map.put("payStatus", 10);
        memberOrderService.update(map);

        try {
            String payWayID = memberOrder.getPaywayID();
            if (StringUtils.isBlank(payWayID)) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "订单缺少支付方式信息，无法调用退款");
            }
            PayWay payWay = paywayService.findById(payWayID);
            if (payWay == null) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "未找到对应的支付配置信息");
            }
            String outTradeNo = memberOrder.getMemberPaymentID();
            if (StringUtils.isBlank(outTradeNo)) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "订单缺少微信支付流水号，无法退款");
            }
            String outRefundNo = "REFUND_" + memberOrderID;
            String merchantId = payWay.getPartnerID();
            com.wechat.pay.java.core.Config config = WECHAT_CONFIG_CACHE.get(merchantId);

            if (config == null) {
                synchronized (this) {
                    config = WECHAT_CONFIG_CACHE.get(merchantId);
                    if (config == null) {
                        config = new com.wechat.pay.java.core.RSAAutoCertificateConfig.Builder()
                                .merchantId(merchantId)
                                .privateKeyFromPath("/opt/app/interface-server/apiclient_key.pem")
                                .merchantSerialNumber("1c6f0cc6431827c1bc7b05195834bef9fb7fe5de")
                                .apiV3Key("XuqingboHulianzhixiang2026041300")
                                .build();
                        WECHAT_CONFIG_CACHE.put(merchantId, config);
                    }
                }
            }

            com.wechat.pay.java.service.refund.RefundService refundService =
                    new com.wechat.pay.java.service.refund.RefundService.Builder().config(config).build();
            com.wechat.pay.java.service.refund.model.CreateRequest refundReq =
                    new com.wechat.pay.java.service.refund.model.CreateRequest();
            refundReq.setOutTradeNo(outTradeNo);
            refundReq.setOutRefundNo(outRefundNo);
            refundReq.setReason("用户申请自动退款");
            refundReq.setNotifyUrl("https://api.hulianweiye.cn/interface-server/api/refundOneMemberOrderForWXCallBack");
            long totalFeeInCents = Math.round(memberOrder.getPayTotal() * 100);
            com.wechat.pay.java.service.refund.model.AmountReq amountReq =
                    new com.wechat.pay.java.service.refund.model.AmountReq();
            amountReq.setRefund(totalFeeInCents);
            amountReq.setTotal(totalFeeInCents);
            amountReq.setCurrency("CNY");

            refundReq.setAmount(amountReq);

//            try {
//                Map<String, Object> browseMapBefore = new HashMap<>();
//                browseMapBefore.put("id", IdGenerator.uuid32());
//                browseMapBefore.put("applicationID", memberOrder.getApplicationID());
//                browseMapBefore.put("companyID", memberOrder.getCompanyID());
//                browseMapBefore.put("siteID", memberOrder.getSiteID());
//                browseMapBefore.put("browseTime", TimeTest.getTimeStr());
//                browseMapBefore.put("memberID", memberOrder.getMemberID());
//                browseMapBefore.put("name", "开始发起微信V3自动退款，退款单号=" + outRefundNo + "，金额(分)=" + totalFeeInCents);
//                browseService.insertByMap(browseMapBefore);
//            } catch (Exception e) {
//                log.error("插入微信退款前 browse 埋点失败", e);
//            }

            log.info("开始调用微信 V3 退款, 订单号:{}, 退款单号:{}, 金额(分):{}", memberOrderID, outRefundNo, totalFeeInCents);
            com.wechat.pay.java.service.refund.model.Refund refundResult = refundService.create(refundReq);
            log.info("微信 V3 退款返回结果: {}", refundResult);

//            try {
//                Map<String, Object> browseMapAfter = new HashMap<>();
//                browseMapAfter.put("id", IdGenerator.uuid32());
//                browseMapAfter.put("applicationID", memberOrder.getApplicationID());
//                browseMapAfter.put("companyID", memberOrder.getCompanyID());
//                browseMapAfter.put("siteID", memberOrder.getSiteID());
//                browseMapAfter.put("browseTime", TimeTest.getTimeStr());
//                browseMapAfter.put("memberID", memberOrder.getMemberID());
//                browseMapAfter.put("name", "微信V3自动退款调用成功，返回状态=" + refundResult.getStatus().name());
//                browseService.insertByMap(browseMapAfter);
//            } catch (Exception e) {
//                log.error("插入微信退款后 browse 埋点失败", e);
//            }

            try {
                memberPayment.setStatus(20);
                memberPayment.setReturnType(1);
                memberPayment.setReturnBackType(1);

                if (refundResult.getAmount() != null && refundResult.getAmount().getRefund() != null) {
                    double actualRefundAmount = refundResult.getAmount().getRefund() / 100.0;
                    memberPayment.setReturnAmount(actualRefundAmount);
                }

                memberPaymentService.update(memberPayment);
            } catch (Exception e) {
                log.error("更新 memberpayment 状态及退款信息失败, 订单ID: {}", memberOrderID, e);
            }

            Map<String, Object> rsMap = Maps.newHashMap();
            rsMap.put("returnTime", TimeTest.getTimeStr());
            rsMap.put("returnAmount", memberOrder.getPayTotal());
            rsMap.put("outRefundNo", outRefundNo);
            rsMap.put("refundStatus", refundResult.getStatus().name());
            return MessagePacket.newSuccess(rsMap, "微信自动退款申请提交成功");

        } catch (com.wechat.pay.java.core.exception.ServiceException e) {
            String errorMsg = "微信退款业务被拒绝: " + e.getErrorMessage();
            log.error("{}, 订单ID: {}, 错误码: {}", errorMsg, memberOrderID, e.getErrorCode(), e);

            rollbackOrderStateOnFail(memberOrderID);

//            try {
//                Map<String, Object> browseMapErr = new HashMap<>();
//                browseMapErr.put("id", IdGenerator.uuid32());
//                browseMapErr.put("applicationID", memberOrder.getApplicationID());
//                browseMapErr.put("companyID", memberOrder.getCompanyID());
//                browseMapErr.put("siteID", memberOrder.getSiteID());
//                browseMapErr.put("browseTime", TimeTest.getTimeStr());
//                browseMapErr.put("memberID", memberOrder.getMemberID());
//                browseMapErr.put("name", "V3退款异常(业务被拒)，错误码=" + e.getErrorCode() + "，原因=" + e.getErrorMessage());
//                browseService.insertByMap(browseMapErr);
//            } catch (Exception ex) {
//                log.error("插入退款业务拒绝 browse 埋点失败", ex);
//            }
            return MessagePacket.newFail(MessageHeader.Code.fail, "微信退款失败: " + e.getErrorMessage());

        } catch (com.wechat.pay.java.core.exception.HttpException e) {
            log.error("微信退款网络异常, 订单ID: {}", memberOrderID, e);

            rollbackOrderStateOnFail(memberOrderID);

//            try {
//                Map<String, Object> browseMapErr = new HashMap<>();
//                browseMapErr.put("id", IdGenerator.uuid32());
//                browseMapErr.put("applicationID", memberOrder.getApplicationID());
//                browseMapErr.put("companyID", memberOrder.getCompanyID());
//                browseMapErr.put("siteID", memberOrder.getSiteID());
//                browseMapErr.put("browseTime", TimeTest.getTimeStr());
//                browseMapErr.put("memberID", memberOrder.getMemberID());
//                browseMapErr.put("name", "V3退款异常(网络超时/断开)");
//                browseService.insertByMap(browseMapErr);
//            } catch (Exception ex) {
//                log.error("插入退款网络异常 browse 埋点失败", ex);
//            }
            return MessagePacket.newFail(MessageHeader.Code.fail, "微信退款网络异常，请稍后重试");

        } catch (Exception e) {
            log.error("微信退款系统内部异常, 订单ID: {}", memberOrderID, e);

            rollbackOrderStateOnFail(memberOrderID);

//            try {
//                Map<String, Object> browseMapErr = new HashMap<>();
//                browseMapErr.put("id", IdGenerator.uuid32());
//                browseMapErr.put("applicationID", memberOrder.getApplicationID());
//                browseMapErr.put("companyID", memberOrder.getCompanyID());
//                browseMapErr.put("siteID", memberOrder.getSiteID());
//                browseMapErr.put("browseTime", TimeTest.getTimeStr());
//                browseMapErr.put("memberID", memberOrder.getMemberID());
//                browseMapErr.put("name", "V3退款内部异常，报错信息=" + e.getMessage());
//                browseService.insertByMap(browseMapErr);
//            } catch (Exception ex) {
//                log.error("插入退款系统异常 browse 埋点失败", ex);
//            }
            return MessagePacket.newFail(MessageHeader.Code.fail, "系统内部报错: " + e.getMessage());
        }
    }

    private void rollbackOrderStateOnFail(String memberOrderID) {
        try {
            Map<String, Object> rollbackMap = new HashMap<>();
            rollbackMap.put("id", memberOrderID);
            rollbackMap.put("status", 4);
            rollbackMap.put("returnType", null);
            memberOrderService.update(rollbackMap);
            log.info("退款失败，已成功将订单状态回滚为待发货。订单ID: {}", memberOrderID);
        } catch (Exception e) {
            log.error("退款失败后，回滚订单状态异常! 订单ID: {}", memberOrderID, e);
        }
    }



    @ApiOperation(value = "refundOneMemberOrderForWXCallBack", notes = "微信支付订单退款结果回调通知")
    @RequestMapping(value = "/refundOneMemberOrderForWXCallBack", method = {RequestMethod.POST}, produces = {"application/json"})
    @ResponseBody
    public Map<String, String> refundOneMemberOrderForWXCallBack(HttpServletRequest request, @RequestBody String requestBody) {
        Map<String, String> resultMap = new HashMap<>();
        log.info("【微信回调】收到报文: {}", requestBody);

        try {
            String apiV3KeyStr = "XuqingboHulianzhixiang2026041300";
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSON.parseObject(requestBody);
            com.alibaba.fastjson.JSONObject resource = jsonObject.getJSONObject("resource");

            String plainText = decryptV3(resource.getString("associated_data"), resource.getString("nonce"), resource.getString("ciphertext"), apiV3KeyStr);
            log.info("【微信回调】解密成功: {}", plainText);

            com.alibaba.fastjson.JSONObject refundData = com.alibaba.fastjson.JSON.parseObject(plainText);
            String refundStatus = refundData.getString("refund_status");

            if ("SUCCESS".equals(refundStatus)) {
                String outRefundNo = refundData.getString("out_refund_no");
                String memberOrderID = outRefundNo.replace("REFUND_", "");

                MemberOrder memberOrder = memberOrderService.findById(memberOrderID);
                if (memberOrder != null) {

                    java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
                    String successTimeStr = refundData.getString("success_time");
                    if (StringUtils.isNotBlank(successTimeStr)) {
                        java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(successTimeStr);
                        ts = java.sql.Timestamp.valueOf(odt.toLocalDateTime());
                    }

                    double actualAmount = 0.0;
                    if (refundData.getJSONObject("amount") != null) {
                        actualAmount = refundData.getJSONObject("amount").getInteger("refund") / 100.0;
                    }

                    Map<String, Object> orderUpdateMap = new HashMap<>();
                    orderUpdateMap.put("id", memberOrderID);
                    orderUpdateMap.put("payStatus", 11);
                    orderUpdateMap.put("status", 5);
                    orderUpdateMap.put("returnTime", ts);
                    orderUpdateMap.put("returnAmount", actualAmount);
                    memberOrderService.update(orderUpdateMap);

                    try {
                        if (StringUtils.isNotBlank(memberOrder.getMemberPaymentID())) {
                            com.kingpivot.model.MemberPayment memberPayment = memberPaymentService.findById(memberOrder.getMemberPaymentID());
                            if (memberPayment != null) {
                                memberPayment.setReturnTime(ts);
                                memberPaymentService.update(memberPayment);
                            }
                        }
                    } catch (Exception e) {
                        log.error("【微信回调】同步更新支付单(memberPayment)退款时间失败: {}", e.getMessage());
                    }

                    try {
                        String memberID = memberOrder.getMemberID();
                        String refundMsg = String.format("您的订单[%s]退款成功，金额：%.2f元", memberOrder.getOrderCode(), actualAmount);
                        com.kingpivot.api.dto.message.OutMessageDto msgDto = new com.kingpivot.api.dto.message.OutMessageDto();
                        msgDto.setReceiverMemberID(memberID);
                        msgDto.setName("退款通知");
                        msgDto.setDescription(refundMsg);
                        msgDto.setObjectID(memberOrder.getId());
                        msgDto.setCode("160");
                        msgDto.setIsPush(1);
                        msgDto.setApplicationID(memberOrder.getApplicationID());
                        msgDto.setSendCompanyID(memberOrder.getCompanyID());
                        msgDto.setSendShopID(memberOrder.getShopID());
                        msgDto.setReceiverNumber(memberOrder.getPhone());
                        sendMessageService.sendCreateMessage(msgDto);
                        log.info("【微信回调】已通过 sendCreateMessage 生成数据库消息，会员: {}", memberID);
                    } catch (Exception e) {
                        log.error("【微信回调】调用 sendCreateMessage 失败: {}", e.getMessage());
                    }
                }
            }

            resultMap.put("code", "SUCCESS");
            resultMap.put("message", "成功");
        } catch (Exception e) {
            log.error("【微信回调】处理异常: ", e);
            resultMap.put("code", "FAIL");
        }
        return resultMap;
    }


    private String decryptV3(String associatedData, String nonce, String ciphertext, String apiV3Key) throws Exception {
        byte[] key = apiV3Key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] nonceBytes = nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] associatedDataBytes = associatedData.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] ciphertextBytes = java.util.Base64.getDecoder().decode(ciphertext);

        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, "AES");
        javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, nonceBytes);

        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, spec);
        cipher.updateAAD(associatedDataBytes);

        return new String(cipher.doFinal(ciphertextBytes), java.nio.charset.StandardCharsets.UTF_8);
    }




    @ApiOperation(value = "outSystemCreateOneMemberOrder", notes = "独立接口：外部系统创建一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "outToken", value = "登录标识", paramType = "query", dataType = "String"),
    })
    @RequestMapping(value = "/outSystemCreateOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemCreateOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
        // User user = userService.selectById(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String applicationID = request.getParameter("applicationID"); // 应用ID
        String siteID = request.getParameter("siteID"); // 站点id
        String channelID = request.getParameter("channelID"); // 社交媒体ID
        String cityID = request.getParameter("cityID"); // 城市ID
        String companyID = request.getParameter("companyID"); // 销售公司ID
        String shopID = request.getParameter("shopID"); // 销售店铺ID
        String propertyID = request.getParameter("propertyID"); // 物业ID
        String departmentID = request.getParameter("departmentID"); // 部门ID
        String distributorCompanyID = request.getParameter("distributorCompanyID"); // 销售渠道公司ID
        String recommendMemberID = request.getParameter("recommendMemberID"); // 推荐会员ID
        String salesMemberID = request.getParameter("salesMemberID"); // 销售会员ID
        String salesEmployeeID = request.getParameter("salesEmployeeID"); // 销售员工ID
        String roomID = request.getParameter("roomID"); // 桌子包房ID
        String tableNumber = request.getParameter("tableNumber"); // 桌号
        String houseID = request.getParameter("houseID"); // 房屋ID
        String officeID = request.getParameter("officeID"); // 办公室ID
        String machineID = request.getParameter("machineID"); // 机器ID
        String orderTemplateID = request.getParameter("orderTemplateID"); // 订单模板ID
        String cardDefineID = request.getParameter("cardDefineID"); // 卡券定义ID
        String actionID = request.getParameter("actionID"); // 活动定义ID
        String constructionProjectID = request.getParameter("constructionProjectID"); // 建设项目ID
        String constructionID = request.getParameter("constructionID"); // 工地ID
        String sceneID = request.getParameter("sceneID"); // 专场ID
        String tenderID = request.getParameter("tenderID"); // 拍卖ID
        String goodsTogetherDefineID = request.getParameter("goodsTogetherDefineID"); // 拼团定义ID
        String goodsTogetherID = request.getParameter("goodsTogetherID"); // 拼团ID
        String goodsTogetherJoinID = request.getParameter("goodsTogetherJoinID"); // 拼团参加ID
        String goodsSecondDefineID = request.getParameter("goodsSecondDefineID"); // 秒杀定义ID
        String goodsSecondID = request.getParameter("goodsSecondID"); // 秒杀记录ID
        String goodsPriceDefineID = request.getParameter("goodsPriceDefineID"); // 特价定义ID
        String goodsPriceID = request.getParameter("goodsPriceID"); // 商品特价ID
        String trainBusinessID = request.getParameter("trainBusinessID"); // 培训业务ID
        String trainCourseID = request.getParameter("trainCourseID"); // 培训课程ID
        String trainHourID = request.getParameter("trainHourID"); // 培训课时ID
        String memberTrainID = request.getParameter("memberTrainID"); // 培训报名ID
        String memberBuyReadID = request.getParameter("memberBuyReadID"); // 会员订阅ID
        String majorID = request.getParameter("majorID"); // 专业定义ID
        String memberMajorID = request.getParameter("memberMajorID"); // 会员专业ID
        String goldenGetID = request.getParameter("goldenGetID"); // 金币获取ID
        String memberPrivilegeID = request.getParameter("memberPrivilegeID"); // 会员特权ID
        String hospitalDiagnosisID = request.getParameter("hospitalDiagnosisID"); // 诊疗记录ID
        String buyCompanyID = request.getParameter("buyCompanyID"); // 订购公司ID
        String buyShopID = request.getParameter("buyShopID"); // 订购店铺
        String buyEmployeeID = request.getParameter("buyEmployeeID"); // 购买员工ID
        String memberID = request.getParameter("memberID"); // 购买会员ID
        String useEmployeeID = request.getParameter("useEmployeeID"); // 使用员工ID
        String useMemberID = request.getParameter("useMemberID"); // 使用会员ID
        String name = request.getParameter("name"); // 名称
        String financeType = request.getParameter("financeType"); // 财务类型
        String actionGoodsType = request.getParameter("actionGoodsType"); // 活动商品类型
        String outPayType = request.getParameter("outPayType"); // 外部支付方式
        String buyType = request.getParameter("buyType"); // 购买类型
        String orderType = request.getParameter("orderType"); // 订单类型
        String orderCode = request.getParameter("orderCode"); // 订单编号
        String orderSequnce = request.getParameter("orderSequnce"); // 订单流水号
        String applyTime = request.getParameter("applyTime"); // 申请时间
        String cancelTime = request.getParameter("cancelTime"); // 取消时间
        String cancelConfirmUserID = request.getParameter("cancelConfirmUserID"); // 确认取消人
        String cancelConformMemberID = request.getParameter("cancelConformMemberID"); // 确认取消会员ID
        String cancelConformEmployeeID = request.getParameter("cancelConformEmployeeID"); // 确认取消员工
        String cancelConfirmTime = request.getParameter("cancelConfirmTime"); // 确认取消时间
        String goodsNumbers = request.getParameter("goodsNumbers"); // 商品种类数
        String goodsQTY = request.getParameter("goodsQTY"); // 商品数量
        String priceTotal = request.getParameter("priceTotal"); // 商品总价格
        String weighTotal = request.getParameter("weighTotal"); // 总重量（克）
        String weightTotalFee = request.getParameter("weightTotalFee"); // 计费重量（克）
        String sendPrice = request.getParameter("sendPrice"); // 运费
        String subPrePay = request.getParameter("subPrePay"); // 减掉预付款
        String pointUsed = request.getParameter("pointUsed"); // 积分使用个数
        String pointSubAmount = request.getParameter("pointSubAmount"); // 积分抵扣金额
        String rebateSubAmount = request.getParameter("rebateSubAmount"); // 返利抵扣金额
        String bonusAmount = request.getParameter("bonusAmount"); // 抵扣券金额
        String weighPrice = request.getParameter("weighPrice"); // 称重浮动价格
        String discountAmount = request.getParameter("discountAmount"); // 折扣金额
        String priceAfterDiscount = request.getParameter("priceAfterDiscount"); // 优惠后总价格
        String tax = request.getParameter("tax"); // 税费
        String serviceFee = request.getParameter("serviceFee"); // 平台服务费
        String closedPriceTotal = request.getParameter("closedPriceTotal"); // 结算总价格
        String payTotal = request.getParameter("payTotal"); // 实际付款金额
        String incomeTotal = request.getParameter("incomeTotal"); // 订单收入金额
        String pointTotal = request.getParameter("pointTotal"); // 积分总价格
        String rebateTotal = request.getParameter("rebateTotal"); // 返利总价格
        String goldenTotal = request.getParameter("goldenTotal"); // 金币总价格
        String beanTotal = request.getParameter("beanTotal"); // 豆子总价格
        String presentPayAmount = request.getParameter("presentPayAmount"); // 礼品卡支付金额
        String presentRecordID = request.getParameter("presentRecordID"); // 礼品卡记录ID
        String payFrom = request.getParameter("payFrom"); // 付款方式
        String payTime = request.getParameter("payTime"); // 付款时间
        String payUserID = request.getParameter("payUserID"); // 付款用户ID
        String payEmployeeID = request.getParameter("payEmployeeID"); // 付款员工ID
        String payMemberID = request.getParameter("payMemberID"); // 付款会员ID
        String paywayID = request.getParameter("paywayID"); // 支付机构ID
        String paySequence = request.getParameter("paySequence"); // 付款流水号
        String memberPaymentID = request.getParameter("memberPaymentID"); // 会员支付ID
        String companyPaymentID = request.getParameter("companyPaymentID"); // 公司付款ID
        String payStatus = request.getParameter("payStatus"); // 付款状态
        String returnType = request.getParameter("returnType"); // 退款类型
        String returnTime = request.getParameter("returnTime"); // 退款时间
        String returnAmount = request.getParameter("returnAmount"); // 退款金额
        String returnPaywayID = request.getParameter("returnPaywayID"); // 退款方式
        String returnSequence = request.getParameter("returnSequence"); // 退款流水号
        String sendType = request.getParameter("sendType"); // 发货方式
        String sendUrgencyType = request.getParameter("sendUrgencyType"); // 送货紧急程度
        String memberPaywayID = request.getParameter("memberPaywayID"); // 会员钱包地址ID
        String memberAddressID = request.getParameter("memberAddressID"); // 会员地址ID
        String memberAddressName = request.getParameter("memberAddressName"); // 会员地址名称
        String contactName = request.getParameter("contactName"); // 联系人
        String phone = request.getParameter("phone"); // 联系电话
        String reservedDate = request.getParameter("reservedDate"); // 预约日期
        String getTime = request.getParameter("getTime"); // 提货时间
        String memberMemo = request.getParameter("memberMemo"); // 会员留言
        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID"); // 会员发票ID
        String invoiceID = request.getParameter("invoiceID"); // 发票ID
        String invoiceCode = request.getParameter("invoiceCode"); // 发票号码
        String invoiceDate = request.getParameter("invoiceDate"); // 发票日期
        String invoiceStatus = request.getParameter("invoiceStatus"); // 发票状态
        String processUserID = request.getParameter("processUserID"); // 订单处理用户
        String processEmployeeID = request.getParameter("processEmployeeID"); // 订单处理员工ID
        String processMemberID = request.getParameter("processMemberID"); // 订单处理会员
        String processTime = request.getParameter("processTime"); // 订单处理时间
        String weighedUserID = request.getParameter("weighedUserID"); // 称重用户
        String weighedTime = request.getParameter("weighedTime"); // 称重时间
        String outUserID = request.getParameter("outUserID"); // 出库用户
        String outMemberID = request.getParameter("outMemberID"); // 出库会员ID
        String outEmployeeID = request.getParameter("outEmployeeID"); // 出库员工ID
        String outTime = request.getParameter("outTime"); // 出库时间
        String expressID = request.getParameter("expressID"); // 发货机构ID
        String sendUserID = request.getParameter("sendUserID"); // 发货用户
        String sendEmployeeID = request.getParameter("sendEmployeeID"); // 发货员工ID
        String sendoutMemberID = request.getParameter("sendoutMemberID"); // 发货会员ID
        String sendTime = request.getParameter("sendTime"); // 发货时间
        String sendCode = request.getParameter("sendCode"); // 发货单号码
        String sendPayType = request.getParameter("sendPayType"); // 货到付款方式
        String sendStatus = request.getParameter("sendStatus"); // 发货状态
        String sendPayTime = request.getParameter("sendPayTime"); // 货到付款时间
        String sendPayAmount = request.getParameter("sendPayAmount"); // 货到付款金额
        String sendCompanyID = request.getParameter("sendCompanyID"); // 配送机构ID
        String sendMemberID = request.getParameter("sendMemberID"); // 配送员ID
        String sendFee = request.getParameter("sendFee"); // 配送费
        String payINtime = request.getParameter("payINtime"); // 交款时间
        String payINType = request.getParameter("payINType"); // 交款方式
        String payINAmount = request.getParameter("payINAmount"); // 交款金额
        String payINuserID = request.getParameter("payINuserID"); // 收款人
        String prepay_id = request.getParameter("prepay_id"); // 微信预付款新ID
        String confirmPayTime = request.getParameter("confirmPayTime"); // 确认收款时间
        String confirmPayUserID = request.getParameter("confirmPayUserID"); // 确认收款用户ID
        String confirmPayEmployeeID = request.getParameter("confirmPayEmployeeID"); // 确认收款员工ID
        String confirmPayMemberID = request.getParameter("confirmPayMemberID"); // 确认收款会员ID
        String givePayFrom = request.getParameter("givePayFrom"); // 给卖家付款方式
        String givePayTime = request.getParameter("givePayTime"); // 给卖家付款时间
        String givePaywayID = request.getParameter("givePaywayID"); // 给卖家支付机构ID
        String givePaySequence = request.getParameter("givePaySequence"); // 给卖家付款流水号
        String tianmaoCode = request.getParameter("tianmaoCode"); // 天猫订单编码
        String taobaoCode = request.getParameter("taobaoCode"); // 淘宝订单编码
        String jingdongCode = request.getParameter("jingdongCode"); // 京东订单编码
        String deliveryNumber = request.getParameter("deliveryNumber"); // 送货单个数
        String deliveryLeftQTY = request.getParameter("deliveryLeftQTY"); // 未送货商品个数
        String getGoodsTime = request.getParameter("getGoodsTime"); // 签收时间
        String getGoodsUserID = request.getParameter("getGoodsUserID"); // 签收用户ID
        String getGoodsEmployeeID = request.getParameter("getGoodsEmployeeID"); // 签收员工ID
        String getGoodsMemberID = request.getParameter("getGoodsMemberID"); // 签收会员ID
        String bizOrderNo = request.getParameter("bizOrderNo"); // 业务订单编号
        String orderNo = request.getParameter("orderNo"); // 业务订单序号
        String returnBizOrderNo = request.getParameter("returnBizOrderNo"); // 业务退回订单编号
        String applyReturnTime = request.getParameter("applyReturnTime"); // 申请退货时间
        String returnGoodsTime = request.getParameter("returnGoodsTime"); // 同意退货时间
        String returnCode = request.getParameter("returnCode"); // 退货单号
        String outOrderCode = request.getParameter("outOrderCode"); // 外部订单编号
        String isMemberHide = request.getParameter("isMemberHide"); // 会员是否隐藏
        String status = request.getParameter("status"); // 订单状态
        String goodsShopID = request.getParameter("goodsShopID"); // 订单状态
        String periodQTY = request.getParameter("periodQTY"); // 订单状态

        if (StringUtils.isNotBlank(applicationID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("application", applicationID))) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID 不正确");
            }
            map.put("applicationID", applicationID);
        }
        if (StringUtils.isNotBlank(siteID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("site", siteID))) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID 不正确");
            }
            map.put("siteID", siteID);
        }
        if (StringUtils.isNotBlank(channelID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("channel", channelID))) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID 不正确");
            }
            map.put("channelID", channelID);
        }
        if (StringUtils.isNotBlank(cityID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("city", cityID))) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDNotFound, "cityID 不正确");
            }
            map.put("cityID", cityID);
        }
        if (StringUtils.isNotBlank(companyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID 不正确");
            }
            map.put("companyID", companyID);
        }
        if (StringUtils.isNotBlank(shopID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("shop", shopID))) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID 不正确");
            }
            map.put("shopID", shopID);
        }
        if (StringUtils.isNotBlank(propertyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("property", propertyID))) {
                return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "propertyID 不正确");
            }
            map.put("propertyID", propertyID);
        }
        if (StringUtils.isNotBlank(departmentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("department", departmentID))) {
                return MessagePacket.newFail(MessageHeader.Code.departmentIDNotFound, "departmentID 不正确");
            }
            map.put("departmentID", departmentID);
        }
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", distributorCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID 不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", recommendMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID 不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", salesMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "salesMemberID 不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", salesEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID 不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        if (StringUtils.isNotBlank(roomID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("room", roomID))) {
                return MessagePacket.newFail(MessageHeader.Code.roomIDNotFound, "roomID 不正确");
            }
            map.put("roomID", roomID);
        }
        if (StringUtils.isNotBlank(tableNumber)) {
            map.put("tableNumber", tableNumber);
        }
        if (StringUtils.isNotBlank(houseID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("house", houseID))) {
                return MessagePacket.newFail(MessageHeader.Code.houseIDNotFound, "houseID 不正确");
            }
            map.put("houseID", houseID);
        }
        if (StringUtils.isNotBlank(officeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("office", officeID))) {
                return MessagePacket.newFail(MessageHeader.Code.officeIDNotFound, "officeID 不正确");
            }
            map.put("officeID", officeID);
        }
        if (StringUtils.isNotBlank(machineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("machine", machineID))) {
                return MessagePacket.newFail(MessageHeader.Code.machineIDNotFound, "machineID 不正确");
            }
            map.put("machineID", machineID);
        }
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("orderTemplate", orderTemplateID))) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIDNotFound, "orderTemplateID 不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        if (StringUtils.isNotBlank(cardDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("cardDefine", cardDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.cardDefineIDNotFound, "cardDefineID 不正确");
            }
            map.put("cardDefineID", cardDefineID);
        }
        if (StringUtils.isNotBlank(actionID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("action", actionID))) {
                return MessagePacket.newFail(MessageHeader.Code.actionIDNotFound, "actionID 不正确");
            }
            map.put("actionID", actionID);
        }
        if (StringUtils.isNotBlank(constructionProjectID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("constructionProject", constructionProjectID))) {
                return MessagePacket.newFail(MessageHeader.Code.constructionProjectIDNotFound, "constructionProjectID 不正确");
            }
            map.put("constructionProjectID", constructionProjectID);
        }
        if (StringUtils.isNotBlank(constructionID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("construction", constructionID))) {
                return MessagePacket.newFail(MessageHeader.Code.constructionIDNotFound, "constructionID 不正确");
            }
            map.put("constructionID", constructionID);
        }
        if (StringUtils.isNotBlank(sceneID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("scene", sceneID))) {
                return MessagePacket.newFail(MessageHeader.Code.sceneIDNotFound, "sceneID 不正确");
            }
            map.put("sceneID", sceneID);
        }
        if (StringUtils.isNotBlank(tenderID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("tender", tenderID))) {
                return MessagePacket.newFail(MessageHeader.Code.tenderIDNotFound, "tenderID 不正确");
            }
            map.put("tenderID", tenderID);
        }
        if (StringUtils.isNotBlank(goodsTogetherDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogetherDefine", goodsTogetherDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsTogetherDefineIDNotFound, "goodsTogetherDefineID 不正确");
            }
            map.put("goodsTogetherDefineID", goodsTogetherDefineID);
        }
        if (StringUtils.isNotBlank(goodsTogetherID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogether", goodsTogetherID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsTogetherIDNotFound, "goodsTogetherID 不正确");
            }
            map.put("goodsTogetherID", goodsTogetherID);
        }
        if (StringUtils.isNotBlank(goodsTogetherJoinID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogetherJoin", goodsTogetherJoinID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsTogetherJoinIDNotFound, "goodsTogetherJoinID 不正确");
            }
            map.put("goodsTogetherJoinID", goodsTogetherJoinID);
        }
        if (StringUtils.isNotBlank(goodsSecondDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsSecondDefine", goodsSecondDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsSecondDefineIDNotFound, "goodsSecondDefineID 不正确");
            }
            map.put("goodsSecondDefineID", goodsSecondDefineID);
        }
        if (StringUtils.isNotBlank(goodsSecondID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsSecond", goodsSecondID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsSecondIDNotFound, "goodsSecondID 不正确");
            }
            map.put("goodsSecondID", goodsSecondID);
        }
        if (StringUtils.isNotBlank(goodsPriceDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsPriceDefine", goodsPriceDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceDefineID 不正确");
            }
            map.put("goodsPriceDefineID", goodsPriceDefineID);
        }
        if (StringUtils.isNotBlank(goodsPriceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsPrice", goodsPriceID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceID 不正确");
            }
            map.put("goodsPriceID", goodsPriceID);
        }
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainBusiness", trainBusinessID))) {
                return MessagePacket.newFail(MessageHeader.Code.trainBusinessIDNotFound, "trainBusinessID 不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        if (StringUtils.isNotBlank(trainCourseID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainCourse", trainCourseID))) {
                return MessagePacket.newFail(MessageHeader.Code.trainCourseIDNotFound, "trainCourseID 不正确");
            }
            map.put("trainCourseID", trainCourseID);
        }
        if (StringUtils.isNotBlank(trainHourID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainHour", trainHourID))) {
                return MessagePacket.newFail(MessageHeader.Code.trainHourIDNotFound, "trainHourID 不正确");
            }
            map.put("trainHourID", trainHourID);
        }
        if (StringUtils.isNotBlank(memberTrainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberTrain", memberTrainID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberTrainIDNotFound, "memberTrainID 不正确");
            }
            map.put("memberTrainID", memberTrainID);
        }
        if (StringUtils.isNotBlank(memberBuyReadID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberBuyRead", memberBuyReadID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberBuyReadIDNotFound, "memberBuyReadID 不正确");
            }
            map.put("memberBuyReadID", memberBuyReadID);
        }
        if (StringUtils.isNotBlank(majorID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("major", majorID))) {
                return MessagePacket.newFail(MessageHeader.Code.majorIDNotFound, "majorID 不正确");
            }
            map.put("majorID", majorID);
        }
        if (StringUtils.isNotBlank(memberMajorID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberMajor", memberMajorID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberMajorID 不正确");
            }
            map.put("memberMajorID", memberMajorID);
        }
        if (StringUtils.isNotBlank(goldenGetID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goldenGet", goldenGetID))) {
                return MessagePacket.newFail(MessageHeader.Code.goldenGetIDNotFound, "goldenGetID 不正确");
            }
            map.put("goldenGetID", goldenGetID);
        }
        if (StringUtils.isNotBlank(memberPrivilegeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPrivilege", memberPrivilegeID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPrivilegeIDNotFound, "memberPrivilegeID 不正确");
            }
            map.put("memberPrivilegeID", memberPrivilegeID);
        }
        if (StringUtils.isNotBlank(hospitalDiagnosisID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("hospitalDiagnosis", hospitalDiagnosisID))) {
                return MessagePacket.newFail(MessageHeader.Code.hospitalDiagnosisIDNotFound, "hospitalDiagnosisID 不正确");
            }
            map.put("hospitalDiagnosisID", hospitalDiagnosisID);
        }
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", buyCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "buyCompanyID 不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }
        if (StringUtils.isNotBlank(buyShopID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("shop", buyShopID))) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "buyShopID 不正确");
            }
            map.put("buyShopID", buyShopID);
        }
        if (StringUtils.isNotBlank(buyEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", buyEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "buyEmployeeID 不正确");
            }
            map.put("buyEmployeeID", buyEmployeeID);
        }
        if (StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID 不正确");
            }
            map.put("memberID", memberID);
        }
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", useEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "useEmployeeID 不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", useMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "useMemberID 不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
        }
        if (StringUtils.isNotBlank(financeType)) {
            if (!NumberUtils.isNumeric(financeType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "financeType 请输入数字");
            }
            map.put("financeType", financeType);
        }
        if (StringUtils.isNotBlank(actionGoodsType)) {
            if (!NumberUtils.isNumeric(actionGoodsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "actionGoodsType 请输入数字");
            }
            map.put("actionGoodsType", actionGoodsType);
        }
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "outPayType 请输入数字");
            }
            map.put("outPayType", outPayType);
        }
        if (StringUtils.isNotBlank(buyType)) {
            if (!NumberUtils.isNumeric(buyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "buyType 请输入数字");
            }
            map.put("buyType", buyType);
        }
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "orderType 请输入数字");
            }
            map.put("orderType", orderType);
        }
        if (StringUtils.isNotBlank(orderCode)) {
            map.put("orderCode", orderCode);
        }
        if (StringUtils.isNotBlank(orderSequnce)) {
            map.put("orderSequnce", orderSequnce);
        }
        if (StringUtils.isNotBlank(applyTime)) {
            if (!TimeTest.dateIsPass(applyTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "applyTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("applyTime", applyTime);
        }
        if (StringUtils.isNotBlank(cancelTime)) {
            if (!TimeTest.dateIsPass(cancelTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "cancelTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("cancelTime", cancelTime);
        }
        if (StringUtils.isNotBlank(cancelConfirmUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", cancelConfirmUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "cancelConfirmUserID 不正确");
            }
            map.put("cancelConfirmUserID", cancelConfirmUserID);
        }
        if (StringUtils.isNotBlank(cancelConformMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", cancelConformMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "cancelConformMemberID 不正确");
            }
            map.put("cancelConformMemberID", cancelConformMemberID);
        }
        if (StringUtils.isNotBlank(cancelConformEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", cancelConformEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "cancelConformEmployeeID 不正确");
            }
            map.put("cancelConformEmployeeID", cancelConformEmployeeID);
        }
        if (StringUtils.isNotBlank(cancelConfirmTime)) {
            if (!TimeTest.dateIsPass(cancelConfirmTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "cancelConfirmTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("cancelConfirmTime", cancelConfirmTime);
        }
        if (StringUtils.isNotBlank(goodsNumbers)) {
            if (!NumberUtils.isNumeric(goodsNumbers)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "goodsNumbers 请输入数字");
            }
            map.put("goodsNumbers", goodsNumbers);
        }
        if (StringUtils.isNotBlank(goodsQTY)) {
            if (!NumberUtils.isNumeric(goodsQTY)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "goodsQTY 请输入数字");
            }
            map.put("goodsQTY", goodsQTY);
        }
        if (StringUtils.isNotBlank(priceTotal)) {
            map.put("priceTotal", priceTotal);
        }
        if (StringUtils.isNotBlank(weighTotal)) {
            if (!NumberUtils.isNumeric(weighTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "weighTotal 请输入数字");
            }
            map.put("weighTotal", weighTotal);
        }
        if (StringUtils.isNotBlank(weightTotalFee)) {
            if (!NumberUtils.isNumeric(weightTotalFee)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "weightTotalFee 请输入数字");
            }
            map.put("weightTotalFee", weightTotalFee);
        }
        if (StringUtils.isNotBlank(sendPrice)) {
            map.put("sendPrice", sendPrice);
        }
        if (StringUtils.isNotBlank(subPrePay)) {
            map.put("subPrePay", subPrePay);
        }
        if (StringUtils.isNotBlank(pointUsed)) {
            if (!NumberUtils.isNumeric(pointUsed)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "pointUsed 请输入数字");
            }
            map.put("pointUsed", pointUsed);
        }
        if (StringUtils.isNotBlank(pointSubAmount)) {
            map.put("pointSubAmount", pointSubAmount);
        }
        if (StringUtils.isNotBlank(rebateSubAmount)) {
            map.put("rebateSubAmount", rebateSubAmount);
        }
        if (StringUtils.isNotBlank(bonusAmount)) {
            map.put("bonusAmount", bonusAmount);
        }
        if (StringUtils.isNotBlank(weighPrice)) {
            map.put("weighPrice", weighPrice);
        }
        if (StringUtils.isNotBlank(discountAmount)) {
            map.put("discountAmount", discountAmount);
        }
        if (StringUtils.isNotBlank(priceAfterDiscount)) {
            map.put("priceAfterDiscount", priceAfterDiscount);
        }
        if (StringUtils.isNotBlank(tax)) {
            map.put("tax", tax);
        }
        if (StringUtils.isNotBlank(serviceFee)) {
            map.put("serviceFee", serviceFee);
        }
        if (StringUtils.isNotBlank(closedPriceTotal)) {
            map.put("closedPriceTotal", closedPriceTotal);
        }
        if (StringUtils.isNotBlank(payTotal)) {
            map.put("payTotal", payTotal);
        }
        if (StringUtils.isNotBlank(incomeTotal)) {
            map.put("incomeTotal", incomeTotal);
        }
        if (StringUtils.isNotBlank(pointTotal)) {
            if (!NumberUtils.isNumeric(pointTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "pointTotal 请输入数字");
            }
            map.put("pointTotal", pointTotal);
        }
        if (StringUtils.isNotBlank(rebateTotal)) {
            map.put("rebateTotal", rebateTotal);
        }
        if (StringUtils.isNotBlank(goldenTotal)) {
            if (!NumberUtils.isNumeric(goldenTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "goldenTotal 请输入数字");
            }
            map.put("goldenTotal", goldenTotal);
        }
        if (StringUtils.isNotBlank(beanTotal)) {
            if (!NumberUtils.isNumeric(beanTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "beanTotal 请输入数字");
            }
            map.put("beanTotal", beanTotal);
        }
        if (StringUtils.isNotBlank(presentPayAmount)) {
            map.put("presentPayAmount", presentPayAmount);
        }
        if (StringUtils.isNotBlank(presentRecordID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("presentRecord", presentRecordID))) {
                return MessagePacket.newFail(MessageHeader.Code.presentRecordIDNotFound, "presentRecordID 不正确");
            }
            map.put("presentRecordID", presentRecordID);
        }
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payFrom 请输入数字");
            }
            map.put("payFrom", payFrom);
        }
        if (StringUtils.isNotBlank(payTime)) {
            if (!TimeTest.dateIsPass(payTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "payTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("payTime", payTime);
        }
        if (StringUtils.isNotBlank(payUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", payUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "payUserID 不正确");
            }
            map.put("payUserID", payUserID);
        }
        if (StringUtils.isNotBlank(payEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", payEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "payEmployeeID 不正确");
            }
            map.put("payEmployeeID", payEmployeeID);
        }
        if (StringUtils.isNotBlank(payMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", payMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "payMemberID 不正确");
            }
            map.put("payMemberID", payMemberID);
        }
        if (StringUtils.isNotBlank(paywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", paywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDNotFound, "paywayID 不正确");
            }
            map.put("paywayID", paywayID);
        }
        if (StringUtils.isNotBlank(paySequence)) {
            map.put("paySequence", paySequence);
        }
        if (StringUtils.isNotBlank(memberPaymentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPayment", memberPaymentID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPaymentIDNotFound, "memberPaymentID 不正确");
            }
            map.put("memberPaymentID", memberPaymentID);
        }
        if (StringUtils.isNotBlank(companyPaymentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("companyPayment", companyPaymentID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyPaymentIDNotFound, "companyPaymentID 不正确");
            }
            map.put("companyPaymentID", companyPaymentID);
        }
        if (StringUtils.isNotBlank(payStatus)) {
            if (!NumberUtils.isNumeric(payStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payStatus 请输入数字");
            }
            map.put("payStatus", payStatus);
        }
        if (StringUtils.isNotBlank(returnType)) {
            if (!NumberUtils.isNumeric(returnType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "returnType 请输入数字");
            }
            map.put("returnType", returnType);
        }
        if (StringUtils.isNotBlank(returnTime)) {
            if (!TimeTest.dateIsPass(returnTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "returnTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("returnTime", returnTime);
        }
        if (StringUtils.isNotBlank(returnAmount)) {
            map.put("returnAmount", returnAmount);
        }
        if (StringUtils.isNotBlank(returnPaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", returnPaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDNotFound, "returnPaywayID 不正确");
            }
            map.put("returnPaywayID", returnPaywayID);
        }
        if (StringUtils.isNotBlank(returnSequence)) {
            map.put("returnSequence", returnSequence);
        }
        if (StringUtils.isNotBlank(sendType)) {
            if (!NumberUtils.isNumeric(sendType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendType 请输入数字");
            }
            map.put("sendType", sendType);
        }
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendUrgencyType 请输入数字");
            }
            map.put("sendUrgencyType", sendUrgencyType);
        }
        if (StringUtils.isNotBlank(memberPaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPayway", memberPaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPaywayIDNotFound, "memberPaywayID 不正确");
            }
            map.put("memberPaywayID", memberPaywayID);
        }
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberAddress", memberAddressID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDNotFound, "memberAddressID 不正确");
            }
            map.put("memberAddressID", memberAddressID);
        }
        if (StringUtils.isNotBlank(memberAddressName)) {
            map.put("memberAddressName", memberAddressName);
        }
        if (StringUtils.isNotBlank(contactName)) {
            map.put("contactName", contactName);
        }
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }
        if (StringUtils.isNotBlank(reservedDate)) {
            if (!TimeTest.dateIsPass(reservedDate)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "reservedDate 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("reservedDate", reservedDate);
        }
        if (StringUtils.isNotBlank(getTime)) {
            map.put("getTime", getTime);
        }
        if (StringUtils.isNotBlank(memberMemo)) {
            map.put("memberMemo", memberMemo);
        }
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberInvoiceDefine", memberInvoiceDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID 不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }
        if (StringUtils.isNotBlank(invoiceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("invoice", invoiceID))) {
                return MessagePacket.newFail(MessageHeader.Code.invoiceIDNotFound, "invoiceID 不正确");
            }
            map.put("invoiceID", invoiceID);
        }
        if (StringUtils.isNotBlank(invoiceCode)) {
            map.put("invoiceCode", invoiceCode);
        }
        if (StringUtils.isNotBlank(invoiceDate)) {
            if (!TimeTest.dateIsPass(invoiceDate)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "invoiceDate 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("invoiceDate", invoiceDate);
        }
        if (StringUtils.isNotBlank(invoiceStatus)) {
            if (!NumberUtils.isNumeric(invoiceStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "invoiceStatus 请输入数字");
            }
            map.put("invoiceStatus", invoiceStatus);
        }
        if (StringUtils.isNotBlank(processUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", processUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "processUserID 不正确");
            }
            map.put("processUserID", processUserID);
        }
        if (StringUtils.isNotBlank(processEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", processEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "processEmployeeID 不正确");
            }
            map.put("processEmployeeID", processEmployeeID);
        }
        if (StringUtils.isNotBlank(processMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", processMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "processMemberID 不正确");
            }
            map.put("processMemberID", processMemberID);
        }
        if (StringUtils.isNotBlank(processTime)) {
            if (!TimeTest.dateIsPass(processTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "processTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("processTime", processTime);
        }
        if (StringUtils.isNotBlank(weighedUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("weighedUseruser", weighedUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "weighedUserID 不正确");
            }
            map.put("weighedUserID", weighedUserID);
        }
        if (StringUtils.isNotBlank(weighedTime)) {
            if (!TimeTest.dateIsPass(weighedTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "weighedTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("weighedTime", weighedTime);
        }
        if (StringUtils.isNotBlank(outUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", outUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.outUserIDNotFound, "outUserID 不正确");
            }
            map.put("outUserID", outUserID);
        }
        if (StringUtils.isNotBlank(outMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", outMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "outMemberID 不正确");
            }
            map.put("outMemberID", outMemberID);
        }
        if (StringUtils.isNotBlank(outEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", outEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.outEmployeeIDNotFound, "outEmployeeID 不正确");
            }
            map.put("outEmployeeID", outEmployeeID);
        }
        if (StringUtils.isNotBlank(outTime)) {
            if (!TimeTest.dateIsPass(outTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "outTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("outTime", outTime);
        }
        if (StringUtils.isNotBlank(expressID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("express", expressID))) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDNotFound, "expressID 不正确");
            }
            map.put("expressID", expressID);
        }
        if (StringUtils.isNotBlank(sendUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", sendUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "sendUserID 不正确");
            }
            map.put("sendUserID", sendUserID);
        }
        if (StringUtils.isNotBlank(sendEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", sendEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "sendEmployeeID 不正确");
            }
            map.put("sendEmployeeID", sendEmployeeID);
        }
        if (StringUtils.isNotBlank(sendoutMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", sendoutMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "sendoutMemberID 不正确");
            }
            map.put("sendoutMemberID", sendoutMemberID);
        }
        if (StringUtils.isNotBlank(sendTime)) {
            if (!TimeTest.dateIsPass(sendTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "sendTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("sendTime", sendTime);
        }
        if (StringUtils.isNotBlank(sendCode)) {
            map.put("sendCode", sendCode);
        }
        if (StringUtils.isNotBlank(sendPayType)) {
            if (!NumberUtils.isNumeric(sendPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendPayType 请输入数字");
            }
            map.put("sendPayType", sendPayType);
        }
        if (StringUtils.isNotBlank(sendStatus)) {
            if (!NumberUtils.isNumeric(sendStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendStatus 请输入数字");
            }
            map.put("sendStatus", sendStatus);
        }
        if (StringUtils.isNotBlank(sendPayTime)) {
            if (!TimeTest.dateIsPass(sendPayTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "sendPayTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("sendPayTime", sendPayTime);
        }
        if (StringUtils.isNotBlank(sendPayAmount)) {
            map.put("sendPayAmount", sendPayAmount);
        }
        if (StringUtils.isNotBlank(sendCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", sendCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "sendCompanyID 不正确");
            }
            map.put("sendCompanyID", sendCompanyID);
        }
        if (StringUtils.isNotBlank(sendMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", sendMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "sendMemberID 不正确");
            }
            map.put("sendMemberID", sendMemberID);
        }
        if (StringUtils.isNotBlank(sendFee)) {
            map.put("sendFee", sendFee);
        }
        if (StringUtils.isNotBlank(payINtime)) {
            if (!TimeTest.dateIsPass(payINtime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "payINtime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("payINtime", payINtime);
        }
        if (StringUtils.isNotBlank(payINType)) {
            if (!NumberUtils.isNumeric(payINType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payINType 请输入数字");
            }
            map.put("payINType", payINType);
        }
        if (StringUtils.isNotBlank(payINAmount)) {
            map.put("payINAmount", payINAmount);
        }
        if (StringUtils.isNotBlank(payINuserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", payINuserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "payINuserID 不正确");
            }
            map.put("payINuserID", payINuserID);
        }
        if (StringUtils.isNotBlank(prepay_id)) {
            map.put("prepay_id", prepay_id);
        }
        if (StringUtils.isNotBlank(confirmPayTime)) {
            if (!TimeTest.dateIsPass(confirmPayTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "confirmPayTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("confirmPayTime", confirmPayTime);
        }
        if (StringUtils.isNotBlank(confirmPayUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", confirmPayUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "confirmPayUserID 不正确");
            }
            map.put("confirmPayUserID", confirmPayUserID);
        }
        if (StringUtils.isNotBlank(confirmPayEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", confirmPayEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "confirmPayEmployeeID 不正确");
            }
            map.put("confirmPayEmployeeID", confirmPayEmployeeID);
        }
        if (StringUtils.isNotBlank(confirmPayMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", confirmPayMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "confirmPayMemberID 不正确");
            }
            map.put("confirmPayMemberID", confirmPayMemberID);
        }
        if (StringUtils.isNotBlank(givePayFrom)) {
            if (!NumberUtils.isNumeric(givePayFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "givePayFrom 请输入数字");
            }
            map.put("givePayFrom", givePayFrom);
        }
        if (StringUtils.isNotBlank(givePayTime)) {
            if (!TimeTest.dateIsPass(givePayTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "givePayTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("givePayTime", givePayTime);
        }
        if (StringUtils.isNotBlank(givePaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", givePaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDNotFound, "givePaywayID 不正确");
            }
            map.put("givePaywayID", givePaywayID);
        }
        if (StringUtils.isNotBlank(givePaySequence)) {
            map.put("givePaySequence", givePaySequence);
        }
        if (StringUtils.isNotBlank(tianmaoCode)) {
            map.put("tianmaoCode", tianmaoCode);
        }
        if (StringUtils.isNotBlank(taobaoCode)) {
            map.put("taobaoCode", taobaoCode);
        }
        if (StringUtils.isNotBlank(jingdongCode)) {
            map.put("jingdongCode", jingdongCode);
        }
        if (StringUtils.isNotBlank(deliveryNumber)) {
            if (!NumberUtils.isNumeric(deliveryNumber)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "deliveryNumber 请输入数字");
            }
            map.put("deliveryNumber", deliveryNumber);
        }
        if (StringUtils.isNotBlank(deliveryLeftQTY)) {
            map.put("deliveryLeftQTY", deliveryLeftQTY);
        }
        if (StringUtils.isNotBlank(getGoodsTime)) {
            if (!TimeTest.dateIsPass(getGoodsTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "getGoodsTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("getGoodsTime", getGoodsTime);
        }
        if (StringUtils.isNotBlank(getGoodsUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", getGoodsUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "getGoodsUserID 不正确");
            }
            map.put("getGoodsUserID", getGoodsUserID);
        }
        if (StringUtils.isNotBlank(getGoodsEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", getGoodsEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "getGoodsEmployeeID 不正确");
            }
            map.put("getGoodsEmployeeID", getGoodsEmployeeID);
        }
        if (StringUtils.isNotBlank(getGoodsMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", getGoodsMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "getGoodsMemberID 不正确");
            }
            map.put("getGoodsMemberID", getGoodsMemberID);
        }
        if (StringUtils.isNotBlank(bizOrderNo)) {
            map.put("bizOrderNo", bizOrderNo);
        }
        if (StringUtils.isNotBlank(orderNo)) {
            map.put("orderNo", orderNo);
        }
        if (StringUtils.isNotBlank(returnBizOrderNo)) {
            map.put("returnBizOrderNo", returnBizOrderNo);
        }
        if (StringUtils.isNotBlank(applyReturnTime)) {
            if (!TimeTest.dateIsPass(applyReturnTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "applyReturnTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("applyReturnTime", applyReturnTime);
        }
        if (StringUtils.isNotBlank(returnGoodsTime)) {
            if (!TimeTest.dateIsPass(returnGoodsTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "returnGoodsTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("returnGoodsTime", returnGoodsTime);
        }
        if (StringUtils.isNotBlank(returnCode)) {
            map.put("returnCode", returnCode);
        }
        if (StringUtils.isNotBlank(outOrderCode)) {
            map.put("outOrderCode", outOrderCode);
        }
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide 请输入数字");
            }
            map.put("isMemberHide", isMemberHide);
        }
        if (StringUtils.isNotBlank(status)) {
            if (!NumberUtils.isNumeric(status)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "status 请输入数字");
            }
            map.put("status", status);
        } else {
            map.put("status", 1);
        }


        String memberOrderID = IdGenerator.uuid32();
        map.put("id", memberOrderID);
        map.put("creator", user.getId());

        memberOrderService.insertByMap(map);

        ApiMemberOrderDetail oneMemberOrderDetailByID = memberOrderService.getOneMemberOrderDetailByID(memberOrderID);

        if (StringUtils.isNotBlank(goodsShopID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsShop", goodsShopID))) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "goodsShopID 不正确");
            }
            map.clear();
            String memberOrderGoodsID = IdGenerator.uuid32();
            map.put("id", memberOrderGoodsID);

            if (oneMemberOrderDetailByID.getApplicationID() != null) {
                map.put("applicationID", oneMemberOrderDetailByID.getApplicationID());
            }

            // 	   应用ID
            if (oneMemberOrderDetailByID.getCompanyID() != null) {
                map.put("companyID", oneMemberOrderDetailByID.getCompanyID());
            }

            // 	   公司ID
            if (oneMemberOrderDetailByID.getShopID() != null) {
                map.put("shopID", oneMemberOrderDetailByID.getShopID());
            }

            // 	   店铺ID
            if (oneMemberOrderDetailByID.getMemberOrderID() != null) {
                map.put("memberOrderID", oneMemberOrderDetailByID.getMemberOrderID());
            }

            // 	   会员订单ID
            if (StringUtils.isNotBlank(periodQTY)) {
                if (!NumberUtils.isNumeric(periodQTY)) {
                    return MessagePacket.newFail(MessageHeader.Code.numberNot, "periodQTY 请输入数字");
                }
                map.put("periodQTY", periodQTY);
            }
            map.put("goodsShopID", goodsShopID);
            memberOrderGoodsService.insertGoodsByMap(map);
        }

        if (opLog != null) {
            String descriptions2 = "外部系统创建一个会员订单";
            sendMessageService.sendDataLogMessage(user, descriptions2, "outSystemCreateOneMemberOrder",
                    DataLog.objectDefineID.timeIntervalInstance.getOname(), null,
                    null, DataLog.OperateType.ADD, request);
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberOrderID", memberOrderID);
        return MessagePacket.newSuccess(rsMap, "outSystemCreateOneMemberOrder success");
    }

    @ApiOperation(value = "outSystemUpdateOneMemberOrder", notes = "独立接口：外部系统修改一个会员订单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "outToken", value = "登录标识", paramType = "query", dataType = "String"),
    })
    @RequestMapping(value = "/outSystemUpdateOneMemberOrder", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemUpdateOneMemberOrder(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
        // User user = userService.selectById(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberOrderID = request.getParameter("memberOrderID");
        if (StringUtils.isBlank(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDIsNull, "memberOrderID不能为空");
        }
        if (!memberOrderService.checkIsValidId(memberOrderID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberOrderIDNotFound, "memberOrderID不正确");
        }
        map.put("id", memberOrderID);


        String applicationID = request.getParameter("applicationID"); // 应用ID
        String siteID = request.getParameter("siteID"); // 站点id
        String channelID = request.getParameter("channelID"); // 社交媒体ID
        String cityID = request.getParameter("cityID"); // 城市ID
        String companyID = request.getParameter("companyID"); // 销售公司ID
        String shopID = request.getParameter("shopID"); // 销售店铺ID
        String propertyID = request.getParameter("propertyID"); // 物业ID
        String departmentID = request.getParameter("departmentID"); // 部门ID
        String distributorCompanyID = request.getParameter("distributorCompanyID"); // 销售渠道公司ID
        String recommendMemberID = request.getParameter("recommendMemberID"); // 推荐会员ID
        String salesMemberID = request.getParameter("salesMemberID"); // 销售会员ID
        String salesEmployeeID = request.getParameter("salesEmployeeID"); // 销售员工ID
        String roomID = request.getParameter("roomID"); // 桌子包房ID
        String tableNumber = request.getParameter("tableNumber"); // 桌号
        String houseID = request.getParameter("houseID"); // 房屋ID
        String officeID = request.getParameter("officeID"); // 办公室ID
        String machineID = request.getParameter("machineID"); // 机器ID
        String orderTemplateID = request.getParameter("orderTemplateID"); // 订单模板ID
        String cardDefineID = request.getParameter("cardDefineID"); // 卡券定义ID
        String actionID = request.getParameter("actionID"); // 活动定义ID
        String constructionProjectID = request.getParameter("constructionProjectID"); // 建设项目ID
        String constructionID = request.getParameter("constructionID"); // 工地ID
        String sceneID = request.getParameter("sceneID"); // 专场ID
        String tenderID = request.getParameter("tenderID"); // 拍卖ID
        String goodsTogetherDefineID = request.getParameter("goodsTogetherDefineID"); // 拼团定义ID
        String goodsTogetherID = request.getParameter("goodsTogetherID"); // 拼团ID
        String goodsTogetherJoinID = request.getParameter("goodsTogetherJoinID"); // 拼团参加ID
        String goodsSecondDefineID = request.getParameter("goodsSecondDefineID"); // 秒杀定义ID
        String goodsSecondID = request.getParameter("goodsSecondID"); // 秒杀记录ID
        String goodsPriceDefineID = request.getParameter("goodsPriceDefineID"); // 特价定义ID
        String goodsPriceID = request.getParameter("goodsPriceID"); // 商品特价ID
        String trainBusinessID = request.getParameter("trainBusinessID"); // 培训业务ID
        String trainCourseID = request.getParameter("trainCourseID"); // 培训课程ID
        String trainHourID = request.getParameter("trainHourID"); // 培训课时ID
        String memberTrainID = request.getParameter("memberTrainID"); // 培训报名ID
        String memberBuyReadID = request.getParameter("memberBuyReadID"); // 会员订阅ID
        String majorID = request.getParameter("majorID"); // 专业定义ID
        String memberMajorID = request.getParameter("memberMajorID"); // 会员专业ID
        String goldenGetID = request.getParameter("goldenGetID"); // 金币获取ID
        String memberPrivilegeID = request.getParameter("memberPrivilegeID"); // 会员特权ID
        String hospitalDiagnosisID = request.getParameter("hospitalDiagnosisID"); // 诊疗记录ID
        String buyCompanyID = request.getParameter("buyCompanyID"); // 订购公司ID
        String buyShopID = request.getParameter("buyShopID"); // 订购店铺
        String buyEmployeeID = request.getParameter("buyEmployeeID"); // 购买员工ID
        String memberID = request.getParameter("memberID"); // 购买会员ID
        String useEmployeeID = request.getParameter("useEmployeeID"); // 使用员工ID
        String useMemberID = request.getParameter("useMemberID"); // 使用会员ID
        String name = request.getParameter("name"); // 名称
        String financeType = request.getParameter("financeType"); // 财务类型
        String actionGoodsType = request.getParameter("actionGoodsType"); // 活动商品类型
        String outPayType = request.getParameter("outPayType"); // 外部支付方式
        String buyType = request.getParameter("buyType"); // 购买类型
        String orderType = request.getParameter("orderType"); // 订单类型
        String orderCode = request.getParameter("orderCode"); // 订单编号
        String orderSequnce = request.getParameter("orderSequnce"); // 订单流水号
        String applyTime = request.getParameter("applyTime"); // 申请时间
        String cancelTime = request.getParameter("cancelTime"); // 取消时间
        String cancelConfirmUserID = request.getParameter("cancelConfirmUserID"); // 确认取消人
        String cancelConformMemberID = request.getParameter("cancelConformMemberID"); // 确认取消会员ID
        String cancelConformEmployeeID = request.getParameter("cancelConformEmployeeID"); // 确认取消员工
        String cancelConfirmTime = request.getParameter("cancelConfirmTime"); // 确认取消时间
        String goodsNumbers = request.getParameter("goodsNumbers"); // 商品种类数
        String goodsQTY = request.getParameter("goodsQTY"); // 商品数量
        String priceTotal = request.getParameter("priceTotal"); // 商品总价格
        String weighTotal = request.getParameter("weighTotal"); // 总重量（克）
        String weightTotalFee = request.getParameter("weightTotalFee"); // 计费重量（克）
        String sendPrice = request.getParameter("sendPrice"); // 运费
        String subPrePay = request.getParameter("subPrePay"); // 减掉预付款
        String pointUsed = request.getParameter("pointUsed"); // 积分使用个数
        String pointSubAmount = request.getParameter("pointSubAmount"); // 积分抵扣金额
        String rebateSubAmount = request.getParameter("rebateSubAmount"); // 返利抵扣金额
        String bonusAmount = request.getParameter("bonusAmount"); // 抵扣券金额
        String weighPrice = request.getParameter("weighPrice"); // 称重浮动价格
        String discountAmount = request.getParameter("discountAmount"); // 折扣金额
        String priceAfterDiscount = request.getParameter("priceAfterDiscount"); // 优惠后总价格
        String tax = request.getParameter("tax"); // 税费
        String serviceFee = request.getParameter("serviceFee"); // 平台服务费
        String closedPriceTotal = request.getParameter("closedPriceTotal"); // 结算总价格
        String payTotal = request.getParameter("payTotal"); // 实际付款金额
        String incomeTotal = request.getParameter("incomeTotal"); // 订单收入金额
        String pointTotal = request.getParameter("pointTotal"); // 积分总价格
        String rebateTotal = request.getParameter("rebateTotal"); // 返利总价格
        String goldenTotal = request.getParameter("goldenTotal"); // 金币总价格
        String beanTotal = request.getParameter("beanTotal"); // 豆子总价格
        String presentPayAmount = request.getParameter("presentPayAmount"); // 礼品卡支付金额
        String presentRecordID = request.getParameter("presentRecordID"); // 礼品卡记录ID
        String payFrom = request.getParameter("payFrom"); // 付款方式
        String payTime = request.getParameter("payTime"); // 付款时间
        String payUserID = request.getParameter("payUserID"); // 付款用户ID
        String payEmployeeID = request.getParameter("payEmployeeID"); // 付款员工ID
        String payMemberID = request.getParameter("payMemberID"); // 付款会员ID
        String paywayID = request.getParameter("paywayID"); // 支付机构ID
        String paySequence = request.getParameter("paySequence"); // 付款流水号
        String memberPaymentID = request.getParameter("memberPaymentID"); // 会员支付ID
        String companyPaymentID = request.getParameter("companyPaymentID"); // 公司付款ID
        String payStatus = request.getParameter("payStatus"); // 付款状态
        String returnType = request.getParameter("returnType"); // 退款类型
        String returnTime = request.getParameter("returnTime"); // 退款时间
        String returnAmount = request.getParameter("returnAmount"); // 退款金额
        String returnPaywayID = request.getParameter("returnPaywayID"); // 退款方式
        String returnSequence = request.getParameter("returnSequence"); // 退款流水号
        String sendType = request.getParameter("sendType"); // 发货方式
        String sendUrgencyType = request.getParameter("sendUrgencyType"); // 送货紧急程度
        String memberPaywayID = request.getParameter("memberPaywayID"); // 会员钱包地址ID
        String memberAddressID = request.getParameter("memberAddressID"); // 会员地址ID
        String memberAddressName = request.getParameter("memberAddressName"); // 会员地址名称
        String contactName = request.getParameter("contactName"); // 联系人
        String phone = request.getParameter("phone"); // 联系电话
        String reservedDate = request.getParameter("reservedDate"); // 预约日期
        String getTime = request.getParameter("getTime"); // 提货时间
        String memberMemo = request.getParameter("memberMemo"); // 会员留言
        String memberInvoiceDefineID = request.getParameter("memberInvoiceDefineID"); // 会员发票ID
        String invoiceID = request.getParameter("invoiceID"); // 发票ID
        String invoiceCode = request.getParameter("invoiceCode"); // 发票号码
        String invoiceDate = request.getParameter("invoiceDate"); // 发票日期
        String invoiceStatus = request.getParameter("invoiceStatus"); // 发票状态
        String processUserID = request.getParameter("processUserID"); // 订单处理用户
        String processEmployeeID = request.getParameter("processEmployeeID"); // 订单处理员工ID
        String processMemberID = request.getParameter("processMemberID"); // 订单处理会员
        String processTime = request.getParameter("processTime"); // 订单处理时间
        String weighedUserID = request.getParameter("weighedUserID"); // 称重用户
        String weighedTime = request.getParameter("weighedTime"); // 称重时间
        String outUserID = request.getParameter("outUserID"); // 出库用户
        String outMemberID = request.getParameter("outMemberID"); // 出库会员ID
        String outEmployeeID = request.getParameter("outEmployeeID"); // 出库员工ID
        String outTime = request.getParameter("outTime"); // 出库时间
        String expressID = request.getParameter("expressID"); // 发货机构ID
        String sendUserID = request.getParameter("sendUserID"); // 发货用户
        String sendEmployeeID = request.getParameter("sendEmployeeID"); // 发货员工ID
        String sendoutMemberID = request.getParameter("sendoutMemberID"); // 发货会员ID
        String sendTime = request.getParameter("sendTime"); // 发货时间
        String sendCode = request.getParameter("sendCode"); // 发货单号码
        String sendPayType = request.getParameter("sendPayType"); // 货到付款方式
        String sendStatus = request.getParameter("sendStatus"); // 发货状态
        String sendPayTime = request.getParameter("sendPayTime"); // 货到付款时间
        String sendPayAmount = request.getParameter("sendPayAmount"); // 货到付款金额
        String sendCompanyID = request.getParameter("sendCompanyID"); // 配送机构ID
        String sendMemberID = request.getParameter("sendMemberID"); // 配送员ID
        String sendFee = request.getParameter("sendFee"); // 配送费
        String payINtime = request.getParameter("payINtime"); // 交款时间
        String payINType = request.getParameter("payINType"); // 交款方式
        String payINAmount = request.getParameter("payINAmount"); // 交款金额
        String payINuserID = request.getParameter("payINuserID"); // 收款人
        String prepay_id = request.getParameter("prepay_id"); // 微信预付款新ID
        String confirmPayTime = request.getParameter("confirmPayTime"); // 确认收款时间
        String confirmPayUserID = request.getParameter("confirmPayUserID"); // 确认收款用户ID
        String confirmPayEmployeeID = request.getParameter("confirmPayEmployeeID"); // 确认收款员工ID
        String confirmPayMemberID = request.getParameter("confirmPayMemberID"); // 确认收款会员ID
        String givePayFrom = request.getParameter("givePayFrom"); // 给卖家付款方式
        String givePayTime = request.getParameter("givePayTime"); // 给卖家付款时间
        String givePaywayID = request.getParameter("givePaywayID"); // 给卖家支付机构ID
        String givePaySequence = request.getParameter("givePaySequence"); // 给卖家付款流水号
        String tianmaoCode = request.getParameter("tianmaoCode"); // 天猫订单编码
        String taobaoCode = request.getParameter("taobaoCode"); // 淘宝订单编码
        String jingdongCode = request.getParameter("jingdongCode"); // 京东订单编码
        String deliveryNumber = request.getParameter("deliveryNumber"); // 送货单个数
        String deliveryLeftQTY = request.getParameter("deliveryLeftQTY"); // 未送货商品个数
        String getGoodsTime = request.getParameter("getGoodsTime"); // 签收时间
        String getGoodsUserID = request.getParameter("getGoodsUserID"); // 签收用户ID
        String getGoodsEmployeeID = request.getParameter("getGoodsEmployeeID"); // 签收员工ID
        String getGoodsMemberID = request.getParameter("getGoodsMemberID"); // 签收会员ID
        String bizOrderNo = request.getParameter("bizOrderNo"); // 业务订单编号
        String orderNo = request.getParameter("orderNo"); // 业务订单序号
        String returnBizOrderNo = request.getParameter("returnBizOrderNo"); // 业务退回订单编号
        String applyReturnTime = request.getParameter("applyReturnTime"); // 申请退货时间
        String returnGoodsTime = request.getParameter("returnGoodsTime"); // 同意退货时间
        String returnCode = request.getParameter("returnCode"); // 退货单号
        String outOrderCode = request.getParameter("outOrderCode"); // 外部订单编号
        String isMemberHide = request.getParameter("isMemberHide"); // 会员是否隐藏
        String status = request.getParameter("status"); // 订单状态

        if (StringUtils.isNotBlank(applicationID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("application", applicationID))) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID 不正确");
            }
            map.put("applicationID", applicationID);
        }
        if (StringUtils.isNotBlank(siteID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("site", siteID))) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID 不正确");
            }
            map.put("siteID", siteID);
        }
        if (StringUtils.isNotBlank(channelID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("channel", channelID))) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID 不正确");
            }
            map.put("channelID", channelID);
        }
        if (StringUtils.isNotBlank(cityID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("city", cityID))) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDNotFound, "cityID 不正确");
            }
            map.put("cityID", cityID);
        }
        if (StringUtils.isNotBlank(companyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID 不正确");
            }
            map.put("companyID", companyID);
        }
        if (StringUtils.isNotBlank(shopID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("shop", shopID))) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID 不正确");
            }
            map.put("shopID", shopID);
        }
        if (StringUtils.isNotBlank(propertyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("property", propertyID))) {
                return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "propertyID 不正确");
            }
            map.put("propertyID", propertyID);
        }
        if (StringUtils.isNotBlank(departmentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("department", departmentID))) {
                return MessagePacket.newFail(MessageHeader.Code.departmentIDNotFound, "departmentID 不正确");
            }
            map.put("departmentID", departmentID);
        }
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", distributorCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID 不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", recommendMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID 不正确");
            }
            map.put("recommendMemberID", recommendMemberID);
        }
        if (StringUtils.isNotBlank(salesMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", salesMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "salesMemberID 不正确");
            }
            map.put("salesMemberID", salesMemberID);
        }
        if (StringUtils.isNotBlank(salesEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", salesEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "salesEmployeeID 不正确");
            }
            map.put("salesEmployeeID", salesEmployeeID);
        }
        if (StringUtils.isNotBlank(roomID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("room", roomID))) {
                return MessagePacket.newFail(MessageHeader.Code.roomIDNotFound, "roomID 不正确");
            }
            map.put("roomID", roomID);
        }
        if (StringUtils.isNotBlank(tableNumber)) {
            map.put("tableNumber", tableNumber);
        }
        if (StringUtils.isNotBlank(houseID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("house", houseID))) {
                return MessagePacket.newFail(MessageHeader.Code.houseIDNotFound, "houseID 不正确");
            }
            map.put("houseID", houseID);
        }
        if (StringUtils.isNotBlank(officeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("office", officeID))) {
                return MessagePacket.newFail(MessageHeader.Code.officeIDNotFound, "officeID 不正确");
            }
            map.put("officeID", officeID);
        }
        if (StringUtils.isNotBlank(machineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("machine", machineID))) {
                return MessagePacket.newFail(MessageHeader.Code.machineIDNotFound, "machineID 不正确");
            }
            map.put("machineID", machineID);
        }
        if (StringUtils.isNotBlank(orderTemplateID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("orderTemplate", orderTemplateID))) {
                return MessagePacket.newFail(MessageHeader.Code.orderTemplateIDNotFound, "orderTemplateID 不正确");
            }
            map.put("orderTemplateID", orderTemplateID);
        }
        if (StringUtils.isNotBlank(cardDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("cardDefine", cardDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.cardDefineIDNotFound, "cardDefineID 不正确");
            }
            map.put("cardDefineID", cardDefineID);
        }
        if (StringUtils.isNotBlank(actionID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("action", actionID))) {
                return MessagePacket.newFail(MessageHeader.Code.actionIDNotFound, "actionID 不正确");
            }
            map.put("actionID", actionID);
        }
        if (StringUtils.isNotBlank(constructionProjectID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("constructionProject", constructionProjectID))) {
                return MessagePacket.newFail(MessageHeader.Code.constructionProjectIDNotFound, "constructionProjectID 不正确");
            }
            map.put("constructionProjectID", constructionProjectID);
        }
        if (StringUtils.isNotBlank(constructionID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("construction", constructionID))) {
                return MessagePacket.newFail(MessageHeader.Code.constructionIDNotFound, "constructionID 不正确");
            }
            map.put("constructionID", constructionID);
        }
        if (StringUtils.isNotBlank(sceneID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("scene", sceneID))) {
                return MessagePacket.newFail(MessageHeader.Code.sceneIDNotFound, "sceneID 不正确");
            }
            map.put("sceneID", sceneID);
        }
        if (StringUtils.isNotBlank(tenderID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("tender", tenderID))) {
                return MessagePacket.newFail(MessageHeader.Code.tenderIDNotFound, "tenderID 不正确");
            }
            map.put("tenderID", tenderID);
        }
        if (StringUtils.isNotBlank(goodsTogetherDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogetherDefine", goodsTogetherDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsTogetherDefineIDNotFound, "goodsTogetherDefineID 不正确");
            }
            map.put("goodsTogetherDefineID", goodsTogetherDefineID);
        }
        if (StringUtils.isNotBlank(goodsTogetherID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogether", goodsTogetherID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsTogetherIDNotFound, "goodsTogetherID 不正确");
            }
            map.put("goodsTogetherID", goodsTogetherID);
        }
        if (StringUtils.isNotBlank(goodsTogetherJoinID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsTogetherJoin", goodsTogetherJoinID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsTogetherJoinIDNotFound, "goodsTogetherJoinID 不正确");
            }
            map.put("goodsTogetherJoinID", goodsTogetherJoinID);
        }
        if (StringUtils.isNotBlank(goodsSecondDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsSecondDefine", goodsSecondDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsSecondDefineIDNotFound, "goodsSecondDefineID 不正确");
            }
            map.put("goodsSecondDefineID", goodsSecondDefineID);
        }
        if (StringUtils.isNotBlank(goodsSecondID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsSecond", goodsSecondID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsSecondIDNotFound, "goodsSecondID 不正确");
            }
            map.put("goodsSecondID", goodsSecondID);
        }
        if (StringUtils.isNotBlank(goodsPriceDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsPriceDefine", goodsPriceDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceDefineID 不正确");
            }
            map.put("goodsPriceDefineID", goodsPriceDefineID);
        }
        if (StringUtils.isNotBlank(goodsPriceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goodsPrice", goodsPriceID))) {
                return MessagePacket.newFail(MessageHeader.Code.goodsPriceIDNotFound, "goodsPriceID 不正确");
            }
            map.put("goodsPriceID", goodsPriceID);
        }
        if (StringUtils.isNotBlank(trainBusinessID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainBusiness", trainBusinessID))) {
                return MessagePacket.newFail(MessageHeader.Code.trainBusinessIDNotFound, "trainBusinessID 不正确");
            }
            map.put("trainBusinessID", trainBusinessID);
        }
        if (StringUtils.isNotBlank(trainCourseID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainCourse", trainCourseID))) {
                return MessagePacket.newFail(MessageHeader.Code.trainCourseIDNotFound, "trainCourseID 不正确");
            }
            map.put("trainCourseID", trainCourseID);
        }
        if (StringUtils.isNotBlank(trainHourID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("trainHour", trainHourID))) {
                return MessagePacket.newFail(MessageHeader.Code.trainHourIDNotFound, "trainHourID 不正确");
            }
            map.put("trainHourID", trainHourID);
        }
        if (StringUtils.isNotBlank(memberTrainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberTrain", memberTrainID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberTrainIDNotFound, "memberTrainID 不正确");
            }
            map.put("memberTrainID", memberTrainID);
        }
        if (StringUtils.isNotBlank(memberBuyReadID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberBuyRead", memberBuyReadID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberBuyReadIDNotFound, "memberBuyReadID 不正确");
            }
            map.put("memberBuyReadID", memberBuyReadID);
        }
        if (StringUtils.isNotBlank(majorID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("major", majorID))) {
                return MessagePacket.newFail(MessageHeader.Code.majorIDNotFound, "majorID 不正确");
            }
            map.put("majorID", majorID);
        }
        if (StringUtils.isNotBlank(memberMajorID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberMajor", memberMajorID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberMajorID 不正确");
            }
            map.put("memberMajorID", memberMajorID);
        }
        if (StringUtils.isNotBlank(goldenGetID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("goldenGet", goldenGetID))) {
                return MessagePacket.newFail(MessageHeader.Code.goldenGetIDNotFound, "goldenGetID 不正确");
            }
            map.put("goldenGetID", goldenGetID);
        }
        if (StringUtils.isNotBlank(memberPrivilegeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPrivilege", memberPrivilegeID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPrivilegeIDNotFound, "memberPrivilegeID 不正确");
            }
            map.put("memberPrivilegeID", memberPrivilegeID);
        }
        if (StringUtils.isNotBlank(hospitalDiagnosisID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("hospitalDiagnosis", hospitalDiagnosisID))) {
                return MessagePacket.newFail(MessageHeader.Code.hospitalDiagnosisIDNotFound, "hospitalDiagnosisID 不正确");
            }
            map.put("hospitalDiagnosisID", hospitalDiagnosisID);
        }
        if (StringUtils.isNotBlank(buyCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", buyCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "buyCompanyID 不正确");
            }
            map.put("buyCompanyID", buyCompanyID);
        }
        if (StringUtils.isNotBlank(buyShopID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("shop", buyShopID))) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "buyShopID 不正确");
            }
            map.put("buyShopID", buyShopID);
        }
        if (StringUtils.isNotBlank(buyEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", buyEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "buyEmployeeID 不正确");
            }
            map.put("buyEmployeeID", buyEmployeeID);
        }
        if (StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID 不正确");
            }
            map.put("memberID", memberID);
        }
        if (StringUtils.isNotBlank(useEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", useEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "useEmployeeID 不正确");
            }
            map.put("useEmployeeID", useEmployeeID);
        }
        if (StringUtils.isNotBlank(useMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", useMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "useMemberID 不正确");
            }
            map.put("useMemberID", useMemberID);
        }
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
        }
        if (StringUtils.isNotBlank(financeType)) {
            if (!NumberUtils.isNumeric(financeType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "financeType 请输入数字");
            }
            map.put("financeType", financeType);
        }
        if (StringUtils.isNotBlank(actionGoodsType)) {
            if (!NumberUtils.isNumeric(actionGoodsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "actionGoodsType 请输入数字");
            }
            map.put("actionGoodsType", actionGoodsType);
        }
        if (StringUtils.isNotBlank(outPayType)) {
            if (!NumberUtils.isNumeric(outPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "outPayType 请输入数字");
            }
            map.put("outPayType", outPayType);
        }
        if (StringUtils.isNotBlank(buyType)) {
            if (!NumberUtils.isNumeric(buyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "buyType 请输入数字");
            }
            map.put("buyType", buyType);
        }
        if (StringUtils.isNotBlank(orderType)) {
            if (!NumberUtils.isNumeric(orderType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "orderType 请输入数字");
            }
            map.put("orderType", orderType);
        }
        if (StringUtils.isNotBlank(orderCode)) {
            map.put("orderCode", orderCode);
        }
        if (StringUtils.isNotBlank(orderSequnce)) {
            map.put("orderSequnce", orderSequnce);
        }
        if (StringUtils.isNotBlank(applyTime)) {
            if (!TimeTest.dateIsPass(applyTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "applyTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("applyTime", applyTime);
        }
        if (StringUtils.isNotBlank(cancelTime)) {
            if (!TimeTest.dateIsPass(cancelTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "cancelTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("cancelTime", cancelTime);
        }
        if (StringUtils.isNotBlank(cancelConfirmUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", cancelConfirmUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "cancelConfirmUserID 不正确");
            }
            map.put("cancelConfirmUserID", cancelConfirmUserID);
        }
        if (StringUtils.isNotBlank(cancelConformMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", cancelConformMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "cancelConformMemberID 不正确");
            }
            map.put("cancelConformMemberID", cancelConformMemberID);
        }
        if (StringUtils.isNotBlank(cancelConformEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", cancelConformEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "cancelConformEmployeeID 不正确");
            }
            map.put("cancelConformEmployeeID", cancelConformEmployeeID);
        }
        if (StringUtils.isNotBlank(cancelConfirmTime)) {
            if (!TimeTest.dateIsPass(cancelConfirmTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "cancelConfirmTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("cancelConfirmTime", cancelConfirmTime);
        }
        if (StringUtils.isNotBlank(goodsNumbers)) {
            if (!NumberUtils.isNumeric(goodsNumbers)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "goodsNumbers 请输入数字");
            }
            map.put("goodsNumbers", goodsNumbers);
        }
        if (StringUtils.isNotBlank(goodsQTY)) {
            if (!NumberUtils.isNumeric(goodsQTY)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "goodsQTY 请输入数字");
            }
            map.put("goodsQTY", goodsQTY);
        }
        if (StringUtils.isNotBlank(priceTotal)) {
            map.put("priceTotal", priceTotal);
        }
        if (StringUtils.isNotBlank(weighTotal)) {
            if (!NumberUtils.isNumeric(weighTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "weighTotal 请输入数字");
            }
            map.put("weighTotal", weighTotal);
        }
        if (StringUtils.isNotBlank(weightTotalFee)) {
            if (!NumberUtils.isNumeric(weightTotalFee)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "weightTotalFee 请输入数字");
            }
            map.put("weightTotalFee", weightTotalFee);
        }
        if (StringUtils.isNotBlank(sendPrice)) {
            map.put("sendPrice", sendPrice);
        }
        if (StringUtils.isNotBlank(subPrePay)) {
            map.put("subPrePay", subPrePay);
        }
        if (StringUtils.isNotBlank(pointUsed)) {
            if (!NumberUtils.isNumeric(pointUsed)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "pointUsed 请输入数字");
            }
            map.put("pointUsed", pointUsed);
        }
        if (StringUtils.isNotBlank(pointSubAmount)) {
            map.put("pointSubAmount", pointSubAmount);
        }
        if (StringUtils.isNotBlank(rebateSubAmount)) {
            map.put("rebateSubAmount", rebateSubAmount);
        }
        if (StringUtils.isNotBlank(bonusAmount)) {
            map.put("bonusAmount", bonusAmount);
        }
        if (StringUtils.isNotBlank(weighPrice)) {
            map.put("weighPrice", weighPrice);
        }
        if (StringUtils.isNotBlank(discountAmount)) {
            map.put("discountAmount", discountAmount);
        }
        if (StringUtils.isNotBlank(priceAfterDiscount)) {
            map.put("priceAfterDiscount", priceAfterDiscount);
        }
        if (StringUtils.isNotBlank(tax)) {
            map.put("tax", tax);
        }
        if (StringUtils.isNotBlank(serviceFee)) {
            map.put("serviceFee", serviceFee);
        }
        if (StringUtils.isNotBlank(closedPriceTotal)) {
            map.put("closedPriceTotal", closedPriceTotal);
        }
        if (StringUtils.isNotBlank(payTotal)) {
            map.put("payTotal", payTotal);
        }
        if (StringUtils.isNotBlank(incomeTotal)) {
            map.put("incomeTotal", incomeTotal);
        }
        if (StringUtils.isNotBlank(pointTotal)) {
            if (!NumberUtils.isNumeric(pointTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "pointTotal 请输入数字");
            }
            map.put("pointTotal", pointTotal);
        }
        if (StringUtils.isNotBlank(rebateTotal)) {
            map.put("rebateTotal", rebateTotal);
        }
        if (StringUtils.isNotBlank(goldenTotal)) {
            if (!NumberUtils.isNumeric(goldenTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "goldenTotal 请输入数字");
            }
            map.put("goldenTotal", goldenTotal);
        }
        if (StringUtils.isNotBlank(beanTotal)) {
            if (!NumberUtils.isNumeric(beanTotal)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "beanTotal 请输入数字");
            }
            map.put("beanTotal", beanTotal);
        }
        if (StringUtils.isNotBlank(presentPayAmount)) {
            map.put("presentPayAmount", presentPayAmount);
        }
        if (StringUtils.isNotBlank(presentRecordID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("presentRecord", presentRecordID))) {
                return MessagePacket.newFail(MessageHeader.Code.presentRecordIDNotFound, "presentRecordID 不正确");
            }
            map.put("presentRecordID", presentRecordID);
        }
        if (StringUtils.isNotBlank(payFrom)) {
            if (!NumberUtils.isNumeric(payFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payFrom 请输入数字");
            }
            map.put("payFrom", payFrom);
        }
        if (StringUtils.isNotBlank(payTime)) {
            if (!TimeTest.dateIsPass(payTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "payTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("payTime", payTime);
        }
        if (StringUtils.isNotBlank(payUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", payUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "payUserID 不正确");
            }
            map.put("payUserID", payUserID);
        }
        if (StringUtils.isNotBlank(payEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", payEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "payEmployeeID 不正确");
            }
            map.put("payEmployeeID", payEmployeeID);
        }
        if (StringUtils.isNotBlank(payMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", payMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "payMemberID 不正确");
            }
            map.put("payMemberID", payMemberID);
        }
        if (StringUtils.isNotBlank(paywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", paywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDNotFound, "paywayID 不正确");
            }
            map.put("paywayID", paywayID);
        }
        if (StringUtils.isNotBlank(paySequence)) {
            map.put("paySequence", paySequence);
        }
        if (StringUtils.isNotBlank(memberPaymentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPayment", memberPaymentID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPaymentIDNotFound, "memberPaymentID 不正确");
            }
            map.put("memberPaymentID", memberPaymentID);
        }
        if (StringUtils.isNotBlank(companyPaymentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("companyPayment", companyPaymentID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyPaymentIDNotFound, "companyPaymentID 不正确");
            }
            map.put("companyPaymentID", companyPaymentID);
        }
        if (StringUtils.isNotBlank(payStatus)) {
            if (!NumberUtils.isNumeric(payStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payStatus 请输入数字");
            }
            map.put("payStatus", payStatus);
        }
        if (StringUtils.isNotBlank(returnType)) {
            if (!NumberUtils.isNumeric(returnType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "returnType 请输入数字");
            }
            map.put("returnType", returnType);
        }
        if (StringUtils.isNotBlank(returnTime)) {
            if (!TimeTest.dateIsPass(returnTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "returnTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("returnTime", returnTime);
        }
        if (StringUtils.isNotBlank(returnAmount)) {
            map.put("returnAmount", returnAmount);
        }
        if (StringUtils.isNotBlank(returnPaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", returnPaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDNotFound, "returnPaywayID 不正确");
            }
            map.put("returnPaywayID", returnPaywayID);
        }
        if (StringUtils.isNotBlank(returnSequence)) {
            map.put("returnSequence", returnSequence);
        }
        if (StringUtils.isNotBlank(sendType)) {
            if (!NumberUtils.isNumeric(sendType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendType 请输入数字");
            }
            map.put("sendType", sendType);
        }
        if (StringUtils.isNotBlank(sendUrgencyType)) {
            if (!NumberUtils.isNumeric(sendUrgencyType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendUrgencyType 请输入数字");
            }
            map.put("sendUrgencyType", sendUrgencyType);
        }
        if (StringUtils.isNotBlank(memberPaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberPayway", memberPaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberPaywayIDNotFound, "memberPaywayID 不正确");
            }
            map.put("memberPaywayID", memberPaywayID);
        }
        if (StringUtils.isNotBlank(memberAddressID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberAddress", memberAddressID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberAddressIDNotFound, "memberAddressID 不正确");
            }
            map.put("memberAddressID", memberAddressID);
        }
        if (StringUtils.isNotBlank(memberAddressName)) {
            map.put("memberAddressName", memberAddressName);
        }
        if (StringUtils.isNotBlank(contactName)) {
            map.put("contactName", contactName);
        }
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }
        if (StringUtils.isNotBlank(reservedDate)) {
            if (!TimeTest.dateIsPass(reservedDate)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "reservedDate 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("reservedDate", reservedDate);
        }
        if (StringUtils.isNotBlank(getTime)) {
            map.put("getTime", getTime);
        }
        if (StringUtils.isNotBlank(memberMemo)) {
            map.put("memberMemo", memberMemo);
        }
        if (StringUtils.isNotBlank(memberInvoiceDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("memberInvoiceDefine", memberInvoiceDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberInvoiceDefineIDNotFound, "memberInvoiceDefineID 不正确");
            }
            map.put("memberInvoiceDefineID", memberInvoiceDefineID);
        }
        if (StringUtils.isNotBlank(invoiceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("invoice", invoiceID))) {
                return MessagePacket.newFail(MessageHeader.Code.invoiceIDNotFound, "invoiceID 不正确");
            }
            map.put("invoiceID", invoiceID);
        }
        if (StringUtils.isNotBlank(invoiceCode)) {
            map.put("invoiceCode", invoiceCode);
        }
        if (StringUtils.isNotBlank(invoiceDate)) {
            if (!TimeTest.dateIsPass(invoiceDate)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "invoiceDate 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("invoiceDate", invoiceDate);
        }
        if (StringUtils.isNotBlank(invoiceStatus)) {
            if (!NumberUtils.isNumeric(invoiceStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "invoiceStatus 请输入数字");
            }
            map.put("invoiceStatus", invoiceStatus);
        }
        if (StringUtils.isNotBlank(processUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", processUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "processUserID 不正确");
            }
            map.put("processUserID", processUserID);
        }
        if (StringUtils.isNotBlank(processEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", processEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "processEmployeeID 不正确");
            }
            map.put("processEmployeeID", processEmployeeID);
        }
        if (StringUtils.isNotBlank(processMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", processMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "processMemberID 不正确");
            }
            map.put("processMemberID", processMemberID);
        }
        if (StringUtils.isNotBlank(processTime)) {
            if (!TimeTest.dateIsPass(processTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "processTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("processTime", processTime);
        }
        if (StringUtils.isNotBlank(weighedUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("weighedUseruser", weighedUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "weighedUserID 不正确");
            }
            map.put("weighedUserID", weighedUserID);
        }
        if (StringUtils.isNotBlank(weighedTime)) {
            if (!TimeTest.dateIsPass(weighedTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "weighedTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("weighedTime", weighedTime);
        }
        if (StringUtils.isNotBlank(outUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", outUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.outUserIDNotFound, "outUserID 不正确");
            }
            map.put("outUserID", outUserID);
        }
        if (StringUtils.isNotBlank(outMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", outMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "outMemberID 不正确");
            }
            map.put("outMemberID", outMemberID);
        }
        if (StringUtils.isNotBlank(outEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", outEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.outEmployeeIDNotFound, "outEmployeeID 不正确");
            }
            map.put("outEmployeeID", outEmployeeID);
        }
        if (StringUtils.isNotBlank(outTime)) {
            if (!TimeTest.dateIsPass(outTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "outTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("outTime", outTime);
        }
        if (StringUtils.isNotBlank(expressID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("express", expressID))) {
                return MessagePacket.newFail(MessageHeader.Code.expressIDNotFound, "expressID 不正确");
            }
            map.put("expressID", expressID);
        }
        if (StringUtils.isNotBlank(sendUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", sendUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "sendUserID 不正确");
            }
            map.put("sendUserID", sendUserID);
        }
        if (StringUtils.isNotBlank(sendEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", sendEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "sendEmployeeID 不正确");
            }
            map.put("sendEmployeeID", sendEmployeeID);
        }
        if (StringUtils.isNotBlank(sendoutMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", sendoutMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "sendoutMemberID 不正确");
            }
            map.put("sendoutMemberID", sendoutMemberID);
        }
        if (StringUtils.isNotBlank(sendTime)) {
            if (!TimeTest.dateIsPass(sendTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "sendTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("sendTime", sendTime);
        }
        if (StringUtils.isNotBlank(sendCode)) {
            map.put("sendCode", sendCode);
        }
        if (StringUtils.isNotBlank(sendPayType)) {
            if (!NumberUtils.isNumeric(sendPayType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendPayType 请输入数字");
            }
            map.put("sendPayType", sendPayType);
        }
        if (StringUtils.isNotBlank(sendStatus)) {
            if (!NumberUtils.isNumeric(sendStatus)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sendStatus 请输入数字");
            }
            map.put("sendStatus", sendStatus);
        }
        if (StringUtils.isNotBlank(sendPayTime)) {
            if (!TimeTest.dateIsPass(sendPayTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "sendPayTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("sendPayTime", sendPayTime);
        }
        if (StringUtils.isNotBlank(sendPayAmount)) {
            map.put("sendPayAmount", sendPayAmount);
        }
        if (StringUtils.isNotBlank(sendCompanyID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("company", sendCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "sendCompanyID 不正确");
            }
            map.put("sendCompanyID", sendCompanyID);
        }
        if (StringUtils.isNotBlank(sendMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", sendMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "sendMemberID 不正确");
            }
            map.put("sendMemberID", sendMemberID);
        }
        if (StringUtils.isNotBlank(sendFee)) {
            map.put("sendFee", sendFee);
        }
        if (StringUtils.isNotBlank(payINtime)) {
            if (!TimeTest.dateIsPass(payINtime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "payINtime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("payINtime", payINtime);
        }
        if (StringUtils.isNotBlank(payINType)) {
            if (!NumberUtils.isNumeric(payINType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "payINType 请输入数字");
            }
            map.put("payINType", payINType);
        }
        if (StringUtils.isNotBlank(payINAmount)) {
            map.put("payINAmount", payINAmount);
        }
        if (StringUtils.isNotBlank(payINuserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", payINuserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "payINuserID 不正确");
            }
            map.put("payINuserID", payINuserID);
        }
        if (StringUtils.isNotBlank(prepay_id)) {
            map.put("prepay_id", prepay_id);
        }
        if (StringUtils.isNotBlank(confirmPayTime)) {
            if (!TimeTest.dateIsPass(confirmPayTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "confirmPayTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("confirmPayTime", confirmPayTime);
        }
        if (StringUtils.isNotBlank(confirmPayUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", confirmPayUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "confirmPayUserID 不正确");
            }
            map.put("confirmPayUserID", confirmPayUserID);
        }
        if (StringUtils.isNotBlank(confirmPayEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", confirmPayEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "confirmPayEmployeeID 不正确");
            }
            map.put("confirmPayEmployeeID", confirmPayEmployeeID);
        }
        if (StringUtils.isNotBlank(confirmPayMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", confirmPayMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "confirmPayMemberID 不正确");
            }
            map.put("confirmPayMemberID", confirmPayMemberID);
        }
        if (StringUtils.isNotBlank(givePayFrom)) {
            if (!NumberUtils.isNumeric(givePayFrom)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "givePayFrom 请输入数字");
            }
            map.put("givePayFrom", givePayFrom);
        }
        if (StringUtils.isNotBlank(givePayTime)) {
            if (!TimeTest.dateIsPass(givePayTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "givePayTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("givePayTime", givePayTime);
        }
        if (StringUtils.isNotBlank(givePaywayID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("payway", givePaywayID))) {
                return MessagePacket.newFail(MessageHeader.Code.paywayIDNotFound, "givePaywayID 不正确");
            }
            map.put("givePaywayID", givePaywayID);
        }
        if (StringUtils.isNotBlank(givePaySequence)) {
            map.put("givePaySequence", givePaySequence);
        }
        if (StringUtils.isNotBlank(tianmaoCode)) {
            map.put("tianmaoCode", tianmaoCode);
        }
        if (StringUtils.isNotBlank(taobaoCode)) {
            map.put("taobaoCode", taobaoCode);
        }
        if (StringUtils.isNotBlank(jingdongCode)) {
            map.put("jingdongCode", jingdongCode);
        }
        if (StringUtils.isNotBlank(deliveryNumber)) {
            if (!NumberUtils.isNumeric(deliveryNumber)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "deliveryNumber 请输入数字");
            }
            map.put("deliveryNumber", deliveryNumber);
        }
        if (StringUtils.isNotBlank(deliveryLeftQTY)) {
            map.put("deliveryLeftQTY", deliveryLeftQTY);
        }
        if (StringUtils.isNotBlank(getGoodsTime)) {
            if (!TimeTest.dateIsPass(getGoodsTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "getGoodsTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("getGoodsTime", getGoodsTime);
        }
        if (StringUtils.isNotBlank(getGoodsUserID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("user", getGoodsUserID))) {
                return MessagePacket.newFail(MessageHeader.Code.userIDNotFound, "getGoodsUserID 不正确");
            }
            map.put("getGoodsUserID", getGoodsUserID);
        }
        if (StringUtils.isNotBlank(getGoodsEmployeeID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("employee", getGoodsEmployeeID))) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "getGoodsEmployeeID 不正确");
            }
            map.put("getGoodsEmployeeID", getGoodsEmployeeID);
        }
        if (StringUtils.isNotBlank(getGoodsMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", getGoodsMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "getGoodsMemberID 不正确");
            }
            map.put("getGoodsMemberID", getGoodsMemberID);
        }
        if (StringUtils.isNotBlank(bizOrderNo)) {
            map.put("bizOrderNo", bizOrderNo);
        }
        if (StringUtils.isNotBlank(orderNo)) {
            map.put("orderNo", orderNo);
        }
        if (StringUtils.isNotBlank(returnBizOrderNo)) {
            map.put("returnBizOrderNo", returnBizOrderNo);
        }
        if (StringUtils.isNotBlank(applyReturnTime)) {
            if (!TimeTest.dateIsPass(applyReturnTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "applyReturnTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("applyReturnTime", applyReturnTime);
        }
        if (StringUtils.isNotBlank(returnGoodsTime)) {
            if (!TimeTest.dateIsPass(returnGoodsTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "returnGoodsTime 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("returnGoodsTime", returnGoodsTime);
        }
        if (StringUtils.isNotBlank(returnCode)) {
            map.put("returnCode", returnCode);
        }
        if (StringUtils.isNotBlank(outOrderCode)) {
            map.put("outOrderCode", outOrderCode);
        }
        if (StringUtils.isNotBlank(isMemberHide)) {
            if (!NumberUtils.isNumeric(isMemberHide)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isMemberHide 请输入数字");
            }
            map.put("isMemberHide", isMemberHide);
        }
        if (StringUtils.isNotBlank(status)) {
            if (!NumberUtils.isNumeric(status)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "status 请输入数字");
            }
            map.put("status", status);
        }


        map.put("modifier", user.getId());
        memberOrderService.updateByMap(map);
        if (opLog != null) {
            String descriptions2 = String.format("外部系统修改一个会员订单");
            sendMessageService.sendDataLogMessage(user, descriptions2, "outSystemUpdateOneMemberOrder",
                    DataLog.objectDefineID.timeIntervalInstance.getOname(), null,
                    null, DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemUpdateOneMemberOrder success");
    }

    /**
     * 比赛微信支付回调
     *
     * @param request
     * @return
     */
//    public String wxPayRefundRequest(String transaction_id, String out_refund_no,
//                                     int total_fee, int refund_fee, String op_user_id) {
//        CloseableHttpClient httpclient = null;
//        CloseableHttpResponse response = null;
//        String strResponse = null;
//        try {
//            httpclient = ClientCustomSSL.getCloseableHttpClient();
//            // 构造HTTP请求
//            HttpPost httpPost = new HttpPost(Configure.PAY_REFUND_API);
//            // PayRefundReqData wxdata = new PayRefundReqData(
//            // "1004720096201602263541023415", "16371", 30, 30, "19417");
//            PayRefundReqData wxdata = new PayRefundReqData(transaction_id,
//                    out_refund_no, total_fee, refund_fee, op_user_id);
//            String requestStr = Util.ConvertObj2Xml(wxdata);
//            StringEntity se = new StringEntity(requestStr.toString());
//            httpPost.setEntity(se);
//            // 发送请求
//            response = httpclient.execute(httpPost);
//
//            HttpEntity entity = response.getEntity();
//            if (entity != null) {
//                SAXReader saxReader = new SAXReader();
//                Document document = saxReader.read(entity.getContent());
//                Element rootElt = document.getRootElement();
//                // 结果码
//                String returnCode = rootElt.elementText("return_code");
//                String resultCode = rootElt.elementText("result_code");
//                if ("SUCCESS".equals(returnCode)&&"SUCCESS".equals(resultCode)) {
//                    strResponse=returnCode;
//                }else {
//                    strResponse=rootElt.elementText("err_code_des");
//                }
//            }
//            EntityUtils.consume(entity);
//        } catch (Exception e) {
//            Logger.getLogger(getClass()).error("payRefundRequest", e);
//        } finally {
//            try {
//                response.close();
//                httpclient.close();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                Logger.getLogger(getClass()).error("payRefundRequest关闭异常:", e);
//            }
//        }
//        return strResponse;
//    }
//    @ApiOperation(value = "微信支付退款回调", notes = "微信支付退款回调")
//    @RequestMapping("/notifyUrl")
//    public Object GameWeiXinPayMemberOrderNotify(HttpServletRequest request) throws IOException, GeneralSecurityException {
//        String ciphertext ="";
//        String nonce = "";
//        String associated_data = "";
//        String apiV3key = "";
//        apiV3key=request.getParameter("apiV3key");
//        ciphertext=request.getParameter("ciphertext");
//        nonce=request.getParameter("nonce");
//        associated_data=request.getParameter("associated_data");
//        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<weixin-notifyUrl-notityXml>>>>>>>>>>>>>>>>>>>>>>>>>：" + ciphertext);
//        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<weixin-apiV3key-notityXml>>>>>>>>>>>>>>>>>>>>>>>>>：" + apiV3key);
//        String resultStr = decryptToString(associated_data.getBytes("UTF-8"), nonce.getBytes("UTF-8"), ciphertext,apiV3key);
//        Map<String, String> reqInfo = com.alibaba.fastjson.JSONObject.parseObject(resultStr, Map.class);
//
//        String refund_status = reqInfo.get("refund_status");//退款状态
//        String out_trade_no = reqInfo.get("out_trade_no"); //订单号
//
//        Map<String, Object> parm = new HashMap<>();
//        if (!StringUtils.isEmpty(refund_status) && "SUCCESS".equals(refund_status))  {
//
//            //你自己的业务
//
//            parm.put("code", "SUCCESS");
//            parm.put("message", "成功");
//        } else {
//            parm.put("code", "FAIL");
//            parm.put("message", "失败");
//            throw new RuntimeException("退款失败");
//        }
//        return parm;  //返回给前端的参数
//    }
//    //退款回调  解密数据
//    public String decryptToString(byte[] associatedData, byte[] nonce, String ciphertext,String apiV3key) throws GeneralSecurityException, IOException {
//        try {
//            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
////            String apiV3key = "";
////            apiV3key=
//            SecretKeySpec key = new SecretKeySpec(apiV3key.getBytes(), "AES");//todo 这里的apiV3key是你的商户APIV3密钥
//            GCMParameterSpec spec = new GCMParameterSpec(128, nonce);//规定为128
//
//            cipher.init(Cipher.DECRYPT_MODE, key, spec);
//            cipher.updateAAD(associatedData);
//
//            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), "utf-8");
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//            throw new IllegalStateException(e);
//        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
//            throw new IllegalArgumentException(e);
//        }
//    }

    @Autowired
    private CartGoodsService cartGoodsService;
    @Autowired
    private PointService pointService;
    @Autowired
    private MemberRebateService memberRebateService;
    @Autowired
    private ExpressService expressService;
    @Autowired
    private CompanyBankService companyBankService;
    @Autowired
    private CompanyPaymentService companyPaymentService;
    @Autowired
    private CompanyPaywayService companyPaywayService;
    @Autowired
    private CityService cityService;
    @Autowired
    private ChannelService channelService;
    @Autowired
    private GoodsShopService goodsShopService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private GoldenGetService goldenGetService;
    @Autowired
    private StoryService storyService;
    @Autowired
    private MemberPaymentService memberPaymentService;
    @Autowired
    private CartService cartService;
    @Autowired
    private MemberTrainService memberTrainService;
    @Autowired
    private MemberMajorService memberMajorService;
    @Autowired
    private OrderTemplateService orderTemplateService;
    @Autowired
    private MemberPrivilegeService memberPrivilegeService;
    @Autowired
    private ActionService actionService;
    @Autowired
    private GoodsSecondService goodsSecondService;
    @Autowired
    private TrainBusinessService trainBusinessService;
    @Autowired
    private TrainCourseService trainCourseService;
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private GoodsCompanyService goodsCompanyService;
    @Autowired
    private MemberBuyReadService memberBuyReadService;
    @Autowired
    private UserService userService;
    @Autowired
    private BaseService baseService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private PeopleService peopleService;
    @Autowired
    private AppointmentService appointmentService;
    @Resource
    private ComputerUnitService computerUnitService;
    @Resource
    private DepositSaveService depositSaveService;
    @Resource
    private GoodsPriceService goodsPriceService;
    @Resource
    private GoodsPriceMemberService goodsPriceMemberService;
    @Resource
    private PresentDefineService presentDefineService;
    @Resource
    private PresentRecordService presentRecordService;
    @Resource
    private MemberActionService memberActionService;
    @Autowired
    private BrowseService browseService;


}
