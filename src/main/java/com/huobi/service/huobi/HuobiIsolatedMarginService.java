package com.huobi.service.huobi;

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.huobi.client.IsolatedMarginClient;
import com.huobi.client.req.margin.*;

import com.huobi.constant.Options;
import com.huobi.constant.enums.MarginTransferDirectionEnum;
import com.huobi.model.isolatedmargin.IsolatedMarginAccount;
import com.huobi.model.isolatedmargin.IsolatedMarginLoadOrder;
import com.huobi.model.isolatedmargin.IsolatedMarginSymbolInfo;
import com.huobi.model.isolatedmargin.LeveragePositionLimitResult;
import com.huobi.service.huobi.connection.HuobiRestConnection;
import com.huobi.service.huobi.connection.HuobiRestConnection25519;
import com.huobi.service.huobi.parser.isolatedmargin.IsolatedMarginAccountParser;
import com.huobi.service.huobi.parser.isolatedmargin.IsolatedMarginLoadOrderParser;
import com.huobi.service.huobi.parser.isolatedmargin.IsolatedMarginSymbolInfoParser;
import com.huobi.service.huobi.parser.isolatedmargin.LeveragePositionLimitResultParser;
import com.huobi.service.huobi.signature.UrlParamsBuilder;
import com.huobi.utils.InputChecker;

public class HuobiIsolatedMarginService implements IsolatedMarginClient {

  public static final String TRANSFER_TO_MARGIN_PATH = "/v1/dw/transfer-in/margin";//资产划入（逐仓）
  public static final String TRANSFER_TO_SPOT_PATH = "/v1/dw/transfer-out/margin";//资产划出（逐仓）
  public static final String GET_BALANCE_PATH = "/v1/margin/accounts/balance";//借币账户详情（逐仓）
  public static final String GET_LOAN_ORDER_PATH = "/v1/margin/loan-orders";//查询借币订单（逐仓）
  public static final String GET_LOAN_INFO_PATH = "/v1/margin/loan-info";//查询借币币息率及额度（逐仓）
  public static final String APPLY_LOAN_PATH = "/v1/margin/orders";//申请借币（逐仓）
  public static final String REPAY_LOAN_PATH = "/v1/margin/orders/{order-id}/repay";//归还借币（逐仓）
  public static final String GET_LEVERAGE_POSITION_LIMIT_PATH = "/v2/margin/limit";//获取杠杆持仓限额（全仓）


  private Options options;

  private HuobiRestConnection restConnection;

  public HuobiIsolatedMarginService(Options options) {
    this.options = options;
    if(options.getSign().equals("256")){
      this.restConnection = new HuobiRestConnection(options);
    }else if(options.getSign().equals("25519")){
      this.restConnection = new HuobiRestConnection25519(options);
    }
  }


  @Override
  public Long transfer(IsolatedMarginTransferRequest request) {

    InputChecker.checker()
        .shouldNotNull(request.getDirection(), "direction")
        .checkSymbol(request.getSymbol())
        .checkCurrency(request.getCurrency())
        .shouldNotNull(request.getAmount(), "amount");

    String path = null;
    if (request.getDirection() == MarginTransferDirectionEnum.SPOT_TO_MARGIN) {
      path = TRANSFER_TO_MARGIN_PATH;
    } else {
      path = TRANSFER_TO_SPOT_PATH;
    }

    UrlParamsBuilder builder = UrlParamsBuilder.build()
        .putToPost("currency", request.getCurrency())
        .putToPost("symbol", request.getSymbol())
        .putToPost("amount", request.getAmount());

    JSONObject jsonObject = restConnection.executePostWithSignature(path, builder);
    return jsonObject.getLong("data");
  }

  @Override
  public Long applyLoan(IsolatedMarginApplyLoanRequest request) {

    InputChecker.checker()
        .checkSymbol(request.getSymbol())
        .checkCurrency(request.getCurrency())
        .shouldNotNull(request.getAmount(), "amount");

    UrlParamsBuilder builder = UrlParamsBuilder.build()
        .putToPost("currency", request.getCurrency())
        .putToPost("symbol", request.getSymbol())
        .putToPost("amount", request.getAmount());

    JSONObject jsonObject = restConnection.executePostWithSignature(APPLY_LOAN_PATH, builder);
    return jsonObject.getLong("data");
  }

  @Override
  public Long repayLoan(IsolatedMarginRepayLoanRequest request) {

    InputChecker.checker()
        .shouldNotNull(request.getOrderId(), "order-id")
        .shouldNotNull(request.getAmount(), "amount");

    UrlParamsBuilder builder = UrlParamsBuilder.build()
        .putToPost("amount", request.getAmount());

    String path = REPAY_LOAN_PATH.replace("{order-id}", request.getOrderId().toString());
    JSONObject jsonObject = restConnection.executePostWithSignature(path, builder);
    return jsonObject.getLong("data");
  }

  @Override
  public List<IsolatedMarginLoadOrder> getLoanOrders(IsolatedMarginLoanOrdersRequest request) {

    InputChecker.checker()
        .checkSymbol(request.getSymbol());

    UrlParamsBuilder builder = UrlParamsBuilder.build()
        .putToUrl("symbol", request.getSymbol())
        .putToUrl("start-date", request.getStartDate(), "yyyy-MM-dd")
        .putToUrl("end-date", request.getEndDate(), "yyyy-MM-dd")
        .putToUrl("states", request.getStatesString())
        .putToUrl("from", request.getFrom())
        .putToUrl("size", request.getSize())
        .putToUrl("direct", request.getDirection() == null ? null : request.getDirection().getCode())
        .putToUrl("sub-uid", request.getSubUid());

    JSONObject jsonObject = restConnection.executeGetWithSignature(GET_LOAN_ORDER_PATH, builder);
    JSONArray data = jsonObject.getJSONArray("data");
    return new IsolatedMarginLoadOrderParser().parseArray(data);
  }

  @Override
  public List<IsolatedMarginAccount> getLoanBalance(IsolatedMarginAccountRequest request) {

    UrlParamsBuilder builder = UrlParamsBuilder.build()
        .putToUrl("symbol", request.getSymbol())
        .putToUrl("sub-uid", request.getSubUid());

    JSONObject jsonObject = restConnection.executeGetWithSignature(GET_BALANCE_PATH, builder);
    JSONArray data = jsonObject.getJSONArray("data");
    return new IsolatedMarginAccountParser().parseArray(data);
  }

  @Override
  public List<IsolatedMarginSymbolInfo> getLoanInfo(IsolatedMarginLoanInfoRequest request) {
    UrlParamsBuilder builder = UrlParamsBuilder.build()
        .putToUrl("symbols", request.getSymbols());

    JSONObject jsonObject = restConnection.executeGetWithSignature(GET_LOAN_INFO_PATH, builder);
    JSONArray data = jsonObject.getJSONArray("data");
    return new IsolatedMarginSymbolInfoParser().parseArray(data);
  }

  @Override
  public List<LeveragePositionLimitResult> getLeveragePositionLimit(LeveragePositionLimitRequest request) {
    InputChecker.checker()
            .shouldNotNull(request.getCurrency(), "currency");
    UrlParamsBuilder builder = UrlParamsBuilder.build()
            .putToUrl("currency", request.getCurrency());

    JSONObject jsonObject = restConnection.executeGetWithSignature(GET_LEVERAGE_POSITION_LIMIT_PATH, builder);
    JSONArray data = jsonObject.getJSONArray("data");
    return new LeveragePositionLimitResultParser().parseArray(data);
  }


}
