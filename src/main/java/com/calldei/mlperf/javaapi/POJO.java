package com.calldei.mlperf.javaapi;

import com.marklogic.client.pojo.annotation.Id;

public class POJO {
    @Id
    public Long id;
    public String name;
    public String value;
    public String attrShort;
    public String attrMedium ;
    public String attrLong ;

    public static class Inner {
        public String name ;
        public String value ;
        public String attrShort;
        public String attrMedium;
        public String attrLong;
        public int attrInt;
        public double attrDouble;
    }
    public Inner innerArray[];

};
