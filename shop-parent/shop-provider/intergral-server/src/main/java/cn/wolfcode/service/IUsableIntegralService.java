package cn.wolfcode.service;

import cn.wolfcode.domain.OperateIntergralVo;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Created by lanxw
 */
@LocalTCC
public interface IUsableIntegralService {
    /**
     * 积分扣减
     * @param vo
     */
    void decrIntegral(OperateIntergralVo vo);

    /**
     * 积分增加
     * @param vo
     */
    void incrIntegral(OperateIntergralVo vo);

    /**
     *
     * @param vo
     * @param context
     */
    @TwoPhaseBusinessAction(name = "decrIntegralTry" ,
                            commitMethod = "incrIntergralCommit",
                            rollbackMethod = "incrIntergralRollback")
    void decrIntegralTry(@BusinessActionContextParameter(paramName = "vo") OperateIntergralVo vo,
                         BusinessActionContext context);

    void incrIntergralCommit(BusinessActionContext context);

    void incrIntergralRollback(BusinessActionContext context);

}
