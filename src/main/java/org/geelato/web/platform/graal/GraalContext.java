package org.geelato.web.platform.graal;

public class GraalContext {

    private Object result;
//    private Object parameter;

    public GraalContext(Object result) {
//        this.parameter=parameter;
        this.result = result;
    }

//    public Object getParameter() {
//        return parameter;
//    }

//    public void setParameter(Object parameter) {
//        this.parameter = parameter;
//    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
