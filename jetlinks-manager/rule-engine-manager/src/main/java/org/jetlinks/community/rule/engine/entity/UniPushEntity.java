package org.jetlinks.community.rule.engine.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.rsocket.Payload;

import java.util.List;

public class UniPushEntity {
    private List<String> push_clientid;
    private String title;
    private String content;
//    private Options options;
    private Payload payload;

    // 构造函数和 getter/setter
    public UniPushEntity(List<String> push_clientid, String title, String content, Payload payload) {
        this.push_clientid = push_clientid;
        this.title = title;
        this.content = content;
        this.payload = payload;
//        this.options = options;
    }

    public List<String> getPush_clientid() {
        return push_clientid;
    }

    public void setPush_clientid(List<String> push_clientid) {
        this.push_clientid = push_clientid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }


//    public Options getOptions() {
//        return options;
//    }
//
//    public void setOptions(Options options) {
//        this.options = options;
//    }


//    @JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null properties
//    public static class Options {
//        private HW HW;
//
//        public Options(HW HW) {
//            this.HW = HW;
//        }
//
//        public HW getHW() {
//            return HW;
//        }
//
//        public void setHW(HW HW) {
//            this.HW = HW;
//        }
//
//        @JsonInclude(JsonInclude.Include.NON_NULL)
//        public static class HW {
//            @JsonProperty("/message/android/category")
//            private String messageAndroidCategory;
//
//            public String getMessageAndroidCategory() {
//                return messageAndroidCategory;
//            }
//
//            public void setMessageAndroidCategory(String messageAndroidCategory) {
//                this.messageAndroidCategory = messageAndroidCategory;
//            }
//
//            public HW(String messageAndroidCategory) {
//                this.messageAndroidCategory = messageAndroidCategory;
//            }
//        }
//    }

    @JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null properties
    public static class Payload {
        private String push_type;

        public Payload(String push_type) {
            this.push_type = push_type;
        }

        public String getPush_type() {
            return push_type;
        }

        public void setPush_type(String push_type) {
            this.push_type = push_type;
        }
    }

}
