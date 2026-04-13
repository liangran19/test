package com.kingpivot.service;

import com.kingpivot.api.dto.memberPayment.MemberPaymentListDto;
import com.kingpivot.api.dto.memberPayment.OutMemberPaymentDto;
import com.kingpivot.api.dto.memberPayment.OutMemberPaymentListDto;
import com.kingpivot.model.MemberPay;
import com.kingpivot.model.MemberPayment;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface MemberPaymentService {
    void insert(MemberPayment memberPayment);

    void insertMemberPay(Map<String,Object> map);

    boolean checkIdIsValid(String id);

    MemberPayment findById(String id);

    MemberPay getMemberPayByID(String id);

    MemberPayment getByObjectID(String objectID);

    void update(MemberPayment memberPayment);

    Timestamp getPayTime(String id);

    List<MemberPaymentListDto> getMemberPaymentList(Map<String,Object> map);

    MemberPayment selectIsCreated(Map<String,Object> map);

    OutMemberPaymentDto getOutSystemDtoById(String id);

    List<OutMemberPaymentListDto> getOutSystemListDtoByMap(Map<String,Object> map);

    OutMemberPaymentDto getOneMemberPaymentDetailByID(String memberPaymentID);
}
