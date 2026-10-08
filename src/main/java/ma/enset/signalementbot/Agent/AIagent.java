package ma.enset.signalementbot.Agent;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.ai.chat.messages.Message;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.util.*;

@Component
public class AIagent {
    private ChatClient chatClient;
    private ChatMemory memory;
    public AIagent(ChatClient.Builder builder, ChatMemory memory, ToolCallbackProvider tools){

        this.memory=memory;

        Arrays.stream(tools.getToolCallbacks()).forEach(toolCallback -> {
            System.out.println("---------------------");
            System.out.println(toolCallback.getToolDefinition());
            System.out.println("---------------------");
        });
        this.chatClient=builder
                .defaultToolCallbacks(tools)
                .build();
    }

    public void viderMemory(String id){
        memory.clear(id);
    }

    public void chargerConversation(String userId, String sessionId) {
        if (!memory.get(userId).isEmpty()) return;

        RestClient restClient = RestClient.create();
        try {
            var response = restClient.get()
                    .uri("http://localhost:8080/Conversation/" + userId)
                    .header("Cookie", "JSESSIONID=" + sessionId)
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody();
            if (body == null || body.equals("null")) return;

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> conversation = mapper.readValue(body, new TypeReference<>() {});

            List<Message> messages = new ArrayList<>();
            conversation.forEach((q, r) -> {
                messages.add(new UserMessage(q));
                messages.add(new AssistantMessage(r));
            });
            memory.add(userId, messages);

        } catch (Exception e) {
            System.out.println("Pas de conversation existante : " + e.getMessage());
        }
    }

    public String AskAgent(String question, MultipartFile image,String role,String id,String sessionId){

        chargerConversation(id,sessionId);
        MessageChatMemoryAdvisor userAdvisor = MessageChatMemoryAdvisor.builder(memory)
                .conversationId(id)  // ← id spécifique
                .build();

        String SystemPrompt;
        if (role.equals("ADMIN")){
            SystemPrompt= """
                                Tu es un assistant de gestion des signalements urbains pour un ADMIN.
                                Tu as accès à tous les outils MCP disponibles.
                                Tu as une mémoire persistante — tu te souviens des conversations passées de l'utilisateur.
                                Ne dis JAMAIS que tu ne peux pas garder les informations en mémoire.
                    """;
        }else {
            SystemPrompt= """
                                Tu es un assistant pour un utilisateur normal.
                                Tu peux UNIQUEMENT utiliser ces deux outils :
                                - listsignalement : voir les signalements
                                - listCommentaire : voir les commentaires
                                Tu NE PEUX PAS supprimer, modifier, ou accéder à d'autres données.
                                Si l'utilisateur demande autre chose, refuse poliment.
                                Tu as une mémoire persistante — tu te souviens des conversations passées de l'utilisateur.
                                Ne dis JAMAIS que tu ne peux pas garder les informations en mémoire.
                    """;
        }

        if (image!=null && !image.isEmpty()){
            try {
                byte[] bytes = image.getBytes();
                String mimeType = image.getContentType();

                var userMessage= UserMessage.builder()
                        .text(question)
                        .media(new Media(MimeTypeUtils.parseMimeType(mimeType),
                                new ByteArrayResource(bytes)))
                        .build();
                return chatClient.prompt()
                        .system(SystemPrompt)
                        .messages(userMessage)
                        .advisors(userAdvisor)
                        .call().content();
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        return chatClient.prompt()
                .system(SystemPrompt)
                .user(question)
                .advisors(userAdvisor)
                .call().content();
    }
}
