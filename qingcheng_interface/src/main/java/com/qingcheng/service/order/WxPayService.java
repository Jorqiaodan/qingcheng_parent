package com.qingcheng.service.order;

import java.util.Map;

public interface WxPayService {

    /**
     * 返回微信支付吗
     * @param orderId
     * @param money
     * @param url
     * @return
     */
    Map wxPay(String orderId,Integer money,String url);

    /**
     * 支付成功回调
     * @param xml
     */
    void payNotify(String xml);

    /**
     * 查询微信支付状态
     * @param orderId
     * @return
     */
    Map queryPayStatus(String orderId);

    /**
     * 关闭微信订单
     * @param
     */
    void closePay(Map map);
}
