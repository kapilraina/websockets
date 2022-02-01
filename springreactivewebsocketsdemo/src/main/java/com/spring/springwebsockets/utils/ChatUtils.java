package com.spring.springwebsockets.utils;

import com.spring.springwebsockets.model.ChatProps;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatUtils {

    private ChatProps props;
    SimpleDateFormat sdf = null;

    public ChatUtils(ChatProps props) {
        this.props = props;
        sdf = new SimpleDateFormat(props.getTimestampdateformat());
    }

    public String getCurrentTimeSamp() {
        return sdf.format(new Date());
    }

}
