package com.zzz.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜寻一些有关蔡徐坤的图片";
        String answer = loveApp.doChatWithMcp(message,chatId);
        System.out.println("answer = " + answer);
    }

    /*@Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();

        String message = "你好，我的名字是水分子";
        String answer = loveApp.doChat(message,chatId);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我的名字是水分子,我失恋了";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message,chatId);
    }*/
}