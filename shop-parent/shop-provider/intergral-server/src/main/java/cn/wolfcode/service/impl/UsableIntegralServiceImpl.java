package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by lanxw
 */
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    public void decrIntegral(OperateIntergralVo vo) {
        int i = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if(i==0){
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }

    }

    @Override
    public void incrIntegral(OperateIntergralVo vo) {
        int i = usableIntegralMapper.incrIntergral(vo.getUserId(), vo.getValue());

    }

    @Override
    @Transactional
    public void decrIntegralTry(OperateIntergralVo vo, BusinessActionContext context) {
        System.out.println("try");
        //插入事务控制表
        AccountTransaction accountTransaction = new AccountTransaction();
        accountTransaction.setTxId(context.getXid());//全局事务id
        accountTransaction.setActionId(context.getBranchId());//分支事务id
        accountTransaction.setUserId(vo.getUserId());
        Date date = new Date();
        accountTransaction.setGmtCreated(date);
        accountTransaction.setGmtModified(date);
        accountTransaction.setAmount(vo.getValue());
        accountTransactionMapper.insert(accountTransaction);
        //减积分逻辑
        int i = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if(i==0){
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    @Transactional
    public void incrIntergralCommit(BusinessActionContext context) {
        System.out.println("commit");
        //查询事务记录
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (accountTransaction!=null){
            if(AccountTransaction.STATE_TRY==accountTransaction.getState()){
                int i = accountTransactionMapper.updateAccountTransactionState(
                        context.getXid(),
                        context.getBranchId(),
                        AccountTransaction.STATE_COMMIT,
                        AccountTransaction.STATE_TRY);
                if (i==0){
                    throw new BusinessException(new CodeMsg(1001,"提交这里出错了"));
                }
            }else if (AccountTransaction.STATE_COMMIT==accountTransaction.getState()){
                //如果是commit不做事前
            }else {
                //通知管理员
            }
        }else {
            //给mq写休息通知管理员
        }
    }

    @Override
    @Transactional
    public void incrIntergralRollback(BusinessActionContext context) {
        System.out.println("rollback");
        //查询事务记录
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (accountTransaction!=null){
            if(AccountTransaction.STATE_TRY==accountTransaction.getState()){
                //处于try，将状态修改为cancel
                accountTransactionMapper.updateAccountTransactionState(context.getXid(),context.getBranchId(),AccountTransaction.STATE_CANCEL,AccountTransaction.STATE_TRY);
                //执行cancel逻辑

                usableIntegralMapper.incrIntergral(accountTransaction.getUserId(),accountTransaction.getAmount());

            }else if (AccountTransaction.STATE_CANCEL==accountTransaction.getState()){
                    //不做处理，之前执行过了
            }else {
                //意外了，通知管理员
            }
        }else {
            //插入事务控制表
            String vo = (String) context.getActionContext("vo");
            System.out.println("存储在上下文的对象"+vo);
            OperateIntergralVo vo1= JSON.parseObject(vo,OperateIntergralVo.class);
            accountTransaction.setTxId(context.getXid());//全局事务id
            accountTransaction.setActionId(context.getBranchId());//分支事务id
            accountTransaction.setUserId(vo1.getUserId());
            Date date = new Date();
            accountTransaction.setGmtCreated(date);
            accountTransaction.setGmtModified(date);
            accountTransaction.setAmount(vo1.getValue());
            accountTransaction.setState(AccountTransaction.STATE_CANCEL);
            accountTransactionMapper.insert(accountTransaction);
        }
    }
}
