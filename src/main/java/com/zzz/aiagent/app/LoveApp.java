package com.zzz.aiagent.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.zzz.aiagent.advisor.MyLoggerAdvisor;
import com.zzz.aiagent.advisor.ReReadingAdvisor;
import com.zzz.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private static final Logger logger = LoggerFactory.getLogger(LoveApp.class);

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深谙恋爱心理领域的专家，对话时用户身份，告知所处阶段。"
            + "围绕单身、恋爱、已婚三种状态提问；单身状态询问社交圈拓展及追求心仪对象的困扰；"
            + "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲密关系处理的问题。"
            + "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";


    public LoveApp(ChatModel dashScopeChatModel) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        this.chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build(),
                        MyLoggerAdvisor.builder().build()
                        //, new ReReadingAdvisor()

                )
                .build();
    }

    public String doChat(String message, String chatId) {
        String con = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec
                        .param("conversationId", chatId)
                        .param("historySize", 10)
                )
                .call()
                .content();
        logger.info("content : {}",con);
        return con;
    }
    record LoveReport(String title,List<String> suggestions){

    }
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport con = chatClient.prompt()
                .system(SYSTEM_PROMPT + "每次对话后都生成恋爱结果，标题为{用户名}，内容为建议列表")
                .user(message)
                .advisors(spec -> spec
                        .param("conversationId", chatId)
                        .param("historySize", 10)
                )
                .call()
                .entity(LoveReport.class);

        logger.info("contentReport : {}", con);
        return con;
    }


    @Resource
    private VectorStore loveAppVectorStore;
    @Resource
    private QueryRewriter queryRewriter;
    @Resource
    private ToolCallback[] alltools;
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithRag(String message , String chatId){
        String rewrite = queryRewriter.doQueryRewrite(message);
        String content = chatClient
                .prompt()
                .user(rewrite)
                .advisors(spec -> spec
                        .param("conversationId", chatId)
                        .param("historySize", 10)
                )
                .advisors(MyLoggerAdvisor.builder().build())
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .content();
        return content;

    }


    public LoveReport doChatWithTools(String message, String chatId) {
        LoveReport con = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(spec -> spec
                        .param("conversationId", chatId)
                        .param("historySize", 10)
                )
                .tools(alltools)
                .call()
                .entity(LoveReport.class);

        logger.info("contentReport : {}", con);
        return con;
    }

    /*@Resource
    private ToolCallbackProvider toolCallbackProvider;*/

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        logger.info("content: {}", content);
        return content;
    }

}