package com.ruoyi.common.exception.user;

/**
 * 验证码失效异常类
 * 
 * @author 职称评审系统项目组
 */
public class CaptchaExpireException extends UserException
{
    private static final long serialVersionUID = 1L;

    public CaptchaExpireException()
    {
        super("user.jcaptcha.expire", null);
    }
}
