package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);


    public Order findById(String id);


    public Map<String,Object> add(Order order);


    public void update(Order order);


    public void delete(String id);

    /**
     * 微信支付后更新订单状态
     * @param out_trade_no
     * @param transaction_id
     */
    void updatePayStatus(String out_trade_no, String transaction_id);

    /**
     * 关闭未支付订单
     * @param
     */
    void closeUnPay(Order order);
}
