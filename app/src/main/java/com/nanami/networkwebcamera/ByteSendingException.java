package com.nanami.networkwebcamera;

public class ByteSendingException extends Exception{
    public ByteSendingException(){}
    public ByteSendingException(String str){
        super(str);
    }
    public ByteSendingException(Throwable cause){
        super(cause);
    }
    public ByteSendingException(String str, Throwable cause){
        super(str, cause);
    }
}
