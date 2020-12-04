package com.example.order.service;

import com.example.common.entities.Order;
import com.example.common.service.AccountApi;
import com.example.common.service.OrderApi;
import com.example.common.service.StorageApi;
import com.example.order.dao.OrderDao;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("all")
public class OrderServiceImpl implements OrderApi {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private StorageApi storageService;

    @Autowired
    private AccountApi accountService;

    /**
     * ��������->���ÿ�����ۼ����->�����˻�����ۼ��˻����->�޸Ķ���״̬
     */
    @Override
    @GlobalTransactional(name="�µ�����",rollbackFor = Exception.class)
    public void create(Order order) {
        log.info("------->�µ���ʼ");
        //��Ӧ�ô�������
        orderDao.insert(order);

        //Զ�̵��ÿ�����ۼ����
        log.info("------->order-service�пۼ���濪ʼ");
        storageService.decrease(order.getProductId(),order.getCount());
        log.info("------->order-service�пۼ�������");

        //Զ�̵����˻�����ۼ����
        log.info("------->order-service�пۼ���ʼ");
        accountService.decrease(order.getUserId(),order.getMoney());
        log.info("------->order-service�пۼ�������");

        //�޸Ķ���״̬Ϊ�����
        log.info("------->order-service���޸Ķ���״̬��ʼ");
        orderDao.update(order.getId(), order.getUserId(),0);
        log.info("------->order-service���޸Ķ���״̬����");

        log.info("------->�µ�����");
    }

}
