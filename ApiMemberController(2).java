package com.kingpivot.api.controller.ApiMemberController;

import cn.jpush.api.JPushClient;
import cn.jpush.api.common.resp.APIConnectionException;
import cn.jpush.api.common.resp.APIRequestException;
import cn.jpush.api.common.resp.DefaultResult;
import cn.jpush.api.device.TagAliasResult;
import com.alipay.api.internal.util.codec.Base64;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2UserGetbymobileRequest;
import com.dingtalk.api.response.OapiV2UserGetbymobileResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.kingpivot.api.dto.application.Application;
import com.kingpivot.api.dto.authCode.AuthCodeDto;
import com.kingpivot.api.dto.badgeDefine.BadgeDefineDto;
import com.kingpivot.api.dto.channel.OutChannelDto;
import com.kingpivot.api.dto.city.CityDto;
import com.kingpivot.api.dto.creditRank.CreditRankDto;
import com.kingpivot.api.dto.device.ApiDeviceDetailDto;
import com.kingpivot.api.dto.employee.EmployeeDetailDto;
import com.kingpivot.api.dto.friend.Friend;
import com.kingpivot.api.dto.friend.FriendDetail;
import com.kingpivot.api.dto.machine.MachineDetail;
import com.kingpivot.api.dto.member.*;
import com.kingpivot.api.dto.memberBuyRead.MemberBuyReadListDto;
import com.kingpivot.api.dto.memberDevice.ApiMemberDeviceList;
import com.kingpivot.api.dto.memberExpenditure.MemberExpenditureDto;
import com.kingpivot.api.dto.memberFingerPrint.ApiMemberFingerPrintDetail;
import com.kingpivot.api.dto.memberIncome.MemberIncomeDto;
import com.kingpivot.api.dto.memberMajor.ApiMemberMajorDetailDto;
import com.kingpivot.api.dto.memberSession.MemberSession;
import com.kingpivot.api.dto.memberSession.MemberSessionDetail;
import com.kingpivot.api.dto.memberStatistics.MemberStatisticsDto;
import com.kingpivot.api.dto.message.MessageDto;
import com.kingpivot.api.dto.patientMember.PatientMemberDetailDto;
import com.kingpivot.api.dto.patientMember.PatientMemberListDto;
import com.kingpivot.api.dto.people.PeopleDetail;
import com.kingpivot.api.dto.rank.ApiRankListDto;
import com.kingpivot.api.dto.roleEmployee.OutSystemRoleEmployeeDetail;
import com.kingpivot.api.dto.school.SchoolDto;
import com.kingpivot.api.dto.shop.ApiShopDetailDto;
import com.kingpivot.api.dto.site.SiteDetail;
import com.kingpivot.api.dto.site.SiteDto;
import com.kingpivot.api.dto.story.OutStoryDto;
import com.kingpivot.api.dto.student.ApiStudentDetail;
import com.kingpivot.api.dto.user.UserDetailDto;
import com.kingpivot.api.dto.user.UserDto;
import com.kingpivot.api.dto.weixin.WeiXinThird;
import com.kingpivot.api.dto.weixin.WeiXinUrlConstants;
import com.kingpivot.api.dto.weixin.WeixinDetail;
import com.kingpivot.api.dto.weixinApp.OutSystemWeixinAppDetail;
import com.kingpivot.api.dto.weixinAppMember.ApiWeixinAppMemberDetail;
import com.kingpivot.api.dto.weixinAppMember.ApiWeixinAppMemberList;
import com.kingpivot.api.dto.weixinMember.ApiWeixinMemberDetail;
import com.kingpivot.api.protocol.MessageHeader;
import com.kingpivot.api.protocol.MessagePacket;
import com.kingpivot.api.protocol.MessagePage;
import com.kingpivot.base.config.Config;
import com.kingpivot.base.config.RedisKey;
import com.kingpivot.base.config.UserAgent;
import com.kingpivot.base.member.model.Member;
import com.kingpivot.base.memberlog.model.Memberlog;
import com.kingpivot.base.memberlog.model.NewMemberlog;
import com.kingpivot.base.support.MemberLogDTO;
import com.kingpivot.common.KingBase;
import com.kingpivot.common.annotation.LoginRequired;
import com.kingpivot.common.jms.SendMessageService;
import com.kingpivot.common.model.BaseParameter;
import com.kingpivot.common.service.BaseService;
import com.kingpivot.common.utils.*;
import com.kingpivot.common.utils.quantum.Quantum;
import com.kingpivot.mapper.DataLogMapper;
import com.kingpivot.model.*;
import com.kingpivot.model.Dictionary;
import com.kingpivot.service.*;
import com.taobao.api.ApiException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping(value = "/api")
public class ApiMemberController {
    @Value("${spring.redis.timeout}")
    private Integer redisTimeOut;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private MemberFollowService memberFollowService;
    @Autowired
    private PeopleService peopleService;
    @Resource
    private SendMessageService sendMessageService;
    @Resource
    private MemberBuyReadService memberBuyReadService;
    @Resource
    private ChannelService channelService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private MemberOrderService memberOrderService;
    @Autowired
    private MemberOrderGoodsService memberOrderGoodsService;
    @Autowired
    private GoodsShopService goodsShopService;
    @Autowired
    private CreditRankService creditRankService;
    @Autowired
    private StoryService storyService;
    @Autowired
    private KingBase kingBase;
    @Autowired
    private FriendService friendService;
    @Autowired
    private ApplicationService applicationService;
    @Resource
    private MemberMajorService memberMajorService;
    @Autowired
    private SchoolService schoolService;
    @Autowired
    private WechartService wechartService;
    @Autowired
    private ParameterService parameterService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private MemberMapService memberMapService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CityService cityService;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private MajorService majorService;
    @Autowired
    private RankService rankService;
    @Autowired
    private DistrictService districtService;
    @Autowired
    private ObjectDefineService objectDefineService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private MemberDeviceService memberDeviceService;

    @Autowired
    private MemberStatisticsService memberStatisticsService;
    @Autowired
    private SequenceDefineService sequenceDefineService;
    @Autowired
    private UserService userService;
    @Autowired
    private WeixinMemberService weixinMemberService;
    @Autowired
    private BrowseService browseService;
    @Autowired
    private CollectService collectService;
    @Autowired
    private ParameterSiteService parameterSiteService;
    @Autowired
    private StarDefineService starDefineService;
    @Autowired
    private BaseService baseService;
    @Autowired
    private MemberTrainService memberTrainService;
    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private RoleEmployeeService roleEmployeeService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private PraiseService praiseService;
    @Autowired
    private MemberSessionService memberSessionService;
    @Autowired
    private MachineService machineService;

    @Resource
    private MemberFingerPrintService memberFingerPrintService;

    public static final String REGEX_MOBILE = "^((17[0-9])|(14[0-9])|(13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$";
    public static final String PARENT_MAJOR_ID = "8a2f462a6ce571e3016ce64460670185";
    public static final String PARENT_MAJOR_NAME = "会员家长";
    @Resource
    private FriendRelationService friendRelationService;
    @Autowired
    private MemberDocumentService memberDocumentService;
    @Autowired
    private DataLogMapper dataLogMapper;
    @Autowired
    private WeixinAppMemberService weixinAppMemberService;
    @Autowired
    private WeixinAppService weixinAppService;
    @Autowired
    private WeixinService weixinService;
    @Autowired
    private AgreementService agreementService;


    @ApiOperation(value = "submitMyLocation", notes = "上传会员定位信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/submitMyLocation", produces = {"application/json;charset=UTF-8"})
    public MessagePacket submitMyLocation(HttpServletRequest request, ModelMap map, ModelMap cmap) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String name = request.getParameter("name"); // 名称道路
        String shortName = request.getParameter("shortName"); // 简称
        String scopeName = request.getParameter("scopeName"); // 范围名称
        String memberAddress = request.getParameter("memberAddress");
        String mapX = request.getParameter("mapX");
        String mapY = request.getParameter("mapY");
        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID) && !cityService.checkIdIsValid(shengID)) {
            return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
        }
        String shengName = request.getParameter("shengName");
        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID) && !cityService.checkIdIsValid(shiID)) {
            return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
        }
        String shiName = request.getParameter("shiName");
        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID) && !cityService.checkIdIsValid(xianID)) {
            return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
        }
        String xianName = request.getParameter("xianName");
        String zhenID = request.getParameter("zhenID");
        if (StringUtils.isNotBlank(zhenID) && !cityService.checkIdIsValid(zhenID)) {
            return MessagePacket.newFail(MessageHeader.Code.zhenIDIsError, "zhenID不正确");
        }
        String zhenName = request.getParameter("zhenName");
        String districtID = request.getParameter("districtID");
        if (StringUtils.isNotBlank(districtID) && !districtService.checkIdIsValid(districtID)) {
            return MessagePacket.newFail(MessageHeader.Code.districtNotNull, "districtID不正确");
        }
        String districtName = request.getParameter("districtName");
        //添加memberMap记录
        MemberMap memberMap = new MemberMap();
        String id = IdGenerator.uuid32();
        memberMap.setId(id);
        memberMap.setApplicationID(member.getApplicationID());
        memberMap.setCompanyID(member.getCompanyID());
        memberMap.setSiteID(member.getSiteID());
        memberMap.setMemberID(member.getId());
        Employee employee;
        try {
            employee = employeeService.selectByMemberID(member.getId());
        } catch (Exception e) {
            return MessagePacket.newFail(MessageHeader.Code.employeeListGthOne, "该会员注册多个员工，请检查！");
        }
        memberMap.setEmployeeID(employee == null ? null : employee.getId());
        memberMap.setName(memberAddress);
        memberMap.setDeviceID(memberLogDTO.getDeviceId());
        memberMap.setName(name);
        memberMap.setShortName(shortName);
        memberMap.setScopeName(scopeName);
        memberMap.setMapX(mapX);
        memberMap.setMapY(mapY);
        if (StringUtils.isNotBlank(shengID)) memberMap.setShengID(shengID);
        if (StringUtils.isNotBlank(shengName)) memberMap.setShengName(shengName);
        if (StringUtils.isNotBlank(shiID)) memberMap.setShiID(shiID);
        if (StringUtils.isNotBlank(shiName)) memberMap.setShiName(shiName);
        if (StringUtils.isNotBlank(xianID)) memberMap.setXianID(xianID);
        if (StringUtils.isNotBlank(xianName)) memberMap.setXianName(xianName);
        if (StringUtils.isNotBlank(zhenID)) memberMap.setZhenID(zhenID);
        if (StringUtils.isNotBlank(zhenName)) memberMap.setZhenName(zhenName);
        if (StringUtils.isNotBlank(districtID)) memberMap.setDistrictID(districtID);
        if (StringUtils.isNotBlank(districtName)) memberMap.setDistrictName(districtName);
        if (StringUtils.isEmpty(shengID) && StringUtils.isNotBlank(shengName)) {
            cmap.put("name", shengName);
            memberMap.setShengName(shengName);
            shengID = memberService.selectCityIDByName(cmap);
            if (shengID != null) {
                /*shengID = cityDto.getCityID();*/
                memberMap.setShengID(shengID);
                memberMap.setName(shengName);
            }
        }
        if (StringUtils.isEmpty(shiID) && StringUtils.isNotBlank(shiName)) {
            cmap.put("name", shiName);
            memberMap.setShiName(shiName);
            if (!shengName.equals(shiName)) {
                cmap.put("parentID", shengID);
            }
            shiID = memberService.selectCityIDByName(cmap);
            if (shiID != null) {
                /* shiID = cityDto.getCityID();*/
                memberMap.setShiID(shiID);
                memberMap.setName(shiName);
            }
        }
        if (StringUtils.isEmpty(xianID) && StringUtils.isNotBlank(xianName)) {
            cmap.put("name", xianName);
            memberMap.setXianName(xianName);
            cmap.put("parentID", shiID);
            xianID = memberService.selectCityIDByName(cmap);
            if (xianID != null) {
                /* xianID = cityDto.getCityID();*/
                memberMap.setXianID(xianID);
                memberMap.setName(xianName);
            }
        }
        if (StringUtils.isEmpty(zhenID) && StringUtils.isNotBlank(zhenName)) {
            cmap.put("name", zhenName);
            memberMap.setZhenName(zhenName);
            cmap.put("parentID", xianID);
            zhenID = memberService.selectCityIDByName(cmap);
            if (zhenID != null) {
                /*zhenID = cityDto.getCityID();*/
                memberMap.setZhenID(zhenID);
                memberMap.setName(zhenName);
            }
        }
        if (StringUtils.isEmpty(districtID) && StringUtils.isNotBlank(districtName)) {
            map.put("name", districtName);
            memberMap.setDistrictName(districtName);
            memberMap.setName(districtName);
            if (StringUtils.isNotBlank(zhenID)) {
                map.put("cityID", zhenID);
            } else if (StringUtils.isNotBlank(xianID)) {
                map.put("cityID", xianID);
            } else if (StringUtils.isNotBlank(shiID)) {
                map.put("cityID", shiID);
            } else if (StringUtils.isNotBlank(shengID)) {
                map.put("cityID", shengID);
            }
            districtID = memberService.selectDistrictIDByName(map);
            if (districtID != null) {
                /*districtID = districtDto.getDistrictID();*/
                memberMap.setDeviceID(districtID);
            }
        }
        memberMapService.insert(memberMap);
        //更改会员定位信息
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("memberAddress", memberAddress);
        param.put("mapX", mapX);
        param.put("mapY", mapY);
        memberService.updateMemberMap(param);
        if (memberLogDTO != null) {
            String description = String.format("上传会员定位信息");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.submitMyLocation);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        Timestamp time = TimeTest.getTime();
        rsMap.put("submitTime", TimeTest.toDateTimeFormat(time));
        rsMap.put("memberMapID", id);
        rsMap.put("shengID", shengID);
        rsMap.put("shiID", shiID);
        rsMap.put("xianID", xianID);
        rsMap.put("zhenID", zhenID);
        rsMap.put("districtID", districtID);
        return MessagePacket.newSuccess(rsMap, "submitMyLocation success");
    }

    @ApiOperation(value = "submitOneCertCode", notes = "独立接口：提交一个电子证件编码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/submitOneCertCode", produces = {"application/json;charset=UTF-8"})
    public MessagePacket submitOneCertCode(HttpServletRequest request, ModelMap map) {

        String sessionID = request.getParameter("sessionID");
        Member member = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        String certCode = request.getParameter("certCode");
        if (StringUtils.isEmpty(certCode)) {
            return MessagePacket.newFail(MessageHeader.Code.certCodeNotNull, "certCode不能为空");
        }
        map.put("memberID", memberID);
        map.put("jingdongCode", certCode);
        memberService.submitOneCertCode(map);
        if (memberLogDTO != null) {
            String description1 = String.format("提一个电子证件编码");
            sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.submitOneCertCode);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("submitTime", TimeTest.getNowTime("yyyy-MM-dd HH:mm:ss"));
        return MessagePacket.newSuccess(rsMap, "submitOneCertCode success");
    }

    @ApiOperation(value = "outSystemCreateOneMember", notes = "独立接口：外部系统创建一个会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemCreateOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemCreateOneMember(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        UserLogin userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        User user = userService.selectUserByOutToken(outToken);
        if (userLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        map.put("siteID", userLogin.getSiteID());

        String applicationID = userLogin.getApplicationID();
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            map.put("applicationID", applicationID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID不能为空");
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID 不正确");
            }
            map.put("siteID", siteID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID 不能为空");
        }

        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
        }

        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (!companyService.checkIdIsValid(companyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
            }
            map.put("companyID", companyID);
        } else map.put("companyID", userLogin.getCompanyID());

        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }

        String roleType = request.getParameter("roleType");
        if (StringUtils.isNotBlank(roleType)) {
            if (!NumberUtils.isNumeric(roleType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "roleType请输入数字");
            }
            map.put("roleType", roleType);
        }

        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            if (!NumberUtils.isNumeric(bizBlock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "bizBlock请输入数字");
            }
            map.put("bizBlock", bizBlock);
        }

        String peopleID = request.getParameter("peopleID");
        if (StringUtils.isNotBlank(peopleID)) {
            if (!peopleService.checkIdIsValid(peopleID)) {
                return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "peopleID不正确");
            }
            map.put("peopleID", peopleID);
        }

        String idType = request.getParameter("idType");
        if (StringUtils.isNotBlank(idType)) {
            if (!NumberUtils.isNumeric(idType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "idType请输入数字");
            }
            map.put("idType", idType);
        }

        String idNumber = request.getParameter("idNumber");
        if (StringUtils.isNotBlank(idNumber)) {
            Member member = memberService.selectByIdNumber(idNumber);
            if (member != null) {
                return MessagePacket.newFail(MessageHeader.Code.idNumberIsExist, "idNumber 已存在"); //159098 isNumber 已存在
            }
            map.put("idNumber", idNumber);
        }

        String name = request.getParameter("name");
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }

        String shortName = request.getParameter("shortName");
        if (StringUtils.isNotBlank(shortName)) {
            map.put("shortName", shortName);
        }

        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }

        String description = request.getParameter("description");
        if (StringUtils.isNotBlank(description)) {
            map.put("description", description);
        }

        String rankID = request.getParameter("rankID");
        if (StringUtils.isNotBlank(rankID)) {
            if (!rankService.checkIdIsValid(rankID)) {
                return MessagePacket.newFail(MessageHeader.Code.rankIDNotFound, "rankID不正确");
            }
            map.put("rankID", rankID);
        }

        String majorID = request.getParameter("majorID");
        if (StringUtils.isNotBlank(majorID)) {
            if (!majorService.checkIdIsValid(majorID)) {
                return MessagePacket.newFail(MessageHeader.Code.majorIsNull, "majorID不正确");
            }
            map.put("majorID", majorID);
        }

        String creditRankID = request.getParameter("creditRankID");
        if (StringUtils.isNotBlank(creditRankID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("creditRank", creditRankID))) {
                return MessagePacket.newFail(MessageHeader.Code.creditRankIDNotFound, "creditRankID不正确");
            }
            map.put("creditRankID", creditRankID);
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isNotBlank(loginName)) {
            map.put("loginName", loginName);
        }

        String loginPassword = request.getParameter("loginPassword");
        if (StringUtils.isNotBlank(loginPassword)) {
            if (loginPassword.length() >= 32) {
                map.put("loginPassword", loginPassword);
            } else {
                map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", loginPassword, Config.ENCODE_KEY)));
            }
        }
        else {
            //密码为空使用默认初始密码
            LocalDate currentDate = LocalDate.now();
            int year = currentDate.getYear();
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            String shortName1 = dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    Locale.ENGLISH
            );
            LocalTime now = LocalTime.now();
            int hour = now.getHour(); // 返回 0-23
            String password = year + shortName1 + "@" + hour;
            String passwordmd = MD5.encodeMd5(String.format("%s%s", password, Config.ENCODE_KEY));
            map.put("loginPassword", passwordmd);
        }

        String password = request.getParameter("password");
        if (StringUtils.isNotBlank(password)) {
            map.put("password", password);
        }

        String tradePassword = request.getParameter("tradePassword");
        if (StringUtils.isNotBlank(tradePassword)) {
            if (tradePassword.length() >= 32) {
                map.put("tradePassword", tradePassword);
            } else {
                String psd = MD5.encodeMd5(String.format("%s%s", tradePassword, Config.ENCODE_KEY));
                map.put("tradePassword", psd);
            }
        }

        String avatarURL = request.getParameter("avatarURL");
        if (StringUtils.isNotBlank(avatarURL)) {
            map.put("avatarURL", avatarURL);
        }

        String avatarKey = request.getParameter("avatarKey");
        if (StringUtils.isNotBlank(avatarKey)) {
            map.put("avatarKey", avatarKey);
        }

        String avatarSmall = request.getParameter("avatarSmall");
        if (StringUtils.isNotBlank(avatarSmall)) {
            map.put("avatarSmall", avatarSmall);
        }

        String avatarMiddle = request.getParameter("avatarMiddle");
        if (StringUtils.isNotBlank(avatarMiddle)) {
            map.put("avatarMiddle", avatarMiddle);
        }

        String avatarLarge = request.getParameter("avatarLarge");
        if (StringUtils.isNotBlank(avatarLarge)) {
            map.put("avatarLarge", avatarLarge);
        }

        String avatarBack = request.getParameter("avatarBack");
        if (StringUtils.isNotBlank(avatarBack)) {
            map.put("avatarBack", avatarBack);
        }

        String titleID = request.getParameter("titleID");
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.titleIDError, "titleID不正确");
            }
            map.put("titleID", titleID);
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            List<Member> memberList = memberService.selectByMemberPhone(phone);
            if (!memberList.isEmpty()) {
                return MessagePacket.newFail(MessageHeader.Code.phoneIsExist, "phone 已存在"); // 14255 phone 已存在
            }
            map.put("phone", phone);
        }

        String email = request.getParameter("email");
        if (StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }

        String birthday = request.getParameter("birthday");
        if (StringUtils.isNotBlank(birthday)) {
            if (!TimeTest.dateIsPass(birthday)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "birthday 请输入数字 请输入yyyy-MM-dd的格式");
            }
            map.put("birthday", birthday);
        }

        String ageCategoryID = request.getParameter("ageCategoryID");
        if (StringUtils.isNotBlank(ageCategoryID)) {
            if (!categoryService.checkCategoryId(ageCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "ageCategoryID不正确");
            }
            map.put("ageCategoryID", ageCategoryID);
        }

        String marriageType = request.getParameter("marriageType");
        if (StringUtils.isNotBlank(marriageType)) {
            if (!"1".equals(marriageType) && !"2".equals(marriageType) && !"3".equals(marriageType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "marriageType请输入数字");
            }
            map.put("marriageType", marriageType);
        }

        String starDefineID = request.getParameter("starDefineID");
        if (StringUtils.isNotBlank(starDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("starDefine", starDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.starDefineIDNotFound, "starDefineID不正确");
            }
            map.put("starDefineID", starDefineID);
        }

        String emotionDictionaryID = request.getParameter("emotionDictionaryID");
        if (StringUtils.isNotBlank(emotionDictionaryID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("dictionary", emotionDictionaryID))) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "emotionDictionaryID不正确");
            }
            map.put("emotionDictionaryID", emotionDictionaryID);
        }

        String bloodDIctionaryID = request.getParameter("bloodDIctionaryID");
        if (StringUtils.isNotBlank(bloodDIctionaryID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("dictionary", bloodDIctionaryID))) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "bloodDIctionaryID不正确");
            }
            map.put("bloodDIctionaryID", bloodDIctionaryID);
        }

        String diseaseCategoriesID = request.getParameter("diseaseCategoriesID");
        if (StringUtils.isNotBlank(diseaseCategoriesID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("category", diseaseCategoriesID))) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "diseaseCategoriesID不正确");
            }
            map.put("diseaseCategoriesID", diseaseCategoriesID);
        }

        String highth = request.getParameter("highth");
        if (StringUtils.isNotBlank(highth)) {
            map.put("highth", highth);
        }

        String weight = request.getParameter("weight");
        if (StringUtils.isNotBlank(weight)) {
            map.put("weight", weight);
        }

        String BMI = request.getParameter("BMI");
        if (StringUtils.isNotBlank(BMI)) {
            map.put("BMI", BMI);
        }

        String chest = request.getParameter("chest");
        if (StringUtils.isNotBlank(chest)) {
            map.put("chest", chest);
        }

        String shoulder = request.getParameter("shoulder");
        if (StringUtils.isNotBlank(shoulder)) {
            map.put("shoulder", shoulder);
        }

        String waist = request.getParameter("waist");
        if (StringUtils.isNotBlank(waist)) {
            map.put("waist", waist);
        }

        String hips = request.getParameter("hips");
        if (StringUtils.isNotBlank(hips)) {
            map.put("hips", hips);
        }

        String legs = request.getParameter("legs");
        if (StringUtils.isNotBlank(legs)) {
            map.put("legs", legs);
        }

        String skinCategoryID = request.getParameter("skinCategoryID");
        if (StringUtils.isNotBlank(skinCategoryID)) {
            if (!categoryService.checkCategoryId(skinCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "skinCategoryID不正确");
            }
            map.put("skinCategoryID", skinCategoryID);
        }

        String faceCategoryID = request.getParameter("faceCategoryID");
        if (StringUtils.isNotBlank(faceCategoryID)) {
            if (!categoryService.checkCategoryId(faceCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "faceCategoryID不正确");
            }
            map.put("faceCategoryID", faceCategoryID);
        }

        String fiveCategoryID = request.getParameter("fiveCategoryID");
        if (StringUtils.isNotBlank(fiveCategoryID)) {
            if (!categoryService.checkCategoryId(fiveCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "fiveCategoryID不正确");
            }
            map.put("fiveCategoryID", fiveCategoryID);
        }

        String lineCategoryID = request.getParameter("lineCategoryID");
        if (StringUtils.isNotBlank(lineCategoryID)) {
            if (!categoryService.checkCategoryId(lineCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "lineCategoryID不正确");
            }
            map.put("lineCategoryID", lineCategoryID);
        }

        String boneCategoryID = request.getParameter("boneCategoryID");
        if (StringUtils.isNotBlank(boneCategoryID)) {
            if (!categoryService.checkCategoryId(boneCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "boneCategoryID不正确");
            }
            map.put("boneCategoryID", boneCategoryID);
        }

        String bodyCategoryID = request.getParameter("bodyCategoryID");
        if (StringUtils.isNotBlank(bodyCategoryID)) {
            if (!categoryService.checkCategoryId(bodyCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "bodyCategoryID不正确");
            }
            map.put("bodyCategoryID", bodyCategoryID);
        }

        String parentGroupCategoryID = request.getParameter("parentGroupCategoryID");
        if (StringUtils.isNotBlank(parentGroupCategoryID)) {
            if (!categoryService.checkCategoryId(parentGroupCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "parentGroupCategoryID不正确");
            }
            map.put("parentGroupCategoryID", parentGroupCategoryID);
        }

        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID)) {
            City sheng = cityService.selectById(shengID);
            if (sheng == null) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            map.put("shengID", sheng.getId());
            map.put("shengName", sheng.getName());
        }

        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID)) {
            City shi = cityService.selectById(shiID);
            if (shi == null) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            map.put("shiID", shi.getId());
            map.put("shiName", shi.getName());
        }

        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID)) {
            City xian = cityService.selectById(xianID);
            if (xian == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("xianID", xian.getId());
            map.put("xianName", xian.getName());
        }

        String zhenID = request.getParameter("zhenID");
        if (StringUtils.isNotBlank(zhenID)) {
            City zhen = cityService.selectById(zhenID);
            if (zhen == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("zhenID", zhenID);
            map.put("zhenName", zhen.getName());
        }

        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            City city = cityService.selectById(cityID);
            if (city == null) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }

        String cityChain = request.getParameter("cityChain");
        if (StringUtils.isNotBlank(cityChain)) {
            map.put("cityChain", cityChain);
        }

        String districtID = request.getParameter("districtID");
        if (StringUtils.isNotBlank(districtID)) {
            if (!districtService.checkIdIsValid(districtID)) {
                return MessagePacket.newFail(MessageHeader.Code.districtIDIsError, "districtID不正确");
            }
            map.put("districtID", districtID);
        }

        String propertyID = request.getParameter("propertyID");
        if (StringUtils.isNotBlank(propertyID)) {
            if (!propertyService.checkIdIsValid(propertyID)) {
                return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "propertyID不正确");
            }
            map.put("propertyID", propertyID);
        }

        String residentsType = request.getParameter("residentsType");
        if (StringUtils.isNotBlank(residentsType)) {
            if (!NumberUtils.isNumeric(residentsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "residentsType请输入数字");
            }
            map.put("residentsType", residentsType);
        }

        String homeAddr = request.getParameter("homeAddr");
        if (StringUtils.isNotBlank(homeAddr)) {
            map.put("homeAddr", homeAddr);
        }

        String homeZip = request.getParameter("homeZip");
        if (StringUtils.isNotBlank(homeZip)) {
            map.put("homeZip", homeZip);
        }

        String companyName = request.getParameter("companyName");
        if (StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }

        String companyJob = request.getParameter("companyJob");
        if (StringUtils.isNotBlank(companyJob)) {
            map.put("companyJob", companyJob);
        }

        String companyAddress = request.getParameter("companyAddress");
        if (StringUtils.isNotBlank(companyAddress)) {
            map.put("companyAddress", companyAddress);
        }

        String companyTel = request.getParameter("companyTel");
        if (StringUtils.isNotBlank(companyTel)) {
            map.put("companyTel", companyTel);
        }

        String companyZip = request.getParameter("companyZip");
        if (StringUtils.isNotBlank(companyZip)) {
            map.put("companyZip", companyZip);
        }

        String headName = request.getParameter("headName");
        if (StringUtils.isNotBlank(headName)) {
            map.put("headName", headName);
        }

        String career = request.getParameter("career");
        if (StringUtils.isNotBlank(career)) {
            map.put("career", career);
        }

        String industryCategoryID = request.getParameter("industryCategoryID");
        if (StringUtils.isNotBlank(industryCategoryID)) {
            if (!categoryService.checkCategoryId(industryCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "industryCategoryID不正确");
            }
            map.put("industryCategoryID", industryCategoryID);
        }

        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (StringUtils.isNotBlank(professionalCategoryID)) {
            if (!categoryService.checkCategoryId(professionalCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "professionalCategoryID不正确");
            }
            map.put("professionalCategoryID", professionalCategoryID);
        }

        String jobCategoryID = request.getParameter("jobCategoryID");
        if (StringUtils.isNotBlank(jobCategoryID)) {
            if (!categoryService.checkCategoryId(jobCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobCategoryID不正确");
            }
            map.put("jobCategoryID", jobCategoryID);
        }

        String jobYearCategoryID = request.getParameter("jobYearCategoryID");
        if (StringUtils.isNotBlank(jobYearCategoryID)) {
            if (!categoryService.checkCategoryId(jobYearCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobYearCategoryID不正确");
            }
            map.put("jobYearCategoryID", jobYearCategoryID);
        }

        String educationCategoryID = request.getParameter("educationCategoryID");
        if (StringUtils.isNotBlank(educationCategoryID)) {
            if (!categoryService.checkCategoryId(educationCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "educationCategoryID不正确");
            }
            map.put("educationCategoryID", educationCategoryID);
        }

        String graduationYear = request.getParameter("graduationYear");
        if (StringUtils.isNotBlank(graduationYear)) {
            if (!NumberUtils.isNumeric(graduationYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "graduationYear请输入数字");
            }
            map.put("graduationYear", graduationYear);
        }

        String schoolID = request.getParameter("schoolID");
        if (StringUtils.isNotBlank(schoolID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("school", schoolID))) {
                return MessagePacket.newFail(MessageHeader.Code.schoolIDNotFound, "schoolID不正确");
            }
            map.put("schoolID", schoolID);
        }

        String departmentID = request.getParameter("departmentID");
        if (StringUtils.isNotBlank(departmentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("department", departmentID))) {
                return MessagePacket.newFail(MessageHeader.Code.departmentIDNotFound, "departmentID不正确");
            }
            map.put("departmentID", departmentID);
        }

        String weibo = request.getParameter("weibo");
        if (StringUtils.isNotBlank(weibo)) {
            map.put("weibo", weibo);
        }

        String weiboToken = request.getParameter("weiboToken");
        if (StringUtils.isNotBlank(weiboToken)) {
            map.put("weiboToken", weiboToken);
        }

        String weiboID = request.getParameter("weiboID");
        if (StringUtils.isNotBlank(weiboID)) {
            map.put("weiboID", weiboID);
        }

        String weixin = request.getParameter("weixin");
        if (StringUtils.isNotBlank(weixin)) {
            map.put("weixin", weixin);
        }

        String weixinToken = request.getParameter("weixinToken");
        if (StringUtils.isNotBlank(weixinToken)) {
            map.put("weixinToken", weixinToken);
        }

        String weixinUnionID = request.getParameter("weixinUnionID");
        if (StringUtils.isNotBlank(weixinUnionID)) {
            map.put("weixinUnionID", weixinUnionID);
        }

        String weixinCode = request.getParameter("weixinCode");
        if (StringUtils.isNotBlank(weixinCode)) {
            map.put("weixinCode", weixinCode);
        }

        String weiboAccessToken = request.getParameter("weiboAccessToken");
        if (StringUtils.isNotBlank(weiboAccessToken)) {
            map.put("weiboAccessToken", weiboAccessToken);
        }

        String weiboTokenExpires = request.getParameter("weiboTokenExpires");
        if (StringUtils.isNotBlank(weiboTokenExpires)) {
            map.put("weiboTokenExpires", weiboTokenExpires);
        }

        String feishuID = request.getParameter("feishuID");
        if (StringUtils.isNotBlank(feishuID)) {
            map.put("feishuID", feishuID);
        }

        String feishuName = request.getParameter("feishuName");
        if (StringUtils.isNotBlank(feishuName)) {
            map.put("feishuName", feishuName);
        }

        String qiyeweixinID = request.getParameter("qiyeweixinID");
        if (StringUtils.isNotBlank(qiyeweixinID)) {
            map.put("qiyeweixinID", qiyeweixinID);
        }

        String qiyeweixinName = request.getParameter("qiyeweixinName");
        if (StringUtils.isNotBlank(qiyeweixinName)) {
            map.put("qiyeweixinName", qiyeweixinName);
        }

        String dingdingID = request.getParameter("dingdingID");
        if (StringUtils.isNotBlank(dingdingID)) {
            map.put("dingdingID", dingdingID);
        }

        String dingdingName = request.getParameter("dingdingName");
        if (StringUtils.isNotBlank(dingdingName)) {
            map.put("dingdingName", dingdingName);
        }

        String qqID = request.getParameter("qqID");
        if (StringUtils.isNotBlank(qqID)) {
            map.put("qqID", qqID);
        }

        String qqName = request.getParameter("qqName");
        if (StringUtils.isNotBlank(qqName)) {
            map.put("qqName", qqName);
        }

        String zhifubaoID = request.getParameter("zhifubaoID");
        if (StringUtils.isNotBlank(zhifubaoID)) {
            map.put("zhifubaoID", zhifubaoID);
        }

        String zhifubaoName = request.getParameter("zhifubaoName");
        if (StringUtils.isNotBlank(zhifubaoName)) {
            map.put("zhifubaoName", zhifubaoName);
        }

        String taobaoID = request.getParameter("taobaoID");
        if (StringUtils.isNotBlank(taobaoID)) {
            map.put("taobaoID", taobaoID);
        }

        String taobaoName = request.getParameter("taobaoName");
        if (StringUtils.isNotBlank(taobaoName)) {
            map.put("taobaoName", taobaoName);
        }

        String jingdongID = request.getParameter("jingdongID");
        if (StringUtils.isNotBlank(jingdongID)) {
            map.put("jingdongID", jingdongID);
        }

        String jingdongName = request.getParameter("jingdongName");
        if (StringUtils.isNotBlank(jingdongName)) {
            map.put("jingdongName", jingdongName);
        }

        String appleUserID = request.getParameter("appleUserID");
        if (StringUtils.isNotBlank(appleUserID)) {
            map.put("appleUserID", appleUserID);
        }

        String domainID = request.getParameter("domainID");
        if (StringUtils.isNotBlank(domainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("domain", domainID))) {
                return MessagePacket.newFail(MessageHeader.Code.domainIDNotFound, "domainID不正确");
            }
            map.put("domainID", domainID);
        }

        String domainAccount = request.getParameter("domainAccount");
        if (StringUtils.isNotBlank(domainAccount)) {
            map.put("domainAccount", domainAccount);
        }

        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
        }

        String recommendID = request.getParameter("recommendID");
        if (StringUtils.isNotBlank(recommendID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", recommendID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendID不正确");
            }
            map.put("recommendID", recommendID);
        }

        String recommendChain = request.getParameter("recommendChain");
        if (StringUtils.isNotBlank(recommendChain)) {
            map.put("recommendChain", recommendChain);
        }

        String levelNumber = request.getParameter("levelNumber");
        if (StringUtils.isNotBlank(levelNumber)) {
            if (!NumberUtils.isNumeric(levelNumber)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "levelNumber请输入数字");

            }
            map.put("levelNumber", levelNumber);
        }

        String linkMemberID = request.getParameter("linkMemberID");
        if (StringUtils.isNotBlank(linkMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", linkMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "linkMemberID不正确");
            }
            map.put("linkMemberID", linkMemberID);
        }

        String linkChain = request.getParameter("linkChain");
        if (StringUtils.isNotBlank(linkChain)) {
            map.put("linkChain", linkChain);
        }

        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }

        String serviceMemberID = request.getParameter("serviceMemberID");
        if (StringUtils.isNotBlank(serviceMemberID)) {
            if (!memberService.checkMemberId(serviceMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "serviceMemberID不正确");
            }
            map.put("serviceMemberID", serviceMemberID);
        }

        String tokenAddress = request.getParameter("tokenAddress");
        if (StringUtils.isNotBlank(tokenAddress)) {
            map.put("tokenAddress", tokenAddress);
        }

        String mapX = request.getParameter("mapX");
        if (StringUtils.isNotBlank(mapX)) {
            map.put("mapX", mapX);
        }

        String mapY = request.getParameter("mapY");
        if (StringUtils.isNotBlank(mapY)) {
            map.put("mapY", mapY);
        }

        String mapAddress = request.getParameter("mapAddress");
        if (StringUtils.isNotBlank(mapAddress)) {
            map.put("mapAddress", mapAddress);
        }

        String openID = request.getParameter("openID");
        if (StringUtils.isNotBlank(openID)) {
            map.put("openID", openID);
        }

        String openRegisterTime = request.getParameter("openRegisterTime");
        if (StringUtils.isNotBlank(openRegisterTime)) {
            map.put("openRegisterTime", openRegisterTime);
        }

        String createType = request.getParameter("createType");
        if (StringUtils.isNotBlank(createType)) {
            map.put("createType", createType);
        }

        String lastLoginTime = request.getParameter("lastLoginTime");
        if (StringUtils.isNotBlank(lastLoginTime)) {
            map.put("lastLoginTime", lastLoginTime);
        }
        String huanxinRegisterTime = request.getParameter("huanxinRegisterTime");
        if (StringUtils.isNotBlank(huanxinRegisterTime)) {
            map.put("huanxinRegisterTime", huanxinRegisterTime);
        }
        String huanxinLoginTime = request.getParameter("huanxinLoginTime");
        if (StringUtils.isNotBlank(huanxinLoginTime)) {
            map.put("huanxinLoginTime", huanxinLoginTime);
        }
        String isPublishMe = request.getParameter("isPublishMe");
        if (StringUtils.isNotBlank(isPublishMe)) {
            if (StringUtils.isNumeric(isPublishMe)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isPublishMe请输入数字");
            }
            map.put("isPublishMe", isPublishMe);
        }
        String preBirthday = request.getParameter("preBirthday");//预产期
        if (StringUtils.isNotBlank(preBirthday)) {
            map.put("preBirthday", TimeTest.strToDate(preBirthday));
        }


        String id = IdGenerator.uuid32();
        map.put("qiyeweixinID", sequenceDefineService.genCode("qiyeweixinID", id));
        String code = memberService.getCurRecommandCode(applicationID);
        if (StringUtils.isNotEmpty(code)) {
            map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
        } else {
            map.put("recommendCode", "00001");
        }
        map.put("id", id);
        map.put("levelNumber", 1);
        map.put("creator", userLogin.getUserID());
        memberService.insertByMap(map);

        // 创建关系记录
        String parentMemberID = request.getParameter("parentMemberID");
        String relationType = request.getParameter("relationType");
        String familyType = request.getParameter("familyType");
        ModelMap modelMap = new ModelMap();
        if (StringUtils.isNotBlank(parentMemberID)) {
            modelMap.put("applicationID", applicationID);
            modelMap.put("companyID", companyID);
            modelMap.put("Name", name);
            modelMap.put("titleID", titleID);
            modelMap.put("tel", phone);
            modelMap.put("email", email);
            modelMap.put("birthday", birthday);
            modelMap.put("toMemberID", id);
            modelMap.put("memberID", parentMemberID);
            if (StringUtils.isBlank(relationType) && StringUtils.isBlank(familyType)) {
                return MessagePacket.newFail(MessageHeader.Code.relationTypeORfamilyTypeNotAllNull, "relationType 或 familyType 不能同时为空");
            }
            modelMap.put("relationType", relationType);
            // modelMap.put("familyType",relationType);
            modelMap.put("familyType", familyType);
            modelMap.put("id", IdGenerator.uuid32());
            modelMap.put("creator", user.getId());
            relationshipService.insert(modelMap);
        }

        // 创建一个空的会员统计表
//        MemberStatisticsDto memberStatisticsDto = new MemberStatisticsDto();
//        String memberStatisticsID = IdGenerator.uuid32();
//        memberStatisticsDto.setId(memberStatisticsID);
//        memberStatisticsDto.setApplicationID(applicationID);
//        memberStatisticsDto.setMemberID(id);
//        memberStatisticsService.insert(memberStatisticsDto);
        if (user != null) {
            String description1 = String.format("外部系统创建一个会员");
            sendMessageService.sendDataLogMessage(user, description1, "outSystemCreateOneMember",
                    DataLog.objectDefineID.member.getOname(), id, name, DataLog.OperateType.ADD, request);
        }

        // == 创建会员发送队列 ==
        // session检查
        String ipaddr = WebUtil.getRemortIP(request);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);

        Member member = memberService.selectById(id);
        memberService.checkSession(null, session.getId(), id, siteID, member.toString());

        String description2 = "会员:" + name + "注册成功";
        MemberLogDTO memberLogDTO = new MemberLogDTO(siteID, member.getApplicationID(),
                null, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);

        sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description2,
                Memberlog.MemberOperateType.addSuccess);
        memberService.createMemberPrivilege(applicationID, member.getId());
        // == 创建会员发送队列 ==
        //新增memberLogin
        Site site = siteService.selectSiteById(siteID);
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", member.getId());
        memberLogin.setMemberID(member.getId());
        map1.put("memberName", member.getName());
        memberLogin.setMemberName(member.getName());
        map1.put("memberShortName", member.getShortName());
        memberLogin.setMemberShortName(member.getShortName());
        map1.put("memberAvatar", member.getAvatarURL());
        memberLogin.setMemberAvatar(member.getAvatarURL());
        if (StringUtils.isNotBlank(loginName)) {
            map1.put("loginName", loginName);
            memberLogin.setLoginName(loginName);
        } else {
            map1.put("loginName", member.getLoginName());
            memberLogin.setLoginName(member.getLoginName());
        }
        map1.put("phone", member.getPhone());
        memberLogin.setPhone(member.getPhone());
        map1.put("weixinToken", member.getWeixinToken());
        memberLogin.setWeixinToken(member.getWeixinToken());
        map1.put("weixinUnionID", member.getWeixinUnionID());
        memberLogin.setWeixinUnionID(member.getWeixinUnionID());
        map1.put("recommendCode", member.getRecommendCode());
        memberLogin.setRecommendCode(member.getRecommendCode());
        map1.put("recommendID", member.getRecommandID());
        memberLogin.setRecommendID(member.getRecommandID());
        map1.put("companyID", member.getCompanyID());
        memberLogin.setCompanyID(member.getCompanyID());
        map1.put("shopID", member.getShopID());
        memberLogin.setShopID(member.getShopID());
        map1.put("rankID", member.getRankID());
        memberLogin.setRankID(member.getRankID());
        map1.put("peopleID", member.getPeopleID());
        memberLogin.setPeopleID(member.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());

        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", id);
//        rsMap.put("memberStatisticsID", memberStatisticsID);

        return MessagePacket.newSuccess(rsMap, "outSystemCreateOneMember success");
    }

    @ApiOperation(value = "outSystemUpdateOneMember", notes = "独立接口：外部系统修改一个会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemUpdateOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemUpdateOneMember(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        UserLogin userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        User user = userService.selectUserByOutToken(outToken);
        if (userLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member = memberService.selectById(memberID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }
        map.put("id", memberID);

        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
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
            Channel channel = channelService.selectById(channelID);
            if (channel == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
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

        String roleType = request.getParameter("roleType");
        if (StringUtils.isNotBlank(roleType)) {
            if (!NumberUtils.isNumeric(roleType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "roleType请输入数字");
            }
            map.put("roleType", roleType);
        }

        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            if (!NumberUtils.isNumeric(bizBlock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "bizBlock请输入数字");
            }
            map.put("bizBlock", bizBlock);
        }

        String peopleID = request.getParameter("peopleID");
        if (StringUtils.isNotBlank(peopleID)) {
            if (!peopleService.checkIdIsValid(peopleID)) {
                return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "peopleID不正确");
            }
            map.put("peopleID", peopleID);
        }

        String idType = request.getParameter("idType");
        if (StringUtils.isNotBlank(idType)) {
            if (!NumberUtils.isNumeric(idType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "idType请输入数字");
            }
            map.put("idType", idType);
        }

        String idNumber = request.getParameter("idNumber");
        if (StringUtils.isNotBlank(idNumber)) {
            map.put("idNumber", idNumber);
        }

        String name = request.getParameter("name");
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
            member.setName(name);
        }

        String shortName = request.getParameter("shortName");
        if (StringUtils.isNotBlank(shortName)) {
            map.put("shortName", shortName);
        }

        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }

        String description = request.getParameter("description");
        if (StringUtils.isNotBlank(description)) {
            map.put("description", description);
        }

        String rankID = request.getParameter("rankID");
        if (StringUtils.isNotBlank(rankID)) {
            if (!rankService.checkIdIsValid(rankID)) {
                return MessagePacket.newFail(MessageHeader.Code.rankIDNotFound, "rankID不正确");
            }
            map.put("rankID", rankID);
        }

        String majorID = request.getParameter("majorID");
        if (StringUtils.isNotBlank(majorID)) {
            if (!majorService.checkIdIsValid(majorID)) {
                return MessagePacket.newFail(MessageHeader.Code.majorIsNull, "majorID不正确");
            }
            map.put("majorID", majorID);
        }

        String creditRankID = request.getParameter("creditRankID");
        if (StringUtils.isNotBlank(creditRankID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("creditRank", creditRankID))) {
                return MessagePacket.newFail(MessageHeader.Code.creditRankIDNotFound, "creditRankID不正确");
            }
            map.put("creditRankID", creditRankID);
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isNotBlank(loginName)) {
            map.put("loginName", loginName);
        }

        String loginPassword = request.getParameter("loginPassword");
        if (StringUtils.isNotBlank(loginPassword)) {
            if (loginPassword.length() >= 32) {
                map.put("loginPassword", loginPassword);
            } else {
//                login-Pass-word =generatePass-word("13100000000");
                loginPassword = generatePassword("1122334455");
                map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", loginPassword, Config.ENCODE_KEY)));
            }
        }

        String password = request.getParameter("password");
        if (StringUtils.isNotBlank(password)) {
            map.put("password", password);
        }

        String tradePassword = request.getParameter("tradePassword");
        if (StringUtils.isNotBlank(tradePassword)) {
            if (tradePassword.length() >= 32) {
                map.put("tradePassword", tradePassword);
            } else {
//                tradePass-word =generatePass-word("13100000000");
                tradePassword = generatePassword("1122334455");
                String psd = MD5.encodeMd5(String.format("%s%s", tradePassword, Config.ENCODE_KEY));
                map.put("tradePassword", psd);
            }
        }

        String avatarURL = request.getParameter("avatarURL");
        if (StringUtils.isNotBlank(avatarURL)) {
            map.put("avatarURL", avatarURL);
        }

        String avatarKey = request.getParameter("avatarKey");
        if (StringUtils.isNotBlank(avatarKey)) {
            map.put("avatarKey", avatarKey);
        }

        String avatarSmall = request.getParameter("avatarSmall");
        if (StringUtils.isNotBlank(avatarSmall)) {
            map.put("avatarSmall", avatarSmall);
        }

        String avatarMiddle = request.getParameter("avatarMiddle");
        if (StringUtils.isNotBlank(avatarMiddle)) {
            map.put("avatarMiddle", avatarMiddle);
        }

        String avatarLarge = request.getParameter("avatarLarge");
        if (StringUtils.isNotBlank(avatarLarge)) {
            map.put("avatarLarge", avatarLarge);
        }

        String avatarBack = request.getParameter("avatarBack");
        if (StringUtils.isNotBlank(avatarBack)) {
            map.put("avatarBack", avatarBack);
        }

        String titleID = request.getParameter("titleID");
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.titleIDError, "titleID不正确");
            }
            map.put("titleID", titleID);
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }

        String email = request.getParameter("email");
        if (StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }

        String birthday = request.getParameter("birthday");
        if (StringUtils.isNotBlank(birthday)) {
            if (!TimeTest.isTimeType(birthday)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "birthday 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("birthday", birthday);
        }

        String ageCategoryID = request.getParameter("ageCategoryID");
        if (StringUtils.isNotBlank(ageCategoryID)) {
            if (!categoryService.checkCategoryId(ageCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "ageCategoryID不正确");
            }
            map.put("ageCategoryID", ageCategoryID);
        }

        String marriageType = request.getParameter("marriageType");
        if (StringUtils.isNotBlank(marriageType)) {
            if (!"1".equals(marriageType) && !"2".equals(marriageType) && !"3".equals(marriageType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "marriageType请输入数字");
            }
            map.put("marriageType", marriageType);
        }

        String starDefineID = request.getParameter("starDefineID");
        if (StringUtils.isNotBlank(starDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("starDefine", starDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.starDefineIDNotFound, "starDefineID不正确");
            }
            map.put("starDefineID", starDefineID);
        }

        String emotionDictionaryID = request.getParameter("emotionDictionaryID");
        if (StringUtils.isNotBlank(emotionDictionaryID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("dictionary", emotionDictionaryID))) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "emotionDictionaryID不正确");
            }
            map.put("emotionDictionaryID", emotionDictionaryID);
        }

        String bloodDIctionaryID = request.getParameter("bloodDIctionaryID");
        if (StringUtils.isNotBlank(bloodDIctionaryID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("dictionary", bloodDIctionaryID))) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "bloodDIctionaryID不正确");
            }
            map.put("bloodDIctionaryID", bloodDIctionaryID);
        }

        String diseaseCategoriesID = request.getParameter("diseaseCategoriesID");
        if (StringUtils.isNotBlank(diseaseCategoriesID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("category", diseaseCategoriesID))) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "diseaseCategoriesID不正确");
            }
            map.put("diseaseCategoriesID", diseaseCategoriesID);
        }

        String highth = request.getParameter("highth");
        if (StringUtils.isNotBlank(highth)) {
            map.put("highth", highth);
        }

        String weight = request.getParameter("weight");
        if (StringUtils.isNotBlank(weight)) {
            map.put("weight", weight);
        }

        String BMI = request.getParameter("BMI");
        if (StringUtils.isNotBlank(BMI)) {
            map.put("BMI", BMI);
        }

        String chest = request.getParameter("chest");
        if (StringUtils.isNotBlank(chest)) {
            map.put("chest", chest);
        }

        String shoulder = request.getParameter("shoulder");
        if (StringUtils.isNotBlank(shoulder)) {
            map.put("shoulder", shoulder);
        }

        String waist = request.getParameter("waist");
        if (StringUtils.isNotBlank(waist)) {
            map.put("waist", waist);
        }

        String hips = request.getParameter("hips");
        if (StringUtils.isNotBlank(hips)) {
            map.put("hips", hips);
        }

        String legs = request.getParameter("legs");
        if (StringUtils.isNotBlank(legs)) {
            map.put("legs", legs);
        }

        String skinCategoryID = request.getParameter("skinCategoryID");
        if (StringUtils.isNotBlank(skinCategoryID)) {
            if (!categoryService.checkCategoryId(skinCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "skinCategoryID不正确");
            }
            map.put("skinCategoryID", skinCategoryID);
        }

        String faceCategoryID = request.getParameter("faceCategoryID");
        if (StringUtils.isNotBlank(faceCategoryID)) {
            if (!categoryService.checkCategoryId(faceCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "faceCategoryID不正确");
            }
            map.put("faceCategoryID", faceCategoryID);
        }

        String fiveCategoryID = request.getParameter("fiveCategoryID");
        if (StringUtils.isNotBlank(fiveCategoryID)) {
            if (!categoryService.checkCategoryId(fiveCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "fiveCategoryID不正确");
            }
            map.put("fiveCategoryID", fiveCategoryID);
        }

        String lineCategoryID = request.getParameter("lineCategoryID");
        if (StringUtils.isNotBlank(lineCategoryID)) {
            if (!categoryService.checkCategoryId(lineCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "lineCategoryID不正确");
            }
            map.put("lineCategoryID", lineCategoryID);
        }

        String boneCategoryID = request.getParameter("boneCategoryID");
        if (StringUtils.isNotBlank(boneCategoryID)) {
            if (!categoryService.checkCategoryId(boneCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "boneCategoryID不正确");
            }
            map.put("boneCategoryID", boneCategoryID);
        }

        String bodyCategoryID = request.getParameter("bodyCategoryID");
        if (StringUtils.isNotBlank(bodyCategoryID)) {
            if (!categoryService.checkCategoryId(bodyCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "bodyCategoryID不正确");
            }
            map.put("bodyCategoryID", bodyCategoryID);
        }

        String parentGroupCategoryID = request.getParameter("parentGroupCategoryID");
        if (StringUtils.isNotBlank(parentGroupCategoryID)) {
            if (!categoryService.checkCategoryId(parentGroupCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "parentGroupCategoryID不正确");
            }
            map.put("parentGroupCategoryID", parentGroupCategoryID);
        }

        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID)) {
            City sheng = cityService.selectById(shengID);
            if (sheng == null) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            map.put("shengID", sheng.getId());
            map.put("shengName", sheng.getName());
        }

        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID)) {
            City shi = cityService.selectById(shiID);
            if (shi == null) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            map.put("shiID", shi.getId());
            map.put("shiName", shi.getName());
        }

        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID)) {
            City xian = cityService.selectById(xianID);
            if (xian == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("xianID", xian.getId());
            map.put("xianName", xian.getName());
        }

        String zhenID = request.getParameter("zhenID");
        if (StringUtils.isNotBlank(zhenID)) {
            City zhen = cityService.selectById(zhenID);
            if (zhen == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("zhenID", zhenID);
            map.put("zhenName", zhen.getName());
        }

        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            City city = cityService.selectById(cityID);
            if (city == null) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }

        String cityChain = request.getParameter("cityChain");
        if (StringUtils.isNotBlank(cityChain)) {
            map.put("cityChain", cityChain);
        }

        String districtID = request.getParameter("districtID");
        if (StringUtils.isNotBlank(districtID)) {
            if (!districtService.checkIdIsValid(districtID)) {
                return MessagePacket.newFail(MessageHeader.Code.districtIDIsError, "districtID不正确");
            }
            map.put("districtID", districtID);
        }

        String propertyID = request.getParameter("propertyID");
        if (StringUtils.isNotBlank(propertyID)) {
            if (!propertyService.checkIdIsValid(propertyID)) {
                return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "propertyID不正确");
            }
            map.put("propertyID", propertyID);
        }

        String residentsType = request.getParameter("residentsType");
        if (StringUtils.isNotBlank(residentsType)) {
            if (!NumberUtils.isNumeric(residentsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "residentsType请输入数字");
            }
            map.put("residentsType", residentsType);
        }

        String homeAddr = request.getParameter("homeAddr");
        if (StringUtils.isNotBlank(homeAddr)) {
            map.put("homeAddr", homeAddr);
        }

        String homeZip = request.getParameter("homeZip");
        if (StringUtils.isNotBlank(homeZip)) {
            map.put("homeZip", homeZip);
        }

        String companyName = request.getParameter("companyName");
        if (StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }

        String companyJob = request.getParameter("companyJob");
        if (StringUtils.isNotBlank(companyJob)) {
            map.put("companyJob", companyJob);
        }

        String companyAddress = request.getParameter("companyAddress");
        if (StringUtils.isNotBlank(companyAddress)) {
            map.put("companyAddress", companyAddress);
        }

        String companyTel = request.getParameter("companyTel");
        if (StringUtils.isNotBlank(companyTel)) {
            map.put("companyTel", companyTel);
        }

        String companyZip = request.getParameter("companyZip");
        if (StringUtils.isNotBlank(companyZip)) {
            map.put("companyZip", companyZip);
        }

        String headName = request.getParameter("headName");
        if (StringUtils.isNotBlank(headName)) {
            map.put("headName", headName);
        }

        String career = request.getParameter("career");
        if (StringUtils.isNotBlank(career)) {
            map.put("career", career);
        }

        String industryCategoryID = request.getParameter("industryCategoryID");
        if (StringUtils.isNotBlank(industryCategoryID)) {
            if (!categoryService.checkCategoryId(industryCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "industryCategoryID不正确");
            }
            map.put("industryCategoryID", industryCategoryID);
        }

        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (StringUtils.isNotBlank(professionalCategoryID)) {
            if (!categoryService.checkCategoryId(professionalCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "professionalCategoryID不正确");
            }
            map.put("professionalCategoryID", professionalCategoryID);
        }

        String jobCategoryID = request.getParameter("jobCategoryID");
        if (StringUtils.isNotBlank(jobCategoryID)) {
            if (!categoryService.checkCategoryId(jobCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobCategoryID不正确");
            }
            map.put("jobCategoryID", jobCategoryID);
        }

        String jobYearCategoryID = request.getParameter("jobYearCategoryID");
        if (StringUtils.isNotBlank(jobYearCategoryID)) {
            if (!categoryService.checkCategoryId(jobYearCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobYearCategoryID不正确");
            }
            map.put("jobYearCategoryID", jobYearCategoryID);
        }

        String educationCategoryID = request.getParameter("educationCategoryID");
        if (StringUtils.isNotBlank(educationCategoryID)) {
            if (!categoryService.checkCategoryId(educationCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "educationCategoryID不正确");
            }
            map.put("educationCategoryID", educationCategoryID);
        }

        String graduationYear = request.getParameter("graduationYear");
        if (StringUtils.isNotBlank(graduationYear)) {
            if (!NumberUtils.isNumeric(graduationYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "graduationYear请输入数字");
            }
            map.put("graduationYear", graduationYear);
        }

        String schoolID = request.getParameter("schoolID");
        if (StringUtils.isNotBlank(schoolID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("school", schoolID))) {
                return MessagePacket.newFail(MessageHeader.Code.schoolIDNotFound, "schoolID不正确");
            }
            map.put("schoolID", schoolID);
        }

        String departmentID = request.getParameter("departmentID");
        if (StringUtils.isNotBlank(departmentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("department", departmentID))) {
                return MessagePacket.newFail(MessageHeader.Code.departmentIDNotFound, "departmentID不正确");
            }
            map.put("departmentID", departmentID);
        }

        String weibo = request.getParameter("weibo");
        if (StringUtils.isNotBlank(weibo)) {
            map.put("weibo", weibo);
        }

        String weiboToken = request.getParameter("weiboToken");
        if (StringUtils.isNotBlank(weiboToken)) {
            map.put("weiboToken", weiboToken);
        }

        String weiboID = request.getParameter("weiboID");
        if (StringUtils.isNotBlank(weiboID)) {
            map.put("weiboID", weiboID);
        }

        String weixin = request.getParameter("weixin");
        if (StringUtils.isNotBlank(weixin)) {
            map.put("weixin", weixin);
        }

        String weixinToken = request.getParameter("weixinToken");
        if (StringUtils.isNotBlank(weixinToken)) {
            map.put("weixinToken", weixinToken);
        }

        String weixinUnionID = request.getParameter("weixinUnionID");
        if (StringUtils.isNotBlank(weixinUnionID)) {
            map.put("weixinUnionID", weixinUnionID);
        }

        String weixinCode = request.getParameter("weixinCode");
        if (StringUtils.isNotBlank(weixinCode)) {
            map.put("weixinCode", weixinCode);
        }

        String weiboAccessToken = request.getParameter("weiboAccessToken");
        if (StringUtils.isNotBlank(weiboAccessToken)) {
            map.put("weiboAccessToken", weiboAccessToken);
        }

        String weiboTokenExpires = request.getParameter("weiboTokenExpires");
        if (StringUtils.isNotBlank(weiboTokenExpires)) {
            map.put("weiboTokenExpires", weiboTokenExpires);
        }

        String feishuID = request.getParameter("feishuID");
        if (StringUtils.isNotBlank(feishuID)) {
            map.put("feishuID", feishuID);
        }

        String feishuName = request.getParameter("feishuName");
        if (StringUtils.isNotBlank(feishuName)) {
            map.put("feishuName", feishuName);
        }

        String qiyeweixinID = request.getParameter("qiyeweixinID");
        if (StringUtils.isNotBlank(qiyeweixinID)) {
            map.put("qiyeweixinID", qiyeweixinID);
        }

        String qiyeweixinName = request.getParameter("qiyeweixinName");
        if (StringUtils.isNotBlank(qiyeweixinName)) {
            map.put("qiyeweixinName", qiyeweixinName);
        }

        String dingdingID = request.getParameter("dingdingID");
        if (StringUtils.isNotBlank(dingdingID)) {
            map.put("dingdingID", dingdingID);
        }

        String dingdingName = request.getParameter("dingdingName");
        if (StringUtils.isNotBlank(dingdingName)) {
            map.put("dingdingName", dingdingName);
        }

        String qqID = request.getParameter("qqID");
        if (StringUtils.isNotBlank(qqID)) {
            map.put("qqID", qqID);
        }

        String qqName = request.getParameter("qqName");
        if (StringUtils.isNotBlank(qqName)) {
            map.put("qqName", qqName);
        }

        String zhifubaoID = request.getParameter("zhifubaoID");
        if (StringUtils.isNotBlank(zhifubaoID)) {
            map.put("zhifubaoID", zhifubaoID);
        }

        String zhifubaoName = request.getParameter("zhifubaoName");
        if (StringUtils.isNotBlank(zhifubaoName)) {
            map.put("zhifubaoName", zhifubaoName);
        }

        String taobaoID = request.getParameter("taobaoID");
        if (StringUtils.isNotBlank(taobaoID)) {
            map.put("taobaoID", taobaoID);
        }

        String taobaoName = request.getParameter("taobaoName");
        if (StringUtils.isNotBlank(taobaoName)) {
            map.put("taobaoName", taobaoName);
        }

        String jingdongID = request.getParameter("jingdongID");
        if (StringUtils.isNotBlank(jingdongID)) {
            map.put("jingdongID", jingdongID);
        }

        String jingdongName = request.getParameter("jingdongName");
        if (StringUtils.isNotBlank(jingdongName)) {
            map.put("jingdongName", jingdongName);
        }

        String appleUserID = request.getParameter("appleUserID");
        if (StringUtils.isNotBlank(appleUserID)) {
            map.put("appleUserID", appleUserID);
        }

        String domainID = request.getParameter("domainID");
        if (StringUtils.isNotBlank(domainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("domain", domainID))) {
                return MessagePacket.newFail(MessageHeader.Code.domainIDNotFound, "domainID不正确");
            }
            map.put("domainID", domainID);
        }

        String domainAccount = request.getParameter("domainAccount");
        if (StringUtils.isNotBlank(domainAccount)) {
            map.put("domainAccount", domainAccount);
        }

        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
        }

        String recommendID = request.getParameter("recommendID");
        if (StringUtils.isNotBlank(recommendID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", recommendID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendID不正确");
            }
            map.put("recommendID", recommendID);
        }

        String recommendChain = request.getParameter("recommendChain");
        if (StringUtils.isNotBlank(recommendChain)) {
            map.put("recommendChain", recommendChain);
        }

        String levelNumber = request.getParameter("levelNumber");
        if (StringUtils.isNotBlank(levelNumber)) {
            if (!NumberUtils.isNumeric(levelNumber)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "levelNumber请输入数字");

            }
            map.put("levelNumber", levelNumber);
        }

        String linkMemberID = request.getParameter("linkMemberID");
        if (StringUtils.isNotBlank(linkMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", linkMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "linkMemberID不正确");
            }
            map.put("linkMemberID", linkMemberID);
        }

        String linkChain = request.getParameter("linkChain");
        if (StringUtils.isNotBlank(linkChain)) {
            map.put("linkChain", linkChain);
        }

        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }

        String serviceMemberID = request.getParameter("serviceMemberID");
        if (StringUtils.isNotBlank(serviceMemberID)) {
            if (!memberService.checkMemberId(serviceMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "serviceMemberID不正确");
            }
            map.put("serviceMemberID", serviceMemberID);
        }

        String tokenAddress = request.getParameter("tokenAddress");
        if (StringUtils.isNotBlank(tokenAddress)) {
            map.put("tokenAddress", tokenAddress);
        }

        String mapX = request.getParameter("mapX");
        if (StringUtils.isNotBlank(mapX)) {
            map.put("mapX", mapX);
        }

        String mapY = request.getParameter("mapY");
        if (StringUtils.isNotBlank(mapY)) {
            map.put("mapY", mapY);
        }

        String mapAddress = request.getParameter("mapAddress");
        if (StringUtils.isNotBlank(mapAddress)) {
            map.put("mapAddress", mapAddress);
        }

        String openID = request.getParameter("openID");
        if (StringUtils.isNotBlank(openID)) {
            map.put("openID", openID);
        }

        String openRegisterTime = request.getParameter("openRegisterTime");
        if (StringUtils.isNotBlank(openRegisterTime)) {
            map.put("openRegisterTime", openRegisterTime);
        }

        String createType = request.getParameter("createType");
        if (StringUtils.isNotBlank(createType)) {
            map.put("createType", createType);
        }

        String lastLoginTime = request.getParameter("lastLoginTime");
        if (StringUtils.isNotBlank(lastLoginTime)) {
            map.put("lastLoginTime", lastLoginTime);
        }

        String huanxinRegisterTime = request.getParameter("huanxinRegisterTime"); // 环信注册时间
        String huanxinLoginTime = request.getParameter("huanxinLoginTime"); // 环信登录时间

        if (StringUtils.isNotBlank(huanxinRegisterTime)) {
            if (!TimeTest.isTimeType(huanxinRegisterTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "huanxinRegisterTime 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("huanxinRegisterTime", huanxinRegisterTime);
        }
        if (StringUtils.isNotBlank(huanxinLoginTime)) {
            if (!TimeTest.isTimeType(huanxinLoginTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "huanxinLoginTime 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("huanxinLoginTime", huanxinLoginTime);
        }
        String isPublishMe = request.getParameter("isPublishMe");
        if (StringUtils.isNotBlank(isPublishMe)) {
            if (!NumberUtils.isNumeric(isPublishMe)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isPublishMe请输入数字");
            }
            map.put("isPublishMe", isPublishMe);
        }
        String companyFax = request.getParameter("companyFax");
        if (StringUtils.isNotBlank(companyFax)) {
            map.put("companyFax", companyFax);
        }

        String companyWebsite = request.getParameter("companyWebsite");
        if (StringUtils.isNotBlank(companyWebsite)) {
            map.put("companyWebsite", companyWebsite);
        }

        String weixinQrURL = request.getParameter("weixinQrURL");
        if (StringUtils.isNotBlank(weixinQrURL)) {
            map.put("weixinQrURL", weixinQrURL);
        }

        String qiyeweixinQrURL = request.getParameter("qiyeweixinQrURL");
        if (StringUtils.isNotBlank(qiyeweixinQrURL)) {
            map.put("qiyeweixinQrURL", qiyeweixinQrURL);
        }

        String clothingSize = request.getParameter("clothingSize");
        if (StringUtils.isNotBlank(clothingSize)) {
            if (!NumberUtils.isNumeric(clothingSize)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "clothingSize请输入数字");
            }
            map.put("clothingSize", clothingSize);
        }

        String jingdongCode = request.getParameter("jingdongCode");
        if (StringUtils.isNotBlank(jingdongCode)) {
            if (!NumberUtils.isNumeric(jingdongCode)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "jingdongCode请输入数字");
            }
            map.put("jingdongCode", jingdongCode);
        }

        map.put("modifier", userLogin.getMemberID());
        map.put("lastChangePWTime", TimeTest.getTimeStr());
        map.put("PWErrorTimes", 0);
        memberService.updateMember(map);
        if (user != null) {
            String description1 = String.format("外部系统修改一个会员");
            sendMessageService.sendDataLogMessage(user, description1, "outSystemUpdateOneMember",
                    DataLog.objectDefineID.member.getOname(), memberID, member.getName(), DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        rsMap.put("loginPassword", loginPassword);
        rsMap.put("tradePassword", tradePassword);
        return MessagePacket.newSuccess(rsMap, "outSystemUpdateOneMember success");
    }

    @ApiOperation(value = "outSystemResetOneMemberPassword", notes = "独立接口：外部系统重置一个会员密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
            @ApiImplicitParam(paramType = "query", name = "memberID", value = "会员ID", dataType = "String")
    })
    @LoginRequired(tokenType = LoginRequired.TokenType.OUTTOKEN)
    @RequestMapping(value = "/outSystemResetOneMemberPassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemResetOneMemberPassword(HttpServletRequest request, ModelMap map) {
        UserLogin userLogin = (UserLogin) request.getAttribute("userLogin");
        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member = memberService.selectById(memberID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }
        PermissionsUtil permissionsUtil = new PermissionsUtil();
        permissionsUtil.setPermissions("resetpw");
        permissionsUtil.setDataId(memberID);
        permissionsUtil.setTable("member");

        MessagePacket messagePacket = userService.userPermissionJudgment(request, permissionsUtil);
        if (messagePacket != null) return messagePacket;
//        String loginPass-word =generatePass-word("13100000000");
        String loginPassword = generatePassword("112234455");
        map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", loginPassword, Config.ENCODE_KEY)));
//        String tradePass-word =generatePass-word("13100000000");
        String tradePassword = generatePassword("112234455");
        map.put("tradePassword", MD5.encodeMd5(String.format("%s%s", tradePassword, Config.ENCODE_KEY)));
        map.put("id", memberID);
        map.put("lastChangePWTime", TimeTest.getTimeStr());
        map.put("PWErrorTimes", 0);
        memberService.updateMember(map);

        userLogin.setInterfaceName("外部系统重置一个会员密码");
        userLogin.setInterfaceShortName("outSystemResetOneMemberPassword");
        userLogin.setInterfaceOperateType(DataLog.OperateType.UPDATE);
        userLogin.setObjectDefineID(DataLog.objectDefineID.member.getOname());
        userLogin.setObjectID(memberID);
        userLogin.setObjectName(member.getName());
        userLogin.setOperateType(DataLog.OperateType.UPDATE);
        sendMessageService.sendDataLogMessageNew(userLogin, request);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        rsMap.put("loginPassword", loginPassword);
        rsMap.put("tradePassword", tradePassword);
        return MessagePacket.newSuccess(rsMap, "outSystemResetOneMemberPassword success");
    }

    @ApiOperation(value = "outSystemDeleteOneMember", notes = "独立接口：外部系统删除一个会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemDeleteOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemDeleteOneMember(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//      User user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("modifier", user.getId());
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        map.put("id", memberID);
        map.put("isValid", 0);
        Member data = memberService.selectById(memberID);
        memberService.updateMember(map);

        if (user != null) {
            String descriptions = String.format("外部系统删除一个会员");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemDeleteOneMember",
                    DataLog.objectDefineID.member.getOname(), memberID,
                    data.getName(), DataLog.OperateType.DELETE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("deleteTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemDeleteOneMember success");
    }

    @ApiOperation(value = "outSystemLockOneMember", notes = "独立接口：外部系统锁定一个会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemLockOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemLockOneMember(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//      User user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("modifier", user.getId());
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        map.put("id", memberID);
        map.put("isLock", 1);

        memberService.updateMember(map);

        if (user != null) {
            Member data = memberService.selectById(memberID);
            String descriptions = String.format("外部系统锁定一个会员");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemLockOneMember",
                    DataLog.objectDefineID.member.getOname(), memberID,
                    data.getName(), DataLog.OperateType.LOCKORUNLOCK, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("lockTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemLockOneMember success");
    }

    @ApiOperation(value = "outSystemUnLockOneMember", notes = "独立接口：外部系统解锁一个会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemUnLockOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemUnLockOneMember(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//      User user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("modifier", user.getId());
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        map.put("id", memberID);
        map.put("isLock", 0);
        map.put("PWErrorTimes", 0);

        memberService.updateMember(map);

        if (user != null) {
            Member data = memberService.selectById(memberID);
            String descriptions = String.format("外部系统解锁一个会员");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemUnLockOneMember",
                    DataLog.objectDefineID.member.getOname(), memberID,
                    data.getName(), DataLog.OperateType.LOCKORUNLOCK, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("unLockTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemUnLockOneMember success");
    }

    @ApiOperation(value = "outSystemGetMemberDetail", notes = "独立接口：外部系统获取会员详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetMemberDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberDetail(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        UserLogin userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        User user=userService.selectUserByOutToken(outToken);
        if (userLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        OutMemberList data = memberService.outSystemGetMemberDetail(memberID);
        if (data == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        if (user != null) {
            String descriptions = String.format("外部系统获取会员详细信息");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberDetail",
                    DataLog.objectDefineID.member.getOname(), memberID,
                    data.getName(), DataLog.OperateType.DETAIL, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", data);
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberDetail success");
    }

    @ApiOperation(value = "outSystemGetMemberList", notes = "独立接口：外部系统获取会员列表")
    @RequestMapping(value = "/outSystemGetMemberList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberList(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        User user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
        // User user=userService.selectUserByOutToken(outToken);
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
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
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
            if (!baseService.checkIdIsValid(new BaseParameter("channel", channelID))) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (!companyService.checkIdIsValid(companyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String checkMemberMajor = request.getParameter("checkMemberMajor");
        if (StringUtils.isNotBlank(checkMemberMajor)) {
            map.put("checkMemberMajor", checkMemberMajor);
        }
        String shopID = request.getParameter("shopID");
        if (StringUtils.isNotBlank(shopID)) {
            if (!shopService.checkIdIsValid(shopID)) {
                return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
            }
            map.put("shopID", shopID);
        }
        String rankID = request.getParameter("rankID");
        if (StringUtils.isNotBlank(rankID)) {
            if (!rankService.checkIdIsValid(rankID)) {
                return MessagePacket.newFail(MessageHeader.Code.rankIDNotFound, "rankID不正确");
            }
            map.put("rankID", rankID);
        }
        String majorID = request.getParameter("majorID");
        if (StringUtils.isNotBlank(majorID)) {
            if (!majorService.checkIdIsValid(majorID)) {
                return MessagePacket.newFail(MessageHeader.Code.majorIsNull, "majorID不正确");
            }
            map.put("majorID", majorID);
        }
        String titleID = request.getParameter("titleID");
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.titleIDError, "titleID不正确");
            }
            map.put("titleID", titleID);
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            if (!NumberUtils.isNumeric(bizBlock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "bizBlock请输入数字");
            }
            map.put("bizBlock", bizBlock);
        }
        String roleType = request.getParameter("roleType");
        if (StringUtils.isNotBlank(roleType)) {
            if (!NumberUtils.isNumeric(roleType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "roleType请输入数字");
            }
            map.put("roleType", roleType);
        }
        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID)) {
            if (!cityService.checkIdIsValid(shengID)) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDNotFond, "shengID不正确");
            }
            map.put("shengID", shengID);
        }
        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID)) {
            if (!cityService.checkIdIsValid(shiID)) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDError, "shiID不正确");
            }
            map.put("shiID", shiID);
        }
        String recommendID = request.getParameter("recommendID");
        if (StringUtils.isNotBlank(recommendID)) {
            if (!memberService.checkMemberId(recommendID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendID不正确");
            }
            map.put("recommendID", recommendID);
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }
        String serviceMemberID = request.getParameter("serviceMemberID");
        if (StringUtils.isNotBlank(serviceMemberID)) {
            if (!memberService.checkMemberId(serviceMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "serviceMemberID不正确");
            }
            map.put("serviceMemberID", serviceMemberID);
        }
        String createType = request.getParameter("createType");
        if (StringUtils.isNotBlank(createType)) {
            if (!NumberUtils.isNumeric(createType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "createType请输入数字");
            }
            map.put("createType", createType);
        }
        String isValid = request.getParameter("isValid");
        if (StringUtils.isNotBlank(isValid)) {
            if (!NumberUtils.isNumeric(isValid)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isValid请输入数字");
            }
            map.put("isValid", isValid);
        } else {
            map.put("isValid", 1);
        }
        String isLock = request.getParameter("isLock");
        if (StringUtils.isNotBlank(isLock)) {
            if (!NumberUtils.isNumeric(isLock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isLock请输入数字");
            }
            map.put("isLock", isLock);
        }
        /*else {
            map.put("isLock", 0);
        }*/
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            map.put("keyWords", "%" + keyWords + "%");
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) {
            map.put("beginTime", beginTime);
        }
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) {
            map.put("endTime", endTime);
        }
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) {
            if (!NumberUtils.isNumeric(sortTypeTime)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入1到2的数字");
            }
            map.put("sortTypeTime", sortTypeTime);
        }
        String sortTypeMemberMajorEffEndDate = request.getParameter("sortTypeMemberMajorEffEndDate");
        if (StringUtils.isNotBlank(sortTypeMemberMajorEffEndDate)) {
            if (!NumberUtils.isNumeric(sortTypeMemberMajorEffEndDate)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeMemberMajorEffEndDate请输入1到2的数字");
            }
            map.put("sortTypeMemberMajorEffEndDate", sortTypeMemberMajorEffEndDate);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入1到2的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        String sortTypeRecommendCode = request.getParameter("sortTypeRecommendCode");
        if (StringUtils.isNotBlank(sortTypeRecommendCode)) {
            if (!NumberUtils.isNumeric(sortTypeRecommendCode)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeRecommendCode 请输入数字");
            }
            map.put("sortTypeRecommendCode", sortTypeRecommendCode);
        }
        Page page = HttpServletHelper.assembyPage(request);        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<OutSystemMemberList> list = memberService.outSystemGetMemberList(map);
        if (!list.isEmpty()) {
            PageInfo<OutSystemMemberList> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
            for (OutSystemMemberList member : list) {

                //查看是不是推广人
                map.put("memberID", member.getMemberID());
                map.put("majorID", "8a2f462a78ecd2fa0178eedb3ed4057b");
                ApiMemberMajorDetailDto memberMajorDetail = memberMajorService.getDetailByMap(map);
                if (memberMajorDetail != null) {
                    member.setIsPromoter(memberMajorDetail.getVerifyStatus());
                }

                if (StringUtils.isNotBlank(member.getCompanyID())) {
                    Company company = companyService.selectById(member.getCompanyID());
                    if (company != null) {
                        member.setCompanyName(company.getName());
                    }
                }
                if (StringUtils.isNotBlank(member.getSiteID())) {
                    Site site = siteService.selectSiteById(member.getSiteID());
                    if (site != null) {
                        member.setSiteName(site.getName());
                    }
                }
                if (StringUtils.isNotBlank(member.getShopID())) {
                    ApiShopDetailDto shop = shopService.getShopDetail(member.getShopID());
                    if (shop != null) {
                        member.setShopName(shop.getName());
                    }
                }
                if (StringUtils.isNotBlank(member.getShopID())) {
                    People people = peopleService.getById(member.getShopID());
                    if (people != null) {
                        member.setPeopleName(people.getName());
                    }
                }
                if (StringUtils.isNotBlank(member.getMajorID())) {
                    String majorName = majorService.getNameById(member.getMajorID());
                    if (StringUtils.isNotBlank(majorName)) {
                        member.setMajorName(majorName);
                    }
                }
                if (StringUtils.isNotBlank(member.getTitleID())) {
                    String titleName = categoryService.getNameById(member.getTitleID());
                    if (StringUtils.isNotBlank(titleName)) {
                        member.setTitleName(titleName);
                    }
                }

                if (StringUtils.isNotBlank(member.getLoginName())){
                    String loginName = member.getLoginName();
                    int length = loginName.length();
                    String maskedLoginName;
                    if (length >= 3) {
                        // 计算三部分的长度（首字符、中间*、尾字符）
                        int firstPart = length / 3;
                        int lastPart = length / 3;
                        int middlePart = length - firstPart - lastPart;

                        // 截取首部分和尾部分
                        String first = loginName.substring(0, firstPart);
                        String last = loginName.substring(length - lastPart);

                        // 生成中间*部分
                        String middle = StringUtils.repeat("*", middlePart);

                        // 拼接处理后的字符串并设置回去
                         maskedLoginName = first + middle + last;

                    }
                    else {
                        maskedLoginName = StringUtils.repeat("*", length);
                    }
                    member.setLoginName(maskedLoginName);
                }
                if (StringUtils.isNotBlank(member.getPhone())){
                    String phone = member.getPhone();
                    int length = phone.length();
                    String maskedLoginName;
                    if (length >= 3) {
                        // 计算三部分的长度（首字符、中间*、尾字符）
                        int firstPart = length / 3;
                        int lastPart = length / 3;
                        int middlePart = length - firstPart - lastPart;

                        // 截取首部分和尾部分
                        String first = phone.substring(0, firstPart);
                        String last = phone.substring(length - lastPart);

                        // 生成中间*部分
                        String middle = StringUtils.repeat("*", middlePart);

                        // 拼接处理后的字符串并设置回去
                        maskedLoginName = first + middle + last;
                        member.setPhone(maskedLoginName);
                    }
                }

            }
        }
        if (user != null) {
            String descriptions = String.format("外部系统获取会员列表");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberList",
                    DataLog.objectDefineID.member.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberList success");
    }

    @ApiOperation(value = "queryMemberRealNameStatus", notes = "查询会员实名认证的状态")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/queryMemberRealNameStatus", produces = {"application/json;charset=UTF-8"})
    public MessagePacket queryMemberRealNameStatus(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        /*只能查询自己的
        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            memberID = member.getId();
        }*/
        String memberID = member.getId();
        if (StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID 不正确");
            }
        }
        if (StringUtils.isNotBlank(memberID) && !memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        if (StringUtils.isNotBlank(memberID)) {
            member = memberService.selectById(memberID);
        }
        map.put("memberID", memberID);
        MemberPeopleDto memberPeopleDto = memberService.queryMemberRealNameStatus1(memberID);

        if (memberPeopleDto != null) {
            Application byId = applicationService.getById(member.getApplicationID());
            if (byId.getEncryptionType().equals("1")) {
                ApiMemberFingerPrintDetail oneMemberFingerPrintDetailByMember = memberFingerPrintService.getOneMemberFingerPrintDetailByMemberID(member.getId());
                if (oneMemberFingerPrintDetailByMember != null) {
                    String code = oneMemberFingerPrintDetailByMember.getCode();
                    if (StringUtils.isNotBlank(memberPeopleDto.getIdNumber())) {
                        String idNumber = Quantum.sessionKeyDecrypt(code, memberPeopleDto.getIdNumber(), member.getId());
                        DataLog dataLog = new DataLog();
                        dataLog.setId(IdGenerator.uuid32());
                        dataLog.setSystemDefineID("8a2f462a76b8278c0176cb3373822442");
                        dataLog.setObjectID(member.getId());
                        dataLog.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
                        dataLog.setObjectName(member.getName());
                        dataLog.setOperateType("量子解密");
                        dataLog.setDescription("独立接口:查询会员实名认证的状态对身份证进行量子解密");
                        memberPeopleDto.setIdNumber(idNumber);
                        memberPeopleDto.setCardCode(idNumber);
                        dataLogMapper.insert(dataLog);
                    }
                }
            }
        }

        /*
        MemberDetailDto memberDetailDto = memberService.getOneMemberDetail(map);
        if(memberDetailDto!=null&&memberPeopleDto!=null){
            if (memberDetailDto.getIdStatus()!=null) {
                memberPeopleDto.setIdStatus(memberDetailDto.getIdStatus());
            }
        }*/
        if (memberPeopleDto == null) {
            memberService.updateMemberPeople(memberID);
            memberPeopleDto = new MemberPeopleDto();
            memberPeopleDto.setIdStatus(0);

        }
        if (memberLogDTO != null) {
            String description = String.format("查询会员实名认证的状态");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.queryMemberRealNameStatus);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", memberPeopleDto);
        return MessagePacket.newSuccess(rsMap, "queryMemberRealNameStatus success");
    }

    @ApiOperation(value = "applyRealNameVerify", notes = "标准通用的申请实名认证")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/applyRealNameVerify", produces = {"application/json;charset=UTF-8"})
    public MessagePacket applyRealNameVerify(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(sessionID) && StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "参数异常");
        }
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        } else if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
            }
            member = memberService.selectById(memberID);
        }
        String name = request.getParameter("name");
        if (StringUtils.isBlank(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }
        String idNumber = request.getParameter("idNumber");
        if (StringUtils.isBlank(idNumber)) {
            return MessagePacket.newFail(MessageHeader.Code.idNumberNotNull, "idNumber不能为空");
        }

        /********************************************************************************/
        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID)) {
            City sheng = cityService.selectById(shengID);
            if (sheng == null) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            map.put("shengID", sheng.getId());
            map.put("shengName", sheng.getName());
        }

        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID)) {
            City shi = cityService.selectById(shiID);
            if (shi == null) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            map.put("shiID", shi.getId());
            map.put("shiName", shi.getName());
        }

        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID)) {
            City xian = cityService.selectById(xianID);
            if (xian == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("xianID", xian.getId());
            map.put("xianName", xian.getName());
        }
        String zhenID = request.getParameter("zhenID");
        if (StringUtils.isNotBlank(zhenID)) {
            City zhen = cityService.selectById(zhenID);
            if (zhen == null) {
                return MessagePacket.newFail(MessageHeader.Code.zhenIDNotFound, "zhenID不正确");
            }
            map.put("zhenID", zhen.getId());
            map.put("zhenName", zhen.getName());
        }
        String cunID = request.getParameter("cunID");
        if (StringUtils.isNotBlank(cunID)) {
            City cun = cityService.selectById(cunID);
            if (cun == null) {
                return MessagePacket.newFail(MessageHeader.Code.zhenIDNotFound, "cunID不正确");
            }
            map.put("cunID", cun.getId());
            map.put("cunName", cun.getName());
        }

        /**********************************************************************************/
        String faceImage = request.getParameter("faceImage");
        String backImage = request.getParameter("backImage");
        String handImage = request.getParameter("handImage");
        String headImage = request.getParameter("headImage");
        String isChinese = request.getParameter("isChinese");


        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("idNumber", idNumber);
        String peopleID = memberService.selectPeoleID(idNumber);
        if (StringUtils.isNotBlank(peopleID)) {
            return MessagePacket.newFail(MessageHeader.Code.idNumberNotNull, "idNumber已经存在");
        }
        if (member != null&&memberLogDTO!=null) {
            peopleID = memberService.addPeople(member.getApplicationID(), memberLogDTO.getCompanyId(), name, idNumber, faceImage, backImage, handImage, headImage, StringUtils.isNotBlank(isChinese) ? Integer.parseInt(isChinese) : 1, 0, null, member.getId());
        }
        People people = peopleService.getById(peopleID);
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(peopleID)) {
            param.put("idNumber", people.getIdNumber());
            param.put("peopleID", peopleID);
            if (member != null) {
                param.put("id", member.getId());
            }
            param.put("name", name);
            param.put("idStatus", 1);
            memberService.updateMember(param);
        }
        //赠送积分
        if (member != null) {
            member = memberService.selectById(member.getId());
        }
        if (member != null) {
            kingBase.addMemberPoint(Config.MEMBERID_OBJECTDEFINEID, member.getId(), member.getName(), "实名认证", member.getSiteID(), member, 1, 1);
        }
        if (memberLogDTO != null) {
            String description = String.format("标准通用的申请实名认证");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.applyRealNameVerify);
        }
        return MessagePacket.newSuccess("applyRealNameVerify success");
    }

    @ApiOperation(value = "getOneMemberDetail", notes = "获取一个会员的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/getOneMemberDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOneMemberDetail(HttpServletRequest request, ModelMap map) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        String memberID = request.getParameter("memberID");
        if (Config.SafeGrade) {
            String timeStamp = request.getParameter("timeStamp");
            String encryptString = request.getParameter("encryptString");
            Map<String, Object> map1 = kingBase.base64EncryptStringNew(request,timeStamp, encryptString);
            if (!map1.get("result").equals("通过")) {
                String result = map1.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }

        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//            member=memberService.selectById(sessionID);
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        } else {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogin memberLogin = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, member.getApplicationID()));


        String recommendCode = request.getParameter("recommendCode");

        if (StringUtils.isBlank(memberID) && StringUtils.isBlank(recommendCode)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "memberID和recommendCode不能同时为空");
        }

        if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
            }
            map.put("memberID", memberID);
        } else {
            if (StringUtils.isNotBlank(recommendCode)) {
                if (!NumberUtils.isNumeric(recommendCode)) {
                    return MessagePacket.newFail(MessageHeader.Code.numberNot, "recommendCode 请输入数字");
                }
                map.put("recommendCode", recommendCode);
            }
        }

        MemberDetailDto memberDetailDto = memberService.getOneMemberDetail(map);

        // if (memberLogDTO != null) {
        //     String description = String.format("获取一个会员的信息");
        //     sendMessageService.sendMemberLogMessage(sessionID, description, request,
        //             Memberlog.MemberOperateType.getOneMemberDetail);
        // }
        if (memberDetailDto != null) {
            //加个返回值， myMemberDocument： 就是去 获取 这个会员 对应的 memberDocument 的记录个数 （isValid=1）
            if (memberDetailDto.getMemberID()!=null) {
                Integer memberDocument = memberDocumentService.countByMemberID(memberDetailDto.getMemberID());
               memberDetailDto.setMyMemberDocument(memberDocument);
            }
            //增加会员统计字段
            MemberStatisticsDto statisticsDto = memberStatisticsService.getByMemID(memberDetailDto.getMemberID());
            if (statisticsDto != null) {
                Integer articleGetPraiseTimes = 0, storyGetPraiseTimes = 0, documentGetPraiseTimes = 0, selfGetPraiseTimes = 0, discussGetPraiseTimes = 0;
                if (statisticsDto.getArticleGetPraiseTimes() != null) {
                    memberDetailDto.setArticleGetPraiseTimes(statisticsDto.getArticleGetPraiseTimes());
                    articleGetPraiseTimes = statisticsDto.getArticleGetPraiseTimes();
                }
                if (statisticsDto.getStoryGetPraiseTimes() != null) {
                    memberDetailDto.setStoryGetPraiseTimes(statisticsDto.getStoryGetPraiseTimes());
                    storyGetPraiseTimes = statisticsDto.getStoryGetPraiseTimes();
                }
                if (statisticsDto.getDocumentGetPraiseTimes() != null) {
                    memberDetailDto.setDocumentGetPraiseTimes(statisticsDto.getDocumentGetPraiseTimes());
                    documentGetPraiseTimes = statisticsDto.getDocumentGetPraiseTimes();
                }
                if (statisticsDto.getSelfGetPraiseTimes() != null) {
                    memberDetailDto.setSelfGetPraiseTimes(statisticsDto.getSelfGetPraiseTimes());
                    selfGetPraiseTimes = statisticsDto.getSelfGetPraiseTimes();
                }
                if (statisticsDto.getDiscussGetPraiseTimes() != null) {
                    memberDetailDto.setDiscussGetPraiseTimes(statisticsDto.getDiscussGetPraiseTimes());
                    discussGetPraiseTimes = statisticsDto.getDiscussGetPraiseTimes();
                }
                memberDetailDto.setGetPraiseTotal(articleGetPraiseTimes + storyGetPraiseTimes + documentGetPraiseTimes + selfGetPraiseTimes + discussGetPraiseTimes);

            }
            String graduationYearStr = "未知";
            if (memberDetailDto.getGraduationYear() != null) {
                switch (memberDetailDto.getGraduationYear().intValue()) {
                    case 1:
                        graduationYearStr = "小学一年级";
                        break;
                    case 2:
                        graduationYearStr = "小学二年级";
                        break;
                    case 3:
                        graduationYearStr = "小学三年级";
                        break;
                    case 4:
                        graduationYearStr = "小学四年级";
                        break;
                    case 5:
                        graduationYearStr = "小学五年级";
                        break;
                    case 6:
                        graduationYearStr = "小学六年级";
                        break;
                    case 11:
                        graduationYearStr = "初中一年级";
                        break;
                    case 12:
                        graduationYearStr = "初中二年级";
                        break;
                    case 13:
                        graduationYearStr = "初中三年级";
                        break;
                    case 21:
                        graduationYearStr = "高中一年级";
                        break;
                    case 22:
                        graduationYearStr = "高中二年级";
                        break;
                    case 23:
                        graduationYearStr = "高中三年级";
                        break;
                    case 30:
                        graduationYearStr = "大学";
                        break;
                    default:
                        graduationYearStr = memberDetailDto.getGraduationYear().toString();
                        break;
                }
            }
            memberDetailDto.setGraduationYearStr(graduationYearStr);
            if (StringUtils.isNotBlank(memberDetailDto.getRankID())) {
                String rankName = rankService.getNameById(memberDetailDto.getRankID());
                memberDetailDto.setRankName(rankName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getMajorID())) {
                String majorName = majorService.getNameById(memberDetailDto.getMajorID());
                memberDetailDto.setMajorName(majorName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getCreditRankID())) {
                // CreditRankDto creditRankDto = creditRankService.getDetailById(memberDetailDto.getCreditRankID());
                CreditRankDto creditRankDto = creditRankService.getOneCreditRankDetailByID(memberDetailDto.getCreditRankID());
                if (creditRankDto != null) {
                    memberDetailDto.setCreditRankName(creditRankDto.getName());
                }
            }
            if (StringUtils.isNotBlank(memberDetailDto.getTitleID())) {
                String categoryName = categoryService.getNameById(memberDetailDto.getTitleID());
                memberDetailDto.setTitleName(categoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getParentGroupCategoryID())) {
                String categoryName = categoryService.getNameById(memberDetailDto.getParentGroupCategoryID());
                memberDetailDto.setParentGroupCategoryName(categoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getAgeCategoryID())) {
                String categoryName = categoryService.getNameById(memberDetailDto.getAgeCategoryID());
                memberDetailDto.setAgeCategoryName(categoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getIndustryCategoryID())) {
                String industryCategoryName = categoryService.getNameById(memberDetailDto.getIndustryCategoryID());
                memberDetailDto.setIndustryCategoryName(industryCategoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getJobCategoryID())) {
                String jobCategoryName = categoryService.getNameById(memberDetailDto.getJobCategoryID());
                memberDetailDto.setJobCategoryName(jobCategoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getJobYearCategoryID())) {
                String jobYearCategoryName = categoryService.getNameById(memberDetailDto.getJobYearCategoryID());
                memberDetailDto.setJobYearCategoryName(jobYearCategoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getProfessionalCategoryID())) {
                String professionalCategoryName = categoryService.getNameById(memberDetailDto.getProfessionalCategoryID());
                memberDetailDto.setProfessionalCategoryName(professionalCategoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getEducationCategoryID())) {
                String educationCategoryName = categoryService.getNameById(memberDetailDto.getEducationCategoryID());
                memberDetailDto.setEducationCategoryName(educationCategoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getBodyCategoryID())) {
                String bodyCategoryName = categoryService.getNameById(memberDetailDto.getBodyCategoryID());
                memberDetailDto.setBodyCategoryName(bodyCategoryName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getStarDefineID())) {
                String starDefineName = starDefineService.getNameById(memberDetailDto.getStarDefineID());
                memberDetailDto.setStarDefineName(starDefineName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getStarDefineID())) {
                String starDefineName = starDefineService.getNameById(memberDetailDto.getStarDefineID());
                memberDetailDto.setStarDefineName(starDefineName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getEmotionDictionaryID())) {
                Dictionary dictionary = dictionaryService.selectById(memberDetailDto.getEmotionDictionaryID());
                if (dictionary != null) {
                    memberDetailDto.setEmotionDictionaryName(dictionary.getName());
                }
            }
            if (StringUtils.isNotBlank(memberDetailDto.getBloodDIctionaryID())) {
                Dictionary dictionary = dictionaryService.selectById(memberDetailDto.getBloodDIctionaryID());
                if (dictionary != null) {
                    memberDetailDto.setBloodDIctionaryName(dictionary.getName());
                }
            }
            if (StringUtils.isNotBlank(memberDetailDto.getDistrictID())) {
                String districtName = districtService.getNameById(memberDetailDto.getDistrictID());
                memberDetailDto.setDistrictName(districtName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getPropertyID())) {
                String propertyName = propertyService.getNameById(memberDetailDto.getPropertyID());
                memberDetailDto.setPropertyName(propertyName);
            }
            /*
            if (StringUtils.isNotBlank(memberDetailDto.getCompanyID())) {
                String companyName = companyService.selectNameById(memberDetailDto.getCompanyID());
                memberDetailDto.setCompanyName(companyName);
            }*/
            if (StringUtils.isNotBlank(memberDetailDto.getDistributorCompanyID())) {
                String companyName = companyService.selectNameById(memberDetailDto.getDistributorCompanyID());
                memberDetailDto.setDistributorCompanyName(companyName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getDepartmentID())) {
                Department department = departmentService.selectById(memberDetailDto.getDepartmentID());
                if (department != null) {
                    memberDetailDto.setDepartmentName(department.getName());
                }
            }
            if (StringUtils.isNotBlank(memberDetailDto.getSchoolID())) {
                SchoolDto schoolDto = schoolService.selectSchool(memberDetailDto.getSchoolID());
                if (schoolDto != null) {
                    memberDetailDto.setSchoolName(schoolDto.getName());
                }
            }
            if (StringUtils.isNotBlank(memberDetailDto.getRecommendID())) {
                String memberName = memberService.getNameById(memberDetailDto.getRecommendID());
                memberDetailDto.setRecommendName(memberName);
            }
            if (StringUtils.isNotBlank(memberDetailDto.getServiceMemberID())) {
                String memberName = memberService.getNameById(memberDetailDto.getServiceMemberID());
                memberDetailDto.setServiceMemberName(memberName);
            }
            if (StringUtils.isNotBlank(sessionID)) {
                String checkMeFollow = request.getParameter("checkMeFollow");
                if (StringUtils.isNotBlank(checkMeFollow)) {
                    map.clear();
                    map.put("followID", member.getId());
                    map.put("memberID", memberDetailDto.getMemberID());
                    Integer isMeFans = memberFollowService.getISFollowMe(map);
                    memberDetailDto.setIsMeFans(isMeFans > 0 ? 1 : 0);
                    map.clear();
                    map.put("followID", memberDetailDto.getMemberID());
                    map.put("memberID", member.getId());
                    Integer isMeFollow = memberFollowService.getISMeFollow(map);
                    memberDetailDto.setIsMeFollow(isMeFollow > 0 ? 1 : 0);
                }
                String checkHeFollow = request.getParameter("checkHeFollow");
                if (StringUtils.isNotBlank(checkHeFollow)) {
                    memberDetailDto.setMemberFollowTotal(memberFollowService.getFollowTotal(memberDetailDto.getMemberID()));
                    memberDetailDto.setMemberFansTotal(memberFollowService.getFansTotal(memberDetailDto.getMemberID()));
                }
                String checkMeFriend = request.getParameter("checkMeFriend");
                if (StringUtils.isNotBlank(checkMeFriend)) {
                    map.clear();
                    map.put("toMemberID", memberDetailDto.getMemberID());
                    map.put("memberID", member.getId());
                    memberDetailDto.setIsMeFriend(friendService.checkOneFriendOne(map));
                }
                String checkMeBrowse = request.getParameter("checkMeBrowse");
                if (StringUtils.isNotBlank(checkMeBrowse)) {
                    memberDetailDto.setBrowseTotal(browseService.getBrowseCount(member.getId()));
                }
                String checkHeBrowse = request.getParameter("checkHeBrowse");
                if (StringUtils.isNotBlank(checkHeBrowse)) {
                    memberDetailDto.setMemberBrowseTotal(browseService.getBrowseCount(memberDetailDto.getMemberID()));
                }
                String checkMeCollect = request.getParameter("checkMeCollect");
                if (StringUtils.isNotBlank(checkMeCollect)) {
                    memberDetailDto.setMemberCollectCompanyTotal(collectService.getMyCollectCompanyTotal(member.getId()));
                    memberDetailDto.setStoryNumber(collectService.getMyCollectStoryTotal(member.getId()));
                }
                String checkMeTrain = request.getParameter("checkMeTrain");
                if (StringUtils.isNotBlank(checkMeTrain)) {
                    map.clear();
                    map.put("joinMemberID", member.getId());
                    map.put("status", 10);
                    memberDetailDto.setMyMemberTrainTotal(memberTrainService.getCountMemberTrain(map));
                }
                String checkMeBuyRead = request.getParameter("checkMeBuyRead");
                if (StringUtils.isNotBlank(checkMeBuyRead)) {
                    map.clear();
                    map.put("functionType", 4);
                    map.put("status", 2);
                    map.put("memberID", member.getId());
                    List<MemberBuyReadListDto> listDtoList = memberBuyReadService.getMemberBuyReadList(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setMyBuyReadStoryTotal4(listDtoList.size());
                    }
                    map.clear();
                    map.put("functionType", 9);
                    map.put("memberID", member.getId());
                    map.put("status", 2);
                    listDtoList = memberBuyReadService.getMemberBuyReadList(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setMyBuyReadStoryTotal9(listDtoList.size());
                    }
                    map.clear();
                    map.put("functionType", 12);
                    map.put("memberID", member.getId());
                    map.put("status", 2);
                    listDtoList = memberBuyReadService.getMemberBuyReadList(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setMyBuyReadStoryTotal12(listDtoList.size());
                    }
                    map.clear();
                    map.put("functionType", 13);
                    map.put("memberID", member.getId());
                    map.put("status", 2);
                    listDtoList = memberBuyReadService.getMemberBuyReadList(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setMyBuyReadStoryTotal13(listDtoList.size());
                    }
                }
                String checkArthor = request.getParameter("checkArthor");
                if (StringUtils.isNotBlank(checkArthor)) {
                    map.clear();
                    map.put("functionType", 4);
                    map.put("memberID", member.getId());
                    List<OutStoryDto> listDtoList = storyService.getOutStoryDtoListByMap(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setStoryNumber4(listDtoList.size());
                    }
                    map.clear();
                    map.put("functionType", 9);
                    map.put("memberID", member.getId());
                    listDtoList = storyService.getOutStoryDtoListByMap(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setStoryNumber9(listDtoList.size());
                    }
                    map.clear();
                    map.put("functionType", 12);
                    map.put("memberID", member.getId());
                    listDtoList = storyService.getOutStoryDtoListByMap(map);
                    if (!listDtoList.isEmpty()) {
                        memberDetailDto.setStoryNumber12(listDtoList.size());
                    }
                }
                String checkLink = request.getParameter("checkLink");
                if (StringUtils.isNotBlank(checkLink)) {
                    Member member1 = memberService.selectById(member.getId());
                    if (member1 != null) {
                        memberDetailDto.setLinkMemberID(member1.getLinkMemberID());
                        memberDetailDto.setLinkChain(member1.getLinkChain());
                        member1 = memberService.selectById(member1.getLinkMemberID());
                        if (member1 != null) {
                            memberDetailDto.setLinkMemberName(member1.getName());
                            memberDetailDto.setLinkMemberAvatar(member1.getAvatarURL());
                        }
                    }
                }
                String checkMePraise = request.getParameter("checkMePraise");
                if (StringUtils.isNotBlank(checkMePraise)) {
                    if (!NumberUtils.isNumeric(checkMePraise))
                        return MessagePacket.newFail(MessageHeader.Code.numberNot, "checkMePraise 请输入数字");
                    map.clear();
                    map.put("toMemberID", memberDetailDto.getMemberID());
                    map.put("memberID", member.getId());
                    Integer isPraise = praiseService.checkOnePraise(map);
                    memberDetailDto.setIsMePraise(isPraise > 0 ? isPraise : 0);
                }
            }
            //判断这个会员是否是VIP
            map.clear();
            map.put("memberID", memberDetailDto.getMemberID());
            map.put("majorID", "ff808081798df2a201798e518e7e008b");
            memberDetailDto.setMemberIsVIP(memberMajorService.IsVIP(map));

            //判断这个会员是否是admin
            map.clear();
            map.put("memberID", memberDetailDto.getMemberID());
            map.put("majorID", "ff8080818498d2ff0184ae2c31d2000b");
            memberDetailDto.setMemberIsAdmin(memberMajorService.IsVIP(map));

            //判断这个会员是否是作者
            map.clear();
            map.put("memberID", memberDetailDto.getMemberID());
            map.put("majorID", "ff8080818007d51101800d05f94f0035");
            memberDetailDto.setMemberIsAuthor(memberMajorService.IsVIP(map));

            //判断这个会员是否是永久会员
            map.clear();
            map.put("memberID", memberDetailDto.getMemberID());
            map.put("majorID", "ff8080817d6f50e1017d88c9af9b0048");
            memberDetailDto.setMemberIsForeverVIP(memberMajorService.IsVIP(map));
        }
        if (member != null && StringUtils.isNotBlank(memberID)) {
            if (memberID.equals(member.getName())) {
                map.clear();
                map.put("id", memberID);
                map.put("lastLoginTime", TimeTest.getTime());
                memberService.updateMember(map);
            }
        }
        return MessagePacket.newSuccess(memberDetailDto, "getOneMemberDetail success");
    }

    @ApiOperation(value = "saveFormid", notes = "保存一个formid在会员表")
    @RequestMapping(value = "/saveFormid", produces = {"application/json;charset=UTF-8"})
    public MessagePacket saveFormid(HttpServletRequest request, ModelMap map) {
        String formid = request.getParameter("formid");
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
        Map<String, Object> rsMap = Maps.newHashMap();
        map.put("id", member.getId());
        map.put("jingdongCode", formid);
        memberService.updateMember(map);
        return MessagePacket.newSuccess(rsMap, "saveFormid success");
    }

    @ApiOperation(value = "getMemberIncomeList", notes = "获取会员收入列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberIncomeList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberIncomeList(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID) && !memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        if (StringUtils.isNotBlank(memberID)) {
            map.put("memberID", memberID);
        } else {
            map.put("memberID", member.getId());
        }
        String beginTime = request.getParameter("beginTime");
        String endTime = request.getParameter("endTime");
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime) && !NumberUtils.isNumeric(sortTypeTime)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入的数字");
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        if (StringUtils.isNotBlank(sortTypeTime)) {
            map.put("sortTypeTime", sortTypeTime);
        }
        if (StringUtils.isNotBlank(beginTime)) {
            map.put("beginTime", beginTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            map.put("endTime", endTime);
        }
        List<MemberIncomeDto> list = memberService.getMemberIncomeList(map);
        if (!list.isEmpty()) {
            PageInfo<MemberIncomeDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
            for (MemberIncomeDto dto : list) {
                MemberOrder memberOrder = memberOrderService.findById(dto.getObjectID());
                if (memberOrder != null) {
                    Member member1 = memberService.selectById(memberOrder.getMemberID());
                    if (member1 != null) {
                        dto.setMemberName(member1.getName());
                    }
                    List<MemberOrderGoods> lists = memberOrderGoodsService.getMemberOrderGoodsByMemberOrderID(memberOrder.getId());
                    if (!lists.isEmpty()) {
                        GoodsShop goodsShop = goodsShopService.getGoodsShopByID(lists.get(0).getGoodsShopID());
                        if (goodsShop != null) {
                            dto.setGoodsShopName(goodsShop.getName());
                        }

                    }
                }
            }
        }
        if (memberLogDTO != null) {
            String description = String.format("获取会员收入列表");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberIncomeList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberIncomeList success");
    }

    @ApiOperation(value = "getMemberIncomeDetail", notes = "获取会员收入的详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberIncomeDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberIncomeDetail(HttpServletRequest request, ModelMap map) {
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
        String memberIncomeID = request.getParameter("memberIncomeID");
        if (StringUtils.isBlank(memberIncomeID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIncomeIDNotNull, "memberIncomeID不能为空");
        }
        MemberIncome memberIncome = memberService.selectMemberIncomeById(memberIncomeID);
        if (memberIncome == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberIncomeID不正确");
        }
        if (memberLogDTO != null) {
            String description = String.format("获取会员收入的详细信息");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberIncomeDetail);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberIncomeID", memberIncome.getId());
        rsMap.put("name", memberIncome.getName());
        rsMap.put("amount", memberIncome.getAmount());
        rsMap.put("fee", memberIncome.getFee());
        rsMap.put("description", memberIncome.getDescription());
        rsMap.put("operateTime", memberIncome.getOperateTime());
        rsMap.put("operateTimeStr", TimeTest.toDateTimeFormat(memberIncome.getOperateTime()));
        rsMap.put("approveStatus", memberIncome.getApproveStatus());
        rsMap.put("approveTime", memberIncome.getApproveTime());
        rsMap.put("approveTimeStr", TimeTest.toDateTimeFormat(memberIncome.getApproveTime()));
        rsMap.put("approveType", memberIncome.getApproveType());
        rsMap.put("approveDescription", memberIncome.getApproveDescription());
        rsMap.put("giveAmount", memberIncome.getGiveAmount());
        rsMap.put("giveTime", memberIncome.getGiveTime());
        rsMap.put("giveTimeStr", TimeTest.toDateTimeFormat(memberIncome.getGiveTime()));
        rsMap.put("orderCode", memberIncome.getOrderCode());
        rsMap.put("objectDefineID", memberIncome.getObjectDefineID());
        rsMap.put("objectID", memberIncome.getObjectID());
        rsMap.put("objectName", memberIncome.getObjectName());
        rsMap.put("status", memberIncome.getStatus());
        if (StringUtils.isNotBlank(memberIncome.getObjectDefineID())) {
            rsMap.put("objectDefineName", objectDefineService.getNameById(memberIncome.getObjectDefineID()));
        } else {
            rsMap.put("objectDefineName", null);
        }
        return MessagePacket.newSuccess(rsMap, "getMemberIncomeDetail success");
    }

    @ApiOperation(value = "getOneMemberExpenditureDetail", notes = "获取一个会员支出详细记录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getOneMemberExpenditureDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOneMemberExpenditureDetail(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        /*Member member = memberService.selectById(sessionID);*/
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberExpenditureID = request.getParameter("memberExpenditureID");
        if (StringUtils.isBlank(memberExpenditureID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberExpenditureIDNotNull, "memberExpenditureID不能为空");
        }
        MemberExpenditure memberExpenditure = memberService.selectMemberExpenditureById(memberExpenditureID);
        if (memberExpenditure == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberExpenditureIDNotFound, "memberExpenditureID不正确");
        }
        if (memberLogDTO != null) {
            String description = String.format("获取一个会员支出详细记录");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getOneMemberExpenditureDetail);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberExpenditureID", memberExpenditure.getId());
        rsMap.put("companyID", memberExpenditure.getCompanyID());
        if (StringUtils.isNotBlank(memberExpenditure.getCompanyID())) {
            rsMap.put("companyName", companyService.selectNameById(memberExpenditure.getCompanyID()));
        } else {
            rsMap.put("companyName", null);
        }
        rsMap.put("shopID", memberExpenditure.getShopID());
        if (StringUtils.isNotBlank(memberExpenditure.getShopID())) {
            rsMap.put("shopName", shopService.getNameById(memberExpenditure.getShopID()));
        } else {
            rsMap.put("shopName", null);
        }
        rsMap.put("memberID", memberExpenditure.getMemberID());
        if (StringUtils.isNotBlank(memberExpenditure.getMemberID())) {
            rsMap.put("memberName", memberService.getNameById(memberExpenditure.getMemberID()));
        } else {
            rsMap.put("memberName", null);
        }
        rsMap.put("name", memberExpenditure.getName());
        rsMap.put("actionType", memberExpenditure.getActionType());
        rsMap.put("actionTime", memberExpenditure.getActionTime());
        rsMap.put("amount", memberExpenditure.getAmount());
        rsMap.put("objectDefineID", memberExpenditure.getObjectDefineID());
        rsMap.put("objectID", memberExpenditure.getObjectID());
        rsMap.put("objectName", memberExpenditure.getObjectName());
        if (StringUtils.isNotBlank(memberExpenditure.getObjectDefineID())) {
            rsMap.put("objectDefineName", objectDefineService.getNameById(memberExpenditure.getObjectDefineID()));
        } else {
            rsMap.put("objectDefineName", null);
        }
        return MessagePacket.newSuccess(rsMap, "getOneMemberExpenditureDetail success");
    }

    @ApiOperation(value = "getMemberExpenditureList", notes = "获取会员支出列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberExpenditureList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberExpenditureList(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        /*Member member = memberService.selectById(sessionID);*/
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID) && !memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (companyService.selectById(companyID) == null) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
            }
            map.put("companyID", companyID);
        }
        String employeeID = request.getParameter("employeeID");
        if (StringUtils.isNotBlank(employeeID)) {
            if (employeeService.selectById(employeeID) == null) {
                return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "employeeID不正确");
            }
            map.put("employeeID", employeeID);
        }
        String amountBegin = request.getParameter("amountBegin");
        if (StringUtils.isNotBlank(amountBegin)) {
            map.put("amountBegin", Double.valueOf(amountBegin));
        }
        String amountEnd = request.getParameter("amountEnd");
        if (StringUtils.isNotBlank(amountEnd)) {
            map.put("amountEnd", Double.valueOf(amountEnd));
        }
        String actionType = request.getParameter("actionType");
        if (StringUtils.isNotBlank(actionType) && !NumberUtils.isNumeric(actionType)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "actionType请输入数字");
        }
        String beginTime = request.getParameter("beginTime");
        String endTime = request.getParameter("endTime");
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime) && !NumberUtils.isNumeric(sortTypeTime)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入的数字");
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        String sortTypeAmount = request.getParameter("sortTypeAmount");
        if (StringUtils.isNotBlank(sortTypeAmount)) {
            if (!NumberUtils.isNumeric(sortTypeAmount)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeAmount请输入的数字");
            }
            map.put("sortTypeAmount", sortTypeAmount);
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        if (StringUtils.isNotBlank(sortTypeTime)) {
            map.put("sortTypeTime", sortTypeTime);
        }
        if (StringUtils.isNotBlank(memberID)) {
            map.put("memberID", memberID);
        } else {
            map.put("memberID", member.getId());
        }
        if (StringUtils.isNotBlank(actionType)) {
            map.put("actionType", actionType);
        }
        if (StringUtils.isNotBlank(beginTime)) {
            map.put("beginTime", beginTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            map.put("endTime", endTime);
        }
        List<MemberExpenditureDto> list = memberService.getMemberExpenditureList(map);
        if (!list.isEmpty()) {
            PageInfo<MemberExpenditureDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (memberLogDTO != null) {
            String description = String.format("获取会员支出列表");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberExpenditureList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberExpenditureList success");
    }

    @ApiOperation(value = "getMemberLinkUPList", notes = "获取会员接点上级列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberLinkUPList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberLinkUPList(HttpServletRequest request, ModelMap map) {
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
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member1 = memberService.selectById(memberID);
        if (member1 == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        List<ApiMemberDto> list = null;
        String str = member1.getLinkChain();
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        Map<String, Object> rsMap = Maps.newHashMap();
        if (StringUtils.isEmpty(str)) {
            MessagePage messagePage = new MessagePage(page, list);
            rsMap.put("data", messagePage);
            return MessagePacket.newSuccess(rsMap, "getMemberLinkUPList success");
        }
        String[] strs = str.split(",");
        map.put("strs", strs);
        if (StringUtils.isNotBlank(member1.getApplicationID())) map.put("applicationID", member1.getApplicationID());
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);
        String sortTypeCode = request.getParameter("sortTypeCode");
        if (StringUtils.isNotBlank(sortTypeCode)) {
            if (!NumberUtils.isNumeric(sortTypeCode)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeCode请输入的数字");
            }
            map.put("sortTypeCode", sortTypeCode);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        list = memberService.getLinkUPListByMap(map);
        if (!list.isEmpty() && list.size() > 0) {
            PageInfo<ApiMemberDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        String description = String.format("获取会员接点上级列表");
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberLinkUPList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberLinkUPList success");
    }

    @ApiOperation(value = "getMemberLinkList", notes = "获取会员接点下级列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberLinkList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberLinkList(HttpServletRequest request, ModelMap map) {
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
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member1 = memberService.selectById(memberID);
        if (member1 == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        List<ApiMemberDto> list = null;
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        Map<String, Object> rsMap = Maps.newHashMap();
        map.put("linkMemberID", memberID);
        if (StringUtils.isNotBlank(member1.getApplicationID())) map.put("applicationID", member1.getApplicationID());
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);
        String sortTypeCode = request.getParameter("sortTypeCode");
        if (StringUtils.isNotBlank(sortTypeCode)) {
            if (!NumberUtils.isNumeric(sortTypeCode)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeCode请输入的数字");
            }
            map.put("sortTypeCode", sortTypeCode);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        list = memberService.getLinkUPListByMap(map);
        if (!list.isEmpty() && list.size() > 0) {
            PageInfo<ApiMemberDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        String description = String.format("获取会员接点下级列表");
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberLinkList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberLinkList success");
    }

    @ApiOperation(value = "getMemberRecommendUPList", notes = "获取会员的推荐上级列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberRecommendUPList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberRecommendUPList(HttpServletRequest request, ModelMap map) {
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
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member1 = memberService.selectById(memberID);
        if (member1 == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        List<ApiMemberDto> list = null;
        String str = member1.getRecommandChain();
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        Map<String, Object> rsMap = Maps.newHashMap();
        if (StringUtils.isEmpty(str)) {
            MessagePage messagePage = new MessagePage(page, list);
            rsMap.put("data", messagePage);
            return MessagePacket.newSuccess(rsMap, "getMemberRecommendUPList success");
        }
        String[] strs = str.split(",");
        map.put("strs", strs);
        if (StringUtils.isNotBlank(member1.getApplicationID())) map.put("applicationID", member1.getApplicationID());
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);
        String sortTypeCode = request.getParameter("sortTypeCode");
        if (StringUtils.isNotBlank(sortTypeCode)) {
            if (!NumberUtils.isNumeric(sortTypeCode)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeCode请输入的数字");
            }
            map.put("sortTypeCode", sortTypeCode);
        }
        list = memberService.getLinkUPListByMap(map);
        if (!list.isEmpty() && list.size() > 0) {
            PageInfo<ApiMemberDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        String description = String.format("获取会员的推荐上级列表");
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberRecommendUPList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberRecommendUPList success");
    }

    @ApiOperation(value = "getMemberRecommendList", notes = "获取会员的推荐会员列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberRecommendList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberRecommendList(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member1 = memberService.selectById(memberID);
        if (member1 == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        String distributorCompanyID = request.getParameter("distributorCompanyID");//服务商公司ID，可以为空
        if (StringUtils.isNotBlank(distributorCompanyID) && StringUtils.isBlank(companyService.getIsValidIdBy(distributorCompanyID))) {
            return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "distributorCompanyID不正确");
        }
        map.put("distributorCompanyID", distributorCompanyID);
        String serviceMemberID = request.getParameter("serviceMemberID");//服务会员，可以为空
        if (StringUtils.isNotBlank(serviceMemberID) && !memberService.checkMemberId(serviceMemberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "serviceMemberID不正确");
        }
        map.put("serviceMemberID", serviceMemberID);
        List<ApiMemberDto> list = null;
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        Map<String, Object> rsMap = Maps.newHashMap();
        map.put("recommandID", memberID);
        if (StringUtils.isNotBlank(member1.getApplicationID())) map.put("applicationID", member1.getApplicationID());
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) map.put("beginTime", beginTime);
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) map.put("endTime", endTime);
        String sortTypeCode = request.getParameter("sortTypeCode");
        if (StringUtils.isNotBlank(sortTypeCode)) {
            if (!NumberUtils.isNumeric(sortTypeCode)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeCode请输入的数字");
            }
            map.put("sortTypeCode", sortTypeCode);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        list = memberService.getLinkUPListByMap(map);
        if (!list.isEmpty() && list.size() > 0) {
            PageInfo<ApiMemberDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        String description = String.format("获取会员的推荐会员列表");
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberRecommendList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberRecommendList success");
    }

    @ApiOperation(value = "memberBandingPhone", notes = "会员绑定手机号")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/memberBandingPhone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberBandingPhone(HttpServletRequest request, ModelMap map) {
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
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号不能为空");
        }
        String verifyCode = request.getParameter("verifyCode");
        if (StringUtils.isBlank(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotNull, "验证码不能为空");
        }
        String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.BING_PHONE_AUTH_CODE, phone));
        if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
        }
        String password = request.getParameter("password");
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        if (Pattern.matches(REGEX_MOBILE, member.getLoginName()) || StringUtils.isNotBlank(password)) {
            param.put("loginName", phone);
        }
        if (StringUtils.isNotBlank(password)) {
            param.put("password", password);
        }
        param.put("phone", phone);
        memberService.updateMember(param);
        redisTemplate.delete(String.format("%s_%s", CacheContant.BING_PHONE_AUTH_CODE, phone));
        member = memberService.selectById(member.getId());
        //赠送积分
        kingBase.addMemberPoint(Config.MEMBERID_OBJECTDEFINEID, member.getId(), member.getName(), "绑定手机号", member.getSiteID(), member, 1, 1);
        if (memberLogDTO != null) {
            String description = String.format("会员绑定手机号");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.memberBandingPhone);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "memberBandingPhone success");
    }

    @ApiOperation(value = "removeMemberRealName", notes = "独立接口：会员取消自己的实名认证")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/removeMemberRealName", produces = {"application/json;charset=UTF-8"})
    public MessagePacket removeMemberRealName(HttpServletRequest request, ModelMap map) {
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
        MemberDetail memberDetail = memberService.getMemberDetail(member.getId());
        String peopleID = memberDetail.getPeopleID();
        if (StringUtils.isNotBlank(peopleID)) {
            Integer status = memberService.selectPeopleApproveStatus(peopleID);
            if (status != 0) {
                return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "只能取消未审核的实名认证信息！");
            }
            memberService.updateMemberPeople(member.getId());
            memberService.deletePeople(peopleID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "未查询到该会员的实名信息！");
        }
        if (memberLogDTO != null) {
            String description = String.format("会员取消自己的实名认证");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.removeMemberRealName);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("deleteTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "removeMemberRealName success");
    }

    @ApiOperation(value = "updateMyMemberSchool", notes = "设置我的会员学校")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberSchool", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberSchool(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String schoolID = request.getParameter("schoolID");
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
        if (StringUtils.isBlank(schoolID)) {
            return MessagePacket.newFail(MessageHeader.Code.schoolIDIsNull, "学校不能为空");
        }
        if (StringUtils.isNotBlank(schoolID) && StringUtils.isBlank(schoolService.checkIdIsValid(schoolID))) {
            return MessagePacket.newFail(MessageHeader.Code.schoolIDNotFound, "学校不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("schoolID", schoolID);
        memberService.updateMyMemberSchool(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员学校");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberSchool);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        Timestamp time = TimeTest.getTime();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberSchool success");
    }

    @ApiOperation(value = "updateMyMemberGraduationYear", notes = "设置我的会员毕业年级")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberGraduationYear", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberGraduationYear(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String graduationYear = request.getParameter("graduationYear");
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
        if (StringUtils.isBlank(graduationYear)) {
            return MessagePacket.newFail(MessageHeader.Code.graduationYearIsNull, "毕业年级不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("graduationYear", graduationYear);
        memberService.updateMyMemberGraduationYear(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员毕业年级");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberGraduationYear);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        Timestamp time = TimeTest.getTime();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberGraduationYear success");
    }

    @ApiOperation(value = "resetMemberPassword", notes = "忘记密码找回密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "deviceID", value = "设备ID", dataType = "String")
    })
    @RequestMapping(value = "/resetMemberPassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket resetMemberPassword(HttpServletRequest request, ModelMap map) {
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号不能为空");
        }
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isNotBlank(deviceID) && !deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }

        if (phone != null) {
            Map<String, Object> requestMap = Maps.newHashMap();
            requestMap.put("Isphone",true);
            Map<String, Object> adressIp = kingBase.getAdressIp(request, site.getApplicationID(), null,  phone, requestMap);
            if (!adressIp.get("result").equals("通过")) {
                String result = adressIp.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        else {
            Map<String, Object> adressIp = kingBase.getAdressIp(request, site.getApplicationID(), null,  null, null);
            if (!adressIp.get("result").equals("通过")) {
                String result = adressIp.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }

        String password = request.getParameter("password");
        if (StringUtils.isBlank(password)) {
            return MessagePacket.newFail(MessageHeader.Code.passwordNotNull, "password不能为空");
        }
        if (!"8a2f462a78ecd2fa0178eedb1d280510".equals(site.getApplicationID())) {
            String authCode = request.getParameter("authCode");
            if (StringUtils.isBlank(authCode)) {
                return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotNull, "验证码不能为空");
            }
            String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FIND_MEMBER_PASS, phone));
            if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(authCode)) {
                return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
            }
        }

        stringRedisTemplate.delete(String.format("%s_%s", CacheContant.LOGIN_AUTH_CODE, phone));
        Map<String, Object> param = new HashMap<>();
        param.put("applicationID", site.getApplicationID());
        param.put("phone", phone);
        Member member = memberService.selectByPhoneAndApplicationID(param);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "该手机号会员未找到");
        }
        Map<String, Object> updateParam = new HashMap<>();
        updateParam.put("id", member.getId());
        //前端已经md5加密过不重复加密
        if (password.length() < 32){
            String psd = MD5.encodeMd5(String.format("%s%s", password, Config.ENCODE_KEY));
            updateParam.put("loginPassword", psd);
        }
        else if (password.length() >= 32){
            updateParam.put("loginPassword", password);
        }
        updateParam.put("lastChangePWTime", TimeTest.getTimeStr());
        updateParam.put("PWErrorTimes", 0);
        memberService.updateMember(updateParam);
        redisTemplate.delete(String.format("%s_%s", CacheContant.FIND_MEMBER_PASS, phone));
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "resetMemberPassword success");
    }

    @ApiOperation(value = "memberLogin", notes = "会员登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/memberLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberLogin(HttpServletRequest request, ModelMap map) {
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }

        ApiDeviceDetailDto dto = deviceService.selectByID(deviceID);
        if (dto == null) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isBlank(loginName)) {
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotNull, "loginName不能为空");
        }


        Map<String, Object> requestMap = Maps.newHashMap();
        requestMap.put("Isphone",true);
        Map<String, Object> adressIp = kingBase.getAdressIp(request, site.getApplicationID(), null, loginName, requestMap);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }


        String password = request.getParameter("password");
        if (StringUtils.isBlank(password)) {
            return MessagePacket.newFail(MessageHeader.Code.passwordNotNull, "password不能为空");
        }
        log.info("log" + String.format("time:%s-siteID:%s-loginName:%s-password:%s", TimeTest.getTimeStr(), siteID, loginName, password));


        Map<String, Object> param = new HashMap<>();
        param.put("applicationID", site.getApplicationID());
        param.put("loginName", loginName);
        Member mem = memberService.selectByloginNameAndApplicationID(param);

        if (mem == null) {
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, "登录信息错误");
        }

        if (mem.getIsLock() == 1) {
            return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "会员已被锁定");
        }

        // log.info("log"+mem.toString());
        if (password.length() < 32) {
            String encodePassword = MD5.encodeMd5(String.format("%s%s", password, Config.ENCODE_KEY));
            if (!encodePassword.equals(mem.getLoginPassword())) {
                if (mem.getPWErrorTimes() != null && Integer.valueOf(mem.getPWErrorTimes()) >= 0 && Integer.valueOf(mem.getPWErrorTimes()) <= 3) {
                    Map<String, Object> paramsMap = new HashMap<>();
                    paramsMap.put("PWErrorTimes", mem.getPWErrorTimes() + 1);
                    paramsMap.put("id", mem.getId());
                    memberService.updateMember(paramsMap);
                }
                else {
                    Map<String, Object> paramsMap = new HashMap<>();
                    paramsMap.put("isLock", 1);
                    paramsMap.put("PWErrorTimes", mem.getPWErrorTimes() + 1);
                    paramsMap.put("id", mem.getId());
                    memberService.updateMember(paramsMap);
                }
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, "登录信息错误");
            }
        } else if (password.length() >= 32) {
            if (!password.equals(mem.getLoginPassword())) {

                if (mem.getPWErrorTimes() != null && Integer.valueOf(mem.getPWErrorTimes()) >= 0 && Integer.valueOf(mem.getPWErrorTimes()) <= 3) {
                    Map<String, Object> paramsMap = new HashMap<>();
                    paramsMap.put("PWErrorTimes", mem.getPWErrorTimes() + 1);
                    paramsMap.put("id", mem.getId());
                    memberService.updateMember(paramsMap);
                }
                else {
                    Map<String, Object> paramsMap = new HashMap<>();
                    paramsMap.put("isLock", 1);
                    paramsMap.put("PWErrorTimes", mem.getPWErrorTimes() + 1);
                    paramsMap.put("id", mem.getId());
                    memberService.updateMember(paramsMap);
                }
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, "登录信息错误");
            }
        }

        String pushDeviceID = dto.getPushDeviceID();
        if (StringUtils.isNotBlank(pushDeviceID)) {
            map.clear();
            map.put("code", "JpushAppKey");
            map.put("siteID", siteID);
            String jpushAppKey = parameterSiteService.getValueByMap(map);
            map.put("code", "JpushMasterSecret");
            String jpushMasterSecret = parameterSiteService.getValueByMap(map);
            if (StringUtils.isNotBlank(jpushAppKey) && StringUtils.isNotBlank(jpushMasterSecret)) {
                JPushClient jPushClient = new JPushClient(jpushMasterSecret, jpushAppKey);
                try {
                    DefaultResult defaultResult = jPushClient.updateDeviceTagAlias(pushDeviceID, mem.getId(), null, null);
                    log.info("log" + defaultResult.toString());
                    TagAliasResult result = jPushClient.getDeviceTagAlias(pushDeviceID);
                    log.info("log" + mem.getName() + "别名：" + result.toString());
                } catch (APIConnectionException e) {
                    log.error("log",e);
                } catch (APIRequestException e) {
                    log.error("log",e);
                }
            }
        }
        map.clear();
        map.put("memberID", mem.getId());
        WeixinAppMember weixinAppMember = weixinMemberService.getByMap(map);
        if (weixinAppMember != null) {
            weixinMemberService.updateWeixinAppMember(weixinAppMember);
        }
        ;
        map.put("deviceID", deviceID);
        String memberDeviceID = memberDeviceService.getIdByMap(map);
        if (StringUtils.isBlank(memberDeviceID)) {
            map.put("name", mem.getName() + deviceService.getNameById(deviceID));
            map.put("creator", mem.getId());
            map.put("id", IdGenerator.uuid32());
            memberDeviceService.insertByMap(map);
        }
        //2020-09-27 修改device机器最后登录时间
        deviceService.updateLastLoadTime(deviceID);
        String ipaddr = WebUtil.getRemortIP(request);
        String uuid = IdGenerator.uuid32();
        String description = String.format("会员登录");
        MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), mem.getApplicationID(),
                deviceID, mem.getId(), memberService.getLocationID(ipaddr), mem.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, uuid), mem, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, uuid), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
        sendMessageService.sendMemberLoginMessage(false, uuid, ipaddr, ipaddr, description, Memberlog.MemberOperateType.memberLogin);//
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", uuid);
        rsMap.put("memberID", mem.getId());
        rsMap.put("shortName", mem.getShortName());
        rsMap.put("avatarURL", mem.getAvatarURL());
        rsMap.put("loginName", mem.getLoginName());
        rsMap.put("phone", mem.getPhone());
        rsMap.put("name", mem.getName());
        rsMap.put("recommandCode", mem.getRecommandCode());
        rsMap.put("recommendCode", mem.getRecommendCode());
        rsMap.put("weixinUnionID", mem.getWeixinUnionID());
        rsMap.put("weixinToken", mem.getWeixinToken());
        rsMap.put("loginPassword", mem.getLoginPassword());
        rsMap.put("rankID", mem.getRankID());
        rsMap.put("lastChangePWTim", mem.getLastChangePWTime());
        rsMap.put("isNeedChangePW", mem.getIsNeedChangePW());
        rsMap.put("PWErrorTimes", mem.getPWErrorTimes());

        if (StringUtils.isNotBlank(mem.getRankID())) {
            rsMap.put("rankName", rankService.getNameById(mem.getRankID()));
        } else {
            rsMap.put("rankName", null);
        }
        rsMap.put("majorID", mem.getMajorID());
        if (StringUtils.isNotBlank(mem.getMajorID())) {
            rsMap.put("majorName", majorService.getNameById(mem.getMajorID()));
        } else {
            rsMap.put("majorName", null);
        }

        //新增memberLogin
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        //如果传了，则 去 machine表判断是否存在，不存在就不处理，如果存在的话，就在 redis的 memberLogin 对象里面，增加 machineID 属性，machineName 2个属性
        String machineID = request.getParameter("machineID");
        if (StringUtils.isNotBlank(machineID)) {
            MachineDetail MachineDetail = machineService.getDetail(machineID);
            if (MachineDetail!=null){
                memberLogin.setObjectID(machineID);
                memberLogin.setObjectName(MachineDetail.getName());
            }

        }
        //增加参数versionID，可以为空
        //如果不为空，则登录成功，修改device表的versionlD
        String versionID = request.getParameter("versionID");
        if (StringUtils.isNotBlank(versionID)) {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("deviceID",deviceID);
            deviceMap.put("versionID",versionID);
            deviceService.updateDevicePhone(deviceMap);
        }
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", mem.getId());
        memberLogin.setMemberID(mem.getId());
        map1.put("memberName", mem.getName());
        memberLogin.setMemberName(mem.getName());
        map1.put("memberShortName", mem.getShortName());
        memberLogin.setMemberShortName(mem.getShortName());
        map1.put("memberAvatar", mem.getAvatarURL());
        memberLogin.setMemberAvatar(mem.getAvatarURL());
        if (StringUtils.isNotBlank(loginName)) {
            map1.put("loginName", loginName);
            memberLogin.setLoginName(loginName);
        } else {
            map1.put("loginName", mem.getLoginName());
            memberLogin.setLoginName(mem.getLoginName());
        }
        map1.put("phone", mem.getPhone());
        memberLogin.setPhone(mem.getPhone());
        map1.put("weixinToken", mem.getWeixinToken());
        memberLogin.setWeixinToken(mem.getWeixinToken());
        map1.put("weixinUnionID", mem.getWeixinUnionID());
        memberLogin.setWeixinUnionID(mem.getWeixinUnionID());
        map1.put("recommendCode", mem.getRecommendCode());
        memberLogin.setRecommendCode(mem.getRecommendCode());
        map1.put("recommendID", mem.getRecommandID());
        memberLogin.setRecommendID(mem.getRecommandID());
        map1.put("companyID", mem.getCompanyID());
        memberLogin.setCompanyID(mem.getCompanyID());
        map1.put("shopID", mem.getShopID());
        memberLogin.setShopID(mem.getShopID());
        map1.put("rankID", mem.getRankID());
        memberLogin.setRankID(mem.getRankID());
        map1.put("peopleID", mem.getPeopleID());
        memberLogin.setPeopleID(mem.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, uuid), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        return MessagePacket.newSuccess(rsMap, "memberLogin success");
    }

    @ApiOperation(value = "memberLoginSJ", notes = "独立接口：设计得到会员登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/memberLoginSJ", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberLoginSJ(HttpServletRequest request, ModelMap map) {
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        ApiDeviceDetailDto dto = deviceService.selectByID(deviceID);
        if (dto == null) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isBlank(loginName)) {
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotNull, "loginName不能为空");
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        String password = request.getParameter("password");
        String uuid = "";
        map.put("applicationID", site.getApplicationID());
        map.put("loginName", loginName);
        Member mem = memberService.selectByloginNameAndApplicationID(map);
        if (mem == null) {
            String generatePassword = generatePassword(loginName);
            // 创建一个新的会员
            map.clear();
            String memberID = IdGenerator.uuid32();
            map.put("id", memberID);
            map.put("name", loginName);
            map.put("shortName", loginName);
            map.put("phone", loginName);
            map.put("loginName", loginName);
            map.put("password", generatePassword);
            map.put("applicationID", site.getApplicationID());
            map.put("siteID", siteID);
            map.put("companyID", site.getCompanyID());
            String reCode = memberService.getCurRecommandCode(site.getApplicationID());
            String code = strFormat3(String.valueOf(Integer.valueOf(reCode) + 1));
            map.put("recommendCode", code);
            memberService.insertByMap(map);
            map.clear();
            map.put("name", loginName + deviceService.getNameById(deviceID));
            map.put("creator", memberID);
            map.put("id", IdGenerator.uuid32());
            memberDeviceService.insertByMap(map);
            rsMap.put("memberID", memberID);
            rsMap.put("shortName", loginName);
            rsMap.put("loginName", loginName);
            rsMap.put("phone", loginName);
            rsMap.put("name", loginName);
            rsMap.put("recommandCode", code);
            //2020-09-27 修改device机器最后登录时间
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            uuid = IdGenerator.uuid32();
            String description = String.format("设计得到会员登录");
            MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), site.getApplicationID(),
                    deviceID, memberID, memberService.getLocationID(ipaddr), site.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, uuid), mem, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, uuid), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(true, uuid, ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.memberLoginSJ);
            //新增memberLogin
            MemberLogin memberLogin = new MemberLogin();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("applicationID", site.getApplicationID());
            memberLogin.setApplicationID(site.getApplicationID());
            map1.put("applicationName", site.getApplicationName());
            memberLogin.setApplicationName(site.getApplicationName());
            map1.put("siteID", site.getId());
            memberLogin.setSiteID(site.getId());
            map1.put("siteName", site.getName());
            memberLogin.setSiteName(site.getName());
            map1.put("deviceID", deviceID);
            memberLogin.setDeviceID(deviceID);
            String ip = WebUtil.getRemortIP(request);
            map1.put("ipAddress", ip);
            memberLogin.setIpAddress(ip);
            map1.put("locationID", memberService.getLocationID(ip));
            memberLogin.setLocationID(memberService.getLocationID(ip));
            String userAgent = request.getHeader("User-Agent");
            String browserType = "Unknown";
            if (userAgent != null) {
                if (userAgent.contains("Firefox")) {
                    browserType = "Firefox";
                } else if (userAgent.contains("Chrome")) {
                    browserType = "Chrome";
                } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                    browserType = "Internet Explorer";
                } else if (userAgent.contains("Safari")) {
                    browserType = "Safari";
                } else if (userAgent.contains("Opera")) {
                    browserType = "Opera";
                }
            }
            map1.put("browserType", browserType);
            memberLogin.setBrowserType(browserType);
            map1.put("memberID", mem.getId());
            memberLogin.setMemberID(mem.getId());
            map1.put("memberName", mem.getName());
            memberLogin.setMemberName(mem.getName());
            map1.put("memberShortName", mem.getShortName());
            memberLogin.setMemberShortName(mem.getShortName());
            map1.put("memberAvatar", mem.getAvatarURL());
            memberLogin.setMemberAvatar(mem.getAvatarURL());
            if (StringUtils.isNotBlank(loginName)) {
                map1.put("loginName", loginName);
                memberLogin.setLoginName(loginName);
            } else {
                map1.put("loginName", mem.getLoginName());
                memberLogin.setLoginName(mem.getLoginName());
            }
            map1.put("phone", mem.getPhone());
            memberLogin.setPhone(mem.getPhone());
            map1.put("weixinToken", mem.getWeixinToken());
            memberLogin.setWeixinToken(mem.getWeixinToken());
            map1.put("weixinUnionID", mem.getWeixinUnionID());
            memberLogin.setWeixinUnionID(mem.getWeixinUnionID());
            map1.put("recommendCode", mem.getRecommendCode());
            memberLogin.setRecommendCode(mem.getRecommendCode());
            map1.put("recommendID", mem.getRecommandID());
            memberLogin.setRecommendID(mem.getRecommandID());
            map1.put("companyID", mem.getCompanyID());
            memberLogin.setCompanyID(mem.getCompanyID());
            map1.put("shopID", mem.getShopID());
            memberLogin.setShopID(mem.getShopID());
            map1.put("rankID", mem.getRankID());
            memberLogin.setRankID(mem.getRankID());
            map1.put("peopleID", mem.getPeopleID());
            memberLogin.setPeopleID(mem.getPeopleID());
            memberLogin.setLoginTime(TimeTest.getTime());
            /**
             * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
             * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
             * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
             * 并设置 isWork=1；(感觉没必要用消息队列)
             */
            Employee employee = employeeService.getMemberLogin(memberLogin);
            if (employee != null) {
                if (StringUtils.isNotBlank(employee.getCompanyID())) {
                    memberLogin.setIsWork(1);
                    memberLogin.setWorkEmployeeID(employee.getId());
                    memberLogin.setWorkCompanyID(employee.getCompanyID());
                    memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                    memberLogin.setWorkShopID(employee.getShopID());
                    memberLogin.setWorkJobID(employee.getJobID());
                    memberLogin.setWorkGradeID(employee.getGradeID());
                }
            }
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, uuid), memberLogin, redisTimeOut, TimeUnit.SECONDS);


        } else {
            String passwords = mem.getPassword();
            if (StringUtils.isNotBlank(passwords)) {
                if (StringUtils.isBlank(password)) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotNull, "密码不能为空");
                }
                if (!passwords.equals(password)) {
                    Map<String, Object> paramsMap = new HashMap<>();
                    paramsMap.put("PWErrorTimes", mem.getPWErrorTimes() + 1);
                    memberService.updateMember(paramsMap);
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "密码错误");
                }
            }
            if (mem.getIsLock() == 1) {
                return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "会员已被锁定");
            }
            map.clear();
            map.put("memberID", mem.getId());
            WeixinAppMember weixinAppMember = weixinMemberService.getByMap(map);
            if (weixinAppMember != null) {
                weixinMemberService.updateWeixinAppMember(weixinAppMember);
            }
            map.put("deviceID", deviceID);
            String memberDeviceID = memberDeviceService.getIdByMap(map);
            if (StringUtils.isBlank(memberDeviceID)) {
                map.put("name", mem.getName() + deviceService.getNameById(deviceID));
                map.put("creator", mem.getId());
                map.put("id", IdGenerator.uuid32());
                memberDeviceService.insertByMap(map);
            }
            //2020-09-27 修改device机器最后登录时间
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            uuid = IdGenerator.uuid32();
            String description = String.format("设计得到会员登录");
            MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), mem.getApplicationID(),
                    deviceID, mem.getId(), memberService.getLocationID(ipaddr), mem.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, uuid), mem, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, uuid), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, uuid, ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.memberLoginSJ);
            rsMap.put("memberID", mem.getId());
            rsMap.put("shortName", mem.getShortName());
            rsMap.put("loginName", mem.getLoginName());
            rsMap.put("recommandCode", mem.getRecommandCode());
            rsMap.put("recommendCode", mem.getRecommendCode());
            rsMap.put("phone", mem.getPhone());
            rsMap.put("name", mem.getName());
            rsMap.put("majorID", mem.getMajorID());
            if (StringUtils.isNotBlank(mem.getMajorID())) {
                rsMap.put("majorName", majorService.getNameById(mem.getMajorID()));
            } else {
                rsMap.put("majorName", null);
            }
            //新增memberLogin
            MemberLogin memberLogin = new MemberLogin();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("applicationID", site.getApplicationID());
            memberLogin.setApplicationID(site.getApplicationID());
            map1.put("applicationName", site.getApplicationName());
            memberLogin.setApplicationName(site.getApplicationName());
            map1.put("siteID", site.getId());
            memberLogin.setSiteID(site.getId());
            map1.put("siteName", site.getName());
            memberLogin.setSiteName(site.getName());
            map1.put("deviceID", deviceID);
            memberLogin.setDeviceID(deviceID);
            String ip = WebUtil.getRemortIP(request);
            map1.put("ipAddress", ip);
            memberLogin.setIpAddress(ip);
            map1.put("locationID", memberService.getLocationID(ip));
            memberLogin.setLocationID(memberService.getLocationID(ip));
            String userAgent = request.getHeader("User-Agent");
            String browserType = "Unknown";
            if (userAgent != null) {
                if (userAgent.contains("Firefox")) {
                    browserType = "Firefox";
                } else if (userAgent.contains("Chrome")) {
                    browserType = "Chrome";
                } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                    browserType = "Internet Explorer";
                } else if (userAgent.contains("Safari")) {
                    browserType = "Safari";
                } else if (userAgent.contains("Opera")) {
                    browserType = "Opera";
                }
            }
            map1.put("browserType", browserType);
            memberLogin.setBrowserType(browserType);
            map1.put("memberID", mem.getId());
            memberLogin.setMemberID(mem.getId());
            map1.put("memberName", mem.getName());
            memberLogin.setMemberName(mem.getName());
            map1.put("memberShortName", mem.getShortName());
            memberLogin.setMemberShortName(mem.getShortName());
            map1.put("memberAvatar", mem.getAvatarURL());
            memberLogin.setMemberAvatar(mem.getAvatarURL());
            if (StringUtils.isNotBlank(loginName)) {
                map1.put("loginName", loginName);
                memberLogin.setLoginName(loginName);
            } else {
                map1.put("loginName", mem.getLoginName());
                memberLogin.setLoginName(mem.getLoginName());
            }
            map1.put("phone", mem.getPhone());
            memberLogin.setPhone(mem.getPhone());
            map1.put("weixinToken", mem.getWeixinToken());
            memberLogin.setWeixinToken(mem.getWeixinToken());
            map1.put("weixinUnionID", mem.getWeixinUnionID());
            memberLogin.setWeixinUnionID(mem.getWeixinUnionID());
            map1.put("recommendCode", mem.getRecommendCode());
            memberLogin.setRecommendCode(mem.getRecommendCode());
            map1.put("recommendID", mem.getRecommandID());
            memberLogin.setRecommendID(mem.getRecommandID());
            map1.put("companyID", mem.getCompanyID());
            memberLogin.setCompanyID(mem.getCompanyID());
            map1.put("shopID", mem.getShopID());
            memberLogin.setShopID(mem.getShopID());
            map1.put("rankID", mem.getRankID());
            memberLogin.setRankID(mem.getRankID());
            map1.put("peopleID", mem.getPeopleID());
            memberLogin.setPeopleID(mem.getPeopleID());
            memberLogin.setLoginTime(TimeTest.getTime());
            /**
             * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
             * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
             * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
             * 并设置 isWork=1；(感觉没必要用消息队列)
             */
            Employee employee = employeeService.getMemberLogin(memberLogin);
            if (employee != null) {
                if (StringUtils.isNotBlank(employee.getCompanyID())) {
                    memberLogin.setIsWork(1);
                    memberLogin.setWorkEmployeeID(employee.getId());
                    memberLogin.setWorkCompanyID(employee.getCompanyID());
                    memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                    memberLogin.setWorkShopID(employee.getShopID());
                    memberLogin.setWorkJobID(employee.getJobID());
                    memberLogin.setWorkGradeID(employee.getGradeID());
                }
            }
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, uuid), memberLogin, redisTimeOut, TimeUnit.SECONDS);
        }
        rsMap.put("sessionID", uuid);
        return MessagePacket.newSuccess(rsMap, "memberLoginSJ success");
    }

    @ApiOperation(value = "openIDorUnionIDFastLogin", notes = "微信公众号小程序openIDunionID直接登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/openIDorUnionIDFastLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket openIDorUnionIDFastLogin(HttpServletRequest request, ModelMap map) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        if (Config.SafeGrade) {
            String timeStamp = request.getParameter("timeStamp");
            String encryptString = request.getParameter("encryptString");
            Map<String, Object> map1 = kingBase.base64EncryptStringNew(request,timeStamp, encryptString);
            if (!map1.get("result").equals("通过")) {
                String result = map1.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }

        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        ApiDeviceDetailDto dto = deviceService.selectByID(deviceID);
        if (dto == null) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }

        String openID = request.getParameter("openID");
        String unionID = request.getParameter("unionID");
        if (StringUtils.isBlank(openID) && StringUtils.isBlank(unionID)) {
            return MessagePacket.newFail(MessageHeader.Code.openIDORUnionIDNotAllNull, "openID和unionID不能同时为空");
        }
        Map<String, Object> openMap = Maps.newHashMap();
        openMap.put("openid",openID);
        openMap.put("weixinUnionID",unionID);
        openMap.put("deviceID",deviceID);

        ApiWeixinAppMemberDetail weixinAppMemberDetail = weixinAppMemberService.getByMap(openMap);
        ApiWeixinMemberDetail weixinMemberDetail = weixinMemberService.getByMapopen(openMap);


        // 1. 小程序层面的日志判断
        if (weixinAppMemberDetail == null) {
            log.info("登录检测 - 根本找不到这个用户的微信小程序记录（没用过）。当前请求 deviceID: {}", deviceID);
        } else if (!Objects.equals(deviceID, weixinAppMemberDetail.getDeviceID())) {
            log.info("登录检测 - 虽然有小程序记录，但当前请求传过来的 deviceID [{}] 和数据库里绑定的 deviceID [{}] 对不上。", deviceID, weixinAppMemberDetail.getDeviceID());
        }

        // 2. 公众号层面的日志判断
        if (weixinMemberDetail == null) {
            log.info("登录检测 - 没关注公众号。当前请求 deviceID: {}", deviceID);
        } else if (!Objects.equals(deviceID, weixinMemberDetail.getDeviceID())) {
            log.info("登录检测 - 虽然关注了公众号，但当前设备号 [{}] 和公众号记录里的设备号 [{}] 对不上。", deviceID, weixinMemberDetail.getDeviceID());
        }

        // 注释掉原本的布尔值和拦截逻辑，直接放行
        // boolean isAppValid = (weixinAppMemberDetail != null && Objects.equals(deviceID, weixinAppMemberDetail.getDeviceID()));
        // boolean isWeixinValid = (weixinMemberDetail != null && Objects.equals(deviceID, weixinMemberDetail.getDeviceID()));
        // if (!isAppValid && !isWeixinValid) {
        //     return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "公众号小程序无微信关注");
        // }

        Map<String, Object> param = new HashMap<>();
        param.put("applicationID", site.getApplicationID());
        if (StringUtils.isNotBlank(openID)) {
            param.put("openID", openID);
        }
        if (StringUtils.isNotBlank(unionID)) {
            param.put("unionID", unionID);
        }
        Member mem = memberService.selectByOpenIDORUnionID(param);

        openMap.clear();
        openMap.put("deviceID", deviceID);
        List<ApiMemberDeviceList> memberDeviceLists = memberDeviceService.getDeviceIDByMemberID(openMap);
        if (memberDeviceLists == null || memberDeviceLists.isEmpty()) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "无会员设备记录");
        }
        if (mem == null) {
            return MessagePacket.newFail(MessageHeader.Code.openIDORUnionIDAllNotFound, "openID或unionID不正确,无法查找到对应的会员");
//            return MessagePacket.newFail(MessageHeader.Code.openIDORUnionIDAllNotFound, "登录名不正确");
        }

        if (mem.getIsLock() == 1) {
            return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "会员已被锁定");
        }
        String pushDeviceID = dto.getPushDeviceID();
        if (StringUtils.isNotBlank(pushDeviceID)) {
            map.clear();
            map.put("code", "JpushAppKey");
            map.put("siteID", siteID);
            String jpushAppKey = parameterSiteService.getValueByMap(map);
            map.put("code", "JpushMasterSecret");
            String jpushMasterSecret = parameterSiteService.getValueByMap(map);
            if (StringUtils.isNotBlank(jpushAppKey) && StringUtils.isNotBlank(jpushMasterSecret)) {
                JPushClient jPushClient = new JPushClient(jpushMasterSecret, jpushAppKey);
                try {
                    DefaultResult defaultResult = jPushClient.updateDeviceTagAlias(pushDeviceID, mem.getId(), null, null);
                    log.info("log" + defaultResult.toString());
                    TagAliasResult result = jPushClient.getDeviceTagAlias(pushDeviceID);
                    log.info("log" + mem.getName() + "别名：" + result.toString());
                } catch (APIConnectionException e) {
                    log.error("log",e);
                } catch (APIRequestException e) {
                    log.error("log",e);
                }
            }
        }
        map.clear();
        map.put("memberID", mem.getId());
        WeixinAppMember weixinAppMember = weixinMemberService.getByMap(map);
        if (weixinAppMember != null) {
            weixinMemberService.updateWeixinAppMember(weixinAppMember);
        }
        map.put("deviceID", deviceID);
        String memberDeviceID = memberDeviceService.getIdByMap(map);
        if (StringUtils.isBlank(memberDeviceID)) {
            map.put("name", mem.getName() + deviceService.getNameById(deviceID));
            map.put("creator", mem.getId());
            map.put("id", IdGenerator.uuid32());
            memberDeviceService.insertByMap(map);
        }
        //2020-09-27 修改device机器最后登录时间
        deviceService.updateLastLoadTime(deviceID);
        String ipaddr = WebUtil.getRemortIP(request);
        String uuid = IdGenerator.uuid32();
        // session检查
        memberService.checkSession(deviceID, uuid, mem.getId(), siteID, mem.toString());
        String description = String.format("会员登录");
        MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), mem.getApplicationID(),
                deviceID, mem.getId(), memberService.getLocationID(ipaddr), mem.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, uuid), mem, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, uuid), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
        sendMessageService.sendMemberLoginMessage(false, uuid, ipaddr, ipaddr, description, Memberlog.MemberOperateType.memberLogin);//
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", uuid);
        rsMap.put("memberID", mem.getId());
        rsMap.put("weixinAppMemberID", weixinAppMemberDetail != null ? weixinAppMemberDetail.getWeixinAppMemberID() : "");
        rsMap.put("weixinMemberID", weixinMemberDetail != null ? weixinMemberDetail.getWeixinMemberID() : "");
        rsMap.put("shortName", mem.getShortName());
        rsMap.put("avatarURL", mem.getAvatarURL());
        rsMap.put("loginName", mem.getLoginName());
        rsMap.put("phone", mem.getPhone());
        rsMap.put("name", mem.getName());
        rsMap.put("recommandCode", mem.getRecommandCode());
        rsMap.put("recommendCode", mem.getRecommendCode());
        rsMap.put("weixinUnionID", mem.getWeixinUnionID());
        rsMap.put("weixinToken", mem.getWeixinToken());
        rsMap.put("loginPassword", mem.getLoginPassword());
        rsMap.put("rankID", mem.getRankID());
        rsMap.put("lastChangePWTime", mem.getLastChangePWTime());
        rsMap.put("isNeedChangePW", mem.getIsNeedChangePW());
        rsMap.put("PWErrorTimes", mem.getPWErrorTimes());

        if (StringUtils.isNotBlank(mem.getRankID())) {
            rsMap.put("rankName", rankService.getNameById(mem.getRankID()));
        } else {
            rsMap.put("rankName", null);
        }

        rsMap.put("majorID", mem.getMajorID());
        if (StringUtils.isNotBlank(mem.getMajorID())) {
            rsMap.put("majorName", majorService.getNameById(mem.getMajorID()));
        } else {
            rsMap.put("majorName", null);
        }
        //新增memberLogin
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", mem.getId());
        memberLogin.setMemberID(mem.getId());
        map1.put("memberName", mem.getName());
        memberLogin.setMemberName(mem.getName());
        map1.put("memberShortName", mem.getShortName());
        memberLogin.setMemberShortName(mem.getShortName());
        map1.put("memberAvatar", mem.getAvatarURL());
        memberLogin.setMemberAvatar(mem.getAvatarURL());
        map1.put("loginName", mem.getLoginName());
        memberLogin.setLoginName(mem.getLoginName());

        map1.put("phone", mem.getPhone());
        memberLogin.setPhone(mem.getPhone());
        map1.put("weixinToken", mem.getWeixinToken());
        memberLogin.setWeixinToken(mem.getWeixinToken());
        map1.put("weixinUnionID", mem.getWeixinUnionID());
        memberLogin.setWeixinUnionID(mem.getWeixinUnionID());
        map1.put("recommendCode", mem.getRecommendCode());
        memberLogin.setRecommendCode(mem.getRecommendCode());
        map1.put("recommendID", mem.getRecommandID());
        memberLogin.setRecommendID(mem.getRecommandID());
        map1.put("companyID", mem.getCompanyID());
        memberLogin.setCompanyID(mem.getCompanyID());
        map1.put("shopID", mem.getShopID());
        memberLogin.setShopID(mem.getShopID());
        map1.put("rankID", mem.getRankID());
        memberLogin.setRankID(mem.getRankID());
        map1.put("peopleID", mem.getPeopleID());
        memberLogin.setPeopleID(mem.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }

        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, uuid), memberLogin, redisTimeOut, TimeUnit.SECONDS);
        return MessagePacket.newSuccess(rsMap, "memberLogin success");
    }

    @ApiOperation(value = "退出登录，清除session", notes = "退出登录，清除session")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "sessionID", dataType = "String")})
    @RequestMapping(value = "/loginOut", produces = {"application/json;charset=UTF-8"})
    public MessagePacket loginOut(HttpServletRequest request) {
        String sessionID = request.getParameter("sessionID");
        String siteID = request.getParameter("siteID");
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "token不能为空");
        }
        MemberLogin memberLogin = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
        if (memberLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        if (StringUtils.isNotBlank(siteID)&&!siteID.equals(memberLogin.getSiteID())) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "siteID不匹配");
        }
        if (StringUtils.isNotBlank(deviceID)&&!deviceID.equals(memberLogin.getDeviceID())) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "deviceID不匹配");
        }


        MemberSession memberSession = memberService.checkMemberSession(new HashMap<String, Object>() {
            {
                put("sessionID", sessionID);
            }
        });
        if (memberSession != null) {
            // 修改退出时间
            memberService.sessionLogOut(new HashMap<String, Object>() {
                {
                    put("id", memberSession.getId());
                    put("outType", 1);
                }
            });
        }

        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
//            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "token已失效");
            return MessagePacket.newSuccess("loginOut success!");
        }
        if (memberLogDTO != null) {
            String description = String.format("%s退出登录", memberLogDTO.getMemberName());
            sendMessageService.sendMemberLoginOutMessage(sessionID, description, request, Memberlog.MemberOperateType.LOGINOUT);
        }
        return MessagePacket.newSuccess("loginOut success!");
    }
    @ApiOperation(value = "memberPhoneRegister", notes = "会员注册")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/memberPhoneRegister", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberPhoneRegister(HttpServletRequest request, ModelMap map) {
        String deviceID = request.getParameter("deviceID");
        String avatarURL = request.getParameter("avatarURL");
        String titleID = request.getParameter("titleID");
        String birthday = request.getParameter("birthday");
        String linkCode = request.getParameter("linkCode");
        String serviceMemberID = request.getParameter("serviceMemberID");
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        String openID = request.getParameter("openID");
        String unionID = request.getParameter("unionID");
        String companyName = request.getParameter("companyName");
        String companyJob = request.getParameter("companyJob");
        ApiWeixinAppMemberDetail weixinAppMemberDetail = null;

        ApiWeixinMemberDetail weixinMemberDetail = null;
        List<ApiMemberDeviceList> memberDeviceLists = null;
        Map<String, Object> openMap = Maps.newHashMap();
        openMap.put("openid",openID);
        openMap.put("weixinUnionID",unionID);
        if (StringUtils.isNotBlank(openID)||StringUtils.isNotBlank(unionID)) {
            weixinAppMemberDetail = weixinAppMemberService.getByMap(openMap);
            weixinMemberDetail = weixinMemberService.getByMapopen(openMap);

            if (weixinAppMemberDetail == null && weixinMemberDetail == null){
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "会员注册无微信关注");
            }
            memberDeviceLists = memberDeviceService.getDeviceIDByMemberID(openMap);
        }

        if (StringUtils.isNotBlank(serviceMemberID) && !memberService.checkMemberId(serviceMemberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "serviceMemberID不正确");
        }
        if (StringUtils.isNotBlank(distributorCompanyID) && StringUtils.isBlank(companyService.getIsValidIdBy(distributorCompanyID))) {
            return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "distributorCompanyID不正确");
        }
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }

        if (phone != null) {
            Map<String, Object> requestMap = Maps.newHashMap();
            requestMap.put("Isphone",true);
            Map<String, Object> adressIp = kingBase.getAdressIp(request, site.getApplicationID(), null,  phone, requestMap);
            if (!adressIp.get("result").equals("通过")) {
                String result = adressIp.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        else {
            Map<String, Object> requestMap = Maps.newHashMap();
            Map<String, Object> adressIp = kingBase.getAdressIp(request, site.getApplicationID(), null,  null, requestMap);
            if (!adressIp.get("result").equals("通过")) {
                String result = adressIp.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        // Map<String, Object> memParam = new HashMap<>();
        // memParam.put("loginName", phone);
        // memParam.put("applicationID", site.getApplicationID());
        // Member mem = memberService.selectByloginNameAndApplicationID(memParam);
        // if (mem != null) {
        //     return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "该手机号已注册");
        // }
        Map<String, Object> memParam = new HashMap<>();
        memParam.put("phone", phone);
        memParam.put("applicationID", site.getApplicationID());
        // String mem = memberService.selectByPhone(memParam);
        MemberDetailDto mem = memberService.getOneMemberDetail(memParam);
        if (mem != null) {
            if(mem.getIsLock()==1){
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "账户被锁定，请联系管理员解锁！");
            }
            else return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "该手机号已注册");
        }

        String verifyCode = request.getParameter("verifyCode");
        //设计得到注册会员不需要验证码
        if (!"8a2f462a78ecd2fa0178eedb1d280510".equals(site.getApplicationID())) {
            if (StringUtils.isBlank(verifyCode)) {
                return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotNull, "验证码不能为空");
            }
            String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.REGISTER_AUTH_CODE, phone));
            //  if (StringUtils.isNotBlank(oldVerifyCode) && !oldVerifyCode.equals("999999")) {
            if (!verifyCode.equals("999999")) {
                if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
                    return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
                }
            }

            //从redis里删除这个验证码
            if (!verifyCode.equals("999999")) {
                stringRedisTemplate.delete(String.format("%s_%s", CacheContant.REGISTER_AUTH_CODE, phone));
            }
            // }
        }
        String loginName = request.getParameter("loginName");
        if (StringUtils.isBlank(loginName)) {
            loginName = phone;
            map.clear();
            memParam.put("loginName", phone);
            memParam.put("applicationID", site.getApplicationID());
            Member selectedByloginNameAndApplicationID = memberService.selectByloginNameAndApplicationID(memParam);
            if (selectedByloginNameAndApplicationID != null) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "loginName 重复");
            }
        }
        map.clear();
        memParam.put("loginName", loginName);
        memParam.put("applicationID", site.getApplicationID());
        Member selectedByloginNameAndApplicationID = memberService.selectByloginNameAndApplicationID(memParam);
        if (selectedByloginNameAndApplicationID != null) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "loginName 重复");
        }


        String channelCode = request.getParameter("channelCode");
        if (StringUtils.isNotBlank(channelCode)) {
            channelID = channelService.selectByServiceCode(channelCode);
        }
        String shortName = request.getParameter("shortName");
        String password = request.getParameter("password");
        if (StringUtils.isBlank(password)) {
            //      按照 随机生成的 当前日期 mmdd + 随机2个字母+ 手机号 后3位+ 1个 随机符号（英文的 逗号等 可输入的符合）
            password = generatePassword(phone);
        }else if(!Pattern.matches("(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}",password)){
            password = generatePassword(phone);
        }



        String recommandCode = request.getParameter("recommandCode");
        String recommandID = null;
        String linkMemberID = null;
        Member recommand = null;
        boolean canChangeLinkChain = false;
        if (StringUtils.isNotBlank(recommandCode)) {
            log.info("log" + "recommandCode:" + recommandCode);
            Map<String, Object> param = new HashMap<>();
            param.put("applicationID", site.getApplicationID());
            param.put("recommandCode", recommandCode);
            recommandID = memberService.selectByRecommandCode(param);
            if (StringUtils.isBlank(recommandID)) {
                recommandID = memberService.selectByPhone(param);
                if (StringUtils.isEmpty(recommandID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "推荐码或手机号不正确");
                }
            }
            recommand = memberService.selectById(recommandID);
            if (recommand == null) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "推荐码不正确");
            }
            if (StringUtils.isNotBlank(linkCode)) {
                param.put("recommandCode", linkCode);
                linkMemberID = memberService.selectByRecommandCode(param);
                if (StringUtils.isBlank(linkMemberID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "接点上级推荐码不正确");
                }
                Map<String, Object> linkParam = new HashMap<>();
                linkParam.put("linkChain", recommand.getLinkChain() + "," + recommandCode);
                linkParam.put("recommandCode", recommandCode);
                linkParam.put("applicationID", site.getApplicationID());
                List<String> linkMember = memberService.getLinkList(linkParam);
                List<String> members = memberService.getLinkMemberList(linkMemberID);
                if (linkMember.isEmpty() || !linkMember.contains(linkMemberID) || members.size() >= 3) {
                    return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "该接点上级推荐码不可用");
                }
                canChangeLinkChain = true;
            }
        } else {
            log.info("log" + "recommandCode is null");
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            if (!NumberUtils.isNumeric(bizBlock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "bizBlock请输入数字");
            }
        } else {
            bizBlock = "0";
        }
        String name = request.getParameter("name");
        String recommendID = "";
        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.clear();
            map.put("recommendCode", recommendCode);
            map.put("applicationID", site.getApplicationID());
            Member member1 = memberService.getMemberByRecommendCode(map);
            if (member1 != null) {
                recommendID = member1.getId();
            }
        }
        String recommendMemberID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendMemberID)) {
            if (!memberService.checkMemberId(recommendMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
            }
            recommendID = recommendMemberID;

        }

        String applicationID = site.getApplicationID();
        if (applicationID!=null&&memberDeviceLists != null && !memberDeviceLists.isEmpty()) {
            long matchCount = memberDeviceLists.stream()
                    // 过滤条件：memberDevice的applicationID与目标applicationID一致（非空判断避免空指针）
                    .filter(memberDevice -> applicationID != null
                            && applicationID.equals(memberDevice.getApplicationID()))
                    // 统计符合条件的记录数
                    .count();
            if (matchCount >= 10) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "该应用对应会员设备已经超过10个");
            }
        }
        Member member = new Member();
        String memberID = IdGenerator.uuid32();
        member.setId(memberID);
        member.setCreatedTime(new Timestamp(System.currentTimeMillis()));
        String highth = request.getParameter("highth");
        if (StringUtils.isNotBlank(highth)) {
            if (!NumberUtils.isNumeric(highth)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "highth 请输入数字");
            }
            member.setHighth(Double.valueOf(highth));
        }
        String weight = request.getParameter("weight");
        if (StringUtils.isNotBlank(weight)) {
            if (!NumberUtils.isNumeric(weight)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "weight 请输入数字");
            }
            member.setWeight(Double.valueOf(weight));
        }
        if (StringUtils.isNotBlank(recommendID)) {
            member.setRecommandID(recommendID);
            member.setRecommendID(recommendID);
        }
        if (StringUtils.isNotBlank(recommendID)) {
            MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(recommendID);

            if (memberStatistics != null && memberStatistics.getRecommendMembers() != null) {
                int recommendNum = memberStatistics.getRecommendMembers() + 1;
                memberStatistics.setRecommendMembers(recommendNum);
                memberStatisticsService.updateMemberStatistics(memberStatistics);
            }


        }
        if (StringUtils.isNotBlank(channelID)) {
            member.setChannelID(channelID);
        }

        if (StringUtils.isNotBlank(phone)) {
            member.setPhone(phone);
        }
        if (StringUtils.isNotBlank(loginName)) {
            member.setLoginName(loginName);
        }

        if (StringUtils.isNotBlank(siteID)) {
            member.setSiteID(siteID);
        }

        String encodePassword = MD5.encodeMd5(String.format("%s%s", password, Config.ENCODE_KEY));
        member.setLoginPassword(encodePassword);
        if (StringUtils.isNotBlank(birthday)) {
            member.setBirthday(TimeTest.strToTimes(birthday));
        }
        if (StringUtils.isNotBlank(titleID)) {
            member.setTitleID(titleID);
        }
        member.setApplicationID(applicationID);
        member.setCompanyID(site.getCompanyID());
        member.setAvatarURL(avatarURL);
        member.setServiceMemberID(serviceMemberID);
        member.setDistributorCompanyID(distributorCompanyID);
        int level = 1;
        //如果推荐人ID不为空，把他的recommendChain拿来过来加入推荐的的ID组成新的recommendChain
        if (StringUtils.isNotBlank(recommendID)) {

            Member recommendMember = memberService.selectById(recommendID);
            if (recommendMember != null) {
                if (recommendMember.getLevelNumber() != null) {
                    level = recommendMember.getLevelNumber() + 1;
                }
                if (StringUtils.isNotBlank(recommendMember.getRecommendChain())) {
                    member.setRecommendChain(recommendMember.getRecommendChain() + "," + recommendMember.getRecommandCode());
                    member.setRecommandChain(recommendMember.getRecommendChain() + "," + recommendMember.getRecommandCode());
                } else {
                    member.setRecommendChain(recommendMember.getRecommandCode());
                    member.setRecommandChain(recommendMember.getRecommandCode());
                }
            }
        }
        String reCode = memberService.getCurRecommandCode(site.getApplicationID());
        if (StringUtils.isNotEmpty(reCode)) {
            member.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
            member.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
        } else {
            member.setRecommandCode("00001");
            member.setRecommendCode("00001");
        }
        Map<String, Object> pMap = Maps.newHashMap();
        pMap.put("applicationID", site.getApplicationID());
        pMap.put("code", "memberLockNeedVerify");
        String code = parameterService.selectParameterApplication(pMap);
        int isLock = 0;
        if (StringUtils.isNotBlank(code) && code.equals("1")) isLock = 1;
        member.setBizBlock(site.getBizBlock());
        member.setQiyeweixinID(sequenceDefineService.genCode("qiyeweixinID", memberID));
        member.setIsLock(isLock);
        member.setCreateType(4);
        member.setLevelNumber(level);
        String finalBizBlock = bizBlock;
/* List<ApiRankListDto> rankList = rankService.getRankList(
                new HashMap<String, Object>() {{
                    put("applicationID", applicationID);
                    put("bizBlock", finalBizBlock);
                    put("sortTypeOrderSeq", 1);
                }});
        if (!rankList.isEmpty() && rankList.size() > 0) {
            ApiRankListDto rank = rankList.get(0);
            member.setName(rank.getName() + member.getRecommandCode());
            member.setShortName(rank.getName() + member.getRecommandCode());
        } else {
            member.setName(name);
            member.setShortName(shortName);
        }*/
        // 校验name与shortName为空?name||shortName : 否则为手机号
        if (StringUtils.isNotBlank(name)) {
            member.setName(name);
        } else {
            member.setName(phone);
        }

        if (StringUtils.isNotBlank(shortName)) {
            member.setShortName(shortName);
        }else {
            if (!site.getApplicationID().equals("8a2f462a953c60d101953cada9f400ae")) {
                member.setShortName(phone);//仅热线名片取消shortname默认设置为手机号
            }
        }
        log.info("log" + site.getApplicationID());

        if (StringUtils.isNotBlank(openID)) {
            // map.clear();
            memParam.clear();
            memParam.put("applicationID", site.getApplicationID());
            memParam.put("openID", openID);
            Member selectByOpenID = memberService.selectByOpenIDMap(memParam);
            if (selectByOpenID != null) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "openID 重复");
            }
            member.setWeixinToken(openID);
        }
        if (StringUtils.isNotBlank(unionID)) {
            map.clear();
            memParam.put("applicationID", site.getApplicationID());
            memParam.put("unionID", unionID);
            Member selectByOpenID = memberService.selectByOpenIDMap(memParam);
            if (selectByOpenID != null) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "unionID 重复");
            }
            member.setWeixinUnionID(unionID);
        }
        //热线小程序新增companyName和companyJob
        if (StringUtils.isNotBlank(companyName)) {
            member.setCompanyName(companyName);
        }
        if (StringUtils.isNotBlank(companyJob)) {
            member.setCompanyJob(companyJob);
        }
        // 新增字段
        /**
         * 新增字段
         * shengID: that.data.member.provinceID, // 省ID
         * shengName: that.data.member.provinceName, // 省名称
         * shiID: that.data.member.cityID, // 市ID
         * shiName: that.data.member.cityName, // 市名称
         * xianID: that.data.member.countyID, // 县ID
         * xianName: that.data.member.countyName, // 县名称
         */
        String shengID = request.getParameter("shengID");
        String shengName = request.getParameter("shengName");
        String shiID = request.getParameter("shiID");
        String shiName = request.getParameter("shiName");
        String xianID = request.getParameter("xianID");
        String xianName = request.getParameter("xianName");
        if (StringUtils.isNotBlank(shengID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("city", shengID))) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            member.setShengID(shengID);
        }
        if (StringUtils.isNotBlank(shengName)) {
            member.setShengName(shengName);
        }
        if (StringUtils.isNotBlank(shiID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("city", shiID))) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            member.setShiID(shiID);
        }
        if (StringUtils.isNotBlank(shiName)) {
            member.setShiName(shiName);
        }
        if (StringUtils.isNotBlank(xianID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("city", xianID))) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            member.setXianID(xianID);
        }
        if (StringUtils.isNotBlank(xianName)) {
            member.setXianName(xianName);
        }

        memberService.insertMember(member);
        //修改
        if (StringUtils.isNotBlank(openID)||StringUtils.isNotBlank(unionID)) {
            Map<String, Object> updateMap = Maps.newHashMap();
            if (weixinAppMemberDetail != null) {
                updateMap.put("id", weixinAppMemberDetail.getWeixinAppMemberID());
                updateMap.put("memberID", memberID);
                updateMap.put("deviceID", deviceID);
                updateMap.put("name", member.getName() + weixinAppMemberDetail.getName());
                updateMap.put("phone", member.getPhone());
                updateMap.put("nickname", member.getName());
                updateMap.put("doTime", TimeTest.getTimeStr());
                updateMap.put("doType", 1);
                weixinAppMemberService.updateByMap(updateMap);
            }
            //openMap
            if (weixinMemberDetail != null) {
                updateMap.clear();
                updateMap.put("id", weixinMemberDetail.getWeixinMemberID());
                updateMap.put("memberID", memberID);
                updateMap.put("name", member.getName() + weixinMemberDetail.getName());
                updateMap.put("phone", member.getPhone());
                updateMap.put("nickname", member.getName());
                updateMap.put("doTime", TimeTest.getTimeStr());
                updateMap.put("doType", 1);
                weixinMemberService.updateByMap(updateMap);
            }
//            //修改可能存在的memberdevice
//            if (memberDeviceLists != null && !memberDeviceLists.isEmpty()) {
//                for (ApiMemberDeviceList memberDeviceList : memberDeviceLists) {
//                    updateMap.clear();
//                    updateMap.put("id", memberDeviceList.getMemberDeviceID());
//                    updateMap.put("memberID", memberID);
//                    updateMap.put("openid", openID);
//                    updateMap.put("weixinUnionID", unionID);
//                    updateMap.put("applicationID", applicationID);
//                    updateMap.put("name", member.getName() + memberDeviceList.getName());
//                    updateMap.put("lastUseTime", TimeTest.getTimeStr());
//                    memberDeviceService.updateByMap(updateMap);
//                }
//            }
        }
        //查这个 deviceID 过去24小时的记录，如果有的话，把里面的 memberID 填写，签署时间=显示
        if (StringUtils.isNotBlank(deviceID)) {
            List<String>  ids = agreementService.getBefore24h(deviceID);
            if (ids != null && !ids.isEmpty()) {
                for (String id : ids) {
                    Map<String, Object> agreementMap = new HashMap<>();
                    agreementMap.put("agreementID", id);
                    agreementMap.put("memberID", memberID);
                    agreementMap.put("signedTime", TimeTest.getTimeStr());
                    agreementService.update(agreementMap);
                }
            }

        }
        //额外创建一条新的memberdevice
        Map<String, Object> memberDeviceMap = Maps.newHashMap();
        String memberDeviceid = IdGenerator.uuid32();
        memberDeviceMap.put("id",memberDeviceid);
        memberDeviceMap.put("applicationID",member.getApplicationID());
        memberDeviceMap.put("memberID",memberID);
        memberDeviceMap.put("name",member.getName() + "会员设备注册");
        memberDeviceMap.put("orderSeq",1);
        memberDeviceMap.put("openid",openID);
        memberDeviceMap.put("weixinUnionID",unionID);
        memberDeviceMap.put("deviceID",deviceID);
        memberDeviceMap.put("lastUseTime",TimeTest.getTimeStr());
        memberDeviceService.insertByMap(memberDeviceMap);



        //如果有推荐人，两个互加好友 在消息队列实现
        if (StringUtils.isNotBlank(recommendMemberID)) {
            friendRelationService.createFriendRelation(memberID, recommendMemberID, site.getApplicationID());
            friendRelationService.createFriendRelation(recommendMemberID, memberID, site.getApplicationID());
        }
        if (canChangeLinkChain) {
            Map<String, Object> linkParam = changeLinkChain(member.getId(), linkMemberID, request);
            boolean flag = (boolean) linkParam.get("success");
            String linkMemberIDNew = (String) linkParam.get("linkMemberID");
            String linkChain = (String) linkParam.get("linkChain");
            member.setLinkMemberID(linkMemberIDNew);
            member.setLinkChain(linkChain);
        }
        if (recommand != null) {
            MessageDto messageDto = new MessageDto();
            messageDto.setName("会员" + member.getName() + "手机号" + member.getPhone() + "注册成功，恭喜你。");
            messageDto.setApplicationID(member.getApplicationID());
            messageDto.setDescription("会员" + member.getName() + "手机号" + member.getPhone() + "注册成功，恭喜你。");
            messageDto.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
            messageDto.setObjectID(member.getId());
            messageDto.setObjectName(member.getName());
            messageDto.setReceiverMemberID(recommand.getId());
            messageDto.setSenderMemberID(member.getId());
            messageDto.setSendCompanyID(member.getCompanyID());
            messageDto.setReceiveCompanyID(recommand.getCompanyID());
            kingBase.messageSend(messageDto, request);
        }
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        String ipaddr = WebUtil.getRemortIP(request);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);

        // session检查
        memberService.checkSession(deviceID, session.getId(), memberID, siteID, member.toString());

        String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")手机号注册成功";
        MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), member.getApplicationID(),
                deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
        sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description,
                Memberlog.MemberOperateType.addSuccess);
        memberService.createMemberPrivilege(applicationID, member.getId());
        map.clear();
        map.put("id", member.getId());
        map.put("lastLoginTime", TimeTest.getTime());
        try {
            Thread.sleep(1000);
            memberService.updateMember(map);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")手机号登录成功";
        sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                Memberlog.MemberOperateType.memberPhoneRegister);
        // 检查memberDevice
        //memberService.checkMemberDevice(member.getId(), deviceID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", session.getId());
        rsMap.put("memberID", member.getId());
        rsMap.put("recommandCode", member.getRecommandCode());
        rsMap.put("recommendCode", member.getRecommendCode());
        //新增memberLogin
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", member.getId());
        memberLogin.setMemberID(member.getId());
        map1.put("memberName", member.getName());
        memberLogin.setMemberName(member.getName());
        map1.put("memberShortName", member.getShortName());
        memberLogin.setMemberShortName(member.getShortName());
        map1.put("memberAvatar", member.getAvatarURL());
        memberLogin.setMemberAvatar(member.getAvatarURL());
        if (StringUtils.isNotBlank(loginName)) {
            map1.put("loginName", loginName);
            memberLogin.setLoginName(loginName);
        } else {
            map1.put("loginName", member.getLoginName());
            memberLogin.setLoginName(member.getLoginName());
        }
        map1.put("phone", member.getPhone());
        memberLogin.setPhone(member.getPhone());
        map1.put("weixinToken", member.getWeixinToken());
        memberLogin.setWeixinToken(member.getWeixinToken());
        map1.put("weixinUnionID", member.getWeixinUnionID());
        memberLogin.setWeixinUnionID(member.getWeixinUnionID());
        map1.put("recommendCode", member.getRecommendCode());
        memberLogin.setRecommendCode(member.getRecommendCode());
        map1.put("recommendID", member.getRecommandID());
        memberLogin.setRecommendID(member.getRecommandID());
        map1.put("companyID", member.getCompanyID());
        memberLogin.setCompanyID(member.getCompanyID());
        map1.put("shopID", member.getShopID());
        memberLogin.setShopID(member.getShopID());
        map1.put("rankID", member.getRankID());
        memberLogin.setRankID(member.getRankID());
        map1.put("peopleID", member.getPeopleID());
        memberLogin.setPeopleID(member.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        return MessagePacket.newSuccess(rsMap, "memberPhoneRegister success");
    }

    //            按照 随机生成的 当前日期 mmdd + 随机2个字母+ 手机号 后3位+ 1个 随机符号（英文的 逗号等 可输入的符合）
    @NotNull
    private static String generatePassword(String phone) {
        // 获取当前日期mmdd格式
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        String dateStr = sdf.format(new Date());

        // 生成2个随机字母
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        //Random random = new Random();
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        /*sb.append(letters.charAt(random.nextInt(letters.length())));
        sb.append(letters.charAt(random.nextInt(letters.length())));*/
        sb.append(letters.charAt(secureRandom.nextInt(letters.length())));
        sb.append(letters.charAt(secureRandom.nextInt(letters.length())));

        // 获取手机号后3位(假设phone是类成员变量)

        String phoneLast3 = phone.substring(phone.length() - 3);

        // 生成1个随机符号
        String symbols = ",.!@#$%^&*()_+-=[]{}|;':\"<>?";
        //char symbol = symbols.charAt(random.nextInt(symbols.length()));
        char symbol = symbols.charAt(secureRandom.nextInt(symbols.length()));

        // 组合所有部分
        return dateStr + sb.toString() + phoneLast3 + symbol;
    }

    @ApiOperation(value = "memberPhoneRegisterSJ", notes = "独立接口:设计得到会员注册")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/memberPhoneRegisterSJ", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberPhoneRegisterSJ(HttpServletRequest request, ModelMap map) {
        Map<String, Object> memParam = new HashMap<>();
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }

        String serviceMemberID = request.getParameter("serviceMemberID");
        if (StringUtils.isNotBlank(serviceMemberID)) {
            if (!memberService.checkMemberId(serviceMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "serviceMemberID不正确");
            }
        }

        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (!siteService.checkIdIsValid(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }

        String channelID = request.getParameter("channelID");
        if (StringUtils.isNotBlank(channelID)) {
            Channel channel = channelService.selectById(channelID);
            if (channel == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }

        String avatarURL = request.getParameter("avatarURL");

        Site site = siteService.selectSiteById(siteID);
        memParam.put("loginName", phone);
        memParam.put("applicationID", site.getApplicationID());
        Member mem = memberService.selectByloginNameAndApplicationID(memParam);
        if (mem != null) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "该手机号已注册");
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isBlank(loginName)) {
            loginName = phone;
        }

        String shortName = request.getParameter("shortName");
        if (StringUtils.isBlank(shortName)) {
            shortName = phone;
        }

        String name = request.getParameter("name");
        if (StringUtils.isBlank(name)) {
            name = phone;
        }

        String password = request.getParameter("password");

        //            按照 随机生成的 当前日期 mmdd + 随机2个字母+ 手机号 后3位+ 1个 随机符号（英文的 逗号等 可输入的符合）
        password = generatePassword(phone);

        String linkCode = request.getParameter("linkCode");
        String titleID = request.getParameter("titleID");
        String birthday = request.getParameter("birthday");
        String recommandCode = request.getParameter("recommandCode");
        String recommandID = null;
        String linkMemberID = null;
        Member recommand = null;
        boolean canChangeLinkChain = false;
        if (StringUtils.isNotBlank(recommandCode)) {
            log.info("log" + "recommandCode:" + recommandCode);
            Map<String, Object> param = new HashMap<>();
            param.put("applicationID", site.getApplicationID());
            param.put("recommandCode", recommandCode);
            recommandID = memberService.selectByRecommandCode(param);
            if (StringUtils.isBlank(recommandID)) {
                recommandID = memberService.selectByPhone(param);
                if (StringUtils.isEmpty(recommandID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "推荐码或手机号不正确");
                }
            }
            recommand = memberService.selectById(recommandID);
            if (recommand == null) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "推荐码不正确");
            }
            if (StringUtils.isNotBlank(linkCode)) {
                param.put("recommandCode", linkCode);
                linkMemberID = memberService.selectByRecommandCode(param);
                if (StringUtils.isBlank(linkMemberID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "接点上级推荐码不正确");
                }
                Map<String, Object> linkParam = new HashMap<>();
                linkParam.put("linkChain", recommand.getLinkChain() + "," + recommandCode);
                linkParam.put("recommandCode", recommandCode);
                linkParam.put("applicationID", site.getApplicationID());
                List<String> linkMember = memberService.getLinkList(linkParam);
                List<String> members = memberService.getLinkMemberList(linkMemberID);
                if (linkMember.isEmpty() || !linkMember.contains(linkMemberID) || members.size() >= 3) {
                    return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "该接点上级推荐码不可用");
                }
                canChangeLinkChain = true;
            }
        } else {
            log.info("log" + "recommandCode is null");
        }
        Member member = new Member();
        member.setId(IdGenerator.uuid32());
        member.setCreatedTime(new Timestamp(System.currentTimeMillis()));
        member.setName(name);
        member.setShortName(shortName);
        if (recommandID!=null&&StringUtils.isNotBlank(recommandID)) {
            member.setRecommandID(recommandID);
            member.setRecommendID(recommandID);
        }
        member.setChannelID(channelID);
        member.setPhone(phone);
        member.setSiteID(siteID);
        member.setLoginName(loginName);
        member.setLoginPassword(password);
        if (StringUtils.isNotBlank(birthday)) {
            member.setBirthday(TimeTest.strToTimes(birthday));
        }
        if (StringUtils.isNotBlank(titleID)) {
            member.setTitleID(titleID);
        }
        member.setApplicationID(site.getApplicationID());
        member.setCompanyID(site.getCompanyID());
        member.setAvatarURL(avatarURL);
        member.setServiceMemberID(serviceMemberID);
        member.setDistributorCompanyID(distributorCompanyID);
        int level = 1;
        if (recommand != null) {
            if (recommand.getLevelNumber() != null) {
                level = recommand.getLevelNumber() + 1;
            }
            if (StringUtils.isNotBlank(recommand.getRecommandChain())) {
                member.setRecommandChain(recommand.getRecommandChain() + "," + recommand.getRecommandCode());
                member.setRecommendChain(recommand.getRecommandChain() + "," + recommand.getRecommandCode());
            } else {
                member.setRecommandChain(recommand.getRecommandCode());
                member.setRecommendChain(recommand.getRecommandCode());
            }
        }
        String reCode = memberService.getCurRecommandCode(site.getApplicationID());
        if (StringUtils.isNotEmpty(reCode)) {
            member.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
            member.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
        } else {
            member.setRecommandCode("00001");
            member.setRecommendCode("00001");
        }
        Map<String, Object> pMap = Maps.newHashMap();
        pMap.put("applicationID", site.getApplicationID());
        pMap.put("code", "memberLockNeedVerify");
        String code = parameterService.selectParameterApplication(pMap);
        int isLock = 0;
        if (StringUtils.isNotBlank(code) && code.equals("1")) isLock = 1;
        member.setBizBlock(site.getBizBlock());
        member.setIsLock(isLock);
        member.setLevelNumber(level);
        memberService.insertMember(member);
        if (canChangeLinkChain) {
            Map<String, Object> linkParam = changeLinkChain(member.getId(), linkMemberID, request);
            boolean flag = (boolean) linkParam.get("success");
            String linkMemberIDNew = (String) linkParam.get("linkMemberID");
            String linkChain = (String) linkParam.get("linkChain");
            member.setLinkMemberID(linkMemberIDNew);
            member.setLinkChain(linkChain);
        }
        if (recommand != null) {
            MessageDto messageDto = new MessageDto();
            messageDto.setName("会员" + member.getName() + "手机号" + member.getPhone() + "注册成功，恭喜你。");
            messageDto.setApplicationID(member.getApplicationID());
            messageDto.setDescription("会员" + member.getName() + "手机号" + member.getPhone() + "注册成功，恭喜你。");
            messageDto.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
            messageDto.setObjectID(member.getId());
            messageDto.setObjectName(member.getName());
            messageDto.setReceiverMemberID(recommand.getId());
            messageDto.setSenderMemberID(member.getId());
            messageDto.setSendCompanyID(member.getCompanyID());
            messageDto.setReceiveCompanyID(recommand.getCompanyID());
            kingBase.messageSend(messageDto, request);
        }
        String ipaddr = WebUtil.getRemortIP(request);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);

        // session检查
        memberService.checkSession(deviceID, session.getId(), member.getId(), siteID, member.toString());

        String description = String.format("设计得到会员注册");
        MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), member.getApplicationID(),
                deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());

        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
        sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description, Memberlog.MemberOperateType.memberPhoneRegister);
        memberService.createMemberPrivilege(site.getApplicationID(), member.getId());
        // 检查memberDevice
        memberService.checkMemberDevice(member.getId(), deviceID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", session.getId());
        rsMap.put("memberID", member.getId());
        rsMap.put("recommandCode", member.getRecommandCode());
        //新增memberLogin
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", mem.getId());
        memberLogin.setMemberID(mem.getId());
        map1.put("memberName", mem.getName());
        memberLogin.setMemberName(mem.getName());
        map1.put("memberShortName", mem.getShortName());
        memberLogin.setMemberShortName(mem.getShortName());
        map1.put("memberAvatar", mem.getAvatarURL());
        memberLogin.setMemberAvatar(mem.getAvatarURL());
        if (StringUtils.isNotBlank(loginName)) {
            map1.put("loginName", loginName);
            memberLogin.setLoginName(loginName);
        } else {
            map1.put("loginName", mem.getLoginName());
            memberLogin.setLoginName(mem.getLoginName());
        }
        map1.put("phone", mem.getPhone());
        memberLogin.setPhone(mem.getPhone());
        map1.put("weixinToken", mem.getWeixinToken());
        memberLogin.setWeixinToken(mem.getWeixinToken());
        map1.put("weixinUnionID", mem.getWeixinUnionID());
        memberLogin.setWeixinUnionID(mem.getWeixinUnionID());
        map1.put("recommendCode", mem.getRecommendCode());
        memberLogin.setRecommendCode(mem.getRecommendCode());
        map1.put("recommendID", mem.getRecommandID());
        memberLogin.setRecommendID(mem.getRecommandID());
        map1.put("companyID", mem.getCompanyID());
        memberLogin.setCompanyID(mem.getCompanyID());
        map1.put("shopID", mem.getShopID());
        memberLogin.setShopID(mem.getShopID());
        map1.put("rankID", mem.getRankID());
        memberLogin.setRankID(mem.getRankID());
        map1.put("peopleID", mem.getPeopleID());
        memberLogin.setPeopleID(mem.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }

        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);
        return MessagePacket.newSuccess(rsMap, "memberPhoneRegisterSJ success");
    }

    @ApiOperation(value = "getMemberBlankLinkList", notes = "获取一个会员可用接点列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberBlankLinkList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberBlankLinkList(HttpServletRequest request, ModelMap map) {
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
        String beginTime = request.getParameter("beginTime");
        String endTime = request.getParameter("endTime");
        String sortTypeCode = request.getParameter("sortTypeCode");
        if (StringUtils.isNotBlank(sortTypeCode) && !NumberUtils.isNumeric(sortTypeCode)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeCode请输入的数字");
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(member.getLinkChain())) {
            param.put("linkChain", member.getLinkChain() + "," + member.getRecommandCode());
        } else {
            param.put("linkChain", member.getRecommandCode());
        }
        param.put("recommandCode", member.getRecommandCode());
        param.put("applicationID", member.getApplicationID());
        List<String> linkMember = memberService.getLinkList(param);
        List<String> linkMembers = new ArrayList<>();
        if (!linkMember.isEmpty()) {
            for (String s : linkMember) {
                List<String> members = memberService.getLinkMemberList(s);
                if (members.size() >= 3) {
                    continue;
                }
                linkMembers.add(s);
            }
        }
        map.put("linkMembers", linkMembers);
        if (StringUtils.isNotBlank(sortTypeCode)) {
            map.put("sortTypeCode", sortTypeCode);
        }
        if (StringUtils.isNotBlank(beginTime)) {
            map.put("beginTime", beginTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            map.put("endTime", endTime);
        }
        List<ApiMemberLinkDto> list = new ArrayList<>();
        if (!linkMembers.isEmpty()) {
            list = memberService.getMemberBlankLinkList(map);
        }
        if (!list.isEmpty()) {
            PageInfo<ApiMemberLinkDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (memberLogDTO != null) {
            String description = String.format("获取一个会员可用接点列表");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberBlankLinkList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberBlankLinkList success");
    }

    @ApiOperation(value = "resetMemberUpLinkMemberID", notes = "修改一个会员的接点上级")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/resetMemberUpLinkMemberID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket resetMemberUpLinkMemberID(HttpServletRequest request, ModelMap map) {
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
        String linkMemberID = request.getParameter("linkMemberID");
        if (StringUtils.isBlank(linkMemberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "linkMemberID不能为空");
        }
        Member linkMember = memberService.selectById(linkMemberID);
        if (linkMember == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "linkMemberID不正确");
        }
        String memberID = request.getParameter("memberID");
        String maxNumber = request.getParameter("maxNumber");
        if (StringUtils.isBlank(maxNumber)) {
            maxNumber = "3";
        }
        List<String> members = memberService.getLinkMemberList(linkMember.getId());
        if (members != null && members.size() >= Integer.parseInt(maxNumber)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "该上级的接点下级已超出最大数");
        }
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member changeMember = memberService.selectById(memberID);
        if (changeMember == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        String oldLinkChain = null;
        String newLinkChain = null;
        if (StringUtils.isEmpty(changeMember.getLinkChain())) {
            oldLinkChain = changeMember.getRecommandCode();
        } else {
            oldLinkChain = String.format("%s,%s", changeMember.getLinkChain(), changeMember.getRecommandCode());
        }
        if (StringUtils.isEmpty(linkMember.getLinkChain())) {
            newLinkChain = String.format("%s,%s", linkMember.getRecommandCode(), changeMember.getRecommandCode());
        } else {
            newLinkChain = String.format("%s,%s,%s", linkMember.getLinkChain(),
                    linkMember.getRecommandCode(), changeMember.getRecommandCode());
        }
        String linkChain = "";
        if (StringUtils.isNotBlank(linkMember.getLinkChain())) {
            linkChain = String.format("%s,%s", linkMember.getLinkChain(), linkMember.getRecommandCode());
        } else {
            linkChain = String.format("%s", linkMember.getRecommandCode());
        }
        Map<String, Object> memberParam = new HashMap<>();
        memberParam.put("oldChain", oldLinkChain);
        memberParam.put("newChain", newLinkChain);
        Map<String, Object> linkMemberParam = new HashMap<>();
        linkMemberParam.put("id", memberID);
        linkMemberParam.put("linkChain", linkChain);
        linkMemberParam.put("linkMemberID", linkMemberID);
        memberService.updateMember(linkMemberParam);
        memberService.updateLinkChain(memberParam);
        if (linkMember != null) {
            MessageDto messageDto = new MessageDto();
            messageDto.setName("恭喜您，会员" + changeMember.getName() + "已经安置到您的部门，请及时联络，开始您的精彩人生。");
            messageDto.setApplicationID(linkMember.getApplicationID());
            messageDto.setDescription("恭喜您，会员" + changeMember.getName() + "已经安置到您的部门，请及时联络，开始您的精彩人生。");
            messageDto.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
            messageDto.setObjectID(linkMember.getId());
            messageDto.setObjectName(linkMember.getName());
            messageDto.setReceiverMemberID(linkMember.getId());
            messageDto.setSenderMemberID(changeMember.getId());
            messageDto.setSendCompanyID(changeMember.getCompanyID());
            messageDto.setReceiveCompanyID(linkMember.getCompanyID());
            kingBase.messageSend(messageDto, request);
        }
        if (changeMember != null) {
            MessageDto messageDto = new MessageDto();
            messageDto.setName("您好，已经为您指定了接点上级：会员" + linkMember.getName() + "，请及时和他联系，共同开创精彩事业！");
            messageDto.setApplicationID(changeMember.getApplicationID());
            messageDto.setDescription("您好，已经为您指定了接点上级：会员" + linkMember.getName() + "，请及时和他联系，共同开创精彩事业！");
            messageDto.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
            messageDto.setObjectID(changeMember.getId());
            messageDto.setObjectName(changeMember.getName());
            messageDto.setReceiverMemberID(changeMember.getId());
            messageDto.setSenderMemberID(linkMember.getId());
            messageDto.setSendCompanyID(linkMember.getCompanyID());
            messageDto.setReceiveCompanyID(changeMember.getCompanyID());
            kingBase.messageSend(messageDto, request);
        }
        if (memberLogDTO != null) {
            String description = String.format("修改一个会员的接点上级");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.resetMemberUpLinkMemberID);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "resetMemberUpLinkMemberID success");
    }

    @ApiOperation(value = "updateMyMemberInfo", notes = "设置我的个人资料")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberInfo", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberInfo(HttpServletRequest request, ModelMap map) {
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
        String name = request.getParameter("name");
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
        }
        String shortName = request.getParameter("shortName");
        if (StringUtils.isNotBlank(shortName)) {
            map.put("shortName", shortName);
        }
        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }
        String description = request.getParameter("description");
        if (StringUtils.isNotBlank(description)) {
            map.put("description", description);
        }
        String titleID = request.getParameter("titleID");
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "titleID不正确");
            }
            map.put("titleID", titleID);
        }
        String birthday = request.getParameter("birthday");
        if (StringUtils.isNotBlank(birthday)) {
            if (!TimeTest.dateIsPass(birthday)) {
                return MessagePacket.newFail(MessageHeader.Code.dateTypeNotFound, "birthday请输入yyyy-MM-dd格式");
            }
            map.put("birthday", TimeTest.strToTimes(birthday));
        }
        String email = request.getParameter("email");
        if (StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }
        String address = request.getParameter("address");
        if (StringUtils.isNotBlank(address)) {
            map.put("address", address);
        }
        String marriageType = request.getParameter("marriageType");
        if (StringUtils.isNotBlank(marriageType)) {
            if (!NumberUtils.isNumeric(marriageType)) {
                return MessagePacket.newFail(MessageHeader.Code.dateTypeNotFound, "marriageType请输入数字");
            }
            map.put("marriageType", Integer.valueOf(marriageType));
        }
        String starDefineID = request.getParameter("starDefineID");
        if (StringUtils.isNotBlank(starDefineID)) {
            if (!starDefineService.checkIdIsValid(starDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.starDefineIDNotFound, "starDefineID不正确");
            }
            map.put("starDefineID", starDefineID);
        }
        String emotionDictionaryID = request.getParameter("emotionDictionaryID");
        if (StringUtils.isNotBlank(emotionDictionaryID)) {
            if (!dictionaryService.checkIsValidId(emotionDictionaryID)) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "emotionDictionaryID不正确");
            }
            map.put("emotionDictionaryID", emotionDictionaryID);
        }
        String bloodDIctionaryID = request.getParameter("bloodDIctionaryID");
        if (StringUtils.isNotBlank(bloodDIctionaryID)) {
            if (!dictionaryService.checkIsValidId(bloodDIctionaryID)) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "bloodDIctionaryID不正确");
            }
            map.put("bloodDIctionaryID", bloodDIctionaryID);
        }
        String highth = request.getParameter("highth");
        if (StringUtils.isNotBlank(highth)) {
            if (!NumberUtils.isNumeric(highth)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "highth请输入数字");
            }
            map.put("highth", Double.valueOf(highth));
        }
        String weight = request.getParameter("weight");
        if (StringUtils.isNotBlank(weight)) {
            if (!NumberUtils.isNumeric(weight)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "weight请输入数字");
            }
            map.put("weight", Double.valueOf(weight));
        }
        String BMI = request.getParameter("BMI");
        if (StringUtils.isNotBlank(BMI)) {
            if (!NumberUtils.isNumeric(BMI)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "BMI请输入数字");
            }
            map.put("BMI", Double.valueOf(BMI));
        }
        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            if (!cityService.checkIdIsValid(cityID)) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDNotFound, "cityID不正确");
            }
            map.put("cityID", cityID);
        }
        String schoolID = request.getParameter("schoolID");
        if (StringUtils.isNotBlank(schoolID)) {
            if (StringUtils.isBlank(schoolService.checkIdIsValid(schoolID))) {
                return MessagePacket.newFail(MessageHeader.Code.schoolIDNotFound, "学校ID不正确");
            }
            map.put("schoolID", schoolID);
        }
        String graduationYear = request.getParameter("graduationYear");
        if (StringUtils.isNotBlank(graduationYear)) {
            if (!NumberUtils.isNumeric(graduationYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "graduationYear请输入数字");
            }
            map.put("graduationYear", Integer.valueOf(graduationYear));
        }
        String jobYear = request.getParameter("jobYear");
        if (StringUtils.isNotBlank(jobYear)) {
            if (!NumberUtils.isNumeric(jobYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "jobYear请输入数字");
            }
            map.put("jobYear", Integer.valueOf(jobYear));
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }
        String peopleID = request.getParameter("peopleID");
        if (StringUtils.isNotBlank(peopleID)) {
            if (!memberService.checkPeopleId(peopleID)) {
                return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "peopleID不正确");
            }
            map.put("peopleID", peopleID);
        }
        String headName = request.getParameter("headName");
        if (StringUtils.isNotBlank(headName)) {
            map.put("headName", headName);
        }
        String idNumber = request.getParameter("idNumber");
        if (StringUtils.isNotBlank(idNumber)) {
            map.put("idNumber", idNumber);
        }
        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID)) {
            if (!cityService.checkIdIsValid(shengID)) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDNotFond, "shengID不正确");
            }
            map.put("shengID", shengID);
        }
        String shengName = request.getParameter("shengName");
        if (StringUtils.isNotBlank(shengName)) {
            map.put("shengName", shengName);
        }
        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID)) {
            if (!cityService.checkIdIsValid(shiID)) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDNotFound, "shiID不正确");
            }
            map.put("shiID", shiID);
        }
        String shiName = request.getParameter("shiName");
        if (StringUtils.isNotBlank(shiName)) {
            map.put("shiName", shiName);
        }
        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID)) {
            if (!cityService.checkIdIsValid(xianID)) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDNotFound, "xianID不正确");
            }
            map.put("xianID", xianID);
        }
        String xianName = request.getParameter("xianName");
        if (StringUtils.isNotBlank(xianName)) {
            map.put("xianName", xianName);
        }
        String zhenID = request.getParameter("zhenID");
        if (StringUtils.isNotBlank(zhenID)) {
            if (!cityService.checkIdIsValid(zhenID)) {
                return MessagePacket.newFail(MessageHeader.Code.zhenIDNotFound, "zhenID不正确");
            }
            map.put("zhenID", zhenID);
        }
        String zhenName = request.getParameter("zhenName");
        if (StringUtils.isNotBlank(zhenName)) {
            map.put("zhenName", zhenName);
        }
        String companyName = request.getParameter("companyName");
        if (StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
            map.put("careerApplyTime", TimeTest.getTime());
        }
        String companyJob = request.getParameter("companyJob");
        if (StringUtils.isNotBlank(companyJob)) {
            map.put("companyJob", companyJob);
        }
        String companyAddress = request.getParameter("companyAddress");
        if (StringUtils.isNotBlank(companyAddress)) {
            map.put("companyAddress", companyAddress);
        }
        String companyTel = request.getParameter("companyTel");
        if (StringUtils.isNotBlank(companyTel)) {
            map.put("companyTel", companyTel);
        }
        String industryCategoryID = request.getParameter("industryCategoryID");
        if (StringUtils.isNotBlank(industryCategoryID)) {
            if (!categoryService.checkCategoryId(industryCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.industryCategoryIDIsError, "industryCategoryID不正确");
            }
            map.put("industryCategoryID", industryCategoryID);
        }
        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (StringUtils.isNotBlank(professionalCategoryID) && !categoryService.checkCategoryId(professionalCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "professionalCategoryID不正确");
        } else {
            map.put("professionalCategoryID", professionalCategoryID);
        }
        String qqName = request.getParameter("qqName");
        if (StringUtils.isNotBlank(qqName)) {
            map.put("qqName", qqName);
        }
        String weixin = request.getParameter("weixin");
        if (StringUtils.isNotBlank(weixin)) {
            map.put("weixin", weixin);
        }
        String jobCategoryID = request.getParameter("jobCategoryID");
        if (StringUtils.isNotBlank(jobCategoryID)) {
            //小叶要求20220921
//            if (!categoryService.checkCategoryId(jobCategoryID)) {
//                return MessagePacket.newFail(MessageHeader.Code.jobCategoryIDNotFound, "jobCategoryID不正确");
//            }
            map.put("jobCategoryID", jobCategoryID);
        }
        String jobYearCategoryID = request.getParameter("jobYearCategoryID");
        if (StringUtils.isNotBlank(jobYearCategoryID)) {
            if (!categoryService.checkCategoryId(jobYearCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobYearCategoryID不正确");
            }
            map.put("jobYearCategoryID", jobYearCategoryID);
        }
        String educationCategoryID = request.getParameter("educationCategoryID");
        if (StringUtils.isNotBlank(educationCategoryID)) {
            if (!categoryService.checkCategoryId(educationCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.educationCategoryIDNotFound, "educationCategoryID不正确");
            }
            map.put("educationCategoryID", educationCategoryID);
        }
        String bodyCategoryID = request.getParameter("bodyCategoryID");
        if (StringUtils.isNotBlank(bodyCategoryID)) {
            if (!categoryService.checkCategoryId(bodyCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "bodyCategoryID不正确");
            }
            map.put("bodyCategoryID", bodyCategoryID);
        }
        String qiyeweixinID = request.getParameter("qiyeweixinID");
        if (StringUtils.isNotBlank(qiyeweixinID)) {
            map.put("qiyeweixinID", qiyeweixinID);
        }

        String isPublishMe = request.getParameter("isPublishMe");
        if (StringUtils.isNotBlank(isPublishMe)) {
            if (!NumberUtils.isNumeric(isPublishMe)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "isPublishMe请输入数字");
            }
            map.put("isPublishMe", Integer.valueOf(isPublishMe));
        }
        String career = request.getParameter("career");
        if (StringUtils.isNotBlank(career)) {
            map.put("career", career);
        }
        map.put("id", member.getId());
        memberService.updateMember(map);
        // 同时修改统计表头像
        MemberStatisticsDto memberStatisticsDto = memberStatisticsService.getByMemID(member.getId());
        if (memberStatisticsDto != null) {
            memberStatisticsService.updateByID(new HashMap<String, Object>() {
                {
                    put("id", memberStatisticsDto.getId());
                    if (StringUtils.isNotBlank(member.getAvatarURL())) {
                        put("avatarURL", member.getAvatarURL());
                    }
                }
            });
        }
        if (memberLogDTO != null) {
            String description1 = String.format("设置我的个人资料");
            sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.updateMyMemberInfo);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberInfo success");
    }

    @ApiOperation(value = "updateMyMemberShortname", notes = "设置我的昵称")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
            @ApiImplicitParam(paramType = "query", name = "shortName", value = "昵称", dataType = "String"),
    })
    @RequestMapping(value = "/updateMyMemberShortname", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberShortname(HttpServletRequest request, ModelMap map) {
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
        String shortName = request.getParameter("shortName");
        if (StringUtils.isBlank(shortName)) {
            return MessagePacket.newFail(MessageHeader.Code.shortNameNotNull, "shortName不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(shortName)) {
            param.put("shortName", shortName);
        }
        param.put("id", member.getId());
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的昵称");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberShortname);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberShortname success");
    }

    @ApiOperation(value = "updateMyMemberShortDescription", notes = "设置我的个性签名简单介绍")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberShortDescription", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberShortDescription(HttpServletRequest request, ModelMap map) {
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
        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isBlank(shortDescription)) {
            return MessagePacket.newFail(MessageHeader.Code.shortDescriptionNotNull, "shortDescription不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(shortDescription)) {
            param.put("shortDescription", shortDescription);
        }
        param.put("id", member.getId());
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的个性签名简单介绍");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberShortDescription);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberShortDescription success");
    }

    @ApiOperation(value = "updateMyMemberAvatar", notes = "设置我的头像")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberAvatar", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberAvatar(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Map<String, Object> param = new HashMap<>();

        String avatarURL = request.getParameter("avatarURL");
        String avatarSmall = request.getParameter("avatarSmall");
        String avatarMIddle = request.getParameter("avatarMIddle");
        String avatarLarge = request.getParameter("avatarLarge");
        String avatarBack = request.getParameter("avatarBack");
        if (StringUtils.isBlank(avatarURL)
                && StringUtils.isBlank(avatarSmall)
                && StringUtils.isBlank(avatarMIddle)
                && StringUtils.isBlank(avatarLarge)
                && StringUtils.isBlank(avatarBack)
        ) {
            return MessagePacket.newFail(MessageHeader.Code.avatarURLNotNull, "avatarURL,avatarSmall,avatarMIddle,avatarLarge,avatarBack不能同时为空");
        }
        if (StringUtils.isNotBlank(avatarURL)) {
            param.put("avatarURL", avatarURL);
        }
        if (StringUtils.isNotBlank(avatarSmall)) {
            param.put("avatarSmall", avatarSmall);
        }
        if (StringUtils.isNotBlank(avatarMIddle)) {
            param.put("avatarMIddle", avatarMIddle);
        }
        if (StringUtils.isNotBlank(avatarLarge)) {
            param.put("avatarLarge", avatarLarge);
        }

        if (StringUtils.isNotBlank(avatarBack)) {
            param.put("avatarBack", avatarBack);
        }
        param.put("id", member.getId());

        if (StringUtils.isNotBlank(member.getAvatarURL()) && !member.getAvatarURL().equals(avatarURL)) {
            String filesID = applicationService.getFilesIDByFullURL(member.getAvatarURL());
            if (StringUtils.isNotBlank(filesID)) applicationService.detectFiles(filesID);
//            kingBase.deleteQiniuFile(member.getAvatarURL());
        }
        memberService.updateMember(param);
        // 同时修改统计表头像
        MemberStatisticsDto memberStatisticsDto = memberStatisticsService.getByMemID(member.getId());
        if (memberStatisticsDto != null) {
            memberStatisticsService.updateByID(new HashMap<String, Object>() {
                {
                    put("id", memberStatisticsDto.getId());
                    if (StringUtils.isNotBlank(avatarURL)) {
                        put("avatarURL", avatarURL);
                    }
                }
            });
        }
        if (memberLogDTO != null) {
            String description = String.format("设置我的头像");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberAvatar);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberAvatar success");
    }

    @ApiOperation(value = "updateMyMemberBirthday", notes = "设置我的生日")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberBirthday", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberBirthday(HttpServletRequest request, ModelMap map) {
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
        String birthday = request.getParameter("birthday");
        if (StringUtils.isBlank(birthday)) {
            return MessagePacket.newFail(MessageHeader.Code.birthdayNotNull, "birthday不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(birthday)) {
            param.put("birthday", TimeTest.strToTimes(birthday));
        }
        param.put("id", member.getId());
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的生日");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberBirthday);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberBirthday success");
    }

    @ApiOperation(value = "updateMyMemberTitle", notes = "设置我的性别成为")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberTitle", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberTitle(HttpServletRequest request, ModelMap map) {
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
        String titleID = request.getParameter("titleID");
        if (StringUtils.isBlank(titleID)) {
            return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotNull, "titleID不能为空");
        }
        if (!categoryService.checkCategoryId(titleID)) {
            return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "titleID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(titleID)) {
            param.put("titleID", titleID);
        }
        param.put("id", member.getId());
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的性别成为");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberTitle);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberTitle success");
    }

    @ApiOperation(value = "forgetTradePassword", notes = "忘记交易密码支付密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/forgetTradePassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket forgetTradePassword(HttpServletRequest request, ModelMap map) {
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
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号不能为空");
        }
        Map<String, Object> requestMap = Maps.newHashMap();
        requestMap.put("Isphone",true);
        Map<String, Object> adressIp = kingBase.getAdressIp(request, member.getApplicationID(), null,  phone,requestMap);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }
        String newTradePassword = request.getParameter("newTradePassword");
        if (StringUtils.isBlank(newTradePassword)) {
            return MessagePacket.newFail(MessageHeader.Code.passwordNotNull, "newTradePassword不能为空");
        }
        String authCode = request.getParameter("authCode");
        if (StringUtils.isBlank(authCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotNull, "验证码不能为空");
        }
        String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FIND_MEMBER_BUY_PASS, phone));
        if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(authCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
        }
        stringRedisTemplate.delete(String.format("%s_%s", CacheContant.LOGIN_AUTH_CODE, phone));
        Map<String, Object> updateParam = new HashMap<>();
        updateParam.put("id", member.getId());
        String psd = MD5.encodeMd5(String.format("%s%s", newTradePassword, Config.ENCODE_KEY));
        updateParam.put("tradePassword", psd);
        memberService.updateMember(updateParam);
        redisTemplate.delete(String.format("%s_%s", CacheContant.FIND_MEMBER_BUY_PASS, phone));
        if (memberLogDTO != null) {
            String description = String.format("忘记交易密码支付密码");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.forgetTradePassword);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "forgetTradePassword success");
    }

    @ApiOperation(value = "getMemberPassword", notes = "获取登录密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMemberPassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberPassword(HttpServletRequest request, ModelMap map) {
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
        Member mem = memberService.selectById(member.getId());
        if (memberLogDTO != null) {
            String description = String.format("获取登录密码");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMemberPassword);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("password", mem == null ? null : mem.getLoginPassword());
        rsMap.put("lastChangePWTime", mem == null ? null : mem.getLastChangePWTime());
        return MessagePacket.newSuccess(rsMap, "getMemberPassword success");
    }

    @ApiOperation(value = "getMyTradePassword", notes = "获取会员交易密码支付密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMyTradePassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMyTradePassword(HttpServletRequest request, ModelMap map) {
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
        Member mem = memberService.selectById(member.getId());
        if (memberLogDTO != null) {
            String description = String.format("获取会员交易密码支付密码");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMyTradePassword);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("password", (mem == null ? null : mem.getTradePassword()));
        return MessagePacket.newSuccess(rsMap, "getMyTradePassword success");
    }

    @ApiOperation(value = "setTradePassword", notes = "设置交易密码支付密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/setTradePassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket setTradePassword(HttpServletRequest request, ModelMap map) {
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
        member = memberService.selectById(member.getId());
        String oldTradePassword = request.getParameter("oldTradePassword");
        String newTradePassword = request.getParameter("newTradePassword");
        if (StringUtils.isBlank(newTradePassword)) {
            return MessagePacket.newFail(MessageHeader.Code.passwordNotNull, "newTradePassword不能为空");
        }
        if (StringUtils.isNotBlank(member.getTradePassword()) && StringUtils.isBlank(oldTradePassword)) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "oldTradePassword不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(oldTradePassword)) {
            if (oldTradePassword.length() > 6) {
                if (!oldTradePassword.equals(member.getTradePassword())) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不正确");
                }
                if (oldTradePassword.equals(newTradePassword)) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不能与新密码一致");
                }
                param.put("tradePassword", newTradePassword);
            } else {
                if (!MD5.encodeMd5(String.format("%s%s", oldTradePassword, Config.ENCODE_KEY)).equals(member.getTradePassword())) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不正确");
                }
                if (oldTradePassword.equals(newTradePassword)) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不能与新密码一致");
                }
                String psd = MD5.encodeMd5(String.format("%s%s", newTradePassword, Config.ENCODE_KEY));
                param.put("tradePassword", psd);
            }
        } else {
            if (newTradePassword.length() > 6) {
                param.put("tradePassword", newTradePassword);
            } else {
                String psd = MD5.encodeMd5(String.format("%s%s", newTradePassword, Config.ENCODE_KEY));
                param.put("tradePassword", psd);
            }
        }
        param.put("id", member.getId());
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置交易密码支付密码");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.setTradePassword);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "setTradePassword success");
    }

    @ApiOperation(value = "setMemberPassword", notes = "修改登录密码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/setMemberPassword", produces = {"application/json;charset=UTF-8"})
    public MessagePacket setMemberPassword(HttpServletRequest request, ModelMap map) {
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
        String memberID = member.getId();
        Map<String, Object> adressIp = kingBase.getAdressIp(request, member.getApplicationID(), null,null, null);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }
        member = memberService.selectById(memberID);
        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        if (StringUtils.isBlank(newPassword)) {
            return MessagePacket.newFail(MessageHeader.Code.passwordNotNull, "newPassword不能为空");
        }
        if (StringUtils.isNotBlank(oldPassword)) {
            boolean isMD5 = false;
            String oldPsd = "";
            if (oldPassword.length() > 20) {
                isMD5 = true;
            }
            if (isMD5) {
                oldPsd = oldPassword;
                if (!oldPsd.equals(member.getLoginPassword())) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不正确");
                }
            } else {
                oldPsd = MD5.encodeMd5(String.format("%s%s", oldPassword, Config.ENCODE_KEY));
                if (!oldPsd.equals(member.getLoginPassword())) {
                    oldPsd = MD5.encodeMd5(oldPassword);
                    if (!oldPsd.equals(member.getLoginPassword())) {
                        return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不正确");
                    }
                }
            }
            if (newPassword.length() > 20) {
                if (oldPsd.equals(newPassword)) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不能与新密码一致");
                }
            } else {
                String md5NewPassword = MD5.encodeMd5(String.format("%s%s", newPassword, Config.ENCODE_KEY));
                if (oldPsd.equals(md5NewPassword)) {
                    md5NewPassword = MD5.encodeMd5(md5NewPassword);
                    if (oldPsd.equals(md5NewPassword)) {
                        return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不能与新密码一致");
                    }
                }
            }
        } else {
            if (StringUtils.isNotBlank(member.getLoginPassword())) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "oldPassword旧密码不能为空");
            }
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(newPassword)) {
            //前端已经md5加密时候不要再加密
            if (newPassword.length() < 32){
                String psd = MD5.encodeMd5(String.format("%s%s", newPassword, Config.ENCODE_KEY));
                param.put("loginPassword", psd);
            }
            else if (newPassword.length() >= 32){
                param.put("loginPassword", newPassword);
            }
        }
        param.put("id", member.getId());
        param.put("lastChangePWTime", TimeTest.getTimeStr());
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("修改登录密码");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.setMemberPassword);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "setMemberPassword success");
    }

    @ApiOperation(value = "thirdLogin", notes = "第三方登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "deviceID", value = "设备ID", dataType = "String")
    })
    @RequestMapping(value = "/thirdLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket thirdLogin(HttpServletRequest request, ModelMap map) {
        String deviceID = request.getParameter("deviceID");
        String avatarImage = request.getParameter("avatarImage");
        String memberID = request.getParameter("memberID");
        String recommendCode = request.getParameter("recommendCode");
        String type = request.getParameter("type");
        String publicNo = request.getParameter("appServerCode");
        String roleType = request.getParameter("roleType");
        String serviceMemberID = request.getParameter("serviceMemberID");
        String distributorCompanyID = request.getParameter("distributorCompanyID");
        int levelNumber = 0;
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        if (StringUtils.isBlank(type)) {
            return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "type不能为空");
        }
        String code = request.getParameter("code");
        if (StringUtils.isBlank(code)) {
            return MessagePacket.newFail(MessageHeader.Code.codeNotNull, "code不能为空");
        }
        if (StringUtils.isNotBlank(serviceMemberID)) {
            if (!memberService.checkMemberId(serviceMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "serviceMemberID不正确");
            }
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            if (!NumberUtils.isNumeric(bizBlock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "bizBlock请输入数字");
            }
        } else {
            bizBlock = "0";
        }
        Member member = null;
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (StringUtils.isBlank(companyService.getIsValidIdBy(distributorCompanyID))) {
                return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "distributorCompanyID不正确");
            }
            levelNumber = 1;
        }
        //通过recommendCode查询出推荐人
        if (StringUtils.isNotBlank(recommendCode)) {
            Map<String, Object> maps = new HashMap<>();
            String applicationID = site.getApplicationID();
            if (StringUtils.isNotBlank(applicationID)) maps.put("applicationID", applicationID);
            maps.put("recommandCode", recommendCode);
            member = memberService.getMemberByRecommandCode(maps);
            if (member != null) {
                memberID = member.getId();
//                String description = String.format("recommendCode:" + recommendCode + "查询到memberID:" + member.getId() + "recommendChain:" + member.getRecommandChain());
                String description = String.format("通过recommendCode:%s查询到memberID:%s,recommendChain:%s", recommendCode, member.getId(), member.getRecommandChain());
                sendMessageService.sendMemberLogMessage(null, description, request, Memberlog.MemberOperateType.CREATEONEPATIENTMEMBER);
                if (StringUtils.isEmpty(distributorCompanyID)) {
                    distributorCompanyID = member.getDistributorCompanyID();
//                    String descriptions = String.format("recommendCode:" + recommendCode + "查询到memberID:" + member.getId() + "distributorCompanyID:" + member.getDistributorCompanyID());
                    String descriptions = String.format("recommendCode:%s，查询到memberID:%s,distributorCompanyID:%s", recommendCode, member.getId(), member.getDistributorCompanyID());
                    sendMessageService.sendMemberLogMessage(null, descriptions, request, Memberlog.MemberOperateType.CREATEONEPATIENTMEMBER);
                }
            } else {
                String description = String.format("通过recommendCode:" + recommendCode + "未查询到符合条件的memberID");
                sendMessageService.sendMemberLogMessage(null, description, request, Memberlog.MemberOperateType.CREATEONEPATIENTMEMBER);
            }
        } else if (StringUtils.isNotBlank(memberID)) {
            member = memberService.selectById(memberID);
        }
        if (member != null && StringUtils.isNotBlank(member.getDistributorCompanyID())) {
            if (StringUtils.isEmpty(distributorCompanyID)) {
                distributorCompanyID = member.getDistributorCompanyID();
            }
            memberID = member.getId();
            levelNumber = member.getLevelNumber() + 1;
        }
        /**
         * 排重
         */
        String redisCode = stringRedisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.CODE_KEY.key, code));
        if (StringUtils.isNotBlank(redisCode)) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "请勿重新登录！");
        }
        stringRedisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.CODE_KEY.key, code), code, 30, TimeUnit.SECONDS);
        String name = request.getParameter("name");
        String unionID = request.getParameter("unionID");
        if (StringUtils.isBlank(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }
        if (!type.equals("1") && !type.equals("2") && !type.equals("3") && !type.equals("4") && !type.equals("5")) {
            return MessagePacket.newFail(MessageHeader.Code.typeNotNull, "请输入正确type");
        }
        String typeCode = null;
        Member members = null;
        switch (type) {
            case "1":
                typeCode = "qqToken";
                break;
            case "2":
                typeCode = "weixinToken";
                break;
            case "3":
                typeCode = "weiboToken";
                break;
            case "4":
                typeCode = "zhifubaoToken";
                break;
            case "5":
                typeCode = "dingdingID";
                break;
        }
        String phone = request.getParameter("phone");
        Map<String, Object> memParam = new HashMap<>();
        memParam.put("distributorCompanyID", distributorCompanyID);
        memParam.put("typeCode", typeCode);
        memParam.put("code", code);
        memParam.put("levelNumber", levelNumber);
        memParam.put("applicationID", site.getApplicationID());
        members = this.memberService.getThirdMember(memParam);//根据typeCode code applicationID 到member表查询符合条件的记录 如果为空 则创建一条会员记录
        boolean isnew = false;
        if (null == members) {
            members = registerThird(levelNumber, site, code, typeCode, name, phone, avatarImage, memberID, unionID, publicNo, roleType, serviceMemberID, distributorCompanyID, bizBlock);
            isnew = true;
        } else {
            if (members.getIsLock() == Constants.ISLOCK_YES) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "会员被锁定!");
            }
            map.put("memberID", members.getId());
            WeixinAppMember weixinAppMember = weixinMemberService.getByMap(map);
            if (weixinAppMember != null) {
                weixinMemberService.updateWeixinAppMember(weixinAppMember);
            }
            map.clear();
        }
        deviceService.updateLastLoadTime(deviceID);
        memParam.put("id", members.getId());
        memParam.put("phone", phone);
        memParam.put("loginName", phone);
        memberService.updateMember(memParam);//修改会员的信息
        map.put("memberID", members.getId());
        map.put("deviceID", deviceID);
        String memberDeviceID = memberDeviceService.getIdByMap(map);
        if (StringUtils.isBlank(memberDeviceID)) {
            map.put("name", members.getName() + deviceService.getNameById(deviceID));
            map.put("creator", members.getId());
            map.put("id", IdGenerator.uuid32());
            memberDeviceService.insertByMap(map);
        }
        String ipaddr = WebUtil.getRemortIP(request);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);
        UserAgent userAgent = UserAgentUtil.getUserAgent(request.getHeader("user-agent"));
//        String description = String.format("会员" + members.getName() + " -" + distributorCompanyID + "-" + members.getDistributorCompanyID() + "-" + "第三方登录");
        String description = String.format("会员%s-%s-%s-第三方登录", members.getName(), distributorCompanyID, members.getDistributorCompanyID());
        MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), members.getApplicationID(),
                deviceID, members.getId(), memberService.getLocationID(ipaddr), members.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), members, 7, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, 7, TimeUnit.DAYS);
        stringRedisTemplate.delete(String.format("%s%s", RedisKey.Key.CODE_KEY.key, code));
        sendMessageService.sendMemberLoginMessage(isnew, session.getId(), userAgent == null ? null : userAgent.getBrowserType(), ipaddr, description, Memberlog.MemberOperateType.thirdLogin);

        // 极光别名
        ApiDeviceDetailDto dto = deviceService.selectByID(deviceID);
        String pushDeviceID = dto.getPushDeviceID();
        if (StringUtils.isNotBlank(pushDeviceID)) {
            map.clear();
            map.put("code", "JpushAppKey");
            map.put("siteID", siteID);
            String jpushAppKey = parameterSiteService.getValueByMap(map);
            map.put("code", "JpushMasterSecret");
            String jpushMasterSecret = parameterSiteService.getValueByMap(map);
            if (StringUtils.isNotBlank(jpushAppKey) && StringUtils.isNotBlank(jpushMasterSecret)) {
                JPushClient jPushClient = new JPushClient(jpushMasterSecret, jpushAppKey);
                try {
                    DefaultResult defaultResult = jPushClient.updateDeviceTagAlias(pushDeviceID, members.getId(), null, null);
                    log.info("log" + defaultResult.toString());
                    TagAliasResult result = jPushClient.getDeviceTagAlias(pushDeviceID);
                    log.info("log" + members.getName() + "别名：" + result.toString());
                } catch (APIConnectionException e) {
                    log.error("log",e);
                } catch (APIRequestException e) {
                    log.error("log",e);
                }
            }
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", session.getId());
        rsMap.put("memberID", members.getId());
        rsMap.put("shortName", members.getShortName());
        rsMap.put("avatarURL", members.getAvatarURL());
        rsMap.put("recommendCode", members.getRecommendCode());
        rsMap.put("roleType", members.getRoleType());
        rsMap.put("bizBlock", members.getBizBlock());
        return MessagePacket.newSuccess(rsMap, "thirdLogin success");
    }

    @ApiOperation(value = "createOnePatientMember", notes = "创建一个患者会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/createOnePatientMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket createOnePatientMember(HttpServletRequest request, ModelMap relationshipMap) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        Map<String, Object> requestMap = Maps.newHashMap();
        Map<String, Object> adressIp = kingBase.getAdressIp(request, member.getApplicationID(), null,null, requestMap);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }


        String companyID = request.getParameter("companyID");//必须，默认是当前应用的companyID，通常是医院的公司ID
        if (StringUtils.isEmpty(companyID)) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不能为空");
        }
        if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
        }
        String shopID = request.getParameter("shopID");//可以为空，通常是医院的分店
        if (StringUtils.isNotBlank(shopID)) {
            return MessagePacket.newFail(MessageHeader.Code.shopIDNotFound, "shopID不正确");
        }
        String name = request.getParameter("name");//姓名，必须
        if (StringUtils.isEmpty(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }
        String titleID = request.getParameter("titleID");//性别，必须
        if (StringUtils.isEmpty(titleID)) {
            return MessagePacket.newFail(MessageHeader.Code.titleIdIsNull, "titleID不能为空");
        }
        String phone = request.getParameter("phone");//手机号，必须
        if (StringUtils.isEmpty(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }
        String birthday = request.getParameter("birthday");//生日，必须
        /*if (StringUtils.isEmpty(birthday)) {
            return MessagePacket.newFail(MessageHeader.Code.birthdayNotNull, "birthday不能为空");
        }*/
        String idNumber = request.getParameter("idNumber");//身份证号码，必须
        /*if (StringUtils.isEmpty(idNumber)) {
            return MessagePacket.newFail(MessageHeader.Code.idNumberNotNull, "idNumber不能为空");
        }*/
        String marriageType = request.getParameter("marriageType");//婚姻，可以为空
        String bloodDictionaryID = request.getParameter("bloodDictionaryID");//血型，可以为空
        String highth = request.getParameter("highth");//身高，可以为空
        String weight = request.getParameter("weight");//体重，可以为空
        String shengID = request.getParameter("shengID");//省ID，可以为空
        String shengName = request.getParameter("shengName");// 可以为空
        String shiID = request.getParameter("shiID");//市可以为空
        String shiName = request.getParameter("shiName");// ,可以为空
        String xianID = request.getParameter("xianID");//县，可以为空
        String xianName = request.getParameter("xianName");//县name
        String zhenID = request.getParameter("zhenID");//镇ID，可以为空
        String zhenName = request.getParameter("zhenName");//镇Name，可以为空
        String residentsType = request.getParameter("residentsType");//居住类型（1= 本地居民2= 外地户籍在此工作3=临时来此）
        String homeAddr = request.getParameter("homeAddr");//居住地址，必须
        String homeZip = request.getParameter("homeZip");//可以为空
        String parentGroupCategoryID = request.getParameter("parentGroupCategoryID");//可以为空
        String career = request.getParameter("career");//可以为空
        String headName = request.getParameter("headName");//可以为空
        String companyTel = request.getParameter("companyTel");//可以为空
        String domainAccount = request.getParameter("domainAccount");//可以为空
        String tokenAddress = request.getParameter("tokenAddress");//可以为空
        String peopleID = request.getParameter("peopleID");//身份证号码，必须
        Member patientMember = new Member();
        String id = IdGenerator.uuid32();
        patientMember.setId(id);
        patientMember.setCreator(member.getId());
        patientMember.setRoleType(3);
        patientMember.setApplicationID(member.getApplicationID());
        patientMember.setSiteID(memberLogDTO.getSiteId());
        patientMember.setBizBlock(0);
        patientMember.setName(name);
        patientMember.setShortName(name);
        patientMember.setLoginName(phone);
        patientMember.setCreateType(3);
        patientMember.setLoginPassword(MD5.encodeMd5("111111"));
        patientMember.setIdType(1);
        String reCode = memberService.getCurRecommandCode(member.getApplicationID());
        if (StringUtils.isNotEmpty(reCode)) {
            patientMember.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
            patientMember.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
        } else {
            patientMember.setRecommandCode("00001");
            patientMember.setRecommendCode("00001");
        }
        patientMember.setRecommandID(member.getId());
        patientMember.setRecommendID(member.getId());
        if (StringUtils.isNotBlank(member.getRecommandChain())) {
            patientMember.setRecommandChain(member.getRecommandChain() + "," + member.getRecommandCode());
            patientMember.setRecommendChain(member.getRecommandChain() + "," + member.getRecommandCode());
        } else {
            patientMember.setRecommandChain(member.getRecommandCode());
            patientMember.setRecommendChain(member.getRecommandCode());
        }
        patientMember.setLinkMemberID(member.getId());
        if (StringUtils.isNotBlank(member.getLinkChain())) {
            patientMember.setLinkChain(member.getLinkChain() + "," + member.getRecommandCode());
        } else {
            patientMember.setLinkChain(member.getRecommandCode());
        }
        patientMember.setLevelNumber(member.getLevelNumber() == null ? 1 : member.getLevelNumber().intValue() + 1);
        if (StringUtils.isNotBlank(companyID)) {
            patientMember.setCompanyID(companyID);
        }
        if (StringUtils.isNotBlank(shopID)) {
            patientMember.setShopID(shopID);
        }
        if (StringUtils.isNotBlank(peopleID)) {
            patientMember.setPeopleID(peopleID);
        }
        if (StringUtils.isNotBlank(titleID)) {
            patientMember.setTitleID(titleID);
        }
        patientMember.setPhone(phone);
        if (StringUtils.isNotBlank(birthday)) {
            patientMember.setBirthday(TimeTest.strToDate(birthday));
        }
        patientMember.setMarriageType(marriageType);
        if (StringUtils.isNotBlank(bloodDictionaryID)) {
            if (!dictionaryService.checkIsValidId(bloodDictionaryID)) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "bloodDictionaryID不正确");
            }
            patientMember.setBloodDIctionaryID(bloodDictionaryID);
        }
        if (StringUtils.isNotBlank(highth)) {
            patientMember.setHighth(Double.parseDouble(highth));
        }
        if (StringUtils.isNotBlank(weight)) {
            patientMember.setWeight(Double.parseDouble(weight));
        }
        if (StringUtils.isNotBlank(highth) && StringUtils.isNotBlank(weight)) {
            Double w = Double.valueOf(weight);
            Double h = Double.valueOf(highth);
//            patientMember.setBMI(w / (h * h));
            patientMember.setBMI(BigDecimal.valueOf(w)
                    .divide(BigDecimal.valueOf(h)
                            .multiply(BigDecimal.valueOf(h)), 2, RoundingMode.HALF_UP)
                    .doubleValue());
        }
        if (StringUtils.isNotBlank(shengID)) {
            if (!cityService.checkIdIsValid(shengID)) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            patientMember.setShengID(shengID);
        }
        patientMember.setShengName(shengName);
        if (StringUtils.isNotBlank(shiID)) {
            if (!cityService.checkIdIsValid(shiID)) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            patientMember.setShiID(shiID);
        }
        patientMember.setShiName(shiName);
        if (StringUtils.isNotBlank(xianID)) {
            if (!cityService.checkIdIsValid(xianID)) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            patientMember.setXianID(xianID);
        }
        patientMember.setXianName(xianName);
        if (StringUtils.isNotBlank(zhenID)) {
            if (!cityService.checkIdIsValid(zhenID)) {
                return MessagePacket.newFail(MessageHeader.Code.zhenIDIsError, "zhenID不正确");
            }
            patientMember.setZhenID(zhenID);
        }
        patientMember.setZhenName(zhenName);
        if (StringUtils.isNotBlank(residentsType)) {
            patientMember.setResidentsType(Integer.parseInt(residentsType));
        }
        patientMember.setHomeAddr(homeAddr);
        patientMember.setHomeZip(homeZip);
        if (StringUtils.isNotBlank(parentGroupCategoryID)) {
            if (!categoryService.checkCategoryId(parentGroupCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "parentGroupCategoryID不正确");
            }
            patientMember.setParentGroupCategoryID(parentGroupCategoryID);
        }
        if (StringUtils.isNotBlank(career)) {
            patientMember.setCareer(career);
        }
        if (StringUtils.isNotBlank(headName)) {
            patientMember.setHeadName(headName);
        }
        if (StringUtils.isNotBlank(companyTel)) {
            patientMember.setCompanyTel(companyTel);
        }
        if (StringUtils.isNotBlank(domainAccount)) {
            patientMember.setDomainAccount(domainAccount);
        }
        if (StringUtils.isNotBlank(tokenAddress)) {
            patientMember.setTokenAddress(tokenAddress);
        }
        if (StringUtils.isNotBlank(idNumber)) {
            patientMember.setIdNumber(idNumber);
        }
        memberService.insertMember(patientMember);

        String applicationID = member.getApplicationID();

        Friend friend = new Friend();
        friend.setFriendID(IdGenerator.uuid32());
        friend.setApplicationID(applicationID);
        friend.setName(member.getName() + name);
        String friendType = request.getParameter("friendType");
        if (StringUtils.isNotBlank(friendType)) {
            if (NumberUtils.isNumeric(friendType)) {
                friend.setFriendType(Integer.valueOf(friendType));
            } else {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "friendType 必须为整数类型");
            }
        }
        friend.setMemberID(member.getId());
        friend.setToMemberID(id);
        friend.setApplyTime(new Date());
        friend.setAnswerTime(new Date());
        friend.setIsAgree(1);

        friendService.insert(friend);

        Map<String, Object> memberFollowMap = new HashMap<>();
        memberFollowMap.put("id", IdGenerator.uuid32());
        memberFollowMap.put("applicationID", applicationID);
        memberFollowMap.put("name", member.getName() + name);
        memberFollowMap.put("memberID", member.getId());
        memberFollowMap.put("followID", id);
        memberFollowMap.put("applyTime", new Date());
        memberFollowMap.put("answerTime", new Date());
        memberFollowMap.put("isAgree", 1);
        memberFollowService.insertMemberFollow(memberFollowMap);


        relationshipMap.put("id", IdGenerator.uuid32());
        relationshipMap.put("applicationID", applicationID);
        relationshipMap.put("companyID", companyID);
        String relationType1 = request.getParameter("relationType");
        if (StringUtils.isNotBlank(relationType1)) {
            if (NumberUtils.isNumeric(relationType1)) {
                Integer relationType = Integer.valueOf(relationType1);
                if (relationType == 10) relationType = 2;
                relationshipMap.put("relationType", relationType);
            } else {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "relationType 必须为整数类型");
            }
        }
        relationshipMap.put("name", name);
        relationshipMap.put("memberID", member.getId());
        relationshipMap.put("toMemberID", id);
        relationshipMap.put("applyTime", new Date());
        relationshipMap.put("answerTime", new Date());
        relationshipMap.put("isAgree", 1);
        relationshipMap.put("tel", phone);
        relationshipMap.put("tTel", phone);
        relationshipService.insert(relationshipMap);

        // == 创建会员发送队列 ==
        // session检查
        String ipaddr = WebUtil.getRemortIP(request);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);

        Member mr = memberService.selectById(id);
        memberService.checkSession(null, session.getId(), id, mr.getSiteID(), mr.toString());

        String description2 = "会员:" + name + "注册成功";
        MemberLogDTO memberLogDTO1 = new MemberLogDTO(mr.getSiteID(), mr.getApplicationID(),
                null, mr.getId(), memberService.getLocationID(ipaddr), mr.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), mr, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO1, redisTimeOut, TimeUnit.SECONDS);

        sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description2,
                Memberlog.MemberOperateType.addSuccess);
        memberService.createMemberPrivilege(applicationID, mr.getId());
        // == 创建会员发送队列 ==

        /**
         * 发送创建一个患者会员队列
         */
        sendMessageService.createOnePatientMemberQueue(patientMember.getId());
        String description = String.format("%s创建一个患者会员", member.getName());
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.CREATEONEPATIENTMEMBER);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", patientMember.getId());
        return MessagePacket.newSuccess(rsMap, "createOnePatientMember success");
    }

    @ApiOperation(value = "updateOnePatientMember", notes = "修改一个患者的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateOnePatientMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateOnePatientMember(HttpServletRequest request) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");//会员id
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        String name = request.getParameter("name");//姓名，必须
        if (StringUtils.isEmpty(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }
        String titleID = request.getParameter("titleID");//性别，必须
        if (StringUtils.isEmpty(titleID)) {
            return MessagePacket.newFail(MessageHeader.Code.titleIdIsNull, "titleID不能为空");
        }
        String phone = request.getParameter("phone");//手机号，必须
        if (StringUtils.isEmpty(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }
        String birthday = request.getParameter("birthday");//生日，必须
        if (StringUtils.isEmpty(birthday)) {
            return MessagePacket.newFail(MessageHeader.Code.birthdayNotNull, "birthday不能为空");
        }
        String marriageType = request.getParameter("marriageType");//婚姻，可以为空
        String bloodDictionaryID = request.getParameter("bloodDictionaryID");//血型，可以为空
        String height = request.getParameter("height");//身高，可以为空
        String weight = request.getParameter("weight");//体重，可以为空
        String shengID = request.getParameter("shengID");//省ID，可以为空
        String shengName = request.getParameter("shengName");// 可以为空
        String shiID = request.getParameter("shiID");//市可以为空
        String shiName = request.getParameter("shiName");// ,可以为空
        String xianID = request.getParameter("xianID");//县，可以为空
        String xianName = request.getParameter("xianName");//县name
        String zhenID = request.getParameter("zhenID");//镇ID，可以为空
        String zhenName = request.getParameter("zhenName");//镇Name，可以为空
        String residentsType = request.getParameter("residentsType");//居住类型（1= 本地居民2= 外地户籍在此工作3=临时来此）
        String homeAddr = request.getParameter("homeAddr");//居住地址，必须
        String homeZip = request.getParameter("homeZip");//可以为空
        String peopleID = request.getParameter("peopleID");//可以为空
        String idNumber = request.getParameter("idNumber");//可以为空
        String parentGroupCategoryID = request.getParameter("parentGroupCategoryID");//可以为空
        String career = request.getParameter("career");//可以为空
        String headName = request.getParameter("headName");//可以为空
        String companyTel = request.getParameter("companyTel");//可以为空
        String domainAccount = request.getParameter("domainAccount");//可以为空
        String tokenAddress = request.getParameter("tokenAddress");//可以为空
        Map<String, Object> param = new HashMap<>();
        param.put("id", memberID);
        param.put("name", name);
        param.put("shortName", name);
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "titleID不正确");
            }
            param.put("titleID", titleID);
        }
        if (StringUtils.isNotBlank(peopleID)) {
            if (!memberService.checkPeopleId(peopleID)) {
                return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "peopleID不正确");
            }
            param.put("peopleID", peopleID);
        }
        if (StringUtils.isNotBlank(idNumber)) {
            param.put("idNumber", idNumber);
        }
        param.put("phone", phone);
        if (StringUtils.isNotBlank(birthday)) {
            if (!TimeTest.dateIsPass(birthday)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "birthday请输入yyyy-MM-dd的格式");
            }
            param.put("birthday", birthday);
        }
        if (StringUtils.isNotBlank(marriageType)) {
            if (!NumberUtils.isNumeric(marriageType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "marriageType请输入数字1,2,3");
            }
            param.put("marriageType", marriageType);
        }
        if (StringUtils.isNotBlank(bloodDictionaryID)) {
            if (!dictionaryService.checkIsValidId(bloodDictionaryID)) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "bloodDictionaryID不正确");
            }
            param.put("bloodDIctionaryID", bloodDictionaryID);
        }
        if (StringUtils.isNotBlank(height)) {
            param.put("highth", Double.parseDouble(height));
        }
        if (StringUtils.isNotBlank(weight)) {
            param.put("weight", Double.parseDouble(weight));
        }
        if (StringUtils.isNotBlank(shengID)) {
            if (!cityService.checkIdIsValid(shengID)) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            param.put("shengID", shengID);
        }
        if (StringUtils.isNotBlank(shengName)) {
            param.put("shengName", shengName);
        }
        if (StringUtils.isNotBlank(shiID)) {
            if (!cityService.checkIdIsValid(shiID)) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            param.put("shiID", shiID);
        }
        if (StringUtils.isNotBlank(shiName)) {
            param.put("shiName", shiName);
        }
        if (StringUtils.isNotBlank(xianID)) {
            if (!cityService.checkIdIsValid(xianID)) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            param.put("xianID", xianID);
        }
        if (StringUtils.isNotBlank(xianName)) {
            param.put("xianName", xianName);
        }
        if (StringUtils.isNotBlank(zhenID)) {
            if (!cityService.checkIdIsValid(zhenID)) {
                return MessagePacket.newFail(MessageHeader.Code.zhenIDIsError, "zhenID不正确");
            }
            param.put("zhenID", zhenID);
        }
        if (StringUtils.isNotBlank(zhenName)) {
            param.put("zhenName", zhenName);
        }
        if (StringUtils.isNotBlank(residentsType)) {
            if (!NumberUtils.isNumeric(residentsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "residentsType请输入数字");
            }
            param.put("residentsType", Integer.parseInt(residentsType));
        }
        if (StringUtils.isNotBlank(homeAddr)) {
            param.put("homeAddr", homeAddr);
        }
        if (StringUtils.isNotBlank(homeZip)) {
            param.put("homeZip", homeZip);
        }
        if (StringUtils.isNotBlank(parentGroupCategoryID)) {
            if (!categoryService.checkCategoryId(parentGroupCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "parentGroupCategoryID不正确");
            }
            param.put("parentGroupCategoryID", parentGroupCategoryID);
        }
        if (StringUtils.isNotBlank(career)) {
            param.put("career", career);
        }
        if (StringUtils.isNotBlank(headName)) {
            param.put("headName", headName);
        }
        if (StringUtils.isNotBlank(companyTel)) {
            param.put("companyTel", companyTel);
        }
        if (StringUtils.isNotBlank(domainAccount)) {
            param.put("domainAccount", domainAccount);
        }
        if (StringUtils.isNotBlank(tokenAddress)) {
            param.put("tokenAddress", tokenAddress);
        }
        memberService.updateMember(param);
        String description = String.format("%s修改一个患者的信息", member.getName());
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.UPDATEONEPATIENTMEMBER);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateOnePatientMember success");
    }

    @ApiOperation(value = "deleteOnePatientMember", notes = "删除一个患者会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/deleteOnePatientMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket deleteOnePatientMember(HttpServletRequest request) {
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
        String memberID = request.getParameter("memberID");//会员id
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member patientMember = memberService.selectById(memberID);
        if (patientMember == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "会员不存在");
        }
        if (StringUtils.isEmpty(patientMember.getLinkMemberID()) || !patientMember.getLinkMemberID().equals(member.getId())) {
            return MessagePacket.newFail(MessageHeader.Code.notMePatientMember, "不是我的患者病人");
        }
        if (StringUtils.isNotBlank(patientMember.getTradePassword()) || StringUtils.isNotBlank(patientMember.getLoginPassword())) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "密码已设置，无法删除");
        }
        memberService.delById(memberID);
        String description = String.format("%s删除一个患者会员", member.getName());
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.DELETEONEPATIENTMEMBER);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("deleteTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "deleteOnePatientMember success");
    }

    @ApiOperation(value = "getOnePatientMemberDetail", notes = "获取一个患者会员详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getOnePatientMemberDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOnePatientMemberDetail(HttpServletRequest request) {
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
        String memberID = request.getParameter("memberID");//会员id
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        PatientMemberDetailDto data = memberService.getOnePatientMemberDetail(memberID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", data);
        String description = String.format("%s获取一个患者会员详细信息", member.getName());
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.GETONEPATIENTMEMBERDETAIL);
        }
        return MessagePacket.newSuccess(rsMap, "getOnePatientMemberDetail success");
    }

    @ApiOperation(value = "getMyPatientMemberList", notes = "获取我的患者列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getMyPatientMemberList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMyPatientMemberList(HttpServletRequest request) {
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
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        String titleID = request.getParameter("titleID");
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "titleID不正确");
            }
            param.put("titleID", titleID);
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) {
            if (!TimeTest.isTimeType(beginTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "beginTime请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            param.put("beginTime", beginTime);
        }
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) {
            if (!TimeTest.isTimeType(endTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "endTime请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            param.put("endTime", endTime);
        }
        String sortTypeCode = request.getParameter("sortTypeCode");
        if (StringUtils.isNotBlank(sortTypeCode)) {
            param.put("sortTypeCode", sortTypeCode);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            param.put("sortTypeName", sortTypeName);
        }
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            param.put("keyWords", "%" + keyWords + "%");
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<PatientMemberListDto> list = memberService.getMyPatientMemberList(param);
        if (!list.isEmpty()) {
            PageInfo<PatientMemberListDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        } else {
            list = new ArrayList<>();
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        String description = String.format("%s获取我的患者列表", member.getName());
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.GETMYPATIENTMEMBERLIST);
        }
        return MessagePacket.newSuccess(rsMap, "getMyPatientMemberList success");
    }

    private static String strFormat3(String s1) {
        StringBuilder st = new StringBuilder("0");
        for (Integer i = 0; i < (4 - s1.length()); i++) {
            st.append("0");
        }
        st.append(s1);
        return st.toString();
    }

    private Member registerThird(Integer levelNumber, Site site, String code, String typeCode, String name, String phone, String avatarImage,
                                 String memberID, String unionID, String publicNo, String roleType, String serviceMemberID, String distributorCompanyID, String bizBlock) {
        Member member = new Member();
        member.setId(IdGenerator.uuid32());
        member.setCreatedTime(new Timestamp(System.currentTimeMillis()));
        member.setName(name);
        member.setShortName(name);
        if (StringUtils.isNotBlank(memberID)) {
            Member recommand = memberService.selectById(memberID);//通过推荐人的ID查询
            if (recommand != null) {
                member.setRecommandID(memberID);
                member.setRecommendID(memberID);
                //设置会员推荐链
                if (StringUtils.isNotBlank(recommand.getRecommandChain())) {
                    member.setRecommandChain(recommand.getRecommandChain() + "," + recommand.getRecommandCode());
                    member.setRecommendChain(recommand.getRecommandChain() + "," + recommand.getRecommandCode());
                } else {
                    member.setRecommandChain(recommand.getRecommandCode());
                    member.setRecommendChain(recommand.getRecommandCode());
                }
            }
        }
        member.setLevelNumber(levelNumber);
        member.setSiteID(site.getId());
        member.setApplicationID(site.getApplicationID());
        //2021-03-31 //qq发消息说小程序用的注册接口， member里面的公司ID，默认= Application里面的 公司ID
        Application application = applicationService.getById(site.getApplicationID());
        if (application != null) {
            log.info("log" + "应用不为空 生效：会员里面的公司ID从应用中获取");
            member.setCompanyID(application.getCompanyID());
        } else {
            member.setCompanyID(site.getCompanyID());
            log.info("log" + "应用为空 没生效：会员里面的公司ID从站点中获取");
        }
        member.setAvatarURL(avatarImage);
        if (StringUtils.isNotBlank(unionID)) {
            member.setWeixinUnionID(unionID);
        }
        switch (typeCode) {
            case "qqToken":
                member.setQqToken(code);
                member.setQq(name);
                break;
            case "weixinToken":
                member.setWeixinToken(code);
                member.setWeixin(name);
                break;
            case "weiboToken":
                member.setWeiboToken(code);
                member.setWeibo(name);
                break;
            case "zhifubaoToken":
                member.setZhifubao(name);
                member.setZhifubaoToken(code);
                break;
            case "dingdingID":
                member.setDingdingName(name);
                member.setDingdingID(code);
                break;
            default:
                break;
        }
        String reCode = memberService.getCurRecommandCode(site.getApplicationID());
        if (StringUtils.isNotEmpty(reCode)) {
            member.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
            member.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
        } else {
            member.setRecommandCode("00001");
            member.setRecommendCode("00001");
        }
        String value = parameterService.selectByCode("memberbizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            member.setBizBlock(Integer.valueOf(bizBlock));
        } else {
            if (StringUtils.isNotBlank(value)) {
                member.setBizBlock(Integer.valueOf(value));
            }
        }
        member.setIsLock(0);
        member.setCreateType(5);
        member.setQiyeweixinID(sequenceDefineService.genCode("qiyeweixinID", member.getId()));
        member.setLoginName(phone);
        member.setLoginPassword(MD5.encodeMd5(String.format("%s%s", "111111", Config.ENCODE_KEY)));
        if (StringUtils.isNotBlank(roleType)) {
            member.setRoleType(Integer.parseInt(roleType));
        }
        if (StringUtils.isNotBlank(serviceMemberID)) member.setServiceMemberID(serviceMemberID);
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            member.setDistributorCompanyID(distributorCompanyID);
        }
        memberService.insertMember(member);
//        if (StringUtils.isNotBlank(publicNo) /*&& StringUtils.isNotBlank(site.getCategory()) && site.getCategory().equals("5")*/) {
//            kingBase.addWeixinAppMember(member, site.getId(), code, publicNo);
//        }
        return member;
    }

    @ApiOperation(value = "updateMyMemberCareer", notes = "独立接口：设置我的职业标签")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberCareer", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberAgeCategoryID(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("id", member.getId());

        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String newCareer = request.getParameter("newCareer");
        if (StringUtils.isBlank(newCareer)) {
            return MessagePacket.newFail(MessageHeader.Code.careerNotNull, "career不能为空");
        }
        map.put("career", newCareer);

        String companyName = request.getParameter("companyName");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }
        String companyDepartment = request.getParameter("companyDepartment");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyDepartment)) {
            map.put("companyDepartment", companyDepartment);
        }
        String companyJob = request.getParameter("companyJob");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyJob)) {
            map.put("companyJob", companyJob);
        }
        String companyAddress = request.getParameter("companyAddress");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyAddress)) {
            map.put("companyAddress", companyAddress);
        }
        String companyTel = request.getParameter("companyTel");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyTel)) {
            map.put("companyTel", companyTel);
        }
        String industryCategoryID = request.getParameter("industryCategoryID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(industryCategoryID)) {
            map.put("industryCategoryID", industryCategoryID);
        }
        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(professionalCategoryID)) {
            map.put("professionalCategoryID", professionalCategoryID);
        }
        String jobCategoryID = request.getParameter("jobCategoryID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(jobCategoryID)) {
            map.put("jobCategoryID", jobCategoryID);
        }
        String jobYear = request.getParameter("jobYear");
        if (org.apache.commons.lang.StringUtils.isNotBlank(jobYear)) {
            if (!NumberUtils.isNumeric(jobYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "jobYear请输入合法数字");
            }
            map.put("jobYear", jobYear);
        }
        map.put("careerApplyTime", TimeTest.getTimeStr());
        map.put("careerStatus", 1);
        // map.put("idStatus",1);

        Timestamp time = TimeTest.getTime();
        memberService.updateMember(map);
        if (memberLogDTO != null) {
            String description = String.format("设置我的职业标签");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberCareer);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberCareer success");
    }


    @ApiOperation(value = "cancelMyMemberCareerApply", notes = "独立接口：取消我的职业认证申请")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/cancelMyMemberCareerApply", produces = {"application/json;charset=UTF-8"})
    public MessagePacket cancelMyMemberCareerApply(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("id", member.getId());

        if (member!=null&&member.getApplicationID()!=null){
            kingBase.getAdressIpCommon(request,member.getApplicationID(),null,null,null);
        }

        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogin memberLogin  = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
        if (memberLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberDetail memberDetail = memberService.getMemberDetail(memberLogin.getMemberID());
        if (memberDetail==null&&memberDetail.getCareerStatus()!=1&&memberDetail.getCareerStatus()!=2){
            return MessagePacket.newFail(MessageHeader.Code.unauth, "careerStatus当前状态无法修改");
        }


        map.put("careerApplyTime", null);
        map.put("careerStatus", 0);
        // map.put("idStatus",1);

        Timestamp time = TimeTest.getTime();
        memberService.updateMember(map);
        if (memberLogDTO != null) {
            String description = String.format("取消我的职业认证申请");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberCareer);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "cancelMyMemberCareerApply success");
    }

    public Map<String, Object> changeLinkChain(String memberID, String linkMemberID, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(memberID) || StringUtils.isBlank(linkMemberID)) {
            map.put("success", false);
            return map;
        }
        Member changeMember = memberService.selectById(memberID);
        Member linkMember = memberService.selectById(linkMemberID);
        if (changeMember == null || linkMember == null) {
            map.put("success", false);
            return map;
        }
        String oldLinkChain = null;
        String newLinkChain = null;
        if (StringUtils.isEmpty(changeMember.getLinkChain())) {
            oldLinkChain = changeMember.getRecommandCode();
        } else {
            oldLinkChain = String.format("%s,%s", changeMember.getLinkChain(), changeMember.getRecommandCode());
        }
        if (StringUtils.isEmpty(linkMember.getLinkChain())) {
            newLinkChain = String.format("%s,%s", linkMember.getRecommandCode(), changeMember.getRecommandCode());
        } else {
            newLinkChain = String.format("%s,%s,%s", linkMember.getLinkChain(),
                    linkMember.getRecommandCode(), changeMember.getRecommandCode());
        }
        String linkChain = "";
        if (StringUtils.isNotBlank(linkMember.getLinkChain())) {
            linkChain = String.format("%s,%s", linkMember.getLinkChain(), linkMember.getRecommandCode());
        } else {
            linkChain = String.format("%s", linkMember.getRecommandCode());
        }
        Map<String, Object> memberParam = new HashMap<>();
        memberParam.put("oldChain", oldLinkChain);
        memberParam.put("newChain", newLinkChain);
        Map<String, Object> linkMemberParam = new HashMap<>();
        linkMemberParam.put("id", memberID);
        linkMemberParam.put("linkChain", linkChain);
        linkMemberParam.put("linkMemberID", linkMemberID);
        memberService.updateMember(linkMemberParam);
        memberService.updateLinkChain(memberParam);
        if (request != null) {
            if (linkMember != null) {
                MessageDto messageDto = new MessageDto();
                messageDto.setName("恭喜您，会员" + changeMember.getName() + "已经安置到您的部门，请及时联络，开始您的精彩人生。");
                messageDto.setApplicationID(linkMember.getApplicationID());
                messageDto.setDescription("恭喜您，会员" + changeMember.getName() + "已经安置到您的部门，请及时联络，开始您的精彩人生。");
                messageDto.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
                messageDto.setObjectID(linkMember.getId());
                messageDto.setObjectName(linkMember.getName());
                messageDto.setReceiverMemberID(linkMember.getId());
                messageDto.setSenderMemberID(changeMember.getId());
                messageDto.setSendCompanyID(changeMember.getCompanyID());
                messageDto.setReceiveCompanyID(linkMember.getCompanyID());
                kingBase.messageSend(messageDto, request);
            }
            if (changeMember != null) {
                MessageDto messageDto = new MessageDto();
                messageDto.setName("您好，已经为您指定了接点上级：会员" + linkMember.getName() + "，请及时和他联系，共同开创精彩事业！");
                messageDto.setApplicationID(changeMember.getApplicationID());
                messageDto.setDescription("您好，已经为您指定了接点上级：会员" + linkMember.getName() + "，请及时和他联系，共同开创精彩事业！");
                messageDto.setObjectDefineID("8af5993a4fdaf145014fde1a732e00ff");
                messageDto.setObjectID(changeMember.getId());
                messageDto.setObjectName(changeMember.getName());
                messageDto.setReceiverMemberID(changeMember.getId());
                messageDto.setSenderMemberID(linkMember.getId());
                messageDto.setSendCompanyID(linkMember.getCompanyID());
                messageDto.setReceiveCompanyID(changeMember.getCompanyID());
                kingBase.messageSend(messageDto, request);
            }
        }
        map.put("success", true);
        map.put("linkMemberID", linkMemberID);
        map.put("linkChain", linkChain);
        return map;
    }

    @ApiOperation(value = "searchMemberByPhone", notes = "通过手机号搜索会员查询会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/searchMemberByPhone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket searchMemberByPhone(HttpServletRequest request, ModelMap map) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String sessionID = request.getParameter("sessionID");
        String applicationID = request.getParameter("applicationID");
        if (Config.SafeGrade) {
            String timeStamp = request.getParameter("timeStamp");
            String encryptString = request.getParameter("encryptString");
            Map<String, Object> map1 = kingBase.base64EncryptStringNew(request,timeStamp, encryptString);
            if (!map1.get("result").equals("通过")) {
                String result = map1.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//            if (member == null) {
//                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//            }
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
//            if (memberLogDTO == null) {
//                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//            }
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号码不能为空");
        }
        if (member == null && StringUtils.isEmpty(applicationID)) {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID和member不能同时为空");
        }
        Map<String, Object> memParam = new HashMap<>();
        memParam.put("phone", phone);
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            memParam.put("applicationID", applicationID);
        }
        if (member != null) {
            memParam.put("applicationID", member.getApplicationID());
        }

        Member mem = memberService.selectByPhoneAndApplicationID(memParam);
        if (mem == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "手机号码未找到会员");
        }
        if (memberLogDTO != null) {
            String description = String.format("通过手机号搜索会员查询会员");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.setTradePassword);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", mem.getId());
        rsMap.put("roleType", mem.getRoleType());
        rsMap.put("name", mem.getName());
        rsMap.put("shortName", mem.getShortName());
        rsMap.put("avatarURL", mem.getAvatarURL());
        rsMap.put("recommendCode", mem.getRecommendCode());
        rsMap.put("createdTime", mem.getCreatedTime());
        rsMap.put("loginName", mem.getLoginName());
        rsMap.put("phone", mem.getPhone());
        rsMap.put("openID", mem.getOpenID());
        rsMap.put("weibo", mem.getWeibo());
        rsMap.put("weiboToken", mem.getWeiboToken());
        rsMap.put("weiboID", mem.getWeiboID());
        rsMap.put("weixin", mem.getWeixin());
        rsMap.put("weixinToken", mem.getWeixinToken());
        rsMap.put("weixinUnionID", mem.getWeixinUnionID());
        rsMap.put("weixinCode", mem.getWeixinCode());
        rsMap.put("weiboAccessToken", mem.getWeiboAccessToken());
        rsMap.put("weiboTokenExpires", mem.getWeiboTokenExpires());
        rsMap.put("feishuID", mem.getFeishuID());
        rsMap.put("feishuName", mem.getFeishuName());
        rsMap.put("qiyeweixinID", mem.getQiyeweixinID());
        rsMap.put("qiyeweixinName", mem.getQiyeweixinName());
        rsMap.put("dingdingID", mem.getDingdingID());
        rsMap.put("dingdingName", mem.getDingdingName());

        sendMessageService.sendSystemLog("searchMemberByPhone", phone);

        return MessagePacket.newSuccess(rsMap, "searchMemberByPhone success");
    }

    @ApiOperation(value = "memberFastBandingPhone", notes = "会员快速绑定手机号")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/memberFastBandingPhone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberFastBandingPhone(HttpServletRequest request) {
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
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        param.put("phone", phone);
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("%s会员快速绑定手机号", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.memberFastBandingPhone);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "memberFastBandingPhone success");
    }

    @ApiOperation(value = "updateMyMemberResidentsType", notes = "设置我的会员居住类型")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberResidentsType", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberResidentsType(HttpServletRequest request) {
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
        String residentsType = request.getParameter("residentsType");
        if (StringUtils.isBlank(residentsType)) {
            return MessagePacket.newFail(MessageHeader.Code.residentsTypeIsNull, "居住类型不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("residentsType", Integer.parseInt(residentsType));
        memberService.updateMyMemberResidentsType(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员居住类型", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberResidentsType);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", new Timestamp(System.currentTimeMillis()));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberResidentsType success");
    }


    @ApiOperation(value = "updateMyMemberHomeAddr", notes = "设置我的会员居住地址")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberHomeAddr", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberHomeAddr(HttpServletRequest request) {
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
        String homeAddr = request.getParameter("homeAddr");
        if (StringUtils.isBlank(homeAddr)) {
            return MessagePacket.newFail(MessageHeader.Code.homeAddrIsNull, "居住地址不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        param.put("homeAddr", homeAddr);
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员居住地址", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberHomeAddr);
        }
        Timestamp time = new Timestamp(System.currentTimeMillis());
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberHomeAddr success");
    }

    @ApiOperation(value = "updateMyMemberDistrictID", notes = "设置我的会员区域")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberDistrictID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberDistrictID(HttpServletRequest request) {
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
        String districtID = request.getParameter("districtID");
        if (StringUtils.isBlank(districtID)) {
            return MessagePacket.newFail(MessageHeader.Code.districtIDNotNull, "居住区域不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        param.put("districtID", districtID);
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员区域", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberHomeAddr);
        }
        Timestamp time = new Timestamp(System.currentTimeMillis());
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberDistrictID success");
    }

    @ApiOperation(value = "updateMyMemberPropertyID", notes = "设置我的会员小区")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberPropertyID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberPropertyID(HttpServletRequest request) {
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
        String propertyID = request.getParameter("propertyID");
        if (StringUtils.isBlank(propertyID)) {
            return MessagePacket.newFail(MessageHeader.Code.propertyIDNotNull, "小区id不能为空");
        }
        if (!propertyService.checkIdIsValid(propertyID)) {
            return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "小区id不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        param.put("propertyID", propertyID);
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员小区", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberPropertyID);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberPropertyID success");
    }

    @ApiOperation(value = "updateMyMemberCompanyID", notes = "设置我的会员公司ID")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberCompanyID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberCompanyID(HttpServletRequest request) {
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
        String companyID = request.getParameter("companyID");
        String companyName = request.getParameter("companyName");
        String companyAddress = request.getParameter("companyAddress");
        if (StringUtils.isBlank(companyID) && StringUtils.isBlank(companyName) && StringUtils.isBlank(companyAddress)) {
            return MessagePacket.newFail(MessageHeader.Code.allOfThemNotNull, "companyID、companyName和companyAddress不能同时为空");
        }
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isBlank(companyID)) companyID = member.getCompanyID();
        Company company = companyService.selectById(companyID);
        if (company == null) {
            return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "companyID不正确");
        }
        param.put("memberID", member.getId());
        param.put("companyID", companyID);
        if (StringUtils.isNotBlank(companyName)) param.put("companyName", companyName);
        if (StringUtils.isNotBlank(companyAddress)) param.put("companyAddress", companyAddress);
        String companyDepartment = request.getParameter("companyDepartment");
        if (StringUtils.isNotBlank(companyDepartment)) param.put("companyDepartment", companyDepartment);
        String companyJob = request.getParameter("companyJob");
        if (StringUtils.isNotBlank(companyJob)) param.put("companyJob", companyJob);

        String companyTel = request.getParameter("companyTel");
        if (StringUtils.isNotBlank(companyTel)) param.put("companyTel", companyTel);

        String companyFax = request.getParameter("companyFax");
        if (StringUtils.isNotBlank(companyJob)) param.put("companyFax", companyFax);

        String companyZip = request.getParameter("companyZip");
        if (StringUtils.isNotBlank(companyZip)) param.put("companyZip", companyZip);

        String companyWebsite = request.getParameter("companyWebsite");
        if (StringUtils.isNotBlank(companyWebsite)) param.put("companyWebsite", companyWebsite);
        memberService.updateMemberCompanyID(param);
        if (StringUtils.isNotBlank(companyName) || StringUtils.isNotBlank(companyAddress)) {
            param.clear();
            param.put("id", companyID);
            //if (StringUtils.isNotBlank(companyName)) param.put("name", companyName);
            if (StringUtils.isNotBlank(companyAddress)) param.put("address", companyAddress);
            companyService.updateByMap(param);
        }
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员公司ID", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberCompanyID);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        Timestamp time = TimeTest.getTime();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberCompanyID success");
    }

    @ApiOperation(value = "bandingMemberByPhone", notes = "通过手机号绑定现有会员实现微信绑定")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "applicationID", value = "应用id", dataType = "String")
    })
    @RequestMapping(value = "/bandingMemberByPhone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket bandingMemberByPhone(HttpServletRequest request) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        if (Config.SafeGrade) {
            String timeStamp = request.getParameter("timeStamp");
            String encryptString = request.getParameter("encryptString");
            Map<String, Object> map1 = kingBase.base64EncryptStringNew(request,timeStamp, encryptString);
            if (!map1.get("result").equals("通过")) {
                String result = map1.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        String openID = request.getParameter("openID");
        String unionID = request.getParameter("unionID");
        ApiWeixinAppMemberDetail weixinAppMemberDetail = null;
        ApiWeixinMemberDetail weixinMemberDetail = null;

        Map<String, Object> openMap = Maps.newHashMap();
        openMap.put("openid", openID);
        openMap.put("weixinUnionID", unionID);

        if (StringUtils.isEmpty(openID) && StringUtils.isEmpty(unionID)) {
            return MessagePacket.newFail(MessageHeader.Code.codeNotNull, "openID和unionID不能同时为空");
        } else {
            weixinAppMemberDetail = weixinAppMemberService.getByMap(openMap);
            weixinMemberDetail = weixinMemberService.getByMapopen(openMap);
            if (weixinAppMemberDetail == null && weixinMemberDetail == null) {
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "无微信关注或身份匹配失败");
            }
        }


        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isEmpty(applicationID)) {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID应用id不能为空");
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isEmpty(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone手机号不能为空");
        }

        Map<String, Object> param = new HashMap<>();
        param.put("applicationID", applicationID);
        param.put("phone", phone);
        Member member = memberService.selectByPhoneAndApplicationID(param);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.phoneError, "phone手机号不正确");
        }
//        if (StringUtils.isNotBlank(member.getPhone())||StringUtils.isNotBlank(member.getOpenID())||StringUtils.isNotBlank(member.getWeixinUnionID())){
//            return MessagePacket.newFail(MessageHeader.Code.phoneError, "该会员已绑定");
//        }
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("weixinToken", openID);
        objectMap.put("weixinUnionID", unionID);
        objectMap.put("id", member.getId());
        memberService.updateMember(objectMap);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", member.getId());
        return MessagePacket.newSuccess(rsMap, "bandingMemberByPhone success");
    }

    @ApiOperation(value = "bandingMemberWithRecommandCode", notes = "通过推荐码和姓名绑定会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "bandingMemberWithRecommandCode", produces = {"application/json;charset=UTF-8"})
    public MessagePacket bandingMemberWithRecommandCode(HttpServletRequest request) {
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
        String recommandCode = request.getParameter("recommandCode");
        String recommandID = request.getParameter("recommandID");
        if (StringUtils.isEmpty(recommandCode) && StringUtils.isEmpty(recommandID)) {
            return MessagePacket.newFail(MessageHeader.Code.recommandCodeIsNull, "推荐人为空");
        }
        if (StringUtils.isNotBlank(member.getRecommandID())) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "已有推荐人，请联系管理员");
        }
        Member recommandMember = null;
        if (StringUtils.isNotBlank(recommandCode)) {
            Map<String, Object> param = new HashMap<>();
            param.put("recommandCode", recommandCode);
            param.put("applicationID", member.getApplicationID());
            recommandMember = memberService.getMemberByRecommandCode(param);
            if (recommandMember == null) {
                return MessagePacket.newFail(MessageHeader.Code.recommandCodeIsError, "推荐码不正确");
            }
            param = new HashMap<>();
            param.put("id", member.getId());
            param.put("recommandID", recommandMember.getId());
            memberService.updateMember(param);
        } else if (StringUtils.isNotBlank(recommandID)) {
            recommandMember = memberService.selectById(recommandID);
            if (recommandMember == null) {
                return MessagePacket.newFail(MessageHeader.Code.recommandCodeIsError, "推荐码不正确");
            }
            Map<String, Object> param = new HashMap<>();
            param = new HashMap<>();
            param.put("id", member.getId());
            param.put("recommandID", recommandMember.getId());
            memberService.updateMember(param);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", IdGenerator.uuid32());
        map.put("getType", 1);
        map.put("verifyStatus", 1);
        map.put("verifyDescription", "认证通过");
        map.put("verifyTime", new Timestamp(System.currentTimeMillis()));
        map.put("applytime", new Timestamp(System.currentTimeMillis()));
        map.put("creator", member.getId());
        map.put("memberID", member.getId());
        map.put("majorID", PARENT_MAJOR_ID);
        map.put("name", PARENT_MAJOR_NAME);
        map.put("applicationID", member.getApplicationID());
        memberMajorService.insert(map);
        String description = String.format("%s通过推荐码和姓名绑定会员", member.getName());
        sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.bandingEmployeeWithCode);
        Map<String, Object> rsMap = Maps.newHashMap();
        if (recommandMember != null) {
            rsMap.put("memberID", recommandMember.getId());
        }
        return MessagePacket.newSuccess(rsMap, "bandingMemberWithRecommandCode success");
    }

    @ApiOperation(value = "getMyMemberBody", notes = "获取我的身体健康心理数据")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/getMyMemberBody", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMyMemberBody(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(sessionID) && StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        if (StringUtils.isNotBlank(sessionID)) {
            Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//            Member member=memberService.selectById(sessionID);
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            if (StringUtils.isEmpty(memberID)) {
                memberID = member.getId();
            }
            MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            if (memberLogDTO != null) {
                String description = String.format("获取我的身体健康心理数据");
                sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getMyMemberBody);
            }
        }
        if (StringUtils.isNotBlank(memberID)) {
            if (StringUtils.isNotBlank(memberID) && !memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
            }
        }
        MyMemberDto myMemberDto = memberService.getMyMemberBody(memberID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", myMemberDto);
        return MessagePacket.newSuccess(rsMap, "getMyMemberBody success");
    }

    @ApiOperation(value = "updateMyMemberBody", notes = "设置我的身体心理健康数据")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberBody", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberBody(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String diseaseCategoriesID = request.getParameter("diseaseCategoriesID");
        String starDefineID = request.getParameter("starDefineID");
        String emotionDictionaryID = request.getParameter("emotionDictionaryID");
        String skinCategoryID = request.getParameter("skinCategoryID");
        String faceCategoryID = request.getParameter("faceCategoryID");
        String fiveCategoryID = request.getParameter("fiveCategoryID");
        String lineCategoryID = request.getParameter("lineCategoryID");
        String boneCategoryID = request.getParameter("boneCategoryID");
        String bodyCategoryID = request.getParameter("bodyCategoryID");
        String highth = request.getParameter("highth");
        String weight = request.getParameter("weight");
        String chest = request.getParameter("chest");
        String shoulder = request.getParameter("shoulder");
        String waist = request.getParameter("waist");
        String hips = request.getParameter("hips");
        String legs = request.getParameter("legs");
        String clothingSize = request.getParameter("clothingSize");
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(diseaseCategoriesID)) {
            param.put("diseaseCategoriesID", diseaseCategoriesID);
        }
        param.put("id", member.getId());
        if (StringUtils.isNotBlank(starDefineID)) {
            param.put("starDefineID", starDefineID);
        }
        if (StringUtils.isNotBlank(emotionDictionaryID)) {
            param.put("emotionDictionaryID", emotionDictionaryID);
        }
        if (StringUtils.isNotBlank(skinCategoryID)) {
            param.put("skinCategoryID", skinCategoryID);
        }
        if (StringUtils.isNotBlank(faceCategoryID)) {
            param.put("faceCategoryID", faceCategoryID);
        }
        if (StringUtils.isNotBlank(fiveCategoryID)) {
            param.put("fiveCategoryID", fiveCategoryID);
        }
        if (StringUtils.isNotBlank(lineCategoryID)) {
            param.put("lineCategoryID", lineCategoryID);
        }
        if (StringUtils.isNotBlank(boneCategoryID)) {
            param.put("boneCategoryID", boneCategoryID);
        }
        if (StringUtils.isNotBlank(bodyCategoryID)) {
            param.put("bodyCategoryID", bodyCategoryID);
        }
        if (StringUtils.isNotBlank(highth)) {
            param.put("highth", highth);
        }
        if (StringUtils.isNotBlank(weight)) {
            param.put("weight", weight);
        }
        if (StringUtils.isNotBlank(chest)) {
            param.put("chest", chest);
        }
        if (StringUtils.isNotBlank(shoulder)) {
            param.put("shoulder", shoulder);
        }
        if (StringUtils.isNotBlank(waist)) {
            param.put("waist", waist);
        }
        if (StringUtils.isNotBlank(hips)) {
            param.put("hips", hips);
        }
        if (StringUtils.isNotBlank(legs)) {
            param.put("legs", legs);
        }
        if (StringUtils.isNotBlank(clothingSize)) {
            param.put("clothingSize", clothingSize);
        }
        if (StringUtils.isNotBlank(highth) && StringUtils.isNotBlank(weight)) {
            int h = Integer.valueOf(highth);
            Double w = Double.valueOf(weight);
            Double BMI = w / (h * h / 10000);                                    //计算BMI的国际公式、结果为f1
            BigDecimal b = new BigDecimal(BMI);
            double f1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            param.put("BMI", f1);
        }
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的身体心理健康数据");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberBody);
        }
        Timestamp time = TimeTest.getTime();
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberBody success");
    }

    @ApiOperation(value = "getApplicationMemberList", notes = "独立接口：获取会员校友列表通讯录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getApplicationMemberList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getApplicationMemberList(HttpServletRequest request) {
        String sessionID = request.getParameter("sessionID");
        String applicationID = request.getParameter("applicationID");
        MemberLogDTO memberLogDTO = null;
        Map<String, Object> param = new HashMap<>();
        if (StringUtils.isNotBlank(sessionID)) {
            Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            param.put("applicationID", member.getApplicationID());
        } else if (StringUtils.isNotBlank(applicationID)) {
            param.put("applicationID", applicationID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "sessionID和applicationID不能同时为空");
        }
        String industryCategoryID = request.getParameter("industryCategoryID");
        if (StringUtils.isNotBlank(industryCategoryID)) {
            param.put("industryCategoryID", industryCategoryID);
        }
        String isLink = request.getParameter("isLink");
        if (StringUtils.isNotBlank(isLink)) {
            param.put("isLink", isLink);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            param.put("sortTypeName", sortTypeName);
        }
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) {
            param.put("sortTypeTime", sortTypeTime);
        }
        String keywords = request.getParameter("keywords");
        if (StringUtils.isNotBlank(keywords)) {
            param.put("keywords", keywords);
        }
        String majorID = request.getParameter("majorID");
        if (StringUtils.isNotBlank(majorID)) {
            param.put("majorID", majorID);
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<ApplicationMemberDto> list = memberService.getApplicationMemberList(param);
        if (!list.isEmpty()) {
            PageInfo<ApplicationMemberDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (memberLogDTO != null) {
            String description = String.format("%s获取会员校友列表通讯录", memberLogDTO.getMemberName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.GETAPPLICATIONMEMBERLIST);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getApplicationMemberList success");
    }

    @ApiOperation(value = "linkOneMember", notes = "独立接口：校友认证会员认证")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/linkOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket linkOneMember(HttpServletRequest request) {
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
        Member linkMember = memberService.selectById(member.getId());
        if (linkMember == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member setMember = memberService.selectById(memberID);
        if (setMember == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        if (StringUtils.isNotBlank(setMember.getLinkMemberID())) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "已认证，无法再次认证");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("linkMemberID", linkMember.getId());
        if (StringUtils.isNotBlank(linkMember.getLinkChain())) {
            param.put("linkChain", String.format("%s,%s", linkMember.getLinkChain(), linkMember.getRecommandCode()));
        } else {
            param.put("linkChain", String.format("%s", linkMember.getRecommandCode()));
        }
        String rankID = request.getParameter("rankID");
        if (StringUtils.isNotBlank(rankID)) {
            param.put("rankID", rankID);
            kingBase.saveMemberRank(setMember.getId(), rankID);
        }
        param.put("id", memberID);
        memberService.updateMember(param);
        /**
         * 发送验证通过消息
         */
        MessageDto messageDto = new MessageDto();
//        messageDto.setName(String.format("恭喜您，%s同意了您的认证申请。", member.getName()));
        messageDto.setName(String.format("您已通过认证，可以发布需求、响应需求并参与活动了。"));
        messageDto.setApplicationID(linkMember.getApplicationID());
        messageDto.setDescription(messageDto.getName());
        messageDto.setObjectDefineID(Config.MEMBERID_OBJECTDEFINEID);
        messageDto.setObjectID(linkMember.getId());
        messageDto.setObjectName(linkMember.getName());
        messageDto.setReceiverMemberID(setMember.getId());
        messageDto.setReceiveCompanyID(setMember.getCompanyID());
        messageDto.setSenderMemberID(member.getId());
        messageDto.setSendCompanyID(member.getCompanyID());
        messageDto.setCode("909");
        kingBase.messageSend(messageDto, request);
        if (memberLogDTO != null) {
            String description = String.format("%s校友认证会员认证", member.getName());
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.LINKONEMEMBER);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "linkOneMember success");
    }

    /**
     * 微信号通过公众号url获取用户信息
     *
     * @param request
     * @return
     */
    @RequestMapping("getThirdWeiXinAppValues")
    @ResponseBody
    public MessagePacket getThirdWeiXinAppValues(HttpServletRequest request) {
        String publicNo = request.getParameter("publicNo");
        String lang = request.getParameter("lang");
        String appid = request.getParameter("appid");
        String secret = request.getParameter("secret");
        String code = request.getParameter("code");
        Map tokenAndOpen = WechartApi.getGZHTokenAndOpenID(appid, secret, code);
        String openid = (String) tokenAndOpen.get("openid");
        String token = (String) tokenAndOpen.get("access_token");
//        String openid = WechartApi.getGZHOpenID(appid,secret,code);
        if (org.springframework.util.StringUtils.isEmpty(publicNo)) {
            return MessagePacket.newFail(MessageHeader.Code.publicNoIsNull, "publicNo不能为空");
        }
        Wechart wechart = wechartService.getWechartByPublicNo(publicNo);
        if (wechart == null) {
            return MessagePacket.newFail(MessageHeader.Code.publicNoIsError, "publicNo不正确");
        }
        Map<String, String> param = new HashMap<>();
        WeiXinThird weiXinThird = null;
        param.put("access_token", token);
        param.put("openid", openid);
        param.put("lang", lang);
        Map<String, String> rsMap = new HashMap<>();
        try {
            String responseStr = com.kingpivot.api.dto.weixin.HttpUtil.doGet(WeiXinUrlConstants.GET_THIRDWEIXINVALUE, param);
            weiXinThird = JacksonHelper.fromJson(responseStr, WeiXinThird.class);
        } catch (IOException e) {
            log.error("log",e);
        }
        if (weiXinThird != null) {
            rsMap.put("nickName", weiXinThird.getNickname());
            rsMap.put("sex", String.valueOf(weiXinThird.getSex()));
            rsMap.put("openid", weiXinThird.getOpenid());
            rsMap.put("headimgurl", weiXinThird.getHeadimgurl());
        }

        return MessagePacket.newSuccess(rsMap, "getThirdWeiXinAppValues success");
    }

    /**
     * 小程序获取微信openoid
     *
     * @param request
     * @return
     */
    @RequestMapping("getWeiXinAppOpenId")
    @ResponseBody
    public MessagePacket getOpenIDInfo(HttpServletRequest request) throws IOException, JSONException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String pno = request.getParameter("publicNo");
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(pno)) {
            return MessagePacket.newFail(MessageHeader.Code.publicNoIsNull, "publicNo 不能为空");
        }
        if (Config.SafeGrade) {
            String timeStamp = request.getParameter("timeStamp");
            String encryptString = request.getParameter("encryptString");
            Map<String, Object> map1 = kingBase.base64EncryptStringNew(request, timeStamp, encryptString);
            if (!map1.get("result").equals("通过")) {
                String result = map1.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        String publicNo = pno.trim();
        String js_code = request.getParameter("js_code");
        String encryptedData = request.getParameter("encryptedData");
        String iv = request.getParameter("iv");
        Wechart wechart = wechartService.getWechartByPublicNo(publicNo);
        if (wechart == null) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "微信记录不存在");
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        String openID = "";
        String unionID = "";
        if (wechart.getIsunionid() == null || wechart.getIsunionid() == 0) {
            String requestUrl = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    wechart.getAppId(), wechart.getAppSecret(), js_code);
            String info = HttpUtil.doGet(requestUrl);
            JSONObject jsonObject = null;
            if (StringUtils.isNotBlank(info)) {
                jsonObject = new JSONObject(info);
                if (jsonObject.has("openid")) {
                    log.info("log" + info);
                    rsMap.put("openID", jsonObject.get("openid"));
                    openID = String.valueOf(jsonObject.get("openid"));
                } else {
                    return MessagePacket.newFail(MessageHeader.Code.weixinFormID, String.valueOf(jsonObject.get("errmsg")));
                }

                if (info.contains("unionid")) {
                    if (jsonObject.has("unionid")) {
                        log.info("log" + info);
                        rsMap.put("unionid", jsonObject.get("unionid"));
                        openID = String.valueOf(jsonObject.get("unionid"));
                    } else {
                        return MessagePacket.newFail(MessageHeader.Code.weixinFormID, String.valueOf(jsonObject.get("errmsg")));
                    }
                }
            } else {
                log.info("log" + "error:" + info);
                return MessagePacket.newFail(MessageHeader.Code.weixinFormID, "获取openid异常，请联系管理员");
            }
        } else {
            Map<String, Object> map = WeiXinInfoUtil.decodeUserInfo(encryptedData, iv, js_code, wechart.getAppId(), wechart.getAppSecret());
            rsMap.put("openID", map.get("openid"));
            rsMap.put("unionID", map.get("unionId"));
            openID = String.valueOf(map.get("openid"));
            unionID = String.valueOf(map.get("unionId"));
        }
        //第一：修改  查询openID和（或者） unionid的 接口，查询到openID和（或者） unionid，
        // 先到 wenxinAPPmember表 ，查询 这个 openID和（或者） unionid是否存在，不存在的话，
        // 就创建1个记录， 作为合法openID和（或者） unionid依据
        Map<String, Object> openMap = Maps.newHashMap();
        openMap.put("openid",rsMap.get("openID"));
        openMap.put("weixinUnionID",rsMap.get("unionid"));
        ApiWeixinAppMemberDetail weixinAppMemberDetail = weixinAppMemberService.getByMap(openMap);
        if (weixinAppMemberDetail == null) {
            openMap.clear();
            openMap.put("wxAppOriginalID",pno);
            OutSystemWeixinAppDetail outSystemWeixinAppDetail = weixinAppService.getByMap(openMap);
            if (outSystemWeixinAppDetail!=null){
                String weixinMemberid = IdGenerator.uuid32();
                openMap.put("id",weixinMemberid);
                openMap.put("applicationID",outSystemWeixinAppDetail.getApplicationID());
                openMap.put("siteID",outSystemWeixinAppDetail.getSiteID());
                openMap.put("weixinAppID",outSystemWeixinAppDetail.getWeixinAppID());
                openMap.put("companyID",outSystemWeixinAppDetail.getCompanyID());
                openMap.put("shopID",outSystemWeixinAppDetail.getShopID());
                openMap.put("name",outSystemWeixinAppDetail.getName());
                openMap.put("publicNo",publicNo);
                openMap.put("openid",rsMap.get("openID"));
                openMap.put("weixinUnionID",rsMap.get("unionid"));
                openMap.put("subscribeTime",TimeTest.getTimeStr());
                weixinAppMemberService.insertByMap(openMap);

            }

        }
        //清缓存后更新deviceID
        else if (weixinAppMemberDetail != null&&StringUtils.isNotBlank(deviceID)&&!deviceID.equals(weixinAppMemberDetail.getDeviceID())){
            Map<String, Object> updateDeviceMap = Maps.newHashMap();
            updateDeviceMap.put("id",weixinAppMemberDetail.getWeixinAppMemberID());
            updateDeviceMap.put("deviceID",deviceID);
            weixinAppMemberService.updateByMap(updateDeviceMap);


            Map<String, Object> memberDeviceMap = Maps.newHashMap();
            String memberDeviceid = IdGenerator.uuid32();
            memberDeviceMap.put("id",memberDeviceid);
            memberDeviceMap.put("applicationID",weixinAppMemberDetail.getApplicationID());
            memberDeviceMap.put("memberID",weixinAppMemberDetail.getMemberID());
            memberDeviceMap.put("name",weixinAppMemberDetail.getName());
            memberDeviceMap.put("orderSeq",1);
            memberDeviceMap.put("openid",weixinAppMemberDetail.getOpenid());
            memberDeviceMap.put("weixinUnionID",weixinAppMemberDetail.getWeixinUnionID());
            memberDeviceMap.put("deviceID",deviceID);
            memberDeviceMap.put("lastUseTime",TimeTest.getTimeStr());
            memberDeviceService.insertByMap(memberDeviceMap);
        }
        //getWeiXinAppOpenId 获取openID 的接口，增加 memberDevice 记录的创建，在这个接口里面
        // ，通过publicNo 参数 ，先去查 weixinAPP ，如果查到则获取 weixinAPPID，创建weixinappMember记录的时候会用到 ，
        // 再去查 weixin 这个表，如果查到则 获取 weixinID，用于创建 weixinMember 表的记录。 如果2个表都有，则 2个记录都创建。
        openMap.clear();
        openMap.put("openid",rsMap.get("openID"));
        openMap.put("weixinUnionID",rsMap.get("unionid"));
        ApiWeixinMemberDetail weixinMemberDetail = weixinMemberService.getByMapopen(openMap);
        if (weixinMemberDetail==null){
            ModelMap pnoMap = new ModelMap();
            pnoMap.put("PUBLIC_NO",pno);
            WeixinDetail weixinDetail = weixinService.getDetilByMap(pnoMap);
            if (weixinDetail!=null){
                openMap.clear();
                String weixinMemberid = IdGenerator.uuid32();
                openMap.put("id",weixinMemberid);
                openMap.put("applicationID",weixinDetail.getApplicationID());
                openMap.put("siteID",weixinDetail.getSiteID());
                openMap.put("weixinID",weixinDetail.getWeixinID());
                openMap.put("companyID",weixinDetail.getCompanyID());
                openMap.put("name",weixinDetail.getName());
                openMap.put("PUBLIC_NO",publicNo);
                openMap.put("openid",rsMap.get("openID"));
                openMap.put("weixinUnionID",rsMap.get("unionid"));
                weixinMemberService.insertByMap(openMap);
//
            }
        }

        sendMessageService.sendSystemLog("getWeiXinAppOpenId", "openID=" + openID + " unionID=" + unionID);

        return MessagePacket.newSuccess(rsMap, "getWeiXinAppOpenId success");
    }

    /**
     * 小程序获取钉钉openoid
     *
     * @param request
     * @return
     */
    @RequestMapping("getDingDingAppOpenId")
    @ResponseBody
    public MessagePacket getDingDingAppOpenId(HttpServletRequest request) throws IOException, JSONException, ApiException {
        String appkey = request.getParameter("keys");
        String appsecret = request.getParameter("appsecret");
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(appkey) || StringUtils.isBlank(appsecret) || StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "appkey,appsecret,mobile不能为空");
        }
        DingDingApp dingDingApp = memberService.getDingDingAppByAppkey(appkey);
        if (dingDingApp == null) {
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "钉钉应用记录不存在");
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        // 获取企业的access_token
        String requestUrl = String.format("https://oapi.dingtalk.com/gettoken?appkey=%s&appsecret=%s",
                appkey, appsecret);
        String info = HttpUtil.doGet(requestUrl);
        if (StringUtils.isNotBlank(info)) {
            JSONObject jsonObject = new JSONObject(info);
            String access_token = jsonObject.get("access_token").toString();
            // 通过手机号获取openID
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/getbymobile");
            OapiV2UserGetbymobileRequest req = new OapiV2UserGetbymobileRequest();
            req.setMobile(phone);
            OapiV2UserGetbymobileResponse rsp = client.execute(req, access_token);
            rsMap.put("openID", rsp.getResult());
        } else {
            log.info("log" + "error:" + info);
            return MessagePacket.newFail(MessageHeader.Code.fail, "获取openid异常，请联系管理员");
        }
        return MessagePacket.newSuccess(rsMap, "getDingDingAppOpenId success");
    }

    @ApiOperation(value = "kangNingMemberPhoneLogin", notes = "康宁会员手机号登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "deviceID", value = "设备id", dataType = "String")
    })
    @RequestMapping(value = "/kangNingMemberPhoneLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket kangNingMemberPhoneLogin(HttpServletRequest request) {
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "设备不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "设备不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "站点不能为空");
        }
        Site site = siteService.selectSiteById(siteID);
        if (site == null) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "站点不正确");
        }
        String verifyCode = request.getParameter("verifyCode");
        if (StringUtils.isBlank(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.codeNotNull, "验证码不能为空");
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号不能为空");
        }
        String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FAST_LOGIN_AUTH_CODE, phone));
        if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
        }
        Map<String, Object> empMap = new HashMap<>();
        empMap.put("phone", phone);
        empMap.put("applicationID", site.getApplicationID());
        Employee employee = employeeService.getEmpByPhone(empMap);
        Map<String, Object> rsMap = Maps.newHashMap();
        boolean isNew = false;
        Member member = null;
        if (employee == null) {
            isNew = true;
            member = new Member();
            member.setId(IdGenerator.uuid32());
            member.setCreatedTime(new Timestamp(System.currentTimeMillis()));
            member.setName(phone);
            member.setShortName(phone);
            member.setPhone(phone);
            member.setSiteID(siteID);
            member.setLoginName(phone);
            String encodePassword = MD5.encodeMd5(String.format("%s%s", "111111", Config.ENCODE_KEY));
            member.setLoginPassword(encodePassword);
            member.setApplicationID(site.getApplicationID());
            member.setCompanyID(site.getCompanyID());
            String reCode = memberService.getCurRecommandCode(site.getApplicationID());
            if (StringUtils.isNotEmpty(reCode)) {
                member.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
                member.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
            } else {
                member.setRecommandCode("00001");
                member.setRecommendCode("00001");
            }
            member.setIsLock(0);
            member.setLevelNumber(1);
            memberService.insertMember(member);
            rsMap.put("employeeID", "");
            rsMap.put("empCompanyID", "");
        } else {
            if (StringUtils.isNotBlank(employee.getMemberID())) {
                member = memberService.selectById(employee.getMemberID());
            }
            if (member == null) {
                isNew = true;
                member = new Member();
                member.setId(IdGenerator.uuid32());
                member.setCreatedTime(new Timestamp(System.currentTimeMillis()));
                member.setName(employee.getName());
                member.setShortName(employee.getName());
                member.setPhone(phone);
                member.setSiteID(siteID);
                member.setLoginName(phone);
                String encodePassword = MD5.encodeMd5(String.format("%s%s", "111111", Config.ENCODE_KEY));
                member.setLoginPassword(encodePassword);
                member.setApplicationID(site.getApplicationID());
                member.setCompanyID(site.getCompanyID());
                String reCode = memberService.getCurRecommandCode(site.getApplicationID());
                if (StringUtils.isNotEmpty(reCode)) {
                    member.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
                    member.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
                } else {
                    member.setRecommandCode("00001");
                    member.setRecommendCode("00001");
                }
                member.setIsLock(0);
                member.setLevelNumber(1);
                memberService.insertMember(member);
                employee.setMemberID(member.getId());
                employeeService.update(employee);
            }
            rsMap.put("employeeID", employee.getId());
            rsMap.put("empCompanyID", employee.getCompanyID());
            // 注册memberMajor
            Map<String, Object> maps = Maps.newHashMap();
            maps.put("applicationID", "8a2f462a6e7dd0c0016e811475c30748");
            maps.put("majorID", "0000000077cd71950177ce4f48b400aa");
            maps.put("memberID", member.getId());
            maps.put("name", member.getName());
            maps.put("id", IdGenerator.uuid32());
            maps.put("employeeID", employee.getId());
            maps.put("companyID", employee.getCompanyID());
            maps.put("tel", phone);
            maps.put("applytime", TimeTest.getTime());
            memberMajorService.insert(maps);
        }
        HttpSession session = request.getSession();
        String ipaddr = WebUtil.getRemortIP(request);
        MemberLogDTO memberLogDTO = new MemberLogDTO(site.getId(), member.getApplicationID(),
                deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
        //清除手机号登录验证码
        stringRedisTemplate.delete(String.format("%s_%s", CacheContant.FAST_LOGIN_AUTH_CODE, phone));
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, 30, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, 30, TimeUnit.DAYS);
        rsMap.put("sessionID", session.getId());
        rsMap.put("memberID", member.getId());
        rsMap.put("isNew", isNew);
        //新增memberLogin
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", member.getId());
        memberLogin.setMemberID(member.getId());
        map1.put("memberName", member.getName());
        memberLogin.setMemberName(member.getName());
        map1.put("memberShortName", member.getShortName());
        memberLogin.setMemberShortName(member.getShortName());
        map1.put("memberAvatar", member.getAvatarURL());
        memberLogin.setMemberAvatar(member.getAvatarURL());
        map1.put("loginName", member.getLoginName());
        memberLogin.setLoginName(member.getLoginName());

        map1.put("phone", member.getPhone());
        memberLogin.setPhone(member.getPhone());
        map1.put("weixinToken", member.getWeixinToken());
        memberLogin.setWeixinToken(member.getWeixinToken());
        map1.put("weixinUnionID", member.getWeixinUnionID());
        memberLogin.setWeixinUnionID(member.getWeixinUnionID());
        map1.put("recommendCode", member.getRecommendCode());
        memberLogin.setRecommendCode(member.getRecommendCode());
        map1.put("recommendID", member.getRecommandID());
        memberLogin.setRecommendID(member.getRecommandID());
        map1.put("companyID", member.getCompanyID());
        memberLogin.setCompanyID(member.getCompanyID());
        map1.put("shopID", member.getShopID());
        memberLogin.setShopID(member.getShopID());
        map1.put("rankID", member.getRankID());
        memberLogin.setRankID(member.getRankID());
        map1.put("peopleID", member.getPeopleID());
        memberLogin.setPeopleID(member.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());

        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        return MessagePacket.newSuccess(rsMap, "kangNingMemberPhoneLogin success");
    }

    @ApiOperation(value = "updateMyMemberIndustryCategoryID", notes = "设置我的会员行业分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberIndustryCategoryID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberIndustryCategoryID(HttpServletRequest request, ModelMap map) {
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
        String industryCategoryID = request.getParameter("industryCategoryID");
        if (StringUtils.isEmpty(industryCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "industryCategoryID不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        param.put("industryCategoryID", industryCategoryID);
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员行业分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberIndustryCategoryID);
        }
        Timestamp time = TimeTest.getTime();
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberIndustryCategoryID success");
    }

    @ApiOperation(value = "updateMyMemberJobCategoryID", notes = "设置会员我的职业分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberJobCategoryID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberJobCategoryID(HttpServletRequest request, ModelMap map) {
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
        String JobCategoryID = request.getParameter("JobCategoryID");
        if (StringUtils.isEmpty(JobCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "JobCategoryID不能为空");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("id", member.getId());
        param.put("jobCategoryID", JobCategoryID);
        memberService.updateMember(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员我的职业分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberJobCategoryID);
        }
        Timestamp time = TimeTest.getTime();
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", time);
        rsMap.put("updateTimeStr", TimeTest.toDateTimeFormat(time));
        return MessagePacket.newSuccess(rsMap, "updateMyMemberJobCategoryID success");
    }

    @ApiOperation(value = "updateMemberProfessionalCategory", notes = "独立接口：设置会员的学术专业分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberProfessionalCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberProfessionalCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (StringUtils.isEmpty(professionalCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "professionalCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(professionalCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "professionalCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("professionalCategoryID", professionalCategoryID);
        memberService.updateMemberProfessionalCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的学术专业分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberProfessionalCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberProfessionalCategory success");
    }

    @ApiOperation(value = "updateMemberEducationCategory", notes = "独立接口：设置会员的教育程度分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberEducationCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberEducationCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String educationCategoryID = request.getParameter("educationCategoryID");
        if (StringUtils.isEmpty(educationCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "educationCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(educationCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "educationCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("educationCategoryID", educationCategoryID);
        memberService.updateMemberEducationCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的教育程度分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberEducationCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberEducationCategory success");
    }

    @ApiOperation(value = "updateMyMemberPhone", notes = "独立接口：直接修改会员的手机号")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberPhone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberPhone(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("memberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isEmpty(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空！");
        }
        String modifierId = null;
        if (member != null) {
            modifierId = member.getId();
        }
        map.put("modifier", modifierId);
        map.put("phone", phone);
        // 判断当前会员的登录名称是否为空，如果为空则保存手机号到登录名中
        if (StringUtils.isBlank(member.getLoginName())) {
            map.put("loginName", phone);
        }
        memberService.updateMyMemberPhone(map);
        if (memberLogDTO != null) {
            String description = String.format("直接修改会员的手机号");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberPhone);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberPhone success");
    }

    @ApiOperation(value = "updateMyMemberEmail", notes = "独立接口：设置会员我的电子邮件")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberEmail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberEmail(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("memberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String email = request.getParameter("email");
        if (StringUtils.isEmpty(email)) {
            return MessagePacket.newFail(MessageHeader.Code.emailNotNull, "email不能为空！");
        }
        String modifierId = null;
        if (member != null) {
            modifierId = member.getId();
        }
        map.put("modifier", modifierId);
        map.put("email", email);
        memberService.updateMyMemberEmail(map);
        if (memberLogDTO != null) {
            String description = String.format("设置会员我的电子邮件");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberEmail);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberEmail success");
    }

    @ApiOperation(value = "paiMaiTuikuai", notes = "拍卖后台强制退款的验证码匹配")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "verifyCode", value = "验证码", dataType = "String")
    })
    @RequestMapping(value = "/paiMaiTuikuai", produces = {"application/json;charset=UTF-8"})
    public MessagePacket paiMaiTuikuai(HttpServletRequest request, ModelMap map) {
        String verifyCode = request.getParameter("verifyCode");
        if (StringUtils.isBlank(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "验证码不能为空");
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "手机号不能为空");
        }
        String sendType = request.getParameter("sendType");
        if (StringUtils.isBlank(sendType)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "sendType不能为空");
        }
        String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FIND_MEMBER_BUY_PASS, phone));
        switch (sendType) {
            case "0":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.REGISTER_AUTH_CODE, phone));
                break;
            case "1":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.BING_PHONE_AUTH_CODE, phone));
                break;
            case "2":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FIND_MEMBER_PASS, phone));
                break;
            case "3":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FIND_MEMBER_BUY_PASS, phone));
                break;
            case "4":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.MEMBER_WITHDRAW_AUTH_CODE, phone));
                break;
            case "5":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.MEMBER_ADD_BANK_AUTH_CODE, phone));
                break;
            case "6":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.FAST_LOGIN_AUTH_CODE, phone));
                break;
            case "7":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.UPDATE_BING_PHONE_AUTH_CODE, phone));
                break;
            case "10":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.MONEY_ENCRYPTION_AUTH_CODE, phone));
                break;
            case "11":
                oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.RESETUSERWORD_AUTH_CODE, phone));
                break;
            default:
                break;
        }
        if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "验证码不正确");
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("verifyTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "paiMaiTuikuai success");
    }

    @ApiOperation(value = "updateMemberSkinCategory", notes = "独立接口：设置会员的皮肤分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberSkinCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberSkinCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String skinCategoryID = request.getParameter("skinCategoryID");
        if (StringUtils.isEmpty(skinCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "skinCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(skinCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "skinCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("skinCategoryID", skinCategoryID);
        memberService.updateMemberSkinCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的皮肤分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberSkinCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberSkinCategory success");
    }

    @ApiOperation(value = "updateMemberFaceCategory", notes = "独立接口：设置会员的脸感量分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberFaceCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberFaceCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String faceCategoryID = request.getParameter("faceCategoryID");
        if (StringUtils.isEmpty(faceCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "faceCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(faceCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "faceCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("faceCategoryID", faceCategoryID);
        memberService.updateMemberFaceCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的脸感量分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberFaceCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberFaceCategory success");
    }

    @ApiOperation(value = "updateMemberFiveCategory", notes = "独立接口：设置会员的五官分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberFiveCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberFiveCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String fiveCategoryID = request.getParameter("fiveCategoryID");
        if (StringUtils.isEmpty(fiveCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "fiveCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(fiveCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "fiveCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("fiveCategoryID", fiveCategoryID);
        memberService.updateMemberFiveCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的五官分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberFiveCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberFiveCategory success");
    }

    @ApiOperation(value = "updateMemberLineCategory", notes = "独立接口：设置会员的线性分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberLineCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberLineCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String lineCategoryID = request.getParameter("lineCategoryID");
        if (StringUtils.isEmpty(lineCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "lineCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(lineCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "lineCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("lineCategoryID", lineCategoryID);
        memberService.updateMemberLineCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的线性分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberLineCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberLineCategory success");
    }

    @ApiOperation(value = "updateMemberBoneCategory", notes = "独立接口：设置会员的骨骼感分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberBoneCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberBoneCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String boneCategoryID = request.getParameter("boneCategoryID");
        if (StringUtils.isEmpty(boneCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "boneCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(boneCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "boneCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("boneCategoryID", boneCategoryID);
        memberService.updateMemberBoneCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的骨骼感分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberBoneCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberBoneCategory success");
    }

    @ApiOperation(value = "updateMemberBodyCategory", notes = "独立接口：设置会员的体型分类")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberBodyCategory", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberBodyCategory(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String bodyCategoryID = request.getParameter("bodyCategoryID");
        if (StringUtils.isEmpty(bodyCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotNull, "bodyCategoryID不能为空");
        }
        if (!categoryService.checkCategoryId(bodyCategoryID)) {
            return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "bodyCategoryID不正确");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("memberID", member.getId());
        param.put("modifier", member.getId());
        param.put("bodyCategoryID", bodyCategoryID);
        memberService.updateMemberBodyCategory(param);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的体型分类");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberBodyCategory);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberBodyCategory success");
    }

    @ApiOperation(value = "getOneMemberBodyDetail", notes = "独立接口：获取会员的身体数据")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/getOneMemberBodyDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOneMemberBodyDetail(HttpServletRequest request) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");//会员id
        if (StringUtils.isNotBlank(memberID) && !memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        if (StringUtils.isEmpty(memberID)) {
            memberID = member.getId();
        }
        MemberBodyDto data = memberService.getOneMemberBodyDetail(memberID);
        Map<String, Object> rsMap = Maps.newHashMap();
        if (data == null) {
            data = new MemberBodyDto();
            rsMap.put("data", data);
        } else {
            rsMap.put("data", data);
        }
        String description = String.format("%s获取会员的身体数据", member.getName());
        if (memberLogDTO != null) {
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.getOneMemberBodyDetail);
        }
        return MessagePacket.newSuccess(rsMap, "getOneMemberBodyDetail success");
    }

    @ApiOperation(value = "updateMemberBodyMessage", notes = "独立接口：设置会员的详细身体数据")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMemberBodyMessage", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMemberBodyMessage(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//        Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");//会员id
        if (StringUtils.isNotBlank(memberID) && !memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        if (StringUtils.isEmpty(memberID)) {
            memberID = member.getId();
        }
        map.put("id", memberID);
        String highth = request.getParameter("highth");
        if (StringUtils.isNotBlank(highth) && !NumberUtils.isNumeric(highth)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "highth请输入数字，单位为M");
        }
        map.put("highth", highth);
        String weight = request.getParameter("weight");
        if (StringUtils.isNotBlank(weight) && !NumberUtils.isNumeric(weight)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "weight请输入数字，单位为KG");
        }
        map.put("weight", weight);
        String chest = request.getParameter("chest");
        if (StringUtils.isNotBlank(chest) && !NumberUtils.isNumeric(chest)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "chest请输入数字，单位为CM");
        }
        map.put("chest", chest);
        String shoulder = request.getParameter("shoulder");
        if (StringUtils.isNotBlank(shoulder) && !NumberUtils.isNumeric(shoulder)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "shoulder请输入数字，单位为CM");
        }
        map.put("shoulder", shoulder);
        String waist = request.getParameter("waist");
        if (StringUtils.isNotBlank(waist) && !NumberUtils.isNumeric(waist)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "waist请输入数字，单位为CM");
        }
        map.put("waist", waist);
        String hips = request.getParameter("hips");
        if (StringUtils.isNotBlank(hips) && !NumberUtils.isNumeric(hips)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "hips请输入数字，单位为CM");
        }
        map.put("hips", hips);
        String legs = request.getParameter("legs");
        if (StringUtils.isNotBlank(legs) && !NumberUtils.isNumeric(legs)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "legs请输入数字，单位为CM");
        }
        map.put("legs", legs);

        String clothingSize = request.getParameter("clothingSize");
        if (StringUtils.isNotBlank(clothingSize) && !NumberUtils.isNumeric(clothingSize)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "clothingSize请输入数字，单位为CM");
        }
        map.put("clothingSize", clothingSize);

        String modifierId = null;
        if (member != null) {
            modifierId = member.getId();
        }
        map.put("modifier", modifierId);
        memberService.updateMemberBodyMessage(map);
        if (memberLogDTO != null) {
            String description = String.format("设置会员的详细身体数据");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMemberBodyMessage);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMemberBodyMessage success");
    }

    /**
     * 宝山政府随申码
     * LZT
     *
     * @param request
     * @return
     */
//    @RequestMapping("getSuiShenMaCode")
//    @ResponseBody
//    public MessagePacket getSuiShenMaCode(HttpServletRequest request) throws Exception {
//        String result = "";
//        String isShanghai = "0";
//        String url = "https://ywopen.sh.gov.cn/clientgateway/?from=bsjgj42f38389f1c99868f286";
//        StringBuffer stringBuffer = request.getRequestURL();
////        if (stringBuffer.toString().contains("haoju")){
////            url = "https://183.194.243.116/clientgateway/?from=bsjgj42f38389f1c99868f286";
////        }else {
////
////        }
//        String data = request.getParameter("data");
//        if (StringUtils.isNotBlank(data)) {
//            url += "&" + "data=" + data;
//        }
//
//        People people = null;
//        String kingID = request.getParameter("kingID");
//        if (StringUtils.isNotBlank(kingID)) {
//            people = peopleService.getByKingID(kingID);
//        }
//
//        String code = "";
//        String message = "";
//        String data1 = "";
//        HttpPost httpPost = new HttpPost(url);
//        httpPost.addHeader("appid", "f2ff3ae5-2a79-49c3-ba37-e739190b4d5a");
//        httpPost.addHeader("apiname", "2fae9f66-2698-430b-9afa-55993eff3506");
//        httpPost.addHeader("signature", "Xx6fjB+bNjHA7CgT2mB9axYkfdcfzhDesvKm3iq0Wt39wVlxUHrJRztn+CZfxVsyjOBamkXNCGMk3jXTONPEFF5Yt3jEsIEnqxXP/h53Ox+3IZe/BIk9XBbWiykTg8OO");
//        String secret = "7ae9cb96c91f441ccc611ee6d0819aa5";
//        String iv = "b643e5c5427e4d6b";
//        Map<String, String> rsMap = new HashMap<>();
//        try {
//            CloseableHttpClient httpClient = HttpClients.createDefault();
//            CloseableHttpResponse response = httpClient.execute(httpPost);
//            HttpEntity responseEntity = response.getEntity();
//            if (responseEntity != null) {
//                result = EntityUtils.toString(responseEntity);
//            }
//        } catch (IOException e) {
//            log.error("log",e);
//        }
//        log.info("log" + "clientgateway接口返回值=====" + result);
//        String isHaoJu = request.getParameter("isHaoJu");
//        if (StringUtils.isBlank(isHaoJu)) {
//            isHaoJu = "0";
//        }
//        if (!"1".equals(isHaoJu)) {
//            Map<String, Object> logMap = new HashMap<>();
//            logMap.put("id", IdGenerator.uuid32());
//            logMap.put("operateTime", TimeTest.getTime());
//            logMap.put("name", "宝山政府随申码接口参数不通过调用失败");
//            logMap.put("description", "宝山政府随申码接口参数不通过调用失败,失败时间为：" + TimeTest.getTimeStr());
//            logMap.put("message", "宝山政府随申码接口参数不通过调用失败,失败时间为：" + TimeTest.getTimeStr());
//            memberService.insertSystemLogByMap(logMap);
//            return MessagePacket.newFail(MessageHeader.Code.fail, "只能内部调用");
//        }
//        // 从响应模型中获取响应实体
//        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(result);
//        if (StringUtils.isNotBlank(jsonObject.getString("code"))) {
//            code = jsonObject.getString("code");
//        }
//        if ("0".equals(code)) {
//            data1 = jsonObject.getString("data");
//            String data2 = AESUtil.decode(secret, iv, data1);
//            String xm = "";
//            String type = "";
//            String zjhm = "";
//            String phone = "";
//            String dzzz = "";
//            String uuid = "";
//            net.sf.json.JSONObject jsonObject2 = net.sf.json.JSONObject.fromObject(data2);
//            if (StringUtils.isNotBlank(jsonObject2.getString("xm"))) {
//                xm = jsonObject2.getString("xm");
//            }
//            if (StringUtils.isNotBlank(jsonObject2.getString("type"))) {
//                type = jsonObject2.getString("type");
//            }
//            if (StringUtils.isNotBlank(jsonObject2.getString("zjhm"))) {
//                zjhm = jsonObject2.getString("zjhm");
//            }
//            if (StringUtils.isNotBlank(jsonObject2.getString("phone"))) {
//                phone = jsonObject2.getString("phone");
//            }
//            if (StringUtils.isNotBlank(jsonObject2.getString("uuid"))) {
//                uuid = jsonObject2.getString("uuid");
//            }
//            //创建一个刷随申码进出纪录
//            Map<String, Object> map = new HashMap<>();
//            String id = IdGenerator.uuid32();
//            map.put("id", id);
//            map.put("applicationID", "8a2f462a7572dfaf0175740cf8590ba0");
//            map.put("companyID", "8a2f462a7598462c01759d2ec6640b6d");
//            map.put("autogateID", "8a2f462a781f19ff0178209b66c002e7");
//            map.put("name", xm + TimeTest.getTimeStr() + "持健康码通过通道");
//            map.put("description", xm + TimeTest.getTimeStr() + "持健康码通过通道");
//            map.put("moveType", 1);
////            vehicleMoveService.insertOneRecord(map);
//            // 调用公安接口
//            String timestamp = String.valueOf(System.currentTimeMillis());
//            String testResult = "";
//            String testUrl = "http://10.246.5.90:8080/prod-api/ssmuuid/1";
//            String appKey = "5d23bdb3a84d4af2820d35d2858723fc";
//            if (StringUtils.isNotBlank(appKey)) {
//                testUrl += "?" + "appKey=" + appKey;
//            }
//            if (StringUtils.isNotBlank(timestamp)) {
//                testUrl += "&" + "timestamp=" + timestamp;
//            }
//            String serviceCode = "aTs8HWN5Y8WgCPWF";
//            if (StringUtils.isNotBlank(serviceCode)) {
//                testUrl += "&" + "serviceCode=" + serviceCode;
//            }
//            String signature = DigestUtils.sha1Hex(appKey + "IIIYOKLO" + timestamp);
//            if (StringUtils.isNotBlank(signature)) {
//                testUrl += "&" + "signature=" + signature;
//            }
//            if (StringUtils.isNotBlank(uuid)) {
//                testUrl += "&" + "uuid=" + uuid;
//            }
//
//            String zhenshi = "";
//            log.info("log" + "Url=====" + testUrl);
//            HttpPost testPost = new HttpPost(testUrl);
//            testPost.addHeader("Content-Type", "application/json;charset=UTF-8");
//            testPost.addHeader("DSC_APP_CODE", "vbr4R6SDQT6NriGDJR0kq9ysCdYeom56");
//            try {
//                CloseableHttpClient testHttpClient = HttpClients.createDefault();
//                CloseableHttpResponse testResponse = testHttpClient.execute(testPost);
//                HttpEntity testResponseEntity = testResponse.getEntity();
//                if (testResponseEntity != null) {
//                    testResult = EntityUtils.toString(testResponseEntity);
//                }
//            } catch (IOException e) {
//                log.error("log",e);
//            }
//            // 从响应模型中获取响应实体
//            log.info("log" + "testResult=========" + testResult);
//
//            if (testResult.length() > 100) {
//                com.alibaba.fastjson.JSONObject uuJsonObject = JSON.parseObject(testResult);
//                if (StringUtils.isNotBlank(uuJsonObject.getString("data"))) {
//                    zhenshi = uuJsonObject.getString("data");
//                }
//                SM4Util sm4Util = new SM4Util();
//                int index1 = zhenshi.indexOf("\\\"zjhm\\\":\\\"");
//                int result1 = zhenshi.indexOf("\\\",\\\"sjh\\\"");
//                if (index1 > 0) {
//                    zjhm = sm4Util.decrypt(zhenshi.substring(index1 + 8, result1), "0a1abe71e34145a1");
//                    log.info("log" + "身份证号===========：" + zjhm);
//                    isShanghai = "1";
//                }
//            }
//            if (people == null) {
//                map.clear();
//                map.put("id", IdGenerator.uuid32());
//                map.put("applicationID", "8a2f462a7572dfaf0175740cf8590ba0");
//                map.put("companyID", "8a2f462a7598462c01759d2ec6640b6d");
//                map.put("name", xm);
//                map.put("kingID", kingID);
//                map.put("idNumber", zjhm);
//                peopleService.insertByMap(map);
//            }
//            rsMap.put("xm", xm);
//            rsMap.put("type", type);
//            rsMap.put("zjhm", zjhm);
//            rsMap.put("phone", phone);
//            rsMap.put("isShanghai", isShanghai);
//        } else {
//            message = jsonObject.getString("message");
//            return MessagePacket.newFail(MessageHeader.Code.fail, message);
//        }
//        return MessagePacket.newSuccess(rsMap, "getSuiShenMaCode success");
//    }

    @ApiOperation(value = "getMemberLogSimpleList", notes = "独立接口：获取会员日志列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/getMemberLogSimpleList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberLogSimpleList(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//      member=memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String objectDefineID = request.getParameter("objectDefineID");
        if (StringUtils.isNotBlank(objectDefineID)) {
            if (!objectDefineService.checkIsValidId(objectDefineID)) {
                return MessagePacket.newFail(MessageHeader.Code.objectDefineIDNotFound, "objectDefineID不正确");
            }
            map.put("objectDefineID", objectDefineID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.objectDefineIDNotNull, "objectDefineID不能为空");
        }
        String objectID = request.getParameter("objectID");
        if (StringUtils.isNotBlank(objectID)) {
            map.put("objectID", objectID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.objectIDNotNull, "objectID不能为空");
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) {
            map.put("beginTime", beginTime);
        }
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) {
            map.put("endTime", endTime);
        }
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) {
            if (!NumberUtils.isNumeric(sortTypeTime)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入1到2的数字");
            }
            if (!sortTypeTime.equals("1") && !sortTypeTime.equals("2")) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入1到2的数字");
            }
            map.put("sortTypeTime", sortTypeTime);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入1到2的数字");
            }
            if (!sortTypeName.equals("1") && !sortTypeName.equals("2")) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入1到2的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页11
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<MemberLogList> list = memberService.getListByObjID(map);
        if (!list.isEmpty()) {
            PageInfo<BadgeDefineDto> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (memberLogDTO != null) {
            String description1 = "获取会员日志列表";
            sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.getMemberLogSimpleList);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "getMemberLogSimpleList success");
    }

    @ApiOperation(value = "checkCompanyOrMemberID", notes = "独立接口：判断id是公司还是会员")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/checkCompanyOrMemberID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket checkCompanyOrMemberID(HttpServletRequest request, ModelMap map, Map<String, Object> rsMap) {
        String id = request.getParameter("id");
        if (StringUtils.isEmpty(id)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "id不能为空");
        }
        if (memberService.checkMemberId(id)) {
            rsMap.put("type", "member");
        } else if (companyService.checkIdIsValid(id)) {
            rsMap.put("type", "company");
        } else {
            rsMap.put("type", "none");
        }
        return MessagePacket.newSuccess(rsMap, "checkCompanyOrMemberID success");
    }

    @ApiOperation(value = "getMemberIncomeAmount", notes = "独立接口：获取会员分销金额总额")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/getMemberIncomeAmount", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberIncomeAmount(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("applicationID", member.getApplicationID());
        map.put("memberID", member.getId());
        memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Integer amount = memberService.getAmount(map);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("amount", amount);
        return MessagePacket.newSuccess(rsMap, "checkCompanyOrMemberID success");
    }

    @ApiOperation(value = "checkISVisitor", notes = "独立接口：查询ios游客会员是否存在")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/checkISVisitor", produces = {"application/json;charset=UTF-8"})
    public MessagePacket checkISVisitor(HttpServletRequest request, ModelMap map) {
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isNotBlank(deviceID)) {
            if (!deviceService.checkIdIsValid(deviceID)) {
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
            }
        } else {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        String sessionID = "游客不存在";
        String ipaddr = WebUtil.getRemortIP(request);
        String memberID = memberService.checkIsVisitor(deviceID);
        if (StringUtils.isNotBlank(memberID)) {
            Member member = memberService.selectById(memberID);
            // 游客登录返回sessionID
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            String description = String.format("游客:%s 登录", member.getName());
            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, 86400, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, 86400, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, session.getId(), "", ipaddr, description, Memberlog.MemberOperateType.memberLogin);
            sessionID = session.getId();
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            memberService.updateMember(map);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        rsMap.put("memberID", memberID);
        return MessagePacket.newSuccess(rsMap, "checkISVisitor success");
    }

    @ApiOperation(value = "createOneVisitor", notes = "独立接口：注册一个ios游客")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/createOneVisitor", produces = {"application/json;charset=UTF-8"})
    public MessagePacket createOneVisitor(HttpServletRequest request, ModelMap map) {
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isNotBlank(deviceID)) {
            if (!deviceService.checkIdIsValid(deviceID)) {
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
            }
            map.put("openID", deviceID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            map.put("applicationID", applicationID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID不能为空");
        }
        Map<String, Object> requestMap = Maps.newHashMap();
        Map<String, Object> adressIp = kingBase.getAdressIp(request, applicationID, null, null, requestMap);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (!companyService.checkIdIsValid(companyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "companyID不正确");
            }
            map.put("companyID", companyID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不能为空");
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isBlank(bizBlock)) {
            bizBlock = "0";
        }
        String memberID = IdGenerator.uuid32();
        String randomNum = sequenceDefineService.genCode("visitorNum", memberID);
        SiteDto site = siteService.getSiteDetail(siteID);
        String code = memberService.getCurRecommandCode(site.getApplicationID());
        if (StringUtils.isNotEmpty(code)) {
            map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
        } else {
            map.put("recommendCode", "00001");
        }
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        map.put("name", "游客" + randomNum);
        map.put("id", memberID);
        map.put("shortName", "游客" + randomNum);
        map.put("createType", 5);
        map.put("bizBlock", bizBlock);
        memberService.insertByMap(map);
        // 登录
        deviceService.updateLastLoadTime(deviceID);
        Member member = memberService.selectById(memberID);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);
        String ipaddr = WebUtil.getRemortIP(request);
        String sessionID = session.getId();
        MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, 86400, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, 86400, TimeUnit.SECONDS);
        String description = member.getName() + "使用设备(" + device.getName() + ")注册一个ios游客登录成功";
        sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description,
                Memberlog.MemberOperateType.addSuccess);
        description = member.getName() + "使用设备(" + device.getName() + ")注册一个ios游客登录成功";
        sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                Memberlog.MemberOperateType.createOneVisitor);
        memberService.createMemberPrivilege(applicationID, member.getId());
        map.clear();
        map.put("id", member.getId());
        map.put("lastLoginTime", TimeTest.getTime());
        memberService.updateMember(map);
        // 检查memberDevice
        memberService.checkMemberDevice(memberID, deviceID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", memberID);
        rsMap.put("sessionID", sessionID);
        //新增memberLogin
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", member.getId());
        memberLogin.setMemberID(member.getId());
        map1.put("memberName", member.getName());
        memberLogin.setMemberName(member.getName());
        map1.put("memberShortName", member.getShortName());
        memberLogin.setMemberShortName(member.getShortName());
        map1.put("memberAvatar", member.getAvatarURL());
        memberLogin.setMemberAvatar(member.getAvatarURL());
        map1.put("loginName", member.getLoginName());
        memberLogin.setLoginName(member.getLoginName());

        map1.put("phone", member.getPhone());
        memberLogin.setPhone(member.getPhone());
        map1.put("weixinToken", member.getWeixinToken());
        memberLogin.setWeixinToken(member.getWeixinToken());
        map1.put("weixinUnionID", member.getWeixinUnionID());
        memberLogin.setWeixinUnionID(member.getWeixinUnionID());
        map1.put("recommendCode", member.getRecommendCode());
        memberLogin.setRecommendCode(member.getRecommendCode());
        map1.put("recommendID", member.getRecommandID());
        memberLogin.setRecommendID(member.getRecommandID());
        map1.put("companyID", member.getCompanyID());
        memberLogin.setCompanyID(member.getCompanyID());
        map1.put("shopID", member.getShopID());
        memberLogin.setShopID(member.getShopID());
        map1.put("rankID", member.getRankID());
        memberLogin.setRankID(member.getRankID());
        map1.put("peopleID", member.getPeopleID());
        memberLogin.setPeopleID(member.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        return MessagePacket.newSuccess(rsMap, "createOneVisitor success");
    }

    @ApiOperation(value = "phoneFastLogin", notes = "独立接口：手机短信验证快速登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/phoneFastLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket phoneFastLogin(HttpServletRequest request, ModelMap map, ModelMap memberMap) {
        String memberName = "";
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }
        map.put("phone", phone);

        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            map.put("applicationID", applicationID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID不能为空");
        }


        if (phone != null) {
            Map<String, Object> requestMap = Maps.newHashMap();
            requestMap.put("Isphone",true);
            Map<String, Object> adressIp = kingBase.getAdressIp(request, applicationID, null,  phone, requestMap);
            if (!adressIp.get("result").equals("通过")) {
                String result = adressIp.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }
        else {
            Map<String, Object> adressIp = kingBase.getAdressIp(request, applicationID, null,  null, null);
            if (!adressIp.get("result").equals("通过")) {
                String result = adressIp.get("result").toString();
                return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
            }
        }



        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isBlank(bizBlock)) {
            bizBlock = "0";
        }

        String verifyCode = request.getParameter("verifyCode");
        if (StringUtils.isBlank(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotNull, "验证码不能为空");
        }
        String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.LOGIN_AUTH_CODE, phone));
        if (StringUtils.isNotBlank(verifyCode)) {
            if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
                return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
            }
            //当验证码错误时候，如
        }
        //从redis里删除
        stringRedisTemplate.delete(String.format("%s_%s", CacheContant.LOGIN_AUTH_CODE, phone));
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isNotBlank(deviceID)) {
            if (!deviceService.checkIdIsValid(deviceID)) {
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
            }
            map.put("openID", deviceID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }


        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            if (!companyService.checkIdIsValid(companyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyNotNull, "companyID不正确");
            }
            map.put("companyID", companyID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不能为空");
        }

        String channelID = "";
        String channelCode = request.getParameter("channelCode");
        if (StringUtils.isNotBlank(channelCode)) {
            channelID = channelService.selectByServiceCode(channelCode);
        }
        String recommendID = request.getParameter("recommendMemberID");
        if (StringUtils.isNotBlank(recommendID)) {
            if (!memberService.checkIdIsValid(recommendID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendID 不正确");
            }
        }
        Member member1 = null;
        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
            member1 = memberService.getMemberByRecommendCode(map);
        }
        // 到系统中查找是否存在会员，如果存在就登录，不存在就注册
        String sessionID = "";
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        map.clear();
        map.put("bizBlock", bizBlock);
        map.put("applicationID", applicationID);
        map.put("phone", phone);
        String memberID = memberService.selectByPhone(map);
        Member member;
        if (StringUtils.isNotBlank(memberID)) {
            member = memberService.selectById(memberID);
            memberName = member.getName();
            member.setSiteID(siteID);
            if (member.getIsLock() == 1) {
                return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "当前会员已被锁定");
            }
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);

            // session检查
            memberService.checkSession(deviceID, session.getId(), memberID, siteID, member.toString());

            String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")手机短信验证快速登录成功1";
            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.memberLogin);
            sessionID = session.getId();

            // 储存进redis
            memberService.saveMemberRedis(memberID);
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            if (member1 != null) {
                map.put("recommandID", member1.getId());
                map.put("recommendID", member1.getId());
            }

            try {
                Thread.sleep(1000);
                memberService.updateMember(map);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        } else {
            // 会员不存在，注册登录
            member = new Member();
            memberID = IdGenerator.uuid32();
            member.setId(memberID);
            member.setCreatedTime(new Timestamp(System.currentTimeMillis()));
            member.setPhone(phone);
            member.setSiteID(siteID);
            member.setChannelID(channelID);
            member.setCreateType(4);
            member.setBizBlock(Integer.parseInt(bizBlock));
            member.setLoginName(phone);
            member.setQiyeweixinID(sequenceDefineService.genCode("qiyeweixinID", memberID));
            member.setApplicationID(applicationID);
            member.setCompanyID(companyID);
            if (member1 != null) {
                member.setRecommandID(member1.getId());
                member.setRecommendID(member1.getId());
                MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(member1.getId());
                Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                memberStatistics.setRecommendMembers(recommendNum);
                memberStatisticsService.updateMemberStatistics(memberStatistics);
                member.setLevelNumber(member1.getLevelNumber() + 1);
                if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                    member.setRecommandChain(member1.getRecommandChain() + "," + member1.getRecommandCode());
                    member.setRecommendChain(member1.getRecommandChain() + "," + member1.getRecommandCode());
                } else {
                    member.setRecommandChain(member1.getRecommandCode());
                    member.setRecommendChain(member1.getRecommandCode());
                }
            } else {
                if (StringUtils.isNotBlank(recommendID)) {
                    member.setRecommandID(recommendID);
                    member.setRecommendID(recommendID);
                }

                member.setLevelNumber(1);
            }
            String reCode = memberService.getCurRecommandCode(applicationID);
            if (StringUtils.isNotEmpty(reCode)) {
                member.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
                member.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(reCode) + 1)));
            } else {
                member.setRecommandCode("00001");
                member.setRecommendCode("00001");
            }
            String finalBizBlock = bizBlock;
            List<ApiRankListDto> rankList = rankService.getRankList(
                    new HashMap<String, Object>() {{
                        put("applicationID", applicationID);
                        put("bizBlock", finalBizBlock);
                        put("sortTypeOrderSeq", 1);
                    }});
            if (!rankList.isEmpty() && rankList.size() > 0) {
                ApiRankListDto rank = rankList.get(0);
                member.setName(rank.getName() + member.getRecommandCode());
                member.setShortName(rank.getName() + member.getRecommandCode());
            } else {
                member.setName(phone);
                member.setShortName(phone);
            }
            memberName = member.getName();
            Map<String, Object> pMap = Maps.newHashMap();
            pMap.put("applicationID", applicationID);
            pMap.put("code", "memberLockNeedVerify");
            String code = parameterService.selectParameterApplication(pMap);
            int isLock = 0;
            if (StringUtils.isNotBlank(code) && code.equals("1")) isLock = 1;
            member.setIsLock(isLock);
            memberService.insertMember(member);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);

            // 多货币队列
            sendMessageService.sendMcApplicationCurrency(memberID, WebUtil.getRemortIP(request));

            // session检查
            memberService.checkSession(deviceID, session.getId(), memberID, siteID, member.toString());

            MemberLogDTO memberLogDTO = new MemberLogDTO(siteID, member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")手机短信验证快速注册成功";
            sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.addSuccess);
            description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")手机短信验证快速登录成功";
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr,
                    ipaddr, description, Memberlog.MemberOperateType.phoneFastLogin);
            memberService.createMemberPrivilege(applicationID, member.getId());
            sessionID = session.getId();
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            memberService.updateMember(map);

            // 储存进redis
            memberService.saveMemberRedis(memberID);
        }
        // 检查memberDevice
        memberService.checkMemberDevice(memberID, deviceID);


        //新增memberLogin
        Site site = siteService.selectSiteById(siteID);
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", member.getId());
        memberLogin.setMemberID(member.getId());
        map1.put("memberName", member.getName());
        memberLogin.setMemberName(member.getName());
        map1.put("memberShortName", member.getShortName());
        memberLogin.setMemberShortName(member.getShortName());
        map1.put("memberAvatar", member.getAvatarURL());
        memberLogin.setMemberAvatar(member.getAvatarURL());
        if (StringUtils.isNotBlank(phone)) {
            map1.put("loginName", phone);
            memberLogin.setLoginName(phone);
        } else {
            map1.put("loginName", member.getLoginName());
            memberLogin.setLoginName(member.getLoginName());
        }
        map1.put("phone", member.getPhone());
        memberLogin.setPhone(member.getPhone());
        map1.put("weixinToken", member.getWeixinToken());
        memberLogin.setWeixinToken(member.getWeixinToken());
        map1.put("weixinUnionID", member.getWeixinUnionID());
        memberLogin.setWeixinUnionID(member.getWeixinUnionID());
        map1.put("recommendCode", member.getRecommendCode());
        memberLogin.setRecommendCode(member.getRecommendCode());
        map1.put("recommendID", member.getRecommandID());
        memberLogin.setRecommendID(member.getRecommandID());
        map1.put("companyID", member.getCompanyID());
        memberLogin.setCompanyID(member.getCompanyID());
        map1.put("shopID", member.getShopID());
        memberLogin.setShopID(member.getShopID());
        map1.put("rankID", member.getRankID());
        memberLogin.setRankID(member.getRankID());
        map1.put("peopleID", member.getPeopleID());
        memberLogin.setPeopleID(member.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        HttpSession session = request.getSession();
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        // 极光别名
        ApiDeviceDetailDto dto = deviceService.selectByID(deviceID);
        String pushDeviceID = dto.getPushDeviceID();
        if (StringUtils.isNotBlank(pushDeviceID)) {
            map.clear();
            map.put("code", "JpushAppKey");
            map.put("siteID", siteID);
            String jpushAppKey = parameterSiteService.getValueByMap(map);
            map.put("code", "JpushMasterSecret");
            String jpushMasterSecret = parameterSiteService.getValueByMap(map);
            if (StringUtils.isNotBlank(jpushAppKey) && StringUtils.isNotBlank(jpushMasterSecret)) {
                JPushClient jPushClient = new JPushClient(jpushMasterSecret, jpushAppKey);
                try {
                    DefaultResult defaultResult = jPushClient.updateDeviceTagAlias(pushDeviceID, memberID, null, null);
                    log.info("log" + defaultResult.toString());
                    TagAliasResult result = jPushClient.getDeviceTagAlias(pushDeviceID);
                    log.info("log" + memberName + "别名：" + result.toString());
                } catch (APIConnectionException e) {
                    log.error("log",e);
                } catch (APIRequestException e) {
                    log.error("log",e);
                }
            }
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        rsMap.put("memberID", memberID);

        // 以下字段同步自 memberLogin 接口
        rsMap.put("shortName", member.getShortName());
        rsMap.put("avatarURL", member.getAvatarURL());
        rsMap.put("loginName", member.getLoginName());
        rsMap.put("phone", member.getPhone());
        rsMap.put("name", member.getName());
        rsMap.put("recommandCode", member.getRecommandCode());
        rsMap.put("recommendCode", member.getRecommendCode());
        rsMap.put("weixinUnionID", member.getWeixinUnionID());
        rsMap.put("weixinToken", member.getWeixinToken());
        rsMap.put("loginPassword", member.getLoginPassword());
        rsMap.put("rankID", member.getRankID());
        rsMap.put("lastChangePWTim", member.getLastChangePWTime());
        rsMap.put("isNeedChangePW", member.getIsNeedChangePW());
        rsMap.put("PWErrorTimes", member.getPWErrorTimes());


        if (StringUtils.isNotBlank(member.getRankID())) {
            rsMap.put("rankName", rankService.getNameById(member.getRankID()));
        } else {
            rsMap.put("rankName", null);
        }


        rsMap.put("majorID", member.getMajorID());
        if (StringUtils.isNotBlank(member.getMajorID())) {

            rsMap.put("majorName", majorService.getNameById(member.getMajorID()));
        } else {
            rsMap.put("majorName", null);
        }


        return MessagePacket.newSuccess(rsMap, "phoneFastLogin success");
    }

    @ApiOperation(value = "appleLogin", notes = "独立接口：苹果一键注册登录")
    @RequestMapping(value = "/appleLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket appleLogin(HttpServletRequest request, ModelMap map, ModelMap memberMap) {
        String appleUserID = request.getParameter("appleUserID");
        if (StringUtils.isBlank(appleUserID)) {
            return MessagePacket.newFail(MessageHeader.Code.userIDNotNull, "appleUserID不能为空");
        }

        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
        }

        SiteDto site = siteService.getSiteDetail(siteID);
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        String applicationID = site.getApplicationID();
        String companyID = site.getCompanyid();
        // 到系统中查找是否存在会员，如果存在就登录，不存在就创建
        String sessionID = "";
        String memberID = memberService.selectByAppleUserID(appleUserID);
        if (StringUtils.isNotBlank(memberID)) {
            Member member = memberService.selectById(memberID);
            member.setSiteID(siteID);
            if (member.getIsLock() == 1) {
                return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "当前会员已被锁定");
            }
            map.clear();
            map.put("memberID", memberID);
            map.put("deviceID", deviceID);
            String memberDeviceID = memberDeviceService.getIdByMap(map);
            if (StringUtils.isBlank(memberDeviceID)) {
                // 创建memberDevice记录
                String newMemberDeviceID = IdGenerator.uuid32();
                map.put("id", newMemberDeviceID);
                memberDeviceService.insertByMap(map);
            }

            // 登录
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            sessionID = session.getId();

            // session检查
            memberService.checkSession(deviceID, sessionID, memberID, siteID, member.toString());

            String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")苹果一键登录成功";
            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID), member, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, sessionID, ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.appleLogin);
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            memberService.updateMember(map);
        } else {
            // 注册会员
            map.clear();
            memberID = IdGenerator.uuid32();
            map.put("id", memberID);
            map.put("appleUserID", appleUserID);
            map.put("levelNumber", 1);
            map.put("applicationID", applicationID);
            map.put("siteID", siteID);
            map.put("companyID", companyID);
            map.put("createType", 5);
            String bizBlock = request.getParameter("bizBlock");
            if (StringUtils.isBlank(bizBlock)) {
                bizBlock = "0";
            }
            map.put("bizBlock", bizBlock);

            String name = request.getParameter("name");
            if (StringUtils.isNotBlank(name)) {
                map.put("name", name);
            }

            String phone = request.getParameter("phone");
            if (StringUtils.isNotBlank(phone)) {
                map.put("phone", phone);
                map.put("loginName", phone);
            }

            String shortName = request.getParameter("shortName");
            if (StringUtils.isNotBlank(shortName)) {
                map.put("shortName", shortName);
            }

            String avatarURL = request.getParameter("avatarURL");
            if (StringUtils.isNotBlank(avatarURL)) {
                map.put("avatarURL", avatarURL);
            }

            String recommendMemberID = request.getParameter("recommendMemberID");
            if (StringUtils.isNotBlank(recommendMemberID)) {
                if (!memberService.checkMemberId(recommendMemberID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
                }
                MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(recommendMemberID);
                Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                memberStatistics.setRecommendMembers(recommendNum);
                memberStatisticsService.updateMemberStatistics(memberStatistics);
                // 获取推荐链
                Member member1 = memberService.selectById(recommendMemberID);
                map.put("levelNumber", member1.getLevelNumber() + 1);
                if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                    map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                } else {
                    map.put("recommandChain", member1.getRecommandCode());
                }
            }
            String recommendCode = request.getParameter("recommendCode");
            if (StringUtils.isNotBlank(recommendCode)) {
                Member member1 = memberService.getMemberByRecommendCode(map);
                recommendMemberID = member1.getId();
                if (member1 != null) {
                    MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(member1.getId());
                    Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                    memberStatistics.setRecommendMembers(recommendNum);
                    memberStatisticsService.updateMemberStatistics(memberStatistics);
                    map.put("levelNumber", member1.getLevelNumber() + 1);
                    // 获取推荐链
                    if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                        map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                    } else {
                        map.put("recommandChain", member1.getRecommandCode());
                    }
                }
            }
            map.put("recommendID", recommendMemberID);

            String code = memberService.getCurRecommandCode(site.getApplicationID());
            if (StringUtils.isNotEmpty(code)) {
                map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
            } else {
                map.put("recommendCode", "00001");
            }
            memberService.insertByMap(map);

            // 创建会员设备记录
            map.clear();
            map.put("memberID", memberID);
            map.put("deviceID", deviceID);
            String newMemberDeviceIDs = IdGenerator.uuid32();
            map.put("id", newMemberDeviceIDs);
            memberDeviceService.insertByMap(map);

            // 登录
            Member member1 = memberService.selectById(memberID);
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            sessionID = session.getId();

            // session检查
            memberService.checkSession(deviceID, sessionID, memberID, siteID, member1.toString());

            MemberLogDTO memberLogDTO = new MemberLogDTO(member1.getSiteID(), member1.getApplicationID(),
                    deviceID, member1.getId(), memberService.getLocationID(ipaddr), member1.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID), member1, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);

            String description = "会员:" + member1.getName() + "使用设备(" + device.getName() + ")苹果一键注册成功";
            sendMessageService.sendMemberLoginMessage(true, sessionID, ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.addSuccess);

            description = "会员:" + member1.getName() + "使用设备(" + device.getName() + ")苹果一键登录成功";
            sendMessageService.sendMemberLoginMessage(false, sessionID, ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.appleLogin);
            memberService.createMemberPrivilege(site.getApplicationID(), member1.getId());
            map.clear();
            map.put("id", member1.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            memberService.updateMember(map);

            // 储存进redis
            memberService.saveMemberRedis(memberID);
        }
        // 检查memberDevice
        memberService.checkMemberDevice(memberID, deviceID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        rsMap.put("memberID", memberID);
        return MessagePacket.newSuccess(rsMap, "appleLogin success");
    }

    @ApiOperation(value = "sessionTimeoutLogin", notes = "独立接口：session过期登录接口")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/sessionTimeoutLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket sessionTimeoutLogin(HttpServletRequest request, ModelMap map) {
        //sessionID：不能为空，要到 memberSession 里面去查 这个 sessionID是否存在，是否过期 ，不存在的就退出，存在就获取里面 的登录时间，deviceID
        MemberSessionDetail sessionDetail =null;
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isNotBlank(sessionID)) {
             sessionDetail = memberSessionService.getBySessionID(sessionID);
            if (sessionDetail==null){
                return MessagePacket.newFail(MessageHeader.Code.unauth, "sessionID 不正确");
            }
        } else {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "sessionID 不能为空");
        }

        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isNotBlank(deviceID)) {
            //先判断 传入的 deviceID是否 和 sessionID获取的一致，不一致报错退出
            if (!deviceID.equals(sessionDetail.getDeviceID())){
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID 不一致");
            }
            //再看上次的登录时间，如果超过 10天则报错退出
            LocalDateTime now = LocalDateTime.now();
            // 将Timestamp转换为LocalDateTime
            LocalDateTime loginDateTime = sessionDetail.getLoginTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // 计算两者之间的天数差
            long daysBetween = ChronoUnit.DAYS.between(loginDateTime, now);

            // 判断是否在10天内（包括当天）
            if (!(daysBetween >= 0 && daysBetween <= 10)){
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "登录时间超时");
            }

            if (!deviceService.checkIdIsValid(deviceID)) {
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
            }
            map.put("deviceID", deviceID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        //memberID 可以为空，修改为不能为空
        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!memberService.checkMemberId(memberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
            }
            map.put("memberID", memberID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        // 到系统中查找是否存在会员，如果存在就登录，不存不能登录
        //String sessionID = "";
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        String memberDeviceID = memberDeviceService.getIdByMap(map);
        if (StringUtils.isNotBlank(memberDeviceID)) {
            deviceService.updateLastLoadTime(deviceID);
        }
        Member member = memberService.selectById(memberID);
        String ipaddr = WebUtil.getRemortIP(request);
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(5);
        String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")进行session过期登录接口登录成功";
        MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
        sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description, Memberlog.MemberOperateType.memberLogin);
        sessionID = session.getId();
//            // session检查
//            memberService.checkSession(deviceID,session.getId(),memberID,member.getSiteID(),member.toString());

//            // 储存进redis
//            memberService.saveMemberRedis(member.getId());
        map.clear();
        map.put("id", member.getId());
        map.put("lastLoginTime", TimeTest.getTime());
        //注意，登录成功要先在 memberSession创建1个新的记录
        ModelMap membserSessionMap = new ModelMap();
        String id = IdGenerator.uuid32();
        membserSessionMap.put("id", id);
        membserSessionMap.put("applicationID", sessionDetail.getApplicationID());
        membserSessionMap.put("siteID", sessionDetail.getSiteID());
        membserSessionMap.put("memberID", memberID);
        membserSessionMap.put("deviceID", deviceID);
        membserSessionMap.put("name", member.getName()+"登录");
        membserSessionMap.put("sessionID", sessionID);
        membserSessionMap.put("loginTime", TimeTest.getTime());

        memberSessionService.insertByMap(membserSessionMap);

        //新增memberLogin

        Site site = siteService.selectSiteById(member.getSiteID());
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("applicationID", site.getApplicationID());
        memberLogin.setApplicationID(site.getApplicationID());
        map1.put("applicationName", site.getApplicationName());
        memberLogin.setApplicationName(site.getApplicationName());
        map1.put("siteID", site.getId());
        memberLogin.setSiteID(site.getId());
        map1.put("siteName", site.getName());
        memberLogin.setSiteName(site.getName());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", member.getId());
        memberLogin.setMemberID(member.getId());
        map1.put("memberName", member.getName());
        memberLogin.setMemberName(member.getName());
        map1.put("memberShortName", member.getShortName());
        memberLogin.setMemberShortName(member.getShortName());
        map1.put("memberAvatar", member.getAvatarURL());
        memberLogin.setMemberAvatar(member.getAvatarURL());
        map1.put("loginName", member.getLoginName());
        memberLogin.setLoginName(member.getLoginName());

        map1.put("phone", member.getPhone());
        memberLogin.setPhone(member.getPhone());
        map1.put("weixinToken", member.getWeixinToken());
        memberLogin.setWeixinToken(member.getWeixinToken());
        map1.put("weixinUnionID", member.getWeixinUnionID());
        memberLogin.setWeixinUnionID(member.getWeixinUnionID());
        map1.put("recommendCode", member.getRecommendCode());
        memberLogin.setRecommendCode(member.getRecommendCode());
        map1.put("recommendID", member.getRecommandID());
        memberLogin.setRecommendID(member.getRecommandID());
        map1.put("companyID", member.getCompanyID());
        memberLogin.setCompanyID(member.getCompanyID());
        map1.put("shopID", member.getShopID());
        memberLogin.setShopID(member.getShopID());
        map1.put("rankID", member.getRankID());
        memberLogin.setRankID(member.getRankID());
        map1.put("peopleID", member.getPeopleID());
        memberLogin.setPeopleID(member.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);


        memberService.updateMember(map);
//        } else {
//            sessionID = "当前设备没有注册会员";
//            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotMember, "当前设备没有注册会员");
//        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        return MessagePacket.newSuccess(rsMap, "sessionTimeoutLogin success");
    }

    @ApiOperation(value = "phoneLoginJiGuang", notes = "独立接口：极光手机号一键登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/phoneLoginJiGuang", produces = {"application/json;charset=UTF-8"})
    public MessagePacket phoneLoginJiGuang(HttpServletRequest request, ModelMap map, ModelMap memberMap) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String loginToken = request.getParameter("loginToken");
        if (StringUtils.isBlank(loginToken)) {
            return MessagePacket.newFail(MessageHeader.Code.loginTokenNotNull, "loginToken不能为空");
        }
        String AppKey = request.getParameter("AppKey");
        if (StringUtils.isBlank(AppKey)) {
            return MessagePacket.newFail(MessageHeader.Code.AppKeyNotNull, "AppKey不能为空");
        }
        String MasterSecret = request.getParameter("MasterSecret");
        if (StringUtils.isBlank(MasterSecret)) {
            return MessagePacket.newFail(MessageHeader.Code.MasterSecretNotNull, "MasterSecret不能为空");
        }
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isBlank(bizBlock)) {
            bizBlock = "0";
        }
        SiteDto site = siteService.getSiteDetail(siteID);
        String applicationID = site.getApplicationID();
        String companyID = site.getCompanyid();
        // 极光私钥
        String prikey = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBANdKocSxzsPvX9ug" +
                "FizA6Lo7/+V/jw4HW8Uy2fn4bsVQagrRJdFzGDlo3Hf/HV80OxL1HrNCmwOM3Dar" +
                "HE+I2ds3ef0K1YWtS4AGn2hYgAft2XJ/GLHo0jxuXxFKOjYKP4duivI36xyF+xK/" +
                "5/HtNx+yOkGArEF16v3GPY9R8GsrAgMBAAECgYEAq9cgI/WgifgI1S6ERJh9qhi1" +
                "+iLEtnfdIhuirMlE8S1sVtH1k6aAUWKHhygT86em90y7gm3JeR33r5MId9tNtxqZ" +
                "OcjqpZ4RnvoStQpVG5jmyeeqtrnxbFe4sSpw7WYtMxH9QQ1uN92v3yVq+Rr4hUO1" +
                "h6V3kK3QI225ml2SPiECQQD6PnEf3ZKoqnk3IX+rSXY/QQYjTfG8JLMFq/CkK+Y3" +
                "qZfj+7k+v6zZaVlVnXiSyW/nSkfyuOc7CJoa2Cl488njAkEA3D5flBIAdoiNTmRq" +
                "fzg0N94RwCmzQt24gJYwwPtQgJmhIBYN4lGXr+X7wCiikehWJSXPltyBy5zSQzgt" +
                "gNW8GQJAc5bkKhPu6nnUA+M1ValZNV6TtzsJrh5FkxkYzrx4Wr27q1Na/eELtmEz" +
                "IpjaWPMy/WMSWMuQA3S1ujCe93+kgQJBANDB0kiAU64GuqGObp9Nf7lgpUSWghKk" +
                "JGjFc0rIK0Lp256VKO3W1sdkX56BJa7VISouz7g4JFMBvw715bOTY6kCQQDMZE4F" +
                "bjtHVsn6rtAd3u/X1g7/bf8hrWFK5Xl1SsXfzC6qoaCrkTmTr6M7sqcs8IK+NOCM" +
                "AQOjK1Tx37fMHePy";
//        String prikey = request.getParameter("prikey");
//        if (StringUtils.isBlank(prikey)){
//            return MessagePacket.newFail(MessageHeader.Code.prikeyNotNull, "prikey不能为空");
//        }
        // 获取basic auth认证
        String auth = AppKey + ":" + MasterSecret;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        // 连接极光接口loginTokenVerify API获取加密的手机号
        Map<String, Object> returnMap = new HashMap<>();
        String errorMsg = "";
        String encryptionPhone = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            String uri = "https://api.verification.jpush.cn/v1/web/loginTokenVerify";
            HttpPost httppost = new HttpPost(uri);
            //添加http头信息
            httppost.addHeader("Authorization", authHeader); //认证token
            httppost.addHeader("Content-Type", "application/json");
            net.sf.json.JSONObject obj = new net.sf.json.JSONObject();
            obj.put("loginToken", loginToken);
            httppost.setEntity(new StringEntity(obj.toString()));
            HttpResponse response;
            response = httpclient.execute(httppost);
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
        }
        // 检查返回值中的数据，查看请求是否成功
        Integer returnCode = (Integer) returnMap.get("code");
        if (8000 == returnCode) {
            // 请求成功，获取加密手机号
            encryptionPhone = (String) returnMap.get("phone");
        } else {
            // 请求失败，打印失败的信息
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "接口调用失败：" + errorMsg);
        }
        //解析加密的手机号
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(java.util.Base64.getDecoder().decode(prikey));
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
//        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
//        Cipher cipher = Cipher.getInstance("RSA");
//        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] b = java.util.Base64.getDecoder().decode(encryptionPhone);
        String phone = new String(cipher.doFinal(b));
        // 到系统中查找是否存在会员，如果存在就登录，不存在就创建
        String sessionID = "";
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        String MemberID = "";
        map.put("phone", phone);
        map.put("applicationID", applicationID);
        map.put("bizBlock", bizBlock);
        String memberID = memberService.selectByPhone(map);
        if (StringUtils.isNotBlank(memberID)) {
            Member member = memberService.selectById(memberID);
            member.setSiteID(siteID);
            if (member.getIsLock() == 1) {
                return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "当前会员已被锁定");
            }
            MemberID = memberID;
            map.clear();
            map.put("memberID", memberID);
            map.put("deviceID", deviceID);
            String memberDeviceID = memberDeviceService.getIdByMap(map);
            if (StringUtils.isBlank(memberDeviceID)) {
                // 创建memberDevice记录
                String newMemberDeviceID = IdGenerator.uuid32();
                map.put("id", newMemberDeviceID);
                memberDeviceService.insertByMap(map);
            }
            // 登录
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            sessionID = session.getId();

            // session检查
            memberService.checkSession(deviceID, sessionID, memberID, siteID, member.toString());

            session.setMaxInactiveInterval(5);
            String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")极光手机号一键登录成功";
            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.phoneLoginJiGuang);
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            String recommendCode = request.getParameter("recommendCode");
            if (StringUtils.isNotBlank(recommendCode)) {
                map.put("recommendCode", recommendCode);
                Member member1 = memberService.getMemberByRecommendCode(map);
                if (member1 != null) {
                    map.put("recommandID", member1.getId());
                }
            }
            memberService.updateMember(map);
        } else {
            // 注册会员
            map.clear();
            map.put("levelNumber", 1);
            String recommendMemberID = request.getParameter("recommendMemberID");
            if (StringUtils.isNotBlank(recommendMemberID)) {
                if (!memberService.checkMemberId(recommendMemberID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
                }
                map.put("recommendID", recommendMemberID);
                MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(recommendMemberID);
                Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                memberStatistics.setRecommendMembers(recommendNum);
                memberStatisticsService.updateMemberStatistics(memberStatistics);
                Member member1 = memberService.selectById(recommendMemberID);
                map.put("levelNumber", member1.getLevelNumber() + 1);
                // 获取推荐链
                if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                    map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                } else {
                    map.put("recommandChain", member1.getRecommandCode());
                }
            }
            String weixin = request.getParameter("weixin");
            if (StringUtils.isNotBlank(weixin)) {
                map.put("weixin", weixin);
            }
            String weixinCode = request.getParameter("weixinCode");
            if (StringUtils.isNotBlank(weixinCode)) {
                map.put("weixinCode", weixinCode);
            }

            String avatarURL = request.getParameter("avatarURL");
            if (StringUtils.isNotBlank(avatarURL)) {
                map.put("avatarURL", avatarURL);
            }
            String newMemberID = IdGenerator.uuid32();
            MemberID = newMemberID;
            map.put("id", newMemberID);
            map.put("companyID", companyID);
            map.put("applicationID", applicationID);
            if (StringUtils.isNotBlank(siteID)) {
                map.put("siteID", siteID);
            }
            map.put("phone", phone);
            map.put("loginName", phone);
            map.put("bizBlock", bizBlock);
            map.put("createType", 4);
            map.put("qiyeweixinID", sequenceDefineService.genCode("qiyeweixinID", newMemberID));
            String code = memberService.getCurRecommandCode(site.getApplicationID());
            if (StringUtils.isNotEmpty(code)) {
                map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
            } else {
                map.put("recommendCode", "00001");
            }
            String finalBizBlock = bizBlock;
            List<ApiRankListDto> rankList = rankService.getRankList(
                    new HashMap<String, Object>() {{
                        put("applicationID", applicationID);
                        put("bizBlock", finalBizBlock);
                        put("sortTypeOrderSeq", 1);
                    }});
            if (!rankList.isEmpty() && rankList.size() > 0) {
                ApiRankListDto rank = rankList.get(0);
                map.put("name", rank.getName() + map.get("recommendCode").toString());
                map.put("shortName", rank.getName() + map.get("recommendCode").toString());
            } else {
                map.put("name", phone);
                map.put("shortName", phone);
            }
            memberService.insertByMap(map);
            // 创建会员设备记录
            map.clear();
            map.put("memberID", newMemberID);
            map.put("deviceID", deviceID);
            String newMemberDeviceIDs = IdGenerator.uuid32();
            map.put("id", newMemberDeviceIDs);
            memberDeviceService.insertByMap(map);
            // 登录
            deviceService.updateLastLoadTime(deviceID);
            Member member = memberService.selectById(newMemberID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            sessionID = session.getId();

            // 多货币队列
            sendMessageService.sendMcApplicationCurrency(newMemberID, WebUtil.getRemortIP(request));

            // session检查
            memberService.checkSession(deviceID, sessionID, newMemberID, siteID, member.toString());

            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")极光手机号一键注册成功";
            sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.addSuccess);

            description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")极光手机号一键登录成功";
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.phoneLoginJiGuang);
            memberService.createMemberPrivilege(site.getApplicationID(), member.getId());
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            String recommendCode = request.getParameter("recommendCode");
            if (StringUtils.isNotBlank(recommendCode)) {
                map.put("recommendCode", recommendCode);
                Member member1 = memberService.getMemberByRecommendCode(map);
                if (member1 != null) {
                    map.put("recommandID", member1.getId());
                    MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(member1.getId());
                    Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                    memberStatistics.setRecommendMembers(recommendNum);
                    memberStatisticsService.updateMemberStatistics(memberStatistics);
                    map.put("levelNumber", member1.getLevelNumber() + 1);
                    // 获取推荐链
                    if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                        map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                    } else {
                        map.put("recommandChain", member1.getRecommandCode());
                    }
                }
            }
            memberService.updateMember(map);
            // 储存进redis
            memberService.saveMemberRedis(memberID);
        }
        // 检查memberDevice
        memberService.checkMemberDevice(MemberID, deviceID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        rsMap.put("memberID", MemberID);
        return MessagePacket.newSuccess(rsMap, "phoneLoginJiGuang success");
    }

    @ApiOperation(value = "weiXinLogin", notes = "独立接口：微信一键登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/weiXinLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket weiXinLogin(HttpServletRequest request, ModelMap map, ModelMap memberMap) throws UnsupportedEncodingException {
        String accessToken = request.getParameter("accessToken");
        if (StringUtils.isBlank(accessToken)) {
            return MessagePacket.newFail(MessageHeader.Code.accessTokenNotNull, "accessToken不能为空");
        }
        String openID = request.getParameter("openID");
        if (StringUtils.isBlank(openID)) {
            return MessagePacket.newFail(MessageHeader.Code.openidIsNull, "openID不能为空");
        }
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isBlank(bizBlock)) {
            bizBlock = "0";
        }
        String phone = request.getParameter("phone");

        // 连接微信接口获取个人信息
        Map<String, Object> returnMap = new HashMap<>();
        String errorMsg = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            String uri = "https://api.weixin.qq.com/sns/userinfo?access_token=" + accessToken +
                    "&openid=" + openID + "&lang=en";
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
        }
        // 检查返回值中的数据，查看请求是否成功
        String errmsg = (String) returnMap.get("errmsg");
        if (StringUtils.isNotBlank(errmsg)) {
            // 请求失败，打印失败的信息
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "接口调用失败：" + errorMsg);
        }
        SiteDto site = siteService.getSiteDetail(siteID);
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        String applicationID = site.getApplicationID();
        String companyID = site.getCompanyid();
        // 到系统中查找是否存在会员，如果存在就登录，不存在就创建
        String sessionID = "";
        String MemberID = "";
        String weixinUnionID = (String) returnMap.get("unionid");
        map.clear();
        if (StringUtils.isNotBlank(weixinUnionID)) {
            map.put("weixinUnionID", weixinUnionID);
        } else {
            map.put("weixinUnionID", openID);
        }
        map.put("bizBlock", bizBlock);
        map.put("applicationID", applicationID);
        String memberID = memberService.selectByWeixinUnionIDAndBizBlock(map);
        if (StringUtils.isNotBlank(memberID)) {
            Member member = memberService.selectById(memberID);
            member.setSiteID(siteID);
            if (member.getIsLock() == 1) {
                return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "当前会员已被锁定");
            }
            MemberID = memberID;
            map.clear();
            map.put("memberID", memberID);
            map.put("deviceID", deviceID);
            String memberDeviceID = memberDeviceService.getIdByMap(map);
            if (StringUtils.isBlank(memberDeviceID)) {
                // 创建memberDevice记录
                String newMemberDeviceID = IdGenerator.uuid32();
                map.put("id", newMemberDeviceID);
                memberDeviceService.insertByMap(map);
            }
            // 登录
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            sessionID = session.getId();

            // session检查
            memberService.checkSession(deviceID, sessionID, memberID, siteID, member.toString());

            String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")微信一键登录成功";
            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.weiXinLogin);
            map.clear();
            map.put("id", member.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            //新增memberLogin
            MemberLogin memberLogin = new MemberLogin();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("applicationID", site.getApplicationID());
            memberLogin.setApplicationID(site.getApplicationID());
            map1.put("applicationName", site.getApplicationName());
            memberLogin.setApplicationName(site.getApplicationName());
            map1.put("siteID", site.getId());
            memberLogin.setSiteID(site.getId());
            map1.put("siteName", site.getName());
            memberLogin.setSiteName(site.getName());
            map1.put("deviceID", deviceID);
            memberLogin.setDeviceID(deviceID);
            String ip = WebUtil.getRemortIP(request);
            map1.put("ipAddress", ip);
            memberLogin.setIpAddress(ip);
            map1.put("locationID", memberService.getLocationID(ip));
            memberLogin.setLocationID(memberService.getLocationID(ip));
            String userAgent = request.getHeader("User-Agent");
            String browserType = "Unknown";
            if (userAgent != null) {
                if (userAgent.contains("Firefox")) {
                    browserType = "Firefox";
                } else if (userAgent.contains("Chrome")) {
                    browserType = "Chrome";
                } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                    browserType = "Internet Explorer";
                } else if (userAgent.contains("Safari")) {
                    browserType = "Safari";
                } else if (userAgent.contains("Opera")) {
                    browserType = "Opera";
                }
            }
            map1.put("browserType", browserType);
            memberLogin.setBrowserType(browserType);
            map1.put("memberID", member.getId());
            memberLogin.setMemberID(member.getId());
            map1.put("memberName", member.getName());
            memberLogin.setMemberName(member.getName());
            map1.put("memberShortName", member.getShortName());
            memberLogin.setMemberShortName(member.getShortName());
            map1.put("memberAvatar", member.getAvatarURL());
            memberLogin.setMemberAvatar(member.getAvatarURL());
            map1.put("loginName", member.getLoginName());
            memberLogin.setLoginName(member.getLoginName());

            map1.put("phone", member.getPhone());
            memberLogin.setPhone(member.getPhone());
            map1.put("weixinToken", member.getWeixinToken());
            memberLogin.setWeixinToken(member.getWeixinToken());
            map1.put("weixinUnionID", member.getWeixinUnionID());
            memberLogin.setWeixinUnionID(member.getWeixinUnionID());
            map1.put("recommendCode", member.getRecommendCode());
            memberLogin.setRecommendCode(member.getRecommendCode());
            map1.put("recommendID", member.getRecommandID());
            memberLogin.setRecommendID(member.getRecommandID());
            map1.put("companyID", member.getCompanyID());
            memberLogin.setCompanyID(member.getCompanyID());
            map1.put("shopID", member.getShopID());
            memberLogin.setShopID(member.getShopID());
            map1.put("rankID", member.getRankID());
            memberLogin.setRankID(member.getRankID());
            map1.put("peopleID", member.getPeopleID());
            memberLogin.setPeopleID(member.getPeopleID());
            memberLogin.setLoginTime(TimeTest.getTime());
            /**
             * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
             * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
             * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
             * 并设置 isWork=1；(感觉没必要用消息队列)
             */
            Employee employee = employeeService.getMemberLogin(memberLogin);
            if (employee != null) {
                if (StringUtils.isNotBlank(employee.getCompanyID())) {
                    memberLogin.setIsWork(1);
                    memberLogin.setWorkEmployeeID(employee.getId());
                    memberLogin.setWorkCompanyID(employee.getCompanyID());
                    memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                    memberLogin.setWorkShopID(employee.getShopID());
                    memberLogin.setWorkJobID(employee.getJobID());
                    memberLogin.setWorkGradeID(employee.getGradeID());
                }
            }
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

            memberService.updateMember(map);
        } else {
            // 注册会员
            map.clear();
            map.put("levelNumber", 1);
            String recommendCode = request.getParameter("recommendCode");
            if (StringUtils.isNotBlank(recommendCode)) {
                map.put("recommendCode", recommendCode);
                Member member1 = memberService.getMemberByRecommendCode(map);
                if (member1 != null) {
                    map.put("recommendID", member1.getId());
                    MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(member1.getId());
                    Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                    memberStatistics.setRecommendMembers(recommendNum);
                    memberStatisticsService.updateMemberStatistics(memberStatistics);
                    map.put("levelNumber", member1.getLevelNumber() + 1);
                    // 获取推荐链
                    if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                        map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                    } else {
                        map.put("recommandChain", member1.getRecommandCode());
                    }
                }
            }
            String recommendMemberID = request.getParameter("recommendMemberID");
            if (StringUtils.isNotBlank(recommendMemberID)) {
                if (!memberService.checkMemberId(recommendMemberID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
                }
                map.put("recommendID", recommendMemberID);
                MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(recommendMemberID);
                Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                memberStatistics.setRecommendMembers(recommendNum);
                memberStatisticsService.updateMemberStatistics(memberStatistics);
                // 获取推荐链
                Member member1 = memberService.selectById(recommendMemberID);
                map.put("levelNumber", member1.getLevelNumber() + 1);
                if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                    map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                } else {
                    map.put("recommandChain", member1.getRecommandCode());
                }
            }
            String weixin = request.getParameter("weixin");
            if (StringUtils.isNotBlank(weixin)) {
                map.put("weixin", weixin);
            }
            String weixinCode = request.getParameter("weixinCode");
            if (StringUtils.isNotBlank(weixinCode)) {
                map.put("weixinCode", weixinCode);
            }
            String avatarURL = request.getParameter("avatarURL");
            if (StringUtils.isNotBlank(avatarURL)) {
                map.put("avatarURL", avatarURL);
            }
            String newMemberID = IdGenerator.uuid32();
            MemberID = newMemberID;
            map.put("id", newMemberID);
            map.put("companyID", companyID);
            map.put("createType", 5);
            if (StringUtils.isNotBlank(phone)) {
                map.put("phone", phone);
                map.put("loginName", phone);
            }
            map.put("bizBlock", bizBlock);
            map.put("applicationID", applicationID);
            map.put("siteID", siteID);
            String nickname = (String) returnMap.get("nickname");
            if (StringUtils.isNotBlank(nickname)) {
                String name = new String(nickname.getBytes("ISO-8859-1"), "UTF-8");
                map.put("name", name);
                map.put("shortName", name);
            }
            String headimgurl = (String) returnMap.get("headimgurl");
            if (StringUtils.isNotBlank(headimgurl)) {
                map.put("avatarURL", headimgurl);
            }
            String unionid = (String) returnMap.get("unionid");
            if (StringUtils.isNotBlank(unionid)) {
                map.put("weixinUnionID", unionid);
            } else {
                map.put("weixinUnionID", openID);
            }
            map.put("qiyeweixinID", sequenceDefineService.genCode("qiyeweixinID", newMemberID));
            String code = memberService.getCurRecommandCode(site.getApplicationID());
            if (StringUtils.isNotEmpty(code)) {
                map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
            } else {
                map.put("recommendCode", "00001");
            }
            Integer sex = (Integer) returnMap.get("sex");
            if (sex != null) {
                if (sex == 1) {
                    map.put("titleID", "b3d9feef6c5c485fa1d089503f034952");
                } else if (sex == 2) {
                    map.put("titleID", "23ec6505d9d742f0bdd0046dfd64b92c");
                }
            }
            map.put("weixinToken", openID);
            memberService.insertByMap(map);
            // 创建会员设备记录
            map.clear();
            map.put("memberID", newMemberID);
            map.put("deviceID", deviceID);
            String newMemberDeviceIDs = IdGenerator.uuid32();
            map.put("id", newMemberDeviceIDs);
            memberDeviceService.insertByMap(map);
            // 登录
            Member member1 = memberService.selectById(MemberID);
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);

            // 多货币队列
            sendMessageService.sendMcApplicationCurrency(MemberID, WebUtil.getRemortIP(request));

            // session检查
            memberService.checkSession(deviceID, session.getId(), MemberID, siteID, member1.toString());

            MemberLogDTO memberLogDTO = new MemberLogDTO(member1.getSiteID(), member1.getApplicationID(),
                    deviceID, member1.getId(), memberService.getLocationID(ipaddr), member1.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member1, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            String description = "会员:" + member1.getName() + "使用设备(" + device.getName() + ")微信一键住注册成功";
            sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.addSuccess);
            description = "会员:" + member1.getName() + "使用设备(" + device.getName() + ")微信一键登录成功";
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.weiXinLogin);
            memberService.createMemberPrivilege(site.getApplicationID(), member1.getId());
            map.clear();
            map.put("id", member1.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            memberService.updateMember(map);
            sessionID = session.getId();
            // 储存进redis
            memberService.saveMemberRedis(memberID);
            //新增memberLogin
            Member member = memberService.selectById(memberID);

            MemberLogin memberLogin = new MemberLogin();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("applicationID", site.getApplicationID());
            memberLogin.setApplicationID(site.getApplicationID());
            map1.put("applicationName", site.getApplicationName());
            memberLogin.setApplicationName(site.getApplicationName());
            map1.put("siteID", site.getId());
            memberLogin.setSiteID(site.getId());
            map1.put("siteName", site.getName());
            memberLogin.setSiteName(site.getName());
            map1.put("deviceID", deviceID);
            memberLogin.setDeviceID(deviceID);
            String ip = WebUtil.getRemortIP(request);
            map1.put("ipAddress", ip);
            memberLogin.setIpAddress(ip);
            map1.put("locationID", memberService.getLocationID(ip));
            memberLogin.setLocationID(memberService.getLocationID(ip));
            String userAgent = request.getHeader("User-Agent");
            String browserType = "Unknown";
            if (userAgent != null) {
                if (userAgent.contains("Firefox")) {
                    browserType = "Firefox";
                } else if (userAgent.contains("Chrome")) {
                    browserType = "Chrome";
                } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                    browserType = "Internet Explorer";
                } else if (userAgent.contains("Safari")) {
                    browserType = "Safari";
                } else if (userAgent.contains("Opera")) {
                    browserType = "Opera";
                }
            }
            map1.put("browserType", browserType);
            memberLogin.setBrowserType(browserType);
            map1.put("memberID", member.getId());
            memberLogin.setMemberID(member.getId());
            map1.put("memberName", member.getName());
            memberLogin.setMemberName(member.getName());
            map1.put("memberShortName", member.getShortName());
            memberLogin.setMemberShortName(member.getShortName());
            map1.put("memberAvatar", member.getAvatarURL());
            memberLogin.setMemberAvatar(member.getAvatarURL());
            map1.put("loginName", member.getLoginName());
            memberLogin.setLoginName(member.getLoginName());

            map1.put("phone", member.getPhone());
            memberLogin.setPhone(member.getPhone());
            map1.put("weixinToken", member.getWeixinToken());
            memberLogin.setWeixinToken(member.getWeixinToken());
            map1.put("weixinUnionID", member.getWeixinUnionID());
            memberLogin.setWeixinUnionID(member.getWeixinUnionID());
            map1.put("recommendCode", member.getRecommendCode());
            memberLogin.setRecommendCode(member.getRecommendCode());
            map1.put("recommendID", member.getRecommandID());
            memberLogin.setRecommendID(member.getRecommandID());
            map1.put("companyID", member.getCompanyID());
            memberLogin.setCompanyID(member.getCompanyID());
            map1.put("shopID", member.getShopID());
            memberLogin.setShopID(member.getShopID());
            map1.put("rankID", member.getRankID());
            memberLogin.setRankID(member.getRankID());
            map1.put("peopleID", member.getPeopleID());
            memberLogin.setPeopleID(member.getPeopleID());
            memberLogin.setLoginTime(TimeTest.getTime());
            /**
             * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
             * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
             * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
             * 并设置 isWork=1；(感觉没必要用消息队列)
             */
            Employee employee = employeeService.getMemberLogin(memberLogin);
            if (employee != null) {
                if (StringUtils.isNotBlank(employee.getCompanyID())) {
                    memberLogin.setIsWork(1);
                    memberLogin.setWorkEmployeeID(employee.getId());
                    memberLogin.setWorkCompanyID(employee.getCompanyID());
                    memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                    memberLogin.setWorkShopID(employee.getShopID());
                    memberLogin.setWorkJobID(employee.getJobID());
                    memberLogin.setWorkGradeID(employee.getGradeID());
                }
            }
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, session.getId()), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        }
        // 检查memberDevice
        memberService.checkMemberDevice(MemberID, deviceID);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        rsMap.put("memberID", MemberID);
        return MessagePacket.newSuccess(rsMap, "weiXinLogin success");
    }

    @ApiOperation(value = "changeSJName", notes = "独立接口：设计得到会员修改个人唯一号码")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/changeSJName", produces = {"application/json;charset=UTF-8"})
    public MessagePacket changeSJName(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String qiyeweixinID = request.getParameter("qiyeweixinID");
        if (StringUtils.isBlank(qiyeweixinID)) {
            return MessagePacket.newFail(MessageHeader.Code.qiyeweixinIDNotNull, "qiyeweixinID不能为空");
        }
        String id = memberService.getByQiYeWeiXinID(qiyeweixinID);
        if (StringUtils.isNotBlank(id)) {
            return MessagePacket.newFail(MessageHeader.Code.qiyeweixinIDISFound, "qiyeweixinID已经存在");
        }
        // 判断今年修改了几次
        map.put("memberID", member.getId());
        map.put("operateType", "%设计得到会员修改个人唯一号码%");
        Integer count = memberService.updateMember(map);
        if (count >= 2) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "今年已经修改过两次了");
        } else {
            map.put("id", member.getId());
            map.put("qiyeweixinID", qiyeweixinID);
            memberService.updateMember(map);
        }
        // 记录会员日志
        String description = "会员：" + member.getName() + "修改修改个人唯一号码";
        map.clear();
        map.put("id", IdGenerator.uuid32());
        map.put("operateType", description);
        map.put("description", description);
        map.put("applicationID", member.getApplicationID());
        map.put("siteID", member.getSiteID());
        memberService.submitOneCertCode(map);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "changeSJName success");
    }

    @ApiOperation(value = "memberWithWeixinUnionID", notes = "独立接口：会员绑定微信")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/memberWithWeixinUnionID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberWithWeixinUnionID(HttpServletRequest request, ModelMap map) {
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
            String memberID = memberService.selectByPhone(map);
            map.clear();
            member = memberService.selectById(memberID);
        }
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
//       member = memberService.selectById(sessionID);
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            String modifierId = null;
            if (member != null) {
                modifierId = member.getId();
            }
            map.put("modifier", modifierId);
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            Member member1 = memberService.selectById(member.getId());
            phone = member1.getPhone();
        }
        if (StringUtils.isBlank(phone) && StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "phone和sessionID不能同时为空");
        }
        String accessToken = request.getParameter("accessToken");
        if (StringUtils.isBlank(accessToken)) {
            return MessagePacket.newFail(MessageHeader.Code.accessTokenNotNull, "accessToken不能为空");
        }
        String openID = request.getParameter("openID");
        if (StringUtils.isBlank(openID)) {
            return MessagePacket.newFail(MessageHeader.Code.openidIsNull, "openID不能为空");
        }
        String verifyCode = request.getParameter("verifyCode");
        if (StringUtils.isNotBlank(verifyCode)) {
            String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.BIND_WECHART_CODE, phone));
            if (StringUtils.isNotBlank(verifyCode)) {
                if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
                    return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
                }
            }
        }
        // 连接微信接口获取个人信息
        Map<String, Object> returnMap = new HashMap<>();
        String errorMsg = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            String uri = "https://api.weixin.qq.com/sns/userinfo?access_token=" + accessToken +
                    "&openid=" + openID + "&lang=en";
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
        }
        // 检查返回值中的数据，查看请求是否成功
        String errmsg = (String) returnMap.get("errmsg");
        if (StringUtils.isNotBlank(errmsg)) {
            // 请求失败，打印失败的信息
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "接口调用失败：" + errorMsg);
        }
        Member member1 = null;
        if (member!=null){
            member1 = memberService.selectById(member.getId());
        }
        String weixinUnionID =null;
        if (member1!=null){
            weixinUnionID = member1.getWeixinUnionID();
        }
        if (weixinUnionID!=null&&StringUtils.isNotBlank(weixinUnionID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "已经绑定过微信号了");
        } else {
            weixinUnionID = (String) returnMap.get("unionid");
            map.clear();
            String weixin = request.getParameter("weixin");
            if (StringUtils.isNotBlank(weixin)) {
                map.put("weixin", weixin);
            }
            String weixinCode = request.getParameter("weixinCode");
            if (StringUtils.isNotBlank(weixinCode)) {
                map.put("weixinCode", weixinCode);
            }
            String avatarURL = request.getParameter("avatarURL");
            if (StringUtils.isNotBlank(avatarURL)) {
                map.put("avatarURL", avatarURL);
            }
            if (StringUtils.isNotBlank(weixinUnionID)) {
                map.put("weixinUnionID", weixinUnionID);
            } else {
                map.put("weixinUnionID", openID);
                weixinUnionID = openID;
            }
            map.put("weixinToken", openID);
            if (member != null) {
                map.put("id", member.getId());
            }
            List<Member> members = memberService.selectMemberByWeiXinUnionID(weixinUnionID);
            if (members != null && members.size() > 0) {
                for (int i = 0; i < members.size(); i++) {
                    if (members.get(i).getPhone() == null) {
                        String id = members.get(i).getId();
                        memberService.delById(id);
                    } else {
                        return MessagePacket.newFail(MessageHeader.Code.fail, "当前微信号已经被绑定");
                    }
                }
            }
            memberService.updateMemberByMap(map);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "memberWithWeixinUnionID success");
    }

    /**
     * 设计得到H5一键登录
     *
     * @param request
     * @return
     */
    @RequestMapping("SJH5WeChatLogin")
    @ResponseBody
    public MessagePacket SJH5WeChatLogin(HttpServletRequest request, ModelMap memberMap) throws UnsupportedEncodingException {
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
        }
        String appID = request.getParameter("appID");
        if (StringUtils.isBlank(appID)) {
            return MessagePacket.newFail(MessageHeader.Code.appIDNotNull, "appID不能为空");
        }
        String code = request.getParameter("code");
        if (StringUtils.isBlank(code)) {
            return MessagePacket.newFail(MessageHeader.Code.codeNotNull, "code不能为空");
        }
        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isBlank(bizBlock)) {
            bizBlock = "0";
        }
        String isKZY = request.getParameter("isKZY");
        String phone = request.getParameter("phone");
        // 通过appID找到AppSecret
        Wechart wechart = wechartService.getWechartByAppID(appID);
        if (wechart == null) {
            return MessagePacket.newFail(MessageHeader.Code.appIDNotFound, "appID不能为空");
        }
        String secret = wechart.getAppSecret();
        if (StringUtils.isBlank(secret)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "对应的appSecret为空");
        }
        // 调用微信api通过code获取token和openID
        Map tokenAndOpen = WechartApi.getGZHTokenAndOpenID(appID, secret, code);
        String openid = (String) tokenAndOpen.get("openid");
        String token = (String) tokenAndOpen.get("access_token");
        // 连接微信接口获取个人信息
        Map<String, Object> returnMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        String errorMsg = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            String uri = "https://api.weixin.qq.com/sns/userinfo?access_token=" + token +
                    "&openid=" + openid + "&lang=en";
            HttpGet httpGet = new HttpGet(uri);
            //添加http头信息
            httpGet.addHeader("Accept", "application/json;charset=UTF-8");
            net.sf.json.JSONObject obj = new net.sf.json.JSONObject();
            HttpResponse response;
            response = httpclient.execute(httpGet);
            //检验状态码，如果成功接收数据
            int jsonCode = response.getStatusLine().getStatusCode();
            if (jsonCode == 200) {
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
        }
        // 检查返回值中的数据，查看请求是否成功
        String errmsg = (String) returnMap.get("errmsg");
        if (StringUtils.isNotBlank(errmsg)) {
            // 请求失败，打印失败的信息
            return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "接口调用失败：" + errorMsg);
        }
        SiteDto site = siteService.getSiteDetail(siteID);
        String applicationID = site.getApplicationID();
        String companyID = site.getCompanyid();
        // 到系统中查找是否存在会员，如果存在就登录，不存在就创建
        String sessionID = "";
        String MemberID = "";
        map.clear();
        String weixinUnionID = (String) returnMap.get("unionid");
        if (StringUtils.isNotBlank(weixinUnionID)) {
            map.put("weixinUnionID", weixinUnionID);
        } else {
            map.put("weixinUnionID", openid);
        }
        map.put("bizBlock", bizBlock);
        map.put("applicationID", applicationID);
        String memberID = memberService.selectByWeixinUnionIDAndBizBlock(map);
        ApiDeviceDetailDto device = deviceService.selectByID(deviceID);
        if (StringUtils.isNotBlank(memberID)) {
            Member member = memberService.selectById(memberID);
            member.setSiteID(siteID);
            if (member.getIsLock() == 1) {
                return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "当前会员已被锁定");
            }
            MemberID = memberID;
            map.clear();
            map.put("memberID", memberID);
            map.put("deviceID", deviceID);
            String memberDeviceID = memberDeviceService.getIdByMap(map);
            if (StringUtils.isBlank(memberDeviceID)) {
                // 创建memberDevice记录
                String newMemberDeviceID = IdGenerator.uuid32();
                map.put("id", newMemberDeviceID);
                memberDeviceService.insertByMap(map);
            }
            // 登录
            if (StringUtils.isNotBlank(member.getPhone()) || StringUtils.isNotBlank(isKZY)) {
                deviceService.updateLastLoadTime(deviceID);
                String ipaddr = WebUtil.getRemortIP(request);
                HttpSession session = request.getSession();
                session.setMaxInactiveInterval(5);

                // session检查
                memberService.checkSession(deviceID, session.getId(), memberID, siteID, member.toString());

                String description = "会员:" + member.getName() + "使用设备(" + device.getName() + ")微信一键登录成功";
                MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                        deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
                redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, redisTimeOut, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
                sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                        Memberlog.MemberOperateType.weiXinLogin);
                sessionID = session.getId();
            }
        } else {
            // 注册会员
            map.clear();
            map.put("levelNumber", 1);
            String recommendCode = request.getParameter("recommendCode");
            if (StringUtils.isNotBlank(recommendCode)) {
                map.put("recommendCode", recommendCode);
                Member member1 = memberService.getMemberByRecommendCode(map);
                if (member1 != null) {
                    map.put("recommendID", member1.getId());
                    MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(member1.getId());
                    Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                    memberStatistics.setRecommendMembers(recommendNum);
                    memberStatisticsService.updateMemberStatistics(memberStatistics);
                    map.put("levelNumber", member1.getLevelNumber() + 1);
                    // 获取推荐链
                    if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                        map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                    } else {
                        map.put("recommandChain", member1.getRecommandCode());
                    }
                }
            }
            String recommendMemberID = request.getParameter("recommendMemberID");
            if (StringUtils.isNotBlank(recommendMemberID)) {
                if (!memberService.checkMemberId(recommendMemberID)) {
                    return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendMemberID不正确");
                }
                map.put("recommendID", recommendMemberID);
                MemberStatisticsDto memberStatistics = memberStatisticsService.getByMemID(recommendMemberID);
                Integer recommendNum = memberStatistics.getRecommendMembers() + 1;
                memberStatistics.setRecommendMembers(recommendNum);
                memberStatisticsService.updateMemberStatistics(memberStatistics);
                // 获取推荐链
                Member member1 = memberService.selectById(recommendMemberID);
                map.put("levelNumber", member1.getLevelNumber() + 1);
                if (StringUtils.isNotBlank(member1.getRecommandChain())) {
                    map.put("recommandChain", member1.getRecommandChain() + "," + member1.getRecommandCode());
                } else {
                    map.put("recommandChain", member1.getRecommandCode());
                }
            }
            String weixin = request.getParameter("weixin");
            if (StringUtils.isNotBlank(weixin)) {
                map.put("weixin", weixin);
            }
            String weixinCode = request.getParameter("weixinCode");
            if (StringUtils.isNotBlank(weixinCode)) {
                map.put("weixinCode", weixinCode);
            }
            String avatarURL = request.getParameter("avatarURL");
            if (StringUtils.isNotBlank(avatarURL)) {
                map.put("avatarURL", avatarURL);
            }
            String newMemberID = IdGenerator.uuid32();
            MemberID = newMemberID;
            map.put("id", newMemberID);
            map.put("companyID", companyID);
            map.put("applicationID", applicationID);
            map.put("bizBlock", bizBlock);
            map.put("levelNumber", 1);
            if (StringUtils.isNotBlank(phone)) {
                map.put("phone", phone);
                map.put("loginName", phone);
            }
            map.put("createType", 5);
            map.put("siteID", siteID);
            String nickname = (String) returnMap.get("nickname");
            if (StringUtils.isNotBlank(nickname)) {
                String name = new String(nickname.getBytes("ISO-8859-1"), "UTF-8");
                map.put("name", name);
                map.put("shortName", name);
                map.put("weixin", name);
            }
            String headimgurl = (String) returnMap.get("headimgurl");
            if (StringUtils.isNotBlank(headimgurl)) {
                map.put("avatarURL", headimgurl);
            }
            String unionid = (String) returnMap.get("unionid");
            if (StringUtils.isNotBlank(unionid)) {
                map.put("weixinUnionID", unionid);
            } else {
                map.put("weixinUnionID", openid);
            }
            map.put("qiyeweixinID", sequenceDefineService.genCode("qiyeweixinID", newMemberID));
            String code1 = memberService.getCurRecommandCode(site.getApplicationID());
            if (StringUtils.isNotEmpty(code1)) {
                map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code1) + 1)));
            } else {
                map.put("recommendCode", "00001");
            }
            Integer sex = (Integer) returnMap.get("sex");
            if (sex != null) {
                if (sex == 1) {
                    map.put("titleID", "b3d9feef6c5c485fa1d089503f034952");
                } else if (sex == 2) {
                    map.put("titleID", "23ec6505d9d742f0bdd0046dfd64b92c");
                }
            }
            memberService.insertByMap(map);
            // 创建会员设备记录
            map.clear();
            map.put("memberID", newMemberID);
            map.put("deviceID", deviceID);
            String newMemberDeviceIDs = IdGenerator.uuid32();
            map.put("id", newMemberDeviceIDs);
            memberDeviceService.insertByMap(map);
            // 登录
            Member member1 = memberService.selectById(MemberID);
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            sessionID = session.getId();

            // session检查
            memberService.checkSession(deviceID, session.getId(), MemberID, siteID, member1.toString());

            MemberLogDTO memberLogDTO = new MemberLogDTO(member1.getSiteID(), member1.getApplicationID(),
                    deviceID, member1.getId(), memberService.getLocationID(ipaddr), member1.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member1, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, redisTimeOut, TimeUnit.SECONDS);
            String description = "会员:" + member1.getName() + "使用设备(" + device.getName() + ")微信一键住注册成功";
            sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.addSuccess);
            description = "会员:" + member1.getName() + "使用设备(" + device.getName() + ")微信一键登录成功";
            sendMessageService.sendMemberLoginMessage(false, session.getId(), ipaddr, ipaddr, description,
                    Memberlog.MemberOperateType.weiXinLogin);
            memberService.createMemberPrivilege(site.getApplicationID(), member1.getId());
            map.clear();
            map.put("id", member1.getId());
            map.put("lastLoginTime", TimeTest.getTime());
            memberService.updateMember(map);
            // 储存进redis
            memberService.saveMemberRedis(memberID);
        }
        // 检查memberDevice
        memberService.checkMemberDevice(MemberID, deviceID);
        Map<String, String> rsMap = new HashMap<>();
        rsMap.put("sessionID", sessionID);
        rsMap.put("memberID", MemberID);
        rsMap.put("openID", openid);
        rsMap.put("access_token", token);
        return MessagePacket.newSuccess(rsMap, "SJH5WeChatLogin success");
    }



    @RequestMapping("queryWeixinOpenIDByCode")
    @ResponseBody
    public MessagePacket queryWeixinOpenIDByCode(HttpServletRequest request, ModelMap memberMap) {
        String ipAddr = WebUtil.getRemortIP(request);
        String appID = request.getParameter("appID");
        String code = request.getParameter("code");
        String deviceID = request.getParameter("deviceID");

        // === 新增：接收 recommendCode 参数 ===
        String recommendCode = request.getParameter("recommendCode");

        // 初始校验
        if (StringUtils.isBlank(appID)) return MessagePacket.newFail(MessageHeader.Code.appIDNotNull, "appID不能为空");
        if (StringUtils.isBlank(code)) return MessagePacket.newFail(MessageHeader.Code.codeNotNull, "code不能为空");

        // 获取公共信息：applicationID, companyID, weixinID 等
        Wechart wechart = wechartService.getWechartByAppID(appID);
        if (wechart == null) return MessagePacket.newFail(MessageHeader.Code.appIDNotFound, "未找到对应的Wechart配置");

        ModelMap weixinQueryMap = new ModelMap();
        weixinQueryMap.put("appID", appID);
        WeixinDetail weixinDetail = weixinService.getDetilByMap(weixinQueryMap);
        if (weixinDetail == null) return MessagePacket.newFail(MessageHeader.Code.fail, "未找到WeixinDetail公共信息");

        String access_token = null;
        String openid = null;
        String unionid = null;
        Integer expires_in = 0;
        String refresh_token = null;
        String scope = null;


        // 统一调用微信接口逻辑
        try {
            HttpClient httpclient = new DefaultHttpClient();
            // 1. 获取 token 和 openid
            String tokenUri = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appID +
                    "&secret=" + wechart.getAppSecret() + "&code=" + code + "&grant_type=authorization_code";
            HttpGet tokenGet = new HttpGet(tokenUri);
            tokenGet.addHeader("Accept", "application/json;charset=UTF-8");
            HttpResponse tokenRes = httpclient.execute(tokenGet);

            if (tokenRes.getStatusLine().getStatusCode() != 200) {
                return MessagePacket.newFail(MessageHeader.Code.fail, "微信Token接口通信失败");
            }

            String tokenRev = EntityUtils.toString(tokenRes.getEntity());
            net.sf.json.JSONObject tokenObj = net.sf.json.JSONObject.fromObject(tokenRev);

            if (tokenObj.has("errmsg") && StringUtils.isNotBlank(tokenObj.getString("errmsg"))) {
                return MessagePacket.newFail(MessageHeader.Code.illegalParameter, "微信接口返回错误：" + tokenRev);
            }

            access_token = tokenObj.optString("access_token");
            openid = tokenObj.optString("openid");
            expires_in = tokenObj.optInt("expires_in");
            refresh_token = tokenObj.optString("refresh_token");
            scope = tokenObj.optString("scope");

            // 2. 获取用户信息（主要为了获取 unionid）
            String userUri = "https://api.weixin.qq.com/sns/userinfo?access_token=" + access_token + "&openid=" + openid + "&lang=en";
            HttpGet userGet = new HttpGet(userUri);
            userGet.addHeader("Accept", "application/json;charset=UTF-8");
            HttpResponse userRes = httpclient.execute(userGet);

            if (userRes.getStatusLine().getStatusCode() == 200) {
                String userRev = EntityUtils.toString(userRes.getEntity());
                net.sf.json.JSONObject userObj = net.sf.json.JSONObject.fromObject(userRev);
                unionid = userObj.optString("unionid");
            }
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            return MessagePacket.newFail(MessageHeader.Code.fail, "调用微信接口发生系统异常");
        }

        log.info("【微信授权】deviceID={}, openid={}, unionid={}", deviceID, openid, unionid);

        try {
            Map<String, Object> newBrowse = Maps.newHashMap();
            newBrowse.put("id", IdGenerator.uuid32());
            newBrowse.put("applicationID", weixinDetail.getApplicationID());
            newBrowse.put("companyID", weixinDetail.getCompanyID());
            newBrowse.put("name", code);                      // name 保存 code
            newBrowse.put("objectName", appID + " & " + unionid);             // objectName 保存 unionID
            newBrowse.put("toObjectName", openid);            // toObjectName 保存 openID
            newBrowse.put("ipAddress", ipAddr);
            newBrowse.put("browseTime", TimeTest.getTimeStr());
            newBrowse.put("createdTime", TimeTest.getTimeStr());

            // 调用 browseService 保存记录
            browseService.insertByMap(newBrowse);
        } catch (Exception e) {
            log.error("保存 browse 浏览记录异常", e);
        }

            // 第一步：处理 device 逻辑
        if (StringUtils.isNotBlank(deviceID)) {
            ApiDeviceDetailDto deviceDetail = deviceService.selectByID(deviceID);
            if (deviceDetail != null) {
                Map<String, Object> upDeviceMap = Maps.newHashMap();
                upDeviceMap.put("deviceID", deviceID);
                upDeviceMap.put("browseNumber", deviceDetail.getBrowseNumber() + 1);
                upDeviceMap.put("lastLoadTime", TimeTest.getTimeStr());
                deviceService.updateDevicePhone(upDeviceMap);
            } else {
                return MessagePacket.newFail(MessageHeader.Code.fail, "deviceID不存在");
            }
        }

        String memberID = null;

        // 第二步：如果 unionid 不为空，优先用 unionid 查
        if (StringUtils.isNotBlank(unionid)) {
            Map<String, Object> memberQuery = Maps.newHashMap();
            memberQuery.put("weixinUnionID", unionid);
            Member m = memberService.getByMap(memberQuery);
            if (m != null) {
                memberID = m.getId();
            }
        }

        if (StringUtils.isBlank(unionid) && StringUtils.isBlank(memberID) && StringUtils.isNotBlank(openid)) {
            Member m = memberService.getMemberByWeixinToken(openid);
            if (m != null) memberID = m.getId();
        }


        String recommendMemberID = null;
        if (StringUtils.isNotBlank(recommendCode)) {
            Map<String, Object> recQuery = Maps.newHashMap();
            recQuery.put("applicationID", weixinDetail.getApplicationID());
            recQuery.put("recommendCode", recommendCode);
            recQuery.put("isValid", 1);

            Member recMember = memberService.getByMap(recQuery);
            if (recMember != null) {
                recommendMemberID = recMember.getId();
            }
        }

        // 第四步：处理 weixinMember
        Map<String, Object> wmQuery = Maps.newHashMap();
        wmQuery.put("openid", openid);
        ApiWeixinMemberDetail wmDetail = weixinMemberService.getByMapopen(wmQuery);

        if (wmDetail == null) {
            // 创建 weixinMember
            Map<String, Object> newWM = Maps.newHashMap();
            newWM.put("id", IdGenerator.uuid32());
            newWM.put("applicationID", weixinDetail.getApplicationID());
            newWM.put("siteID", weixinDetail.getSiteID());
            newWM.put("weixinID", weixinDetail.getWeixinID());
            newWM.put("companyID", weixinDetail.getCompanyID());
            newWM.put("name", weixinDetail.getName());
            newWM.put("openid", openid);
            newWM.put("weixinUnionID", unionid);
            newWM.put("subscribeTime", TimeTest.getTimeStr());
            newWM.put("deviceID", deviceID);
            newWM.put("ipAddr", ipAddr);
            newWM.put("lastComeTime", TimeTest.getTimeStr());
            newWM.put("memberID", memberID);
            newWM.put("language", code);


            // === 新增：将推荐数据保存到 weixinMember ===
            if (StringUtils.isNotBlank(recommendCode)) {
                newWM.put("recommendCode", recommendCode);
                if (StringUtils.isNotBlank(recommendMemberID)) {
                    newWM.put("recommendMemberID", recommendMemberID);
                }
            }

            weixinMemberService.insertByMap(newWM);



        } else {
            // 更新 weixinMember
            Map<String, Object> upWM = Maps.newHashMap();
            upWM.put("id", wmDetail.getWeixinMemberID());
            upWM.put("lastComeTime", TimeTest.getTimeStr());
            upWM.put("ipAddr", ipAddr);

            if (StringUtils.isBlank(wmDetail.getMemberID()) && StringUtils.isNotBlank(memberID)) {
                upWM.put("memberID", memberID);
            }

            // === 修改点：增加“如果原本没有推荐人，才进行绑定”的判断 ===
            // 只有当数据库里现有的 recommendCode 为空时，才接受传入的新 recommendCode
            if (StringUtils.isBlank(wmDetail.getRecommendCode()) && StringUtils.isNotBlank(recommendCode)) {
                upWM.put("recommendCode", recommendCode);
                if (StringUtils.isNotBlank(recommendMemberID)) {
                    upWM.put("recommendMemberID", recommendMemberID);
                }
            }

            weixinMemberService.updateByMap(upWM);
        }

        // 第六步：处理 memberDevice
        if (StringUtils.isNotBlank(memberID) && StringUtils.isNotBlank(deviceID)) {

            Map<String, Object> mdQuery = Maps.newHashMap();
            mdQuery.put("memberID", memberID);
            mdQuery.put("deviceID", deviceID);

            // 获取列表取第一个作为当前记录
            List<ApiMemberDeviceList> mdList = memberDeviceService.getDeviceIDByMemberID(mdQuery);

            if (mdList == null || mdList.isEmpty()) {
                // 创建 memberDevice
                Map<String, Object> newMD = Maps.newHashMap();
                newMD.put("id", IdGenerator.uuid32());
                newMD.put("memberID", memberID);
                newMD.put("deviceID", deviceID);
                newMD.put("applicationID", weixinDetail.getApplicationID());
                newMD.put("name", "_QWOIDBC100");
                newMD.put("orderSeq", 1);
                newMD.put("openid", openid);
                newMD.put("weixinUnionID", unionid);
                newMD.put("lastUseTime", null);
                newMD.put("modifiedTime", null);
                newMD.put("ipAddr", ipAddr);
                memberDeviceService.insertByMap(newMD);
            } else {
                // 修改已存在的记录
                ApiMemberDeviceList existMD = mdList.get(0);
                Map<String, Object> upMD = Maps.newHashMap();
                upMD.put("id", existMD.getMemberDeviceID());
                upMD.put("lastUseTime", TimeTest.getTimeStr());
                upMD.put("ipAddr", ipAddr);
                // 如果 openID 为空则更新

                upMD.put("name", "_QWOIDBC200");
                int currentSeq = (existMD.getOrderSeq() == null) ? 0 : existMD.getOrderSeq();
                upMD.put("orderSeq", currentSeq + 1);
                if (StringUtils.isBlank(existMD.getOpenid())) {
                    upMD.put("openid", openid);
                }
                // 如果 WeixinUnionID 为空且返回不为空则更新
                if (StringUtils.isBlank(existMD.getWeixinUnionID()) && StringUtils.isNotBlank(unionid)) {
                    upMD.put("weixinUnionID", unionid);
                }
                memberDeviceService.updateByMap(upMD);
            }
        }

        // 返回结果
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("access_token", access_token);
        rsMap.put("expires_in", expires_in);
        rsMap.put("refresh_token", refresh_token);
        rsMap.put("openid", openid);
        rsMap.put("unionid", unionid);
        rsMap.put("scope", scope);

        return MessagePacket.newSuccess(rsMap, "queryWeixinOpenIDByCode success");
    }

    @ApiOperation(value = "SJMemberBindPhone", notes = "独立接口:设计得到会员绑定手机号")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/SJMemberBindPhone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket SJMemberBindPhone(HttpServletRequest request, ModelMap map) {
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "手机号不能为空");
        }
        String verifyCode = request.getParameter("verifyCode");
        if (StringUtils.isBlank(verifyCode)) {
            return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotNull, "验证码不能为空");
        }
        String oldVerifyCode = stringRedisTemplate.opsForValue().get(String.format("%s_%s", CacheContant.BING_PHONE_AUTH_CODE, phone));
        if (StringUtils.isNotBlank(verifyCode)) {
            if (StringUtils.isBlank(oldVerifyCode) || !oldVerifyCode.equals(verifyCode)) {
                return MessagePacket.newFail(MessageHeader.Code.verifyCodeNotFound, "验证码不正确");
            }
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }
        map.put("id", memberID);
        map.put("phone", phone);
        map.put("loginName", phone);
        memberService.updateMember(map);
        redisTemplate.delete(String.format("%s_%s", CacheContant.BING_PHONE_AUTH_CODE, phone));
        String description = "设计得到会员绑定手机号";
        sendMessageService.sendMemberLogMessage(null, description, request,
                Memberlog.MemberOperateType.SJMemberBindPhone);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("bindTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "SJMemberBindPhone success");
    }

    @ApiOperation(value = "updateMyMemberAliPay", notes = "独立接口：设置我的会员支付宝信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberAliPay", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberAliPay(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("memberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String zhifubaoID = request.getParameter("zhifubaoID");
        if (StringUtils.isEmpty(zhifubaoID)) {
            return MessagePacket.newFail(MessageHeader.Code.zhifubaoIDNotNull, "zhifubaoID不能为空！");
        }
        String zhifubaoName = request.getParameter("zhifubaoName");
        if (StringUtils.isEmpty(zhifubaoName)) {
            return MessagePacket.newFail(MessageHeader.Code.zhifubaoNameNotNull, "zhifubaoName不能为空！");
        }
        String modifierId = null;
        if (member != null) {
            modifierId = member.getId();
        }
        map.put("modifier", modifierId);
        map.put("zhifubaoID", zhifubaoID);
        map.put("zhifubaoName", zhifubaoName);
        memberService.updateMyMemberAliPay(map);
        if (memberLogDTO != null) {
            String description = String.format("设置我的会员支付宝信息");
            sendMessageService.sendMemberLogMessage(sessionID, description, request, Memberlog.MemberOperateType.updateMyMemberAliPay);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberAliPay success");
    }

    @ApiOperation(value = "cancelBindThird", notes = "独立接口:第三方账号取消绑定")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/cancelBindThird", produces = {"application/json;charset=UTF-8"})
    public MessagePacket cancelBindThird(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member = memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String modifierId = null;
        if (member != null) {
            modifierId = member.getId();
        }
        map.put("modifier", modifierId);
        map.put("id", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        // 检查会员是否拥有支付密码，如果没有就直接取消绑定
        Member cancelMember = memberService.selectById(member.getId());
        String password = cancelMember.getTradePassword();
        String tradePassword = request.getParameter("tradePassword");
        if (StringUtils.isNotBlank(password)) {
            if (StringUtils.isBlank(tradePassword)) {
                return MessagePacket.newFail(MessageHeader.Code.tradePasswordIsNull, "交易密码不能为空");
            }
            if (tradePassword.length() > 6) {
                if (!tradePassword.equals(password)) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不正确");
                }
            } else {
                if (!MD5.encodeMd5(String.format("%s%s", tradePassword, Config.ENCODE_KEY)).equals(password)) {
                    return MessagePacket.newFail(MessageHeader.Code.passwordNotFound, "原密码不正确");
                }
            }
        }
        // 需要取消绑定的第三方账号 1=微信 2=支付宝 3=微博 4=飞书 5=钉钉 6=淘宝 7=京东 8=企业微信 9=qq
        String cancelType = request.getParameter("cancelType");
        if (StringUtils.isBlank(cancelType)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "第三方类型不能为空");
        }
        if ("1".equals(cancelType)) {
            map.put("weixin", "null");
            map.put("weixinToken", "null");
            map.put("weixinCode", "null");
            map.put("weixinUnionID", "null");
        } else if ("2".equals(cancelType)) {
            map.put("zhifubaoID", "null");
            map.put("zhifubaoName", "null");
        } else if ("3".equals(cancelType)) {
            map.put("weibo", "null");
            map.put("weiboToken", "null");
            map.put("weiboAccessToken", "null");
            map.put("weiboTokenExpires", "null");
            map.put("weiboID", "null");
        } else if ("4".equals(cancelType)) {
            map.put("feishuID", "null");
            map.put("feishuName", "null");
        } else if ("5".equals(cancelType)) {
            map.put("dingdingID", "null");
            map.put("dingdingName", "null");
        } else if ("6".equals(cancelType)) {
            map.put("taobaoID", "null");
            map.put("taobaoName", "null");
        } else if ("7".equals(cancelType)) {
            map.put("jingdongID", "null");
            map.put("jingdongName", "null");
        } else if ("8".equals(cancelType)) {
            map.put("qiyeweixinID", "null");
            map.put("qiyeweixinName", "null");
        } else if ("9".equals(cancelType)) {
            map.put("qqID", "null");
            map.put("qqName", "null");
        }
        memberService.cancelBindThird(map);
        String description = "第三方账号取消绑定";
        sendMessageService.sendMemberLogMessage(sessionID, description, request,
                Memberlog.MemberOperateType.cancelBindThird);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("cancelTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "cancelBindThird success");
    }

    @ApiOperation(value = "kpbaseMemberFastLogin", notes = "独立接口:haoju后台会员快速登录")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/kpbaseMemberFastLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket kpbaseMemberFastLogin(HttpServletRequest request, ModelMap map) {
        String sessionID = "";
        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }
        Member member = memberService.selectById(memberID);
        if (StringUtils.isNotBlank(member.getId())) {
            deviceService.updateLastLoadTime(deviceID);
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);
            String description = "会员:" + member.getName() + "haoju快速登录";
            MemberLogDTO memberLogDTO = new MemberLogDTO(member.getSiteID(), member.getApplicationID(),
                    deviceID, member.getId(), memberService.getLocationID(ipaddr), member.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), member, 86400, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO, 86400, TimeUnit.SECONDS);
            sendMessageService.sendMemberLoginMessage(false, session.getId(), "", ipaddr, description,
                    Memberlog.MemberOperateType.weiXinLogin);
            sessionID = session.getId();
        }
        String description = "haoju后台会员快速登录";
        sendMessageService.sendMemberLogMessage(sessionID, description, request,
                Memberlog.MemberOperateType.kpbaseMemberFastLogin);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", sessionID);
        return MessagePacket.newSuccess(rsMap, "kpbaseMemberFastLogin success");
    }

    @ApiOperation(value = "outSystemGetMemberUserList", notes = "独立接口：外部系统获取用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetMemberUserList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberUserList(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user = userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String recommendID = request.getParameter("recommendID");
        if (StringUtils.isNotBlank(recommendID)) {
            if (!memberService.checkMemberId(recommendID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendID不正确");
            }
            map.put("recommendID", recommendID);
        }
        String companyID = request.getParameter("companyID");
        if (StringUtils.isNotBlank(companyID)) {
            map.put("companyID", companyID);
        }
        String beginTime = request.getParameter("beginTime");
        if (StringUtils.isNotBlank(beginTime)) {
            map.put("beginTime", beginTime);
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }
        String endTime = request.getParameter("endTime");
        if (StringUtils.isNotBlank(endTime)) {
            map.put("endTime", endTime);
        }
        String keyWords = request.getParameter("keyWords");
        if (StringUtils.isNotBlank(keyWords)) {
            map.put("keyWords", "%" + keyWords + "%");
        }
        String sortTypeTime = request.getParameter("sortTypeTime");
        if (StringUtils.isNotBlank(sortTypeTime)) {
            if (!NumberUtils.isNumeric(sortTypeTime)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入1到2的数字");
            }
            if (!sortTypeTime.equals("1") && !sortTypeTime.equals("2")) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeTime请输入1到2的数字");
            }
            map.put("sortTypeTime", sortTypeTime);
        }
        String sortTypeName = request.getParameter("sortTypeName");
        if (StringUtils.isNotBlank(sortTypeName)) {
            if (!NumberUtils.isNumeric(sortTypeName)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入1到2的数字");
            }
            if (!sortTypeName.equals("1") && !sortTypeName.equals("2")) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "sortTypeName请输入1到2的数字");
            }
            map.put("sortTypeName", sortTypeName);
        }
        Page page = HttpServletHelper.assembyPage(request);
        //构造分页
        PageHelper.startPage(page.getStart(), page.getLimit());
        List<MemberList> list = memberService.outSystemGetMemberUserList(map);
        if (!list.isEmpty()) {
            for (MemberList list1 : list) {
                String memberID = list1.getMemberID();
                List<MajorList> roleTypeList = memberService.selectMemberMajor(memberID);
                List doubleList = new ArrayList();
                for (MajorList str : roleTypeList) {
                    if (!doubleList.toString().contains(str.toString())) {
                        doubleList.add(str);
                    }
                }
                list1.setRoleTypeList(doubleList);
            }
            PageInfo<MemberList> pageInfo = new PageInfo(list);
            page.setTotalSize((int) pageInfo.getTotal());
        }
        if (user != null) {
            String descriptions = String.format("外部系统获取员工表列表");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberUserList",
                    DataLog.objectDefineID.member.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        }
        MessagePage messagePage = new MessagePage(page, list);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", messagePage);
        PageHelper.clearPage();
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberUserList success");
    }

    @ApiOperation(value = "outSystemGetOneMemberUserDetail", notes = "独立接口：外部系统获取一个用户详情")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetOneMemberUserDetail", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetOneMemberUserDetail(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user = userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        map.put("memberID", memberID);
        MemberSJDto data = memberService.outSystemGetOneMemberUserDetail(map);
        if (user != null) {
            String descriptions = String.format("外部系统获取一个员工详情");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetOneMemberUserDetail",
                    DataLog.objectDefineID.member.getOname(), memberID,
                    data.getName(), DataLog.OperateType.DETAIL, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", data);
        return MessagePacket.newSuccess(rsMap, "outSystemGetOneMemberUserDetail success");
    }

    @ApiOperation(value = "outSystemGetMemberRecommendList", notes = "独立接口：外部系统获取会员推荐路径和团队")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/outSystemGetMemberRecommendList", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberRecommendList(HttpServletRequest request) {
        String outToken = request.getParameter("outToken");
        User user = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//        user = userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String memberID = request.getParameter("memberID");
        if (StringUtils.isEmpty(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID不正确");
        }
        //获取到会员详情
        OutMemberList memberDetail = memberService.outSystemGetMemberDetail(memberID);
        List<MemberRecommendDto> data = new ArrayList<>();
        MemberRecommendDto memberRecommendDto = null;
        Map<String, Object> map = new HashMap<>();
        if (memberDetail != null) {
            String recommendChain = memberDetail.getRecommendChain();
            if (recommendChain != null) {
                boolean flag = recommendChain.contains(",");
                if (flag) {//如果包含 说明有多个推荐链
                    String[] recommendCodes = recommendChain.split(",");
                    if (recommendCodes.length > 0 && recommendCodes != null) {
                        for (int i = 0; i < recommendCodes.length; i++) {
                            map.put("recommendCode", recommendCodes[i]);
                            memberRecommendDto = memberService.outSystemGetRecommendDetail(map);
                            data.add(memberRecommendDto);
                        }
                    }
                } else {//只有一个推荐人
                    map.put("recommendCode", recommendChain);
                    memberRecommendDto = memberService.outSystemGetRecommendDetail(map);
                    data.add(memberRecommendDto);
                }
            } else {
                data.add(memberRecommendDto);//null
            }
        }
        if (user != null) {
            String descriptions = String.format("外部系统获取会员推荐路径和团队");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberRecommendList",
                    DataLog.objectDefineID.member.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", data);
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberRecommendList success");
    }

    @ApiOperation(value = "outSystemQueryLoginNameIsMember", notes = "独立接口：外部系统查询登录名称是否存在会员")
    @RequestMapping(value = "/outSystemQueryLoginNameIsMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemQueryLoginNameIsMember(HttpServletRequest request, ModelMap map) {
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
        String loginName = request.getParameter("loginName");
        if (StringUtils.isBlank(loginName)) {
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotNull, "loginName不能为空");
        }
        List<ApiMemberListDto> list = memberService.getListByMap(new HashMap<String, Object>() {
            {
                put("loginName", loginName);
            }
        });
        Map<String, Object> rsMap = Maps.newHashMap();
        if (!list.isEmpty()) {
            rsMap.put("data", list);
        } else {
            rsMap.put("data", "未找到合适的会员");
        }
        if (user != null) {
            String description1 = String.format("外部系统查询登录名称是否存在会员");
            sendMessageService.sendDataLogMessage(user, description1, "outSystemQueryLoginNameIsMember",
                    null, null, null, DataLog.OperateType.ADD, request);
        }
        return MessagePacket.newSuccess(rsMap, "outSystemQueryLoginNameIsMember success");
    }

    @ApiOperation(value = "outSystemQueryPhoneIsMember", notes = "独立接口：外部系统查询一个手机号是否存在会员")
    @RequestMapping(value = "/outSystemQueryPhoneIsMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemQueryPhoneIsMember(HttpServletRequest request, ModelMap map) {
//        String outToken = request.getParameter("outToken");
//        User user = null;
//        if (StringUtils.isEmpty(outToken)) {
//            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//        }
//        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
////        user=userService.selectUserByOutToken(outToken);
//        if (user == null) {
//            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//        }
//        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
//        if (opLog == null) {
//            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
//        }
        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isNotBlank(applicationID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("application", applicationID))) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            map.put("applicationID", applicationID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID不能为空");
        }
        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }
        map.put("phone", phone);
        Member member = memberService.getByMap(map);
//        if (list.isEmpty()) {
//            return MessagePacket.newFail(MessageHeader.Code.noPhoneNumberFound, "该手机未找到合适的会员");
//        }  // 不能出现 -1，查不到就是查不到直接返回即可
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", member);
        return MessagePacket.newSuccess(rsMap, "outSystemQueryPhoneIsMember success");
    }

    /**
     * 这套代码弃用,这套代码弃用,这套代码弃用,这套代码弃用,这套代码弃用,
     *
     * @param request
     * @param map
     * @return
     */
    @ApiOperation(value = "outSystemCreateMember", notes = "独立接口：外部系统创建一个会员")
    @RequestMapping(value = "/outSystemCreateMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemCreateMember(HttpServletRequest request, ModelMap map) {
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
        map.put("creator", user.getId());
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            map.put("bizBlock", bizBlock);
        } else {
            map.put("bizBlock", "0");
        }

        String name = request.getParameter("name");
        if (StringUtils.isBlank(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }
        map.put("name", name);

        String shortName = request.getParameter("shortName");
        if (StringUtils.isBlank(shortName)) {
            return MessagePacket.newFail(MessageHeader.Code.shortNameNotNull, "shortName不能为空");
        }
        map.put("shortName", shortName);

        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (!siteService.checkIdIsValid(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        map.put("siteID", siteID);

        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }

        String description = request.getParameter("description");
        if (StringUtils.isNotBlank(description)) {
            map.put("description", description);
        }

        String avatarURL = request.getParameter("avatarURL");
        if (StringUtils.isNotBlank(avatarURL)) {
            map.put("avatarURL", avatarURL);
        }

        String email = request.getParameter("email");
        if (StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }

        String birthday = request.getParameter("birthday");
        if (StringUtils.isNotBlank(birthday)) {
            map.put("birthday", birthday);
        }

        String companyName = request.getParameter("companyName");
        if (StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }

        String loginPassword = request.getParameter("loginPassword");
        if (StringUtils.isNotBlank(loginPassword)) {
            map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", loginPassword, Config.ENCODE_KEY)));
        } else {
            map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", "111111", Config.ENCODE_KEY)));
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isBlank(loginName)) {
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotNull, "loginName不能为空");
        }
        List<ApiMemberListDto> loginNameList = memberService.getListByMap(new HashMap<String, Object>() {
            {
                put("loginName", loginName);
            }
        });
        if (!loginNameList.isEmpty()) {
            return MessagePacket.newFail(MessageHeader.Code.loginNameISFound, "loginName已存在");
        } else {
            map.put("loginName", loginName);
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isBlank(phone)) {
            return MessagePacket.newFail(MessageHeader.Code.phoneNotNull, "phone不能为空");
        }
        List<ApiMemberListDto> phoneList = memberService.getListByMap(new HashMap<String, Object>() {
            {
                put("phone", phone);
            }
        });
        if (!phoneList.isEmpty()) {
            return MessagePacket.newFail(MessageHeader.Code.phoneIsExist, "phone已存在");
        } else {
            map.put("phone", phone);
        }

        String jobYearCategoryID = request.getParameter("jobYearCategoryID");
        if (StringUtils.isNotBlank(jobYearCategoryID)) {
            if (!categoryService.checkCategoryId(jobYearCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobYearCategoryID不正确");
            }
            map.put("jobYearCategoryID", jobYearCategoryID);
        }

        String appleUserID = request.getParameter("appleUserID");
        if (StringUtils.isNotBlank(appleUserID)) {
            map.put("appleUserID", appleUserID);
        }

        String memberID = IdGenerator.uuid32();
        Site site = siteService.selectSiteById(siteID);
        map.put("id", memberID);
        map.put("roleType", 0);
        map.put("levelNumber", 1);
        map.put("createType", 1);
        map.put("companyID", site.getCompanyID());
        map.put("applicationID", site.getApplicationID());
        map.put("qiyeweixinID", sequenceDefineService.genCode("qiyeweixinID", memberID));
        String code = memberService.getCurRecommandCode(site.getApplicationID());
        if (StringUtils.isNotEmpty(code)) {
            map.put("recommendCode", strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
        } else {
            map.put("recommendCode", "00001");
        }
        memberService.insertByMap(map);

        if (user != null) {
            String description1 = String.format("外部系统创建一个会员");
            sendMessageService.sendDataLogMessage(user, description1, "outSystemCreateMember",
                    null, null, null, DataLog.OperateType.ADD, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", memberID);
        return MessagePacket.newSuccess(rsMap, "outSystemCreateMember success");
    }

    /**
     * 这套代码弃用,这套代码弃用,这套代码弃用,这套代码弃用,这套代码弃用,
     *
     * @param request
     * @param map
     * @return
     */
    @ApiOperation(value = "outSystemUpdateMember", notes = "独立接口：外部系统修改一个会员")
    @RequestMapping(value = "/outSystemUpdateMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemUpdateMember(HttpServletRequest request, ModelMap map) {
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
        map.put("modifier", user.getId());
        OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }
        map.put("id", memberID);

        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            map.put("bizBlock", bizBlock);
        }

        String name = request.getParameter("name");
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
        }

        String shortName = request.getParameter("shortName");
        if (StringUtils.isNotBlank(shortName)) {
            map.put("shortName", shortName);
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!siteService.checkIdIsValid(siteID)) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
            }
            map.put("siteID", siteID);
        }

        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }

        String description = request.getParameter("description");
        if (StringUtils.isNotBlank(description)) {
            map.put("description", description);
        }

        String avatarURL = request.getParameter("avatarURL");
        if (StringUtils.isNotBlank(avatarURL)) {
            map.put("avatarURL", avatarURL);
        }

        String email = request.getParameter("email");
        if (StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }

        String birthday = request.getParameter("birthday");
        if (StringUtils.isNotBlank(birthday)) {
            map.put("birthday", birthday);
        }

        String companyName = request.getParameter("companyName");
        if (StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }

        String loginPassword = request.getParameter("loginPassword");
        if (StringUtils.isNotBlank(loginPassword)) {
            map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", loginPassword, Config.ENCODE_KEY)));
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isNotBlank(loginName)) {
            List<ApiMemberListDto> loginNameList = memberService.getListByMap(new HashMap<String, Object>() {
                {
                    put("loginName", loginName);
                }
            });
            if (!loginNameList.isEmpty()) {
                return MessagePacket.newFail(MessageHeader.Code.loginNameISFound, "loginName已存在");
            } else {
                map.put("loginName", loginName);
            }
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            List<ApiMemberListDto> phoneList = memberService.getListByMap(new HashMap<String, Object>() {
                {
                    put("phone", phone);
                }
            });
            if (!phoneList.isEmpty()) {
                return MessagePacket.newFail(MessageHeader.Code.phoneIsExist, "phone已存在");
            } else {
                map.put("phone", phone);
            }
        }

        String jobYearCategoryID = request.getParameter("jobYearCategoryID");
        if (StringUtils.isNotBlank(jobYearCategoryID)) {
            if (!categoryService.checkCategoryId(jobYearCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobYearCategoryID不正确");
            }
            map.put("jobYearCategoryID", jobYearCategoryID);
        }

        String appleUserID = request.getParameter("appleUserID");
        if (StringUtils.isNotBlank(appleUserID)) {
            map.put("appleUserID", appleUserID);
        }
        memberService.updateMember(map);

        if (user != null) {
            String description1 = String.format("外部系统修改一个会员");
            sendMessageService.sendDataLogMessage(user, description1, "outSystemUpdateMember",
                    null, null, null, DataLog.OperateType.UPDATE, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemUpdateMember success");
    }

    @ApiOperation(value = "createOneMemberLoginAuthCode", notes = "独立接口：获取一个新的会员登录临时凭证")
    @RequestMapping(value = "/createOneMemberLoginAuthCode", produces = {"application/json;charset=UTF-8"})
    public MessagePacket createOneMemberLoginAuthCode(HttpServletRequest request, ModelMap map) {
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID不能为空");
        }
        if (!siteService.checkIdIsValid(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        SiteDto site = siteService.getSiteDetail(siteID);

        Map<String, Object> requestMap = Maps.newHashMap();
        Map<String, Object> adressIp = kingBase.getAdressIp(request, site.getApplicationID(), null, null, requestMap);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotNull, "deviceID不能为空");
        }
        if (!deviceService.checkIdIsValid(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID不正确");
        }

        // 生成authCode
        //int randomNo = (int) (Math.random() * 1000000);
        SecureRandom secureRandom = new SecureRandom();
        // 生成 0 到 999999 之间的随机整数（包含 0 和 999999）
        int randomNo = secureRandom.nextInt(1000000);
        String encodePassword = MD5.encodeMd5(String.format("%s%s", siteID + deviceID + randomNo, Config.ENCODE_KEY));
        String authCode = encodePassword.substring(8, 24);

        // 保存进redis
        AuthCodeDto authCodeDto = new AuthCodeDto();
        authCodeDto.setStatus(1);
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.AuthCode_KEY.key,
                authCode), authCodeDto, 86400, TimeUnit.SECONDS);

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("authCode", authCode);
        return MessagePacket.newSuccess(rsMap, "createOneMemberLoginAuthCode success");
    }

    @ApiOperation(value = "setAuthCodeStatusToScan", notes = "独立接口：设置会员登录凭证扫描成功")
    @RequestMapping(value = "/setAuthCodeStatusToScan", produces = {"application/json;charset=UTF-8"})
    public MessagePacket setAuthCodeStatusToScan(HttpServletRequest request, ModelMap map) {
        String authCode = request.getParameter("authCode");
        if (StringUtils.isBlank(authCode)) {
            return MessagePacket.newFail(MessageHeader.Code.authCodeNotNull, "authCode不能为空");
        }
        AuthCodeDto authCodeDto = (AuthCodeDto) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.AuthCode_KEY.key, authCode));
        if (authCodeDto == null) {
            return MessagePacket.newFail(MessageHeader.Code.authCodeNotFound, "authCode不正确或已失效");
        }

        // 修改内容
        authCodeDto.setStatus(2);

        // 保存进redis
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.AuthCode_KEY.key,
                authCode), authCodeDto, redisTimeOut, TimeUnit.SECONDS);

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("status", authCodeDto.getStatus());
        return MessagePacket.newSuccess(rsMap, "setAuthCodeStatusToScan success");
    }

    @ApiOperation(value = "setAuthCodeLoginDone", notes = "独立接口：设置登录凭证登录完成")
    @RequestMapping(value = "/setAuthCodeLoginDone", produces = {"application/json;charset=UTF-8"})
    public MessagePacket setAuthCodeLoginDone(HttpServletRequest request, ModelMap map) {
        String authCode = request.getParameter("authCode");
        if (StringUtils.isBlank(authCode)) {
            return MessagePacket.newFail(MessageHeader.Code.authCodeNotNull, "authCode不能为空");
        }
        AuthCodeDto authCodeDto = (AuthCodeDto) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.AuthCode_KEY.key, authCode));
        if (authCodeDto == null) {
            return MessagePacket.newFail(MessageHeader.Code.authCodeNotFound, "authCode不正确或已失效");
        }

        String status = request.getParameter("status");
        if (StringUtils.isBlank(status)) {
            return MessagePacket.newFail(MessageHeader.Code.statusNotNull, "status不能为空");
        }
        if (!NumberUtils.isNumeric(status)) {
            return MessagePacket.newFail(MessageHeader.Code.numberNot, "status请输入数字");
        }

        String sessionID = request.getParameter("sessionID");
        if ("3".equals(status) && StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "sessionID不能为空");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            authCodeDto.setMemberID(memberID);
        }

        // 修改内容
        authCodeDto.setStatus(Integer.parseInt(status));
        authCodeDto.setSessionID(sessionID);

        // 保存进redis
        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.AuthCode_KEY.key,
                authCode), authCodeDto, redisTimeOut, TimeUnit.SECONDS);

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("status", authCodeDto.getStatus());
        return MessagePacket.newSuccess(rsMap, "setAuthCodeLoginDone success");
    }

    @ApiOperation(value = "getOneAuthCodeStatus", notes = "独立接口：查询一个登录凭证的状态")
    @RequestMapping(value = "/getOneAuthCodeStatus", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getOneAuthCodeStatus(HttpServletRequest request, ModelMap map) {
        String authCode = request.getParameter("authCode");
        if (StringUtils.isBlank(authCode)) {
            return MessagePacket.newFail(MessageHeader.Code.authCodeNotNull, "authCode不能为空");
        }
        AuthCodeDto authCodeDto = (AuthCodeDto) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.AuthCode_KEY.key, authCode));
        if (authCodeDto == null) {
            return MessagePacket.newFail(MessageHeader.Code.authCodeNotFound, "authCode不正确或已失效");
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("status", authCodeDto.getStatus());
        rsMap.put("memberID", authCodeDto.getMemberID());
        rsMap.put("sessionID", authCodeDto.getSessionID());
        return MessagePacket.newSuccess(rsMap, "getOneAuthCodeStatus success");
    }

    @ApiOperation(value = "checkSessionIsOK", notes = "独立接口：检查sessionID")
    @RequestMapping(value = "/checkSessionIsOK", produces = {"application/json;charset=UTF-8"})
    public MessagePacket checkSessionIsOK(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "sessionID不能为空");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        MemberLogin memberLogin = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (member != null) {
            boolean b = memberService.checkIdIsValid(member.getId());
            if (!b) {
                redisTemplate.delete(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
                redisTemplate.delete(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
                redisTemplate.delete(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
                map.put("isTimeOut", true);
                return MessagePacket.newSuccess(map, "未找到对应的会员");
            }
        }
        Boolean flag = false;
        if (member == null || memberLogin == null || memberLogDTO == null) {
            flag = true;
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("isTimeOut", flag);
        return MessagePacket.newSuccess(rsMap, "getOneAuthCodeStatus success");
    }

    @ApiOperation(value = "outSystemGetMemberSimpleInfo", notes = "独立接口：外部系统获取会员简化信息")
    @RequestMapping(value = "/outSystemGetMemberSimpleInfo", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberSimpleInfo(HttpServletRequest request, ModelMap map) {
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

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        if (!memberService.checkMemberId(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }

        // 获取redis
        MemberRedis memberRedis = (MemberRedis) redisTemplate.opsForHash().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key,
                DataLog.objectDefineID.member.getOname()), memberID);

        if (memberRedis == null) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "当前会员未存在redis中");
        }

        if (opLog != null) {
            String descriptions = String.format("外部系统获取会员简化信息");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemGetMemberSimpleInfo",
                    DataLog.objectDefineID.member.getOname(), memberRedis.getMemberID(),
                    memberRedis.getName(), DataLog.OperateType.DETAIL, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", memberRedis);
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberSimpleInfo success");
    }

    @ApiOperation(value = "memberSilenceUserLogin", notes = "独立接口：网站会员静默登录外部系统新后台")
    @RequestMapping(value = "/memberSilenceUserLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberSilenceUserLogin(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        map.put("memberID", member.getId());
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        Map<String, Object> adressIp = kingBase.getAdressIp(request, member.getApplicationID(), null, null, null);
        if (!adressIp.get("result").equals("通过")) {
            String result = adressIp.get("result").toString();
            return MessagePacket.newFail(MessageHeader.Code.loginNameNotFoundORpasswordNotFound, result);
        }

        String companyID = request.getParameter("companyID");
        if (StringUtils.isBlank(companyID)) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不能为空");
        }
        if (!companyService.checkIdIsValid(companyID)) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
        }
        map.put("companyID", companyID);

        map.put("sortTypeTime", 2);
        String userID = "";
        String outToken = "";
        String applicationID = "";
        Company company = companyService.selectById(companyID);
        if (company != null) {
            applicationID = company.getApplicationID();
        }
        List<UserDto> userList = userService.outSystemGetOneUserList(map);
        if (!userList.isEmpty()) {
            userID = userList.get(0).getUserID();
            User user = userService.selectById(userID);
            if (user != null) {
                // 登录
                HttpSession session = request.getSession();
                session.setMaxInactiveInterval(5);
                String description = String.format("网站会员静默登录外部系统新后台成功");
                OpLog opLog = new OpLog();
                opLog.setId(user.getId());
                opLog.setLoginName(user.getLoginName());
                redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.USER_KEY.key, session.getId()), user, 86400, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, session.getId()), opLog, 86400, TimeUnit.SECONDS);
                sendMessageService.sendDataLogMessage(user, description, "memberSilenceUserLogin",
                        DataLog.objectDefineID.user.getOname(), user.getId(), user.getUserName(), DataLog.OperateType.LOGIN, request);
                // 修改用户的最后登录时间
                map.put("id", user.getId());
                map.put("lastLoginTime", TimeTest.getTime());

                userService.outSystemUpdateOneUser(map);

                outToken = session.getId();
            } else {
                return MessagePacket.newFail(MessageHeader.Code.fail, "用户不存在");
            }
        } else {
            return MessagePacket.newFail(MessageHeader.Code.fail, "当前会员无对应用户");
        }

        if (memberLogDTO != null) {
            String description = String.format("网站会员静默登录外部系统新后台");
            sendMessageService.sendMemberLogMessage(sessionID, description, request,
                    Memberlog.MemberOperateType.memberSilenceUserLogin);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("outToken", outToken);
        rsMap.put("userID", userID);
        rsMap.put("applicationID", applicationID);
        return MessagePacket.newSuccess(rsMap, "memberSilenceUserLogin success");
    }

    @ApiOperation(value = "updateMyMemberDescription", notes = "独立接口:修改我的个人描述")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberDescription", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberDescription(HttpServletRequest request, ModelMap map) {
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

        String description = request.getParameter("description");
        if (StringUtils.isEmpty(description)) {
            return MessagePacket.newFail(MessageHeader.Code.descriptionNotNull, "description不能为空");
        }
        map.put("description", description);
        map.put("id", member.getId());
        memberService.updateMember(map);
        if (memberLogDTO != null) {
            String description1 = String.format("修改我的个人描述");
            sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.updateMyMemberDescription);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberDescription success");
    }


    @ApiOperation(value = "outSystemGetMemberCareer", notes = "独立接口：外部系统统计会员职业认证")
    @RequestMapping(value = "/outSystemGetMemberCareer", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemGetMemberCareer(HttpServletRequest request, ModelMap map) {
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

//        applyTotal ：提交总数， 判断 careerApplyTime不为空，isValid=1 ，isLock=0
//        applyTodayTotal ：提交总数， 判断 careerApplyTime=今天，isValid=1 ，isLock=0
//        applyWeekTotal ：提交总数， 判断 careerApplyTime=本周，isValid=1 ，isLock=0
//        verifyTotal ：认证总数，判断careerVerifyTime 是否为空，isValid=1 。isLock=0
//        verifyTodayTotal ：今天认证总数，判断careerVerifyTime =今天，isValid=1 。isLock=0
//        verifyWeekTotal ：今天认证总数，判断careerVerifyTime =今天，isValid=1 。isLock=0


        String applyTotal = memberService.getApplyTotal();
        String applyTodayTotal = memberService.getApplyTodayTotal();
        String applyWeekTotal = memberService.getApplyWeekTotal();
        String verifyTotal = memberService.getVerifyTotal();
        String verifyTodayTotal = memberService.getVerifyTodayTotal();
        String verifyWeekTotal = memberService.getVerifyWeekTotal();


        if (user != null) {
            String description = String.format("外部系统统计会员职业认证");
            sendMessageService.sendMemberLogMessage(outToken, description, request, Memberlog.MemberOperateType.outSystemGetMemberCareer);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("applyTotal", applyTotal);
        rsMap.put("applyTodayTotal", applyTodayTotal);
        rsMap.put("applyWeekTotal", applyWeekTotal);
        rsMap.put("verifyTotal", verifyTotal);
        rsMap.put("verifyTodayTotal", verifyTodayTotal);
        rsMap.put("verifyWeekTotal", verifyWeekTotal);
        return MessagePacket.newSuccess(rsMap, "outSystemGetMemberCareer success");
    }


    //详情
    @ApiOperation(value = "bandingMemberPeopleID", notes = "独立接口:会员绑定实名认证信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
            @ApiImplicitParam(paramType = "query", name = "withdrawDefineID", value = "提现定义ID", dataType = "String"),
    })
    @RequestMapping(value = "/bandingMemberPeopleID", produces = {"application/json;charset=UTF-8"})
    public MessagePacket bandingMemberPeopleID(HttpServletRequest request, ModelMap map, MemberSelect a) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isBlank(sessionID)) {
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


        if (StringUtils.isEmpty(a.getPeopleID())) {
            return MessagePacket.newFail(MessageHeader.Code.peopleIDNotNull, "peopleID 不能为空");
        }//13560 peopleID不能为空

        PeopleDetail peopleDetail = peopleService.SelectByID(a.getPeopleID());

        if (peopleDetail == null) {
            return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "peopleID 不真确");
        }//13559 peopleID不正确

        MemberDetail ms = memberService.getMemberDetail(member.getId());

        if (ms.getName() == null) {
            ms.setName(peopleDetail.getName());
        }
        if (ms.getShortName() == null) {
            ms.setShortName(peopleDetail.getShortName());
        }
        if (ms.getTitleID() == null) {
            ms.setTitleID(peopleDetail.getTitleID());
        }
        if (ms.getBirthday() == null) {
            ms.setBirthday(peopleDetail.getBirthday());
        }
        if (ms.getShengID() == null) {
            ms.setShengID(peopleDetail.getShengID());
        }
        if (ms.getShengName() == null) {
            ms.setShengName(peopleDetail.getShengName());
        }
        if (ms.getShiID() == null) {
            ms.setShiID(peopleDetail.getShiID());
        }
        if (ms.getShiName() == null) {
            ms.setShiName(peopleDetail.getShiName());
        }
        if (ms.getXianID() == null) {
            ms.setXianID(peopleDetail.getXianID());
        }
        if (ms.getXianName() == null) {
            ms.setXianName(peopleDetail.getXianName());
        }
        if (ms.getZhenID() == null) {
            ms.setZhenID(peopleDetail.getZhenID());
        }
        if (ms.getZhenName() == null) {
            ms.setZhenName(peopleDetail.getZhenName());
        }
        if (ms.getCunID() == null) {
            ms.setCunID(peopleDetail.getCunID());
        }
        if (ms.getCunName() == null) {
            ms.setCunName(peopleDetail.getCunName());
        }
        if (ms.getEducationCategoryID() == null) {
            ms.setEducationCategoryID(peopleDetail.getEducationCategoryID());
        }
        if (ms.getIndustryCategoryID() == null) {
            ms.setIndustryCategoryID(peopleDetail.getIndustryCategoryID());
        }
        ms.setModifier(member.getId());
        memberService.updateMemberDetail(ms);

        MemberListDto date = memberService.memberListDtoList(member.getId());
        if (memberLogDTO != null) {
            String description1 = String.format("会员绑定实名认证信息");
            sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.bandingMemberPeopleID);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", date);
        return MessagePacket.newSuccess(rsMap, "bandingMemberPeopleID success");
    }


    @ApiOperation(value = "updateMyMemberRoletype", notes = "独立接口：会员设置我的角色身份")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateMyMemberRoletype", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateMyMemberRoletype(HttpServletRequest request, ModelMap map) {
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

        String roleType = request.getParameter("roleType");
        if (StringUtils.isEmpty(roleType)) {
            return MessagePacket.newFail(MessageHeader.Code.roleTypeNotNull, "roleType不能为空");
        }
        map.put("roleType", roleType);
        map.put("id", member.getId());
        memberService.updateMember(map);
        if (memberLogDTO != null)
            sendMessageService.newSendMemberLogMessage(sessionID, "会员设置我的角色身份", request, NewMemberlog.MemberOperateType.updateMyMemberRoletype);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateMyMemberRoletype success");
    }


    @ApiOperation(value = "updateOtherMemberInfo", notes = "独立接口：设置好友个人信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateOtherMemberInfo", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateOtherMemberInfo(HttpServletRequest request, ModelMap map) {
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

        String memberID = request.getParameter("memberID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID 不正确");
            }
            //是否是好友
            Boolean friend = friendService.isFriend(memberID, member.getId());
            Boolean friend1 = friendService.isFriend(member.getId(),memberID);
            //是否是推荐人
            MemberDetail memberDetail = memberService.getMemberDetail(memberID);
            //是否是亲属
            boolean relationship = relationshipService.checkMemberIDAndToMemberID(memberID, member.getId());
            boolean relationship1 = relationshipService.checkMemberIDAndToMemberID(member.getId(),memberID);

            if (!friend&&!friend1&&!member.getId().equals(memberDetail.getRecommendID())&&!relationship&&!relationship1){
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "没有权限修改");
            }
            map.put("id", memberID);
        }
        else {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID 不能为空");
        }

        String name = request.getParameter("name");
        if (org.apache.commons.lang.StringUtils.isNotBlank(name)) {
            map.put("name", name);
        }
        String shortName = request.getParameter("shortName");
        if (org.apache.commons.lang.StringUtils.isNotBlank(shortName)) {
            map.put("shortName", shortName);
        }
        String phone = request.getParameter("phone");
        if (org.apache.commons.lang.StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }
        String birthday = request.getParameter("birthday");
        if (org.apache.commons.lang.StringUtils.isNotBlank(birthday)) {
            map.put("birthday", birthday);
        }
        String email = request.getParameter("email");
        if (org.apache.commons.lang.StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }
        memberService.updateMember(map);
        if (memberLogDTO != null)
            sendMessageService.newSendMemberLogMessage(sessionID, "独立接口：设置好友个人信息", request, NewMemberlog.MemberOperateType.updateMyMemberRoletype);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateOtherMemberInfo success");
    }


    @ApiOperation(value = "updateOtherMemberCareer", notes = "设置好友的职业标签（名片信息）")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/updateOtherMemberCareer", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateOtherMemberCareer(HttpServletRequest request, ModelMap map) {
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

        String memberID = request.getParameter("memberID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "memberID 不正确");
            }
            //是否是好友
            Boolean friend = friendService.isFriend(memberID, member.getId());
            Boolean friend1 = friendService.isFriend(member.getId(),memberID);
            //是否是推荐人
            MemberDetail memberDetail = memberService.getMemberDetail(memberID);
            //是否是亲属
            boolean relationship = relationshipService.checkMemberIDAndToMemberID(memberID, member.getId());
            boolean relationship1 = relationshipService.checkMemberIDAndToMemberID(member.getId(),memberID);

            if (!friend&&!friend1&&!member.getId().equals(memberDetail.getRecommendID())&&!relationship&&!relationship1){
                return MessagePacket.newFail(MessageHeader.Code.memberNotNull, "没有权限修改");
            }
            map.put("id", memberID);
        }
        else {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID 不能为空");
        }

        String newCareer = request.getParameter("newCareer");
        if (org.apache.commons.lang.StringUtils.isEmpty(newCareer)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "newCareer 不能为空");
        }
        map.put("newCareer", newCareer);


        String companyName = request.getParameter("companyName");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }
        String companyDepartment = request.getParameter("companyDepartment");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyDepartment)) {
            map.put("companyDepartment", companyDepartment);
        }
        String companyJob = request.getParameter("companyJob");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyJob)) {
            map.put("companyJob", companyJob);
        }
        String companyAddress = request.getParameter("companyAddress");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyAddress)) {
            map.put("companyAddress", companyAddress);
        }
        String companyTel = request.getParameter("companyTel");
        if (org.apache.commons.lang.StringUtils.isNotBlank(companyTel)) {
            map.put("companyTel", companyTel);
        }
        String industryCategoryID = request.getParameter("industryCategoryID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(industryCategoryID)) {
            map.put("industryCategoryID", industryCategoryID);
        }
        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(professionalCategoryID)) {
            map.put("professionalCategoryID", professionalCategoryID);
        }
        String jobCategoryID = request.getParameter("jobCategoryID");
        if (org.apache.commons.lang.StringUtils.isNotBlank(jobCategoryID)) {
            map.put("jobCategoryID", jobCategoryID);
        }
        String jobYear = request.getParameter("jobYear");
        if (org.apache.commons.lang.StringUtils.isNotBlank(jobYear)) {
            if (!NumberUtils.isNumeric(jobYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "jobYear请输入合法数字");
            }
            map.put("jobYear", jobYear);
        }

        String shortDescription = request.getParameter("shortDescription");
        if (org.apache.commons.lang.StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }
        String headName = request.getParameter("headName");
        if (org.apache.commons.lang.StringUtils.isNotBlank(headName)) {
            map.put("headName", headName);
        }
        map.put("careerApplyTime", TimeTest.getTimeStr());
        map.put("careerStatus", 1);
        map.put("modifier", member.getId());
        memberService.updateMember(map);
        if (memberLogDTO != null)
            sendMessageService.newSendMemberLogMessage(sessionID, "设置好友的职业标签（名片信息）份", request, NewMemberlog.MemberOperateType.updateMyMemberRoletype);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateOtherMemberCareer success");
    }

    @ApiOperation(value = "createMyBabyMember", notes = "创建我的胎儿")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/createMyBabyMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket createMyBabyMember(HttpServletRequest request, ModelMap map1) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isEmpty(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        Member member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
        //Member member =memberService.selectById(sessionID);
        if (member == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
        if (memberLogDTO == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }

        Member babyMember = new Member();

        String babyNumber = request.getParameter("babyNumber");// 胎儿数量，几个胎儿循环遍历几次
        if (StringUtils.isBlank(babyNumber)) {
            babyNumber = "1";
        }
        Integer num = Integer.valueOf(babyNumber);

        String birthday = request.getParameter("birthday");//生日
        if (StringUtils.isNotBlank(birthday)) {
            babyMember.setBirthday(TimeTest.strToDate(birthday));
        }

        String titleID = request.getParameter("titleID");//性别
        if (StringUtils.isNotBlank(titleID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("category", titleID))) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "titleID不正确");
            }
            babyMember.setTitleID(titleID);
            map1.put("titleID", titleID);
        }

        String preBirthday = request.getParameter("preBirthday");//预产期
        if (StringUtils.isNotBlank(preBirthday)) {
            babyMember.setPreBirthday(TimeTest.strToDate(preBirthday));
            babyMember.setPlanBirthday(TimeTest.strToDate(preBirthday));
        }

        Member m = new Member();
        m.setId(member.getId());
        m.setPlanBirthday(TimeTest.strToTimes(preBirthday));
        List<Member> memberList = memberService.selectByMemberListByIDAndPlanBirthday(m);//根据id和预产期获取会员列表

        String applicationID = member.getApplicationID();
//        String applicationID = request.getParameter("applicationID");// 应用ID
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            babyMember.setApplicationID(applicationID);
        } else {
            return MessagePacket.newFail(MessageHeader.Code.applicationIDNotNull, "applicationID不能为空");
        }

        Friend f = new Friend();
        f.setApplicationID(applicationID);
        f.setMemberID(member.getId());
        List<FriendDetail> friendDetails = friendService.selectList(f);//根据会员ID查找朋友列表

        Integer orderSeq = 1;

        String siteID = member.getSiteID();//站点ID
//        String siteID = request.getParameter("siteID");// 站点ID
        if (!siteService.checkIdIsValid(siteID)) {
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID不正确");
        }
        babyMember.setSiteID(siteID);

        String companyID = member.getCompanyID();
//        String companyID = request.getParameter("companyID");//必须，默认是当前应用的companyID，通常是医院的公司ID
        if (StringUtils.isEmpty(companyID)) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotNull, "companyID不能为空");
        }
        if (StringUtils.isBlank(companyService.getIsValidIdBy(companyID))) {
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID不正确");
        }
        babyMember.setCompanyID(companyID);

        babyMember.setRoleType(8);//身份类型

        babyMember.setBizBlock(0);//业务板块

        babyMember.setCreateType(1);//系统创建

        if (!memberList.isEmpty() & !friendDetails.isEmpty()) {
            for (FriendDetail friendDetail : friendDetails) {
                if (friendDetail.getOrderSeq() >= orderSeq) {
                    orderSeq = friendDetail.getOrderSeq();
                }
            }
            orderSeq += 1;
        }

        if (memberList.isEmpty() & !friendDetails.isEmpty()) {
            for (FriendDetail friendDetail : friendDetails) {
                if (friendDetail.getOrderSeq() >= orderSeq) {
                    orderSeq = friendDetail.getOrderSeq();
                }
            }
            orderSeq += 10;
        }


        String name = request.getParameter("name");//姓名，必须
        if (StringUtils.isEmpty(name)) {
            return MessagePacket.newFail(MessageHeader.Code.nameNotNull, "name不能为空");
        }
        babyMember.setName(member.getName() + name);

        babyMember.setShortName(name);//简称

        babyMember.setLoginPassword(MD5.encodeMd5("111111"));//登录密码

        String shengID = member.getShengID();
        babyMember.setShengID(shengID);

        String shengName = member.getShengName();
        babyMember.setShengName(shengName);

        String shiID = member.getShiID();
        babyMember.setShiID(shiID);

        String shiName = member.getShiName();
        babyMember.setShiName(shiName);

        String xianID = member.getXianID();
        babyMember.setXianID(xianID);

        String xianName = member.getXianName();
        babyMember.setXianName(xianName);

        String zhenID = member.getZhenID();
        babyMember.setZhenID(zhenID);

        String zhenName = member.getZhenName();
        babyMember.setZhenName(zhenName);

        babyMember.setLevelNumber(2);//绝对层级

        String code = memberService.getCurRecommandCode(applicationID);
        if (StringUtils.isNotEmpty(code)) {
            babyMember.setRecommandCode(strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
            babyMember.setRecommendCode(strFormat3(String.valueOf(Integer.valueOf(code) + 1)));
        } else {
            babyMember.setRecommandCode("00001");
            babyMember.setRecommendCode("00001");
        }// 推荐码

        String recommandID = member.getId();
        babyMember.setRecommandID(recommandID);//推荐人
        babyMember.setRecommendID(recommandID);//推荐人

        if (StringUtils.isNotBlank(member.getRecommandChain())) {
            babyMember.setRecommandChain(member.getRecommandChain() + "," + member.getRecommandCode());
            babyMember.setRecommendChain(member.getRecommandChain() + "," + member.getRecommandCode());
        } else {
            babyMember.setRecommandChain(member.getRecommandCode());
            babyMember.setRecommendChain(member.getRecommandCode());
        }//推荐链

        String familyType = request.getParameter("familyType");

        babyMember.setIsValid(1);//有效
        babyMember.setIsLock(0);//不加锁


        Friend friend = new Friend();

        friend.setApplicationID(applicationID);//应用
        friend.setName(member.getName() + name);//名称
        friend.setFriendType(2);//好友类型
        friend.setFriendTypeName("母子关系");//好友关系
        friend.setMemberID(member.getId());//会员ID
        friend.setApplyTime(new Date());//申请时间
        friend.setAnswerTime(new Date());//应答时间
        friend.setOrderSeq(orderSeq);
        friend.setIsAgree(1);//是否同意

        HashMap<String, Object> map = new HashMap<>();
        map.put("applicationID", applicationID);//应用
        map.put("name", member.getName() + name);//名称
        map.put("followType", 2);//关注类型
        map.put("memberID", member.getId());//会员ID
        map.put("applyTime", TimeTest.getTime());//申请时间
        map.put("answerTime", TimeTest.getTime());//应答时间
        map.put("isAgree", 1);

        map1.put("applicationID", applicationID);//应用
        map1.put("companyID", companyID);//公司
        map1.put("memberID", member.getId());//会员
        map1.put("relationType", 10);//关系人类型
        map1.put("Name", name);//名称
        map1.put("preBirthday", preBirthday);//预产日期
        map1.put("birthday", birthday);

        if (StringUtils.isBlank(familyType)) {
            List<Dictionary> familyTypeList = this.dictionaryService.selectByCode("familyTypecode");
            for (Dictionary dictionary : familyTypeList) {
                if ("母子".equals(dictionary.getName())) {
                    familyType = dictionary.getId();
                    break;
                }
            }
        }

        map1.put("familyType", familyType);
        map1.put("applyTime", TimeTest.getTime());//申请时间
        map1.put("answerTime", TimeTest.getTime());//应答时间
        map1.put("isAgree", 1);


        for (int i = 0; i < num; i++) {
            babyMember.setLoginName(member.getPhone() + orderSeq);//登录名
            String id = IdGenerator.uuid32();
            babyMember.setId(id);
            memberService.insertMember(babyMember);

            friend.setToMemberID(id);//好友
            friend.setOrderSeq(orderSeq);//序号
            friend.setFriendID(IdGenerator.uuid32());
            friendService.insert(friend);

            String memberFollowID = IdGenerator.uuid32();
            map.put("followID", id);
            map.put("id", memberFollowID);
            memberFollowService.insertMemberFollow(map);

            map1.put("conceptionSeq", orderSeq);//怀孕序号
            map1.put("toMemberID", id);
            map1.put("id", IdGenerator.uuid32());
            relationshipService.insert(map1);
            orderSeq = orderSeq + 1;

            // == 创建会员发送队列 ==
            // session检查
            String ipaddr = WebUtil.getRemortIP(request);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(5);

            Member mr = memberService.selectById(id);
            memberService.checkSession(null, session.getId(), id, siteID, mr.toString());

            String description2 = "会员:" + name + "注册成功";
            MemberLogDTO memberLogDTO1 = new MemberLogDTO(siteID, mr.getApplicationID(),
                    null, mr.getId(), memberService.getLocationID(ipaddr), mr.getCompanyID());
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, session.getId()), mr, redisTimeOut, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, session.getId()), memberLogDTO1, redisTimeOut, TimeUnit.SECONDS);

            sendMessageService.sendMemberLoginMessage(true, session.getId(), ipaddr, ipaddr, description2,
                    Memberlog.MemberOperateType.addSuccess);
            memberService.createMemberPrivilege(applicationID, mr.getId());
            // == 创建会员发送队列 ==
        }


        String description = String.format("%s创建我的胎儿", babyMember.getName());
        if (memberLogDTO != null)
            sendMessageService.newSendMemberLogMessage(sessionID, description, request, NewMemberlog.MemberOperateType.createMyBabyMember);
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("memberID", babyMember.getId());
        return MessagePacket.newSuccess(rsMap, "createMyBabyMember success");
    }

    @ApiOperation(value = "updateOneMember", notes = "独立接口：修改一个会员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sessionID", value = "登录标识", paramType = "query", dataType = "String"),
    })
    @RequestMapping(value = "/updateOneMember", produces = {"application/json;charset=UTF-8"})
    public MessagePacket updateOneMember(HttpServletRequest request, ModelMap map) throws ParseException {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotNull, "memberID不能为空");
        }
        Member member1 = memberService.selectById(memberID);
        if (member1 == null) {
            return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID不正确");
        }
        map.put("id", memberID);

        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
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
            Channel channel = channelService.selectById(channelID);
            if (channel == null) {
                return MessagePacket.newFail(MessageHeader.Code.channelIDNotFound, "channelID不正确");
            }
            map.put("channelID", channelID);
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

        String roleType = request.getParameter("roleType");
        if (StringUtils.isNotBlank(roleType)) {
            if (!NumberUtils.isNumeric(roleType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "roleType请输入数字");
            }
            map.put("roleType", roleType);
        }

        String bizBlock = request.getParameter("bizBlock");
        if (StringUtils.isNotBlank(bizBlock)) {
            if (!NumberUtils.isNumeric(bizBlock)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "bizBlock请输入数字");
            }
            map.put("bizBlock", bizBlock);
        }

        String peopleID = request.getParameter("peopleID");
        if (StringUtils.isNotBlank(peopleID)) {
            if (!peopleService.checkIdIsValid(peopleID)) {
                return MessagePacket.newFail(MessageHeader.Code.peopleIDNotFound, "peopleID不正确");
            }
            map.put("peopleID", peopleID);
        }

        String idType = request.getParameter("idType");
        if (StringUtils.isNotBlank(idType)) {
            if (!NumberUtils.isNumeric(idType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "idType请输入数字");
            }
            map.put("idType", idType);
        }

        String idNumber = request.getParameter("idNumber");
        if (StringUtils.isNotBlank(idNumber)) {
            map.put("idNumber", idNumber);
        }

        String name = request.getParameter("name");
        if (StringUtils.isNotBlank(name)) {
            map.put("name", name);
            if(member!=null)
            member.setName(name);
        }

        String shortName = request.getParameter("shortName");
        if (StringUtils.isNotBlank(shortName)) {
            map.put("shortName", shortName);
        }

        String shortDescription = request.getParameter("shortDescription");
        if (StringUtils.isNotBlank(shortDescription)) {
            map.put("shortDescription", shortDescription);
        }

        String description = request.getParameter("description");
        if (StringUtils.isNotBlank(description)) {
            map.put("description", description);
        }

        String rankID = request.getParameter("rankID");
        if (StringUtils.isNotBlank(rankID)) {
            if (!rankService.checkIdIsValid(rankID)) {
                return MessagePacket.newFail(MessageHeader.Code.rankIDNotFound, "rankID不正确");
            }
            map.put("rankID", rankID);
        }

        String majorID = request.getParameter("majorID");
        if (StringUtils.isNotBlank(majorID)) {
            if (!majorService.checkIdIsValid(majorID)) {
                return MessagePacket.newFail(MessageHeader.Code.majorIsNull, "majorID不正确");
            }
            map.put("majorID", majorID);
        }

        String creditRankID = request.getParameter("creditRankID");
        if (StringUtils.isNotBlank(creditRankID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("creditRank", creditRankID))) {
                return MessagePacket.newFail(MessageHeader.Code.creditRankIDNotFound, "creditRankID不正确");
            }
            map.put("creditRankID", creditRankID);
        }

        String loginName = request.getParameter("loginName");
        if (StringUtils.isNotBlank(loginName)) {
            map.put("loginName", loginName);
        }

        String loginPassword = request.getParameter("loginPassword");
        if (StringUtils.isNotBlank(loginPassword)) {
            if (loginPassword.length() >= 32) {
                map.put("loginPassword", loginPassword);
            } else {
                map.put("loginPassword", MD5.encodeMd5(String.format("%s%s", loginPassword, Config.ENCODE_KEY)));
            }
        }

        String password = request.getParameter("password");
        if (StringUtils.isNotBlank(password)) {
            map.put("password", password);
        }

        String tradePassword = request.getParameter("tradePassword");
        if (StringUtils.isNotBlank(tradePassword)) {
            if (tradePassword.length() >= 32) {
                map.put("tradePassword", tradePassword);
            } else {
                String psd = MD5.encodeMd5(String.format("%s%s", tradePassword, Config.ENCODE_KEY));
                map.put("tradePassword", psd);
            }
        }

        String avatarURL = request.getParameter("avatarURL");
        if (StringUtils.isNotBlank(avatarURL)) {
            map.put("avatarURL", avatarURL);
        }

        String avatarKey = request.getParameter("avatarKey");
        if (StringUtils.isNotBlank(avatarKey)) {
            map.put("avatarKey", avatarKey);
        }

        String avatarSmall = request.getParameter("avatarSmall");
        if (StringUtils.isNotBlank(avatarSmall)) {
            map.put("avatarSmall", avatarSmall);
        }

        String avatarMiddle = request.getParameter("avatarMiddle");
        if (StringUtils.isNotBlank(avatarMiddle)) {
            map.put("avatarMiddle", avatarMiddle);
        }

        String avatarLarge = request.getParameter("avatarLarge");
        if (StringUtils.isNotBlank(avatarLarge)) {
            map.put("avatarLarge", avatarLarge);
        }

        String avatarBack = request.getParameter("avatarBack");
        if (StringUtils.isNotBlank(avatarBack)) {
            map.put("avatarBack", avatarBack);
        }

        String titleID = request.getParameter("titleID");
        if (StringUtils.isNotBlank(titleID)) {
            if (!categoryService.checkCategoryId(titleID)) {
                return MessagePacket.newFail(MessageHeader.Code.titleIDError, "titleID不正确");
            }
            map.put("titleID", titleID);
        }

        String phone = request.getParameter("phone");
        if (StringUtils.isNotBlank(phone)) {
            map.put("phone", phone);
        }

        String email = request.getParameter("email");
        if (StringUtils.isNotBlank(email)) {
            map.put("email", email);
        }

        String birthday = request.getParameter("birthday");
        if (StringUtils.isNotBlank(birthday)) {
            map.put("birthday", birthday);
        }

        String ageCategoryID = request.getParameter("ageCategoryID");
        if (StringUtils.isNotBlank(ageCategoryID)) {
            if (!categoryService.checkCategoryId(ageCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "ageCategoryID不正确");
            }
            map.put("ageCategoryID", ageCategoryID);
        }

        String marriageType = request.getParameter("marriageType");
        if (StringUtils.isNotBlank(marriageType)) {
            if (!"1".equals(marriageType) && !"2".equals(marriageType) && !"3".equals(marriageType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "marriageType请输入数字");
            }
            map.put("marriageType", marriageType);
        }

        String marriageCategoryID = request.getParameter("marriageCategoryID");
        if (StringUtils.isNotBlank(marriageCategoryID)) {
            if (!categoryService.checkCategoryId(marriageCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "marriageCategoryID 不正确");
            }
            map.put("marriageCategoryID", marriageCategoryID);
        }

        String starDefineID = request.getParameter("starDefineID");
        if (StringUtils.isNotBlank(starDefineID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("starDefine", starDefineID))) {
                return MessagePacket.newFail(MessageHeader.Code.starDefineIDNotFound, "starDefineID不正确");
            }
            map.put("starDefineID", starDefineID);
        }

        String emotionDictionaryID = request.getParameter("emotionDictionaryID");
        if (StringUtils.isNotBlank(emotionDictionaryID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("dictionary", emotionDictionaryID))) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "emotionDictionaryID不正确");
            }
            map.put("emotionDictionaryID", emotionDictionaryID);
        }

        String bloodDIctionaryID = request.getParameter("bloodDIctionaryID");
        if (StringUtils.isNotBlank(bloodDIctionaryID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("dictionary", bloodDIctionaryID))) {
                return MessagePacket.newFail(MessageHeader.Code.dictionaryIDNotFound, "bloodDIctionaryID不正确");
            }
            map.put("bloodDIctionaryID", bloodDIctionaryID);
        }

        String diseaseCategoriesID = request.getParameter("diseaseCategoriesID");
        if (StringUtils.isNotBlank(diseaseCategoriesID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("category", diseaseCategoriesID))) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "diseaseCategoriesID不正确");
            }
            map.put("diseaseCategoriesID", diseaseCategoriesID);
        }

        String highth = request.getParameter("highth");
        if (StringUtils.isNotBlank(highth)) {
            map.put("highth", highth);
        }

        String weight = request.getParameter("weight");
        if (StringUtils.isNotBlank(weight)) {
            map.put("weight", weight);
        }

        String BMI = request.getParameter("BMI");
        if (StringUtils.isNotBlank(BMI)) {
            map.put("BMI", BMI);
        }
        String conceptionTimes = request.getParameter("conceptionTimes");
        if (StringUtils.isNotBlank(conceptionTimes)) {
            map.put("conceptionTimes", conceptionTimes);
        }
        String birthChildTimes = request.getParameter("birthChildTimes");
        if (StringUtils.isNotBlank(birthChildTimes)) {
            map.put("birthChildTimes", birthChildTimes);
        }

        String chest = request.getParameter("chest");
        if (StringUtils.isNotBlank(chest)) {
            map.put("chest", chest);
        }

        String shoulder = request.getParameter("shoulder");
        if (StringUtils.isNotBlank(shoulder)) {
            map.put("shoulder", shoulder);
        }

        String waist = request.getParameter("waist");
        if (StringUtils.isNotBlank(waist)) {
            map.put("waist", waist);
        }

        String hips = request.getParameter("hips");
        if (StringUtils.isNotBlank(hips)) {
            map.put("hips", hips);
        }

        String legs = request.getParameter("legs");
        if (StringUtils.isNotBlank(legs)) {
            map.put("legs", legs);
        }

        String skinCategoryID = request.getParameter("skinCategoryID");
        if (StringUtils.isNotBlank(skinCategoryID)) {
            if (!categoryService.checkCategoryId(skinCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "skinCategoryID不正确");
            }
            map.put("skinCategoryID", skinCategoryID);
        }

        String faceCategoryID = request.getParameter("faceCategoryID");
        if (StringUtils.isNotBlank(faceCategoryID)) {
            if (!categoryService.checkCategoryId(faceCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "faceCategoryID不正确");
            }
            map.put("faceCategoryID", faceCategoryID);
        }

        String fiveCategoryID = request.getParameter("fiveCategoryID");
        if (StringUtils.isNotBlank(fiveCategoryID)) {
            if (!categoryService.checkCategoryId(fiveCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "fiveCategoryID不正确");
            }
            map.put("fiveCategoryID", fiveCategoryID);
        }

        String lineCategoryID = request.getParameter("lineCategoryID");
        if (StringUtils.isNotBlank(lineCategoryID)) {
            if (!categoryService.checkCategoryId(lineCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "lineCategoryID不正确");
            }
            map.put("lineCategoryID", lineCategoryID);
        }

        String boneCategoryID = request.getParameter("boneCategoryID");
        if (StringUtils.isNotBlank(boneCategoryID)) {
            if (!categoryService.checkCategoryId(boneCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "boneCategoryID不正确");
            }
            map.put("boneCategoryID", boneCategoryID);
        }

        String bodyCategoryID = request.getParameter("bodyCategoryID");
        if (StringUtils.isNotBlank(bodyCategoryID)) {
            if (!categoryService.checkCategoryId(bodyCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "bodyCategoryID不正确");
            }
            map.put("bodyCategoryID", bodyCategoryID);
        }

        String parentGroupCategoryID = request.getParameter("parentGroupCategoryID");
        if (StringUtils.isNotBlank(parentGroupCategoryID)) {
            if (!categoryService.checkCategoryId(parentGroupCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "parentGroupCategoryID不正确");
            }
            map.put("parentGroupCategoryID", parentGroupCategoryID);
        }

        String nationID = request.getParameter("nationID");
        if (StringUtils.isNotBlank(nationID)) {
            City nation = cityService.selectById(nationID);
            if (nationID == null) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "nationID 不正确");
            }
            map.put("nationID", nation.getId());
            map.put("nationName", nation.getName());
        }
        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID)) {
            City sheng = cityService.selectById(shengID);
            if (sheng == null) {
                return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
            }
            map.put("shengID", sheng.getId());
            map.put("shengName", sheng.getName());
        }

        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID)) {
            City shi = cityService.selectById(shiID);
            if (shi == null) {
                return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
            }
            map.put("shiID", shi.getId());
            map.put("shiName", shi.getName());
        }

        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID)) {
            City xian = cityService.selectById(xianID);
            if (xian == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("xianID", xian.getId());
            map.put("xianName", xian.getName());
        }

        String zhenID = request.getParameter("zhenID");
        if (StringUtils.isNotBlank(zhenID)) {
            City zhen = cityService.selectById(zhenID);
            if (zhen == null) {
                return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
            }
            map.put("zhenID", zhenID);
            map.put("zhenName", zhen.getName());
        }

        String cunID = request.getParameter("cunID");
        if (StringUtils.isNotBlank(cunID)) {
            City cun = cityService.selectById(cunID);
            if (cun == null) {
                return MessagePacket.newFail(MessageHeader.Code.cunIDNotFound, "cun 不正确");
            }
            map.put("cunID", cunID);
            map.put("cunName", cun.getName());
        }

        String cityID = request.getParameter("cityID");
        if (StringUtils.isNotBlank(cityID)) {
            City city = cityService.selectById(cityID);
            if (city == null) {
                return MessagePacket.newFail(MessageHeader.Code.cityIDIsError, "cityID不正确");
            }
            map.put("cityID", cityID);
        }

        String cityChain = request.getParameter("cityChain");
        if (StringUtils.isNotBlank(cityChain)) {
            map.put("cityChain", cityChain);
        }

        String districtID = request.getParameter("districtID");
        if (StringUtils.isNotBlank(districtID)) {
            if (!districtService.checkIdIsValid(districtID)) {
                return MessagePacket.newFail(MessageHeader.Code.districtIDIsError, "districtID不正确");
            }
            map.put("districtID", districtID);
        }

        String propertyID = request.getParameter("propertyID");
        if (StringUtils.isNotBlank(propertyID)) {
            if (!propertyService.checkIdIsValid(propertyID)) {
                return MessagePacket.newFail(MessageHeader.Code.propertyIDNotFound, "propertyID不正确");
            }
            map.put("propertyID", propertyID);
        }

        String residentsType = request.getParameter("residentsType");
        if (StringUtils.isNotBlank(residentsType)) {
            if (!NumberUtils.isNumeric(residentsType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "residentsType请输入数字");
            }
            map.put("residentsType", residentsType);
        }

        String homeAddr = request.getParameter("homeAddr");
        if (StringUtils.isNotBlank(homeAddr)) {
            map.put("homeAddr", homeAddr);
        }

        String homeZip = request.getParameter("homeZip");
        if (StringUtils.isNotBlank(homeZip)) {
            map.put("homeZip", homeZip);
        }

        String companyName = request.getParameter("companyName");
        if (StringUtils.isNotBlank(companyName)) {
            map.put("companyName", companyName);
        }

        String companyJob = request.getParameter("companyJob");
        if (StringUtils.isNotBlank(companyJob)) {
            map.put("companyJob", companyJob);
        }

        String companyAddress = request.getParameter("companyAddress");
        if (StringUtils.isNotBlank(companyAddress)) {
            map.put("companyAddress", companyAddress);
        }

        String companyTel = request.getParameter("companyTel");
        if (StringUtils.isNotBlank(companyTel)) {
            map.put("companyTel", companyTel);
        }

        String companyZip = request.getParameter("companyZip");
        if (StringUtils.isNotBlank(companyZip)) {
            map.put("companyZip", companyZip);
        }

        String headName = request.getParameter("headName");
        if (StringUtils.isNotBlank(headName)) {
            map.put("headName", headName);
        }

        String career = request.getParameter("career");
        if (StringUtils.isNotBlank(career)) {
            map.put("career", career);
        }

        String industryCategoryID = request.getParameter("industryCategoryID");
        if (StringUtils.isNotBlank(industryCategoryID)) {
            if (!categoryService.checkCategoryId(industryCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "industryCategoryID不正确");
            }
            map.put("industryCategoryID", industryCategoryID);
        }

        String professionalCategoryID = request.getParameter("professionalCategoryID");
        if (StringUtils.isNotBlank(professionalCategoryID)) {
            if (!categoryService.checkCategoryId(professionalCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "professionalCategoryID不正确");
            }
            map.put("professionalCategoryID", professionalCategoryID);
        }

        String jobCategoryID = request.getParameter("jobCategoryID");
        if (StringUtils.isNotBlank(jobCategoryID)) {
            if (!categoryService.checkCategoryId(jobCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobCategoryID不正确");
            }
            map.put("jobCategoryID", jobCategoryID);
        }

        String jobYearCategoryID = request.getParameter("jobYearCategoryID");
        if (StringUtils.isNotBlank(jobYearCategoryID)) {
            if (!categoryService.checkCategoryId(jobYearCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "jobYearCategoryID不正确");
            }
            map.put("jobYearCategoryID", jobYearCategoryID);
        }

        String educationCategoryID = request.getParameter("educationCategoryID");
        if (StringUtils.isNotBlank(educationCategoryID)) {
            if (!categoryService.checkCategoryId(educationCategoryID)) {
                return MessagePacket.newFail(MessageHeader.Code.categoryIDNotFound, "educationCategoryID不正确");
            }
            map.put("educationCategoryID", educationCategoryID);
        }

        String graduationYear = request.getParameter("graduationYear");
        if (StringUtils.isNotBlank(graduationYear)) {
            if (!NumberUtils.isNumeric(graduationYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "graduationYear请输入数字");
            }
            map.put("graduationYear", graduationYear);
        }

        String jobYear = request.getParameter("jobYear");
        if (StringUtils.isNotBlank(jobYear)) {
            if (!NumberUtils.isNumeric(jobYear)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "jobYear 请输入数字");
            }
            map.put("jobYear", jobYear);
        }

        String schoolID = request.getParameter("schoolID");
        if (StringUtils.isNotBlank(schoolID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("school", schoolID))) {
                return MessagePacket.newFail(MessageHeader.Code.schoolIDNotFound, "schoolID不正确");
            }
            map.put("schoolID", schoolID);
        }

        String departmentID = request.getParameter("departmentID");
        if (StringUtils.isNotBlank(departmentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("department", departmentID))) {
                return MessagePacket.newFail(MessageHeader.Code.departmentIDNotFound, "departmentID不正确");
            }
            map.put("departmentID", departmentID);
        }

        String careerApplyTime = request.getParameter("careerApplyTime");
        if (StringUtils.isNotBlank(careerApplyTime)) {
            if (!TimeTest.isTimeType(careerApplyTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "careerApplyTime 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("careerApplyTime", careerApplyTime);
        }
        String careerVerifyTime = request.getParameter("careerVerifyTime");
        if (StringUtils.isNotBlank(careerVerifyTime)) {
            if (!TimeTest.isTimeType(careerVerifyTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "careerVerifyTime 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("careerVerifyTime", careerVerifyTime);
        }

        String weibo = request.getParameter("weibo");
        if (StringUtils.isNotBlank(weibo)) {
            map.put("weibo", weibo);
        }

        String weiboToken = request.getParameter("weiboToken");
        if (StringUtils.isNotBlank(weiboToken)) {
            map.put("weiboToken", weiboToken);
        }

        String weiboID = request.getParameter("weiboID");
        if (StringUtils.isNotBlank(weiboID)) {
            map.put("weiboID", weiboID);
        }

        String weixin = request.getParameter("weixin");
        if (StringUtils.isNotBlank(weixin)) {
            map.put("weixin", weixin);
        }

        String weixinToken = request.getParameter("weixinToken");
        if (StringUtils.isNotBlank(weixinToken)) {
            map.put("weixinToken", weixinToken);
        }

        String weixinUnionID = request.getParameter("weixinUnionID");
        if (StringUtils.isNotBlank(weixinUnionID)) {
            map.put("weixinUnionID", weixinUnionID);
        }

        String weixinCode = request.getParameter("weixinCode");
        if (StringUtils.isNotBlank(weixinCode)) {
            map.put("weixinCode", weixinCode);
        }

        String weiboAccessToken = request.getParameter("weiboAccessToken");
        if (StringUtils.isNotBlank(weiboAccessToken)) {
            map.put("weiboAccessToken", weiboAccessToken);
        }

        String weiboTokenExpires = request.getParameter("weiboTokenExpires");
        if (StringUtils.isNotBlank(weiboTokenExpires)) {
            map.put("weiboTokenExpires", weiboTokenExpires);
        }

        String feishuID = request.getParameter("feishuID");
        if (StringUtils.isNotBlank(feishuID)) {
            map.put("feishuID", feishuID);
        }

        String feishuName = request.getParameter("feishuName");
        if (StringUtils.isNotBlank(feishuName)) {
            map.put("feishuName", feishuName);
        }

        String qiyeweixinID = request.getParameter("qiyeweixinID");
        if (StringUtils.isNotBlank(qiyeweixinID)) {
            map.put("qiyeweixinID", qiyeweixinID);
        }

        String qiyeweixinName = request.getParameter("qiyeweixinName");
        if (StringUtils.isNotBlank(qiyeweixinName)) {
            map.put("qiyeweixinName", qiyeweixinName);
        }

        String dingdingID = request.getParameter("dingdingID");
        if (StringUtils.isNotBlank(dingdingID)) {
            map.put("dingdingID", dingdingID);
        }

        String dingdingName = request.getParameter("dingdingName");
        if (StringUtils.isNotBlank(dingdingName)) {
            map.put("dingdingName", dingdingName);
        }

        String qqCode = request.getParameter("qqCode");
        if (StringUtils.isNotBlank(qqCode)) {
            map.put("qqCode", qqCode);
        }

        String qqID = request.getParameter("qqID");
        if (StringUtils.isNotBlank(qqID)) {
            map.put("qqID", qqID);
        }

        String qqName = request.getParameter("qqName");
        if (StringUtils.isNotBlank(qqName)) {
            map.put("qqName", qqName);
        }

        String zhifubaoID = request.getParameter("zhifubaoID");
        if (StringUtils.isNotBlank(zhifubaoID)) {
            map.put("zhifubaoID", zhifubaoID);
        }

        String zhifubaoName = request.getParameter("zhifubaoName");
        if (StringUtils.isNotBlank(zhifubaoName)) {
            map.put("zhifubaoName", zhifubaoName);
        }

        String taobaoID = request.getParameter("taobaoID");
        if (StringUtils.isNotBlank(taobaoID)) {
            map.put("taobaoID", taobaoID);
        }

        String taobaoName = request.getParameter("taobaoName");
        if (StringUtils.isNotBlank(taobaoName)) {
            map.put("taobaoName", taobaoName);
        }

        String jingdongID = request.getParameter("jingdongID");
        if (StringUtils.isNotBlank(jingdongID)) {
            map.put("jingdongID", jingdongID);
        }

        String jingdongName = request.getParameter("jingdongName");
        if (StringUtils.isNotBlank(jingdongName)) {
            map.put("jingdongName", jingdongName);
        }


        String jingdongCode = request.getParameter("jingdongCode");
        if (StringUtils.isNotBlank(jingdongCode)) {
            map.put("jingdongCode", jingdongCode);
        }

        String appleUserID = request.getParameter("appleUserID");
        if (StringUtils.isNotBlank(appleUserID)) {
            map.put("appleUserID", appleUserID);
        }

        String domainID = request.getParameter("domainID");
        if (StringUtils.isNotBlank(domainID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("domain", domainID))) {
                return MessagePacket.newFail(MessageHeader.Code.domainIDNotFound, "domainID不正确");
            }
            map.put("domainID", domainID);
        }

        String domainAccount = request.getParameter("domainAccount");
        if (StringUtils.isNotBlank(domainAccount)) {
            map.put("domainAccount", domainAccount);
        }


        String orderSeq = request.getParameter("orderSeq");
        if (StringUtils.isNotBlank(orderSeq)) {
            map.put("orderSeq", orderSeq);
        }

        String recommendCode = request.getParameter("recommendCode");
        if (StringUtils.isNotBlank(recommendCode)) {
            map.put("recommendCode", recommendCode);
        }

        String recommendID = request.getParameter("recommendID");
        if (StringUtils.isNotBlank(recommendID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", recommendID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "recommendID不正确");
            }
            map.put("recommendID", recommendID);
        }

        String recommendChain = request.getParameter("recommendChain");
        if (StringUtils.isNotBlank(recommendChain)) {
            map.put("recommendChain", recommendChain);
        }

        String levelNumber = request.getParameter("levelNumber");
        if (StringUtils.isNotBlank(levelNumber)) {
            if (!NumberUtils.isNumeric(levelNumber)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNot, "levelNumber请输入数字");

            }
            map.put("levelNumber", levelNumber);
        }

        String linkMemberID = request.getParameter("linkMemberID");
        if (StringUtils.isNotBlank(linkMemberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", linkMemberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "linkMemberID不正确");
            }
            map.put("linkMemberID", linkMemberID);
        }

        String linkChain = request.getParameter("linkChain");
        if (StringUtils.isNotBlank(linkChain)) {
            map.put("linkChain", linkChain);
        }

        String distributorCompanyID = request.getParameter("distributorCompanyID");
        if (StringUtils.isNotBlank(distributorCompanyID)) {
            if (!companyService.checkIdIsValid(distributorCompanyID)) {
                return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "distributorCompanyID不正确");
            }
            map.put("distributorCompanyID", distributorCompanyID);
        }

        String serviceMemberID = request.getParameter("serviceMemberID");
        if (StringUtils.isNotBlank(serviceMemberID)) {
            if (!memberService.checkMemberId(serviceMemberID)) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "serviceMemberID不正确");
            }
            map.put("serviceMemberID", serviceMemberID);
        }

        String tokenAddress = request.getParameter("tokenAddress");
        if (StringUtils.isNotBlank(tokenAddress)) {
            map.put("tokenAddress", tokenAddress);
        }

        String mapX = request.getParameter("mapX");
        if (StringUtils.isNotBlank(mapX)) {
            map.put("mapX", mapX);
        }

        String mapY = request.getParameter("mapY");
        if (StringUtils.isNotBlank(mapY)) {
            map.put("mapY", mapY);
        }

        String mapAddress = request.getParameter("mapAddress");
        if (StringUtils.isNotBlank(mapAddress)) {
            map.put("mapAddress", mapAddress);
        }

        String openID = request.getParameter("openID");
        if (StringUtils.isNotBlank(openID)) {
            map.put("openID", openID);
        }

        String openRegisterTime = request.getParameter("openRegisterTime");
        if (StringUtils.isNotBlank(openRegisterTime)) {
            map.put("openRegisterTime", openRegisterTime);
        }

        String createType = request.getParameter("createType");
        if (StringUtils.isNotBlank(createType)) {
            map.put("createType", createType);
        }

        String huanxinRegisterTime = request.getParameter("huanxinRegisterTime"); // 环信注册时间
        if (StringUtils.isNotBlank(huanxinRegisterTime)) {
            if (!TimeTest.isTimeType(huanxinRegisterTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "huanxinRegisterTime 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("huanxinRegisterTime", huanxinRegisterTime);
        }

        String huanxinLoginTime = request.getParameter("huanxinLoginTime"); // 环信登录时间
        if (StringUtils.isNotBlank(huanxinLoginTime)) {
            if (!TimeTest.isTimeType(huanxinLoginTime)) {
                return MessagePacket.newFail(MessageHeader.Code.timeType, "huanxinLoginTime 请输入数字 请输入yyyy-MM-dd HH:mm:ss的格式");
            }
            map.put("huanxinLoginTime", huanxinLoginTime);
        }

        String lastLoginTime = request.getParameter("lastLoginTime");
        if (StringUtils.isNotBlank(lastLoginTime)) {
            map.put("lastLoginTime", lastLoginTime);
        }

        String isPublishMe = request.getParameter("isPublishMe");
        if (StringUtils.isNotBlank(isPublishMe)) {
            map.put("isPublishMe", isPublishMe);
        }


        String modifierId = null;
        if (member != null) {
            modifierId = member.getId();
        }
        map.put("modifier", modifierId);
        memberService.updateMember(map);

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("updateTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "updateOneStudent success");
    }


    @ApiOperation(value = "outSystemMemberPictureSeq", notes = "独立接口：外部系统调整会员顺序")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "outToken", value = "登录标识", dataType = "String")})
    @RequestMapping(value = "/outSystemMemberPictureSeq", produces = {"application/json;charset=UTF-8"})
    public MessagePacket outSystemMemberPictureSeq(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        OpLog opLog = null;
        if (StringUtils.isEmpty(outToken)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//      user=userService.selectUserByOutToken(outToken);
        if (user == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
        if (opLog == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }
        String addmembersPictureArray = request.getParameter("addmembersPictureArray");
        if (StringUtils.isEmpty(addmembersPictureArray)) {
            return MessagePacket.newFail(MessageHeader.Code.addmembersPictureArrayIsNotNull, "addmembersPictureArray不能为空！");
        }
        if (addmembersPictureArray.indexOf(",") == -1) {
            return MessagePacket.newFail(MessageHeader.Code.addmembersPictureArrayIsNotNull, "addmembersPictureArray值需要使用英文逗号隔开！");
        }
        String[] addmembersPictures = addmembersPictureArray.split(",");
        if (addmembersPictures != null && addmembersPictures.length > 0) {
            map.put("modifier", user.getId());
            for (int i = 0; i < addmembersPictures.length; i++) {
                map.put("id", addmembersPictures[i]);
                map.put("orderSeq", i + 1);
                memberService.updateMember(map);
            }
        }
        if (user != null) {
            String descriptions = String.format("外部系统调整会员顺序");
            sendMessageService.sendDataLogMessage(user, descriptions, "outSystemMemberPictureSeq",
                    DataLog.objectDefineID.focusPicture.getOname(), null,
                    null, DataLog.OperateType.CHANGESEQ, request);
        }
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("moveTime", TimeTest.getTimeStr());
        return MessagePacket.newSuccess(rsMap, "outSystemMemberPictureSeq success");
    }

    @ApiOperation(value = "memberLoginHuanxin", notes = "独立接口：会员登录到环信")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/memberLoginHuanxin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket memberLoginHuanxin(HttpServletRequest request, ModelMap map) {
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogDTO memberLogDTO = null;
        if (StringUtils.isNotBlank(sessionID)) {
            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
            if (member == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        }
        String applicationID =null;
        if(member!=null) {
             applicationID = member.getApplicationID();
        }

        if (applicationID!=null&&StringUtils.isNotBlank(applicationID)) {
            Application byId = applicationService.getById(applicationID);

            if (byId != null) {
                String huanxinCHannelID = byId.getHuanxinCHannelID();
                if (StringUtils.isBlank(huanxinCHannelID)) {
                    // 在 application表 加了 huanxinCHannelID
                    // 如果 不为空，表示支持环信，为空 则不支持。接口返回错误
                    return MessagePacket.newFail(MessageHeader.Code.huanxinCHannelIDNotFound, "不支持登录环信");
                }
            }
        }

        String channelID = null;
        if (member != null) {
            channelID = member.getChannelID();
        }
        if (channelID!=null&&StringUtils.isNotBlank(channelID)) {
            OutChannelDto outSystemDtoById = channelService.getOutSystemDtoById(channelID);
            if (outSystemDtoById != null) {
                // 获取channel里面的 appkey，clientID，clientSecret 等 去掉环境的接口
                String appkey = outSystemDtoById.getAppkey();
                String clientKey = outSystemDtoById.getClientKey();
                String clientID = outSystemDtoById.getClientID();

                String access_token = null;

                try {
                    CloseableHttpClient authHttpClient = HttpClients.createDefault();
                    HttpPost getDataHttpPost = new HttpPost();
                    String a = "http://a1.easecdn.com/1173231223154042/demo/token";
                    getDataHttpPost.setURI(URI.create(a));
                    // 添加请求头
                    getDataHttpPost.setHeader("Content-Type", "application/json");
                    getDataHttpPost.setHeader("Accept", "application/json");

                    // 添加请求体（JSON数据）
                    // String jsonBody = "{\n" + "    \"grant_type\": \"client_credentials\",\n" + "    \"client_id\": \" "+ clientID +" \",\n" + "    \"client_secret\": \" " + clientKey + "\"\n" + "}";
                    String jsonBody = "{\n" +
                            "    \"grant_type\": \"client_credentials\",\n" +
                            "    \"client_id\": \"YXA6h9BBThfaTKuAz9pLLze1mQ\",\n" +
                            "    \"client_secret\": \"YXA63pSSKNfsF0e0FSuC_6Slbw0P2MQ\"\n" +
                            "}";
                    getDataHttpPost.setEntity(new StringEntity(jsonBody));

                    CloseableHttpResponse authResponse = authHttpClient.execute(getDataHttpPost);
                    net.sf.json.JSONObject jsonObject = new net.sf.json.JSONObject();

                    HttpEntity authResponseEntity = authResponse.getEntity();

                    String authJson = EntityUtils.toString(authResponseEntity);

                    log.info("log" + "authJson = " + authJson);

                    jsonObject = net.sf.json.JSONObject.fromObject(authJson);

                    access_token = String.valueOf(jsonObject.get("access_token"));

                    log.info("log" + "authJson = " + authJson);

                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                } catch (ClientProtocolException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        MemberDetail memberDetail = new MemberDetail();
        if (member != null) {
            memberDetail = memberService.getMemberDetail(member.getId());
        }
        Timestamp huanxinRegisterTime = null;
        if (memberDetail != null) {
            huanxinRegisterTime = memberDetail.getHuanxinRegisterTime();
        }
        if (huanxinRegisterTime != null && huanxinRegisterTime.toString() == null) {
            // 检查member表的 环信注册时间，如果为空，则表示没有注册，当前接口要调环信的注册接口，用户名就是memberID，密码就是会员的创建日期字符串（20240101）

            try {
                CloseableHttpClient authHttpClient = HttpClients.createDefault();
                HttpPost getDataHttpPost = new HttpPost();
                String a = "https://a1.easecdn.com/1173231223154042/onefind/users";
                getDataHttpPost.setURI(URI.create(a));
                // 添加请求头
                getDataHttpPost.setHeader("Content-Type", "application/json");
                getDataHttpPost.setHeader("Accept", "application/json");

                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");

                Date parse = inputFormat.parse(memberDetail.getCreatedTime().toString());
                String format = outputFormat.format(parse);

                // 添加请求体（JSON数据）
                String jsonBody = "{\n" +
                        "     \"username\": \"" + memberDetail.getId() + "\",\n" +
                        "     \"password\": \"" + format + "\"\n" +
                        "   }";
                getDataHttpPost.setEntity(new StringEntity(jsonBody));

                CloseableHttpResponse authResponse = authHttpClient.execute(getDataHttpPost);

                net.sf.json.JSONObject jsonObject = new net.sf.json.JSONObject();

                HttpEntity authResponseEntity = authResponse.getEntity();

                String authJson = EntityUtils.toString(authResponseEntity);

                jsonObject = net.sf.json.JSONObject.fromObject(authJson);

                JSONArray jsonArray = jsonObject.getJSONArray("entities");

                if (jsonArray.size() > 0) {
                    net.sf.json.JSONObject o = (net.sf.json.JSONObject) jsonArray.get(0);

                    Object uuid = o.getString("uuid");

                    log.info("log" + "authJson = " + authJson);
                }
                map.clear();
                map.put("id", memberDetail.getId());
                map.put("huanxinRegisterTime", TimeTest.getTimeStr());
                memberService.updateMember(map);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (java.text.ParseException e) {
                throw new RuntimeException(e);
            } catch (ClientProtocolException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
//        else {
//            // 如果 环信注册时间不为空，则直接调 环信的登录接口。
//        }

        // 返回环信的登录 token
        //
        // 要记录 memberLog
        // 要记录1个 channelRecord
        // 注意，调环信的接口，要先 进行 app登录，获取到 apptoken。在 channel表 加了 appToken，保存到这里，避免频繁登录他们。 登录成功时间记录到 appLoginTime，通过这个判断，超过24小时可 登录1次 app。
        //
        // huanxinRegisterTime
        // huanxinLoginTime
        // createdTime
        // 以上3个数字 来自
        // huanxinToken ：来自环信的接口
        //
        // sessionID 必须，

        if (memberLogDTO != null) {
            String description = String.format("会员登录到环信");
            sendMessageService.newSendMemberLogMessage(sessionID, description, request, NewMemberlog.MemberOperateType.memberLoginHuanxin);
        }
        return MessagePacket.newSuccess("memberLoginHuanxin success");
    }

    @ApiOperation(value = "facilityIdentityMemberFastLogin", notes = "独立接口：人脸识别后的快捷登录会员学生")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/facilityIdentityMemberFastLogin", produces = {"application/json;charset=UTF-8"})
    public MessagePacket facilityIdentityMemberFastLogin(HttpServletRequest request, ModelMap map) {
        String siteID = request.getParameter("siteID");
        if (StringUtils.isBlank(siteID))
            return MessagePacket.newFail(MessageHeader.Code.siteIDNotNull, "siteID 不为空");
        if (StringUtils.isNotBlank(siteID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("site", siteID))) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID 不正确");
            }
            map.put("siteID", siteID);
        }
        String deviceID = request.getParameter("deviceID");
        if (StringUtils.isNotBlank(deviceID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("device", deviceID))) {
                return MessagePacket.newFail(MessageHeader.Code.deviceIDNotFound, "deviceID 不正确");
            }
            map.put("deviceID", deviceID);
        }
        String computerID = request.getParameter("computerID");
        if (StringUtils.isNotBlank(computerID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("computer", computerID))) {
                return MessagePacket.newFail(MessageHeader.Code.computerIDNotFound, "computerID 不正确");
            }
            map.put("computerID", computerID);
        }
        if (StringUtils.isBlank(deviceID) && StringUtils.isBlank(deviceID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "computerID,deviceID 不能同时为空");
        }

        String memberID = request.getParameter("memberID");
        if (StringUtils.isNotBlank(memberID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("member", memberID))) {
                return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID 不正确");
            }
            map.put("memberID", memberID);
        }

        String studentID = request.getParameter("studentID");
        if (StringUtils.isNotBlank(studentID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("student", studentID))) {
                return MessagePacket.newFail(MessageHeader.Code.studentIDNotFound, "studentID 不正确");
            }
            ApiStudentDetail detail = studentService.getOneStudentDetailByID(studentID);
            String student_memberID = detail.getMemberID();
            if (StringUtils.isNotBlank(student_memberID)) {
                memberID = student_memberID;
                map.put("memberID", memberID);
            }
            map.put("studentID", studentID);
        }
        if (StringUtils.isBlank(studentID) && StringUtils.isBlank(memberID)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "studentID,memberID 不能同时为空");
        }
        String requestCode = request.getParameter("requestCode");
        if (StringUtils.isBlank(requestCode)) {
            return MessagePacket.newFail(MessageHeader.Code.fail, "requestCode 不为空");
        }

        String userLoginKey = "request_" + requestCode;
        Optional<Object> optionalUserLogin = Optional.ofNullable(redisTemplate.opsForValue().get(userLoginKey));
//        if (optionalUserLogin.isPresent()) {
//        } else {
//            // 处理键不存在的情况，比如提示没有找到对应的userLogin对象等
//            log.info("log" + "未找到对应的userLogin对象");
//            return MessagePacket.newFail(MessageHeader.Code.userLoginfail, "requestCode 人脸登录失败");
//        }
        if (!optionalUserLogin.isPresent()) {
            // 处理键不存在的情况，比如提示没有找到对应的userLogin对象等
            log.info("log" + "未找到对应的userLogin对象");
            return MessagePacket.newFail(MessageHeader.Code.userLoginfail, "requestCode 人脸登录失败");
        }

        OutMemberList memberDetail = memberService.outSystemGetMemberDetail(memberID);
        if (memberDetail == null) return MessagePacket.newFail(MessageHeader.Code.memberIDNotFound, "memberID 不正确");
        if (memberDetail.getIsLock() == 1) {
            return MessagePacket.newFail(MessageHeader.Code.memberIsLocked, "memberID 会员已被锁定");
        }

        map.clear();
        map.put("memberID", memberDetail.getId());
        WeixinAppMember weixinAppMember = weixinMemberService.getByMap(map);
        if (weixinAppMember != null) {
            weixinMemberService.updateWeixinAppMember(weixinAppMember);
        }
        map.put("deviceID", deviceID);
        String memberDeviceID = memberDeviceService.getIdByMap(map);
        if (StringUtils.isBlank(memberDeviceID)) {
            map.put("name", memberDetail.getName() + deviceService.getNameById(deviceID));
            map.put("creator", memberDetail.getId());
            map.put("id", IdGenerator.uuid32());
            memberDeviceService.insertByMap(map);
        }
        //2020-09-27 修改device机器最后登录时间
        deviceService.updateLastLoadTime(deviceID);

        String ipaddr = WebUtil.getRemortIP(request);
        String uuid = IdGenerator.uuid32();
        // session检查
        memberService.checkSession(deviceID, uuid, memberDetail.getId(), siteID, memberDetail.toString());

        //新增memberLogin
        SiteDetail siteDetail = siteService.getOneSiteDetail(siteID);
        MemberLogin memberLogin = new MemberLogin();
        Map<String, Object> map1 = new HashMap<>();

        map1.put("applicationID", siteDetail.getApplicationID());
        memberLogin.setApplicationID(siteDetail.getApplicationID());
        map1.put("applicationName", siteDetail.getApplicationName());
        memberLogin.setApplicationName(siteDetail.getApplicationName());
        map1.put("siteID", siteDetail.getId());
        memberLogin.setSiteID(siteDetail.getId());
        map1.put("deviceID", deviceID);
        memberLogin.setDeviceID(deviceID);
        map1.put("siteName", siteDetail.getName());
        memberLogin.setSiteName(siteDetail.getName());
        String ip = WebUtil.getRemortIP(request);
        map1.put("ipAddress", ip);
        memberLogin.setIpAddress(ip);
        map1.put("locationID", memberService.getLocationID(ip));
        memberLogin.setLocationID(memberService.getLocationID(ip));
        String userAgent = request.getHeader("User-Agent");
        String browserType = "Unknown";
        if (userAgent != null) {
            if (userAgent.contains("Firefox")) {
                browserType = "Firefox";
            } else if (userAgent.contains("Chrome")) {
                browserType = "Chrome";
            } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/7.0")) {
                browserType = "Internet Explorer";
            } else if (userAgent.contains("Safari")) {
                browserType = "Safari";
            } else if (userAgent.contains("Opera")) {
                browserType = "Opera";
            }
        }
        map1.put("browserType", browserType);
        memberLogin.setBrowserType(browserType);
        map1.put("memberID", memberDetail.getId());
        memberLogin.setMemberID(memberDetail.getId());
        map1.put("memberName", memberDetail.getName());
        memberLogin.setMemberName(memberDetail.getName());
        map1.put("memberShortName", memberDetail.getShortName());
        memberLogin.setMemberShortName(memberDetail.getShortName());
        map1.put("memberAvatar", memberDetail.getAvatarURL());
        memberLogin.setMemberAvatar(memberDetail.getAvatarURL());
        map1.put("loginName", memberDetail.getLoginName());
        memberLogin.setLoginName(memberDetail.getLoginName());

        map1.put("phone", memberDetail.getPhone());
        memberLogin.setPhone(memberDetail.getPhone());
        map1.put("weixinToken", memberDetail.getWeixinToken());
        memberLogin.setWeixinToken(memberDetail.getWeixinToken());
        map1.put("weixinUnionID", memberDetail.getWeixinUnionID());
        memberLogin.setWeixinUnionID(memberDetail.getWeixinUnionID());
        map1.put("recommendCode", memberDetail.getRecommendCode());
        memberLogin.setRecommendCode(memberDetail.getRecommendCode());
        map1.put("recommendID", memberDetail.getRecommendID());
        memberLogin.setRecommendID(memberDetail.getRecommendID());
        map1.put("companyID", memberDetail.getCompanyID());
        memberLogin.setCompanyID(memberDetail.getCompanyID());
        map1.put("shopID", memberDetail.getShopID());
        memberLogin.setShopID(memberDetail.getShopID());
        map1.put("rankID", memberDetail.getRankID());
        memberLogin.setRankID(memberDetail.getRankID());
        map1.put("peopleID", memberDetail.getPeopleID());
        memberLogin.setPeopleID(memberDetail.getPeopleID());
        memberLogin.setLoginTime(TimeTest.getTime());
        /**
         * 2是 在 会员登录的接口，先设置 redis的 isWork=0，
         * 再增加1个 消息队列，在消息队列里面 获取 这个会员memberID对应的员工，
         * 如果有 ，则 获取最新的1个 employee记录，初始化 workXXXX 各属性，
         * 并设置 isWork=1；(感觉没必要用消息队列)
         */
        Employee employee = employeeService.getMemberLogin(memberLogin);
        if (employee != null) {
            if (StringUtils.isNotBlank(employee.getCompanyID())) {
                memberLogin.setIsWork(1);
                memberLogin.setWorkEmployeeID(employee.getId());
                memberLogin.setWorkCompanyID(employee.getCompanyID());
                memberLogin.setWorkDepartmentID(employee.getDepartmentID());
                memberLogin.setWorkShopID(employee.getShopID());
                memberLogin.setWorkJobID(employee.getJobID());
                memberLogin.setWorkGradeID(employee.getGradeID());
            }
        }

        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, uuid), memberLogin, redisTimeOut, TimeUnit.SECONDS);
        redisTemplate.delete(userLoginKey);
        if (memberLogin != null) {
            memberLogin.setInterfaceName("人脸识别后的快捷登录会员学生");
            memberLogin.setInterfaceShortName("facilityIdentityMemberFastLogin");
            memberLogin.setOperateType(DataLog.OperateType.DETAIL);
            memberLogin.setObjectDefineID(DataLog.objectDefineID.memberLog.getOname());
            memberLogin.setObjectID(memberID);
            memberLogin.setObjectName(memberDetail.getName());
            sendMessageService.sendMemberLoginMessageNew(memberLogin, request);
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("sessionID", uuid);
        rsMap.put("memberID", memberDetail.getId());
        return MessagePacket.newSuccess("facilityIdentityMemberFastLogin success");
    }


    @ApiOperation(value = "changeCurrentCompanyAndEmployee", notes = "独立接口：会员切换公司和员工")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String")
    })
    @RequestMapping(value = "/changeCurrentCompanyAndEmployee", produces = {"application/json;charset=UTF-8"})
    public MessagePacket changeCurrentCompanyAndEmployee(HttpServletRequest request, Map<String, Object> map) {
        String sessionID = request.getParameter("sessionID");
        if (StringUtils.isBlank(sessionID)) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "sessionID不能为空");
        }
        MemberLogin memberLogin = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
        if (memberLogin == null) {
            return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
        }


        String roleEmployeeID = request.getParameter("roleEmployeeID");
        String companyID = request.getParameter("companyID");
        String employeeID = request.getParameter("employeeID");
        String functionID = request.getParameter("functionID");
        String roleID = request.getParameter("roleID");
        String departmentID = request.getParameter("departmentID");
        String shopID = request.getParameter("shopID");
        String jobID = request.getParameter("jobID");
        String gradeID = request.getParameter("gradeID");
        String officeID = request.getParameter("officeID");
        String userID = request.getParameter("userID");
        if (StringUtils.isEmpty(roleEmployeeID)) {
            if ((StringUtils.isEmpty(employeeID) && StringUtils.isEmpty(companyID))) {
                return MessagePacket.newFail(MessageHeader.Code.roleEmployeeIDAndCompanyAndEmployeeIDNotFound, "roleEmployeeID为空,companyID,employeeID不能为空");
            }
        }

        EmployeeDetailDto employee = null;
        OutSystemRoleEmployeeDetail roleEmployee = null;
        UserDetailDto user = null;


        if (StringUtils.isNotEmpty(roleEmployeeID)) {

            roleEmployee = roleEmployeeService.outSystemGetOneRoleEmployeeDetailByID(roleEmployeeID);
            if (roleEmployee == null) {
                return MessagePacket.newFail(MessageHeader.Code.roleEmployeeIDNotFound, "roleEmployeeID不存在");
            }
            companyID = roleEmployee.getCompanyID();
            employeeID = roleEmployee.getEmployeeID();
        }
        if (!companyService.checkIdIsValid(companyID))
            return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "companyID 不存在");
        if (!employeeService.checkIdIsValid(employeeID))
            return MessagePacket.newFail(MessageHeader.Code.employeeIDNotFound, "employeeID 不存在");
        employee = employeeService.getDetailById(employeeID);
//        if (!companyID.equals(employee.getCompanyID())) return MessagePacket.newFail(MessageHeader.Code.companyIDNotFound, "employeeID 中的 companyID 不一致。");

        map.clear();
        map.put("companyID", companyID);
        map.put("employeeID", employeeID);
        user = userService.outSystemGetOneUserDetail(map);
        if (user == null) {
            userID = null;
        }

        if (roleEmployee != null) {
            roleID = roleEmployee.getRoleID();
        } else {
            map.clear();
            map.put("companyID", companyID);
            map.put("employeeID", employeeID);
            roleEmployee = roleEmployeeService.getOneRoleEmployeeDetailByMap(map);
            if (roleEmployee == null) {
                roleID = null;
            }
            if (roleEmployee != null) roleID = roleEmployee.getRoleID();
        }

        if (user != null) userID = user.getUserID();
        departmentID = employee.getDepartmentID() == null ? null : employee.getDepartmentID();
        shopID = employee.getShopID() == null ? null : employee.getShopID();
        jobID = employee.getJobID() == null ? null : employee.getJobID();
        gradeID = employee.getGradeID() == null ? null : employee.getGradeID();
        officeID = employee.getOfficeID() == null ? null : employee.getOfficeID();

        memberLogin.setWorkCompanyID(companyID);
        memberLogin.setWorkEmployeeID(employeeID);
        memberLogin.setWorkDepartmentID(departmentID);
        memberLogin.setWorkShopID(shopID);
        memberLogin.setWorkJobID(jobID);
        memberLogin.setWorkGradeID(gradeID);
        memberLogin.setWorkRoleID(roleID);
        memberLogin.setWorkUserID(userID);
        memberLogin.setWorkFunctionID(functionID);
        memberLogin.setIsWork(1);


        redisTemplate.opsForValue().set(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID), memberLogin, redisTimeOut, TimeUnit.SECONDS);

        if (memberLogin != null) {

            memberLogin.setInterfaceName("会员切换公司和员工");
            memberLogin.setInterfaceShortName("changeCurrentCompanyAndEmployee");
            memberLogin.setOperateType(DataLog.OperateType.CHANGE);
            memberLogin.setObjectDefineID(DataLog.objectDefineID.section.getOname());
            memberLogin.setObjectID(employee.getEmployeeID());
            memberLogin.setObjectName(employee.getName());
            memberLogin.setOperateType(DataLog.OperateType.CHANGE);
            sendMessageService.sendMemberLoginMessageNew(memberLogin, request);
        }

        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("companyID", companyID);
        rsMap.put("employeeID", employeeID);
        rsMap.put("userID", userID);
        rsMap.put("roleID", roleID);
        rsMap.put("workCompanyID", memberLogin.getWorkCompanyID());
        rsMap.put("workEmployeeID", memberLogin.getWorkEmployeeID());
        rsMap.put("workShopID", memberLogin.getWorkShopID());
        rsMap.put("workUserID", memberLogin.getWorkUserID());
        rsMap.put("workDepartmentID", memberLogin.getWorkDepartmentID());
        rsMap.put("workJobID", memberLogin.getWorkJobID());
        rsMap.put("workGradeID", memberLogin.getWorkGradeID());
        rsMap.put("workRoleID", memberLogin.getWorkRoleID());
        rsMap.put("workFunctionID", memberLogin.getWorkFunctionID());
        return MessagePacket.newSuccess(rsMap, "changeCurrentCompanyAndEmployee success");
    }



    @ApiOperation(value = "getMemberForStatistics", notes = "独立接口：获取会员的统计信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "sessionID", value = "登录标识", dataType = "String"),
    })
    @RequestMapping(value = "/getMemberForStatistics", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMemberForStatistics(HttpServletRequest request, ModelMap map) {
        String outToken = request.getParameter("outToken");
        User user = null;
        UserLogin userLogin = null;
        if (StringUtils.isNotBlank(outToken)) {
            user = (User) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USER_KEY.key, outToken));
//         user = userService.selectUserByOutToken(outToken);
            if (user == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            OpLog opLog = (OpLog) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.OPLOG_KEY.key, outToken));
            if (opLog == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            userLogin = (UserLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.USERLOGIN_KEY.key, outToken));
            if (userLogin == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        }
        String sessionID = request.getParameter("sessionID");
        Member member = null;
        MemberLogin memberLogin = null;
        if (StringUtils.isNotBlank(sessionID)) {
//            member = (Member) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBER_KEY.key, sessionID));
            MemberLogDTO memberLogDTO = (MemberLogDTO) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOG_KEY.key, sessionID));
            if (memberLogDTO == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
            memberLogin = (MemberLogin) redisTemplate.opsForValue().get(String.format("%s%s", RedisKey.Key.MEMBERLOGIN_KEY.key, sessionID));
            if (memberLogin == null) {
                return MessagePacket.newFail(MessageHeader.Code.unauth, "请先登录");
            }
        }
        String applicationID = request.getParameter("applicationID");
        if (StringUtils.isBlank(outToken) && StringUtils.isBlank(sessionID) && StringUtils.isBlank(applicationID)) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "outToken和sessionID和applicationID不能同时为空");
        }
        /*
        if (StringUtils.isNotBlank(outToken) && StringUtils.isNotBlank(sessionID)&& StringUtils.isBlank(applicationID)) {
            return MessagePacket.newFail(MessageHeader.Code.statusIsError, "outToken和sessionID不能同时存在");
        }*/

        if (StringUtils.isNotBlank(outToken)) {

            if (userLogin != null) {
                map.put("applicationID", userLogin.getApplicationID());
            }
        }
        if (StringUtils.isNotBlank(sessionID)) {
            // map.put("applicationID",userLogin.getApplicationID());
            if (memberLogin != null) {
                map.put("applicationID", memberLogin.getApplicationID());
            }
        }


        if (org.apache.commons.lang3.StringUtils.isNotBlank(applicationID)) {
            if (!applicationService.checkIdIsValid(applicationID)) {
                return MessagePacket.newFail(MessageHeader.Code.applicationIDNotFound, "applicationID不正确");
            }
            map.put("applicationID", applicationID);
        }

        String siteID = request.getParameter("siteID");
        if (StringUtils.isNotBlank(siteID)) {
            if (!baseService.checkIdIsValid(new BaseParameter("site", siteID))) {
                return MessagePacket.newFail(MessageHeader.Code.siteIDNotFound, "siteID 不正确");
            }
            map.put("siteID", siteID);
        }

        String runType = request.getParameter("runType");
        if (StringUtils.isNotBlank(runType)) {
            if (!NumberUtils.isNumeric(runType)) {
                return MessagePacket.newFail(MessageHeader.Code.numberNotNull, "runType 请输入数字");
            }
            map.put("runType", runType);
        }

        MemberForStatistics memberForStatistics = memberService.getMemberForStatistics(map);
        //年龄段
        StringBuilder ageTotal = new StringBuilder();
        String ageArray = request.getParameter("ageArray");
        if (StringUtils.isNotBlank(ageArray)) {
            List<String> list = Arrays.asList(ageArray.split(","));
            List<Integer> sortedIntList = list.stream()
                    .map(Integer::parseInt)          // 字符串转 Integer
                    .sorted()                         // 自然排序（升序）
                    .collect(Collectors.toList());    // 收集为 List
            for (int i = 0; i < sortedIntList.size(); i++) {
                if (i == 0) {
                    map.put("beginAge", sortedIntList.get(i));
                    ageTotal.append("年龄段小于" + sortedIntList.get(i));
                } else if (i == sortedIntList.size() - 1) {
                    map.put("endAge", sortedIntList.get(i));
                    ageTotal.append("年龄段大于" + sortedIntList.get(i));
                } else {
                    map.put("beginAge", sortedIntList.get(i));
                    map.put("endAge", sortedIntList.get(i + 1));
                    ageTotal.append("年龄段介于" + sortedIntList.get(i) + "和" + sortedIntList.get(i + 1));
                }
                MemberForStatistics memberForStatistics1 = memberService.getMemberForStatistics(map);
                Integer memberTotal = memberForStatistics1.getMemberTotal();
                ageTotal.append("的总人数是" + memberTotal + "人。");
            }

            memberForStatistics.setAgeTotal(ageTotal.toString());
            /*
            map.put("status", status);*/
        }


        StringBuilder cityTotal = new StringBuilder();
        String shengID = request.getParameter("shengID");
        if (StringUtils.isNotBlank(shengID) && !cityService.checkIdIsValid(shengID)) {
            return MessagePacket.newFail(MessageHeader.Code.shengIDIsError, "shengID不正确");
        }

        String shiID = request.getParameter("shiID");
        if (StringUtils.isNotBlank(shiID) && !cityService.checkIdIsValid(shiID)) {
            return MessagePacket.newFail(MessageHeader.Code.shiIDIsError, "shiID不正确");
        }
        String xianID = request.getParameter("xianID");
        if (StringUtils.isNotBlank(xianID) && !cityService.checkIdIsValid(xianID)) {
            return MessagePacket.newFail(MessageHeader.Code.xianIDIsError, "xianID不正确");
        }


        String applicationID1 = map.get("applicationID").toString();
        map.clear();
        map.put("applicationID", applicationID1);
        if (StringUtils.isNotBlank(siteID)) {
            map.put("siteID", siteID);
        }
        if (StringUtils.isNotBlank(runType)) {
            map.put("runType", runType);
        }

        if (StringUtils.isNotBlank(shengID)) {
            //map.put("xianID", xianID);
            cityTotal.append("市统计： \n");
            Map<String, Object> map1 = Maps.newHashMap();
            map1.put("parentID", shengID);
            List<CityDto> simpleListByMap = cityService.getSimpleListByMap(map1);
            for (CityDto cityDto : simpleListByMap) {
                map.put("shiID", cityDto.getCityID());
                MemberForStatistics memberForStatistics1 = memberService.getMemberForStatistics(map);
                cityTotal.append("在" + cityDto.getName() + "的总人数是" + memberForStatistics1.getMemberTotal() + "人。");
            }
        }

        if (StringUtils.isNotBlank(shiID)) {
            //map.put("xianID", xianID);
            cityTotal.append("县统计： \n");
            Map<String, Object> map1 = Maps.newHashMap();
            map1.put("parentID", shiID);
            List<CityDto> simpleListByMap = cityService.getSimpleListByMap(map1);
            for (CityDto cityDto : simpleListByMap) {
                map.put("xianID", cityDto.getCityID());
                MemberForStatistics memberForStatistics1 = memberService.getMemberForStatistics(map);
                cityTotal.append("在" + cityDto.getName() + "的总人数是" + memberForStatistics1.getMemberTotal() + "人。");
            }
        }


        if (StringUtils.isNotBlank(xianID)) {
            //map.put("xianID", xianID);
            cityTotal.append("镇统计： \n");
            Map<String, Object> map1 = Maps.newHashMap();
            map1.put("parentID", xianID);
            List<CityDto> simpleListByMap = cityService.getSimpleListByMap(map1);
            for (CityDto cityDto : simpleListByMap) {
                map.put("zhenID", cityDto.getCityID());
                MemberForStatistics memberForStatistics1 = memberService.getMemberForStatistics(map);
                cityTotal.append("在" + cityDto.getName() + "的总人数是" + memberForStatistics1.getMemberTotal() + "人。");
            }
        }
        memberForStatistics.setCityTotal(cityTotal.toString());


        /*
        if (user != null) {//1.outToken不为空输出此日志
            sendMessageService.sendDataLogMessage(user, description1, "getSiteDayList",
                    DataLog.objectDefineID.application.getOname(), null,
                    null, DataLog.OperateType.LIST, request);
        } else {//2.否则输出此日志
            sendMessageService.sendMemberLogMessage(sessionID, description1, request, Memberlog.MemberOperateType.getSiteDayList);
        }*/
        Map<String, Object> rsMap = Maps.newHashMap();
        rsMap.put("data", memberForStatistics);
        return MessagePacket.newSuccess(rsMap, "getMemberForStatistics success");
    }


    @ApiOperation(value = "getMyMemberOpenInfo", notes = "独立接口：获取第三方会员开放信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "outToken", value = "登录标识", paramType = "query", dataType = "String")
    })
    @RequestMapping(value = "/getMyMemberOpenInfo", produces = {"application/json;charset=UTF-8"})
    public MessagePacket getMyMemberOpenInfo(HttpServletRequest request, ModelMap map) throws java.text.ParseException {
        return memberService.getMyMemberOpenInfo(request, map);
    }

    @ApiOperation(value = "writeOneMemberOpenInfo", notes = "独立接口：注册登录第三方开放会员信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "outToken", value = "登录标识", paramType = "query", dataType = "String")
    })
    @RequestMapping(value = "/writeOneMemberOpenInfo", produces = {"application/json;charset=UTF-8"})
    public MessagePacket writeOneMemberOpenInfo(HttpServletRequest request, ModelMap map) throws java.text.ParseException {
        return memberService.writeOneMemberOpenInfo(request, map);
    }

}
